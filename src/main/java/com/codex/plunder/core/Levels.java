package com.codex.plunder.core;

/** The experience table, for how far a goal level is. */
public final class Levels
{
	private static final int[] XP = new int[127];
	/** The highest level a skill shows. */
	public static final int MAX = 99;
	/** The most experience a skill can hold. */
	public static final long MAX_XP = 200_000_000;

	static
	{
		double points = 0;
		for (int level = 1; level < XP.length; level++)
		{
			XP[level] = (int) Math.floor(points / 4);
			points += Math.floor(level + 300 * Math.pow(2, level / 7.0));
		}
	}

	private Levels()
	{
	}

	/** Experience at the start of a level, 1 to 126. */
	public static int xpFor(int level)
	{
		return XP[Math.max(1, Math.min(XP.length - 1, level))];
	}

	public static int levelFor(long xp)
	{
		int level = 1;
		while (level < 99 && XP[level + 1] <= xp)
		{
			level++;
		}
		return level;
	}
}
