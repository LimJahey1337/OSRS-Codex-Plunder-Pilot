package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class SplitsTest
{
	/** A run to the given room, entering each room at the given ticks. */
	private static Run run(Goal goal, int... entered)
	{
		RunTracker t = new RunTracker();
		t.begin(0, 0, goal);
		for (int i = 0; i < entered.length; i++)
		{
			t.room(Room.values()[i], entered[i], null);
		}
		t.thieving(100);
		return t.finish(entered[entered.length - 1] + 50);
	}

	@Test
	public void theBestIsTheFastestToTheDeepestRoomAndEachRoomKeepsItsQuickestCrossing()
	{
		List<Run> runs = Arrays.asList(
			run(Goal.EXPERIENCE, 0, 20, 40, 70, 100),
			run(Goal.EXPERIENCE, 0, 18, 45, 75, 98),
			run(Goal.EXPERIENCE, 0, 25, 41, 66, 104));
		Splits s = Splits.of(runs, Goal.EXPERIENCE, Room.FIVE);
		assertEquals(3, s.getCompared());
		assertEquals(98, s.bestAt(Room.FIVE));
		assertEquals(45, s.bestAt(Room.THREE));
		// Room 1 quickest 18, room 2 16, room 3 25, room 4 23.
		assertEquals(18, s.bestSegment(Room.ONE));
		assertEquals(16, s.bestSegment(Room.TWO));
		assertEquals(25, s.bestSegment(Room.THREE));
		assertEquals(23, s.bestSegment(Room.FOUR));
		assertEquals(82, s.sumOfBest());
		assertEquals(Integer.valueOf(-3), s.delta(Room.THREE, 42));
	}

	@Test
	public void otherGoalsAndShorterRunsAreNotCompared()
	{
		List<Run> runs = Arrays.asList(
			run(Goal.SCEPTRE, 0, 10, 20, 30, 40),
			run(Goal.EXPERIENCE, 0, 10, 20, 30));
		Splits s = Splits.of(runs, Goal.EXPERIENCE, Room.FIVE);
		assertFalse(s.hasBest());
		assertEquals(0, s.getCompared());
		assertNull(s.delta(Room.FIVE, 90));
		assertEquals(-1, s.sumOfBest());
	}

	@Test
	public void aRunJoinedMidwayNeverSetsAFalseBest()
	{
		// The plugin started in room 3: rooms 1 and 2 were never seen, room 3 only when first seen.
		RunTracker t = new RunTracker();
		t.begin(0, 0, Goal.EXPERIENCE);
		t.room(Room.THREE, 60, null);
		t.room(Room.FOUR, 90, null);
		t.room(Room.FIVE, 120, null);
		t.thieving(10);
		Run late = t.finish(200);
		assertEquals(-1, late.splitAt(Room.ONE));
		assertEquals(-1, Splits.segment(late, Room.TWO));
		Splits s = Splits.of(Arrays.asList(run(Goal.EXPERIENCE, 0, 20, 40, 70, 100)), Goal.EXPERIENCE, Room.FIVE);
		assertFalse(s.isNewBest(late, Goal.EXPERIENCE));
	}

	@Test
	public void aNewBestIsOnlyANewBestAgainstTheRunsBefore()
	{
		List<Run> before = new ArrayList<>(Arrays.asList(run(Goal.EXPERIENCE, 0, 20, 40, 70, 100)));
		Run faster = run(Goal.EXPERIENCE, 0, 19, 38, 66, 95);
		Run slower = run(Goal.EXPERIENCE, 0, 21, 44, 72, 101);
		Splits s = Splits.of(before, Goal.EXPERIENCE, Room.FIVE);
		assertTrue(s.isNewBest(faster, Goal.EXPERIENCE));
		assertFalse(s.isNewBest(slower, Goal.EXPERIENCE));
	}

	@Test
	public void oldSavesWithoutSplitsAreSkipped()
	{
		Run old = new Run(0, 400, 5, 9000, 0, 10, 1, 0, 0, 0, 0.001, false, Goal.EXPERIENCE);
		assertEquals(-1, old.splitAt(Room.FIVE));
		assertFalse(Splits.comparable(old, Goal.EXPERIENCE, Room.FIVE));
		assertEquals(0, Splits.of(Arrays.asList(old), Goal.EXPERIENCE, Room.FIVE).getCompared());
	}

	@Test
	public void theSlowestRoomIsTheOneMostOverItsQuickest()
	{
		List<Run> runs = Arrays.asList(
			run(Goal.EXPERIENCE, 0, 20, 40, 70, 100),
			run(Goal.EXPERIENCE, 0, 18, 45, 75, 98));
		Splits s = Splits.of(runs, Goal.EXPERIENCE, Room.FIVE);
		// Quickest: 18, 20, 30, 23. This run: 19, 21, 42, 24.
		Run run = run(Goal.EXPERIENCE, 0, 19, 40, 82, 106);
		assertEquals(Room.THREE, s.slowest(run));
		assertEquals(12, s.overrun(run, Room.THREE));
		// Within three seconds everywhere names nothing.
		assertNull(s.slowest(run(Goal.EXPERIENCE, 0, 19, 40, 72, 96)));
	}
}
