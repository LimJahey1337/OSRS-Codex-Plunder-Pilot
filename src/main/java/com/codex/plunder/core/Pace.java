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
	/** The urn pace in each room, which differs as a room's urns fail more often near its entry level. Older saves have none. */
	private double[] urnByRoom;
	private int[] urnSamplesByRoom;

	public double urn()
	{
		return urnSamples >= TRUSTED ? urn : URN;
	}

	/**
	 * The urn pace in one room: the player's own there once it is settled, otherwise the pace
	 * across every room.
	 */
	public double urn(Room room)
	{
		int i = room == null ? -1 : room.ordinal();
		if (i >= 0 && urnByRoom != null && urnSamplesByRoom != null && i < urnByRoom.length && i < urnSamplesByRoom.length
			&& urnSamplesByRoom[i] >= TRUSTED)
		{
			return urnByRoom[i];
		}
		return urn();
	}

	/** Urn searches timed in one room. */
	public int getUrnSamples(Room room)
	{
		int i = room == null ? -1 : room.ordinal();
		return i < 0 || urnSamplesByRoom == null || i >= urnSamplesByRoom.length ? 0 : urnSamplesByRoom[i];
	}

	/** Whether the room's own urn pace is settled and used. */
	public boolean knowsUrns(Room room)
	{
		return getUrnSamples(room) >= TRUSTED;
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
		urnTook(null, ticks);
	}

	/** One urn in this room took this many ticks, from the last search to this one. */
	public void urnTook(Room room, int ticks)
	{
		if (ticks < 3 || ticks > 40)
		{
			return;
		}
		urn = blend(urn, ticks, urnSamples++);
		if (room != null)
		{
			int n = Room.values().length;
			if (urnByRoom == null || urnByRoom.length != n || urnSamplesByRoom == null || urnSamplesByRoom.length != n)
			{
				urnByRoom = new double[n];
				urnSamplesByRoom = new int[n];
			}
			int i = room.ordinal();
			urnByRoom[i] = blend(urnByRoom[i], ticks, urnSamplesByRoom[i]++);
		}
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

	/** This pace with every room's urns at the one pace across rooms, as before rooms were told apart. */
	public Pace flat()
	{
		Pace p = new Pace();
		p.urn = urn;
		p.chest = chest;
		p.sarcophagus = sarcophagus;
		p.pass = pass;
		p.urnSamples = urnSamples;
		p.passSamples = passSamples;
		return p;
	}

	/** Puts back defaults that an older or damaged save left out. */
	Pace repair()
	{
		if (!(urn > 0 && urn < Planner.RUN_TICKS))
		{
			urn = URN;
		}
		if (!(chest > 0 && chest < Planner.RUN_TICKS))
		{
			chest = CHEST;
		}
		if (!(sarcophagus > 0 && sarcophagus < Planner.RUN_TICKS))
		{
			sarcophagus = SARCOPHAGUS;
		}
		if (!(pass > 0 && pass < Planner.RUN_TICKS))
		{
			pass = PASS;
		}
		urnSamples = Math.max(0, urnSamples);
		passSamples = Math.max(0, passSamples);
		int n = Room.values().length;
		if (urnByRoom != null || urnSamplesByRoom != null)
		{
			double[] rates = new double[n];
			int[] samples = new int[n];
			for (int i = 0; i < n; i++)
			{
				double r = urnByRoom != null && i < urnByRoom.length ? urnByRoom[i] : 0;
				int c = urnSamplesByRoom != null && i < urnSamplesByRoom.length ? urnSamplesByRoom[i] : 0;
				// A room's pace that is out of range, or has no count beside it, starts again.
				boolean ok = r >= 3 && r <= 40 && c > 0;
				rates[i] = ok ? r : 0;
				samples[i] = ok ? c : 0;
			}
			urnByRoom = rates;
			urnSamplesByRoom = samples;
		}
		return this;
	}
}
