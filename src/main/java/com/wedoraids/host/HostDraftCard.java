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
import com.wedoraids.ui.HtmlEscape;
import com.wedoraids.ui.SplitGrid;
import com.wedoraids.ui.WdrTheme;
import com.wedoraids.ui.WrappedText;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.runelite.client.ui.FontManager;

/**
 * The compose surface: a raid-hued card where a host writes a call rather than filling a form.
 *
 * <p>The headline mirrors what a joiner sees. Below it sit the two gating choices (tier and open
 * spots), the ToB role chips, the CoX scale, the scouted layout, and a truth line for everything
 * else that ships. Sentinel-guarded combos live in {@link SentinelCombo} and {@link TierChooser}.
 */
final class HostDraftCard extends JPanel
{
	private static final String DASH = "\u2014";

	private final Supplier<RaidType> selectedRaid;
	private final Runnable onChange;
	private final TierChooser tierChooser = new TierChooser(this::fireChange);
	private final SentinelCombo spots = new SentinelCombo(DASH, this::fireChange);
	private final SentinelCombo team = new SentinelCombo(DASH, this::fireChange);
	private final JTextField scale = new JTextField();
	private final RoleChip[] chips =
		{new RoleChip("mdps"), new RoleChip("rdps"), new RoleChip("nfrz"), new RoleChip("sfrz")};
	private final JLabel raidLabel = new JLabel();
	private final JLabel worldLabel = new JLabel();
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
		// Outlined, not railed: this card has no neighbours, so a rail would mark nothing.
		setBorder(WdrTheme.CARD_BORDER);
		setAlignmentX(Component.LEFT_ALIGNMENT);
		WdrTheme.styleField(scale);
		buildHeadline();
		add(Box.createVerticalStrut(4));
		HostFormLayout.fullWidth(tierChooser.combo());
		add(tierChooser.combo());
		add(tierChooser.hint());
		add(Box.createVerticalStrut(2));
		// "open" matches the panel's existing language for a free spot (live post "+2 open", footer "14 open").
		add(HostFormLayout.pair(
			HostFormLayout.labeled("Open spots", spots),
			HostFormLayout.labeled("Team size", team)));
		buildRoleGroup();
		scaleRow = HostFormLayout.labeled("Scale (0-100)", scale);
		add(scaleRow);
		dim(layoutState);
		add(layoutState);
		// 3px matches the gap a labelled group gets above it, so the truth line reads as its own group.
		add(Box.createVerticalStrut(3));
		muted(truthLine);
		add(truthLine);
		scale.getDocument().addDocumentListener(HostFormLayout.onChange(this::fireChange));
		setRaid(selectedRaid.get());
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	/**
	 * Paints the raid watermark between the card fill and its contents.
	 *
	 * <p>Swing paints background here, children next, border last -- art added at the end of this
	 * method lands above the surface and below every label without touching the outline.
	 */
	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		RaidWatermark.paint((Graphics2D) graphics, selectedRaid.get(), getWidth(), headlineBand());
	}

	/**
	 * Depth of the watermark band: top padding plus headline height plus {@link #BAND_BLEED}.
	 *
	 * <p>The bleed extends into the gap below the headline, which is card surface the opaque tier
	 * combo would mask anyway. Must be measured after layout; falls back to preferred height before
	 * the first paint.
	 */
	private static final int BAND_BLEED = 5;

	private int headlineBand()
	{
		final int headline = raidLabel.getHeight() > 0
			? raidLabel.getHeight()
			: raidLabel.getPreferredSize().height;
		return getInsets().top + headline + BAND_BLEED;
	}

	// --- headline (B1) ---

	private void buildHeadline()
	{
		raidLabel.setFont(FontManager.getRunescapeBoldFont());
		// Seed text so topRow's capped height is measured against the bold line, not an empty label.
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
		raidLabel.setText(tierValue == null ? raid.getDisplayName() : raid.getDisplayName() + " " + tierValue);
		worldLabel.setText(world.isEmpty() ? "" : "W" + world);
		revalidate();
		repaint();
	}

	/** Truth line (B7): the effective values not already shown above, each on its own line and muted. */
	void refreshTruth(String partyHub, String friendsChat)
	{
		final StringBuilder body = new StringBuilder();
		appendRoute(body, partyHub.isEmpty() ? null : "ph: " + partyHub);
		// Both routes use colon-prefix ("ph:", "fc:") to match the feed card's convention.
		appendRoute(body, friendsChat.isEmpty() ? null : "fc: " + friendsChat);
		// HTML wraps at the card measure instead of ellipsizing; a truncated passphrase is useless.
		truthLine.setText(body.length() == 0 ? ""
			: "<html><body style='width:" + WrappedText.CARD + "px'>" + body + "</body></html>");
		truthLine.setVisible(body.length() > 0);
		revalidate();
		repaint();
	}

	private static void appendRoute(StringBuilder body, String value)
	{
		if (value == null)
		{
			return;
		}
		if (body.length() > 0)
		{
			body.append("<br>");
		}
		body.append(HtmlEscape.escape(value));
	}

	// --- role chips (B4) ---

	private void buildRoleGroup()
	{
		final JPanel grid = new JPanel(new SplitGrid(4, 4));
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
		spots.setOptions(spotsOptions(raid));
		team.setOptions(teamOptions(raid));
		roleGroup.setVisible(raid == RaidType.TOB);
		scaleRow.setVisible(raid == RaidType.COX);
		refreshHeadline(headlineWorld);
	}

	private static List<String> spotsOptions(RaidType raid)
	{
		final List<String> options = new ArrayList<>();
		final int maxTeam = raid == RaidType.TOB ? 5 : 8;
		for (int open = 1; open < maxTeam; open++)
		{
			options.add("+" + open);
		}
		return options;
	}

	private static List<String> teamOptions(RaidType raid)
	{
		final List<String> options = new ArrayList<>();
		final int minimum = raid == RaidType.TOB ? 2 : 1;
		final int maximum = raid == RaidType.TOB ? 5 : 8;
		for (int size = minimum; size <= maximum; size++)
		{
			options.add(String.valueOf(size));
		}
		return options;
	}

	void refreshTiers(int killCount)
	{
		tierChooser.refresh(selectedRaid.get(), killCount);
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
		return tierChooser.getTier();
	}

	String getSpots()
	{
		return spots.read();
	}

	String getTeamSize()
	{
		return team.read();
	}

	String getScale()
	{
		return scale.getText().trim();
	}

	/** Marks and focuses the scale field after its value fails validation; the mark clears on edit. */
	void flagScaleInvalid()
	{
		WdrTheme.flagInvalid(scale);
		scale.requestFocusInWindow();
	}

	void selectTier(String value)
	{
		tierChooser.select(value);
	}

	void selectSpots(String value)
	{
		spots.select(value);
	}

	void selectSize(String value)
	{
		team.select(value);
	}

	void setScale(String value)
	{
		scale.setText(value);
	}

	void setTierEnabled(boolean enabled)
	{
		tierChooser.setEnabled(enabled);
	}

	void reset()
	{
		tierChooser.reset();
		spots.clear();
		team.clear();
		updating = true;
		try
		{
			scale.setText("");
		}
		finally
		{
			updating = false;
		}
		for (RoleChip chip : chips)
		{
			chip.setChosen(false);
		}
		layoutState.setVisible(false);
		truthLine.setVisible(false);
		refreshHeadline("");
	}

	// --- internals ---

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

	/**
	 * Applies tertiary ink to a label: joining routes are the card's least urgent content and must
	 * match {@link WdrTheme#TEXT_MUTED} as used for the same values on a feed card.
	 */
	private void muted(JLabel label)
	{
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(WdrTheme.TEXT_MUTED);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
	}
}
