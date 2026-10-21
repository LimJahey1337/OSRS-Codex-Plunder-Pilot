package com.codex.plunder.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Where the clock goes. Each room the player crosses on the way to their deepest is timed over
 * their recent runs without its searches (the trap, the walk and finding the door), so a run
 * that searched more is not counted slow. The usual crossing against the quickest ever is time a
 * run could have spent searching deeper, and that is turned into experience an hour at what the next unplanned
 * search would pay. It also says when the player's own urn times room by room change the plan.
 */
public final class Leaks
{
	/** Recent runs the usual crossing is read from. */
	public static final int RECENT = 20;
	/** Runs a room needs timed before its usual crossing is told. */
	public static final int MIN_RUNS = 3;
	/** A gap under two seconds is the ordinary spread of a room's crossings, not time to find. */
	public static final int NOISE = 3;

	/** One room's crossing: usual against quickest. */
	public static final class Leak
	{
		private final Room room;
		private final int typical;
		private final int quickest;
		private final int xpPerHour;

		Leak(Room room, int typical, int quickest, int xpPerHour)
		{
			this.room = room;
			this.typical = typical;
			this.quickest = quickest;
			this.xpPerHour = xpPerHour;
		}

		public Room getRoom()
		{
			return room;
		}

		/** The median crossing over recent runs, in ticks. */
		public int getTypical()
		{
			return typical;
		}

		/** The quickest crossing ever, in ticks. */
		public int getQuickest()
		{
			return quickest;
		}

		/** Ticks the usual crossing gives away to the quickest. */
		public int getGap()
		{
			return Math.max(0, typical - quickest);
		}

		/** Experience an hour the gap costs; nothing for a gap within the ordinary spread. */
		public int getXpPerHour()
		{
			return xpPerHour;
		}
	}

	private final Room top;
	private final List<Leak> rooms;
	private final int runs;
	private final Room fasterUrnsFrom;
	private final Room fasterUrnsTo;
	private final int movedUrns;
	private final int urnGain;

	private Leaks(Room top, List<Leak> rooms, int runs, Room fasterUrnsFrom, Room fasterUrnsTo, int movedUrns, int urnGain)
	{
		this.top = top;
		this.rooms = rooms;
		this.runs = runs;
		this.fasterUrnsFrom = fasterUrnsFrom;
		this.fasterUrnsTo = fasterUrnsTo;
		this.movedUrns = movedUrns;
		this.urnGain = urnGain;
	}

	/**
	 * @param runs every run kept, oldest first
	 * @param level the player's real Thieving level
	 */
	public static Leaks of(List<Run> runs, Goal goal, int level, Pace pace)
	{
		Room top = Room.highest(level);
		if (top == null || pace == null)
		{
			return new Leaks(top, Collections.emptyList(), 0, null, null, 0, 0);
		}
		Plan fresh = Planner.fresh(level, goal, pace);

		List<Run> recent = new ArrayList<>();
		int[] quickest = new int[Room.values().length];
		Arrays.fill(quickest, -1);
		if (runs != null)
		{
			for (Run run : runs)
			{
				if (!Splits.comparable(run, goal, top))
				{
					continue;
				}
				recent.add(run);
				for (int i = 0; i < top.ordinal(); i++)
				{
					int seg = run.passAt(Room.values()[i]);
					if (seg >= 0 && (quickest[i] < 0 || seg < quickest[i]))
					{
						quickest[i] = seg;
					}
				}
			}
		}
		if (recent.size() > RECENT)
		{
			recent = recent.subList(recent.size() - RECENT, recent.size());
		}

		double perTick = valuePerTick(fresh, pace);
		double runsPerHour = Planner.TICKS_PER_HOUR / Math.max(1, Math.min(fresh.getTicksLeft(), fresh.getTicksNeeded()) + Planner.RESTART_TICKS);
		List<Leak> leaks = new ArrayList<>();
		for (int i = 0; i < top.ordinal(); i++)
		{
			Room room = Room.values()[i];
			List<Integer> segs = new ArrayList<>();
			for (Run run : recent)
			{
				int seg = run.passAt(room);
				if (seg >= 0)
				{
					segs.add(seg);
				}
			}
			if (segs.size() < MIN_RUNS || quickest[i] < 0)
			{
				continue;
			}
			Collections.sort(segs);
			int typical = segs.get(segs.size() / 2);
			int gap = Math.max(0, typical - quickest[i]);
			int cost = gap < NOISE ? 0 : (int) Math.round(gap * perTick * runsPerHour);
			leaks.add(new Leak(room, typical, quickest[i], cost));
		}

		// The plan the one pace across rooms would make, against the one the player's own times make.
		Room from = null;
		Room to = null;
		int moved = 0;
		int gain = 0;
		Plan flat = Planner.fresh(level, goal, pace.flat());
		if (flat != null && goal == Goal.EXPERIENCE)
		{
			int most = 0;
			int least = 0;
			for (Plan.Stop own : fresh.getStops())
			{
				Plan.Stop other = flat.stop(own.getRoom());
				int d = own.getUrns() - (other == null ? 0 : other.getUrns());
				if (d < least)
				{
					least = d;
					from = own.getRoom();
				}
				if (d > most)
				{
					most = d;
					to = own.getRoom();
				}
			}
			if (from != null && to != null)
			{
				moved = Math.min(most, -least);
				// What the flat plan would really earn, timed at the player's own pace room by room.
				gain = fresh.getThievingXp() - timedAtOwnPace(flat, fresh, pace);
			}
			if (moved <= 0 || gain <= 0)
			{
				from = null;
				to = null;
				moved = 0;
				gain = 0;
			}
		}
		return new Leaks(top, Collections.unmodifiableList(leaks), recent.size(), from, to, moved, gain);
	}

	/**
	 * Experience the flat plan's searches would earn within the clock when each urn takes the
	 * player's own time in its room: urns that no longer fit are dropped from the deepest room up
	 * the cheapest way, by what they pay per tick.
	 */
	private static int timedAtOwnPace(Plan flat, Plan own, Pace pace)
	{
		double over = 0;
		List<double[]> urns = new ArrayList<>();
		for (Plan.Stop s : flat.getStops())
		{
			Room r = s.getRoom();
			over += s.getUrns() * (pace.urn(r) - pace.urn());
			for (int u = 0; u < s.getUrns(); u++)
			{
				urns.add(new double[]{r.getUrnXp() / pace.urn(r), pace.urn(r), r.getUrnXp()});
			}
		}
		int xp = flat.getThievingXp();
		// The flat plan believed it fitted; the time it is over by now comes off its worst urns.
		urns.sort((a, b) -> Double.compare(a[0], b[0]));
		for (double[] u : urns)
		{
			if (over <= 0)
			{
				break;
			}
			over -= u[1];
			xp -= (int) u[2];
		}
		return xp;
	}

	/** What one more tick of clock is worth: the best urn the plan leaves closed, or a run's own rate when it leaves none. */
	static double valuePerTick(Plan fresh, Pace pace)
	{
		double best = 0;
		for (Plan.Stop s : fresh.getStops())
		{
			Room r = s.getRoom();
			if (r.getUrns() - s.getUrns() > 0)
			{
				best = Math.max(best, r.getUrnXp() / pace.urn(r));
			}
		}
		if (best > 0)
		{
			return best;
		}
		return fresh.getThievingXp() / (double) Math.max(1, Math.min(fresh.getTicksLeft(), fresh.getTicksNeeded()) + Planner.RESTART_TICKS);
	}

	public Room getTop()
	{
		return top;
	}

	/** Every room with enough recent crossings, shallowest first. */
	public List<Leak> getRooms()
	{
		return rooms;
	}

	/** Comparable runs the usual crossings come from. */
	public int getRuns()
	{
		return runs;
	}

	/** The room whose usual crossing gives the most away, or null when none is past the ordinary spread. */
	public Leak biggest()
	{
		Leak best = null;
		for (Leak l : rooms)
		{
			if (l.getGap() >= NOISE && (best == null || l.getXpPerHour() > best.getXpPerHour()))
			{
				best = l;
			}
		}
		return best;
	}

	/** Experience an hour every room's gap costs together. */
	public int total()
	{
		int sum = 0;
		for (Leak l : rooms)
		{
			sum += l.getXpPerHour();
		}
		return sum;
	}

	/** The room the player's own urn times move urns away from, or null when they change nothing. */
	public Room getFasterUrnsFrom()
	{
		return fasterUrnsFrom;
	}

	/** The room those urns move to. */
	public Room getFasterUrnsTo()
	{
		return fasterUrnsTo;
	}

	public int getMovedUrns()
	{
		return movedUrns;
	}

	/** Experience a run the move is worth. */
	public int getUrnGain()
	{
		return urnGain;
	}
}
