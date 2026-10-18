package com.codex.plunder.ui;

import com.codex.plunder.core.Goal;
import com.codex.plunder.core.Levels;
import com.codex.plunder.core.Odds;
import com.codex.plunder.core.Pace;
import com.codex.plunder.core.Plan;
import com.codex.plunder.core.Planner;
import com.codex.plunder.core.Room;
import com.codex.plunder.core.Run;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The Plunder Pilot side panel: the crest with the clock or the deepest room, then three pages.
 * Pilot is the next step and the route room by room, Odds is the sceptre and how dry the
 * player has been, and Runs is the rate, the way to a goal level and every run so far.
 */
public class PilotPanel extends PluginPanel
{
	static final int WIDTH = 200;
	private static final String[] TABS = {"pilot", "odds", "runs"};
	private static final int RECENT = 8;

	private final Actions actions;
	private final Crest crest = new Crest();
	private final Segmented tabs = new Segmented(true, "Pilot", "Odds", "Runs");
	private final Segmented goal = new Segmented(false, Goal.EXPERIENCE.getLabel(), Goal.SCEPTRE.getLabel());
	private final CardLayout cards = new CardLayout();
	private final JPanel content = new JPanel(cards);
	private final JScrollPane pilot = scroller();
	private final JScrollPane odds = scroller();
	private final JScrollPane runs = scroller();

	private View view = View.loggedOut();

	public PilotPanel(Actions actions)
	{
		super(false);
		this.actions = actions;
		setLayout(new BorderLayout());
		setBackground(Theme.INK);

		JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(crest, BorderLayout.NORTH);
		JPanel tabHolder = new JPanel(new BorderLayout());
		tabHolder.setOpaque(false);
		tabHolder.setBorder(new EmptyBorder(8, 8, 2, 8));
		tabHolder.add(tabs, BorderLayout.CENTER);
		north.add(tabHolder, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		content.setOpaque(false);
		content.add(pilot, TABS[0]);
		content.add(odds, TABS[1]);
		content.add(runs, TABS[2]);
		add(content, BorderLayout.CENTER);

		tabs.onChange(this::showTab);
		goal.onChange(i -> actions.goal(Goal.values()[i]));
		rebuild();
	}

	private static JScrollPane scroller()
	{
		JScrollPane scroll = new JScrollPane();
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		return scroll;
	}

	/** Called on the Swing thread whenever anything shown changes. */
	public void update(View view)
	{
		this.view = view;
		goal.setSelected(view.goal.ordinal(), false);
		updateCrest();
		rebuild();
	}

	/** Opens one tab, for previews and tests. */
	public void showTab(int index)
	{
		tabs.setSelected(index, false);
		cards.show(content, TABS[index]);
	}

	private void updateCrest()
	{
		switch (view.state)
		{
			case LOGGED_OUT:
				crest.update("PYRAMID PLUNDER", "-", null, "Log in to plan your runs", Theme.TEXT_MUTED, Theme.GOLD_DIM, -1);
				return;
			case TOO_LOW:
				crest.update("THIEVING " + view.level, "21", "to enter", "The pyramid opens at Thieving 21", Theme.TEXT_MUTED, Theme.GOLD_DIM, -1);
				return;
			default:
				break;
		}
		Plan plan = view.plan;
		if (view.inRun())
		{
			Plan.Stop stop = plan.current();
			int ticks = view.live.ticksLeft;
			Color tint = Theme.clock(ticks);
			crest.update("ROOM " + stop.getRoom().getNumber() + " OF " + plan.getTop().getNumber(), Format.clock(ticks), "left",
				view.step.getTitle(), ticks <= 150 ? tint : Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.45), tint,
				ticks / (double) Planner.RUN_TICKS);
			return;
		}
		Plan fresh = view.fresh;
		crest.update("THIEVING " + view.level + "  -  " + view.goal.getLabel().toUpperCase(Locale.ENGLISH),
			String.valueOf(fresh.getTop().getNumber()), fresh.getTop().getNumber() == 1 ? "room" : "rooms",
			Format.amount(fresh.xpPerHour(Planner.RESTART_TICKS)) + " xp/hr  -  sceptre " + Format.oneIn(fresh.getSceptreChance()),
			Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.35), Theme.GOLD, -1);
	}

	private void rebuild()
	{
		rebuildPilot();
		rebuildOdds();
		rebuildRuns();
	}

	private boolean waiting(StackPanel stack)
	{
		if (view.state == View.State.LOGGED_OUT)
		{
			stack.add(note("The pyramid is sealed", "Log in and the Pilot plans every Pyramid Plunder run for your Thieving level: which rooms to walk through, which urns, chests and sarcophagi are worth the clock, and your odds of the Pharaoh's sceptre."));
			return true;
		}
		if (view.state == View.State.TOO_LOW)
		{
			int left = Levels.xpFor(Room.ENTRY_LEVEL) - (int) Math.min(Integer.MAX_VALUE, view.xp);
			stack.add(note("Not yet", "Pyramid Plunder needs Thieving 21, and the level cannot be boosted. You are " + Format.amount(Math.max(0, left)) + " experience away."));
			return true;
		}
		return false;
	}

	// The Pilot tab.

	private void rebuildPilot()
	{
		StackPanel stack = new StackPanel(8, 8);
		if (waiting(stack))
		{
			replace(pilot, stack);
			return;
		}
		stack.add(new SectionHeader("Run for", view.inRun() ? "replanned every tick" : null));
		stack.add(goal);

		Card next = new Card(new BorderLayout(0, 4));
		next.accent(view.inRun() ? Theme.clock(view.live.ticksLeft) : Theme.TEAL).highlighted(view.inRun());
		next.add(Ui.label(view.inRun() ? "NEXT" : "BEFORE YOU GO IN", Theme.small(), Theme.TEXT_FAINT), BorderLayout.NORTH);
		JPanel words = new JPanel(new BorderLayout(0, 2));
		words.setOpaque(false);
		words.add(Ui.label(view.step.getTitle(), Theme.title(), Theme.GOLD_BRIGHT), BorderLayout.NORTH);
		words.add(Ui.wrapped(view.step.getDetail(), Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
		next.add(words, BorderLayout.CENTER);
		stack.add(next);

		Plan plan = view.plan;
		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		if (view.inRun())
		{
			tiles.add(new StatTile("+" + Format.amount(view.live.thievingXp), "This run",
				"+" + Format.amount(plan.getThievingXp()) + " to come", Theme.GOLD));
			double chance = 1 - (1 - view.live.sceptreSoFar) * (1 - plan.getSceptreChance());
			tiles.add(new StatTile(Format.oneIn(chance), "Sceptre", view.live.opened + " opened so far", Theme.TEAL));
		}
		else
		{
			tiles.add(new StatTile(Format.amount(plan.getThievingXp()), "Per run",
				plan.getStrengthXp() > 0 ? "+" + Format.amount(plan.getStrengthXp()) + " strength" : "thieving xp", Theme.GOLD));
			tiles.add(new StatTile(Format.oneIn(plan.getSceptreChance()), "Sceptre", "a run", Theme.TEAL));
		}
		stack.add(tiles);

		stack.add(new SectionHeader("Route", "room " + plan.current().getRoom().getNumber() + " to " + plan.getTop().getNumber()));
		Card route = new Card(new BorderLayout());
		StackPanel rows = new StackPanel(0, 0);
		for (Plan.Stop stop : plan.getStops())
		{
			rows.add(new RoomRow(stop, view.inRun() && stop == plan.current()));
		}
		for (int i = plan.getTop().ordinal() + 1; i < Room.values().length; i++)
		{
			rows.add(new RoomRow(Room.values()[i]));
		}
		route.add(rows, BorderLayout.CENTER);
		stack.add(route);
		String foot = view.record.paceSamples >= Pace.TRUSTED
			? "Paced from your own searches. Chests and sarcophagi last in each room, so a swarm or mummy has the least time to chase."
			: "Paced at typical speeds until a few of your runs are timed. Chests and sarcophagi last in each room.";
		stack.add(Ui.wrapped(foot, Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
		replace(pilot, stack);
	}

	// The Odds tab.

	private void rebuildOdds()
	{
		StackPanel stack = new StackPanel(8, 8);
		if (waiting(stack))
		{
			replace(odds, stack);
			return;
		}
		View.Record r = view.record;
		long opened = 0;
		for (long o : r.opened)
		{
			opened += o;
		}
		double expected = Odds.expected(r.opened);
		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		tiles.add(new StatTile(String.format(Locale.ENGLISH, "%,d", opened), "Opened", "chests & sarcs", Theme.GOLD));
		tiles.add(new StatTile(String.format(Locale.ENGLISH, "%.2f", expected), "Expected", r.sceptres + (r.sceptres == 1 ? " sceptre found" : " sceptres found"),
			r.sceptres > 0 ? Theme.TEAL : Theme.TEXT_MUTED));
		stack.add(tiles);

		Card luck = new Card(new BorderLayout(0, 6));
		luck.accent(Theme.TEAL);
		if (opened == 0)
		{
			luck.add(Ui.label("No openings yet", Theme.body(), Theme.GOLD), BorderLayout.NORTH);
			luck.add(Ui.wrapped("Every grand gold chest and sarcophagus you open is counted by room, and your sceptre luck is measured against it.",
				Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
		}
		else
		{
			double share = Odds.luck(r.opened, r.sceptres);
			String verdict;
			Color color;
			if (r.sceptres == 0)
			{
				double had = Odds.atLeastOne(r.opened);
				verdict = Format.percent(had) + " would have one by now";
				color = had > 0.5 ? Theme.EMBER : Theme.GOLD;
			}
			else if (share >= 0.5)
			{
				verdict = "Luckier than " + Format.percent(share);
				color = Theme.TEAL;
			}
			else
			{
				verdict = "Drier than " + Format.percent(1 - share);
				color = Theme.EMBER;
			}
			luck.add(Ui.label(verdict, Theme.body(), color), BorderLayout.NORTH);
			luck.add(new Meter("Your luck", Format.percent(share), share, color, 0.5), BorderLayout.CENTER);
		}
		stack.add(luck);

		Plan fresh = view.fresh;
		double perRun = fresh.getSceptreChance();
		stack.add(new SectionHeader("Your runs", view.goal.getLabel()));
		Card forecast = new Card(new BorderLayout(0, 6));
		StackPanel lines = new StackPanel(4, 0);
		lines.add(line("Each run", Format.oneIn(perRun), Theme.TEAL));
		lines.add(line("Even odds", Format.amount(Odds.runsFor(perRun, 0.5)) + " runs", Theme.GOLD));
		lines.add(line("Nine in ten", Format.amount(Odds.runsFor(perRun, 0.9)) + " runs", Theme.GOLD));
		lines.add(line("In an hour", Format.percent(fresh.sceptrePerHour(Planner.RESTART_TICKS)), Theme.GOLD));
		forecast.add(lines, BorderLayout.CENTER);
		stack.add(forecast);
		if (view.goal == Goal.EXPERIENCE)
		{
			stack.add(Ui.wrapped("Hunting the sceptre opens every chest and the sarcophagi from room 4 on, at some cost in experience. Switch the goal on the Pilot page to compare.",
				Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
		}

		stack.add(new SectionHeader("Per opening", "chest or sarc"));
		Card rooms = new Card(new BorderLayout());
		StackPanel rows = new StackPanel(3, 0);
		for (Room room : Room.values())
		{
			boolean open = view.level >= room.getLevel();
			long n = r.opened[room.ordinal()];
			rows.add(line("Room " + room.getNumber() + (n > 0 ? "  -  " + Format.amount(n) + " opened" : ""),
				"1 in " + String.format(Locale.ENGLISH, "%,d", room.getSceptreOneIn()), open ? Theme.TEAL : Theme.TEXT_FAINT));
		}
		rooms.add(rows, BorderLayout.CENTER);
		stack.add(rooms);
		stack.add(Ui.wrapped("Finding the sceptre ejects you from the pyramid at once. Rates are the game's, per the Old School wiki.",
			Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
		replace(odds, stack);
	}

	// The Runs tab.

	private void rebuildRuns()
	{
		StackPanel stack = new StackPanel(8, 8);
		if (waiting(stack))
		{
			replace(runs, stack);
			return;
		}
		View.Record r = view.record;
		Plan fresh = view.fresh;
		int forecast = fresh.xpPerHour(Planner.RESTART_TICKS);
		int rate = r.recentRate > 0 ? r.recentRate : forecast;
		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		tiles.add(new StatTile(Format.amount(rate), "Xp an hour", r.recentRate > 0 ? "your last runs" : "forecast", Theme.GOLD));
		tiles.add(new StatTile(Format.amount(r.totalRuns), "Runs", Format.coins(r.totalArtefacts) + " gp loot", Theme.TEAL));
		stack.add(tiles);

		int target = Math.max(view.level + 1, Math.min(99, view.targetLevel));
		if (view.level < 99)
		{
			long goalXp = Levels.xpFor(target);
			long left = Math.max(0, goalXp - view.xp);
			int perRun = Math.max(1, fresh.getThievingXp());
			long runsLeft = (left + perRun - 1) / perRun;
			long start = Levels.xpFor(view.level);
			double share = goalXp <= start ? 1 : (view.xp - start) / (double) (goalXp - start);
			Card goalCard = new Card(new BorderLayout(0, 6));
			goalCard.accent(Theme.GOLD);
			goalCard.add(Ui.label("Thieving " + target, Theme.body(), Theme.GOLD), BorderLayout.NORTH);
			String hours = Format.duration(Math.round(left * 3_600_000.0 / Math.max(1, rate)));
			goalCard.add(new Meter(Format.amount(left) + " xp, about " + Format.amount(runsLeft) + " runs", hours, share, Theme.GOLD, -1), BorderLayout.CENTER);
			int unlock = Room.nextUnlock(view.level);
			if (unlock > 0 && unlock < target)
			{
				long toUnlock = Math.max(0, Levels.xpFor(unlock) - view.xp);
				goalCard.add(Ui.wrapped("Room " + Room.highest(unlock).getNumber() + " opens at " + unlock + ", " + Format.amount(toUnlock)
					+ " xp away, and every run after pays more.", Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.SOUTH);
			}
			stack.add(goalCard);
		}

		stack.add(new SectionHeader("The ladder", "xp an hour"));
		Card ladder = new Card(new BorderLayout());
		StackPanel rows = new StackPanel(3, 0);
		Room mine = Room.highest(view.level);
		for (Plan p : view.ladder)
		{
			Room top = p.getTop();
			boolean yours = top == mine;
			rows.add(line((yours ? "You  -  " : "") + "Level " + top.getLevel() + ", room " + top.getNumber(),
				Format.amount(p.xpPerHour(Planner.RESTART_TICKS)), yours ? Theme.GOLD_BRIGHT : view.level >= top.getLevel() ? Theme.GOLD : Theme.TEXT_FAINT));
		}
		ladder.add(rows, BorderLayout.CENTER);
		stack.add(ladder);

		if (!r.runs.isEmpty())
		{
			stack.add(new SectionHeader("Recent runs", Format.amount(r.totalXp) + " xp in all"));
			Card recent = new Card(new BorderLayout());
			StackPanel runRows = new StackPanel(4, 0);
			for (int i = r.runs.size() - 1; i >= 0 && i >= r.runs.size() - RECENT; i--)
			{
				Run run = r.runs.get(i);
				runRows.add(new RunRow(run, view.now));
			}
			recent.add(runRows, BorderLayout.CENTER);
			stack.add(recent);
		}
		replace(runs, stack);
	}

	private static JComponent line(String left, String right, Color color)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		JLabel l = Ui.label(left, Theme.small(), Theme.TEXT_MUTED);
		row.add(l, BorderLayout.CENTER);
		row.add(Ui.label(right, Theme.small(), color), BorderLayout.EAST);
		return row;
	}

	static Card note(String title, String text)
	{
		Card card = new Card(new BorderLayout(0, 4));
		card.accent(Theme.GOLD_DIM);
		card.add(Ui.label(title, Theme.body(), Theme.GOLD), BorderLayout.NORTH);
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
		return card;
	}

	private static void replace(JScrollPane scroll, JComponent view)
	{
		int position = scroll.getVerticalScrollBar().getValue();
		scroll.setViewportView(view);
		scroll.revalidate();
		SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(position));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		g.setColor(Theme.INK);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
