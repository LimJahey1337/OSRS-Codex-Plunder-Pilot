package com.codex.plunder.ui;

import com.codex.plunder.core.Run;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import javax.swing.JComponent;

/**
 * The session's runs as a row of gilt bars, oldest on the left, each as tall as the experience
 * it paid. The best run is lit brightest and a sceptre run stands in teal.
 */
class SessionBars extends JComponent
{
	/** The most bars drawn; a longer session shows its newest runs. */
	private static final int MOST = 30;

	private final List<Run> runs;

	SessionBars(List<Run> runs)
	{
		this.runs = runs.size() > MOST ? runs.subList(runs.size() - MOST, runs.size()) : runs;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 34));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		g.setColor(Theme.alpha(Theme.GOLD, 40));
		g.fillRect(0, h - 1, w, 1);
		if (runs.isEmpty())
		{
			g.dispose();
			return;
		}
		int most = 1;
		Run best = null;
		for (Run r : runs)
		{
			if (best == null || r.getThievingXp() > best.getThievingXp())
			{
				best = r;
			}
			most = Math.max(most, r.getThievingXp());
		}
		int gap = 2;
		int slot = Math.max(3, Math.min(14, (w + gap) / runs.size() - gap));
		int x = 0;
		for (Run r : runs)
		{
			int bh = Math.max(2, (int) Math.round((h - 3) * r.getThievingXp() / (double) most));
			Color c = r.isSceptre() ? Theme.TEAL : r == best ? Theme.GOLD_BRIGHT : Theme.GOLD_DIM;
			g.setPaint(new GradientPaint(0, h - bh, c, 0, h, Theme.mix(c, Theme.INK, 0.55)));
			g.fillRoundRect(x, h - 1 - bh, slot, bh, 3, 3);
			x += slot + gap;
		}
		g.dispose();
	}
}
