package com.codex.plunder.overlay;

import com.codex.plunder.ui.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Collections;
import java.util.List;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Outlines in the room what the plan says to do next and nothing else: the urns worth searching
 * in gold, the chest and sarcophagus in teal when the sceptre odds make them worth it, the spear
 * trap and the untried tomb doors when it is time to move on. Urns the plan skips stay dark.
 */
public class RoomHighlights extends Overlay
{
	/** One object to outline, and in what colour. */
	public static final class Mark
	{
		final TileObject object;
		final Color color;
		final boolean clickbox;

		public Mark(TileObject object, Color color, boolean clickbox)
		{
			this.object = object;
			this.color = color;
			this.clickbox = clickbox;
		}
	}

	private volatile List<Mark> marks = Collections.emptyList();

	public RoomHighlights()
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public void show(List<Mark> marks)
	{
		this.marks = marks == null ? Collections.emptyList() : marks;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		List<Mark> list = marks;
		if (list.isEmpty())
		{
			return null;
		}
		g.setStroke(new BasicStroke(1.6f));
		for (Mark m : list)
		{
			Shape shape = m.clickbox ? m.object.getClickbox() : hull(m.object);
			if (shape == null)
			{
				continue;
			}
			g.setColor(Theme.alpha(m.color, 44));
			g.fill(shape);
			g.setColor(Theme.alpha(m.color, 220));
			g.draw(shape);
		}
		return null;
	}

	private static Shape hull(TileObject object)
	{
		if (object instanceof net.runelite.api.GameObject)
		{
			return ((net.runelite.api.GameObject) object).getConvexHull();
		}
		return object.getClickbox();
	}
}
