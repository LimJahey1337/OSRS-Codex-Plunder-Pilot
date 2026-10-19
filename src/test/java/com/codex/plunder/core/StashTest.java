package com.codex.plunder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class StashTest
{
	private static final List<Integer> ARTEFACTS = Arrays.asList(ItemID.NTK_IVORY_COMB, ItemID.NTK_STATUETTE_GOLD);
	private static final String STEWARD = "{\"version\":1,\"savedAt\":5000,\"items\":{\"995\":100,\""
		+ ItemID.NTK_STATUETTE_GOLD + "\":7},\"truncated\":false}";

	@Test
	public void theNewerLookAtTheBankWins()
	{
		SharedBank steward = SharedBank.read(new Gson(), STEWARD, null);
		Map<Integer, Long> own = new HashMap<>();
		own.put(ItemID.NTK_IVORY_COMB, 3L);
		Stash older = Stash.pick(own, 4000, steward, ARTEFACTS);
		assertEquals(SharedBank.Source.BANK_STEWARD, older.getFrom());
		assertEquals(Collections.singletonMap(ItemID.NTK_STATUETTE_GOLD, 7L), older.getItems());
		Stash newer = Stash.pick(own, 6000, steward, ARTEFACTS);
		assertNull(newer.getFrom());
		assertEquals(3, newer.count());
		assertEquals(6000, newer.getSeenAt());
	}

	@Test
	public void gearRoadmapsBankSaysNothingAboutArtefacts()
	{
		SharedBank roadmap = SharedBank.read(new Gson(), null, "{\"items\":[" + ItemID.NTK_STATUETTE_GOLD + "],\"coins\":5}");
		assertNull(Stash.pick(Collections.emptyMap(), 0, roadmap, ARTEFACTS));
		assertNull(Stash.pick(null, 0, null, ARTEFACTS));
		// An empty bank seen here is still a look: none banked.
		assertEquals(0, Stash.pick(Collections.emptyMap(), 10, roadmap, ARTEFACTS).count());
	}

	@Test
	public void theLogbookKeepsTheBankedArtefactsThroughASave()
	{
		Logbook book = new Logbook();
		Map<Integer, Long> seen = new HashMap<>();
		seen.put(ItemID.NTK_IVORY_COMB, 2L);
		assertEquals(true, book.setBanked(seen, 100));
		assertEquals(false, book.setBanked(seen, 200));
		assertEquals(200, book.getBankedAt());
		Logbook back = new Gson().fromJson(new Gson().toJson(book), Logbook.class).repair();
		assertEquals(Long.valueOf(2), back.getBanked().get(ItemID.NTK_IVORY_COMB));
		assertEquals(200, back.getBankedAt());
		assertEquals(0, new Gson().fromJson("{}", Logbook.class).repair().getBanked().size());
	}
}
