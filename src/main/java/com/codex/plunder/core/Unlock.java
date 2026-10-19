package com.codex.plunder.core;

/**
 * A deeper room opening: every eight to ten Thieving levels the pyramid lets the player one room
 * further in, and a run there pays far more. Worth a line in the chat box when it happens, and a
 * heads-up in the run summary when it is only a few runs away.
 */
public final class Unlock
{
	/** The run summary mentions the next room once it is this few runs away. */
	public static final int SOON_RUNS = 3;

	/** A room that has just opened, with what a run paid before and pays now. */
	public static final class Opened
	{
		private final Room room;
		private final int runXpBefore;
		private final int runXpNow;
		private final int rateBefore;
		private final int rateNow;

		Opened(Room room, int runXpBefore, int runXpNow, int rateBefore, int rateNow)
		{
			this.room = room;
			this.runXpBefore = runXpBefore;
			this.runXpNow = runXpNow;
			this.rateBefore = rateBefore;
			this.rateNow = rateNow;
		}

		/** The deepest room now open. */
		public Room getRoom()
		{
			return room;
		}

		public int getRunXpBefore()
		{
			return runXpBefore;
		}

		public int getRunXpNow()
		{
			return runXpNow;
		}

		/** Experience an hour at the old level, the walk back counted. */
		public int getRateBefore()
		{
			return rateBefore;
		}

		public int getRateNow()
		{
			return rateNow;
		}
	}

	private Unlock()
	{
	}

	/**
	 * The room a level rise opened, or null when it opened none. A rise of several levels at once
	 * (a save from long ago) names only the deepest room now open.
	 *
	 * @param before the Thieving level as last seen; 0 when never seen, which opens nothing
	 */
	public static Opened opened(int before, int now, Goal goal, Pace pace)
	{
		if (before <= 0 || now <= before)
		{
			return null;
		}
		Room was = Room.highest(before);
		Room is = Room.highest(now);
		if (is == null || is == was)
		{
			return null;
		}
		int beforeXp = 0;
		int beforeRate = 0;
		if (was != null)
		{
			Plan old = Planner.fresh(before, goal, pace);
			beforeXp = old.getThievingXp();
			beforeRate = old.xpPerHour(Planner.RESTART_TICKS);
		}
		Plan fresh = Planner.fresh(now, goal, pace);
		return new Opened(is, beforeXp, fresh.getThievingXp(), beforeRate, fresh.xpPerHour(Planner.RESTART_TICKS));
	}

	/**
	 * Runs like this one still to go before the next room opens, or 0 when every room is open or
	 * the run paid nothing to go on.
	 */
	public static long runsToNext(int level, long xp, int perRun)
	{
		int next = Room.nextUnlock(level);
		if (next <= 0 || perRun <= 0)
		{
			return 0;
		}
		long left = Math.max(0, Levels.xpFor(next) - xp);
		return Math.max(1, (left + perRun - 1) / perRun);
	}
}
