/*
 * Copyright (c) 2026, s59
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.wedoraids.feed;

import java.util.ArrayList;
import java.util.List;

public final class RecruitDisplay
{
	private static final int[] WDR_TIER_KC_COX = {0, 5, 25, 75, -1, 500};
	private static final int[] WDR_TIER_KC_TOB = {0, 1, 10, -1, 100, -1};
	private static final int[] WDR_TIER_KC_TOA = {0, 5, -1, -1, -1, -1};

	private RecruitDisplay()
	{
	}

	public static String wdrTierNumber(RaidType raid, int kc)
	{
		if (kc <= 0)
		{
			return null;
		}
		final int[] bounds;
		switch (raid)
		{
			case COX:
				bounds = WDR_TIER_KC_COX;
				break;
			case TOB:
				bounds = WDR_TIER_KC_TOB;
				break;
			case TOA:
				bounds = WDR_TIER_KC_TOA;
				break;
			default:
				return null;
		}
		int tier = 0;
		for (int i = 0; i < bounds.length; i++)
		{
			if (bounds[i] >= 0 && kc >= bounds[i])
			{
				tier = i;
			}
		}
		return "T" + tier;
	}

	public static int teamTotal(String partySize)
	{
		if (partySize == null)
		{
			return 0;
		}
		switch (partySize.toLowerCase().trim())
		{
			case "solo":
				return 1;
			case "duo":
				return 2;
			case "trio":
				return 3;
			case "quad":
				return 4;
			default:
				return firstInt(partySize);
		}
	}

	public static int firstInt(String text)
	{
		if (text == null)
		{
			return 0;
		}
		int value = 0;
		boolean foundDigit = false;
		for (int i = 0; i < text.length(); i++)
		{
			final char character = text.charAt(i);
			if (Character.isDigit(character))
			{
				foundDigit = true;
				final int digit = Character.digit(character, 10);
				if (value > (Integer.MAX_VALUE - digit) / 10)
				{
					return 0;
				}
				value = value * 10 + digit;
			}
			else if (foundDigit)
			{
				return value;
			}
		}
		return value;
	}

	/**
	 * True when the raw message says nothing the card does not already show in its own fields.
	 *
	 * <p>Bridge messages routinely restate the parsed data ("trio w447 mdps/rdps +2 phub olm" beside
	 * a card already showing the world, fill and roles). Rendering both costs two or three lines of a
	 * 225px column per call. Every structured value is removed from a copy of the message; if what
	 * remains carries no word or number, the message is redundant and the caller can drop it. The
	 * original text is never altered for display, so nothing is mangled when it does add something.
	 */
	public static boolean messageIsRedundant(RecruitEntry entry)
	{
		if (entry == null || entry.getMessage() == null)
		{
			return true;
		}
		String residual = entry.getMessage().toLowerCase();
		for (String known : knownTokens(entry))
		{
			if (known != null && !known.isEmpty())
			{
				residual = residual.replace(known, " ");
			}
		}
		for (int i = 0; i < residual.length(); i++)
		{
			if (Character.isLetterOrDigit(residual.charAt(i)))
			{
				return false;
			}
		}
		return true;
	}

	/** Lowercase fragments the card already renders, longest first so "mdps" cannot eat "dps". */
	private static List<String> knownTokens(RecruitEntry entry)
	{
		final List<String> tokens = new ArrayList<>();
		addToken(tokens, entry.getTier());
		addToken(tokens, entry.getMode());
		addToken(tokens, entry.getSpots());
		addToken(tokens, entry.getPartySize());
		addToken(tokens, entry.getRegion());
		addToken(tokens, entry.getHost());
		addToken(tokens, entry.getRaidType() == RaidType.OTHER ? null : entry.getRaidType().getDisplayName());
		if (entry.getRoles() != null)
		{
			for (String role : entry.getRoles().split("[,/]"))
			{
				addToken(tokens, role);
			}
		}
		if (entry.getWorld() != 0)
		{
			final String world = Integer.toString(entry.getWorld());
			addToken(tokens, "w" + world);
			addToken(tokens, world);
		}
		// Party-hub wording the bridge emits alongside the hub name itself.
		tokens.add("phub");
		tokens.add("ph");
		tokens.sort((a, b) -> Integer.compare(b.length(), a.length()));
		return tokens;
	}

	private static void addToken(List<String> tokens, String value)
	{
		if (value == null)
		{
			return;
		}
		final String trimmed = value.trim().toLowerCase();
		if (!trimmed.isEmpty())
		{
			tokens.add(trimmed);
		}
	}
}
