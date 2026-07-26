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
package com.wedoraids.panel;

import com.wedoraids.WeDoRaidsConfig;
import com.wedoraids.bridge.BridgeStatus;
import com.wedoraids.feed.RaidType;
import com.wedoraids.feed.RecruitEntry;
import com.wedoraids.ui.SplitGrid;
import com.wedoraids.ui.WdrButton;
import com.wedoraids.ui.WdrScrollBarUI;
import com.wedoraids.ui.WdrTheme;
import com.wedoraids.ui.WrappedText;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

/** The feed list. Sits inside the panel's shared scrolling content, below the host form. */
final class RecruitListPanel extends JPanel
{
	private static final int MAX_ENTRIES = 50;
	private static final String DISCORD_INVITE = "https://discord.gg/wdr";
	/** Identical feeds skip the rebuild while younger than this, so "Xm ago" labels still refresh. */
	private static final Duration REBUILD_MAX_AGE = Duration.ofSeconds(60);

	private final List<RecruitEntry> entries = new ArrayList<>();
	private final WeDoRaidsConfig config;
	private final RecruitFilterBar filterBar;
	private final BiConsumer<String, String> saveConfig;
	private final IntConsumer onHopWorld;
	private final Consumer<String> onJoinHub;
	private final IntConsumer onEntryCountChanged;
	private final JLabel countLabel = new JLabel();
	/** Hubs joined this session, kept here so the label survives poll-driven rebuilds. */
	private final Set<String> joinedHubs = new HashSet<>();
	private final JPasswordField keyField = new JPasswordField();
	private JPanel keyEntry;
	private JScrollPane scroll;
	private BridgeStatus bridgeStatus = BridgeStatus.OFF;
	private int hiddenByFilters;
	private boolean banned;
	private boolean verified;
	private boolean loggedIn = true;
	private Instant lastRebuildAt = Instant.EPOCH;
	private Consumer<Boolean> onFeedAccessibleChanged;

	RecruitListPanel(WeDoRaidsConfig config, RecruitFilterBar filterBar, BiConsumer<String, String> saveConfig,
		IntConsumer onHopWorld, Consumer<String> onJoinHub, IntConsumer onEntryCountChanged)
	{
		this.config = config;
		this.filterBar = filterBar;
		this.saveConfig = saveConfig;
		this.onHopWorld = onHopWorld;
		this.onJoinHub = onJoinHub;
		this.onEntryCountChanged = onEntryCountChanged;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		countLabel.setFont(FontManager.getRunescapeSmallFont());
	}

	JLabel countLabel()
	{
		return countLabel;
	}

	/** Wraps the scrolling content, keeping a handle so rebuilds can restore the scroll offset. */
	JScrollPane scrollPane(JPanel content)
	{
		JScrollPane scrollPane = new JScrollPane(content,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		scrollPane.setBackground(WdrTheme.BACKGROUND);
		scrollPane.getViewport().setBackground(WdrTheme.BACKGROUND);
		scrollPane.getVerticalScrollBar().setUI(new WdrScrollBarUI());
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scroll = scrollPane;
		return scrollPane;
	}

	void setBridgeStatus(BridgeStatus status)
	{
		if (bridgeStatus == status)
		{
			return;
		}
		bridgeStatus = status;
		// Only the verification notices depend on bridge state; skip the churn otherwise.
		if (loggedIn && !banned && !verified)
		{
			rebuild();
		}
	}

	void setBanned(boolean banned)
	{
		if (this.banned == banned)
		{
			return;
		}
		this.banned = banned;
		if (banned)
		{
			entries.clear();
		}
		rebuild();
	}

	void setVerified(boolean verified)
	{
		this.verified = verified;
		if (!verified)
		{
			entries.clear();
		}
		rebuild();
	}

	/**
	 * Whether the feed is reachable at all. When it is not, the notice is the whole panel: filters
	 * would sort nothing and the host form cannot post, so the surrounding chrome is hidden rather
	 * than left present and inert.
	 */
	boolean feedAccessible()
	{
		return loggedIn && !banned && verified;
	}

	void onFeedAccessibleChanged(Consumer<Boolean> listener)
	{
		onFeedAccessibleChanged = listener;
		listener.accept(feedAccessible());
	}

	void setLoggedIn(boolean loggedIn)
	{
		if (this.loggedIn == loggedIn)
		{
			return;
		}
		this.loggedIn = loggedIn;
		if (!loggedIn)
		{
			entries.clear();
		}
		rebuild();
	}

	void setEntries(List<RecruitEntry> newEntries, int hiddenByFilters)
	{
		final List<RecruitEntry> capped = new ArrayList<>();
		if (!banned && verified && loggedIn)
		{
			for (RecruitEntry entry : newEntries)
			{
				if (capped.size() >= MAX_ENTRIES)
				{
					break;
				}
				capped.add(entry);
			}
		}
		final boolean unchanged = capped.equals(entries) && hiddenByFilters == this.hiddenByFilters;
		this.hiddenByFilters = hiddenByFilters;
		if (unchanged && Duration.between(lastRebuildAt, Instant.now()).compareTo(REBUILD_MAX_AGE) < 0)
		{
			return;
		}
		entries.clear();
		entries.addAll(capped);
		rebuild();
	}

	void clear()
	{
		entries.clear();
		hiddenByFilters = 0;
		rebuild();
	}


	void rebuild()
	{
		lastRebuildAt = Instant.now();
		final int scrollValue = scroll == null ? 0 : scroll.getVerticalScrollBar().getValue();
		removeAll();
		updateCounts();
		onEntryCountChanged.accept(entries.size());
		if (onFeedAccessibleChanged != null)
		{
			onFeedAccessibleChanged.accept(feedAccessible());
		}

		if (!loggedIn)
		{
			add(buildNotice("Log in",
				"Log in on your verified We Do Raids account to see and host raids."));
		}
		else if (banned)
		{
			add(buildNotice("Recruitment hidden",
				"This account is on the We Do Raids ban list, so recruiting calls are hidden and you can't host."));
		}
		else if (!verified)
		{
			addVerificationNotice();
		}
		else
		{
			addEntriesOrEmptyState();
		}

		revalidate();
		repaint();
		if (scroll != null)
		{
			final JScrollPane scrollPane = scroll;
			SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(scrollValue));
		}
	}

	private void addEntriesOrEmptyState()
	{
		final List<RecruitEntry> visible = visibleEntries();
		if (!visible.isEmpty())
		{
			for (RecruitEntry entry : visible)
			{
				add(new RecruitEntryPanel(entry, onHopWorld, this::joinHub, joinedHubs::contains));
				add(Box.createVerticalStrut(6));
			}
			return;
		}
		if (!entries.isEmpty())
		{
			add(buildNotice("No matches",
				"No open raids match this filter. Try 'All raids' / 'All tiers'."));
		}
		else if (hiddenByFilters > 0)
		{
			add(buildNotice("Calls hidden by settings",
				hiddenByFilters + (hiddenByFilters == 1 ? " call is" : " calls are")
					+ " hidden by raid, tier or keyword filters in the We Do Raids plugin settings."));
		}
		else
		{
			add(buildNotice("Waiting for calls",
				"Recruiting calls from the We Do Raids Discord will appear here."));
		}
	}

	private void joinHub(String hub)
	{
		joinedHubs.add(hub);
		onJoinHub.accept(hub);
	}

	private void addVerificationNotice()
	{
		if (config.remoteFeedKey().trim().isEmpty())
		{
			add(buildNotice("Verification required",
				"Run !verify in #auth on the We Do Raids Discord to get your key, then paste it below."));
			add(Box.createVerticalStrut(6));
			add(keyEntryPanel());
		}
		else if (bridgeStatus == BridgeStatus.ONLINE)
		{
			add(buildNotice("Key not accepted",
				"The bridge did not verify this key, or your logged-in RSN doesn't match your WDR nickname. "
					+ "Run !verify again and paste the new key below."));
			add(Box.createVerticalStrut(6));
			add(keyEntryPanel());
		}
		else if (bridgeStatus == BridgeStatus.OFFLINE)
		{
			add(buildNotice("Bridge unavailable",
				"Can't reach the We Do Raids server to check your key. It retries automatically."));
		}
		else
		{
			add(buildNotice("Checking verification",
				"Contacting the We Do Raids bridge…"));
		}
	}

	/** Built once and re-added across rebuilds, so typed key text survives them. */
	private JPanel keyEntryPanel()
	{
		if (keyEntry != null)
		{
			return keyEntry;
		}
		keyEntry = new JPanel();
		keyEntry.setLayout(new BoxLayout(keyEntry, BoxLayout.Y_AXIS));
		keyEntry.setOpaque(false);
		keyEntry.setAlignmentX(Component.LEFT_ALIGNMENT);
		WdrTheme.styleField(keyField);
		keyField.setAlignmentX(Component.LEFT_ALIGNMENT);
		keyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, keyField.getPreferredSize().height));
		keyEntry.add(keyField);
		keyEntry.add(Box.createVerticalStrut(6));
		JPanel buttons = new JPanel(new SplitGrid(6, 0));
		buttons.setOpaque(false);
		WdrButton save = new WdrButton("Save key", WdrButton.Variant.PRIMARY);
		save.addActionListener(e -> saveKey());
		buttons.add(save);
		WdrButton discord = new WdrButton("WDR Discord", WdrButton.Variant.GHOST);
		discord.setToolTipText("Open " + DISCORD_INVITE + " in your browser");
		discord.addActionListener(e -> LinkBrowser.browse(DISCORD_INVITE));
		buttons.add(discord);
		buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
		buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttons.getPreferredSize().height));
		keyEntry.add(buttons);
		keyEntry.setMaximumSize(new Dimension(Integer.MAX_VALUE, keyEntry.getPreferredSize().height));
		return keyEntry;
	}

	private void saveKey()
	{
		final String key = new String(keyField.getPassword()).trim();
		if (key.isEmpty())
		{
			return;
		}
		saveConfig.accept("remoteFeedKey", key);
		keyField.setText("");
	}

	private List<RecruitEntry> visibleEntries()
	{
		final RaidType raid = filterBar.selectedRaid();
		final String tier = filterBar.selectedTier();
		final List<RecruitEntry> visible = new ArrayList<>();
		for (RecruitEntry entry : entries)
		{
			if (raid != null && entry.getRaidType() != raid)
			{
				continue;
			}
			if (tier != null && !tier.equalsIgnoreCase(entry.getTier()))
			{
				continue;
			}
			visible.add(entry);
		}
		return visible;
	}

	private void updateCounts()
	{
		countLabel.setText("<html>"
			+ chip(RaidType.TOB, countEntries(RaidType.TOB)) + "&nbsp;&nbsp;"
			+ chip(RaidType.COX, countEntries(RaidType.COX)) + "&nbsp;&nbsp;"
			+ chip(RaidType.TOA, countEntries(RaidType.TOA)) + "</html>");
	}

	private static JPanel buildNotice(String title, String description)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(WdrTheme.CARD);
		panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(WdrTheme.TEXT);
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(titleLabel);
		panel.add(Box.createVerticalStrut(4));
		JLabel descLabel = new JLabel(WrappedText.centered(description, WrappedText.CARD));
		descLabel.setForeground(WdrTheme.TEXT_DIM);
		descLabel.setFont(FontManager.getRunescapeSmallFont());
		descLabel.setHorizontalAlignment(SwingConstants.CENTER);
		descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(descLabel);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
		return panel;
	}

	private static String chip(RaidType raid, int count)
	{
		return "<span style='color:" + hex(raid.getColor()) + "'>"
			+ raid.getDisplayName() + " " + count + "</span>";
	}

	private int countEntries(RaidType raid)
	{
		return (int) entries.stream().filter(entry -> entry.getRaidType() == raid).count();
	}

	private static String hex(Color color)
	{
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
}
