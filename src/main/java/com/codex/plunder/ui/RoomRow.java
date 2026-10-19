package com.codex.plunder.ui;

import com.codex.plunder.core.Plan;
import com.codex.plunder.core.Room;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;

/**
 * One room of the route: a numbered seal, the room's level, and what to do there. The room the
 * player stands in is ringed in teal; rooms walked straight through sit quietly; rooms the level
 * does not open yet are faded, with the level that opens them.
 */
class RoomRow extends JComponent
{
	private static final int HEIGHT = 34;

	private final Room room;
	private final Plan.Stop stop;
	private final boolean here;
	private final int locked;

	/** A room on the route. */
	RoomRow(Plan.Stop stop, boolean here)
	{
		this.room = stop.getRoom();
		this.stop = stop;
		this.here = here;
		this.locked = 0;
		init();
	}

	/** A room not open yet. */
	RoomRow(Room room)
	{
		this.room = room;
		this.stop = null;
		this.here = false;
		this.locked = room.getLevel();
		init();
	}

	private void init()
	{
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
		setToolTipText("Room " + room.getNumber() + ": urns " + room.getUrnXp() + " xp, chest " + room.getChestXp()
			+ " xp, sceptre 1 in " + room.getSceptreOneIn());
	}

	static String work(Plan.Stop stop)
	{
		if (stop.isPassThrough())
		{
			return "Pass through";
		}
		StringBuilder text = new StringBuilder();
		if (stop.getUrns() > 0)
		{
			text.append(stop.getUrns()).append(stop.getUrns() == 1 ? " urn" : " urns");
		}
		if (stop.isChest())
		{
			text.append(text.length() > 0 ? " + " : "").append("chest");
		}
		if (stop.isSarcophagus())
		{
			text.append(text.length() > 0 ? " + " : "").append("sarc");
		}
		return text.toString();
	}

	private int xp()
	{
		return stop.getUrns() * room.getUrnXp() + (stop.isChest() ? room.getChestXp() : 0);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		boolean pass = stop != null && stop.isPassThrough();
		Color seal = locked > 0 ? Theme.STROKE : pass ? Theme.GOLD_DIM : Theme.GOLD;

		if (here)
		{
			g.setColor(Theme.alpha(Theme.TEAL, 26));
			g.fillRoundRect(0, 1, w, h - 2, 8, 8);
		}

		// The seal: a gilt disc with the room's number.
		int d = 22;
		int sx = 4;
		int sy = (h - d) / 2;
		g.setPaint(new GradientPaint(0, sy, locked > 0 ? Theme.CARD_TOP : Theme.mix(seal, Color.WHITE, 0.2), 0, sy + d, locked > 0 ? Theme.CARD : Theme.mix(seal, Theme.INK_DEEP, 0.4)));
		g.fill(new Ellipse2D.Float(sx, sy, d, d));
		g.setStroke(new BasicStroke(here ? 1.6f : 1f));
		g.setColor(here ? Theme.TEAL : Theme.INK_DEEP);
		g.draw(new Ellipse2D.Float(sx + 0.5f, sy + 0.5f, d - 1, d - 1));
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		String n = String.valueOf(room.getNumber());
		g.setColor(locked > 0 ? Theme.TEXT_FAINT : Theme.INK_DEEP);
		g.drawString(n, sx + (d - bold.stringWidth(n)) / 2, sy + (d + bold.getAscent()) / 2 - 2);

		int tx = sx + d + 8;
		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int top = h / 2 - 2;
		int bottom = h / 2 + small.getAscent() + 1;

		String right = locked > 0 ? "Opens at " + locked : pass ? "" : "+" + Format.amount(xp());
		int rw = small.stringWidth(right);
		g.setFont(Theme.body());
		String name = (locked > 0 ? "Room " + room.getNumber() : stop == null ? "" : work(stop));
		Color nameColor = locked > 0 ? Theme.TEXT_FAINT : pass ? Theme.TEXT_MUTED : Theme.TEXT;
		if (body.stringWidth(name) > w - tx - rw - 12)
		{
			// "13 urns + chest + sarc" beside its experience steps down a size before it is cut.
			g.setFont(Theme.small());
			Ui.shadowed(g, Ui.clip(name, small, w - tx - rw - 12), tx, top, nameColor);
		}
		else
		{
			Ui.shadowed(g, name, tx, top, nameColor);
		}
		g.setFont(Theme.small());
		boolean opening = stop != null && (stop.isSarcophagus() || stop.isChest());
		String odds = opening ? "1/" + room.getSceptreOneIn() : "";
		int oddsW = opening ? small.stringWidth(odds) + 8 : 0;
		String sub = locked > 0 ? room.getUrnXp() + " xp an urn" : here
			? (stop.getUrnsLeft() < room.getUrns() ? "Here, " + (room.getUrns() - stop.getUrnsLeft()) + " searched" : "You are here")
			: "Level " + room.getLevel();
		g.setColor(here ? Theme.TEAL : Theme.TEXT_FAINT);
		g.drawString(Ui.clip(sub, small, w - tx - oddsW - 6), tx, bottom);
		if (!right.isEmpty())
		{
			g.setColor(locked > 0 ? Theme.TEXT_FAINT : Theme.GOLD);
			g.drawString(right, w - rw - 6, top);
		}
		if (opening)
		{
			g.setColor(Theme.alpha(Theme.TEAL, 200));
			g.drawString(odds, w - small.stringWidth(odds) - 6, bottom);
		}
		g.dispose();
	}
}
