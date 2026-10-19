package com.codex.plunder.core;

/** One finished trip into the pyramid. */
public final class Run
{
	private long at;
	private int ticks;
	private int deepest;
	private int thievingXp;
	private int strengthXp;
	private int urns;
	private int chests;
	private int sarcophagi;
	private long artefacts;
	private int artefactCount;
	private double sceptreChance;
	private boolean sceptre;
	private Goal goal;
	/** Ticks from the start of the run to entering each room, by room; -1 where not seen. Older saves have none. */
	private int[] splits;

	Run()
	{
	}

	Run(long at, int ticks, int deepest, int thievingXp, int strengthXp, int urns, int chests, int sarcophagi,
		long artefacts, int artefactCount, double sceptreChance, boolean sceptre, Goal goal)
	{
		this(at, ticks, deepest, thievingXp, strengthXp, urns, chests, sarcophagi, artefacts, artefactCount, sceptreChance, sceptre, goal, null);
	}

	Run(long at, int ticks, int deepest, int thievingXp, int strengthXp, int urns, int chests, int sarcophagi,
		long artefacts, int artefactCount, double sceptreChance, boolean sceptre, Goal goal, int[] splits)
	{
		this.splits = splits == null ? null : splits.clone();
		this.artefactCount = artefactCount;
		this.at = at;
		this.ticks = ticks;
		this.deepest = deepest;
		this.thievingXp = thievingXp;
		this.strengthXp = strengthXp;
		this.urns = urns;
		this.chests = chests;
		this.sarcophagi = sarcophagi;
		this.artefacts = artefacts;
		this.sceptreChance = sceptreChance;
		this.sceptre = sceptre;
		this.goal = goal;
	}

	/** When the run began, in epoch milliseconds. */
	public long getAt()
	{
		return at;
	}

	public int getTicks()
	{
		return ticks;
	}

	/** The deepest room reached. */
	public int getDeepest()
	{
		return deepest;
	}

	public int getThievingXp()
	{
		return thievingXp;
	}

	public int getStrengthXp()
	{
		return strengthXp;
	}

	public int getUrns()
	{
		return urns;
	}

	public int getChests()
	{
		return chests;
	}

	public int getSarcophagi()
	{
		return sarcophagi;
	}

	/** What the artefacts picked up were worth at Grand Exchange prices. */
	public long getArtefacts()
	{
		return artefacts;
	}

	/** How many artefacts were picked up, each taking a slot. */
	public int getArtefactCount()
	{
		return artefactCount;
	}

	/** The chance this run's openings had of a sceptre. */
	public double getSceptreChance()
	{
		return sceptreChance;
	}

	public boolean isSceptre()
	{
		return sceptre;
	}

	void markSceptre()
	{
		sceptre = true;
	}

	public Goal getGoal()
	{
		return goal == null ? Goal.EXPERIENCE : goal;
	}

	/**
	 * Ticks from the start of the run to entering the room, or -1 when the run never got there,
	 * entered it before the plugin was watching, or was saved before splits were kept.
	 */
	public int splitAt(Room room)
	{
		return splits == null || room == null || room.ordinal() >= splits.length ? -1 : splits[room.ordinal()];
	}
}
