package com.codex.plunder.core;

/** The one thing to do next, worked out from the plan and the room as it stands. */
public final class Step
{
	/** What kind of thing it is; the overlay highlights by kind. */
	public enum Kind
	{
		ENTER,
		TRAP,
		URNS,
		CHEST,
		SARCOPHAGUS,
		DOOR,
		LEAVE,
		FINISH
	}

	private final Kind kind;
	private final String title;
	private final String detail;

	private Step(Kind kind, String title, String detail)
	{
		this.kind = kind;
		this.title = title;
		this.detail = detail;
	}

	public Kind getKind()
	{
		return kind;
	}

	public String getTitle()
	{
		return title;
	}

	public String getDetail()
	{
		return detail;
	}

	/**
	 * @param plan the plan for the rest of the run
	 * @param inRun whether a run is under way; before one, the step is to go in
	 * @param trapActive the room's spear trap is still to be passed
	 * @param doorsTried how many of the room's four tomb doors have been tried
	 */
	public static Step next(Plan plan, boolean inRun, boolean trapActive, int doorsTried)
	{
		Plan.Stop stop = plan.current();
		if (!inRun || stop == null)
		{
			return new Step(Kind.ENTER, "Enter the pyramid", enterDetail(plan));
		}
		Room room = stop.getRoom();
		if (trapActive)
		{
			return new Step(Kind.TRAP, "Pass the spear trap", stop.isPassThrough() && !room.equals(plan.getTop())
				? "Nothing in room " + room.getNumber() + " is worth the time: straight through to the door"
				: "Then " + work(stop));
		}
		if (stop.getUrns() > 0)
		{
			return new Step(Kind.URNS, "Search " + stop.getUrns() + (stop.getUrns() == 1 ? " urn" : " urns"),
				urnDetail(stop, plan));
		}
		// Chests and sarcophagi last, so a scarab swarm or a mummy has the least time to chase.
		if (stop.isChest())
		{
			return new Step(Kind.CHEST, "Open the grand gold chest",
				"1 in " + room.getSceptreOneIn() + " for the sceptre here" + (stop.isSarcophagus() ? ", then the sarcophagus" : ""));
		}
		if (stop.isSarcophagus())
		{
			return new Step(Kind.SARCOPHAGUS, "Open the sarcophagus",
				"1 in " + room.getSceptreOneIn() + " for the sceptre, and Strength experience");
		}
		if (room != plan.getTop())
		{
			String tried = doorsTried <= 0 ? "One of the four tomb doors is real" : doorsTried + " of 4 tried, " + (4 - doorsTried) + " left";
			return new Step(Kind.DOOR, "Find the tomb door", tried + ". Room " + (room.getNumber() + 1) + " is next");
		}
		if (plan.spare() < Planner.RESTART_TICKS)
		{
			return new Step(Kind.FINISH, "Time is nearly up", "Too little left for another search; the run ends on its own");
		}
		return new Step(Kind.LEAVE, "Leave and begin a new run", "Everything worth doing here is done; a fresh run pays more than the time left");
	}

	private static String work(Plan.Stop stop)
	{
		StringBuilder text = new StringBuilder();
		if (stop.getUrns() > 0)
		{
			text.append(stop.getUrns()).append(stop.getUrns() == 1 ? " urn" : " urns");
		}
		if (stop.isChest())
		{
			text.append(text.length() > 0 ? ", " : "").append("the chest");
		}
		if (stop.isSarcophagus())
		{
			text.append(text.length() > 0 ? ", " : "").append("the sarcophagus");
		}
		return text.length() == 0 ? "the tomb door" : text.toString();
	}

	private static String urnDetail(Plan.Stop stop, Plan plan)
	{
		int skip = stop.getUrnsLeft() - stop.getUrns();
		String pay = stop.getRoom().getUrnXp() + " xp each";
		if (skip > 0)
		{
			return pay + "; leave the other " + skip + ", " + (stop.getRoom() == plan.getTop() ? "the clock is short" : "deeper rooms pay more");
		}
		return pay + (stop.isChest() ? ", then the chest" : "");
	}

	private static String enterDetail(Plan plan)
	{
		Room top = plan.getTop();
		int looted = 0;
		Room firstLooted = null;
		for (Plan.Stop s : plan.getStops())
		{
			if (s.getUrns() > 0)
			{
				looted++;
				if (firstLooted == null)
				{
					firstLooted = s.getRoom();
				}
			}
		}
		if (firstLooted == null)
		{
			return "Run through to room " + top.getNumber();
		}
		if (firstLooted == top)
		{
			return "Run through to room " + top.getNumber() + " and search its urns";
		}
		if (looted == 1 || firstLooted.getNumber() == 1)
		{
			return "Search urns in rooms " + firstLooted.getNumber() + " to " + top.getNumber();
		}
		return "Walk through rooms 1 to " + (firstLooted.getNumber() - 1) + ", search urns from room " + firstLooted.getNumber();
	}
}
