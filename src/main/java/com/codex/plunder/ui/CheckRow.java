package com.codex.plunder.ui;

import com.codex.plunder.core.Check;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** One line of the ready check: a teal tick for a pass, an ember mark for a warning, a gold dot for a tip. */
class CheckRow extends JPanel
{
	CheckRow(Check check, int width)
	{
		super(new BorderLayout(8, 0));
		setOpaque(false);
		setBorder(new EmptyBorder(2, 0, 2, 0));
		Color color = color(check.getState());
		add(new Mark(check.getState(), color), BorderLayout.WEST);
		JPanel words = new JPanel(new BorderLayout(0, 1));
		words.setOpaque(false);
		words.add(Ui.label(check.getTitle(), Theme.body(), check.getState() == Check.State.WARN ? color : Theme.TEXT), BorderLayout.NORTH);
		words.add(Ui.wrapped(check.getDetail(), Theme.small(), Theme.TEXT_FAINT, width - 22), BorderLayout.CENTER);
		add(words, BorderLayout.CENTER);
	}

	static Color color(Check.State state)
	{
		switch (state)
		{
			case GOOD:
				return Theme.TEAL;
			case WARN:
				return Theme.EMBER;
			default:
				return Theme.GOLD;
		}
	}

	private static final class Mark extends JComponent
	{
		private final Check.State state;
		private final Color color;

		Mark(Check.State state, Color color)
		{
			this.state = state;
			this.color = color;
			setPreferredSize(new Dimension(16, 16));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int d = 14;
			int x = 1;
			int y = 2;
			g.setColor(Theme.alpha(color, 40));
			g.fill(new Ellipse2D.Float(x, y, d, d));
			g.setColor(color);
			g.setStroke(new BasicStroke(1.2f));
			g.draw(new Ellipse2D.Float(x + 0.5f, y + 0.5f, d - 1, d - 1));
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			switch (state)
			{
				case GOOD:
					Path2D tick = new Path2D.Float();
					tick.moveTo(x + 4, y + 7.5f);
					tick.lineTo(x + 6.2f, y + 9.8f);
					tick.lineTo(x + 10.2f, y + 4.6f);
					g.draw(tick);
					break;
				case WARN:
					g.drawLine(x + 7, y + 3, x + 7, y + 8);
					g.fillRect(x + 6, y + 10, 2, 2);
					break;
				default:
					g.fill(new Ellipse2D.Float(x + 5, y + 5, 4, 4));
					break;
			}
			g.dispose();
		}
	}
}
