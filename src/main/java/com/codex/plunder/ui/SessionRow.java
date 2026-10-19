package com.codex.plunder.ui;

import com.codex.plunder.core.Session;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/**
 * One past session: when, how many runs and how long, its experience an hour, and a gauge of that
 * rate against the best session's. The best session is lit and marked with a diamond.
 */
class SessionRow extends JComponent
{
	private final Session session;
	private final int bestRate;
	private final boolean best;

	SessionRow(Session session, int bestRate, boolean best)
	{
		this.session = session;
		this.bestRate = bestRate;
		this.best = best;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 34));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int top = small.getAscent() + 2;
		int x = 0;
		if (best)
		{
			SplitTable.diamond(g, 3, top - 4, Theme.GOLD_BRIGHT);
			x = 10;
		}
		String when = session.isCurrent() ? "Now" : Format.shortDay(session.getStartedAt());
		Ui.shadowed(g, when, x, top, best ? Theme.GOLD_BRIGHT : Theme.GOLD);
		int runs = session.getRuns().size();
		String what = runs + (runs == 1 ? " run, " : " runs, ") + Format.duration(session.spanMs());
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(what, small, w - x - small.stringWidth(when) - 70), x + small.stringWidth(when) + 6, top);
		String rate = Format.amount(session.xpPerHour()) + " xp/hr";
		Ui.shadowed(g, rate, w - small.stringWidth(rate), top, best ? Theme.GOLD_BRIGHT : Theme.GOLD);

		// The rate against the best session's, with a teal tick for a sceptre found.
		int barY = top + 7;
		int barW = w;
		g.setColor(Theme.alpha(Theme.GOLD, 30));
		g.fillRect(0, barY, barW, 4);
		double share = bestRate <= 0 ? 0 : Math.min(1, session.xpPerHour() / (double) bestRate);
		Color fill = best ? Theme.GOLD_BRIGHT : session.isCurrent() ? Theme.GOLD : Theme.GOLD_DIM;
		g.setColor(fill);
		g.fillRect(0, barY, (int) Math.round(barW * share), 4);
		if (session.getSceptres() > 0)
		{
			g.setColor(Theme.TEAL);
			String found = session.getSceptres() == 1 ? "sceptre" : session.getSceptres() + " sceptres";
			g.drawString(found, w - small.stringWidth(found), barY + 4 + small.getAscent() + 1);
		}
		g.dispose();
	}
}
