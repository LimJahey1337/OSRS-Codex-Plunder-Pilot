package com.codex.plunder.ui;

import static org.junit.Assert.assertTrue;
import com.codex.plunder.core.Check;
import com.codex.plunder.core.DamagedDataTest;
import com.codex.plunder.core.Goal;
import com.codex.plunder.core.Logbook;
import com.codex.plunder.core.Plan;
import com.codex.plunder.core.Planner;
import com.codex.plunder.core.Room;
import com.codex.plunder.core.Run;
import com.codex.plunder.core.Session;
import com.codex.plunder.core.Splits;
import com.codex.plunder.core.Step;
import com.google.gson.Gson;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.Test;

/** A damaged logbook still draws: every tab of the panel and the pyramid card, without an exception. */
public class DamagedPanelTest
{
	private static final Gson GSON = new Gson();
	private static final long NOW = 1_760_000_000_000L;

	/** The view the plugin builds from a logbook, as when the panel is published. */
	static View view(Logbook b)
	{
		int level = b.getLevel();
		Goal goal = b.getGoal();
		Room top = Room.highest(Math.max(Room.ENTRY_LEVEL, level));
		List<Run> runs = b.getRuns();
		View.Record record = new View.Record(new ArrayList<>(runs), b.getOpened(), b.getSceptres(), b.getTotalRuns(),
			b.getTotalXp(), b.getTotalArtefacts(), b.recentRate(10), b.getPace().getUrnSamples())
			.splits(Splits.of(runs, goal, top), runs.isEmpty() ? null : runs.get(runs.size() - 1))
			.session(Session.latest(runs, NOW)).history(Session.history(runs, NOW));
		Plan fresh = Planner.fresh(level, goal, b.getPace());
		if (fresh == null)
		{
			return new View(View.State.TOO_LOW, "Zezima", level, b.getXp(), 99, goal, null, null, null, null, null, record, NOW);
		}
		List<Plan> ladder = new ArrayList<>();
		for (Room r : Room.values())
		{
			ladder.add(Planner.fresh(r.getLevel(), goal, b.getPace()));
		}
		View v = new View(View.State.READY, "Zezima", level, b.getXp(), 99, goal, fresh, fresh, ladder,
			Step.next(fresh, false, false, 0), null, record, NOW);
		Check.Kit kit = new Check.Kit(level, 70, 87, 24, false, true);
		return v.ready(new View.Ready(Check.ready(kit, fresh, b.slotsNeeded(fresh, 10)), 0, 0));
	}

	@Test
	public void everyDamagedLogbookDraws() throws Exception
	{
		String good = GSON.toJson(DamagedDataTest.good(71, 6));
		List<String> all = com.codex.plunder.core.DamagedAccess.of(good);
		int drawn = 0;
		for (int i = 0; i < all.size(); i += 4)
		{
			String damaged = all.get(i);
			View v = view(Logbook.read(GSON, damaged));
			try
			{
				draw(v);
				if (v.state == View.State.READY)
				{
					// The card is offered only once the pyramid is open to the player.
					PyramidSheet.render(v);
				}
			}
			catch (RuntimeException | AssertionError e)
			{
				throw new AssertionError("logbook " + com.codex.plunder.core.DamagedAccess.show(damaged) + ": " + e, e);
			}
			drawn++;
		}
		assertTrue(drawn > 50);
	}

	static void draw(View view) throws Exception
	{
		Throwable[] failed = new Throwable[1];
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				PilotPanel p = new PilotPanel(g -> { });
				p.update(view);
				for (int tab = 0; tab < 3; tab++)
				{
					p.showTab(tab);
					p.setSize(225, 1200);
					layout(p);
					BufferedImage img = new BufferedImage(225, 1200, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = img.createGraphics();
					p.printAll(g);
					g.dispose();
				}
			}
			catch (Throwable t)
			{
				failed[0] = t;
			}
		});
		if (failed[0] != null)
		{
			throw new RuntimeException(failed[0]);
		}
	}

	private static void layout(Component c)
	{
		c.doLayout();
		if (c instanceof Container)
		{
			for (Component k : ((Container) c).getComponents())
			{
				layout(k);
			}
		}
	}
}
