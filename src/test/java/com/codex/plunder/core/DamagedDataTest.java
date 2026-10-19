package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

/**
 * Every saved value Plunder Pilot reads survives damage: its own {@code logbook}, Bank Steward's
 * {@code bankV1}, Gear Roadmap's {@code bank} and Navigator's {@code goalBoard}. Whatever the
 * profile holds, nothing throws, the plan and records still come out sensible, and damage to one
 * part does not cost the rest.
 */
public class DamagedDataTest
{
	private static final Gson GSON = new Gson();
	private static final long NOW = 1_760_000_000_000L;
	static final Set<Integer> ARTEFACTS = new HashSet<>(Arrays.asList(9026, 9032, 9036, 9042, 9030, 9038, 9040, 9028, 9034));
		private static final String REAL_BOARD = "{\"version\":1,\"focus\":\"skill:Thieving 70\",\"goals\":[{\"key\":\"skill:Thieving 70\",\"type\":\"skill\","
		+ "\"title\":\"70 Thieving\",\"complete\":false,\"progress\":0.17,\"next\":\"Train Thieving to 70\",\"hoursLeft\":5.0,"
		+ "\"skills\":{\"Thieving\":70},\"quests\":[]},{\"key\":\"diary:Ardougne Hard\",\"type\":\"diary\",\"title\":\"Ardougne Hard Diary\","
		+ "\"complete\":false,\"progress\":0.31,\"next\":\"Start Shield of Arrav\",\"hoursLeft\":59.5,\"skills\":{\"Cooking\":53,"
		+ "\"Fishing\":53,\"Hunter\":59,\"Ranged\":60,\"Farming\":65,\"Smithing\":68,\"Magic\":66,\"Thieving\":72,\"Agility\":65,"
		+ "\"Runecraft\":65},\"quests\":[\"The Grand Tree\",\"Tree Gnome Village\",\"Monkey Madness I\",\"Family Crest\","
		+ "\"Shield of Arrav\",\"Lost City\",\"Merlin\\u0027s Crystal\",\"Dragon Slayer I\",\"Heroes\\u0027 Quest\","
		+ "\"Druidic Ritual\",\"Jungle Potion\",\"Shilo Village\",\"Plague City\",\"Biohazard\",\"Underground Pass\","
		+ "\"Waterfall Quest\",\"Legends\\u0027 Quest\",\"Regicide\",\"Roving Elves\",\"Big Chompy Bird Hunting\","
		+ "\"Sheep Herder\",\"Mourning\\u0027s End Part I\",\"Watchtower\"]},{\"key\":\"quest:Dragon Slayer II\","
		+ "\"type\":\"quest\",\"title\":\"Dragon Slayer II\",\"complete\":false,\"progress\":0.28,\"next\":\"Start Shield of Arrav\","
		+ "\"hoursLeft\":65.0,\"skills\":{\"Cooking\":53,\"Woodcutting\":55,\"Thieving\":60,\"Crafting\":62,\"Agility\":60,"
		+ "\"Smithing\":70,\"Mining\":68,\"Magic\":75,\"Fishing\":53},\"quests\":[\"Family Crest\",\"Shield of Arrav\","
		+ "\"Lost City\",\"Merlin\\u0027s Crystal\",\"Dragon Slayer I\",\"Heroes\\u0027 Quest\",\"Druidic Ritual\","
		+ "\"Jungle Potion\",\"Shilo Village\",\"Plague City\",\"Biohazard\",\"Underground Pass\",\"Waterfall Quest\","
		+ "\"Legends\\u0027 Quest\",\"The Fremennik Trials\",\"Rune Mysteries\",\"Lunar Diplomacy\",\"Death Plateau\","
		+ "\"Troll Stronghold\",\"Eadgar\\u0027s Ruse\",\"Dream Mentor\",\"Gertrude\\u0027s Cat\",\"Icthlarin\\u0027s Little Helper\","
		+ "\"A Tail of Two Cats\",\"The Restless Ghost\",\"Ernest the Chicken\",\"Priest in Peril\",\"Animal Magnetism\","
		+ "\"Ghosts Ahoy\",\"The Dig Site\",\"Bone Voyage\",\"X Marks the Spot\",\"Client of Kourend\",\"Dragon Slayer II\"]},"
		+ "{\"key\":\"skill:Attack 70\",\"type\":\"skill\",\"title\":\"70 Attack\",\"complete\":false,\"progress\":0.17,"
		+ "\"next\":\"Train Attack to 70\",\"hoursLeft\":11.0,\"skills\":{\"Attack\":70},\"quests\":[]},{\"key\":\"diary:Western Provinces Hard\","
		+ "\"type\":\"diary\",\"title\":\"Western Provinces Hard\",\"complete\":false,\"progress\":0.0,\"next\":\"Western Provinces Hard\","
		+ "\"hoursLeft\":0.0,\"skills\":{},\"quests\":[]}]}";

	/** An account with runs behind it: the pace learned, rooms opened and a sceptre found. */
	public static Logbook good(int level, int count)
	{
		Random rnd = new Random(11);
		Logbook book = new Logbook();
		book.seen(level, Levels.xpFor(level) + 12_000);
		Pace pace = book.getPace();
		long t = NOW - count * 400_000L;
		for (int i = 0; i < count; i++)
		{
			RunTracker tr = new RunTracker();
			Goal goal = i % 9 == 4 ? Goal.SCEPTRE : Goal.EXPERIENCE;
			tr.begin(t, 0, goal);
			Plan plan = Planner.fresh(level, goal, pace);
			int tick = 0;
			for (Plan.Stop s : plan.getStops())
			{
				tick += 20 + rnd.nextInt(10);
				tr.room(s.getRoom(), tick, pace);
				tr.thieving(Room.TRAP_XP);
				for (int u = 0; u < s.getUrns(); u++)
				{
					tick += 8 + rnd.nextInt(5);
					tr.urnSearched(tick, pace);
					tr.thieving(s.getRoom().getUrnXp());
					if (rnd.nextInt(4) == 0)
					{
						tr.artefacts(1, 180);
					}
				}
				if (s.isChest())
				{
					tick += 6;
					tr.chestOpened(tick);
					tr.thieving(s.getRoom().getChestXp());
				}
				if (s.isSarcophagus())
				{
					tick += 12;
					tr.sarcophagusOpened(tick);
					tr.strength(s.getRoom().getSarcophagusXp());
				}
			}
			if (i == count - 2)
			{
				tr.sceptre();
			}
			long[] opened = tr.getOpened();
			Run run = tr.finish(Math.min(500, tick + 10));
			book.record(run, opened);
			t += 400_000L;
		}
		book.getSceptre().heard(3);
		book.setBanked(java.util.Collections.singletonMap(9026, 4L), NOW - 60_000);
		return book;
	}

	@Test
	public void everyDamagedLogbookReadsAndPlans()
	{
		String good = GSON.toJson(good(71, 6));
		int tried = 0;
		for (String damaged : Damaged.of(good))
		{
			try
			{
				use(Logbook.read(GSON, damaged));
			}
			catch (RuntimeException | AssertionError | StackOverflowError e)
			{
				throw new AssertionError("logbook " + Damaged.show(damaged) + ": " + e, e);
			}
			tried++;
		}
		assertTrue(tried > 300);
	}

	/** Everything the plugin works out from a logbook, each figure checked for sense. */
	public static void use(Logbook b)
	{
		assertNotNull(b);
		int level = b.getLevel();
		assertTrue("level " + level, level >= 0 && level <= 126);
		assertTrue(b.getXp() >= 0);
		assertNotNull(b.getGoal());
		Pace pace = b.getPace();
		for (long o : b.getOpened())
		{
			assertTrue(o >= 0);
		}
		double odds = Odds.atLeastOne(b.getOpened());
		assertTrue(odds >= 0 && odds <= 1);
		assertTrue(Odds.expected(b.getOpened()) >= 0);
		Odds.luck(b.getOpened(), b.getSceptres());
		assertTrue(b.getSceptres() >= 0 && b.getTotalRuns() >= 0 && b.getTotalXp() >= 0);
		assertTrue(b.recentRate(10) >= 0);
		Room top = Room.highest(Math.max(Room.ENTRY_LEVEL, level));
		for (Goal goal : Goal.values())
		{
			Plan fresh = Planner.fresh(level, goal, pace);
			if (fresh != null)
			{
				assertTrue(b.slotsNeeded(fresh, 10) >= 0);
				Step.next(fresh, false, false, 0);
				Planner.plan(level, goal, pace, new Here(Room.values()[0], 3, false, false, true), 400);
			}
			for (Room r : Room.values())
			{
				Planner.fresh(r.getLevel(), goal, pace);
			}
			Splits.of(b.getRuns(), goal, top).sumOfBest();
		}
		Session.best(Session.history(b.getRuns(), NOW));
		Session.latest(b.getRuns(), NOW);
		Outlook.of(Session.history(b.getRuns(), NOW), NOW);
		Unlock.runsToNext(level, b.getXp(), 1_000);
		Unlock.opened(Math.max(1, level - 1), level, b.getGoal(), pace);
		b.getSceptre().charges(0);
		b.getSceptre().getBanked();
		Stash.pick(b.getBanked(), b.getBankedAt(), null, ARTEFACTS);
		assertTrue(b.getRuns().size() <= Logbook.KEEP);
		for (Run r : b.getRuns())
		{
			assertTrue(r.getTicks() >= 0 && r.getThievingXp() >= 0 && r.getArtefacts() >= 0);
			assertNotNull(r.getGoal());
		}
		// Another run recorded, and the logbook saved and read back, still work.
		Logbook back = Logbook.read(GSON, GSON.toJson(b));
		assertEquals(b.getRuns().size(), back.getRuns().size());
	}

	@Test
	public void oneDamagedFieldCostsOnlyThatField()
	{
		Logbook was = good(71, 6);
		String good = GSON.toJson(was);
		Logbook b = Logbook.read(GSON, good.replace("\"goal\":\"EXPERIENCE\",\"level\"", "\"goal\":\"NOT_A_GOAL\",\"level\""));
		assertEquals(6, b.getRuns().size());
		assertEquals(71, b.getLevel());
		b = Logbook.read(GSON, good.replace("\"level\":71", "\"level\":\"seventy-one\""));
		assertEquals(6, b.getRuns().size());
		assertEquals(was.getTotalXp(), b.getTotalXp());
		assertEquals(1, b.getSceptres());
	}

	@Test
	public void numbersOutOfRangeAreBroughtBack()
	{
		String good = GSON.toJson(good(71, 6));
		Logbook b = Logbook.read(GSON, good.replace("\"level\":71", "\"level\":2000000"));
		assertTrue(b.getLevel() <= 99);
		b = Logbook.read(GSON, good.replace("\"level\":71", "\"level\":-5"));
		assertEquals(0, b.getLevel());
		b = Logbook.read(GSON, good.replace("\"sceptres\":1", "\"sceptres\":-3"));
		assertEquals(0, b.getSceptres());
	}

	@Test
	public void aHugeRunHistoryIsCutToWhatIsKeptAndStaysQuick()
	{
		// Ten thousand runs saved in full, as a damaged or hand-edited save could hold.
		Logbook b = good(91, 30);
		String run = GSON.toJson(b.getRuns().get(3));
		StringBuilder many = new StringBuilder("[");
		for (int i = 0; i < 10_000; i++)
		{
			many.append(i == 0 ? "" : ",").append(run);
		}
		String json = GSON.toJson(b);
		int at = json.indexOf("\"runs\":[") + "\"runs\":".length();
		int end = json.indexOf("],\"opened\"", at) + 1;
		long t0 = System.nanoTime();
		Logbook big = Logbook.read(GSON, json.substring(0, at) + many.append("]") + json.substring(end));
		long readMs = (System.nanoTime() - t0) / 1_000_000;
		assertEquals(Logbook.KEEP, big.getRuns().size());
		use(big);
		Logbook full = good(91, Logbook.KEEP);
		Runnable publish = () ->
		{
			// What the plugin works out on the client thread to draw the panel.
			Room top = Room.highest(91);
			Splits.of(full.getRuns(), Goal.EXPERIENCE, top);
			Session.history(full.getRuns(), NOW);
			Session.latest(full.getRuns(), NOW);
			full.recentRate(10);
			Plan fresh = Planner.fresh(91, Goal.EXPERIENCE, full.getPace());
			for (Room r : Room.values())
			{
				Planner.fresh(r.getLevel(), Goal.EXPERIENCE, full.getPace());
			}
			full.slotsNeeded(fresh, 10);
		};
		Runnable tick = () -> Planner.plan(91, Goal.EXPERIENCE, full.getPace(), new Here(Room.values()[5], 3, false, false, true), 260);
		double publishMs = time(publish);
		double tickMs = time(tick);
		System.out.printf("Plunder Pilot, %d runs kept: read 10,000 in %d ms, publish %.2f ms, plan a tick %.3f ms%n",
			Logbook.KEEP, readMs, publishMs, tickMs);
		assertTrue("publish took " + publishMs, publishMs < 25);
		assertTrue("a tick's plan took " + tickMs, tickMs < 5);
	}

	static double time(Runnable run)
	{
		for (int i = 0; i < 20; i++)
		{
			run.run();
		}
		int runs = 50;
		long t0 = System.nanoTime();
		for (int i = 0; i < runs; i++)
		{
			run.run();
		}
		return (System.nanoTime() - t0) / 1e6 / runs;
	}

	@Test
	public void everyDamagedSharedBankReadsSafely() throws Exception
	{
		String steward = SharedBankContractTest.fixture("bank-steward-bankV1.json");
		String roadmap = SharedBankContractTest.fixture("gear-roadmap-bank.json");
		for (String damaged : Damaged.of(steward))
		{
			bank(damaged, null);
			bank(damaged, roadmap);
		}
		for (String damaged : Damaged.of(roadmap))
		{
			bank(null, damaged);
		}
	}

	private static void bank(String steward, String roadmap)
	{
		try
		{
			SharedBank bank = SharedBank.read(GSON, steward, roadmap);
			if (bank != null)
			{
				assertTrue(bank.getSavedAt() >= 0);
				for (Map.Entry<Integer, Long> e : bank.getItems().entrySet())
				{
					assertTrue(e.getKey() > 0 && e.getValue() > 0);
				}
				Stash s = Stash.pick(java.util.Collections.emptyMap(), 0, bank, ARTEFACTS);
				assertTrue(s == null || s.count() >= 0);
			}
		}
		catch (RuntimeException | AssertionError | StackOverflowError e)
		{
			throw new AssertionError("steward " + Damaged.show(steward) + " roadmap " + Damaged.show(roadmap) + ": " + e, e);
		}
	}

	@Test
	public void everyDamagedGoalBoardReadsSafely()
	{
		for (String damaged : Damaged.of(REAL_BOARD))
		{
			try
			{
				for (int level : new int[]{1, 52, 99})
				{
					List<NavigatorGoals.Need> needs = NavigatorGoals.needs(GSON, damaged, "Thieving", level);
					assertNotNull(needs);
					for (NavigatorGoals.Need n : needs)
					{
						assertTrue(n.getLevel() > level && n.getLevel() <= 99);
					}
				}
			}
			catch (RuntimeException | AssertionError | StackOverflowError e)
			{
				throw new AssertionError("goalBoard " + Damaged.show(damaged) + ": " + e, e);
			}
		}
	}
}
