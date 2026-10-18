package com.codex.plunder;

import com.codex.plunder.core.Goal;
import com.codex.plunder.core.Here;
import com.codex.plunder.core.Logbook;
import com.codex.plunder.core.Odds;
import com.codex.plunder.core.Pace;
import com.codex.plunder.core.Plan;
import com.codex.plunder.core.Planner;
import com.codex.plunder.core.Room;
import com.codex.plunder.core.Run;
import com.codex.plunder.core.RunTracker;
import com.codex.plunder.core.Step;
import com.codex.plunder.overlay.PilotPlaque;
import com.codex.plunder.overlay.RoomHighlights;
import com.codex.plunder.ui.Actions;
import com.codex.plunder.ui.Format;
import com.codex.plunder.ui.PilotPanel;
import com.codex.plunder.ui.Sigil;
import com.codex.plunder.ui.Theme;
import com.codex.plunder.ui.View;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Codex Plunder Pilot",
	description = "Plans every Pyramid Plunder run for your Thieving level: which rooms to walk through, which urns, chests and sarcophagi are worth the clock, and your sceptre odds",
	tags = {"pyramid plunder", "thieving", "sophanem", "sceptre", "pharaoh", "urn", "minigame", "codex"}
)
public class CodexPlunderPilotPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CodexPlunderPilotPlugin.class);
	private static final String LOGBOOK_KEY = "logbook";
	private static final int PYRAMID_REGION = 7749;
	/** About eighteen tiles: the room the player is in, not the ones beside it. */
	private static final int NEAR = 2350;
	/** Finding the sceptre throws the player out; it can land in the pack a moment after the run ends. */
	private static final int SCEPTRE_GRACE_TICKS = 10;

	private static final Set<Integer> URNS = ImmutableSet.of(ObjectID.NTK_URN_TYPE1_MULTI_1, ObjectID.NTK_URN_TYPE1_MULTI_2,
		ObjectID.NTK_URN_TYPE1_MULTI_3, ObjectID.NTK_URN_TYPE1_MULTI_4, ObjectID.NTK_URN_TYPE1_MULTI_5, ObjectID.NTK_URN_TYPE2_MULTI_6,
		ObjectID.NTK_URN_TYPE2_MULTI_7, ObjectID.NTK_URN_TYPE2_MULTI_8, ObjectID.NTK_URN_TYPE2_MULTI_9, ObjectID.NTK_URN_TYPE2_MULTI_10,
		ObjectID.NTK_URN_TYPE3_MULTI_11, ObjectID.NTK_URN_TYPE3_MULTI_12, ObjectID.NTK_URN_TYPE3_MULTI_13, ObjectID.NTK_URN_TYPE3_MULTI_14,
		ObjectID.NTK_URN_TYPE3_MULTI_15);
	private static final Set<Integer> URNS_CLOSED = ImmutableSet.of(ObjectID.NTK_URN1_CLOSED, ObjectID.NTK_URN2_CLOSED, ObjectID.NTK_URN3_CLOSED);
	private static final Set<Integer> DOORS = ImmutableSet.of(ObjectID.NTK_TOMB_DOOR1, ObjectID.NTK_TOMB_DOOR2, ObjectID.NTK_TOMB_DOOR3, ObjectID.NTK_TOMB_DOOR4);
	private static final Set<Integer> ARTEFACTS = ImmutableSet.of(ItemID.NTK_IVORY_COMB, ItemID.NTK_SCARAB_POTTERY, ItemID.NTK_STATUETTE_POTTERY,
		ItemID.NTK_SEAL_STONE, ItemID.NTK_SCARAB_STONE, ItemID.NTK_STATUETTE_STONE, ItemID.NTK_SEAL_GOLD, ItemID.NTK_SCARAB_GOLD, ItemID.NTK_STATUETTE_GOLD);
	private static final Set<Integer> SCEPTRES = ImmutableSet.of(ItemID.PHARAOHS_SCEPTRE, ItemID.PHARAOHS_SCEPTRE_CHARGED, ItemID.PHARAOHS_SCEPTRE_CHARGED_INITIAL);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Gson gson;

	@Inject
	private CodexPlunderPilotConfig config;

	private final PilotPlaque plaque = new PilotPlaque();
	private final RoomHighlights highlights = new RoomHighlights();
	private final RunTracker tracker = new RunTracker();
	private final List<GameObject> urns = new ArrayList<>();
	private final List<GameObject> chests = new ArrayList<>();
	private final List<GameObject> sarcophagi = new ArrayList<>();
	private final List<GameObject> traps = new ArrayList<>();
	private final List<WallObject> doors = new ArrayList<>();
	private final Map<Skill, Integer> lastXp = new EnumMap<>(Skill.class);

	private PilotPanel panel;
	private NavigationButton navButton;

	private String profile;
	private Logbook logbook;
	private boolean unsaved;
	private boolean dirty;

	/** The room as it stood last tick, to see what changed. */
	private Room lastRoom;
	private int lastClosedUrns = -1;
	private boolean lastChestOpen;
	private boolean lastSarcophagusOpen;

	private Map<Integer, Integer> lastPack;
	private int finishedTick = -1000;

	@Provides
	CodexPlunderPilotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexPlunderPilotConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new PilotPanel(new PanelActions());
		navButton = NavigationButton.builder()
			.tooltip("Codex Plunder Pilot")
			.icon(Sigil.icon(16))
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(plaque);
		overlayManager.add(highlights);
		clientThread.invokeLater(() ->
		{
			loadProfile();
			lastXp.clear();
			lastPack = null;
			publish(null);
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(plaque);
		overlayManager.remove(highlights);
		clientToolbar.removeNavigation(navButton);
		clientThread.invokeLater(() ->
		{
			save();
			logbook = null;
			profile = null;
			forgetScene();
		});
		panel = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOADING:
				forgetScene();
				break;
			case LOGIN_SCREEN:
				endRun();
				save();
				lastXp.clear();
				lastPack = null;
				publish(null);
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		save();
		loadProfile();
		lastXp.clear();
		dirty = true;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CodexPlunderPilotConfig.GROUP.equals(event.getGroup()))
		{
			dirty = true;
		}
	}

	// What is in the room, kept as the scene loads it.

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject o = event.getGameObject();
		int id = o.getId();
		if (URNS.contains(id))
		{
			urns.add(o);
		}
		else if (id == ObjectID.NTK_GOLDEN_CHEST_MULTI)
		{
			chests.add(o);
		}
		else if (id == ObjectID.NTK_SARCOPHAGUS_MULTI)
		{
			sarcophagi.add(o);
		}
		else if (id == ObjectID.NTK_SPEARTRAP_INMOTION)
		{
			traps.add(o);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject o = event.getGameObject();
		urns.remove(o);
		chests.remove(o);
		sarcophagi.remove(o);
		traps.remove(o);
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		if (DOORS.contains(event.getWallObject().getId()))
		{
			doors.add(event.getWallObject());
		}
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		doors.remove(event.getWallObject());
	}

	private void forgetScene()
	{
		urns.clear();
		chests.clear();
		sarcophagi.clear();
		traps.clear();
		doors.clear();
		lastClosedUrns = -1;
	}

	// Experience and the pack.

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();
		if (skill != Skill.THIEVING && skill != Skill.STRENGTH)
		{
			return;
		}
		Integer before = lastXp.put(skill, event.getXp());
		if (before == null || event.getXp() <= before)
		{
			return;
		}
		int gained = event.getXp() - before;
		if (skill == Skill.THIEVING)
		{
			tracker.thieving(gained);
		}
		else
		{
			tracker.strength(gained);
		}
		dirty = true;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INV)
		{
			return;
		}
		Map<Integer, Integer> pack = pack(event.getItemContainer());
		Map<Integer, Integer> before = lastPack;
		lastPack = pack;
		if (before == null)
		{
			return;
		}
		long value = 0;
		boolean sceptre = false;
		for (Map.Entry<Integer, Integer> e : pack.entrySet())
		{
			int gained = e.getValue() - before.getOrDefault(e.getKey(), 0);
			if (gained <= 0)
			{
				continue;
			}
			if (ARTEFACTS.contains(e.getKey()))
			{
				value += (long) gained * itemManager.getItemPrice(e.getKey());
			}
			else if (SCEPTRES.contains(e.getKey()))
			{
				sceptre = true;
			}
		}
		if (tracker.isActive())
		{
			tracker.artefacts(value);
			if (sceptre)
			{
				tracker.sceptre();
			}
		}
		else if (sceptre && logbook != null && client.getTickCount() - finishedTick <= SCEPTRE_GRACE_TICKS)
		{
			logbook.sceptreAfter();
			unsaved = true;
			save();
		}
		dirty = true;
	}

	private static Map<Integer, Integer> pack(ItemContainer container)
	{
		Map<Integer, Integer> pack = new HashMap<>();
		if (container != null)
		{
			for (Item item : container.getItems())
			{
				if (ARTEFACTS.contains(item.getId()) || SCEPTRES.contains(item.getId()))
				{
					pack.merge(item.getId(), item.getQuantity(), Integer::sum);
				}
			}
		}
		return pack;
	}

	// Every tick: where the run stands, the plan for the rest, and the next step.

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (logbook == null)
		{
			return;
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		int level = client.getRealSkillLevel(Skill.THIEVING);
		long xp = client.getSkillExperience(Skill.THIEVING);
		if (level != logbook.getLevel() || xp != logbook.getXp())
		{
			logbook.seen(level, xp);
			unsaved = true;
			dirty = true;
		}
		lastXp.putIfAbsent(Skill.THIEVING, client.getSkillExperience(Skill.THIEVING));
		lastXp.putIfAbsent(Skill.STRENGTH, client.getSkillExperience(Skill.STRENGTH));
		if (lastPack == null)
		{
			lastPack = pack(client.getItemContainer(InventoryID.INV));
		}

		int now = client.getTickCount();
		int timer = client.getVarbitValue(VarbitID.NTK_PLAYER_TIMER_COUNT);
		boolean inPyramid = player.getWorldLocation().getRegionID() == PYRAMID_REGION && timer > 0;
		Room room = Room.of(client.getVarbitValue(VarbitID.NTK_ROOM_NUMBER));
		if (!inPyramid || room == null)
		{
			if (tracker.isActive())
			{
				endRun();
			}
			if (dirty)
			{
				publish(null);
			}
			return;
		}
		Pace pace = logbook.getPace();
		if (!tracker.isActive())
		{
			tracker.begin(System.currentTimeMillis(), now - timer, logbook.getGoal());
			lastRoom = null;
		}
		tracker.room(room, now, pace);

		LocalPoint me = player.getLocalLocation();
		int plane = client.getPlane();
		Map<Integer, Boolean> slots = new HashMap<>();
		List<GameObject> closedUrns = new ArrayList<>();
		for (GameObject urn : urns)
		{
			if (!near(urn, me, plane))
			{
				continue;
			}
			ObjectComposition def = client.getObjectDefinition(urn.getId());
			ObjectComposition shown = def.getImpostor();
			boolean closed = shown != null && URNS_CLOSED.contains(shown.getId());
			slots.merge(def.getVarbitId(), closed, Boolean::logicalOr);
			if (closed)
			{
				closedUrns.add(urn);
			}
		}
		int urnsLeft;
		if (slots.isEmpty())
		{
			urnsLeft = room.getUrns();
		}
		else
		{
			int closed = 0;
			for (boolean c : slots.values())
			{
				closed += c ? 1 : 0;
			}
			// A room with fewer urns than slots leaves a slot that never opens.
			urnsLeft = Math.max(0, closed - Math.max(0, slots.size() - room.getUrns()));
		}
		boolean chestOpen = opened(chests, ObjectID.NTK_GOLDEN_CHEST_CLOSED, me, plane);
		boolean sarcophagusOpen = opened(sarcophagi, ObjectID.NTK_SARCOPHAGUS, me, plane);
		if (room == lastRoom)
		{
			if (lastClosedUrns >= 0)
			{
				for (int i = urnsLeft; i < lastClosedUrns; i++)
				{
					tracker.urnSearched(now, pace);
				}
			}
			if (chestOpen && !lastChestOpen)
			{
				tracker.chestOpened(now);
			}
			if (sarcophagusOpen && !lastSarcophagusOpen)
			{
				tracker.sarcophagusOpened(now);
			}
		}
		lastRoom = room;
		lastClosedUrns = slots.isEmpty() ? -1 : urnsLeft;
		lastChestOpen = chestOpen;
		lastSarcophagusOpen = sarcophagusOpen;

		List<WallObject> untried = new ArrayList<>();
		Set<Integer> doorSlots = new java.util.HashSet<>();
		Set<Integer> triedSlots = new java.util.HashSet<>();
		for (WallObject door : doors)
		{
			if (!near(door, me, plane))
			{
				continue;
			}
			ObjectComposition def = client.getObjectDefinition(door.getId());
			ObjectComposition shown = def.getImpostor();
			doorSlots.add(def.getVarbitId());
			if (shown != null && shown.getId() == ObjectID.NTK_TOMB_DOOR_NOANIM)
			{
				untried.add(door);
			}
			else
			{
				triedSlots.add(def.getVarbitId());
			}
		}
		boolean trapActive = client.getVarbitValue(VarbitID.NTK_TRAP_ACTIVE) == 1;
		int ticksLeft = Math.max(0, Planner.RUN_TICKS - timer);
		Here here = new Here(room, urnsLeft, chestOpen, sarcophagusOpen, !trapActive);
		Plan plan = Planner.plan(level, logbook.getGoal(), pace, here, ticksLeft);
		if (plan == null)
		{
			publish(null);
			return;
		}
		Step step = Step.next(plan, true, trapActive, triedSlots.size());

		List<RoomHighlights.Mark> marks = new ArrayList<>();
		if (config.highlights())
		{
			switch (step.getKind())
			{
				case TRAP:
					traps.stream().filter(t -> near(t, me, plane)).forEach(t -> marks.add(new RoomHighlights.Mark(t, Theme.EMBER, true)));
					break;
				case URNS:
					closedUrns.forEach(u -> marks.add(new RoomHighlights.Mark(u, Theme.GOLD, false)));
					break;
				case CHEST:
					chests.stream().filter(c -> near(c, me, plane)).forEach(c -> marks.add(new RoomHighlights.Mark(c, Theme.TEAL, false)));
					break;
				case SARCOPHAGUS:
					sarcophagi.stream().filter(c -> near(c, me, plane)).forEach(c -> marks.add(new RoomHighlights.Mark(c, Theme.VIOLET, false)));
					break;
				case DOOR:
					untried.forEach(d -> marks.add(new RoomHighlights.Mark(d, Theme.GOLD, true)));
					break;
				default:
					break;
			}
		}
		highlights.show(marks);

		if (config.plaque())
		{
			Color color = Theme.clock(ticksLeft);
			plaque.show("R" + room.getNumber() + "  " + Format.clock(ticksLeft), step.getTitle(), step.getDetail(), color,
				ticksLeft / (double) Planner.RUN_TICKS);
		}
		else
		{
			plaque.show(null, null, null, null, 0);
		}
		publish(new Live(plan, step, ticksLeft));
	}

	private static boolean near(TileObject o, LocalPoint me, int plane)
	{
		return o.getPlane() == plane && o.getLocalLocation().distanceTo(me) < NEAR;
	}

	/** Whether the room's chest or sarcophagus has been opened; they all share one state. */
	private boolean opened(List<GameObject> objects, int closedId, LocalPoint me, int plane)
	{
		for (GameObject o : objects)
		{
			if (near(o, me, plane))
			{
				ObjectComposition shown = client.getObjectDefinition(o.getId()).getImpostor();
				return shown != null && shown.getId() != closedId;
			}
		}
		return false;
	}

	private void endRun()
	{
		highlights.show(null);
		plaque.show(null, null, null, null, 0);
		lastRoom = null;
		lastClosedUrns = -1;
		if (!tracker.isActive())
		{
			return;
		}
		long[] opened = tracker.getOpened();
		Run run = tracker.finish(client.getTickCount());
		finishedTick = client.getTickCount();
		if (run == null || logbook == null)
		{
			return;
		}
		logbook.record(run, opened);
		unsaved = true;
		save();
		dirty = true;
		if (config.summary())
		{
			ChatMessageBuilder message = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append("Plunder Pilot: ")
				.append(ChatColorType.NORMAL)
				.append("room " + run.getDeepest() + ", +" + String.format("%,d", run.getThievingXp()) + " Thieving xp");
			if (run.getArtefacts() > 0)
			{
				message.append(", " + Format.coins(run.getArtefacts()) + " gp of artefacts");
			}
			if (run.getChests() + run.getSarcophagi() > 0)
			{
				message.append(". Sceptre odds this run " + Format.oneIn(run.getSceptreChance()));
			}
			message.append(".");
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(message.build())
				.build());
		}
	}

	// Saving.

	private void loadProfile()
	{
		profile = configManager.getRSProfileKey();
		logbook = null;
		if (profile == null)
		{
			return;
		}
		try
		{
			String saved = configManager.getRSProfileConfiguration(CodexPlunderPilotConfig.GROUP, LOGBOOK_KEY);
			if (saved != null && !saved.isEmpty())
			{
				Logbook read = gson.fromJson(saved, Logbook.class);
				logbook = read == null ? null : read.repair();
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read the saved logbook, starting afresh", e);
		}
		if (logbook == null)
		{
			logbook = new Logbook();
		}
		unsaved = false;
		dirty = true;
	}

	private void save()
	{
		if (profile == null || logbook == null || !unsaved)
		{
			return;
		}
		unsaved = false;
		configManager.setConfiguration(CodexPlunderPilotConfig.GROUP, profile, LOGBOOK_KEY, gson.toJson(logbook));
	}

	// Handing the panel what it shows.

	private static final class Live
	{
		final Plan plan;
		final Step step;
		final int ticksLeft;

		Live(Plan plan, Step step, int ticksLeft)
		{
			this.plan = plan;
			this.step = step;
			this.ticksLeft = ticksLeft;
		}
	}

	private void publish(Live live)
	{
		dirty = false;
		PilotPanel target = panel;
		if (target == null)
		{
			return;
		}
		long now = System.currentTimeMillis();
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
		if (logbook == null || !loggedIn && logbook.getLevel() == 0)
		{
			SwingUtilities.invokeLater(() -> target.update(View.loggedOut()));
			return;
		}
		int level = logbook.getLevel();
		Goal goal = logbook.getGoal();
		Pace pace = logbook.getPace();
		View.Record record = new View.Record(new ArrayList<>(logbook.getRuns()), logbook.getOpened(), logbook.getSceptres(),
			logbook.getTotalRuns(), logbook.getTotalXp(), logbook.getTotalArtefacts(), logbook.recentRate(10), pace.getUrnSamples());
		String name = loggedIn ? client.getLocalPlayer().getName() : null;
		Plan fresh = Planner.fresh(level, goal, pace);
		View view;
		if (fresh == null)
		{
			view = new View(View.State.TOO_LOW, name, level, logbook.getXp(), config.goalLevel(), goal, null, null, null, null, null, record, now);
		}
		else
		{
			List<Plan> ladder = new ArrayList<>();
			for (Room r : Room.values())
			{
				ladder.add(Planner.fresh(r.getLevel(), goal, pace));
			}
			Plan plan = live == null ? fresh : live.plan;
			Step step = live == null ? Step.next(fresh, false, false, 0) : live.step;
			View.Live run = live == null ? null : new View.Live(live.ticksLeft, tracker.getThievingXp(), tracker.getArtefacts(),
				tracker.getChests() + tracker.getSarcophagi(), Odds.atLeastOne(tracker.getOpened()));
			view = new View(View.State.READY, name, level, logbook.getXp(), config.goalLevel(), goal, plan, fresh, ladder, step, run, record, now);
		}
		SwingUtilities.invokeLater(() -> target.update(view));
	}

	private final class PanelActions implements Actions
	{
		@Override
		public void goal(Goal goal)
		{
			clientThread.invokeLater(() ->
			{
				if (logbook != null && logbook.getGoal() != goal)
				{
					logbook.setGoal(goal);
					unsaved = true;
					save();
					// During a run the next tick replans with the new goal.
					if (tracker.isActive())
					{
						dirty = true;
					}
					else
					{
						publish(null);
					}
				}
			});
		}
	}
}
