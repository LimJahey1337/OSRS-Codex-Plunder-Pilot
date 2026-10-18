package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ReadyTest
{
	@Test
	public void theRouteWalksTheRoomOnceWithoutCrossingBack()
	{
		// Two rows of urns; nearest-first alone would zigzag between them.
		int[] xs = {0, 4, 8, 12, 0, 4, 8, 12};
		int[] ys = {0, 0, 0, 0, 6, 6, 6, 6};
		List<Integer> path = Route.order(0, 3, xs, ys, 8);
		assertEquals(8, path.size());
		assertEquals(8, path.stream().distinct().count());
		// One row there and the other back is 12 + 6 + 12 = 30 steps plus the 3 to reach the first.
		assertTrue("length " + Route.length(0, 3, xs, ys, path), Route.length(0, 3, xs, ys, path) <= 33);
	}

	@Test
	public void aShortPlanTakesTheNearestUrns()
	{
		int[] xs = {1, 2, 30, 31};
		int[] ys = {0, 0, 0, 0};
		List<Integer> path = Route.order(0, 0, xs, ys, 2);
		assertEquals(Arrays.asList(0, 1), path);
	}

	@Test
	public void theReadyCheckWarnsOfAFullPackAndLowHitpoints()
	{
		Plan plan = Planner.fresh(40, Goal.EXPERIENCE, new Pace());
		List<Check> checks = Check.ready(new Check.Kit(40, 10, 8, 2, false, false), plan, 6);
		assertEquals(Check.State.WARN, checks.get(0).getState());
		assertTrue(checks.stream().anyMatch(c -> c.getTitle().equals("Low hitpoints")));
		assertTrue(checks.stream().anyMatch(c -> c.getTitle().equals("Bring a snake charm")));
		assertTrue(checks.stream().anyMatch(c -> c.getTitle().equals("Bring a lockpick")));

		Plan high = Planner.fresh(91, Goal.EXPERIENCE, new Pace());
		List<Check> late = Check.ready(new Check.Kit(91, 70, 99, 20, true, true), high, 8);
		assertEquals(Check.State.GOOD, late.get(0).getState());
		assertTrue(late.stream().anyMatch(c -> c.getTitle().equals("Leave the snake charm")));
		assertTrue(late.stream().anyMatch(c -> c.getTitle().equals("Leave the lockpick")));
		assertTrue(late.stream().anyMatch(c -> c.getTitle().equals("Protect from Melee")));
	}

	@Test
	public void dangerComesBeforeThePlan()
	{
		Plan plan = Planner.plan(91, Goal.EXPERIENCE, new Pace(), new Here(Room.EIGHT, 13, false, false, true), 300);
		assertEquals(Step.Kind.URNS, Step.next(plan, true, false, 0).getKind());
		Step chased = Step.next(plan, true, false, 0, new Step.Alarm(false, false, true, true));
		assertEquals("Protect from Melee", chased.getTitle());
		assertEquals(Step.Kind.DANGER, Step.next(plan, true, false, 0, new Step.Alarm(true, false, false, false)).getKind());
		assertEquals("Eat first", Step.next(plan, true, false, 0, new Step.Alarm(true, true, false, false)).getTitle());
	}

	@Test
	public void slotsComeFromRecentRunsOnceThereAreThree()
	{
		Logbook book = new Logbook();
		Plan plan = Planner.fresh(91, Goal.EXPERIENCE, new Pace());
		assertTrue(book.slotsNeeded(plan, 10) > 0);
		for (int i = 0; i < 3; i++)
		{
			RunTracker t = new RunTracker();
			t.begin(0, 0, Goal.EXPERIENCE);
			t.room(Room.EIGHT, 0, null);
			t.urnSearched(5, null);
			t.artefacts(4 + i, 1000);
			book.record(t.finish(100), t.getOpened());
		}
		assertEquals(6, book.slotsNeeded(plan, 10));
	}
}
