package com.codex.plunder.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PyramidCardTest
{
	private static final int TOP = 40;
	private static final int BASE = 184;

	@Test
	public void levelsClimbATierEveryTenAndTheLastToNinetyNine()
	{
		// 8 tiers of 18 pixels: 21 at the base, 31 a tier up, 91 at the foot of the top tier, 99 at its top.
		assertEquals(BASE, PyramidCard.levelY(21, TOP, BASE));
		assertEquals(BASE - 18, PyramidCard.levelY(31, TOP, BASE));
		assertEquals(BASE - 7 * 18, PyramidCard.levelY(91, TOP, BASE));
		assertEquals(TOP, PyramidCard.levelY(99, TOP, BASE));
		assertEquals(BASE - 7 * 18 - 9, PyramidCard.levelY(95, TOP, BASE));
		// Below the entry level sits at the base; nothing goes past the top.
		assertEquals(BASE, PyramidCard.levelY(5, TOP, BASE));
		assertEquals(TOP, PyramidCard.levelY(120, TOP, BASE));
	}

	@Test
	public void marksTooCloseAreNudgedApartInLevelOrder()
	{
		List<PyramidCard.Mark> marks = new ArrayList<>(Arrays.asList(
			new PyramidCard.Mark(72, "72", false), new PyramidCard.Mark(70, "70", true), new PyramidCard.Mark(71, "71", false)));
		PyramidCard.spread(marks, TOP, BASE, 11);
		assertEquals(70, marks.get(0).level);
		assertEquals(PyramidCard.levelY(70, TOP, BASE), marks.get(0).y);
		for (int i = 1; i < marks.size(); i++)
		{
			assertTrue(marks.get(i - 1).y - marks.get(i).y >= 11);
		}
	}

	@Test
	public void marksCrowdedAtTheTopStayInside()
	{
		List<PyramidCard.Mark> marks = new ArrayList<>(Arrays.asList(
			new PyramidCard.Mark(96, "96", true), new PyramidCard.Mark(97, "97", false), new PyramidCard.Mark(98, "98", false),
			new PyramidCard.Mark(99, "99", false)));
		PyramidCard.spread(marks, TOP, BASE, 11);
		assertEquals(TOP, marks.get(3).y);
		for (int i = 1; i < marks.size(); i++)
		{
			assertTrue(marks.get(i - 1).y - marks.get(i).y >= 11);
			assertTrue(marks.get(i).y >= TOP);
		}
	}
}
