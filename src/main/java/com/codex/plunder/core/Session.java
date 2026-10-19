package com.codex.plunder.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One sitting at the pyramid: the runs played back to back, with no break longer than twenty
 * minutes between one ending and the next starting. It counts what the sitting earned, the rate
 * it ran at with the walk back between runs included, and the sceptre chance its openings carried.
 */
public final class Session
{
	/** A gap longer than this between runs ends a session. */
	static final long BREAK_MS = 20 * 60_000L;
	private static final long TICK_MS = 600;

	private final List<Run> runs;
	private final boolean current;
	private final long xp;
	private final long strength;
	private final long artefacts;
	private final double sceptreChance;
	private final int sceptres;
	private final long startedAt;
	private final long endedAt;

	private Session(List<Run> runs, boolean current)
	{
		this.runs = Collections.unmodifiableList(runs);
		this.current = current;
		long x = 0;
		long s = 0;
		long a = 0;
		double miss = 1;
		int found = 0;
		for (Run r : runs)
		{
			x += r.getThievingXp();
			s += r.getStrengthXp();
			a += r.getArtefacts();
			miss *= 1 - Math.max(0, Math.min(1, r.getSceptreChance()));
			found += r.isSceptre() ? 1 : 0;
		}
		xp = x;
		strength = s;
		artefacts = a;
		sceptreChance = 1 - miss;
		sceptres = found;
		startedAt = runs.isEmpty() ? 0 : runs.get(0).getAt();
		endedAt = runs.isEmpty() ? 0 : end(runs.get(runs.size() - 1));
	}

	/**
	 * The newest session in the runs.
	 *
	 * @param runs every run kept, oldest first
	 * @param now the time now, to tell a session still going from one that has ended
	 */
	public static Session latest(List<Run> runs, long now)
	{
		List<Run> picked = new ArrayList<>();
		if (runs != null)
		{
			for (int i = runs.size() - 1; i >= 0; i--)
			{
				Run r = runs.get(i);
				if (!picked.isEmpty())
				{
					long gap = picked.get(0).getAt() - end(r);
					if (gap > BREAK_MS)
					{
						break;
					}
				}
				picked.add(0, r);
			}
		}
		boolean current = !picked.isEmpty() && now - end(picked.get(picked.size() - 1)) <= BREAK_MS;
		return new Session(picked, current);
	}

	/** Sessions shorter than this many runs are never called the best: one lucky run is not a pace. */
	public static final int BEST_MIN_RUNS = 3;

	/**
	 * Every session in the runs kept, newest first. The oldest may have begun before the first run
	 * still kept, so it can be shorter than it was.
	 *
	 * @param runs every run kept, oldest first
	 * @param now the time now, to tell whether the newest session is still going
	 */
	public static List<Session> history(List<Run> runs, long now)
	{
		List<Session> out = new ArrayList<>();
		if (runs == null || runs.isEmpty())
		{
			return out;
		}
		List<Run> group = new ArrayList<>();
		for (Run r : runs)
		{
			if (!group.isEmpty() && r.getAt() - end(group.get(group.size() - 1)) > BREAK_MS)
			{
				out.add(0, new Session(group, false));
				group = new ArrayList<>();
			}
			group.add(r);
		}
		out.add(0, new Session(group, now - end(group.get(group.size() - 1)) <= BREAK_MS));
		return out;
	}

	/** The session with the best experience an hour of at least {@link #BEST_MIN_RUNS} runs, or null. */
	public static Session best(List<Session> sessions)
	{
		Session best = null;
		for (Session s : sessions)
		{
			if (s.runs.size() >= BEST_MIN_RUNS && (best == null || s.xpPerHour() > best.xpPerHour()))
			{
				best = s;
			}
		}
		return best;
	}

	private static long end(Run r)
	{
		return r.getAt() + r.getTicks() * TICK_MS;
	}

	public boolean isEmpty()
	{
		return runs.isEmpty();
	}

	/** Whether the last run ended recently enough that the session is still going. */
	public boolean isCurrent()
	{
		return current;
	}

	public List<Run> getRuns()
	{
		return runs;
	}

	public long getXp()
	{
		return xp;
	}

	public long getStrengthXp()
	{
		return strength;
	}

	public long getArtefacts()
	{
		return artefacts;
	}

	public long getStartedAt()
	{
		return startedAt;
	}

	public long getEndedAt()
	{
		return endedAt;
	}

	/** The chance the session's chests and sarcophagi together had of a sceptre. */
	public double getSceptreChance()
	{
		return sceptreChance;
	}

	public int getSceptres()
	{
		return sceptres;
	}

	/**
	 * How long the session has taken: from the first run starting to the last ending, plus one
	 * walk back for the last run, so a single run is not counted as all play and no walking.
	 */
	public long spanMs()
	{
		return runs.isEmpty() ? 0 : Math.max(0, endedAt - startedAt) + Planner.RESTART_TICKS * TICK_MS;
	}

	public int xpPerHour()
	{
		return perHour(xp);
	}

	public int artefactsPerHour()
	{
		return perHour(artefacts);
	}

	private int perHour(long amount)
	{
		long span = spanMs();
		return span <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, Math.round(amount * 3_600_000.0 / span));
	}

	/** The run that paid the most Thieving experience, or null. */
	public Run best()
	{
		Run best = null;
		for (Run r : runs)
		{
			if (best == null || r.getThievingXp() > best.getThievingXp())
			{
				best = r;
			}
		}
		return best;
	}
}
