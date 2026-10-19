package com.codex.plunder.core;

/**
 * When a skill's experience can be taken as the start that gains are counted from. Until the game
 * has sent the stats after a login every skill reads 0, and taking that as the start would count
 * the real total as one huge gain. A real 0 is possible, though: an account that has never trained
 * Strength. Hitpoints is never below 1,154 experience once the stats have arrived, so it tells the
 * two apart, and a 0 read alongside it is a real 0.
 */
public final class Experience
{
	/** Hitpoints at level 10, where every account starts. */
	static final int HITPOINTS_START = 1_154;

	private Experience()
	{
	}

	/**
	 * @param xp the skill's experience as the client has it now
	 * @param hitpointsXp Hitpoints experience as the client has it now
	 * @return whether the skill's experience has been sent and can be counted from
	 */
	public static boolean sent(int xp, int hitpointsXp)
	{
		return xp > 0 || hitpointsXp >= HITPOINTS_START;
	}

	/**
	 * @param before the experience last counted from, or null before there is a start
	 * @param now the experience now
	 * @return the experience gained since, 0 for none or for no start
	 */
	public static int gained(Integer before, int now)
	{
		return before == null || now <= before ? 0 : now - before;
	}
}
