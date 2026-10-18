package com.codex.plunder;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CodexPlunderPilotPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CodexPlunderPilotPlugin.class);
		RuneLite.main(args);
	}
}
