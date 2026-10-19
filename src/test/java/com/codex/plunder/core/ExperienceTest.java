package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ExperienceTest
{
	@Test
	public void statsNotSentYetAreNotAStart()
	{
		// Straight after login every skill reads 0, Hitpoints too.
		assertFalse(Experience.sent(0, 0));
	}

	@Test
	public void aRealZeroIsAStartOnceHitpointsHasArrived()
	{
		// Turned on mid-session on an account that never trained Strength: 0 is where it starts,
		// so the first sarcophagus counts.
		assertTrue(Experience.sent(0, 1_154));
		Integer start = 0;
		assertEquals(54, Experience.gained(start, 54));
	}

	@Test
	public void anyExperienceIsAStart()
	{
		assertTrue(Experience.sent(83, 0));
	}

	@Test
	public void noStartOrNoRiseIsNoGain()
	{
		assertEquals(0, Experience.gained(null, 5_000));
		assertEquals(0, Experience.gained(5_000, 5_000));
		assertEquals(0, Experience.gained(5_000, 4_000));
		assertEquals(250, Experience.gained(5_000, 5_250));
	}
}
