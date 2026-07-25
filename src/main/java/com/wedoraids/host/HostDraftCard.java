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
import com.wedoraids.ui.WdrTheme;
import com.wedoraids.ui.WrappedText;
import java.awt.BorderLayout;
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
 * else that will ship. The sentinel-guarded combos live in {@link SentinelCombo} and
 * {@link TierChooser}; this card owns the presentation around them.
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
		setBorder(CARD_PADDING);
		setAlignmentX(Component.LEFT_ALIGNMENT);
		WdrTheme.styleField(scale);
		buildHeadline();
		add(Box.createVerticalStrut(4));
		HostFormLayout.fullWidth(tierChooser.combo());
		add(tierChooser.combo());
		add(tierChooser.hint());
		add(Box.createVerticalStrut(2));
		add(HostFormLayout.pair(HostFormLayout.labeled("Need", spots), HostFormLayout.labeled("Team", team)));
		buildRoleGroup();
		scaleRow = HostFormLayout.labeled("Scale (0-100)", scale);
		add(scaleRow);
		dim(layoutState);
		add(layoutState);
		add(Box.createVerticalStrut(1));
		dim(truthLine);
		add(truthLine);
		scale.getDocument().addDocumentListener(HostFormLayout.onChange(this::fireChange));
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
		raidLabel.setText(tierValue == null ? raid.getDisplayName() : raid.getDisplayName() + " " + tierValue);
		worldLabel.setText(world.isEmpty() ? "" : "W" + world);
		revalidate();
		repaint();
	}

	/** Truth line (B7): the effective values not already shown above, each on its own line and dim. */
	void refreshTruth(String partyHub, String friendsChat)
	{
		final StringBuilder body = new StringBuilder();
		appendRoute(body, partyHub.isEmpty() ? null : "ph: " + partyHub);
		appendRoute(body, friendsChat.isEmpty() ? null : "fc " + friendsChat);
		// A single JLabel ellipsizes, which silently drops a whole joining route (or the tail of a
		// passphrase, and a partial passphrase is useless). Rendering as HTML at the card measure wraps
		// instead of truncating, and each route gets its own line so a long value never pushes the next
		// one off the end. Every route stays legible; none can vanish.
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
	 * No raid rail here. In the feed a rail separates one call from its neighbours, which is work
	 * worth a pixel; this card has no neighbours, so the rail marked nothing while making the raid a
	 * host is composing look like the raids they are browsing. The headline still carries the hue.
	 */
	private static final Border CARD_PADDING = BorderFactory.createEmptyBorder(9, 10, 10, 10);
}
