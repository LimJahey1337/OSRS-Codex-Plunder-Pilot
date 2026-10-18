package com.codex.plunder.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * The Pilot's mark: a stepped gilt pyramid under a desert sky, with the Pharaoh's sceptre
 * standing before it, its teal jewel catching the light.
 */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;
		float stroke = Math.max(0.6f, 0.55f * s);

		// The pyramid: a lit face and a shadowed face meeting at the ridge.
		float apexX = x + 7.4f * s;
		float apexY = y + 1.6f * s;
		float baseY = y + 14.2f * s;
		float left = x + 0.6f * s;
		float right = x + 14.2f * s;
		float ridgeX = x + 8.8f * s;
		Path2D lit = new Path2D.Float();
		lit.moveTo(apexX, apexY);
		lit.lineTo(ridgeX, baseY);
		lit.lineTo(left, baseY);
		lit.closePath();
		Path2D shade = new Path2D.Float();
		shade.moveTo(apexX, apexY);
		shade.lineTo(right, baseY);
		shade.lineTo(ridgeX, baseY);
		shade.closePath();
		g.setPaint(new GradientPaint(x, apexY, Theme.GOLD_BRIGHT, x, baseY, Theme.GOLD));
		g.fill(lit);
		g.setPaint(new GradientPaint(x, apexY, Theme.mix(Theme.GOLD, Theme.GOLD_DIM, 0.4f), x, baseY, Theme.mix(Theme.GOLD_DIM, Theme.INK_DEEP, 0.45f)));
		g.fill(shade);

		// Courses of stone across both faces.
		g.setStroke(new BasicStroke(Math.max(0.5f, 0.4f * s)));
		g.setColor(Theme.alpha(Theme.INK_DEEP, 120));
		for (int i = 1; i <= 3; i++)
		{
			float t = i / 4f;
			float cy = apexY + (baseY - apexY) * t;
			float lx = apexX + (left - apexX) * t;
			float rx = apexX + (right - apexX) * t;
			g.drawLine(Math.round(lx), Math.round(cy), Math.round(rx), Math.round(cy));
		}
		// The doorway at the foot of the lit face.
		g.setColor(Theme.alpha(Theme.INK_DEEP, 200));
		g.fillRect(Math.round(x + 4.8f * s), Math.round(y + 11.2f * s), Math.max(1, Math.round(1.6f * s)), Math.round(3f * s));

		Path2D outline = new Path2D.Float();
		outline.moveTo(apexX, apexY);
		outline.lineTo(right, baseY);
		outline.lineTo(left, baseY);
		outline.closePath();
		g.setStroke(new BasicStroke(stroke));
		g.setColor(Theme.INK_DEEP);
		g.draw(outline);

		// The sceptre: a gilt staff with a teal jewel, standing in front of the shadowed face.
		float sx = x + 12.2f * s;
		float top = y + 5.6f * s;
		g.setStroke(new BasicStroke(Math.max(1.8f, 2.0f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.INK_DEEP);
		g.drawLine(Math.round(sx), Math.round(top + 2f * s), Math.round(sx), Math.round(y + 15.4f * s));
		g.setStroke(new BasicStroke(Math.max(0.9f, 0.9f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setPaint(new GradientPaint(sx, top, Theme.GOLD_BRIGHT, sx, y + 15 * s, Theme.GOLD_DIM));
		g.drawLine(Math.round(sx), Math.round(top + 2f * s), Math.round(sx), Math.round(y + 15f * s));
		float r = 2.1f * s;
		Ellipse2D jewel = new Ellipse2D.Float(sx - r, top - r, r * 2, r * 2);
		g.setColor(Theme.INK_DEEP);
		g.setStroke(new BasicStroke(Math.max(1.2f, 1.2f * s)));
		g.draw(jewel);
		g.setPaint(new GradientPaint(sx, top - r, Theme.mix(Theme.TEAL, Theme.TEXT, 0.4f), sx, top + r, Theme.mix(Theme.TEAL, Theme.INK_DEEP, 0.3f)));
		g.fill(jewel);
		g.setColor(new Color(255, 255, 255, 160));
		g.fill(new Ellipse2D.Float(sx - r * 0.55f, top - r * 0.6f, r * 0.6f, r * 0.6f));
	}
}
