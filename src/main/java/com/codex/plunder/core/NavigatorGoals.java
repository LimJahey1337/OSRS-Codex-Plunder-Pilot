package com.codex.plunder.core;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The Thieving levels Codex Navigator's pinned goals still need. Navigator saves its goal board
 * for companion plugins under the RS profile key {@code codexnavigator.goalBoard} (fields are
 * only ever added); this keeps each unfinished goal's title and levels. Read only, and with
 * Navigator not installed there is no board and nothing is shown.
 */
public final class NavigatorGoals
{
	public static final String GROUP = "codexnavigator";
	public static final String KEY = "goalBoard";

	/** One pinned goal, as saved. Field names are Navigator's. */
	static final class Entry
	{
		String key;
		String title;
		boolean complete;
		Map<String, Integer> skills;
	}

	/** Navigator's saved board, as far as it is read here. */
	static final class Board
	{
		String focus;
		List<Entry> goals;
	}

	/** A level one goal needs. */
	public static final class Need
	{
		private final String goal;
		private final int level;
		private final boolean focus;

		Need(String goal, int level, boolean focus)
		{
			this.goal = goal;
			this.level = level;
			this.focus = focus;
		}

		/** The goal's title, such as "Desert Treasure II". */
		public String getGoal()
		{
			return goal;
		}

		public int getLevel()
		{
			return level;
		}

		/** The goal Navigator has in focus. */
		public boolean isFocus()
		{
			return focus;
		}
	}

	private NavigatorGoals()
	{
	}

	/**
	 * @param json the board as Navigator saved it, or null
	 * @param skill the skill's name, such as "Thieving"
	 * @param level the level now
	 * @return every unfinished goal that needs the skill above that level, the focus goal first, then lowest level first
	 */
	public static List<Need> needs(Gson gson, String json, String skill, int level)
	{
		if (json == null || json.isEmpty())
		{
			return Collections.emptyList();
		}
		Board board;
		try
		{
			board = gson.fromJson(json, Board.class);
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return Collections.emptyList();
		}
		if (board == null || board.goals == null)
		{
			return Collections.emptyList();
		}
		List<Need> out = new ArrayList<>();
		for (Entry e : board.goals)
		{
			if (e == null || e.complete || e.skills == null)
			{
				continue;
			}
			for (Map.Entry<String, Integer> s : e.skills.entrySet())
			{
				Integer need = s.getValue();
				if (s.getKey() != null && s.getKey().equalsIgnoreCase(skill) && need != null && need > level && need <= 99)
				{
					String title = e.title == null || e.title.isEmpty() ? "A Navigator goal" : e.title;
					out.add(new Need(title, need, e.key != null && e.key.equals(board.focus)));
				}
			}
		}
		out.sort((a, b) -> a.focus != b.focus ? (a.focus ? -1 : 1) : Integer.compare(a.level, b.level));
		return out;
	}
}
