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

		// Nothing between the chip and the actions once the party is full. "Party full" restated what the
		// chip now says itself, in the card's quietest ink directly beneath its loudest — one fact rendered
		// twice at opposite weights. What is left to do when full is close or edit, and both are below.
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
			// Recovery, not a forward action: it exists for the press a host regrets and should be findable
			// then rather than competing before it. Boxed at GHOST it was the third identical button in a
			// stack of five, all claiming the same rank as marking a role filled.
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
		// Labelled the way every other surface renders this value ("ph: catdog" on feed cards and the
		// draft card's truth line). Bare, the passphrase read as an unexplained word on the host's own card.
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
	 * No raid rail here. A rail earns its pixel in the feed by separating one call from the next;
	 * this card stands alone above the feed, so the rail only made the host's own post look like one
	 * more item in the list they are scrolling. The title names the raid instead.
	 *
	 * <p>A neutral outline replaces it, because the surface alone cannot hold the card: against the
	 * panel canvas it measures 1.14:1, so without an edge this card dissolved into the canvas while the
	 * railed feed cards directly beneath it stayed legible as cards. Outlined and railed now say
	 * different things — one thing, versus one of many — which is the distinction that matters here.
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
		// Data, not a control. Boxed in FIELD fill with a BORDER outline this was pixel-identical to the
		// resting GHOST buttons directly beneath it, and everywhere else in the panel that grammar means
		// "pressable". The count keeps its rank through weight instead: the card's one glanceable number,
		// bold, in primary ink, with no box to claim an affordance it does not have.
		JLabel chip = new JLabel(chipText(spots));
		chip.setFont(FontManager.getRunescapeBoldFont());
		chip.setForeground(WdrTheme.TEXT);
		return chip;
	}

	/**
	 * At zero this read "+0 open", which spent the brightest ink in the card on a null quantity and left
	 * the reader to do the arithmetic. The chip is the one thing a host checks without reading anything
	 * else, so it states the state.
	 */
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
		// No hint line. "Open spots" restated the chip two lines up ("+3 open") in quieter ink, and the
		// roles branch's hint earns its line by naming the action ("Mark role filled") where the buttons
		// name roles. "-1 spot" already names its action, so a label above it had nothing left to say.
		WdrButton minusSpot = new WdrButton("-1 spot", WdrButton.Variant.PRIMARY);
		minusSpot.addActionListener(e -> decrementSpot.run());
		HostFormLayout.fullWidth(minusSpot);
		card.add(minusSpot);
	}

	private void addActionButtons(JPanel card)
	{
		JPanel row = new JPanel(new SplitGrid(6, 0));
		row.setOpaque(false);
		// A detour, not a peer of closing the raid. Boxed at GHOST it matched "Close raid" exactly, so a
		// terminal action and a way to amend one read as the same weight of choice.
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
