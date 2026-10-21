package com.codex.plunder.ui;

import com.codex.plunder.core.Leaks;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * Each room on the way down as the player usually gets through it (the trap, the walk and the
 * door, with searching left out), against the quickest they ever have: the usual crossing, the seconds over, a bar with the quickest ticked on it, and the
 * experience an hour those seconds cost. The room that costs most is lit.
 */
class LeakTable extends JComponent
{
	private static final int ROW = 17;

	private final Leaks leaks;

	LeakTable(Leaks leaks)
	{
		this.leaks = leaks;
		setOpaque(false);
		setPreferredSize(new Dimension(10, ROW * Math.max(1, leaks.getRooms().size())));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int w = getWidth();
		Leaks.Leak worst = leaks.biggest();

		int timeRight = fm.stringWidth("Room 8") + 8 + fm.stringWidth("0:00");
		int gapRight = timeRight + 6 + fm.stringWidth("+00s");
		int costW = fm.stringWidth("00.0k/h");
		int barX = gapRight + 8;
		int barW = Math.max(20, w - barX - costW - 8);

		int longest = 1;
		for (Leaks.Leak l : leaks.getRooms())
		{
			longest = Math.max(longest, l.getTypical());
		}

		int y = 0;
		for (Leaks.Leak l : leaks.getRooms())
		{
			int base = y + (ROW + fm.getAscent()) / 2 - 1;
			boolean lit = l == worst;
			if (lit)
			{
				g.setColor(Theme.alpha(Theme.EMBER, 26));
				g.fill(new RoundRectangle2D.Float(-4, y, w + 8, ROW, 6, 6));
			}
			g.setColor(lit ? Theme.EMBER : Theme.TEXT_MUTED);
			g.drawString("Room " + l.getRoom().getNumber(), 0, base);

			String time = Format.clock(l.getTypical());
			g.setColor(Theme.TEXT);
			g.drawString(time, timeRight - fm.stringWidth(time), base);

			int gap = l.getGap();
			boolean clean = Math.round(gap * 0.6) == 0;
			boolean noise = gap < Leaks.NOISE;
			String d = clean ? "even" : "+" + Format.seconds(gap);
			Ui.shadowed(g, d, gapRight - fm.stringWidth(d), base, noise ? Theme.GREEN : lit ? Theme.EMBER : Theme.GOLD_DIM);

			int mid = y + ROW / 2;
			g.setColor(Theme.INK_DEEP);
			g.fillRoundRect(barX, mid - 2, barW, 5, 5, 5);
			int quick = (int) Math.round(barW * l.getQuickest() / (double) longest);
			int fill = Math.max(2, (int) Math.round(barW * l.getTypical() / (double) longest));
			g.setColor(Theme.GOLD_DIM);
			g.fillRoundRect(barX, mid - 2, noise ? fill : Math.min(fill, quick), 5, 5, 5);
			if (fill > quick && !noise)
			{
				g.setColor(lit ? Theme.EMBER : Theme.mix(Theme.GOLD_DIM, Theme.EMBER, 0.5));
				g.fillRect(barX + quick, mid - 2, fill - quick, 5);
			}
			g.setColor(Theme.TEXT);
			g.fillRect(Math.min(barX + barW - 1, barX + quick), mid - 4, 1, 9);

			String cost = l.getXpPerHour() < 100 ? "-" : Format.amount(l.getXpPerHour()) + "/h";
			g.setColor(lit ? Theme.EMBER : Theme.TEXT_MUTED);
			g.drawString(cost, w - fm.stringWidth(cost), base);
			y += ROW;
		}
		g.dispose();
	}
}
