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
package com.wedoraids.host;

import com.wedoraids.feed.RaidType;
import com.wedoraids.ui.WdrTheme;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import net.runelite.client.ui.FontManager;

/**
 * Tier selector: a {@link SentinelCombo} filtered to the tiers a host's KC qualifies for,
 * with a hint line that appears when some tiers are hidden. The card lays out {@link #combo}
 * and {@link #hint} directly.
 */
final class TierChooser
{
	static final String SENTINEL = "Choose tier\u2026";

	private final SentinelCombo combo;
	private final JLabel hint = new JLabel();

	TierChooser(Runnable onChange)
	{
		combo = new SentinelCombo(SENTINEL, onChange);
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setForeground(WdrTheme.TEXT_DIM);
		hint.setAlignmentX(Component.LEFT_ALIGNMENT);
		hint.setVisible(false);
	}

	SentinelCombo combo()
	{
		return combo;
	}

	JLabel hint()
	{
		return hint;
	}

	/** Rebuilds the ladder to the tiers this KC can host, showing the limit hint when some are hidden. */
	void refresh(RaidType raid, int killCount)
	{
		final List<String> eligible = new ArrayList<>();
		for (String option : raid.getTiers())
		{
			if (killCount < 0 || raid.minKc(option) <= killCount)
			{
				eligible.add(option);
			}
		}
		combo.setOptions(eligible);
		final boolean limited = killCount >= 0 && eligible.size() < raid.getTiers().length;
		hint.setVisible(limited);
		if (limited)
		{
			hint.setText("Tiers limited by your " + raid.getDisplayName() + " KC: " + killCount);
		}
	}

	String getTier()
	{
		return combo.read();
	}

	void select(String value)
	{
		combo.select(value);
	}

	void setEnabled(boolean enabled)
	{
		combo.setEnabled(enabled);
	}

	void reset()
	{
		combo.clear();
	}
}
