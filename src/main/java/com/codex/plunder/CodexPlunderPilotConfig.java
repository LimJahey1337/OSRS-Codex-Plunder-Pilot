package com.codex.plunder;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(CodexPlunderPilotConfig.GROUP)
public interface CodexPlunderPilotConfig extends Config
{
	String GROUP = "codexplunderpilot";

	@ConfigItem(
		keyName = "goalLevel",
		name = "Goal level",
		description = "The Thieving level the Runs page counts toward",
		position = 0
	)
	@Range(min = 22, max = 99)
	default int goalLevel()
	{
		return 99;
	}

	@ConfigItem(
		keyName = "highlights",
		name = "Highlight the next step",
		description = "Outline in the room what the plan says to do next: the urns worth searching, the chest or sarcophagus, the spear trap or the untried tomb doors",
		position = 1
	)
	default boolean highlights()
	{
		return true;
	}

	@ConfigItem(
		keyName = "plaque",
		name = "Run plaque",
		description = "During a run, show a small plaque in the game view with the room, the clock and the next step",
		position = 2
	)
	default boolean plaque()
	{
		return true;
	}

	@ConfigItem(
		keyName = "readyPlaque",
		name = "Ready plaque",
		description = "In the pyramid before a run, show the ready check's most urgent point on a plaque in the game view, such as a pack too full for the artefacts",
		position = 3
	)
	default boolean readyPlaque()
	{
		return true;
	}

	@ConfigItem(
		keyName = "summary",
		name = "Run summary in chat",
		description = "When a run ends, write what it earned and the sceptre odds it carried in the chat box",
		position = 4
	)
	default boolean summary()
	{
		return true;
	}

	@ConfigItem(
		keyName = "roomOpens",
		name = "Room openings in chat",
		description = "When a Thieving level opens a deeper room, say so in the chat box with what a run pays there",
		position = 5
	)
	default boolean roomOpens()
	{
		return true;
	}
}
