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
package com.wedoraids.gallery;

import com.wedoraids.WeDoRaidsConfig;
import com.wedoraids.bridge.BridgeStatus;
import com.wedoraids.feed.DemoRecruitEntries;
import com.wedoraids.feed.RaidType;
import com.wedoraids.feed.RecruitEntry;
import com.wedoraids.host.HostDependencies;
import com.wedoraids.host.HostFormPanel;
import com.wedoraids.host.HostPreview;
import com.wedoraids.panel.PanelDependencies;
import com.wedoraids.panel.WeDoRaidsPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.laf.RuneLiteLAF;

/**
 * Design gallery: the real {@link WeDoRaidsPanel} rendered as the client shows it, with every state
 * reachable on demand. Test-source only, so none of it reaches the plugin jar.
 *
 * <p>Run {@code ./gradlew gallery} for a window with a state list and a height picker, or
 * {@code ./gradlew gallery --args="--capture [height]"} to write every state to {@code build/gallery}
 * exit.
 *
 * <p>Fidelity depends on three things worth not breaking: {@link RuneLiteLAF#setup()} runs before any
 * component exists, the panel is pinned to {@link PluginPanel#PANEL_WIDTH} plus
 * {@link PluginPanel#SCROLLBAR_WIDTH} so nothing can stretch it, and states render at real client
 * heights, where scrolling and overflow actually behave.
 *
 * <p>To add a state, extend {@link #defineStates()}. The panel's own public API covers the feed, auth
 * and blocked cases; host-form states go through {@link HostPreview}. The review workflow lives in
 * the README beside this file.
 */
public final class DesignGallery
{
	private static final int OUTER_WIDTH = PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH;
	private static final File OUT = new File("build/gallery");

	/** Client heights worth checking: fixed mode, 720p, 1080p, 1440p sidebars. */
	private static final Integer[] HEIGHTS = {503, 720, 1080, 1440};

	private final JPanel stage = new JPanel(null);
	private final JComboBox<Integer> heightPicker = new JComboBox<>(HEIGHTS);
	private final JLabel caption = new JLabel(" ");
	private final List<State> states = new ArrayList<>();
	private State current;

	public static void main(String[] args) throws Exception
	{
		// Must run before any component is created, or controls keep the default LAF.
		RuneLiteLAF.setup();
		final boolean captureOnly = args.length > 0 && "--capture".equals(args[0]);
		if (captureOnly)
		{
			final int height = args.length > 1 ? Integer.parseInt(args[1]) : 503;
			SwingUtilities.invokeAndWait(() -> new DesignGallery().captureAll(height));
			System.out.println("captured to " + OUT);
			System.exit(0);
		}
		SwingUtilities.invokeLater(() -> new DesignGallery().show());
	}

	private DesignGallery()
	{
		defineStates();
		current = states.get(0);
	}

	private void defineStates()
	{
		states.add(new State("Feed \u00b7 demo calls", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
		}));
		states.add(new State("Feed \u00b7 single raid", panel ->
		{
			live(panel);
			panel.setEntries(onlyRaid(demoEntries(), RaidType.TOB), 0);
		}));
		states.add(new State("Empty \u00b7 waiting for calls", panel ->
		{
			live(panel);
			panel.setEntries(Collections.emptyList(), 0);
		}));
		states.add(new State("Empty \u00b7 hidden by settings", panel ->
		{
			live(panel);
			panel.setEntries(Collections.emptyList(), 4);
		}));
		states.add(new State("Auth \u00b7 verification required", panel ->
		{
			panel.setLoggedIn(true);
			panel.setBridgeStatus(BridgeStatus.OFF);
		}));
		states.add(new State("Auth \u00b7 checking", panel ->
		{
			panel.setLoggedIn(true);
			panel.setBridgeStatus(BridgeStatus.CONNECTING);
		}, "some-key"));
		states.add(new State("Auth \u00b7 key not accepted", panel ->
		{
			panel.setLoggedIn(true);
			panel.setBridgeStatus(BridgeStatus.ONLINE);
		}, "some-key"));
		states.add(new State("Auth \u00b7 bridge unavailable", panel ->
		{
			panel.setLoggedIn(true);
			panel.setBridgeStatus(BridgeStatus.OFFLINE);
		}, "some-key"));
		states.add(new State("Blocked \u00b7 banned", panel ->
		{
			live(panel);
			panel.setBanned(true);
		}));
		states.add(new State("Blocked \u00b7 logged out", panel -> panel.setLoggedIn(false)));
		states.add(new State("Host \u00b7 form expanded", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.expandHostForm(panel, 0);
		}));
		states.add(new State("Host \u00b7 form expanded (CoX)", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.expandHostForm(panel, 1);
		}));
		states.add(new State("Host \u00b7 form expanded (ToA)", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.expandHostForm(panel, 2);
		}));
		states.add(new State("Host · more options open (CoX)", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.expandHostFormWithMoreOptions(panel, 1);
		}));
		states.add(new State("Host \u00b7 live post, roles", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.livePost(panel, "+2", "mdps, rdps", false);
		}));
		states.add(new State("Host \u00b7 live post, spots only", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.livePost(panel, "+3", null, false);
		}));
		states.add(new State("Host \u00b7 live post, party full", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.livePost(panel, "+0", null, false);
		}));
		states.add(new State("Host \u00b7 live post, undo offered", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.livePost(panel, "+1", "mdps", true);
		}));
		states.add(new State("Host \u00b7 inactivity warning", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.livePost(panel, "+2", "mdps, rdps", false);
			HostPreview.promptInactivity(panel);
		}));
		states.add(new State("Feed · demo data", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
		}, "", true, false));
		states.add(new State("Host · edit details", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.editLivePost(panel, "+2", "mdps, rdps");
		}));
		states.add(new State("Host · post failed", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.expandHostFormFilled(panel, 0, tobDraft());
			HostPreview.submitHostForm(panel);
		}, "", false, true));
		states.add(new State("Host · layout scouted (CoX)", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.expandHostFormFilledWithMoreOptions(panel, 1, coxDraft());
		}));
		states.add(new State("Feed · no matches", panel ->
		{
			live(panel);
			panel.setEntries(onlyRaid(demoEntries(), RaidType.TOB), 0);
		}, "", false, false, "CoX"));
		states.add(new State("Feed · hub joined", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.markHubJoined(panel, "olm");
		}));
		states.add(new State("Feed · stress content", panel ->
		{
			live(panel);
			panel.setEntries(stressEntries(), 0);
		}));
		states.add(new State("Host · posting…", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.expandHostFormFilled(panel, 0, tobDraft());
			HostPreview.submitHostForm(panel);
		}));
		states.add(new State("Host · world invalid", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			final Map<String, String> draft = tobDraft();
			draft.put("world", "4l6");
			HostPreview.expandHostFormFilled(panel, 0, draft);
			HostPreview.submitHostForm(panel);
		}));
		states.add(new State("Host · scale invalid", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			final Map<String, String> draft = coxDraft();
			draft.put("scale", "150");
			HostPreview.expandHostFormFilled(panel, 1, draft);
			HostPreview.submitHostForm(panel);
		}));
		states.add(new State("Host · close failed", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			HostPreview.closeLivePost(panel, "+2", "mdps, rdps");
		}, "", false, true));
	}

	private void show()
	{
		OUT.mkdirs();
		JFrame frame = new JFrame("We Do Raids \u2014 design gallery");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout(12, 0));
		frame.getContentPane().setBackground(new Color(24, 24, 24));

		DefaultListModel<String> model = new DefaultListModel<>();
		states.forEach(state -> model.addElement(state.name));
		JList<String> list = new JList<>(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.addListSelectionListener(event ->
		{
			if (!event.getValueIsAdjusting() && list.getSelectedIndex() >= 0)
			{
				current = states.get(list.getSelectedIndex());
				render();
			}
		});

		JButton captureAll = new JButton("Capture all to " + OUT);
		captureAll.addActionListener(event -> captureAll());
		heightPicker.setSelectedItem(503);
		heightPicker.addActionListener(event -> render());

		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
		controls.setBackground(new Color(24, 24, 24));
		controls.add(label("State"));
		JScrollPane listScroll = new JScrollPane(list);
		listScroll.setPreferredSize(new Dimension(260, 380));
		listScroll.setMaximumSize(new Dimension(260, 380));
		listScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		controls.add(listScroll);
		controls.add(label("Client height"));
		heightPicker.setMaximumSize(new Dimension(260, 28));
		heightPicker.setAlignmentX(Component.LEFT_ALIGNMENT);
		controls.add(heightPicker);
		captureAll.setAlignmentX(Component.LEFT_ALIGNMENT);
		captureAll.setMaximumSize(new Dimension(260, 32));
		controls.add(captureAll);
		caption.setForeground(new Color(190, 190, 190));
		caption.setAlignmentX(Component.LEFT_ALIGNMENT);
		controls.add(caption);

		stage.setBackground(new Color(24, 24, 24));
		frame.add(controls, BorderLayout.WEST);
		frame.add(stage, BorderLayout.CENTER);
		render();
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private static JLabel label(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(new Color(150, 150, 150));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	/** Rebuilds the panel from scratch so no state leaks between selections. */
	private WeDoRaidsPanel build(State state, int height)
	{
		WeDoRaidsPanel panel = newPanel(state);
		state.apply.accept(panel);
		panel.setBounds(0, 0, OUTER_WIDTH, height);
		layoutTree(panel);
		return panel;
	}

	private void render()
	{
		final int height = (Integer) heightPicker.getSelectedItem();
		stage.removeAll();
		WeDoRaidsPanel panel = build(current, height);
		stage.add(panel);
		stage.setPreferredSize(new Dimension(OUTER_WIDTH, height));
		caption.setText(OUTER_WIDTH + "\u00d7" + height + " \u00b7 " + current.name);
		stage.revalidate();
		stage.repaint();
		SwingUtilities.getWindowAncestor(stage).pack();
	}

	private void captureAll()
	{
		captureAll((Integer) heightPicker.getSelectedItem());
	}

	private void captureAll(int height)
	{
		OUT.mkdirs();
		int index = 0;
		for (State state : states)
		{
			WeDoRaidsPanel panel = build(state, height);
			JPanel holder = new JPanel(null);
			holder.setSize(OUTER_WIDTH, height);
			holder.add(panel);
			holder.doLayout();
			BufferedImage image = new BufferedImage(OUTER_WIDTH, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = image.createGraphics();
			holder.paint(graphics);
			graphics.dispose();
			try
			{
				final String name = String.format("%02d-%s-%d.png", index++,
					state.name.replaceAll("[^A-Za-z0-9]+", "-").toLowerCase(), height);
				ImageIO.write(image, "png", new File(OUT, name));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		caption.setText("Captured " + states.size() + " states at " + height + "px");
	}

	private static void live(WeDoRaidsPanel panel)
	{
		panel.setLoggedIn(true);
		panel.setVerified(true);
		panel.setBridgeStatus(BridgeStatus.ONLINE);
	}

	private static List<RecruitEntry> demoEntries()
	{
		return DemoRecruitEntries.create(Instant.now());
	}

	private static List<RecruitEntry> onlyRaid(List<RecruitEntry> entries, RaidType raid)
	{
		final List<RecruitEntry> out = new ArrayList<>();
		for (RecruitEntry entry : entries)
		{
			if (entry.getRaidType() == raid)
			{
				out.add(entry);
			}
		}
		return out;
	}

	/** A submittable ToB draft, the values a host would have picked by hand. */
	private static Map<String, String> tobDraft()
	{
		final Map<String, String> values = new LinkedHashMap<>();
		values.put("raid", "TOB");
		values.put("tier", "Standard");
		values.put("spots", "+2");
		values.put("size", "4");
		values.put("world", "416");
		values.put("partyHub", "catdog");
		return values;
	}

	/** A CoX draft whose tier keeps the scouted-layout row applicable (non-CM). */
	private static Map<String, String> coxDraft()
	{
		final Map<String, String> values = new LinkedHashMap<>();
		values.put("raid", "COX");
		values.put("tier", "Scaled");
		values.put("spots", "+2");
		values.put("size", "5");
		values.put("world", "416");
		values.put("partyHub", "catdog");
		values.put("fc", "Zezima");
		return values;
	}

	/**
	 * Entries at the panel's limits, every value the bridge could really send at its longest: a
	 * maximal detail line, wall-of-text messages, a 12-character RSN, an hours-old timestamp and a
	 * long party hub. The demo feed shows the panel at its best; this state shows it surviving.
	 */
	private static List<RecruitEntry> stressEntries()
	{
		final Instant now = Instant.now();
		return Arrays.asList(
			new RecruitEntry("Longest Name", "WDR ToB", RaidType.TOB, "HM Exp", "100+ kc", "+3",
				"mdps/rdps/nfrz/sfrz", "5 man", 520, "eu", "sapphireglacier", 903, "RAID",
				"looking for experienced hard mode five man for hmt6 grind tonight, scythe and 100+ hm kc"
					+ " required, no mercy splits, bring own supplies, ph sapphireglacier",
				now.minus(Duration.ofMinutes(65))),
			new RecruitEntry("Yappity Yap", "WDR CoX", RaidType.COX, null, null, null, null, null, 0, null,
				null, 12, "LFG",
				"hey everyone im back after a long break and looking to get into cox again, did a few normals"
					+ " years ago but pretty rusty now, happy to bring supplies and listen, can raid most"
					+ " evenings after 8pm uk time",
				now.minus(Duration.ofMinutes(1))),
			new RecruitEntry("Kit Chaser", "WDR ToA", RaidType.TOA, "450+", "450-540 invo", "+1", null,
				"trio", 301, null, "verylongpartyhubname", 618, "RAID",
				"expert trio 450+ w301 ph: verylongpartyhubname", now));
	}

	private static WeDoRaidsPanel newPanel(State state)
	{
		final WeDoRaidsConfig config = new WeDoRaidsConfig()
		{
			@Override
			public String remoteFeedKey()
			{
				return state.key;
			}

			@Override
			public boolean demoData()
			{
				return state.demo;
			}

			@Override
			public String lastRaidFilter()
			{
				return state.raidFilter;
			}
		};
		final HostFormPanel.HostActions actions = new HostFormPanel.HostActions()
		{
			@Override
			public void submit(Map<String, String> fields, Consumer<String> status)
			{
				if (state.bridgeDown)
				{
					// The reply the real bridge client gives when the request cannot go out.
					status.accept("Could not reach the bridge.");
				}
			}

			@Override
			public void update(Map<String, String> fields, Consumer<String> status)
			{
				if (state.bridgeDown)
				{
					status.accept("Could not reach the bridge.");
				}
			}

			@Override
			public void close(Map<String, String> fields, Consumer<String> status)
			{
				if (state.bridgeDown)
				{
					status.accept("Could not reach the bridge.");
				}
			}
		};
		final HostDependencies host = new HostDependencies(actions, () -> 416, () -> "Tekton",
			() -> "Zezima", raid -> 250, () ->
		{
		}, () ->
		{
		}, () -> true, world -> null);
		final PanelDependencies panelDeps = new PanelDependencies((k, v) ->
		{
		}, world ->
		{
		}, hub ->
		{
		}, () ->
		{
		});
		return new WeDoRaidsPanel(config, host, panelDeps);
	}

	/** Swing only lays out displayed hierarchies, so drive it manually for the off-screen build. */
	private static void layoutTree(Component component)
	{
		component.doLayout();
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				layoutTree(child);
			}
		}
	}


	private static final class State
	{
		private final String name;
		private final Consumer<WeDoRaidsPanel> apply;
		private final String key;
		private final boolean demo;
		private final boolean bridgeDown;
		private final String raidFilter;

		private State(String name, Consumer<WeDoRaidsPanel> apply)
		{
			this(name, apply, "", false, false, "All raids");
		}

		private State(String name, Consumer<WeDoRaidsPanel> apply, String key)
		{
			this(name, apply, key, false, false, "All raids");
		}

		private State(String name, Consumer<WeDoRaidsPanel> apply, String key, boolean demo, boolean bridgeDown)
		{
			this(name, apply, key, demo, bridgeDown, "All raids");
		}

		private State(String name, Consumer<WeDoRaidsPanel> apply, String key, boolean demo, boolean bridgeDown,
			String raidFilter)
		{
			this.name = name;
			this.apply = apply;
			this.key = key;
			this.demo = demo;
			this.bridgeDown = bridgeDown;
			this.raidFilter = raidFilter;
		}
	}
}
