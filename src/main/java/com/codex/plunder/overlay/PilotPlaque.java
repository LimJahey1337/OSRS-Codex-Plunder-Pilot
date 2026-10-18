package com.codex.plunder.overlay;

import com.codex.plunder.ui.Sigil;
import com.codex.plunder.ui.Theme;
import com.codex.plunder.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A slim gilt plaque in the game view during a run: the room, the clock, and the one thing to
 * do next, with a hairline under it that drains with the clock. It draws what the plugin hands it.
 */
public class PilotPlaque extends Overlay
{
	private static final int PAD = 7;

	private volatile Shown shown;

	private static final class Shown
	{
		final String figure;
		final String step;
		final String detail;
		final Color color;
		final double progress;

		Shown(String figure, String step, String detail, Color color, double progress)
		{
			this.figure = figure;
			this.step = step;
			this.detail = detail;
			this.color = color;
			this.progress = progress;
		}
	}

	public PilotPlaque()
	{
		setPosition(OverlayPosition.TOP_LEFT);
	}

	/**
	 * @param figure the big text, such as "R6  3:12", or null to hide the plaque
	 * @param progress the share of the clock left, 0 to 1
	 */
	public void show(String figure, String step, String detail, Color color, double progress)
	{
		shown = figure == null ? null : new Shown(figure, step, detail, color, progress);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Shown s = shown;
		if (s == null)
		{
			return null;
		}
		Theme.smooth(g);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int icon = 16;
		int textX = PAD + icon + 6;
		int line1 = bold.stringWidth(s.figure) + 10 + bold.stringWidth(s.step);
		int line2 = s.detail == null ? 0 : Math.min(small.stringWidth(s.detail), 260);
		int width = textX + Math.max(line1, line2) + PAD;
		int height = PAD + bold.getHeight() + (s.detail == null ? 0 : small.getHeight() + 1) + PAD;

		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 232), 0, height, Theme.alpha(Theme.INK_DEEP, 232)));
		g.fill(shape);
		g.setColor(Theme.alpha(s.color, 170));
		g.draw(shape);

		Sigil.paint(g, PAD, (height - icon) / 2f - 1, icon);
		int base = PAD + bold.getAscent();
		g.setFont(Theme.title());
		Ui.shadowed(g, s.figure, textX, base, s.color);
		Ui.shadowed(g, s.step, textX + bold.stringWidth(s.figure) + 10, base, Theme.TEXT);
		if (s.detail != null)
		{
			g.setFont(Theme.small());
			Ui.shadowed(g, Ui.clip(s.detail, small, width - textX - PAD), textX, base + small.getHeight() + 2, Theme.TEXT_MUTED);
		}
		int bar = (int) Math.round((width - 20) * Math.max(0, Math.min(1, s.progress)));
		g.setColor(Theme.alpha(Theme.INK_DEEP, 220));
		g.fillRect(10, height - 3, width - 20, 2);
		g.setColor(s.color);
		g.fillRect(10, height - 3, bar, 2);
		return new Dimension(width, height);
	}
}
