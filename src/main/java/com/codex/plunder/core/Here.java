package com.codex.plunder.core;

/** Where a run stands: the room the player is in and what is already done there. */
public final class Here
{
	private final Room room;
	private final int urnsLeft;
	private final boolean chestOpen;
	private final boolean sarcophagusOpen;
	private final boolean trapDone;

	public Here(Room room, int urnsLeft, boolean chestOpen, boolean sarcophagusOpen, boolean trapDone)
	{
		this.room = room;
		this.urnsLeft = Math.max(0, Math.min(room.getUrns(), urnsLeft));
		this.chestOpen = chestOpen;
		this.sarcophagusOpen = sarcophagusOpen;
		this.trapDone = trapDone;
	}

	/** The start of a fresh run, outside room one's spear trap. */
	public static Here start()
	{
		return new Here(Room.ONE, Room.ONE.getUrns(), false, false, false);
	}

	public Room getRoom()
	{
		return room;
	}

	public int getUrnsLeft()
	{
		return urnsLeft;
	}

	public boolean isChestOpen()
	{
		return chestOpen;
	}

	public boolean isSarcophagusOpen()
	{
		return sarcophagusOpen;
	}

	public boolean isTrapDone()
	{
		return trapDone;
	}
}
