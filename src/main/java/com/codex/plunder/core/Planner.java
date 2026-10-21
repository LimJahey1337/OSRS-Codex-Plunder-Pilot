package com.codex.plunder.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Plans the rest of a run against the clock. Getting through each room costs a fixed time;
 * what is left is shared out among the urns, chests and sarcophagi by what the goal values most
 * per tick, so the deepest rooms' urns win and the shallow rooms are walked straight through.
 * It is planned again every tick, so a slow room simply shortens what follows.
 */
public final class Planner
{
	/** A run lasts five minutes. */
	public static final int RUN_TICKS = 500;
	/** Leaving, talking to the mummy and getting back in, between runs. */
	public static final int RESTART_TICKS = 60;
	static final double TICKS_PER_HOUR = 6000;
	/** Ticks held back so the last search is not cut off by the clock. */
	static final int MARGIN = 10;

	private Planner()
	{
	}

	private enum Kind
	{
		URN,
		CHEST,
		SARCOPHAGUS
	}

	private static final class Option
	{
		final Room room;
		final Kind kind;
		final int tier;
		final double ticks;
		final double density;

		Option(Room room, Kind kind, int tier, double ticks, double value)
		{
			this.room = room;
			this.kind = kind;
			this.tier = tier;
			this.ticks = ticks;
			this.density = value / ticks;
		}
	}

	/**
	 * @param level the player's real Thieving level
	 * @param here where the run stands, or null for a run not yet begun
	 * @param ticksLeft ticks on the clock
	 * @return the plan, or null below the pyramid's entry level
	 */
	public static Plan plan(int level, Goal goal, Pace pace, Here here, int ticksLeft)
	{
		Room top = Room.highest(level);
		if (top == null)
		{
			return null;
		}
		if (here == null)
		{
			here = Here.start();
		}
		Room start = here.getRoom().ordinal() > top.ordinal() ? top : here.getRoom();

		double fixed = 0;
		int xp = 0;
		List<Option> options = new ArrayList<>();
		for (int i = start.ordinal(); i <= top.ordinal(); i++)
		{
			Room room = Room.values()[i];
			boolean first = room == start;
			boolean trapDone = first && here.isTrapDone();
			if (!trapDone)
			{
				xp += Room.TRAP_XP;
			}
			if (room == top)
			{
				fixed += trapDone ? 0 : pace.trap();
			}
			else
			{
				fixed += trapDone ? pace.pass() - pace.trap() : pace.pass();
				xp += room.getDoorXp();
			}

			int urns = first ? here.getUrnsLeft() : room.getUrns();
			for (int u = 0; u < urns; u++)
			{
				options.add(new Option(room, Kind.URN, 1, pace.urn(room), room.getUrnXp()));
			}
			if (!(first && here.isChestOpen()))
			{
				if (goal == Goal.SCEPTRE)
				{
					options.add(new Option(room, Kind.CHEST, 0, pace.chest(), room.sceptreChance()));
				}
				else
				{
					options.add(new Option(room, Kind.CHEST, room.getNumber() >= 4 ? 0 : 1, pace.chest(), room.getChestXp()));
				}
			}
			// Sarcophagi are slow; only a sceptre hunter wants them, and only where the odds are good.
			if (goal == Goal.SCEPTRE && room.getNumber() >= 4 && !(first && here.isSarcophagusOpen()))
			{
				options.add(new Option(room, Kind.SARCOPHAGUS, 0, pace.sarcophagus(), room.sceptreChance()));
			}
		}

		options.sort(Comparator.<Option>comparingInt(o -> o.tier).thenComparing(o -> -o.density));
		double budget = ticksLeft - fixed - MARGIN;
		double used = fixed;
		int[] urns = new int[Room.values().length];
		boolean[] chest = new boolean[urns.length];
		boolean[] sarcophagus = new boolean[urns.length];
		int strength = 0;
		double miss = 1;
		for (Option o : options)
		{
			if (o.ticks > budget)
			{
				continue;
			}
			budget -= o.ticks;
			used += o.ticks;
			int r = o.room.ordinal();
			switch (o.kind)
			{
				case URN:
					urns[r]++;
					xp += o.room.getUrnXp();
					break;
				case CHEST:
					chest[r] = true;
					xp += o.room.getChestXp();
					miss *= 1 - o.room.sceptreChance();
					break;
				case SARCOPHAGUS:
					sarcophagus[r] = true;
					strength += o.room.getSarcophagusXp();
					miss *= 1 - o.room.sceptreChance();
					break;
			}
		}

		List<Plan.Stop> stops = new ArrayList<>();
		for (int i = start.ordinal(); i <= top.ordinal(); i++)
		{
			Room room = Room.values()[i];
			boolean first = room == start;
			stops.add(new Plan.Stop(room, urns[i], first ? here.getUrnsLeft() : room.getUrns(), chest[i], sarcophagus[i],
				first && here.isChestOpen(), first && here.isSarcophagusOpen()));
		}
		return new Plan(goal, top, stops, xp, strength, 1 - miss, (int) Math.ceil(used), ticksLeft);
	}

	/** A whole fresh run at this level, for the ladder and the forecasts. */
	public static Plan fresh(int level, Goal goal, Pace pace)
	{
		return plan(level, goal, pace, null, RUN_TICKS);
	}
}
