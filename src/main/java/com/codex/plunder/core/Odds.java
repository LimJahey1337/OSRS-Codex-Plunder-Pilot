package com.codex.plunder.core;

/** Sceptre arithmetic: what a number of chests and sarcophagi should have turned up. */
public final class Odds
{
	private Odds()
	{
	}

	/** The chance of at least one sceptre from these openings, per room. */
	public static double atLeastOne(long[] opened)
	{
		double logMiss = 0;
		for (Room r : Room.values())
		{
			long n = at(opened, r);
			logMiss += n * Math.log1p(-r.sceptreChance());
		}
		return 1 - Math.exp(logMiss);
	}

	/** Sceptres these openings should have turned up on average. */
	public static double expected(long[] opened)
	{
		double sum = 0;
		for (Room r : Room.values())
		{
			sum += at(opened, r) * r.sceptreChance();
		}
		return sum;
	}

	/**
	 * How lucky the player has been, from 0 (as dry as it gets) to 1 (as lucky as it gets): the
	 * share of players opening the same chests who would have found fewer sceptres, counting half
	 * of those who found exactly as many.
	 */
	public static double luck(long[] opened, int found)
	{
		double lambda = expected(opened);
		if (lambda <= 0)
		{
			return 0.5;
		}
		double below = 0;
		double term = Math.exp(-lambda);
		for (int k = 0; k < found; k++)
		{
			below += term;
			term *= lambda / (k + 1);
		}
		return Math.min(1, below + term / 2);
	}

	/** Runs at this per-run chance before the odds of a sceptre reach the given share. */
	public static int runsFor(double perRun, double share)
	{
		if (perRun <= 0)
		{
			return Integer.MAX_VALUE;
		}
		if (perRun >= 1)
		{
			return 1;
		}
		return (int) Math.ceil(Math.log1p(-share) / Math.log1p(-perRun));
	}

	private static long at(long[] opened, Room r)
	{
		return opened == null || opened.length <= r.ordinal() ? 0 : opened[r.ordinal()];
	}
}
