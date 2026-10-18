package com.codex.plunder.core;

/** What a run is for. It decides which urns, chests and sarcophagi are worth the time. */
public enum Goal
{
	/** Thieving experience: urns in the deepest rooms, chests from room four on. */
	EXPERIENCE("Experience"),
	/** The Pharaoh's sceptre: every chest, and the sarcophagi from room four on, before any urn. */
	SCEPTRE("Sceptre");

	private final String label;

	Goal(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
}
