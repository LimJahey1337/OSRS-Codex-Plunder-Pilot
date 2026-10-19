package com.codex.plunder.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * The artefacts waiting in the bank to be sold: as this plugin last saw the bank, or as Codex
 * Bank Steward last saved it when that is newer. Gear Roadmap's saved bank holds only gear, so it
 * never says anything about artefacts and is never used here.
 */
public final class Stash
{
	/**
	 * Bank Steward's bank must be this much newer than this plugin's look to be taken instead: on
	 * the same bank visit both see it, the Steward a moment later, and the look was this plugin's.
	 */
	static final long SAME_VISIT_MS = 60_000L;

	private final Map<Integer, Long> items;
	private final long seenAt;
	private final SharedBank.Source from;

	private Stash(Map<Integer, Long> items, long seenAt, SharedBank.Source from)
	{
		this.items = Collections.unmodifiableMap(items);
		this.seenAt = seenAt;
		this.from = from;
	}

	/**
	 * @param own the artefacts as this plugin last saw the bank, by item id
	 * @param ownAt when that was, 0 for never
	 * @param shared the bank another Codex plugin saved, or null
	 * @param artefacts the item ids that count
	 * @return the newer of the two (the Steward's only when over a minute newer), or null when neither has seen the bank
	 */
	public static Stash pick(Map<Integer, Long> own, long ownAt, SharedBank shared, Collection<Integer> artefacts)
	{
		if (shared != null && shared.isComplete() && shared.getSavedAt() > 0
			&& (ownAt <= 0 || shared.getSavedAt() > ownAt + SAME_VISIT_MS))
		{
			Map<Integer, Long> items = new TreeMap<>();
			for (int id : artefacts)
			{
				long n = shared.count(id);
				if (n > 0)
				{
					items.put(id, n);
				}
			}
			return new Stash(items, shared.getSavedAt(), shared.getSource());
		}
		if (ownAt <= 0)
		{
			return null;
		}
		Map<Integer, Long> items = new TreeMap<>();
		if (own != null)
		{
			for (Map.Entry<Integer, Long> e : own.entrySet())
			{
				if (e.getKey() != null && e.getValue() != null && e.getValue() > 0 && artefacts.contains(e.getKey()))
				{
					items.put(e.getKey(), e.getValue());
				}
			}
		}
		return new Stash(items, ownAt, null);
	}

	/** Artefacts by item id, lowest id first. */
	public Map<Integer, Long> getItems()
	{
		return items;
	}

	public long count()
	{
		long n = 0;
		for (long q : items.values())
		{
			n += q;
		}
		return n;
	}

	/** When the bank was seen. */
	public long getSeenAt()
	{
		return seenAt;
	}

	/** The plugin whose saved bank this is, or null when this plugin saw the bank itself. */
	public SharedBank.Source getFrom()
	{
		return from;
	}
}
