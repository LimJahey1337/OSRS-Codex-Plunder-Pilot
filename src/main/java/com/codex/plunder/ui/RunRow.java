package com.codex.plunder.ui;

import com.codex.plunder.core.Run;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** One past run: how deep, what it paid, and when; a sceptre run is marked in teal. */
class RunRow extends JComponent
{
	private final Run run;
	private final long now;

	RunRow(Run run, long now)
	{
		this.run = run;
		this.now = now;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 32));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		int top = h / 2 - 2;
		int bottom = h / 2 + small.getAscent() + 1;
		int x = 0;
		if (run.isSceptre())
		{
			Sigil.paint(g, 0, (h - 16) / 2f, 16);
			x = 20;
		}
		String title = "+" + Format.amount(run.getThievingXp()) + " xp";
		Ui.shadowed(g, title, x, top, run.isSceptre() ? Theme.TEAL : Theme.GOLD);
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_MUTED);
		g.drawString("to room " + run.getDeepest(), x + body.stringWidth(title) + 6, top);
		String when = Format.ago(now - run.getAt());
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(when, w - small.stringWidth(when), top);

		StringBuilder sub = new StringBuilder();
		sub.append(run.getUrns()).append(run.getUrns() == 1 ? " urn" : " urns");
		if (run.getChests() > 0)
		{
			sub.append(", ").append(run.getChests()).append(run.getChests() == 1 ? " chest" : " chests");
		}
		if (run.getSarcophagi() > 0)
		{
			sub.append(", ").append(run.getSarcophagi()).append(" sarc");
		}
		if (run.getArtefacts() > 0)
		{
			sub.append(", ").append(Format.coins(run.getArtefacts())).append(" gp");
		}
		if (run.isSceptre())
		{
			sub.insert(0, "Sceptre!  ");
		}
		g.setColor(run.isSceptre() ? Theme.TEAL : Theme.TEXT_FAINT);
		g.drawString(Ui.clip(sub.toString(), small, w - x - 40), x, bottom);
		String clock = Format.clock(run.getTicks());
		g.drawString(clock, w - small.stringWidth(clock), bottom);
		g.dispose();
	}
}
