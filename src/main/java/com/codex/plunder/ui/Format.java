package com.codex.plunder.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Experience, coins, odds, the clock and dates in the short forms the panel uses. */
public final class Format
{
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH);

	private Format()
	{
	}

	/** "8,240", "41.2k", "412k", "1.24M". */
	public static String amount(long n)
	{
		if (n < 10_000)
		{
			return String.format(Locale.ENGLISH, "%,d", n);
		}
		if (n < 100_000)
		{
			return trim(n / 1_000.0, 1) + "k";
		}
		if (n < 1_000_000)
		{
			return Math.round(n / 1_000.0) + "k";
		}
		if (n < 10_000_000)
		{
			return trim(n / 1_000_000.0, 2) + "M";
		}
		if (n < 1_000_000_000)
		{
			return trim(n / 1_000_000.0, 1) + "M";
		}
		return trim(n / 1_000_000_000.0, 2) + "B";
	}

	/** Coins in the short form, negative amounts with a minus: "812.4M", "-45.2k". */
	public static String coins(long gp)
	{
		return gp < 0 ? "-" + amount(-gp) : amount(gp);
	}

	/** A change with its sign: "+3.2M", "-412k", "0". */
	public static String signed(long gp)
	{
		return gp > 0 ? "+" + amount(gp) : gp < 0 ? "-" + amount(-gp) : "0";
	}

	/** A quantity change with its sign, written in full: "+1,240", "-6". */
	public static String count(long n)
	{
		return (n > 0 ? "+" : n < 0 ? "-" : "") + String.format(Locale.ENGLISH, "%,d", Math.abs(n));
	}

	/** Game ticks as a clock: "4:58", "0:07". */
	public static String clock(int ticks)
	{
		int seconds = (int) Math.round(Math.max(0, ticks) * 0.6);
		return seconds / 60 + ":" + String.format(Locale.ENGLISH, "%02d", seconds % 60);
	}

	/** A gap in ticks as seconds, with no sign: "5s", "1:04". */
	public static String seconds(int ticks)
	{
		int s = (int) Math.round(Math.abs(ticks) * 0.6);
		return s < 60 ? s + "s" : s / 60 + ":" + String.format(Locale.ENGLISH, "%02d", s % 60);
	}

	/** A split against the best, signed: "-5s" ahead, "+1:04" behind, "even". */
	public static String split(int ticks)
	{
		int s = (int) Math.round(Math.abs(ticks) * 0.6);
		if (s == 0)
		{
			return "even";
		}
		return (ticks < 0 ? "-" : "+") + seconds(ticks);
	}

	/** A chance as odds: "1 in 69", "1 in 4,200", or "none". */
	public static String oneIn(double chance)
	{
		if (chance <= 0)
		{
			return "none";
		}
		return "1 in " + String.format(Locale.ENGLISH, "%,d", Math.round(1 / chance));
	}

	/** A share as a whole percent, or "<1%". */
	public static String percent(double share)
	{
		if (share > 0 && share < 0.01)
		{
			return "<1%";
		}
		return Math.round(share * 100) + "%";
	}

	/** "just now", "12m ago", "3h ago", "2d ago". */
	public static String ago(long ms)
	{
		long minutes = Math.max(0, ms) / 60_000;
		if (minutes < 1)
		{
			return "just now";
		}
		if (minutes < 60)
		{
			return minutes + "m ago";
		}
		if (minutes < 48 * 60)
		{
			return minutes / 60 + "h ago";
		}
		return minutes / (24 * 60) + "d ago";
	}

	private static String trim(double value, int places)
	{
		String text = String.format(Locale.ENGLISH, "%." + places + "f", value);
		while (text.contains(".") && (text.endsWith("0") || text.endsWith(".")))
		{
			text = text.substring(0, text.length() - 1);
		}
		return text;
	}

	/** "2h 14m", "38m", "under a minute". */
	public static String duration(long ms)
	{
		long minutes = ms / 60_000;
		if (minutes < 1)
		{
			return "under a minute";
		}
		if (minutes < 60)
		{
			return minutes + "m";
		}
		return minutes / 60 + "h " + minutes % 60 + "m";
	}

	/** "14:02" in the player's own time. */
	public static String time(long epochMs)
	{
		return CLOCK.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	/** "Thu 16 Oct". */
	public static String day(long epochMs)
	{
		return DAY.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	/** "16 Oct". */
	public static String shortDay(long epochMs)
	{
		return SHORT.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	/** "Oct 26". */
	public static String month(long epochMs)
	{
		return MONTH.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	/** "Thursday 16 October 2026". */
	public static String date(long epochMs)
	{
		return DATE.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}
}
