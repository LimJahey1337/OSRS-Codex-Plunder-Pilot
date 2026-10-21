package com.codex.plunder.core;

import java.util.Arrays;
import java.util.List;

/**
 * Room splits, the way a speedrunner keeps them: the moment each room was entered, measured
 * against the run that reached the deepest room fastest, and the quickest the player has ever
 * crossed each room. Only runs for the same goal that reached the same deepest room are
 * compared, since a sceptre run or a run two rooms short spends its clock differently.
 */
public final class Splits
{
	/** Ticks from the start to entering each room in the best run; -1 where not known. */
	private final int[] best;
	/** The quickest each room has been crossed, entry to entry; -1 where not known. */
	private final int[] segments;
	private final Room top;
	private final int compared;

	private Splits(int[] best, int[] segments, Room top, int compared)
	{
		this.best = best;
		this.segments = segments;
		this.top = top;
		this.compared = compared;
	}

	/**
	 * @param runs every run kept, oldest first
	 * @param goal the goal the comparison is for
	 * @param top the deepest room the player's level opens
	 */
	public static Splits of(List<Run> runs, Goal goal, Room top)
	{
		int n = Room.values().length;
		int[] best = new int[n];
		int[] segments = new int[n];
		Arrays.fill(best, -1);
		Arrays.fill(segments, -1);
		int compared = 0;
		int bestAtTop = Integer.MAX_VALUE;
		if (runs != null && top != null)
		{
			for (Run run : runs)
			{
				if (!comparable(run, goal, top))
				{
					continue;
				}
				compared++;
				int atTop = run.splitAt(top);
				if (atTop < bestAtTop)
				{
					bestAtTop = atTop;
					for (Room r : Room.values())
					{
						best[r.ordinal()] = r.ordinal() <= top.ordinal() ? run.splitAt(r) : -1;
					}
				}
				for (int i = 0; i < top.ordinal(); i++)
				{
					int seg = segment(run, Room.values()[i]);
					if (seg >= 0 && (segments[i] < 0 || seg < segments[i]))
					{
						segments[i] = seg;
					}
				}
			}
		}
		return new Splits(best, segments, top, compared);
	}

	/** Whether a run can be measured against the others: same goal, as deep, and its arrival there seen. */
	public static boolean comparable(Run run, Goal goal, Room top)
	{
		return run != null && top != null && run.getGoal() == goal && run.getDeepest() == top.getNumber() && run.splitAt(top) >= 0;
	}

	/** Ticks the run took from entering the room to entering the next, or -1 when either was not seen. */
	public static int segment(Run run, Room room)
	{
		if (room == null || room.ordinal() + 1 >= Room.values().length)
		{
			return -1;
		}
		int from = run.splitAt(room);
		int to = run.splitAt(Room.values()[room.ordinal() + 1]);
		return from < 0 || to < from ? -1 : to - from;
	}

	public Room getTop()
	{
		return top;
	}

	/** Runs the comparison was drawn from. */
	public int getCompared()
	{
		return compared;
	}

	public boolean hasBest()
	{
		return top != null && best[top.ordinal()] >= 0;
	}

	/** Ticks from the start to entering the room in the best run, or -1. */
	public int bestAt(Room room)
	{
		return room == null ? -1 : best[room.ordinal()];
	}

	/** The quickest the room has been crossed, or -1. */
	public int bestSegment(Room room)
	{
		return room == null ? -1 : segments[room.ordinal()];
	}

	/**
	 * Ticks ahead (negative) or behind (positive) the best run on entering the room, or null when
	 * the best run's arrival there is not known.
	 */
	public Integer delta(Room room, int enteredAt)
	{
		int b = bestAt(room);
		return b < 0 || enteredAt < 0 ? null : enteredAt - b;
	}

	/**
	 * The deepest room reached by stringing together the quickest crossing of every room before it,
	 * which no single run has managed yet: what a clean run would do. -1 until every room has one.
	 */
	public int sumOfBest()
	{
		if (top == null)
		{
			return -1;
		}
		int sum = 0;
		for (int i = 0; i < top.ordinal(); i++)
		{
			if (segments[i] < 0)
			{
				return -1;
			}
			sum += segments[i];
		}
		// Room one is entered when the clock starts, so the best run's first split is the start.
		int start = best[0] < 0 ? 0 : best[0];
		return start + sum;
	}

	/** Ticks over its quickest crossing that a run must lose in a room before it is named: three seconds. */
	public static final int SLOW = 5;

	/**
	 * The room this run lost most time in against its quickest crossing before it, or null when
	 * no room ran {@link #SLOW} ticks over.
	 */
	public Room slowest(Run run)
	{
		Room worst = null;
		int most = SLOW - 1;
		if (run == null || top == null)
		{
			return null;
		}
		for (int i = 0; i < top.ordinal(); i++)
		{
			Room r = Room.values()[i];
			int over = overrun(run, r);
			if (over > most)
			{
				most = over;
				worst = r;
			}
		}
		return worst;
	}

	/** Ticks the run took in the room over its quickest crossing before it, or 0 when either is not known. */
	public int overrun(Run run, Room room)
	{
		int seg = segment(run, room);
		int quickest = bestSegment(room);
		return seg < 0 || quickest < 0 ? 0 : Math.max(0, seg - quickest);
	}

	/** Whether this run reached the deepest room faster than every run before it. */
	public boolean isNewBest(Run run, Goal goal)
	{
		return comparable(run, goal, top) && (!hasBest() || run.splitAt(top) < bestAt(top));
	}
}
