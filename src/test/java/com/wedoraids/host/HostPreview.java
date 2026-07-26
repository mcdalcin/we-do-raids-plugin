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

import com.wedoraids.SwingProbe;
import com.wedoraids.panel.WeDoRaidsPanel;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Puts a rendered panel into a named state for the design gallery, the way a host would.
 *
 * <p>Almost everything here is a real click on a real control, found by its label in the rendered
 * tree. A harness that sets state directly can compose states the panel cannot: this one once
 * rendered a live post above a collapsed toggle, because it made the post visible instead of
 * expanding the form. Clicking the controls rules that out, and a label that moves fails the capture
 * instead of rendering something misleading.
 *
 * <p>Three states have no control to click and keep a package-private entry point:
 * {@link HostFormPanel#enterLivePost(Map, String)} and {@link HostFormPanel#offerUndo(Map)}, which
 * otherwise need a bridge round trip to answer, and {@link HostFormPanel#promptInactivity()}, whose
 * real trigger is a seven-minute timer.
 */
public final class HostPreview
{
	private static final String[] RAID_TABS = {"ToB", "CoX", "ToA"};

	private HostPreview()
	{
	}

	/** Opens the form on a raid tab, as clicking the toggle and then that tab does. */
	public static void expandHostForm(WeDoRaidsPanel panel, int raidTab)
	{
		expand(panel);
		SwingProbe.click(panel, RAID_TABS[raidTab]);
	}

	/** Opens the form on a raid tab with the More options disclosure open. */
	public static void expandHostFormWithMoreOptions(WeDoRaidsPanel panel, int raidTab)
	{
		expandHostForm(panel, raidTab);
		SwingProbe.click(panel, "More options");
	}

	/** Opens the form on a raid tab and fills the draft, as setting each control by hand does. */
	public static void expandHostFormFilled(WeDoRaidsPanel panel, int raidTab, Map<String, String> values)
	{
		expandHostForm(panel, raidTab);
		fill(panel, values);
	}

	/** A filled draft with the More options disclosure open. */
	public static void expandHostFormFilledWithMoreOptions(WeDoRaidsPanel panel, int raidTab,
		Map<String, String> values)
	{
		expandHostFormFilled(panel, raidTab, values);
		SwingProbe.click(panel, "More options");
	}

	/** Clicks Post to Discord; the gallery's own actions decide the reply. */
	public static void submitHostForm(WeDoRaidsPanel panel)
	{
		SwingProbe.click(panel, "Post to Discord");
	}

	/** A live post reopened for editing, as clicking Edit details does. */
	public static void editLivePost(WeDoRaidsPanel panel, String spots, String roles)
	{
		livePost(panel, spots, roles, false);
		SwingProbe.click(panel, "Edit details");
	}

	/** Clicks Close raid on a live post; the gallery's actions decide the reply. */
	public static void closeLivePost(WeDoRaidsPanel panel, String spots, String roles)
	{
		livePost(panel, spots, roles, false);
		SwingProbe.click(panel, "Close raid");
	}

	/**
	 * Renders a live post for a ToB raid.
	 *
	 * <p>The form is expanded first because that is the only route a host has to one: they open the
	 * form, fill it and submit. The post itself is handed to the panel rather than clicked into
	 * existence, because confirming it needs a reply the gallery's bridge never sends.
	 *
	 * @param roles comma-separated role names, or {@code null} for a spots-only post
	 * @param undo  whether a previous value is on offer, as it would be after a real update
	 */
	public static void livePost(WeDoRaidsPanel panel, String spots, String roles, boolean undo)
	{
		final Map<String, String> fields = new LinkedHashMap<>();
		fields.put("raid", "TOB");
		fields.put("tier", "Standard");
		fields.put("world", "416");
		fields.put("partyHub", "catdog");
		fields.put("spots", spots);
		if (roles != null)
		{
			fields.put("roles", roles);
		}
		expand(panel);
		final HostFormPanel form = SwingProbe.only(panel, HostFormPanel.class);
		form.enterLivePost(fields, "msg-1");
		if (undo)
		{
			final Map<String, String> previous = new LinkedHashMap<>(fields);
			previous.put("spots", "+2");
			form.offerUndo(previous);
		}
	}

	/** Shows the idle prompt now instead of waiting out the inactivity timer. */
	public static void promptInactivity(WeDoRaidsPanel panel)
	{
		SwingProbe.only(panel, HostFormPanel.class).promptInactivity();
	}

	/** Presses a card's party hub link, as joining that hub from the feed does. */
	public static void markHubJoined(WeDoRaidsPanel panel, String hub)
	{
		for (JLabel label : SwingProbe.all(panel, JLabel.class))
		{
			final String text = label.getText();
			if (text != null && text.contains("ph: ") && text.contains(hub) && !text.contains("joined"))
			{
				SwingProbe.press(label);
				return;
			}
		}
		throw new AssertionError("No unjoined party hub link for \"" + hub + "\"");
	}

	/** Opens the form when it is closed, leaving it alone when a caller already opened it. */
	private static void expand(WeDoRaidsPanel panel)
	{
		if (SwingProbe.hasButton(panel, "Host raid"))
		{
			SwingProbe.click(panel, "Host raid");
		}
	}

	/** Sets each draft value on the control that owns it, firing the listeners a host would. */
	private static void fill(WeDoRaidsPanel panel, Map<String, String> values)
	{
		select(panel, values.get("tier"));
		combo(panel, "Open spots", values.get("spots"));
		combo(panel, "Team size", values.get("size"));
		text(panel, "World", values.get("world"));
		text(panel, "Party hub", values.get("partyHub"));
		text(panel, "Friends chat", values.get("fc"));
		text(panel, "Scale (0-100)", values.get("scale"));
		text(panel, "Layout", values.get("layout"));
	}

	private static void select(WeDoRaidsPanel panel, String tier)
	{
		if (tier != null)
		{
			SwingProbe.selectOffered(panel, tier);
		}
	}

	private static void combo(WeDoRaidsPanel panel, String caption, String value)
	{
		if (value != null)
		{
			SwingProbe.labelled(panel, caption, JComboBox.class).setSelectedItem(value);
		}
	}

	private static void text(WeDoRaidsPanel panel, String caption, String value)
	{
		if (value != null)
		{
			SwingProbe.labelled(panel, caption, JTextField.class).setText(value);
		}
	}
}
