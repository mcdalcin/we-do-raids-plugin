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
import com.wedoraids.ui.WdrButton;
import com.wedoraids.ui.WdrTheme;
import com.wedoraids.ui.WrappedText;
import java.awt.Component;
import java.awt.Rectangle;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.FontManager;

/**
 * The collapsed-by-default disclosure holding the fields a host rarely retouches: world override,
 * party hub, friends chat, other roles, description and the CoX layout editor. Copy that used to
 * hide in tooltips is surfaced here as visible dim hints, so guidance survives a glance.
 */
final class HostMoreOptions extends JPanel
{
	/** Fields addressable for focus, so a hidden-field validation failure can open here and land on it. */
	enum Field
	{
		WORLD, PARTY_HUB, FRIENDS_CHAT, OTHER_ROLES, DESCRIPTION, LAYOUT
	}

	private static final String HUB_HINT = "Optional passphrase joiners use with the RuneLite Party plugin";
	/** The hub hint wraps inside a nested capped panel granted less width than the bare panel measure. */
	private static final int HUB_HINT_WIDTH = 145;

	private final Runnable onChange;
	private final JTextField world = new JTextField();
	private final JTextField partyHub = new JTextField();
	private final JTextField friendsChat = new JTextField();
	private final JTextField roles = new JTextField();
	private final JTextField layout = new JTextField();
	private final JTextField description = new JTextField();
	private final JLabel partyHubHint = new JLabel();
	private final JLabel layoutHint = new JLabel();
	private final WdrButton toggle = new WdrButton("More options", WdrButton.Variant.QUIET);
	private final JPanel content = new JPanel();
	private JPanel friendsChatRow;
	private JPanel layoutRow;
	private boolean open;

	HostMoreOptions(Runnable onChange)
	{
		this.onChange = onChange;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		setAlignmentX(Component.LEFT_ALIGNMENT);
		for (JTextField field : new JTextField[]{world, partyHub, friendsChat, roles, layout, description})
		{
			WdrTheme.styleField(field);
			field.getDocument().addDocumentListener(HostFormLayout.onChange(onChange));
		}
		HostFormLayout.fullWidth(toggle);
		toggle.addActionListener(event -> setOpen(!open));
		add(toggle);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setOpaque(false);
		content.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.setVisible(false);
		content.add(HostFormLayout.labeled("World", world));
		content.add(buildHubRow());
		friendsChatRow = HostFormLayout.labeled("Friends chat", friendsChat);
		content.add(friendsChatRow);
		content.add(HostFormLayout.labeled("Other roles", roles));
		content.add(HostFormLayout.labeledWithHint("Description", description,
			HostFormLayout.hint("e.g. pogstack, max only")));
		layoutHint.setFont(FontManager.getRunescapeSmallFont());
		layoutHint.setForeground(WdrTheme.TEXT_DIM);
		layoutHint.setAlignmentX(Component.LEFT_ALIGNMENT);
		layoutRow = HostFormLayout.labeledWithHint("Layout", layout, layoutHint);
		content.add(layoutRow);
		add(content);

		partyHub.setToolTipText(HUB_HINT);
		description.setToolTipText("e.g. pogstack, max only");
	}

	private JPanel buildHubRow()
	{
		partyHubHint.setFont(FontManager.getRunescapeSmallFont());
		partyHubHint.setForeground(WdrTheme.TEXT_DIM);
		partyHubHint.setAlignmentX(Component.LEFT_ALIGNMENT);
		setHubHint(false);
		return HostFormLayout.labeledWithHint("Party hub", partyHub, partyHubHint);
	}

	// --- disclosure state ---

	private void setOpen(boolean value)
	{
		open = value;
		content.setVisible(value);
		toggle.setText(value ? "Fewer options" : "More options");
		// The draft card's truth line mirrors these fields while they are hidden, so toggling the
		// disclosure changes what the card should show and has to refresh it like an edit does.
		onChange.run();
		revalidate();
		repaint();
	}

	boolean isOpen()
	{
		return open;
	}

	void openAndFocus(Field field)
	{
		setOpen(true);
		final JTextField target = fieldFor(field);
		// The edge is the association the status line cannot carry from five fields away.
		WdrTheme.flagInvalid(target);
		// The validation that lands here fires from the submit button near the bottom of a viewport
		// that can be ~500px tall, and opening the disclosure grows the form past it. Without a scroll,
		// the field the host must fix and the error explaining why both sit below the fold, so the
		// click appears to do nothing. Deferred, because the field was invisible a moment ago: its
		// bounds only exist after the revalidate this open queued has run.
		SwingUtilities.invokeLater(() ->
		{
			target.scrollRectToVisible(new Rectangle(0, 0, target.getWidth(), target.getHeight()));
			target.requestFocusInWindow();
		});
	}

	private JTextField fieldFor(Field field)
	{
		switch (field)
		{
			case PARTY_HUB:
				return partyHub;
			case FRIENDS_CHAT:
				return friendsChat;
			case OTHER_ROLES:
				return roles;
			case DESCRIPTION:
				return description;
			case LAYOUT:
				return layout;
			case WORLD:
			default:
				return world;
		}
	}

	// --- raid applicability ---

	void setRaid(RaidType raid)
	{
		friendsChatRow.setVisible(raid == RaidType.COX);
	}

	void setLayoutEditorVisible(boolean visible)
	{
		layoutRow.setVisible(visible);
	}

	/**
	 * Mirrors the card's scout line into the editor's own hint, reusing the {@code Scout:} wording so
	 * the card and More agree, and the editor is never a bare box that asks for input and explains
	 * nothing: the hint states what was scouted, or that nothing has been detected yet.
	 */
	void setLayoutScout(String text)
	{
		layoutHint.setText(WrappedText.html(text, HUB_HINT_WIDTH));
	}

	// --- values ---

	String getWorld()
	{
		return world.getText();
	}

	String getPartyHub()
	{
		return partyHub.getText();
	}

	String getFriendsChat()
	{
		return friendsChat.getText();
	}

	String getRoles()
	{
		return roles.getText();
	}

	String getLayoutText()
	{
		return layout.getText();
	}

	String getDescription()
	{
		return description.getText();
	}

	void setWorld(String value)
	{
		world.setText(value);
	}

	void setFriendsChat(String value)
	{
		friendsChat.setText(value);
	}

	void setRoles(String value)
	{
		roles.setText(value);
	}

	void setLayout(String value)
	{
		layout.setText(value);
	}

	void setDescription(String value)
	{
		description.setText(value);
	}

	void setPartyHub(String value, boolean generated)
	{
		partyHub.setText(value);
		setHubHint(generated);
	}

	boolean isPartyHubEmpty()
	{
		return partyHub.getText().trim().isEmpty();
	}

	private void setHubHint(boolean generated)
	{
		final String text = generated ? HUB_HINT + " \u00b7 generated for you" : HUB_HINT;
		partyHubHint.setText(WrappedText.html(text, HUB_HINT_WIDTH));
	}

	void reset()
	{
		world.setText("");
		partyHub.setText("");
		friendsChat.setText("");
		roles.setText("");
		layout.setText("");
		description.setText("");
		setHubHint(false);
		setOpen(false);
	}
}
