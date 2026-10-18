package com.codex.plunder.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/** A labelled gauge: a sunken channel filled toward a share, with an optional tick mark. */
class Meter extends JComponent
{
	private final Animator fill = new Animator(this);
	private final String label;
	private final String value;
	private final Color color;
	private final double mark;

	/** @param mark a share to mark on the channel, or below 0 for none */
	Meter(String label, String value, double share, Color color, double mark)
	{
		this.label = label;
		this.value = value;
		this.color = color;
		this.mark = mark;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 30));
		fill.set(Math.max(0, Math.min(1, share)), false);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(label, small, w - small.stringWidth(value) - 8), 0, small.getAscent());
		g.setColor(color);
		g.drawString(value, w - small.stringWidth(value), small.getAscent());

		int y = small.getHeight() + 4;
		RoundRectangle2D channel = new RoundRectangle2D.Float(0.5f, y + 0.5f, w - 1, 7, 7, 7);
		g.setColor(Theme.INK_DEEP);
		g.fill(channel);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(channel);
		int lit = (int) Math.round((w - 2) * fill.get());
		if (lit > 0)
		{
			g.setPaint(new GradientPaint(0, 0, Theme.mix(color, Theme.INK, 0.4), w, 0, color));
			g.fillRoundRect(1, y + 1, lit, 6, 6, 6);
		}
		if (mark >= 0 && mark <= 1)
		{
			int mx = (int) Math.round(1 + (w - 3) * mark);
			g.setColor(Theme.TEXT);
			g.fillRect(mx, y - 1, 1, 10);
		}
		g.dispose();
	}
}
