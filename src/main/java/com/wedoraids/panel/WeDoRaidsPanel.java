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
import com.wedoraids.feed.RecruitEntry;
import com.wedoraids.host.HostDependencies;
import com.wedoraids.host.HostFormPanel;
import com.wedoraids.ui.WdrTheme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import net.runelite.client.ui.PluginPanel;

public class WeDoRaidsPanel extends PluginPanel
{
	private final HostFormPanel hostForm;
	private final RecruitPanelHeader header;
	private final RecruitFilterBar filterBar;
	private final RecruitListPanel recruitList;
	private boolean verified;

	public WeDoRaidsPanel(WeDoRaidsConfig config, HostDependencies hostDependencies,
		PanelDependencies panelDependencies)
	{
		super(false);
		header = new RecruitPanelHeader(config, panelDependencies.onRefresh());
		filterBar = new RecruitFilterBar(config, panelDependencies.saveFilter(), this::rebuildRecruitList);
		recruitList = new RecruitListPanel(config, filterBar, panelDependencies.saveFilter(),
			panelDependencies.onHopWorld(), panelDependencies.onJoinHub(), header::setEntryCount);

		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBackground(WdrTheme.BACKGROUND);

		// Only the title row and the status bar are fixed chrome. Everything else scrolls together:
		// an expanded host form is taller than a fixed-mode sidebar (~500px), so pinning it above the
		// feed starved the feed to zero height and pushed the raid counts into the status bar.
		hostForm = new HostFormPanel(hostDependencies);
		hostForm.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel demoBanner = header.demoBanner();
		demoBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
		header.refreshDemoBanner();

		JPanel content = new ScrollingContent();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setOpaque(false);
		// Filters, the raid counts and the rule between them only mean anything when there is a feed to
		// sort, so they travel together and disappear together.
		JPanel feedChrome = new JPanel()
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		feedChrome.setLayout(new BoxLayout(feedChrome, BoxLayout.Y_AXIS));
		feedChrome.setOpaque(false);
		feedChrome.setAlignmentX(Component.LEFT_ALIGNMENT);
		feedChrome.add(Box.createVerticalStrut(6));
		feedChrome.add(divider());
		feedChrome.add(Box.createVerticalStrut(6));
		feedChrome.add(filterBar);
		filterBar.restoreSelection();
		feedChrome.add(Box.createVerticalStrut(4));
		feedChrome.add(recruitList.countLabel());
		feedChrome.add(Box.createVerticalStrut(6));

		content.add(demoBanner);
		content.add(hostForm);
		content.add(feedChrome);
		content.add(recruitList);

		// Hosting cannot succeed while logged out, banned or unverified, and the notice already states
		// the one thing to do next, so offering a form that only fails at the bridge is noise.
		recruitList.onFeedAccessibleChanged(accessible ->
		{
			hostForm.setVisible(accessible);
			feedChrome.setVisible(accessible);
		});

		add(header, BorderLayout.NORTH);
		add(recruitList.scrollPane(content), BorderLayout.CENTER);
		add(header.statusBar(), BorderLayout.SOUTH);
		recruitList.rebuild();
	}

	/** Fills the viewport's width so card width never depends on card content. */
	private static final class ScrollingContent extends JPanel implements Scrollable
	{
		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return visibleRect.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}

	public void setBridgeStatus(BridgeStatus status)
	{
		header.setBridgeStatus(status);
		recruitList.setBridgeStatus(status);
	}

	public void setBanned(boolean banned)
	{
		recruitList.setBanned(banned);
	}

	public void setVerified(boolean verified)
	{
		if (this.verified == verified)
		{
			return;
		}
		this.verified = verified;
		recruitList.setVerified(verified);
	}

	public void refreshHostTiers()
	{
		hostForm.refreshTiersPublic();
	}

	public void refreshCoxLayout()
	{
		hostForm.refreshCoxLayout();
	}

	public void enterHostLive(String messageId)
	{
		hostForm.enterLivePost(messageId);
	}

	public void exitHostLive()
	{
		hostForm.exitLivePost();
	}

	public void shutdown()
	{
		hostForm.exitLivePost();
		hostForm.stopTimers();
	}

	public void setLoggedIn(boolean loggedIn)
	{
		recruitList.setLoggedIn(loggedIn);
	}

	public void setEntries(List<RecruitEntry> newEntries)
	{
		setEntries(newEntries, 0);
	}

	public void setEntries(List<RecruitEntry> newEntries, int hiddenByFilters)
	{
		header.refreshDemoBanner();
		recruitList.setEntries(newEntries, hiddenByFilters);
	}

	public void clear()
	{
		recruitList.clear();
	}

	private void rebuildRecruitList()
	{
		recruitList.rebuild();
	}

	private static JPanel divider()
	{
		JPanel line = new JPanel();
		line.setBackground(WdrTheme.BORDER);
		line.setPreferredSize(new Dimension(0, 1));
		line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		line.setAlignmentX(Component.LEFT_ALIGNMENT);
		return line;
	}
}
