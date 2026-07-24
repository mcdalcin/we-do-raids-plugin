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

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The three WDR raids and their tier ladders.
 *
 * <p>Each raid's colour is the panel's one data dimension. The hues stay true to the raids
 * themselves (Verzik purple, Xeric green, Amascut gold) so they are recognisable to players,
 * but all three are equalised at OKLCH lightness 0.76 with matched chroma. Before equalising,
 * ToA read almost twice as strongly as ToB against the card surface (9.6:1 vs 5.0:1), which
 * made ToA calls look more urgent purely by colour weight; they now sit within 0.8 of each
 * other and every one clears WCAG AA on both the card and the panel canvas.
 *
 * <p>Colour is never the only signal: every raid is also labelled in text.
 */
@Getter
@RequiredArgsConstructor
public enum RaidType
{
	TOB("ToB", new Color(201, 154, 240),
		new String[]{"Learner", "Standard", "Advanced", "Efficient", "HM", "HM Exp"},
		new int[]{0, 10, 100, 100, 100, 100}),
	COX("CoX", new Color(122, 199, 124),
		new String[]{"Learner", "Unscaled", "Scaled", "Experienced", "CM", "FFA", "FFA CM", "CM Efficiency"},
		new int[]{0, 5, 25, 75, 75, 25, 75, 500}),
	TOA("ToA", new Color(226, 163, 72),
		new String[]{"0-295", "300-445", "450+", "FFA"},
		new int[]{0, 5, 5, 5}),
	OTHER("Raid", new Color(158, 158, 158), new String[]{}, new int[]{});

	private final String displayName;
	private final Color color;
	private final String[] tiers;
	private final int[] minKcs;

	/** Raid KC required to host in a given tier (0 if unknown). */
	public int minKc(String tier)
	{
		for (int i = 0; i < tiers.length; i++)
		{
			if (tiers[i].equalsIgnoreCase(tier))
			{
				return i < minKcs.length ? minKcs[i] : 0;
			}
		}
		return 0;
	}
}
