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

	/**
	 * Written by Codex Navigator's own GoalBoard.of and Gson (Navigator 71c07ee): a level goal in focus,
	 * a diary and a quest, at level 52 in every skill. Gson writes an apostrophe as an escape.
	 */
	private static final String REAL_BOARD = "{\"version\":1,\"focus\":\"skill:Thieving 70\",\"goals\":[{\"key\":\"skill:Thieving 70\",\"type\":\"skill\","
		+ "\"title\":\"70 Thieving\",\"complete\":false,\"progress\":0.17,\"next\":\"Train Thieving to 70\",\"hoursLeft\":5.0,"
		+ "\"skills\":{\"Thieving\":70},\"quests\":[]},{\"key\":\"diary:Ardougne Hard\",\"type\":\"diary\",\"title\":\"Ardougne Hard Diary\","
		+ "\"complete\":false,\"progress\":0.31,\"next\":\"Start Shield of Arrav\",\"hoursLeft\":59.5,\"skills\":{\"Cooking\":53,"
		+ "\"Fishing\":53,\"Hunter\":59,\"Ranged\":60,\"Farming\":65,\"Smithing\":68,\"Magic\":66,\"Thieving\":72,\"Agility\":65,"
		+ "\"Runecraft\":65},\"quests\":[\"The Grand Tree\",\"Tree Gnome Village\",\"Monkey Madness I\",\"Family Crest\","
		+ "\"Shield of Arrav\",\"Lost City\",\"Merlin\\u0027s Crystal\",\"Dragon Slayer I\",\"Heroes\\u0027 Quest\","
		+ "\"Druidic Ritual\",\"Jungle Potion\",\"Shilo Village\",\"Plague City\",\"Biohazard\",\"Underground Pass\","
		+ "\"Waterfall Quest\",\"Legends\\u0027 Quest\",\"Regicide\",\"Roving Elves\",\"Big Chompy Bird Hunting\","
		+ "\"Sheep Herder\",\"Mourning\\u0027s End Part I\",\"Watchtower\"]},{\"key\":\"quest:Dragon Slayer II\","
		+ "\"type\":\"quest\",\"title\":\"Dragon Slayer II\",\"complete\":false,\"progress\":0.28,\"next\":\"Start Shield of Arrav\","
		+ "\"hoursLeft\":65.0,\"skills\":{\"Cooking\":53,\"Woodcutting\":55,\"Thieving\":60,\"Crafting\":62,\"Agility\":60,"
		+ "\"Smithing\":70,\"Mining\":68,\"Magic\":75,\"Fishing\":53},\"quests\":[\"Family Crest\",\"Shield of Arrav\","
		+ "\"Lost City\",\"Merlin\\u0027s Crystal\",\"Dragon Slayer I\",\"Heroes\\u0027 Quest\",\"Druidic Ritual\","
		+ "\"Jungle Potion\",\"Shilo Village\",\"Plague City\",\"Biohazard\",\"Underground Pass\",\"Waterfall Quest\","
		+ "\"Legends\\u0027 Quest\",\"The Fremennik Trials\",\"Rune Mysteries\",\"Lunar Diplomacy\",\"Death Plateau\","
		+ "\"Troll Stronghold\",\"Eadgar\\u0027s Ruse\",\"Dream Mentor\",\"Gertrude\\u0027s Cat\",\"Icthlarin\\u0027s Little Helper\","
		+ "\"A Tail of Two Cats\",\"The Restless Ghost\",\"Ernest the Chicken\",\"Priest in Peril\",\"Animal Magnetism\","
		+ "\"Ghosts Ahoy\",\"The Dig Site\",\"Bone Voyage\",\"X Marks the Spot\",\"Client of Kourend\",\"Dragon Slayer II\"]},"
		+ "{\"key\":\"skill:Attack 70\",\"type\":\"skill\",\"title\":\"70 Attack\",\"complete\":false,\"progress\":0.17,"
		+ "\"next\":\"Train Attack to 70\",\"hoursLeft\":11.0,\"skills\":{\"Attack\":70},\"quests\":[]},{\"key\":\"diary:Western Provinces Hard\","
		+ "\"type\":\"diary\",\"title\":\"Western Provinces Hard\",\"complete\":false,\"progress\":0.0,\"next\":\"Western Provinces Hard\","
		+ "\"hoursLeft\":0.0,\"skills\":{},\"quests\":[]}]}";

	@Test
	public void readsTheBoardNavigatorReallyWrites()
	{
		List<NavigatorGoals.Need> needs = NavigatorGoals.needs(new Gson(), REAL_BOARD, "Thieving", 52);
		assertEquals(3, needs.size());
		assertEquals("70 Thieving", needs.get(0).getGoal());
		assertTrue("the level goal is the focus", needs.get(0).isFocus());
		assertEquals(70, needs.get(0).getLevel());
		assertEquals("Dragon Slayer II", needs.get(1).getGoal());
		assertEquals(60, needs.get(1).getLevel());
		assertEquals("Ardougne Hard Diary", needs.get(2).getGoal());
		assertEquals(72, needs.get(2).getLevel());
	}
}
