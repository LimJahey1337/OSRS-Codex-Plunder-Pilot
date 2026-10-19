package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;

public class NavigatorGoalsTest
{
	/** A board as Codex Navigator writes it (GoalBoard, version 1), extra fields and all. */
	private static final String BOARD = "{\"version\":1,\"focus\":\"quest:Desert Treasure II - The Fallen Empire\",\"goals\":["
		+ "{\"key\":\"diary:Ardougne Hard\",\"type\":\"diary\",\"title\":\"Ardougne Hard\",\"complete\":false,\"progress\":0.4,"
		+ "\"next\":\"Train Thieving to 72\",\"hoursLeft\":6.5,\"skills\":{\"Thieving\":72,\"Agility\":56},\"quests\":[]},"
		+ "{\"key\":\"quest:Desert Treasure II - The Fallen Empire\",\"type\":\"quest\",\"title\":\"Desert Treasure II - The Fallen Empire\","
		+ "\"complete\":false,\"progress\":0.2,\"next\":\"Train Thieving to 70\",\"hoursLeft\":30.0,"
		+ "\"skills\":{\"Thieving\":70,\"Magic\":75},\"quests\":[\"Desert Treasure II - The Fallen Empire\"]},"
		+ "{\"key\":\"diary:Kourend Easy\",\"type\":\"diary\",\"title\":\"Kourend Easy\",\"complete\":false,\"progress\":0.9,"
		+ "\"hoursLeft\":0.5,\"skills\":{\"Thieving\":25},\"quests\":[]},"
		+ "{\"key\":\"quest:Done Deal\",\"type\":\"quest\",\"title\":\"Done Deal\",\"complete\":true,\"progress\":1.0,"
		+ "\"hoursLeft\":0.0,\"skills\":{\"Thieving\":99},\"quests\":[],\"somethingNew\":[1,2]}]}";

	@Test
	public void theFocusGoalFirstThenTheNearestLevel()
	{
		List<NavigatorGoals.Need> needs = NavigatorGoals.needs(new Gson(), BOARD, "Thieving", 52);
		assertEquals(2, needs.size());
		assertEquals("Desert Treasure II - The Fallen Empire", needs.get(0).getGoal());
		assertTrue(needs.get(0).isFocus());
		assertEquals(70, needs.get(0).getLevel());
		assertEquals("Ardougne Hard", needs.get(1).getGoal());
		assertEquals(72, needs.get(1).getLevel());
	}

	@Test
	public void noBoardOrABadOneIsNoGoals()
	{
		assertTrue(NavigatorGoals.needs(new Gson(), null, "Thieving", 1).isEmpty());
		assertTrue(NavigatorGoals.needs(new Gson(), "[1,2", "Thieving", 1).isEmpty());
		assertTrue(NavigatorGoals.needs(new Gson(), "{}", "Thieving", 1).isEmpty());
		assertTrue(NavigatorGoals.needs(new Gson(), BOARD, "Thieving", 80).isEmpty());
	}
}
