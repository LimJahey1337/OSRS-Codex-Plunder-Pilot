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
	private double sceptreChance;
	private boolean sceptre;
	private Goal goal;

	Run()
	{
	}

	Run(long at, int ticks, int deepest, int thievingXp, int strengthXp, int urns, int chests, int sarcophagi,
		long artefacts, double sceptreChance, boolean sceptre, Goal goal)
	{
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
}
