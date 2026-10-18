package com.codex.plunder.core;

import java.util.Collections;
import java.util.List;

/** What to do in each room still ahead this run, and what that is expected to earn. */
public final class Plan
{
	/** One room's share of the plan. */
	public static final class Stop
	{
		private final Room room;
		private final int urns;
		private final int urnsLeft;
		private final boolean chest;
		private final boolean sarcophagus;
		private final boolean chestOpen;
		private final boolean sarcophagusOpen;

		Stop(Room room, int urns, int urnsLeft, boolean chest, boolean sarcophagus, boolean chestOpen, boolean sarcophagusOpen)
		{
			this.room = room;
			this.urns = urns;
			this.urnsLeft = urnsLeft;
			this.chest = chest;
			this.sarcophagus = sarcophagus;
			this.chestOpen = chestOpen;
			this.sarcophagusOpen = sarcophagusOpen;
		}

		public Room getRoom()
		{
			return room;
		}

		/** Urns worth searching here, not counting those already searched. */
		public int getUrns()
		{
			return urns;
		}

		/** Urns still closed here. */
		public int getUrnsLeft()
		{
			return urnsLeft;
		}

		/** The grand gold chest is worth opening. */
		public boolean isChest()
		{
			return chest;
		}

		public boolean isSarcophagus()
		{
			return sarcophagus;
		}

		public boolean isChestOpen()
		{
			return chestOpen;
		}

		public boolean isSarcophagusOpen()
		{
			return sarcophagusOpen;
		}

		/** Nothing here is worth the time: pass straight through. */
		public boolean isPassThrough()
		{
			return urns == 0 && !chest && !sarcophagus;
		}
	}

	private final Goal goal;
	private final Room top;
	private final List<Stop> stops;
	private final int thievingXp;
	private final int strengthXp;
	private final double sceptreChance;
	private final int ticksNeeded;
	private final int ticksLeft;

	Plan(Goal goal, Room top, List<Stop> stops, int thievingXp, int strengthXp, double sceptreChance, int ticksNeeded, int ticksLeft)
	{
		this.goal = goal;
		this.top = top;
		this.stops = Collections.unmodifiableList(stops);
		this.thievingXp = thievingXp;
		this.strengthXp = strengthXp;
		this.sceptreChance = sceptreChance;
		this.ticksNeeded = ticksNeeded;
		this.ticksLeft = ticksLeft;
	}

	public Goal getGoal()
	{
		return goal;
	}

	/** The deepest room this level opens, where the run ends. */
	public Room getTop()
	{
		return top;
	}

	public List<Stop> getStops()
	{
		return stops;
	}

	/** The stop for the room the player is in, or the first ahead. */
	public Stop current()
	{
		return stops.isEmpty() ? null : stops.get(0);
	}

	public Stop stop(Room room)
	{
		for (Stop s : stops)
		{
			if (s.room == room)
			{
				return s;
			}
		}
		return null;
	}

	/** Thieving experience the rest of the plan is expected to earn. */
	public int getThievingXp()
	{
		return thievingXp;
	}

	public int getStrengthXp()
	{
		return strengthXp;
	}

	/** The chance the rest of the plan turns up a sceptre. */
	public double getSceptreChance()
	{
		return sceptreChance;
	}

	public int getTicksNeeded()
	{
		return ticksNeeded;
	}

	public int getTicksLeft()
	{
		return ticksLeft;
	}

	/** Ticks the plan leaves unused; with plenty spare it is quicker to leave and start afresh. */
	public int spare()
	{
		return Math.max(0, ticksLeft - ticksNeeded);
	}

	/** Thieving experience an hour if every run went like this one, counting the walk back in. */
	public int xpPerHour(int restartTicks)
	{
		int used = Math.max(1, Math.min(ticksLeft, ticksNeeded) + restartTicks);
		return (int) Math.round(thievingXp * (Planner.TICKS_PER_HOUR / used));
	}

	/** A whole run's worth of sceptre chances in an hour, as the chance of at least one. */
	public double sceptrePerHour(int restartTicks)
	{
		int used = Math.max(1, Math.min(ticksLeft, ticksNeeded) + restartTicks);
		double runs = Planner.TICKS_PER_HOUR / used;
		return 1 - Math.pow(1 - sceptreChance, runs);
	}
}
