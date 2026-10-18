package com.codex.plunder.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything kept per account: the learned pace, the chosen goal, every run, and every chest
 * and sarcophagus ever opened per room, which is what the sceptre odds are counted from.
 */
public final class Logbook
{
	/** Runs kept in full; older runs still count in the totals. */
	static final int KEEP = 300;

	private Pace pace = new Pace();
	private Goal goal = Goal.EXPERIENCE;
	private int level;
	private long xp;
	private List<Run> runs = new ArrayList<>();
	private long[] opened = new long[Room.values().length];
	private int sceptres;
	private long totalRuns;
	private long totalXp;
	private long totalArtefacts;
	private long totalTicks;

	public Pace getPace()
	{
		return pace;
	}

	public Goal getGoal()
	{
		return goal;
	}

	public void setGoal(Goal goal)
	{
		this.goal = goal == null ? Goal.EXPERIENCE : goal;
	}

	/** The Thieving level as last seen, so the panel plans while logged out. */
	public int getLevel()
	{
		return level;
	}

	public long getXp()
	{
		return xp;
	}

	public void seen(int level, long xp)
	{
		this.level = level;
		this.xp = xp;
	}

	public List<Run> getRuns()
	{
		return Collections.unmodifiableList(runs);
	}

	public long[] getOpened()
	{
		return opened.clone();
	}

	public long openedTotal()
	{
		long n = 0;
		for (long o : opened)
		{
			n += o;
		}
		return n;
	}

	public int getSceptres()
	{
		return sceptres;
	}

	public long getTotalRuns()
	{
		return totalRuns;
	}

	public long getTotalXp()
	{
		return totalXp;
	}

	public long getTotalArtefacts()
	{
		return totalArtefacts;
	}

	public long getTotalTicks()
	{
		return totalTicks;
	}

	public void record(Run run, long[] openedThisRun)
	{
		runs.add(run);
		while (runs.size() > KEEP)
		{
			runs.remove(0);
		}
		for (int i = 0; i < opened.length && i < openedThisRun.length; i++)
		{
			opened[i] += openedThisRun[i];
		}
		if (run.isSceptre())
		{
			sceptres++;
		}
		totalRuns++;
		totalXp += run.getThievingXp();
		totalArtefacts += run.getArtefacts();
		totalTicks += run.getTicks();
	}

	/**
	 * The sceptre turned up just after the last run was closed, since finding it throws the player
	 * out of the pyramid at once.
	 */
	public void sceptreAfter()
	{
		if (runs.isEmpty())
		{
			return;
		}
		Run last = runs.get(runs.size() - 1);
		if (!last.isSceptre())
		{
			last.markSceptre();
			sceptres++;
		}
	}

	/**
	 * Free slots a run like this one needs for its artefacts: the most picked up in any of the
	 * last few runs, or a guess from the plan before there are three runs to go on.
	 */
	public int slotsNeeded(Plan plan, int last)
	{
		int n = 0;
		int most = 0;
		for (int i = runs.size() - 1; i >= 0 && n < last; i--, n++)
		{
			most = Math.max(most, runs.get(i).getArtefactCount());
		}
		if (n >= 3)
		{
			return most;
		}
		int guess = 0;
		for (Plan.Stop s : plan.getStops())
		{
			guess += s.getUrns() + (s.isChest() ? 3 : 0) + (s.isSarcophagus() ? 3 : 0);
		}
		// Roughly one urn in three holds an artefact; chests and sarcophagi nearly always do.
		return (int) Math.ceil(guess / 3.0);
	}

	/**
	 * Thieving experience an hour over the last few runs, each counted with the usual walk back in
	 * between runs, or 0 before there are two runs to go on.
	 */
	public int recentRate(int last)
	{
		int n = 0;
		long xp = 0;
		long ticks = 0;
		for (int i = runs.size() - 1; i >= 0 && n < last; i--)
		{
			Run r = runs.get(i);
			xp += r.getThievingXp();
			ticks += r.getTicks() + Planner.RESTART_TICKS;
			n++;
		}
		return n < 2 || ticks <= 0 ? 0 : (int) Math.round(xp * Planner.TICKS_PER_HOUR / ticks);
	}

	/** Puts back what an older or damaged save left out. */
	public Logbook repair()
	{
		if (pace == null)
		{
			pace = new Pace();
		}
		pace.repair();
		if (goal == null)
		{
			goal = Goal.EXPERIENCE;
		}
		if (runs == null)
		{
			runs = new ArrayList<>();
		}
		runs.removeIf(r -> r == null);
		if (opened == null || opened.length != Room.values().length)
		{
			long[] fixed = new long[Room.values().length];
			if (opened != null)
			{
				System.arraycopy(opened, 0, fixed, 0, Math.min(opened.length, fixed.length));
			}
			opened = fixed;
		}
		return this;
	}
}
