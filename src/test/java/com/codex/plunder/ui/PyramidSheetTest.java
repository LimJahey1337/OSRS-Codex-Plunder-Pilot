package com.codex.plunder.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.codex.plunder.core.Goal;
import com.codex.plunder.core.Pace;
import com.codex.plunder.core.Plan;
import com.codex.plunder.core.Planner;
import com.codex.plunder.core.Room;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class PyramidSheetTest
{
	private static View view(int level, View.Record record)
	{
		Plan fresh = Planner.fresh(level, Goal.EXPERIENCE, new Pace());
		List<Plan> ladder = new ArrayList<>();
		for (Room room : Room.values())
		{
			ladder.add(Planner.fresh(room.getLevel(), Goal.EXPERIENCE, new Pace()));
		}
		return new View(View.State.READY, "Zezima", level, 0, 99, Goal.EXPERIENCE, fresh, fresh, ladder, null, null, record, 0);
	}

	@Test
	public void aFirstLookWithNoRunsStillFillsTheColumn()
	{
		List<PyramidSheet.Fact> facts = PyramidSheet.facts(view(64, null));
		assertEquals(4, facts.size());
		assertEquals("forecast", facts.get(0).note);
		assertEquals("none logged yet", facts.get(1).note);
		assertEquals("Sceptre", facts.get(2).label);
		// No session to be best yet: the next room to open stands in.
		assertEquals("Room 6", facts.get(3).value);
		assertEquals("opens at 71", facts.get(3).note);
	}

	@Test
	public void atTheTopTheLastFigureSaysEveryRoomIsOpen()
	{
		List<PyramidSheet.Fact> facts = PyramidSheet.facts(view(95, null));
		assertEquals("All 8", facts.get(3).value);
	}

	@Test
	public void theNameIsLeftOffWhenAsked()
	{
		assertEquals("ZEZIMA'S PYRAMID", PyramidSheet.kicker("Zezima"));
		assertEquals("PYRAMID PLUNDER", PyramidSheet.kicker(null));
		BufferedImage card = PyramidSheet.render(view(64, null).nameOnCard(false));
		assertEquals(PyramidSheet.WIDTH, card.getWidth());
		assertTrue(card.getHeight() > 200);
	}
}
