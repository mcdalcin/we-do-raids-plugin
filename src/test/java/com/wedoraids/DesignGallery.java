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
package com.wedoraids;

import com.wedoraids.bridge.BridgeStatus;
import com.wedoraids.feed.DemoRecruitEntries;
import com.wedoraids.feed.RaidType;
import com.wedoraids.panel.PanelPreview;
import com.wedoraids.feed.RecruitEntry;
import com.wedoraids.host.HostDependencies;
import com.wedoraids.host.HostFormPanel;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
 * Design gallery: the real {@link WeDoRaidsPanel} rendered exactly as the client shows it, with
 * every state reachable on demand.
 *
 * <p>Fidelity rests on three things, all of which the earlier throwaway screenshot driver got
 * wrong or skipped:
 *
 * <ul>
 *   <li><b>Look and feel.</b> {@link RuneLiteLAF#setup()} installs the client's FlatLaf theme and
 *       its UI delegates. Without it, combo boxes, checkboxes and plain buttons paint in Swing's
 *       default Metal look, and their preferred sizes differ, so vertical rhythm is wrong too.</li>
 *   <li><b>Width.</b> The sidebar is {@link PluginPanel#PANEL_WIDTH} plus
 *       {@link PluginPanel#SCROLLBAR_WIDTH}. The panel is given exactly that, via absolute bounds,
 *       so nothing can stretch it.</li>
 *   <li><b>Height.</b> Scrolling only behaves correctly at a real client height. Fixed mode is
 *       roughly 500px, which is the case that decides how many calls are visible before the
 *       scrollbar appears.</li>
 * </ul>
 *
 * <p>Run with {@code ./gradlew gallery}, or {@code --capture [height]} to write every state to
 * /tmp/wdr-gallery without opening a window.
 *
 * <p>This class lives on the design-gallery branch only. It reaches into private state to force
 * states that cannot be reached through the UI, and that reflection is deliberately kept off the
 * branch submitted for plugin-hub review. Rebase this branch on the main branch to pick up UI
 * changes, then run the gallery against them.
 */
public final class DesignGallery
{
	private static final int OUTER_WIDTH = PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH;
	private static final File OUT = new File("/tmp/wdr-gallery");

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
			PanelPreview.expandHostForm(panel, 0);
		}));
		states.add(new State("Host \u00b7 form expanded (CoX)", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			PanelPreview.expandHostForm(panel, 1);
		}));
		states.add(new State("Host \u00b7 form expanded (ToA)", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			PanelPreview.expandHostForm(panel, 2);
		}));
		states.add(new State("Host \u00b7 live post, roles", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			PanelPreview.livePost(panel, "+2", "mdps, rdps", false);
		}));
		states.add(new State("Host \u00b7 live post, spots only", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			PanelPreview.livePost(panel, "+3", null, false);
		}));
		states.add(new State("Host \u00b7 live post, party full", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			PanelPreview.livePost(panel, "+0", null, false);
		}));
		states.add(new State("Host \u00b7 live post, undo offered", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			PanelPreview.livePost(panel, "+1", "mdps", true);
		}));
		states.add(new State("Host \u00b7 inactivity warning", panel ->
		{
			live(panel);
			panel.setEntries(demoEntries(), 0);
			PanelPreview.livePost(panel, "+2", "mdps, rdps", false);
			PanelPreview.promptInactivity(panel);
		}));
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

		JButton captureAll = new JButton("Capture all to /tmp/wdr-gallery");
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
		WeDoRaidsPanel panel = newPanel(state.key);
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




	private static WeDoRaidsPanel newPanel(String key)
	{
		final WeDoRaidsConfig config = new WeDoRaidsConfig()
		{
			@Override
			public String remoteFeedKey()
			{
				return key;
			}
		};
		final HostFormPanel.HostActions actions = new HostFormPanel.HostActions()
		{
			@Override
			public void submit(Map<String, String> fields, Consumer<String> status)
			{
			}

			@Override
			public void update(Map<String, String> fields, Consumer<String> status)
			{
			}

			@Override
			public void close(Map<String, String> fields, Consumer<String> status)
			{
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

		private State(String name, Consumer<WeDoRaidsPanel> apply)
		{
			this(name, apply, "");
		}

		private State(String name, Consumer<WeDoRaidsPanel> apply, String key)
		{
			this.name = name;
			this.apply = apply;
			this.key = key;
		}
	}
}
