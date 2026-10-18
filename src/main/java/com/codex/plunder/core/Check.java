package com.codex.plunder.core;

import java.util.ArrayList;
import java.util.List;

/** One line of the ready check before a run: a pass, a warning, or a tip. */
public final class Check
{
	/** How a check came out. */
	public enum State
	{
		GOOD,
		WARN,
		TIP
	}

	/** Below this many hitpoints a snake bite and its poison can hurt. */
	public static final int LOW_HP = 12;
	/** A snake charm helps below this Thieving level and gets in the way from it on. */
	static final int CHARM_LEVEL = 51;
	/** Protect from Melee stops the scarab swarm and the mummies. */
	static final int PROTECT_MELEE = 43;
	/** From here the door experience a lockpick halves is worth more than the tries it saves. */
	static final int LOCKPICK_LEVEL = 71;

	private final State state;
	private final String title;
	private final String detail;

	Check(State state, String title, String detail)
	{
		this.state = state;
		this.title = title;
		this.detail = detail;
	}

	public State getState()
	{
		return state;
	}

	public String getTitle()
	{
		return title;
	}

	public String getDetail()
	{
		return detail;
	}

	/** What the player carries and can do, read before a run. */
	public static final class Kit
	{
		final int level;
		final int prayer;
		final int hp;
		final int freeSlots;
		final boolean charm;
		final boolean lockpick;

		public Kit(int level, int prayer, int hp, int freeSlots, boolean charm, boolean lockpick)
		{
			this.level = level;
			this.prayer = prayer;
			this.hp = hp;
			this.freeSlots = freeSlots;
			this.charm = charm;
			this.lockpick = lockpick;
		}
	}

	/**
	 * @param slotsNeeded free slots the run's artefacts are expected to take
	 */
	public static List<Check> ready(Kit kit, Plan plan, int slotsNeeded)
	{
		List<Check> checks = new ArrayList<>();
		if (kit.freeSlots < slotsNeeded)
		{
			checks.add(new Check(State.WARN, kit.freeSlots + (kit.freeSlots == 1 ? " free slot" : " free slots"),
				"A run like this turns up about " + slotsNeeded + " artefacts; a full pack stops the loot"));
		}
		else
		{
			checks.add(new Check(State.GOOD, kit.freeSlots + " free slots", "Room for the " + slotsNeeded + " or so artefacts a run turns up"));
		}
		if (kit.hp <= LOW_HP)
		{
			checks.add(new Check(State.WARN, "Low hitpoints", "Snakes in the urns bite for up to 4 and poison; eat or bring food"));
		}
		if (kit.level < CHARM_LEVEL)
		{
			checks.add(kit.charm
				? new Check(State.GOOD, "Snake charm", "Below 51 it makes urns safer to search")
				: new Check(State.TIP, "Bring a snake charm", "Below 51 Thieving it makes urns safer to search"));
		}
		else if (kit.charm)
		{
			checks.add(new Check(State.TIP, "Leave the snake charm", "From 51 Thieving charming the snake first only slows you down"));
		}
		int doorXp = 0;
		for (Plan.Stop s : plan.getStops())
		{
			if (s.getRoom() != plan.getTop())
			{
				doorXp += s.getRoom().getDoorXp();
			}
		}
		if (kit.level < LOCKPICK_LEVEL)
		{
			checks.add(kit.lockpick
				? new Check(State.GOOD, "Lockpick", "Finds the true door sooner; worth the " + doorXp / 2 + " door xp it halves at your level")
				: new Check(State.TIP, "Bring a lockpick", "Finds the true door sooner; at your level that beats the " + doorXp / 2 + " door xp it costs"));
		}
		else if (kit.lockpick)
		{
			checks.add(new Check(State.TIP, "Leave the lockpick", "It halves door xp, " + doorXp / 2 + " a run here, for few saved tries"));
		}
		if (kit.prayer >= PROTECT_MELEE)
		{
			checks.add(new Check(State.GOOD, "Protect from Melee", "Stops all damage from the scarab swarm and the mummies"));
		}
		return checks;
	}
}
