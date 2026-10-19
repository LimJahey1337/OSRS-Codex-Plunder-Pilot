package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class OutlookTest
{
	private static final long MIN = 60_000L;
	private static final long DAY = 24 * 60 * MIN;

	/** Ten five-minute runs a minute apart, starting on the given day. */
	private static void sitting(List<Run> runs, long day)
	{
		for (int i = 0; i < 10; i++)
		{
			runs.add(new Run(day * DAY + i * 6 * MIN, 500, 8, 30_000, 0, 20, 2, 0, 1000, 2, 0.01, false, Goal.EXPERIENCE));
		}
	}

	@Test
	public void twoSittingsAWeekPutAHundredRunsFiveWeeksAway()
	{
		List<Run> runs = new ArrayList<>();
		for (long day : new long[]{100, 103, 107, 110})
		{
			sitting(runs, day);
		}
		long now = 114 * DAY;
		Outlook o = Outlook.of(Session.history(runs, now), now);
		assertTrue(o.isKnown());
		// A hundred runs is ten sittings like these; at two a week that is five weeks.
		assertEquals(now + 35 * DAY, o.afterRuns(100, now), 60_000);
		assertEquals(2 * 59.6 / 60, o.hoursPerWeek(), 1e-9);
	}

	@Test
	public void oneSittingOrAnOldOneSaysNothing()
	{
		List<Run> runs = new ArrayList<>();
		sitting(runs, 100);
		assertFalse(Outlook.of(Session.history(runs, 101 * DAY), 101 * DAY).isKnown());
		sitting(runs, 101);
		// Both sittings are over four weeks old by now.
		long later = 140 * DAY;
		Outlook old = Outlook.of(Session.history(runs, later), later);
		assertFalse(old.isKnown());
		assertEquals(-1, old.afterRuns(10, later));
	}

	@Test
	public void aFirstBusyWeekendIsSpreadOverAWeek()
	{
		List<Run> runs = new ArrayList<>();
		sitting(runs, 100);
		sitting(runs, 101);
		long now = 102 * DAY;
		Outlook o = Outlook.of(Session.history(runs, now), now);
		// Two sittings in two days read as two a week, not one a day.
		assertEquals(2 * 59.6 / 60, o.hoursPerWeek(), 1e-9);
		assertEquals(-1, o.afterRuns(1_000_000, now));
	}
}
