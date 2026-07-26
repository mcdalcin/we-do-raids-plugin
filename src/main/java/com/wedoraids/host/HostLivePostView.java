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

import com.wedoraids.ui.SplitGrid;
import com.wedoraids.ui.WdrButton;
import com.wedoraids.ui.WdrTheme;
import com.wedoraids.ui.WrappedText;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.FontManager;

final class HostLivePostView extends JPanel
{
	private final HostInactivityGuard inactivityGuard;
	private final Consumer<String> fillRole;
	private final Runnable decrementSpot;
	private final Runnable beginEdit;
	private final Runnable close;
	private final Runnable undo;
	private final JLabel status = new JLabel(" ");

	HostLivePostView(HostInactivityGuard inactivityGuard, Consumer<String> fillRole,
		Runnable decrementSpot, Runnable beginEdit, Runnable close, Runnable undo)
	{
		this.inactivityGuard = inactivityGuard;
		this.fillRole = fillRole;
		this.decrementSpot = decrementSpot;
		this.beginEdit = beginEdit;
		this.close = close;
		this.undo = undo;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(WdrTheme.TEXT_DIM);
		status.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

	JLabel statusLabel()
	{
		return status;
	}

	void rebuild(Map<String, String> fields, boolean canUndo)
	{
		removeAll();
		if (inactivityGuard.isPrompting())
		{
			add(inactivityGuard.banner());
			add(Box.createVerticalStrut(6));
		}
		JPanel card = card();
		final String spots = fields.get("spots");
		JLabel title = new JLabel("Your " + raidLabel(fields.get("raid")) + " raid is live");
		title.setFont(FontManager.getRunescapeSmallFont());
		title.setForeground(WdrTheme.TEXT);
		HostFormLayout.fullWidth(title);
		card.add(title);
		final String summary = summary(fields);
		if (!summary.isEmpty())
		{
			card.add(Box.createVerticalStrut(2));
			card.add(hint(summary));
		}
		card.add(Box.createVerticalStrut(4));
		card.add(row(spotsChip(spots)));

		// When the party is full the chip already says so; the only remaining actions are close and edit.
		final List<String> roles = roles(fields);
		if (!"+0".equals(spots))
		{
			card.add(Box.createVerticalStrut(9));
			if (roles.isEmpty())
			{
				addSpotControl(card);
			}
			else
			{
				addRoleControls(card, roles);
			}
		}
		if (canUndo)
		{
			card.add(Box.createVerticalStrut(6));
			// Recovery action: lower visual rank than the role/spot controls above it.
			WdrButton undoButton = new WdrButton("Undo last change", WdrButton.Variant.QUIET);
			undoButton.addActionListener(e -> undo.run());
			HostFormLayout.fullWidth(undoButton);
			card.add(undoButton);
		}
		card.add(Box.createVerticalStrut(10));
		addActionButtons(card);
		card.add(Box.createVerticalStrut(6));
		card.add(status);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
		add(card);
		revalidate();
		repaint();
	}

	/** One-line reminder of what the Discord post shows, e.g. "Standard · W416 · catdog". */
	private static String summary(Map<String, String> fields)
	{
		final StringBuilder summary = new StringBuilder();
		appendSummary(summary, fields.get("tier"));
		final String world = fields.get("world");
		appendSummary(summary, world == null || world.isEmpty() ? null : "W" + world);
		// Prefixed "ph:" to match how feed cards and the draft card render this value.
		final String hub = fields.get("partyHub");
		appendSummary(summary, hub == null || hub.isEmpty() ? null : "ph: " + hub);
		return summary.toString();
	}

	private static void appendSummary(StringBuilder summary, String value)
	{
		if (value == null || value.isEmpty())
		{
			return;
		}
		if (summary.length() > 0)
		{
			summary.append(" · ");
		}
		summary.append(value);
	}

	void setStatus(String message, boolean error)
	{
		status.setText(WrappedText.html(message, WrappedText.CARD));
		status.setForeground(error ? WdrTheme.ERROR : WdrTheme.TEXT_DIM);
	}

	/**
	 * Card for the host's live post.
	 *
	 * <p>Uses a neutral outline rather than a raid rail: the surface alone measures 1.14:1 against
	 * the canvas, so an unbordered card dissolves into it. Outlined means "one thing"; railed means
	 * "one of many".
	 */
	private static JPanel card()
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(WdrTheme.CARD);
		card.setBorder(WdrTheme.CARD_BORDER);
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		return card;
	}

	private static JLabel spotsChip(String spots)
	{
		// Not a control: bold primary ink conveys rank without a box that implies pressability.
		JLabel chip = new JLabel(chipText(spots));
		chip.setFont(FontManager.getRunescapeBoldFont());
		chip.setForeground(WdrTheme.TEXT);
		return chip;
	}

	/** Returns "Full" when spots is "+0", otherwise "N open". */
	private static String chipText(String spots)
	{
		if ("+0".equals(spots))
		{
			return "Full";
		}
		return (spots == null || spots.isEmpty() ? "-" : spots) + " open";
	}

	private void addRoleControls(JPanel card, List<String> roles)
	{
		card.add(hint("Mark role filled"));
		card.add(Box.createVerticalStrut(5));
		JPanel grid = new JPanel(new SplitGrid(5, 5));
		grid.setOpaque(false);
		for (String role : roles)
		{
			WdrButton button = new WdrButton(role, WdrButton.Variant.GHOST);
			button.addActionListener(e -> fillRole.accept(role));
			grid.add(button);
		}
		HostFormLayout.fullWidth(grid);
		card.add(grid);
		card.add(Box.createVerticalStrut(6));
		WdrButton other = new WdrButton("-1 other spot", WdrButton.Variant.GHOST);
		other.addActionListener(e -> decrementSpot.run());
		HostFormLayout.fullWidth(other);
		card.add(other);
	}

	private void addSpotControl(JPanel card)
	{
		// No hint line: "-1 spot" already names its action.
		WdrButton minusSpot = new WdrButton("-1 spot", WdrButton.Variant.PRIMARY);
		minusSpot.addActionListener(e -> decrementSpot.run());
		HostFormLayout.fullWidth(minusSpot);
		card.add(minusSpot);
	}

	private void addActionButtons(JPanel card)
	{
		JPanel row = new JPanel(new SplitGrid(6, 0));
		row.setOpaque(false);
		// Edit is a detour, not a peer of closing: QUIET rank keeps it below the DANGER close button.
		WdrButton edit = new WdrButton("Edit details", WdrButton.Variant.QUIET);
		edit.addActionListener(e -> beginEdit.run());
		row.add(edit);
		WdrButton closeButton = new WdrButton("Close raid", WdrButton.Variant.DANGER);
		closeButton.addActionListener(e -> close.run());
		row.add(closeButton);
		HostFormLayout.fullWidth(row);
		card.add(row);
	}

	private static List<String> roles(Map<String, String> fields)
	{
		final List<String> roles = new ArrayList<>();
		final String raw = fields.get("roles");
		if (raw != null && !raw.isBlank())
		{
			for (String part : raw.split(","))
			{
				final String role = part.trim();
				if (!role.isEmpty())
				{
					roles.add(role);
				}
			}
		}
		return roles;
	}

	private static JLabel hint(String text)
	{
		JLabel hint = new JLabel(text);
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setForeground(WdrTheme.TEXT_DIM);
		HostFormLayout.fullWidth(hint);
		return hint;
	}

	private static JPanel row(Component item)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		row.setOpaque(false);
		row.add(item);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	private static String raidLabel(String code)
	{
		if (code == null)
		{
			return "";
		}
		switch (code.toUpperCase())
		{
			case "TOB": return "ToB";
			case "COX": return "CoX";
			case "TOA": return "ToA";
			default: return code;
		}
	}

}
