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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import net.runelite.client.ui.FontManager;

/**
 * The compose surface: a raid-hued card, built in the same visual language as the live post and the
 * feed card, so a host writes a call rather than filling a database record. The headline mirrors
 * what a joiner will see; below it sit the two choices that gate a post (tier, and how many are
 * needed), the ToB role chips, the CoX scale, the scouted layout, and a truth line of everything
 * else that will ship.
 */
final class HostDraftCard extends JPanel
{
	private static final String TIER_SENTINEL = "Choose tier\u2026";
	private static final String DASH = "\u2014";

	private final Supplier<RaidType> selectedRaid;
	private final Runnable onChange;
	private final JComboBox<String> tier = new JComboBox<>();
	private final JComboBox<String> spots = new JComboBox<>();
	private final JComboBox<String> team = new JComboBox<>();
	private final JTextField scale = new JTextField();
	private final RoleChip[] chips =
		{new RoleChip("mdps"), new RoleChip("rdps"), new RoleChip("nfrz"), new RoleChip("sfrz")};
	private final JLabel raidLabel = new JLabel();
	private final JLabel worldLabel = new JLabel();
	private final JLabel tierHint = new JLabel();
	private final JLabel layoutState = new JLabel();
	private final JLabel truthLine = new JLabel();
	private JPanel roleGroup;
	private JPanel scaleRow;
	private boolean updating;
	private String headlineWorld = "";

	HostDraftCard(Supplier<RaidType> selectedRaid, Runnable onChange)
	{
		this.selectedRaid = selectedRaid;
		this.onChange = onChange;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(WdrTheme.CARD);
		setBorder(cardBorder(selectedRaid.get().getColor()));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		WdrTheme.styleCombo(tier);
		WdrTheme.styleCombo(spots);
		WdrTheme.styleCombo(team);
		WdrTheme.styleField(scale);
		buildHeadline();
		add(Box.createVerticalStrut(4));
		HostFormLayout.fullWidth(tier);
		add(tier);
		dim(tierHint);
		tierHint.setVisible(false);
		add(tierHint);
		add(Box.createVerticalStrut(2));
		add(HostFormLayout.pair(HostFormLayout.labeled("Need", spots), HostFormLayout.labeled("Team", team)));
		buildRoleGroup();
		scaleRow = HostFormLayout.labeled("Scale", scale);
		add(scaleRow);
		dim(layoutState);
		add(layoutState);
		add(Box.createVerticalStrut(1));
		dim(truthLine);
		add(truthLine);
		wireListeners();
		setRaid(selectedRaid.get());
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	// --- headline (B1) ---

	private void buildHeadline()
	{
		raidLabel.setFont(FontManager.getRunescapeBoldFont());
		// Seed the raid name so topRow's capped height is measured against the bold line, not an empty
		// label; refreshHeadline overwrites the text and colour on the first layout.
		raidLabel.setText(selectedRaid.get().getDisplayName());
		raidLabel.setForeground(selectedRaid.get().getColor());
		worldLabel.setFont(FontManager.getRunescapeSmallFont());
		worldLabel.setForeground(WdrTheme.TEXT);
		final JPanel west = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		west.setOpaque(false);
		west.add(raidLabel);
		final JPanel topRow = new JPanel(new BorderLayout());
		topRow.setOpaque(false);
		topRow.add(west, BorderLayout.WEST);
		topRow.add(worldLabel, BorderLayout.EAST);
		topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, topRow.getPreferredSize().height));
		add(topRow);
	}

	void refreshHeadline(String world)
	{
		headlineWorld = world;
		final RaidType raid = selectedRaid.get();
		final String tierValue = getTier();
		raidLabel.setForeground(raid.getColor());
		if (tierValue == null)
		{
			raidLabel.setText(raid.getDisplayName());
		}
		else
		{
			raidLabel.setText(raid.getDisplayName() + " " + tierValue);
		}
		worldLabel.setText(world.isEmpty() ? "" : "W" + world);
		revalidate();
		repaint();
	}

	/** Truth line (B7): the effective values not already shown above, {@code ·}-joined and dim. */
	void refreshTruth(String partyHub, String friendsChat)
	{
		final StringBuilder truth = new StringBuilder();
		append(truth, partyHub.isEmpty() ? null : "ph: " + partyHub);
		append(truth, friendsChat.isEmpty() ? null : "fc " + friendsChat);
		truthLine.setText(truth.toString());
		truthLine.setVisible(truth.length() > 0);
		revalidate();
		repaint();
	}

	private static void append(StringBuilder truth, String value)
	{
		if (value == null)
		{
			return;
		}
		if (truth.length() > 0)
		{
			truth.append(" \u00b7 ");
		}
		truth.append(value);
	}

	// --- role chips (B4) ---

	private void buildRoleGroup()
	{
		final JPanel grid = new JPanel(new GridLayout(2, 2, 4, 4));
		grid.setOpaque(false);
		for (RoleChip chip : chips)
		{
			grid.add(chip);
		}
		roleGroup = HostFormLayout.labeled("Roles needed", grid);
		add(roleGroup);
	}

	List<String> getSelectedRoles()
	{
		final List<String> roles = new ArrayList<>();
		for (RoleChip chip : chips)
		{
			if (chip.isChosen())
			{
				roles.add(chip.getText());
			}
		}
		return roles;
	}

	void setSelectedRoles(List<String> roles)
	{
		for (RoleChip chip : chips)
		{
			chip.setChosen(roles.contains(chip.getText()));
		}
	}

	// --- raid applicability + option ranges ---

	void setRaid(RaidType raid)
	{
		updating = true;
		try
		{
			setBorder(cardBorder(raid.getColor()));
			rebuildSpots(raid);
			rebuildTeam(raid);
			roleGroup.setVisible(raid == RaidType.TOB);
			scaleRow.setVisible(raid == RaidType.COX);
		}
		finally
		{
			updating = false;
		}
		refreshHeadline(headlineWorld);
	}

	private void rebuildSpots(RaidType raid)
	{
		final Object previous = spots.getSelectedItem();
		spots.removeAllItems();
		spots.addItem(DASH);
		final int maxTeam = raid == RaidType.TOB ? 5 : 8;
		for (int open = 1; open < maxTeam; open++)
		{
			spots.addItem("+" + open);
		}
		if (previous != null)
		{
			spots.setSelectedItem(previous);
		}
	}

	private void rebuildTeam(RaidType raid)
	{
		final Object previous = team.getSelectedItem();
		team.removeAllItems();
		team.addItem(DASH);
		final int minimum = raid == RaidType.TOB ? 2 : 1;
		final int maximum = raid == RaidType.TOB ? 5 : 8;
		for (int size = minimum; size <= maximum; size++)
		{
			team.addItem(String.valueOf(size));
		}
		if (previous != null)
		{
			team.setSelectedItem(previous);
		}
	}

	// --- tier chooser (B2) ---

	void refreshTiers(int killCount)
	{
		final RaidType raid = selectedRaid.get();
		final Object previous = tier.getSelectedItem();
		updating = true;
		try
		{
			tier.removeAllItems();
			tier.addItem(TIER_SENTINEL);
			for (String option : raid.getTiers())
			{
				if (killCount < 0 || raid.minKc(option) <= killCount)
				{
					tier.addItem(option);
				}
			}
			restoreTierSelection(previous);
		}
		finally
		{
			updating = false;
		}
		refreshTierHint(raid, killCount);
	}

	/** Restores a previously chosen tier only if still offered; otherwise falls back to the sentinel. */
	private void restoreTierSelection(Object previous)
	{
		if (previous != null && !TIER_SENTINEL.equals(previous))
		{
			for (int index = 0; index < tier.getItemCount(); index++)
			{
				if (previous.equals(tier.getItemAt(index)))
				{
					tier.setSelectedIndex(index);
					return;
				}
			}
		}
		tier.setSelectedIndex(0);
	}

	private void refreshTierHint(RaidType raid, int killCount)
	{
		final int realCount = tier.getItemCount() - 1;
		final boolean limited = killCount >= 0 && realCount < raid.getTiers().length;
		tierHint.setVisible(limited);
		if (limited)
		{
			tierHint.setText("Tiers limited by your " + raid.getDisplayName() + " KC: " + killCount);
		}
		revalidate();
		repaint();
	}

	// --- scouted layout state (B6) ---

	void setLayoutState(boolean applies, String text)
	{
		layoutState.setVisible(applies);
		layoutState.setText(text);
		revalidate();
		repaint();
	}

	// --- values (wire contract reads) ---

	String getTier()
	{
		final Object item = tier.getSelectedItem();
		return item == null || TIER_SENTINEL.equals(item) ? null : (String) item;
	}

	String getSpots()
	{
		final Object item = spots.getSelectedItem();
		return item == null || DASH.equals(item) ? null : (String) item;
	}

	String getTeamSize()
	{
		final Object item = team.getSelectedItem();
		return item == null || DASH.equals(item) ? null : (String) item;
	}

	String getScale()
	{
		return scale.getText().trim();
	}

	void selectTier(String value)
	{
		updating = true;
		try
		{
			if (value == null)
			{
				tier.setSelectedIndex(0);
			}
			else
			{
				tier.setSelectedItem(value);
				if (getTier() == null)
				{
					tier.setSelectedIndex(0);
				}
			}
		}
		finally
		{
			updating = false;
		}
	}

	void selectSpots(String value)
	{
		setCombo(spots, value);
	}

	void selectSize(String value)
	{
		setCombo(team, value);
	}

	void setScale(String value)
	{
		scale.setText(value);
	}

	private void setCombo(JComboBox<String> combo, String value)
	{
		updating = true;
		try
		{
			final String target = value == null || value.isEmpty() ? DASH : value;
			combo.setSelectedItem(target);
			if (!target.equals(combo.getSelectedItem()))
			{
				combo.setSelectedIndex(0);
			}
		}
		finally
		{
			updating = false;
		}
	}

	void setTierEnabled(boolean enabled)
	{
		tier.setEnabled(enabled);
	}

	void reset()
	{
		updating = true;
		try
		{
			if (tier.getItemCount() > 0)
			{
				tier.setSelectedIndex(0);
			}
			if (spots.getItemCount() > 0)
			{
				spots.setSelectedIndex(0);
			}
			if (team.getItemCount() > 0)
			{
				team.setSelectedIndex(0);
			}
			scale.setText("");
			for (RoleChip chip : chips)
			{
				chip.setChosen(false);
			}
		}
		finally
		{
			updating = false;
		}
		layoutState.setVisible(false);
		truthLine.setVisible(false);
		refreshHeadline("");
	}

	// --- internals ---

	private void wireListeners()
	{
		tier.addActionListener(event -> fireChange());
		spots.addActionListener(event -> fireChange());
		team.addActionListener(event -> fireChange());
		scale.getDocument().addDocumentListener(HostFormLayout.onChange(this::fireChange));
	}

	private void fireChange()
	{
		if (!updating)
		{
			onChange.run();
		}
	}

	private void dim(JLabel label)
	{
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(WdrTheme.TEXT_DIM);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

	private static Border cardBorder(Color raidColor)
	{
		return BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 1, 0, 0, raidColor),
			BorderFactory.createEmptyBorder(9, 10, 10, 10));
	}
}
