package com.codex.plunder.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class OlderSessionsTest
{
	@Test
	public void oneOlderSessionIsSingular()
	{
		// Nine sessions kept and eight listed used to read "1 older sessions not shown".
		assertEquals("1 older session not shown", PilotPanel.olderNote(1));
		assertEquals("3 older sessions not shown", PilotPanel.olderNote(3));
	}
}
