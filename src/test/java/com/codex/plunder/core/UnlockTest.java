package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class UnlockTest
{
	private final Pace pace = new Pace();

	@Test
	public void reachingARoomsLevelOpensIt()
	{
		Unlock.Opened o = Unlock.opened(60, 61, Goal.EXPERIENCE, pace);
		assertEquals(Room.FIVE, o.getRoom());
		assertTrue("room 5 pays more a run", o.getRunXpNow() > o.getRunXpBefore());
		assertTrue(o.getRateNow() > o.getRateBefore());
		assertEquals(Planner.fresh(61, Goal.EXPERIENCE, pace).getThievingXp(), o.getRunXpNow());
		assertEquals(Planner.fresh(60, Goal.EXPERIENCE, pace).getThievingXp(), o.getRunXpBefore());
	}

	@Test
	public void levelsBetweenRoomsAndFirstSightingsOpenNothing()
	{
		assertNull(Unlock.opened(61, 62, Goal.EXPERIENCE, pace));
		assertNull("never seen before: no news", Unlock.opened(0, 91, Goal.EXPERIENCE, pace));
		assertNull(Unlock.opened(91, 91, Goal.EXPERIENCE, pace));
		assertNull(Unlock.opened(99, 98, Goal.EXPERIENCE, pace));
	}

	@Test
	public void theFirstRoomAndALongJumpNameTheDeepestRoom()
	{
		Unlock.Opened first = Unlock.opened(20, 21, Goal.EXPERIENCE, pace);
		assertEquals(Room.ONE, first.getRoom());
		assertEquals(0, first.getRunXpBefore());
		assertEquals(Room.SEVEN, Unlock.opened(55, 81, Goal.SCEPTRE, pace).getRoom());
	}

	@Test
	public void runsToTheNextRoom()
	{
		// Level 60 to 61 is 273,742 to 302,288 xp, 28,546 apart: at 10,000 a run, 3 runs.
		long xp = Levels.xpFor(61) - 28_546;
		assertEquals(3, Unlock.runsToNext(60, xp, 10_000));
		assertEquals(1, Unlock.runsToNext(60, Levels.xpFor(61) - 1, 10_000));
		assertEquals(0, Unlock.runsToNext(91, Levels.xpFor(95), 10_000));
		assertEquals(0, Unlock.runsToNext(60, xp, 0));
	}
}
