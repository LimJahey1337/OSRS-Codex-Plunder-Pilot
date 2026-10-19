package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class SceptreTest
{
	@Test
	public void messagesAboutThePharaohsSceptreAreRead()
	{
		assertEquals(Integer.valueOf(23), Sceptre.parse("Your Pharaoh's sceptre has 23 charges left."));
		assertEquals(Integer.valueOf(1), Sceptre.parse("<col=ff0000>Your sceptre has 1 charge left.</col>"));
		assertEquals(Integer.valueOf(0), Sceptre.parse("Your sceptre has no charges left."));
		assertNull(Sceptre.parse("Your Skull sceptre has 4 charges left."));
		assertNull(Sceptre.parse("Your Thammaron's sceptre has 500 charges left."));
		assertNull(Sceptre.parse("You have 3 charges left."));
	}

	@Test
	public void theVariableIsOnlyBelievedOnceAMessageAgreesWithIt()
	{
		Sceptre s = new Sceptre();
		assertEquals(-1, s.charges(40));
		s.heard(12);
		assertEquals(12, s.charges(40));
		s.compare(12);
		assertTrue(s.isLive());
		assertEquals(11, s.charges(11));
	}

	@Test
	public void aVariableThatDisagreesIsNeverBelievedAgain()
	{
		Sceptre s = new Sceptre();
		s.heard(12);
		s.compare(3);
		assertFalse(s.isLive());
		assertEquals(12, s.charges(3));
		s.heard(11);
		s.compare(11);
		assertFalse(s.isLive());
		assertEquals(11, s.charges(99));
	}

	@Test
	public void theReadyCheckSpeaksOfTheSceptre()
	{
		Plan plan = Planner.fresh(91, Goal.EXPERIENCE, new Pace());
		List<Check> empty = Check.ready(new Check.Kit(91, 70, 90, 20, false, false).sceptre(Check.SceptreKit.UNCHARGED, 0, false, Check.SceptreKit.NONE), plan, 6);
		assertTrue(empty.stream().anyMatch(c -> c.getState() == Check.State.WARN && c.getTitle().equals("Sceptre has no charges")));
		List<Check> low = Check.ready(new Check.Kit(91, 70, 90, 20, false, false).sceptre(Check.SceptreKit.CHARGED, 2, true, Check.SceptreKit.NONE), plan, 6);
		assertTrue(low.stream().anyMatch(c -> c.getTitle().equals("Sceptre: 2 charges")));
		List<Check> banked = Check.ready(new Check.Kit(91, 70, 90, 20, false, false).sceptre(Check.SceptreKit.NONE, -1, false, Check.SceptreKit.CHARGED), plan, 6);
		assertTrue(banked.stream().anyMatch(c -> c.getTitle().equals("Bring your sceptre")));
		List<Check> none = Check.ready(new Check.Kit(91, 70, 90, 20, false, false), plan, 6);
		assertFalse(none.stream().anyMatch(c -> c.getTitle().contains("ceptre")));
	}

	private static Check sceptreCheck(Check.SceptreKit carried, int charges, boolean live, Check.SceptreKit banked)
	{
		return Check.sceptre(new Check.Kit(91, 70, 90, 20, false, false).sceptre(carried, charges, live, banked));
	}

	@Test
	public void aChargedSceptreIsNeverCalledEmptyByAStaleCount()
	{
		// The last message said none left, then the sceptre was recharged with no message read:
		// the item is the charged one, so the count of none is out of date, not a reason to warn.
		Check c = sceptreCheck(Check.SceptreKit.CHARGED, 0, false, Check.SceptreKit.NONE);
		assertEquals(Check.State.GOOD, c.getState());
		assertEquals("Pharaoh's sceptre", c.getTitle());
	}

	@Test
	public void aCountKeptFromTheLastCheckSaysSo()
	{
		assertTrue(sceptreCheck(Check.SceptreKit.CHARGED, 8, false, Check.SceptreKit.NONE).getDetail().startsWith("At its last check"));
		assertFalse(sceptreCheck(Check.SceptreKit.CHARGED, 8, true, Check.SceptreKit.NONE).getDetail().contains("last check"));
	}

	@Test
	public void anEmptySceptreInTheBankIsToBeRechargedNotBrought()
	{
		Check c = sceptreCheck(Check.SceptreKit.NONE, -1, false, Check.SceptreKit.UNCHARGED);
		assertEquals("Recharge your sceptre", c.getTitle());
		assertNull(sceptreCheck(Check.SceptreKit.NONE, -1, false, Check.SceptreKit.NONE));
	}

	@Test
	public void theBankIsRememberedWithItsCharge()
	{
		Sceptre s = new Sceptre();
		assertNull(s.getBanked());
		assertTrue(s.setBanked(Check.SceptreKit.UNCHARGED));
		assertEquals(Check.SceptreKit.UNCHARGED, s.getBanked());
		assertFalse(s.setBanked(Check.SceptreKit.UNCHARGED));
		assertTrue(s.setBanked(Check.SceptreKit.CHARGED));
		assertTrue(s.setBanked(Check.SceptreKit.NONE));
		assertEquals(Check.SceptreKit.NONE, s.getBanked());
	}
}
