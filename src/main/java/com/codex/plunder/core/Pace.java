package com.codex.plunder.core;

/**
 * How long things take, in game ticks. It starts from typical times and learns the player's own
 * from their runs, so the plan fits the hands that play it.
 */
public final class Pace
{
	/** A search, with the odd failure and the walk to the next urn. */
	static final double URN = 10;
	static final double CHEST = 6;
	static final double SARCOPHAGUS = 12;
	/** Disarming the spear trap, finding the true door among four and walking through. */
	static final double PASS = 25;
	/** Only the spear trap, for the room the run ends in. */
	static final double TRAP = 6;
	/** Weight of each new measurement once a pace is settled. */
	private static final double LEARN = 1 / 8.0;
	/** Measurements before the learned pace is used. */
	public static final int TRUSTED = 6;

	private double urn = URN;
	private double chest = CHEST;
	private double sarcophagus = SARCOPHAGUS;
	private double pass = PASS;
	private int urnSamples;
	private int passSamples;

	public double urn()
	{
		return urnSamples >= TRUSTED ? urn : URN;
	}

	public double chest()
	{
		return chest;
	}

	public double sarcophagus()
	{
		return sarcophagus;
	}

	public double pass()
	{
		return passSamples >= TRUSTED ? pass : PASS;
	}

	public double trap()
	{
		return TRAP;
	}

	public int getUrnSamples()
	{
		return urnSamples;
	}

	public int getPassSamples()
	{
		return passSamples;
	}

	/** One urn took this many ticks, from the last search to this one. */
	public void urnTook(int ticks)
	{
		if (ticks < 3 || ticks > 40)
		{
			return;
		}
		urn = blend(urn, ticks, urnSamples++);
	}

	/** Getting through one room, from its last search to the next room, took this many ticks. */
	public void passTook(int ticks)
	{
		if (ticks < 8 || ticks > 120)
		{
			return;
		}
		pass = blend(pass, ticks, passSamples++);
	}

	/** A plain mean over the first few measurements, then a slow moving average. */
	private static double blend(double current, double sample, int seen)
	{
		if (seen == 0)
		{
			return sample;
		}
		double weight = seen < 8 ? 1.0 / (seen + 1) : LEARN;
		return current + (sample - current) * weight;
	}

	/** Puts back defaults that an older or damaged save left out. */
	Pace repair()
	{
		if (!(urn > 0))
		{
			urn = URN;
		}
		if (!(chest > 0))
		{
			chest = CHEST;
		}
		if (!(sarcophagus > 0))
		{
			sarcophagus = SARCOPHAGUS;
		}
		if (!(pass > 0))
		{
			pass = PASS;
		}
		return this;
	}
}
