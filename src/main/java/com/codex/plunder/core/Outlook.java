package com.codex.plunder.core;

import java.util.List;

/**
 * How much the player plays, read from their own sessions over the last four weeks, so a number
 * of runs or hours can be turned into a date: "even odds of a sceptre by 12 Nov". Only sessions
 * that started inside the window count, and a window younger than a week is stretched to a week,
 * so one long first evening does not read as playing every day.
 */
public final class Outlook
{
	static final long DAY_MS = 24 * 3_600_000L;
	/** How far back the pace is read. */
	static final long WINDOW_MS = 28 * DAY_MS;
	/** The least history a pace is read from. */
	static final long MIN_SPAN_MS = 7 * DAY_MS;
	/** Fewer sessions than this say nothing about how often the player comes back. */
	static final int MIN_SESSIONS = 2;
	/** Past this a date says nothing useful. */
	static final long MAX_DAYS = 3 * 365;

	private final int sessions;
	private final int runs;
	private final long playedMs;
	private final long spanMs;

	private Outlook(int sessions, int runs, long playedMs, long spanMs)
	{
		this.sessions = sessions;
		this.runs = runs;
		this.playedMs = playedMs;
		this.spanMs = spanMs;
	}

	/**
	 * @param history every session kept, newest first, as {@link Session#history} gives them
	 * @param now the time now
	 */
	public static Outlook of(List<Session> history, long now)
	{
		int n = 0;
		int runs = 0;
		long played = 0;
		long first = now;
		for (Session s : history)
		{
			if (s.isEmpty() || s.getStartedAt() < now - WINDOW_MS)
			{
				continue;
			}
			n++;
			runs += s.getRuns().size();
			played += s.spanMs();
			first = Math.min(first, s.getStartedAt());
		}
		return new Outlook(n, runs, played, Math.max(MIN_SPAN_MS, now - first));
	}

	/** Enough sessions to tell a pace from. */
	public boolean isKnown()
	{
		return sessions >= MIN_SESSIONS && playedMs > 0 && runs > 0;
	}

	/** Hours played a week. */
	public double hoursPerWeek()
	{
		return playedMs / 3_600_000.0 * (7.0 * DAY_MS / spanMs);
	}

	/** Runs played an hour, the walk back included. */
	public double runsPerHour()
	{
		return playedMs <= 0 ? 0 : runs * 3_600_000.0 / playedMs;
	}

	/**
	 * @return when that many more hours of play are done at this pace, or -1 when it cannot be told or is years away
	 */
	public long afterHours(double hours, long now)
	{
		if (!isKnown() || hours < 0)
		{
			return -1;
		}
		double days = hours / hoursPerWeek() * 7;
		return days > MAX_DAYS ? -1 : now + Math.round(days * DAY_MS);
	}

	/** @return when that many more runs are played at this pace, or -1 when it cannot be told or is years away */
	public long afterRuns(long runs, long now)
	{
		double perHour = runsPerHour();
		return perHour <= 0 ? -1 : afterHours(runs / perHour, now);
	}
}
