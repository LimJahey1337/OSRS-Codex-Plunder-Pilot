package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class LeaksTest
{
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
	public void theUsualCrossingIsTheMedianAndTheGapIsPricedInExperience()
	{
		// Level 51 opens room 4. Room 2 is crossed in 20, 30, 30: usual 30, quickest 20.
		List<Run> runs = Arrays.asList(
			run(Goal.EXPERIENCE, 0, 25, 45, 70),
			run(Goal.EXPERIENCE, 0, 25, 55, 80),
			run(Goal.EXPERIENCE, 0, 26, 56, 82));
		Leaks l = Leaks.of(runs, Goal.EXPERIENCE, 51, new Pace());
		assertEquals(3, l.getRuns());
		assertEquals(3, l.getRooms().size());
		Leaks.Leak two = l.getRooms().get(1);
		assertEquals(Room.TWO, two.getRoom());
		assertEquals(30, two.getTypical());
		assertEquals(20, two.getQuickest());
		assertEquals(10, two.getGap());
		assertTrue(two.getXpPerHour() > 0);
		assertEquals(Room.TWO, l.biggest().getRoom());
		assertEquals(l.getRooms().get(0).getXpPerHour() + two.getXpPerHour() + l.getRooms().get(2).getXpPerHour(), l.total());
	}

	@Test
	public void fewerThanThreeRunsOrOtherGoalsTellNothing()
	{
		List<Run> runs = Arrays.asList(
			run(Goal.EXPERIENCE, 0, 25, 45, 70),
			run(Goal.SCEPTRE, 0, 25, 55, 80),
			run(Goal.EXPERIENCE, 0, 25, 55, 80, 100));
		Leaks l = Leaks.of(runs, Goal.EXPERIENCE, 51, new Pace());
		assertTrue(l.getRooms().isEmpty());
		assertNull(l.biggest());
		assertEquals(0, l.total());
	}

	@Test
	public void onlyTheLastTwentyRunsSetTheUsualCrossing()
	{
		List<Run> runs = new ArrayList<>();
		for (int i = 0; i < 30; i++)
		{
			runs.add(run(Goal.EXPERIENCE, 0, 20, 40, 60));
		}
		for (int i = 0; i < 20; i++)
		{
			runs.add(run(Goal.EXPERIENCE, 0, 30, 50, 70));
		}
		Leaks l = Leaks.of(runs, Goal.EXPERIENCE, 51, new Pace());
		assertEquals(Leaks.RECENT, l.getRuns());
		assertEquals(30, l.getRooms().get(0).getTypical());
		assertEquals(20, l.getRooms().get(0).getQuickest());
	}

	@Test
	public void aCleanRunGivesNothingAway()
	{
		List<Run> runs = Collections.nCopies(5, run(Goal.EXPERIENCE, 0, 20, 40, 60));
		Leaks l = Leaks.of(runs, Goal.EXPERIENCE, 51, new Pace());
		assertNull(l.biggest());
		assertEquals(0, l.total());
	}

	@Test
	public void eachRoomLearnsItsOwnUrnPaceOnceSettled()
	{
		Pace p = new Pace();
		for (int i = 0; i < Pace.TRUSTED - 1; i++)
		{
			p.urnTook(Room.EIGHT, 16);
		}
		assertEquals(p.urn(), p.urn(Room.EIGHT), 1e-9);
		p.urnTook(Room.EIGHT, 16);
		assertTrue(p.knowsUrns(Room.EIGHT));
		assertEquals(16, p.urn(Room.EIGHT), 1e-9);
		// A room with no searches of its own uses the pace across rooms.
		assertEquals(p.urn(), p.urn(Room.SEVEN), 1e-9);
		assertEquals(0, p.getUrnSamples(Room.SEVEN));
	}

	@Test
	public void slowUrnsInTheDeepestRoomMoveTheSearchesToTheRoomAbove()
	{
		Pace p = new Pace();
		for (int i = 0; i < 8; i++)
		{
			p.urnTook(Room.SEVEN, 8);
			p.urnTook(Room.EIGHT, 20);
		}
		Plan plan = Planner.fresh(91, Goal.EXPERIENCE, p);
		Plan flat = Planner.fresh(91, Goal.EXPERIENCE, p.flat());
		assertTrue(plan.stop(Room.SEVEN).getUrns() > flat.stop(Room.SEVEN).getUrns());
		Leaks l = Leaks.of(Collections.emptyList(), Goal.EXPERIENCE, 91, p);
		assertEquals(Room.EIGHT, l.getFasterUrnsFrom());
		assertEquals(Room.SEVEN, l.getFasterUrnsTo());
		assertTrue(l.getMovedUrns() > 0);
		assertTrue(l.getUrnGain() > 0);
	}

	@Test
	public void evenUrnTimesMoveNothing()
	{
		Pace p = new Pace();
		for (int i = 0; i < 8; i++)
		{
			p.urnTook(Room.SEVEN, 10);
			p.urnTook(Room.EIGHT, 10);
		}
		Leaks l = Leaks.of(Collections.emptyList(), Goal.EXPERIENCE, 91, p);
		assertNull(l.getFasterUrnsFrom());
		assertEquals(0, l.getUrnGain());
	}

	@Test
	public void aDamagedRoomPaceStartsAgain()
	{
		Pace p = Salvage.read(new com.google.gson.Gson(), "{\"urn\":9,\"urnSamples\":10,\"urnByRoom\":[0,0,0,0,0,0,99,12],\"urnSamplesByRoom\":[0,0,0,0,0,0,9,9]}", Pace.class);
		assertNotNull(p);
		p.repair();
		assertEquals(0, p.getUrnSamples(Room.SEVEN));
		assertEquals(9, p.getUrnSamples(Room.EIGHT));
		assertEquals(12, p.urn(Room.EIGHT), 1e-9);
	}

	@Test
	public void searchingIsNotCountedAsASlowCrossing()
	{
		// Every run takes 20 ticks of trap, walk and door in room 2; one also searches 5 urns there.
		List<Run> runs = new ArrayList<>();
		for (int i = 0; i < 4; i++)
		{
			RunTracker t = new RunTracker();
			t.begin(0, 0, Goal.EXPERIENCE);
			t.room(Room.ONE, 0, null);
			t.room(Room.TWO, 20, null);
			int tick = 30;
			if (i == 3)
			{
				for (int u = 0; u < 5; u++)
				{
					t.urnSearched(tick, null);
					tick += 10;
				}
				tick -= 10;
			}
			t.room(Room.THREE, tick + 10, null);
			t.room(Room.FOUR, tick + 30, null);
			t.thieving(100);
			runs.add(t.finish(tick + 80));
		}
		assertEquals(20, runs.get(3).passAt(Room.TWO));
		assertTrue(Splits.segment(runs.get(3), Room.TWO) > 20);
		Leaks l = Leaks.of(runs, Goal.EXPERIENCE, 51, new Pace());
		assertEquals(0, l.getRooms().get(1).getGap());
		assertNull(l.biggest());
	}
}
