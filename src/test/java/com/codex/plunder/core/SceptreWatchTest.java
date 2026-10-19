package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SceptreWatchTest
{
	@Test
	public void aSceptreFoundInARunCounts()
	{
		SceptreWatch w = new SceptreWatch();
		assertEquals(SceptreWatch.Seen.NOTHING, w.tick(0, true, 100));
		assertEquals(SceptreWatch.Seen.FOUND, w.tick(1, true, 101));
		// Moved from the pack to the hand: still one.
		assertEquals(SceptreWatch.Seen.NOTHING, w.tick(1, false, 102));
	}

	@Test
	public void theOneThatLandsAsTheRunEndsCountsForIt()
	{
		SceptreWatch w = new SceptreWatch();
		w.tick(0, true, 100);
		w.runEnded(101);
		assertEquals(SceptreWatch.Seen.FOUND_AFTER, w.tick(1, false, 103));
	}

	@Test
	public void thePackArrivingAfterALoginIsNoFind()
	{
		SceptreWatch w = new SceptreWatch();
		w.tick(0, true, 100);
		// The run ends because the player logs out; the client's tick count starts again on login.
		w.runEnded(5000);
		w.reset();
		// The first ticks after login: the pack has not arrived, then it holds a sceptre from the bank.
		assertEquals(SceptreWatch.Seen.NOTHING, w.tick(-1, false, 1));
		assertEquals(SceptreWatch.Seen.NOTHING, w.tick(1, false, 2));
	}

	@Test
	public void withoutAResetAStalePackStillIsNotAFind()
	{
		SceptreWatch w = new SceptreWatch();
		w.tick(0, false, 10);
		// The pack goes unknown for a tick (a hop) and comes back with the sceptre it had all along.
		assertEquals(SceptreWatch.Seen.NOTHING, w.tick(-1, false, 11));
		assertEquals(SceptreWatch.Seen.NOTHING, w.tick(1, false, 12));
	}

	@Test
	public void longAfterARunASceptreIsSomethingElse()
	{
		SceptreWatch w = new SceptreWatch();
		w.tick(0, true, 100);
		w.runEnded(101);
		assertEquals(SceptreWatch.Seen.NOTHING, w.tick(0, false, 102));
		assertEquals(SceptreWatch.Seen.OTHER, w.tick(1, false, 101 + SceptreWatch.GRACE_TICKS + 1));
	}
}
