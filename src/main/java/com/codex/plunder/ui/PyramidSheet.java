package com.codex.plunder.ui;

import com.codex.plunder.core.Planner;
import com.codex.plunder.core.Room;
import com.codex.plunder.core.Session;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The pyramid card: the player's climb drawn as the panel's pyramid, beside what their runs earn,
 * in the panel's ink and gold. Made to be pasted into a chat to show a friend how far up the
 * Thieving pyramid they are, so it carries its own heading and needs nothing around it. Nothing on
 * it is an item picture, so it is drawn whole the moment it is asked for.
 */
public final class PyramidSheet
{
	static final int WIDTH = 330;
	private static final int PAD = 14;
	private static final int HEADER = 66;
	/** The pyramid's own width on the card; the figures take the rest. */
	private static final int PYRAMID_W = 196;

	private PyramidSheet()
	{
	}

	/** One figure in the column beside the pyramid. */
	static final class Fact
	{
		final String label;
		final String value;
		final String note;
		final Color color;

		Fact(String label, String value, String note, Color color)
		{
			this.label = label;
			this.value = value;
			this.note = note;
			this.color = color;
		}
	}

	/**
	 * The four figures beside the pyramid, most telling first: the experience an hour, the runs
	 * made, the sceptre, and the best session or, before there is one, the next room to open.
	 * A first look with no runs yet still fills all four, from the forecast.
	 */
	static List<Fact> facts(View view)
	{
		List<Fact> out = new ArrayList<>();
		View.Record r = view.record;
		int forecast = view.fresh.xpPerHour(Planner.RESTART_TICKS);
		boolean measured = r.recentRate > 0;
		out.add(new Fact("Xp an hour", Format.amount(measured ? r.recentRate : forecast), measured ? "your last runs" : "forecast", Theme.GOLD));
		out.add(new Fact("Runs", Format.amount(r.totalRuns), r.totalRuns == 0 ? "none logged yet"
			: Format.amount(r.totalXp) + " xp", Theme.TEAL));
		double chance = view.fresh.getSceptreChance();
		if (r.sceptres > 0)
		{
			out.add(new Fact("Sceptres", String.valueOf(r.sceptres), r.sceptres == 1 ? "found" : "found so far", Theme.TEAL));
		}
		else
		{
			out.add(new Fact("Sceptre", chance > 0 ? Format.oneIn(chance) : "-", "chance a run", Theme.GOLD));
		}
		Session best = Session.best(r.history);
		int unlock = Room.nextUnlock(view.level);
		if (best != null)
		{
			out.add(new Fact("Best session", Format.amount(best.xpPerHour()), "xp an hour, " + Format.shortDay(best.getStartedAt()), Theme.GOLD));
		}
		else if (unlock > 0)
		{
			out.add(new Fact("Next room", "Room " + Room.highest(unlock).getNumber(), "opens at " + unlock, Theme.TEAL));
		}
		else
		{
			out.add(new Fact("Rooms", "All 8", "open to you", Theme.GOLD));
		}
		return out;
	}

	/** The card's heading: whose climb it is, or the plain kicker when names are left off. */
	static String kicker(String player)
	{
		return player == null ? "PYRAMID PLUNDER" : player.toUpperCase(Locale.ENGLISH) + "'S PYRAMID";
	}

	/** Draws the card. */
	public static BufferedImage render(View view)
	{
		List<Fact> facts = facts(view);
		PyramidCard pyramid = new PyramidCard(view.ladder, view.level, null, view.record.sceptres, view.goals);
		int pyramidH = pyramid.getPreferredSize().height;
		int bodyY = HEADER + 12;
		int footY = bodyY + pyramidH + 12;
		int height = footY + 26;
		BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);

		// The parchment-dark ground with a gilt frame.
		RoundRectangle2D frame = new RoundRectangle2D.Float(0.5f, 0.5f, WIDTH - 1, height - 1, 14, 14);
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, height, Theme.INK_DEEP));
		g.fill(frame);
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.INK, Theme.GOLD_DIM, 0.3), 0, HEADER, Theme.alpha(Theme.INK, 0)));
		g.fill(new RoundRectangle2D.Float(1, 1, WIDTH - 2, HEADER, 13, 13));
		g.setStroke(new BasicStroke(1.5f));
		g.setPaint(new GradientPaint(0, 0, Theme.GOLD, 0, height, Theme.GOLD_DIM));
		g.draw(new RoundRectangle2D.Float(1, 1, WIDTH - 2.5f, height - 2.5f, 13, 13));
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD, 40));
		g.draw(new RoundRectangle2D.Float(4.5f, 4.5f, WIDTH - 9, height - 9, 10, 10));

		header(g, view.nameOnCard ? view.player : null, view.level);
		rule(g, HEADER + 2);

		// The pyramid, as the Runs page draws it.
		pyramid.setSize(PYRAMID_W, pyramidH);
		Graphics2D pg = (Graphics2D) g.create(PAD, bodyY, PYRAMID_W, pyramidH);
		pyramid.paint(pg);
		pg.dispose();

		// The figures beside it.
		int fx = PAD + PYRAMID_W + 12;
		int fw = WIDTH - fx - PAD;
		int each = pyramidH / facts.size();
		int fy = bodyY + 2;
		for (Fact f : facts)
		{
			fact(g, f, fx, fy, fw, each);
			fy += each;
		}

		// The foot: where it came from and when.
		rule(g, footY);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String left = view.goal.getLabel() + " runs";
		String right = "Codex Plunder Pilot  -  " + Format.shortDay(view.now);
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(left, small, WIDTH - PAD * 2 - small.stringWidth(right) - 10), PAD, footY + 16);
		g.setColor(Theme.GOLD_DIM);
		g.drawString(right, WIDTH - PAD - small.stringWidth(right), footY + 16);
		g.dispose();
		return image;
	}

	private static void header(Graphics2D g, String player, int level)
	{
		int md = 46;
		int mx = PAD;
		int my = (HEADER - md) / 2 + 3;
		g.setPaint(new RadialGradientPaint(mx + md / 2f, my + md / 3f, md * 0.7f, new float[]{0, 1},
			new Color[]{Theme.mix(Theme.CARD_TOP, Theme.GOLD_DIM, 0.35), Theme.INK_DEEP}));
		g.fill(new Ellipse2D.Float(mx, my, md, md));
		g.setStroke(new BasicStroke(2f));
		g.setPaint(new GradientPaint(mx, my, Theme.GOLD_BRIGHT, mx, my + md, Theme.GOLD_DIM));
		g.draw(new Ellipse2D.Float(mx + 1, my + 1, md - 2, md - 2));
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD, 70));
		g.draw(new Ellipse2D.Float(mx + 5, my + 5, md - 10, md - 10));
		Sigil.paint(g, mx + 10, my + 10, md - 20);

		int tx = mx + md + 12;
		int room = WIDTH - tx - PAD;
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(kicker(player), small, room), tx, my + 12);
		Crest.paintLarge(g, "THIEVING " + level, tx, my + 18, Theme.GOLD);
	}

	private static void fact(Graphics2D g, Fact f, int x, int y, int w, int h)
	{
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(f.label.toUpperCase(Locale.ENGLISH), small, w), x, y + small.getAscent());
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		Ui.shadowed(g, Ui.clip(f.value, bold, w), x, y + small.getHeight() + bold.getAscent() + 1, f.color);
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(f.note, small, w), x, y + small.getHeight() + bold.getHeight() + small.getAscent() + 1);
		// A hairline under each figure, fading out, to keep the column in step with the tiers.
		int ly = y + h - 6;
		g.setPaint(new GradientPaint(x, 0, Theme.alpha(Theme.GOLD, 70), x + w, 0, Theme.alpha(Theme.GOLD, 0)));
		g.drawLine(x, ly, x + w, ly);
	}

	private static void rule(Graphics2D g, int y)
	{
		g.setPaint(new GradientPaint(PAD, 0, Theme.alpha(Theme.GOLD, 0), WIDTH / 2f, 0, Theme.alpha(Theme.GOLD, 140), true));
		g.drawLine(PAD, y, WIDTH - PAD, y);
		Path2D diamond = new Path2D.Float();
		diamond.moveTo(WIDTH / 2f, y - 3);
		diamond.lineTo(WIDTH / 2f + 3, y);
		diamond.lineTo(WIDTH / 2f, y + 3);
		diamond.lineTo(WIDTH / 2f - 3, y);
		diamond.closePath();
		g.setColor(Theme.GOLD);
		g.fill(diamond);
	}

	/** The card as something the system clipboard can hold. */
	public static Transferable transferable(Image image)
	{
		return new Transferable()
		{
			@Override
			public DataFlavor[] getTransferDataFlavors()
			{
				return new DataFlavor[]{DataFlavor.imageFlavor};
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor)
			{
				return DataFlavor.imageFlavor.equals(flavor);
			}

			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
			{
				if (!isDataFlavorSupported(flavor))
				{
					throw new UnsupportedFlavorException(flavor);
				}
				return image;
			}
		};
	}
}
