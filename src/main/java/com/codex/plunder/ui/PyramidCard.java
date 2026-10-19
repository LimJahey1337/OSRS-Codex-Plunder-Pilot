package com.codex.plunder.ui;

import com.codex.plunder.core.NavigatorGoals;
import com.codex.plunder.core.Plan;
import com.codex.plunder.core.Planner;
import com.codex.plunder.core.Room;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/**
 * The pyramid itself: eight tiers, one a room, room 1 at the base and the Pharaoh's room under the
 * capstone, each beside the level that opens it and the experience an hour a run to it pays. The
 * rooms open to the player are sandstone, the rest dark; their own Thieving level and the levels
 * Codex Navigator's goals need are marked up its side, so the climb still ahead can be seen.
 */
class PyramidCard extends JComponent
{
	private static final int PAD = 12;
	private static final int TIER = 18;
	private static final int CAP = 12;
	private static final int TOP = 30;
	private static final int BASE_W = 86;
	/** Room for the level marks to the pyramid's left. */
	private static final int MARGIN = 30;
	private static final int LEVELS = 10;
	private static final int HEIGHT = TOP + CAP + Room.values().length * TIER + 34;

	/** One mark up the pyramid's side: the player's level, or a goal's. */
	static final class Mark
	{
		final int level;
		final String label;
		final boolean you;
		int y;

		Mark(int level, String label, boolean you)
		{
			this.level = level;
			this.label = label;
			this.you = you;
		}
	}

	private final List<Plan> ladder;
	private final int level;
	private final Room current;
	private final int sceptres;
	private final List<Mark> marks = new ArrayList<>();

	/**
	 * @param ladder a fresh run at each room's entry level, room 1 first
	 * @param current the room of the run in hand, or null outside a run
	 * @param goals the Thieving levels Navigator's goals need
	 */
	PyramidCard(List<Plan> ladder, int level, Room current, int sceptres, List<NavigatorGoals.Need> goals)
	{
		this.ladder = ladder;
		this.level = level;
		this.current = current;
		this.sceptres = sceptres;
		StringBuilder tip = new StringBuilder("<html>You: Thieving " + level);
		int[] levels = new int[goals.size()];
		for (int i = 0; i < levels.length; i++)
		{
			NavigatorGoals.Need n = goals.get(i);
			levels[i] = n.getLevel();
			if (n.getLevel() > level && n.getLevel() <= 99)
			{
				tip.append("<br>").append(n.getLevel()).append(": ").append(Ui.escape(n.getGoal()));
			}
		}
		marks.addAll(marks(level, levels));
		spread(marks, top(), base(), 11);
		setToolTipText(tip.append("</html>").toString());
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
		setMinimumSize(new Dimension(10, HEIGHT));
	}

	/** Most goal levels marked, the nearest first, so the marks always fit up the side. */
	static final int MOST_GOALS = 6;

	/**
	 * The marks up the side: the player's level, then each level a goal needs above it, once however
	 * many goals share it, and only the nearest few.
	 */
	static List<Mark> marks(int level, int[] goals)
	{
		List<Mark> out = new ArrayList<>();
		out.add(new Mark(level, String.valueOf(level), true));
		java.util.TreeSet<Integer> above = new java.util.TreeSet<>();
		for (int g : goals)
		{
			if (g > level && g <= 99)
			{
				above.add(g);
			}
		}
		for (int g : above)
		{
			if (out.size() > MOST_GOALS)
			{
				break;
			}
			out.add(new Mark(g, String.valueOf(g), false));
		}
		return out;
	}

	private static int top()
	{
		return TOP + CAP;
	}

	private static int base()
	{
		return TOP + CAP + Room.values().length * TIER;
	}

	/**
	 * Where a Thieving level sits up the pyramid: the base is 21, where the pyramid opens, and each
	 * tier spans the ten levels to the next room, the top one 91 to 99.
	 */
	static int levelY(int level, int top, int base)
	{
		int tiers = Room.values().length;
		double tier = (Math.max(Room.ENTRY_LEVEL, Math.min(99, level)) - Room.ENTRY_LEVEL) / (double) LEVELS;
		if (tier > tiers - 1)
		{
			// The top tier is 91 to 99, eight levels rather than ten.
			tier = tiers - 1 + (tier - (tiers - 1)) * LEVELS / 8.0;
		}
		return (int) Math.round(base - Math.min(tiers, tier) * (base - top) / tiers);
	}

	/**
	 * Places the marks at their levels, then nudges any too close apart so no two labels overlap,
	 * keeping them in level order and inside the pyramid's height.
	 */
	static void spread(List<Mark> marks, int top, int base, int gap)
	{
		marks.sort((a, b) -> Integer.compare(a.level, b.level));
		for (Mark m : marks)
		{
			m.y = levelY(m.level, top, base);
		}
		// Upwards from the lowest: each sits at least a gap above the one below.
		for (int i = 1; i < marks.size(); i++)
		{
			marks.get(i).y = Math.min(marks.get(i).y, marks.get(i - 1).y - gap);
		}
		// Then back down from the top, if that pushed any past it.
		if (!marks.isEmpty() && marks.get(marks.size() - 1).y < top)
		{
			marks.get(marks.size() - 1).y = top;
			for (int i = marks.size() - 2; i >= 0; i--)
			{
				marks.get(i).y = Math.max(marks.get(i).y, marks.get(i + 1).y + gap);
			}
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, HEIGHT - 1, Theme.RADIUS, Theme.RADIUS);
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, HEIGHT, Theme.CARD));
		g.fill(shape);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(shape);

		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		Ui.shadowed(g, "The pyramid", PAD, 8 + body.getAscent(), Theme.GOLD);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String head = "xp an hour";
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(head, w - PAD - small.stringWidth(head), 8 + body.getAscent());

		int top = top();
		int base = base();
		float cx = PAD + MARGIN + BASE_W / 2f;
		Room mine = level >= Room.ENTRY_LEVEL ? Room.highest(level) : null;
		Room[] rooms = Room.values();
		int labelX = (int) (cx + BASE_W / 2f) + 10;

		// The capstone: gilt once a sceptre has come out of the pyramid.
		float capHalf = half(top, top - CAP, base);
		Path2D cap = new Path2D.Float();
		cap.moveTo(cx, top - CAP);
		cap.lineTo(cx + capHalf + 3, top - 1);
		cap.lineTo(cx - capHalf - 3, top - 1);
		cap.closePath();
		g.setPaint(sceptres > 0 ? new GradientPaint(cx - 6, top - CAP, Theme.GOLD_BRIGHT, cx + 6, top, Theme.GOLD)
			: new GradientPaint(cx - 6, top - CAP, Theme.STROKE, cx + 6, top, Theme.INK_DEEP));
		g.fill(cap);
		if (sceptres > 0)
		{
			String found = sceptres == 1 ? "1 sceptre" : sceptres + " sceptres";
			Ui.shadowed(g, found, labelX, top - 2, Theme.TEAL);
		}

		for (Room room : rooms)
		{
			int i = room.ordinal();
			int yb = base - i * TIER;
			int yt = yb - TIER + 1;
			float hb = half(yb, top - CAP, base);
			float ht = half(yt, top - CAP, base);
			Path2D tier = new Path2D.Float();
			tier.moveTo(cx - hb, yb);
			tier.lineTo(cx + hb, yb);
			tier.lineTo(cx + ht, yt);
			tier.lineTo(cx - ht, yt);
			tier.closePath();
			boolean open = mine != null && room.ordinal() <= mine.ordinal();
			boolean here = room == current;
			Color face = here ? Theme.TEAL : open ? Theme.mix(Theme.GOLD_DIM, Theme.GOLD, 0.25 + 0.6 * i / rooms.length) : Theme.mix(Theme.INK_DEEP, Theme.STROKE, 0.6);
			// Lit from the left, as the sun falls on the desert.
			g.setPaint(new GradientPaint(cx - hb, 0, Theme.mix(face, Color.WHITE, open || here ? 0.12 : 0), cx + hb, 0, Theme.mix(face, Theme.INK_DEEP, 0.45)));
			g.fill(tier);
			g.setColor(room == mine && !here ? Theme.GOLD_BRIGHT : Theme.alpha(Theme.INK_DEEP, 200));
			g.draw(tier);
			// The room's number on its face.
			String n = String.valueOf(room.getNumber());
			g.setColor(open || here ? Theme.alpha(Theme.INK_DEEP, 220) : Theme.TEXT_FAINT);
			g.drawString(n, Math.round(cx - small.stringWidth(n) / 2f), yb - (TIER - small.getAscent()) / 2 - 1);

			// Beside it: the level that opens it and what a run to it pays.
			int mid = yb - TIER / 2 + small.getAscent() / 2 - 1;
			Plan p = i < ladder.size() ? ladder.get(i) : null;
			String rate = p == null ? "" : Format.amount(p.xpPerHour(Planner.RESTART_TICKS));
			Color c = here ? Theme.TEAL : room == mine ? Theme.GOLD_BRIGHT : open ? Theme.GOLD : Theme.TEXT_FAINT;
			g.setColor(open || here ? Theme.TEXT_MUTED : Theme.TEXT_FAINT);
			g.drawString(String.valueOf(room.getLevel()), labelX, mid);
			Ui.shadowed(g, rate, w - PAD - small.stringWidth(rate), mid, c);
			// A faint course line from the tier to its figures.
			g.setColor(Theme.alpha(c, 40));
			g.drawLine(Math.round(cx + hb) + 3, yb - TIER / 2, labelX - 3, yb - TIER / 2);
		}

		// The player's level and the goals' levels, up the left side.
		for (Mark m : marks)
		{
			int ly = levelY(m.level, top, base);
			float hx = cx - half(ly, top - CAP, base);
			Color c = m.you ? Theme.GOLD_BRIGHT : Theme.TEAL;
			if (!m.you)
			{
				// The goal's level, dashed across the climb still to make.
				g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{2f, 2f}, 0f));
				g.setColor(Theme.alpha(Theme.TEAL, 150));
				g.drawLine(Math.round(hx), ly, Math.round(cx + half(ly, top - CAP, base)), ly);
				g.setStroke(new BasicStroke(1f));
			}
			// A pointer from the label to the slope.
			Path2D tip = new Path2D.Float();
			tip.moveTo(hx - 2, ly);
			tip.lineTo(hx - 7, ly - 3);
			tip.lineTo(hx - 7, ly + 3);
			tip.closePath();
			g.setColor(c);
			g.fill(tip);
			g.setColor(Theme.alpha(c, 110));
			g.drawLine(PAD + small.stringWidth(m.label) + 3, m.y, Math.round(hx) - 7, ly);
			Ui.shadowed(g, m.label, PAD, m.y + small.getAscent() / 2, c);
		}

		g.setColor(Theme.TEXT_FAINT);
		String foot = marks.size() > 1 ? "Gold: you. Teal: Navigator goals." : "Your level is marked in gold.";
		g.drawString(Ui.clip(foot, small, w - PAD * 2), PAD, HEIGHT - 10);
		g.dispose();
	}

	/** Half the pyramid's width at a height, from nothing at the apex to half the base. */
	private static float half(float y, float apex, float base)
	{
		return Math.max(0, (y - apex) / (base - apex)) * BASE_W / 2f;
	}
}
