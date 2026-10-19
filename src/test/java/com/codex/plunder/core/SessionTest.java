package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class SessionTest
{
	private static final long MIN = 60_000L;

	/** A five minute run starting at the given minute. */
	private static Run run(long startMinute, int xp, double sceptre, boolean found)
	{
		return new Run(startMinute * MIN, 500, 8, xp, 0, 20, 2, 0, 1000, 2, sceptre, found, Goal.EXPERIENCE);
	}

	@Test
	public void aBreakEndsTheSession()
	{
		Session s = Session.latest(Arrays.asList(run(0, 30_000, 0, false), run(60, 40_000, 0.01, false), run(66, 42_000, 0.01, false)),
			72 * MIN);
		assertEquals(2, s.getRuns().size());
		assertEquals(82_000, s.getXp());
		assertTrue(s.isCurrent());
	}

	@Test
	public void theRateCountsTheWalkBackBetweenRuns()
	{
		// Two runs six minutes apart: 5 + 1 + 5 minutes of play, plus one walk back (36s) after the last.
		Session s = Session.latest(Arrays.asList(run(0, 50_000, 0, false), run(6, 50_000, 0, false)), 12 * MIN);
		long span = 11 * MIN + 36_000;
		assertEquals(span, s.spanMs());
		assertEquals(Math.round(100_000 * 3_600_000.0 / span), s.xpPerHour());
	}

	@Test
	public void theSceptreChanceCompoundsAcrossRuns()
	{
		Session s = Session.latest(Arrays.asList(run(0, 1, 0.01, false), run(6, 1, 0.02, false)), 12 * MIN);
		assertEquals(1 - 0.99 * 0.98, s.getSceptreChance(), 1e-9);
		assertEquals(0, s.getSceptres());
	}

	@Test
	public void anOldSessionIsShownAsTheLastOne()
	{
		Session s = Session.latest(Arrays.asList(run(0, 1, 0, false), run(6, 9, 0, true)), 600 * MIN);
		assertFalse(s.isCurrent());
		assertEquals(1, s.getSceptres());
		assertEquals(9, s.best().getThievingXp());
		assertTrue(Session.latest(Collections.emptyList(), 0).isEmpty());
	}
}
