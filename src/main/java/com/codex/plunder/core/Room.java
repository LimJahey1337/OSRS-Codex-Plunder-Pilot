package com.codex.plunder.core;

/**
 * The eight rooms of the Jalsavrah pyramid, with what each one pays. Every figure is the
 * game's own as recorded by the Old School wiki: urn, chest and door experience are Thieving,
 * the sarcophagus pays Strength, and the sceptre rate is per chest or sarcophagus opened.
 */
public enum Room
{
	ONE(1, 21, 13, 60, 40, 40, 20, 4200),
	TWO(2, 31, 13, 90, 60, 60, 30, 2800),
	THREE(3, 41, 12, 150, 100, 100, 50, 1600),
	FOUR(4, 51, 13, 215, 140, 140, 70, 950),
	FIVE(5, 61, 13, 300, 200, 200, 100, 800),
	SIX(6, 71, 13, 450, 300, 300, 150, 750),
	SEVEN(7, 81, 13, 675, 450, 450, 225, 650),
	EIGHT(8, 91, 13, 825, 550, 0, 275, 650);

	/** Experience for disarming a room's spear trap. */
	public static final int TRAP_XP = 10;
	/** Lowest Thieving level the pyramid lets in. */
	public static final int ENTRY_LEVEL = 21;

	private final int number;
	private final int level;
	private final int urns;
	private final int urnXp;
	private final int chestXp;
	private final int doorXp;
	private final int sarcophagusXp;
	private final int sceptreOneIn;

	Room(int number, int level, int urns, int urnXp, int chestXp, int doorXp, int sarcophagusXp, int sceptreOneIn)
	{
		this.number = number;
		this.level = level;
		this.urns = urns;
		this.urnXp = urnXp;
		this.chestXp = chestXp;
		this.doorXp = doorXp;
		this.sarcophagusXp = sarcophagusXp;
		this.sceptreOneIn = sceptreOneIn;
	}

	public int getNumber()
	{
		return number;
	}

	public int getLevel()
	{
		return level;
	}

	public int getUrns()
	{
		return urns;
	}

	public int getUrnXp()
	{
		return urnXp;
	}

	public int getChestXp()
	{
		return chestXp;
	}

	/** Thieving experience for the true tomb door; the last room has none. */
	public int getDoorXp()
	{
		return doorXp;
	}

	public int getSarcophagusXp()
	{
		return sarcophagusXp;
	}

	public int getSceptreOneIn()
	{
		return sceptreOneIn;
	}

	/** The chance one chest or sarcophagus here holds the sceptre. */
	public double sceptreChance()
	{
		return 1.0 / sceptreOneIn;
	}

	public boolean isLast()
	{
		return this == EIGHT;
	}

	/** @return the room with this number, or null */
	public static Room of(int number)
	{
		return number < 1 || number > 8 ? null : values()[number - 1];
	}

	/** @return the deepest room a Thieving level opens, or null below 21 */
	public static Room highest(int thievingLevel)
	{
		Room best = null;
		for (Room r : values())
		{
			if (thievingLevel >= r.level)
			{
				best = r;
			}
		}
		return best;
	}

	/** @return the next room's entry level, or 0 when every room is open */
	public static int nextUnlock(int thievingLevel)
	{
		for (Room r : values())
		{
			if (thievingLevel < r.level)
			{
				return r.level;
			}
		}
		return 0;
	}
}
