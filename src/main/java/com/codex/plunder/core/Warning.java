package com.codex.plunder.core;

/** Where a mid-run warning goes: in place of the next step, on a line under it, or nowhere. */
public enum Warning
{
	/** The warning is the step, and the plan waits until it clears. */
	FIRST("In place of the step"),
	/** The step goes on, with the warning on a line of its own under it. */
	UNDER("Under the step"),
	OFF("Off");

	private final String label;

	Warning(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
