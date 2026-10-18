package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PlannerTest
{
	private final Pace pace = new Pace();

	@Test
	public void roomsOpenEveryTenLevels()
	{
		assertNull(Room.highest(20));
		assertEquals(Room.ONE, Room.highest(21));
		assertEquals(Room.FOUR, Room.highest(60));
		assertEquals(Room.EIGHT, Room.highest(99));
		assertEquals(61, Room.nextUnlock(55));
		assertEquals(0, Room.nextUnlock(91));
	}

	@Test
	public void deepRoomsGetTheUrnsAndShallowRoomsArePassed()
	{
		Plan plan = Planner.fresh(91, Goal.EXPERIENCE, pace);
		assertEquals(Room.EIGHT, plan.getTop());
		assertEquals(8, plan.getStops().size());
		assertEquals(13, plan.stop(Room.EIGHT).getUrns());
		assertEquals(13, plan.stop(Room.SEVEN).getUrns());
		assertTrue(plan.stop(Room.ONE).isPassThrough());
		assertTrue(plan.stop(Room.TWO).isPassThrough());
		// Chests from room four on are cheap and always taken.
		for (Room r : new Room[]{Room.FOUR, Room.FIVE, Room.SIX, Room.SEVEN, Room.EIGHT})
		{
			assertTrue(r.name(), plan.stop(r).isChest());
		}
		assertFalse(plan.stop(Room.EIGHT).isSarcophagus());
		assertTrue(plan.getTicksNeeded() <= Planner.RUN_TICKS);
		// Roughly the quarter million an hour the wiki gives for 91.
		int rate = plan.xpPerHour(Planner.RESTART_TICKS);
		assertTrue("rate " + rate, rate > 200_000 && rate < 320_000);
	}

	@Test
	public void lowLevelsLootEverythingAndLeaveEarly()
	{
		Plan plan = Planner.fresh(25, Goal.EXPERIENCE, pace);
		assertEquals(1, plan.getStops().size());
		Plan.Stop only = plan.current();
		assertEquals(13, only.getUrns());
		assertTrue(only.isChest());
		assertTrue(plan.spare() > 100);
	}

	@Test
	public void sceptreHuntersOpenChestsAndSarcophagiFirst()
	{
		Plan plan = Planner.fresh(91, Goal.SCEPTRE, pace);
		for (Room r : Room.values())
		{
			assertTrue(r.name(), plan.stop(r).isChest());
			assertEquals(r.name(), r.getNumber() >= 4, plan.stop(r).isSarcophagus());
		}
		Plan xp = Planner.fresh(91, Goal.EXPERIENCE, pace);
		assertTrue(plan.getSceptreChance() > xp.getSceptreChance());
		// The wiki's figure for every chest and the deep sarcophagi at 91 is about 1 in 69.
		assertEquals(1 / 69.0, plan.getSceptreChance(), 0.002);
	}

	@Test
	public void aSlowRunShortensWhatFollows()
	{
		Here inSeven = new Here(Room.SEVEN, 13, false, false, true);
		Plan plenty = Planner.plan(91, Goal.EXPERIENCE, pace, inSeven, 300);
		Plan short_ = Planner.plan(91, Goal.EXPERIENCE, pace, inSeven, 120);
		assertEquals(13, plenty.stop(Room.EIGHT).getUrns());
		int urns = short_.stop(Room.SEVEN).getUrns() + short_.stop(Room.EIGHT).getUrns();
		assertTrue(urns < 13);
		// Room eight's urns pay more, so they are kept over room seven's.
		assertTrue(short_.stop(Room.EIGHT).getUrns() >= short_.stop(Room.SEVEN).getUrns());
		assertTrue(short_.getTicksNeeded() <= 120);
	}

	@Test
	public void searchedUrnsAreNotPlannedAgain()
	{
		Here half = new Here(Room.EIGHT, 5, true, false, true);
		Plan plan = Planner.plan(95, Goal.EXPERIENCE, pace, half, 400);
		assertEquals(5, plan.current().getUrns());
		assertFalse(plan.current().isChest());
		assertTrue(plan.current().isChestOpen());
	}

	@Test
	public void oddsAddUpAcrossRooms()
	{
		long[] opened = new long[8];
		opened[7] = 650;
		assertEquals(1.0, Odds.expected(opened), 1e-9);
		assertEquals(1 - Math.pow(1 - 1 / 650.0, 650), Odds.atLeastOne(opened), 1e-9);
		assertEquals(Math.exp(-1) / 2, Odds.luck(opened, 0), 1e-9);
		assertTrue(Odds.luck(opened, 2) > 0.8);
		assertEquals(451, Odds.runsFor(1 / 650.0, 0.5));
	}

	@Test
	public void paceLearnsFromRuns()
	{
		Pace p = new Pace();
		for (int i = 0; i < Pace.TRUSTED; i++)
		{
			p.urnTook(7);
		}
		assertEquals(7, p.urn(), 1e-9);
		p.urnTook(500);
		assertEquals(7, p.urn(), 1e-9);
	}

	@Test
	public void aRunIsFollowedRoomByRoom()
	{
		Pace p = new Pace();
		RunTracker t = new RunTracker();
		t.begin(0, 0, Goal.EXPERIENCE);
		t.room(Room.SEVEN, 0, p);
		t.urnSearched(10, p);
		t.urnSearched(18, p);
		t.chestOpened(25);
		t.thieving(675 * 2 + 450);
		t.room(Room.EIGHT, 45, p);
		t.sarcophagusOpened(60);
		t.strength(275);
		Run run = t.finish(100);
		assertEquals(8, run.getDeepest());
		assertEquals(2, run.getUrns());
		assertEquals(1, run.getChests());
		assertEquals(1, run.getSarcophagi());
		assertEquals(1 - (1 - 1 / 650.0) * (1 - 1 / 650.0), run.getSceptreChance(), 1e-9);
		assertEquals(1, p.getUrnSamples());
		assertEquals(1, p.getPassSamples());

		Logbook book = new Logbook();
		book.record(run, t.getOpened());
		assertEquals(2, book.openedTotal());
		assertEquals(1, book.getTotalRuns());
	}

	@Test
	public void theExperienceTableMatchesTheGame()
	{
		assertEquals(0, Levels.xpFor(1));
		assertEquals(83, Levels.xpFor(2));
		assertEquals(13_034_431, Levels.xpFor(99));
		assertEquals(91, Levels.levelFor(5_902_831));
	}
}
