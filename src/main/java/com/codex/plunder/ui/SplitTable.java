package com.codex.plunder.ui;

import com.codex.plunder.core.Room;
import com.codex.plunder.core.Run;
import com.codex.plunder.core.Splits;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * The last run to the deepest room, room by room, against the best before it: when each room
 * was entered, how far ahead or behind that was, and how long the room took against its quickest
 * crossing ever. A room crossed as fast as it has ever been gets a gold diamond.
 */
class SplitTable extends JComponent
{
	private static final int ROW = 17;

	private final Run run;
	private final Splits best;

	/**
	 * @param run the run to show, which reached the deepest room
	 * @param best the splits from the runs before it
	 */
	SplitTable(Run run, Splits best)
	{
		this.run = run;
		this.best = best;
		setOpaque(false);
		setPreferredSize(new Dimension(10, ROW * (best.getTop().ordinal() + 1)));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int w = getWidth();
		Room top = best.getTop();

		// Column edges: room, time entered, the gap to the best, then the room's own bar.
		int timeRight = fm.stringWidth("Room 8") + 8 + fm.stringWidth("0:00");
		int deltaRight = timeRight + 6 + fm.stringWidth("+0:00");
		int barX = deltaRight + 8;
		int barW = Math.max(20, w - barX - 2);

		int longest = 1;
		for (int i = 0; i < top.ordinal(); i++)
		{
			Room r = Room.values()[i];
			longest = Math.max(longest, Math.max(Splits.segment(run, r), best.bestSegment(r)));
		}

		for (int i = 0; i <= top.ordinal(); i++)
		{
			Room room = Room.values()[i];
			int y = i * ROW;
			int base = y + (ROW + fm.getAscent()) / 2 - 1;
			boolean last = room == top;
			if (last)
			{
				g.setColor(Theme.alpha(Theme.GOLD, 26));
				g.fill(new RoundRectangle2D.Float(-4, y, w + 8, ROW, 6, 6));
			}
			g.setColor(last ? Theme.GOLD : Theme.TEXT_MUTED);
			g.drawString("Room " + room.getNumber(), 0, base);

			int at = run.splitAt(room);
			String time = at < 0 ? "-" : Format.clock(at);
			g.setColor(last ? Theme.GOLD_BRIGHT : Theme.TEXT);
			g.drawString(time, timeRight - fm.stringWidth(time), base);

			Integer delta = i == 0 ? null : best.delta(room, at);
			if (delta != null)
			{
				String d = Format.split(delta);
				Color c = Math.round(delta * 0.6) == 0 ? Theme.TEXT_MUTED : delta < 0 ? Theme.GREEN : Theme.EMBER;
				Ui.shadowed(g, d, deltaRight - fm.stringWidth(d), base, c);
			}

			if (!last)
			{
				int seg = Splits.segment(run, room);
				int quickest = best.bestSegment(room);
				int mid = y + ROW / 2;
				g.setColor(Theme.INK_DEEP);
				g.fillRoundRect(barX, mid - 2, barW, 5, 5, 5);
				if (seg >= 0)
				{
					boolean gold = quickest < 0 || seg <= quickest;
					int fill = Math.max(2, (int) Math.round(barW * seg / (double) longest));
					g.setColor(gold ? Theme.GOLD : Theme.mix(Theme.GOLD_DIM, Theme.EMBER, quickest > 0 ? Math.min(1, (seg - quickest) / (double) quickest) : 0));
					g.fillRoundRect(barX, mid - 2, fill, 5, 5, 5);
					if (gold && quickest >= 0)
					{
						diamond(g, barX + fill + 4, mid, Theme.GOLD_BRIGHT);
					}
				}
				if (quickest >= 0)
				{
					int mx = barX + (int) Math.round(barW * quickest / (double) longest);
					g.setColor(Theme.TEXT);
					g.fillRect(Math.min(barX + barW - 1, mx), mid - 4, 1, 9);
				}
			}
		}
		g.dispose();
	}

	static void diamond(Graphics2D g, int x, int y, Color c)
	{
		Path2D d = new Path2D.Float();
		d.moveTo(x, y - 3);
		d.lineTo(x + 3, y);
		d.lineTo(x, y + 3);
		d.lineTo(x - 3, y);
		d.closePath();
		g.setColor(c);
		g.fill(d);
	}
}
