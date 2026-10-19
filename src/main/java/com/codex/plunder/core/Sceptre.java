package com.codex.plunder.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Pharaoh's sceptre's charges and where the sceptre is, kept in the logbook. Its Jalsavrah
 * teleport lands at the pyramid, so a charged sceptre saves the walk back after banking.
 *
 * <p>Where the count comes from is the one guess in here: the game keeps a player variable
 * named for the sceptre, which looks like the charge count but has not been checked in game.
 * It is only believed once a game message naming the charges has agreed with it; until then
 * the count is the one the last such message gave, and a message that disagrees stops it
 * being believed for good. So a wrong guess can never put a made-up number on screen.
 */
public final class Sceptre
{
	/** "Your sceptre has 23 charges left", "...has 1 charge left". Lenient on purpose: the exact wording is unverified. */
	private static final Pattern CHARGES = Pattern.compile("(\\d{1,4}) charges?\\b", Pattern.CASE_INSENSITIVE);

	/** Other sceptres that hold charges and say so in the same words. */
	private static final Pattern OTHERS = Pattern.compile("skull|thammaron|accursed|warped|lithic|revenant");

	private Integer heard;
	private boolean confirmed;
	private boolean refuted;
	private Boolean banked;

	/** The charges a game message names, or null when it does not speak of the sceptre's charges. */
	public static Integer parse(String message)
	{
		if (message == null)
		{
			return null;
		}
		String text = message.replaceAll("<[^>]*>", "");
		String lower = text.toLowerCase(Locale.ENGLISH);
		if (!lower.contains("sceptre") || OTHERS.matcher(lower).find())
		{
			return null;
		}
		if (lower.contains("no charges") || lower.contains("out of charges") || lower.contains("has run out"))
		{
			return 0;
		}
		Matcher m = CHARGES.matcher(text);
		return m.find() ? Integer.parseInt(m.group(1)) : null;
	}

	/** A game message named this many charges. */
	public void heard(int charges)
	{
		heard = Math.max(0, charges);
	}

	/**
	 * Holds the player variable up against the last message, a tick after it so the variable has
	 * caught up. A match makes the variable believed; a mismatch stops it being believed for good.
	 */
	public void compare(int variable)
	{
		if (heard == null || refuted)
		{
			return;
		}
		if (variable == heard)
		{
			confirmed = true;
		}
		else
		{
			confirmed = false;
			refuted = true;
		}
	}

	/**
	 * @param variable the player variable as it stands now
	 * @return the charges, or -1 when not known
	 */
	public int charges(int variable)
	{
		if (confirmed && !refuted && variable >= 0)
		{
			return variable;
		}
		return heard == null ? -1 : heard;
	}

	/** Whether the live count is in use, rather than the last message's. */
	public boolean isLive()
	{
		return confirmed && !refuted;
	}

	/** Whether a sceptre was in the bank when it was last seen, or null if the bank has not been seen. */
	public Boolean getBanked()
	{
		return banked;
	}

	public void setBanked(boolean value)
	{
		banked = value;
	}
}
