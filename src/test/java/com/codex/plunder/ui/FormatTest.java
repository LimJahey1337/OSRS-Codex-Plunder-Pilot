package com.codex.plunder.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FormatTest
{
	@Test
	public void amountsSideBySideKeepTheSamePrecision()
	{
		assertEquals("8,935", Format.amount(8_935));
		assertEquals("95.7k", Format.amount(95_700));
		// The past sessions read "95k" beside "95.1k" before: a round rate keeps its zero.
		assertEquals("95.0k", Format.amount(95_000));
		assertEquals("246k", Format.amount(246_000));
		assertEquals("12.2M", Format.amount(12_200_000));
		assertEquals("1.00M", Format.amount(999_600));
		assertEquals("100k", Format.amount(99_960));
		assertEquals("+9,375", Format.signed(9_375));
	}
}
