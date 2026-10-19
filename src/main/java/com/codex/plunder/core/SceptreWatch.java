package com.codex.plunder.core;

/**
 * Tells a Pharaoh's sceptre found from one that only turned up in the counts. The sceptres carried
 * and worn are counted once a tick; a sceptre found is one more of them than the tick before. One
 * moved between hand and pack, or emptied by its last teleport, is the same sceptre under another
 * id and does not count, and nor does the pack arriving after a login or a hop: until it has, the
 * count is unknown, not zero.
 *
 * <p>Finding the sceptre throws the player out of the pyramid, so it can land in the pack a moment
 * after the run ends. Within {@link #GRACE_TICKS} of a run's end a new sceptre is credited to it;
 * a run ended by logging out or hopping was not thrown out by one.</p>
 */
public final class SceptreWatch
{
	/** What a tick's count showed. */
	public enum Seen
	{
		NOTHING,
		/** One more sceptre during a run. */
		FOUND,
		/** One more sceptre just after a run ended. */
		FOUND_AFTER,
		/** One more sceptre away from any run: bought, or taken from the bank. */
		OTHER
	}

	/** About six seconds after a run ends. */
	public static final int GRACE_TICKS = 10;

	private int last = -1;
	private int endedAt = Integer.MIN_VALUE;

	/** The pack is about to be sent afresh (login, hop): forget what it held and any run just ended. */
	public void reset()
	{
		last = -1;
		endedAt = Integer.MIN_VALUE;
	}

	/** A run ended on this tick, by the clock or the sceptre. */
	public void runEnded(int tick)
	{
		endedAt = tick;
	}

	/**
	 * @param count sceptres carried and worn, or -1 while the pack has not arrived
	 * @param inRun a run is under way
	 * @param tick the game tick
	 */
	public Seen tick(int count, boolean inRun, int tick)
	{
		int before = last;
		last = count;
		if (before < 0 || count <= before)
		{
			return Seen.NOTHING;
		}
		if (inRun)
		{
			return Seen.FOUND;
		}
		return endedAt != Integer.MIN_VALUE && tick - endedAt >= 0 && tick - endedAt <= GRACE_TICKS ? Seen.FOUND_AFTER : Seen.OTHER;
	}
}
