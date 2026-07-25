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

import com.wedoraids.ui.WdrTheme;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Off-screen render harness for the host form. Writes PNGs of each state to {@link #OUT} and prints
 * the measured panel height, so the design can be inspected without a running client.
 */
public final class DesignShot
{
	private static final int PANEL_WIDTH = 225;
	private static final String OUT = "/tmp/wdr-shots";

	private DesignShot()
	{
	}

	public static void main(String[] args) throws Exception
	{
		new File(OUT).mkdirs();
		SwingUtilities.invokeAndWait(DesignShot::run);
		System.out.println("done");
		System.exit(0);
	}

	private static void run()
	{
		try
		{
shot("01-collapsed", panel(null, false));
shot("02-expanded-no-raid", panel(null, true));
shot("03-tob", panel(0, true));
shot("04-cox", panel(1, true));
shot("05-toa", panel(2, true));
shot("06-live-roles", live("+2", "mdps, rdps"));
shot("07-live-spots", live("+3", ""));
shot("08-tob-more", moreExpanded());
			shot("09-edit", editMode());
			shot("10-tob-ready", ready());
			shot("11-tier-limited", tierLimited());
			shot("12-error", errorStatus());
			shot("13-cox-cm", coxCm());
			shot("14-overflow", overflow());
			shot("15-cox-more", coxMore());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private static HostFormPanel panel(Integer raidIndex, boolean expanded) throws Exception
	{
		final HostFormPanel panel = newPanel();
		if (expanded)
		{
			invoke(panel, "setExpanded", true);
		}
		if (raidIndex != null)
		{
			final Object raidForm = field(panel, "raidForm");
			invoke(raidForm, "selectRaidTab", raidIndex);
		}
		return panel;
	}

	private static HostFormPanel live(String spots, String roles) throws Exception
	{
		final HostFormPanel panel = newPanel();
		invoke(panel, "setExpanded", true);
		final Map<String, String> fields = new LinkedHashMap<>();
		fields.put("raid", "TOB");
		fields.put("tier", "Standard");
		fields.put("world", "416");
		fields.put("spots", spots);
		fields.put("roles", roles);
		fields.put("partyHub", "catdog");
		setField(panel, "lastSubmittedFields", fields);
		panel.enterLivePost("mid");
		return panel;
	}

	private static HostFormPanel moreExpanded() throws Exception
	{
		final HostFormPanel panel = panel(0, true);
		final Object raidForm = field(panel, "raidForm");
		final Object fields = field(raidForm, "fields");
		final Object card = field(fields, "card");
		// Drive the real combos so the listener chain fires and the headline mirrors the choice, the way
		// a host selecting from the dropdown would.
		invoke(field(card, "tier"), "setSelectedItem", "Standard");
		invoke(field(card, "spots"), "setSelectedItem", "+2");
		invoke(field(fields, "more"), "setOpen", true);
		return panel;
	}

	/** Everything chosen: chips selected and Post enabled. */
	private static HostFormPanel ready() throws Exception
	{
		final HostFormPanel panel = panel(0, true);
		final Object card = card(panel);
		invoke(field(card, "tier"), "setSelectedItem", "Standard");
		invoke(field(card, "spots"), "setSelectedItem", "+2");
		invoke(field(card, "team"), "setSelectedItem", "5");
		final Object[] chips = field(card, "chips");
		invoke(chips[0], "setChosen", true);
		invoke(chips[2], "setChosen", true);
		return panel;
	}

	/** A low KC hides tiers, so the limit hint shows. */
	private static HostFormPanel tierLimited() throws Exception
	{
		final HostFormPanel panel = new HostFormPanel(new HostDependencies(new NoopActions(), () -> 416,
			() -> "", () -> "Zezima", raid -> 12, () ->
			{
			}, () ->
			{
			}, () -> true, world -> null));
		invoke(panel, "setExpanded", true);
		invoke(field(panel, "raidForm"), "selectRaidTab", 0);
		return panel;
	}

	/** A rejected world, so the error status renders. */
	private static HostFormPanel errorStatus() throws Exception
	{
		final HostFormPanel panel = panel(0, true);
		invoke(field(panel, "raidForm"), "setStatus",
			"W301 is a members world, pick a different world.", true);
		return panel;
	}

	/** CoX on a CM tier: scale applies, the scouted layout does not. */
	private static HostFormPanel coxCm() throws Exception
	{
		final HostFormPanel panel = panel(1, true);
		invoke(field(card(panel), "tier"), "setSelectedItem", "CM");
		invoke(field(card(panel), "spots"), "setSelectedItem", "+3");
		return panel;
	}

	/** Longest realistic values, to expose clipping and overflow. */
	private static HostFormPanel overflow() throws Exception
	{
		final HostFormPanel panel = newPanel();
		invoke(panel, "setExpanded", true);
		final Map<String, String> fields = new LinkedHashMap<>();
		fields.put("raid", "COX");
		fields.put("tier", "CM Efficiency");
		fields.put("world", "416");
		fields.put("size", "8");
		fields.put("spots", "+7");
		fields.put("scale", "100");
		fields.put("fc", "Longestnamehere");
		fields.put("partyHub", "correcthorsebatterystaple");
		fields.put("roles", "mdps, bgs spec, thrall carrier");
		fields.put("desc", "pogstack, max only, no learners, 200kc+ please");
		invoke(field(panel, "raidForm"), "populate", fields);
		invoke(field(fields(panel), "more"), "setOpen", true);
		return panel;
	}

	/** CoX with More open, so friends chat and the layout editor are visible. */
	private static HostFormPanel coxMore() throws Exception
	{
		final HostFormPanel panel = panel(1, true);
		invoke(field(card(panel), "tier"), "setSelectedItem", "Scaled");
		invoke(field(card(panel), "spots"), "setSelectedItem", "+2");
		invoke(field(fields(panel), "more"), "setOpen", true);
		return panel;
	}

	private static Object fields(HostFormPanel panel)
	{
		return field(field(panel, "raidForm"), "fields");
	}

	private static Object card(HostFormPanel panel)
	{
		return field(fields(panel), "card");
	}

	private static HostFormPanel editMode() throws Exception
	{
		final HostFormPanel panel = newPanel();
		invoke(panel, "setExpanded", true);
		final Object raidForm = field(panel, "raidForm");
		final Map<String, String> fields = new LinkedHashMap<>();
		fields.put("raid", "COX");
		fields.put("tier", "Scaled");
		fields.put("world", "416");
		fields.put("size", "4");
		fields.put("spots", "+3");
		fields.put("scale", "50");
		fields.put("fc", "Zezima");
		fields.put("partyHub", "catdog");
		fields.put("desc", "max only");
		invoke(raidForm, "populate", fields);
		invoke(raidForm, "beginEdit");
		invoke(raidForm, "setStatus", "Editing your live raid.", false);
		return panel;
	}

	private static HostFormPanel newPanel()
	{
		return new HostFormPanel(new HostDependencies(new NoopActions(), () -> 416, () -> "",
			() -> "Zezima", raid -> -1, () ->
			{
			}, () ->
			{
			}, () -> true, world -> null));
	}

	private static void shot(String name, HostFormPanel content) throws Exception
	{
		final JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
		root.setBackground(WdrTheme.BACKGROUND);
		root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		// Mirror WeDoRaidsPanel: the toggle is pinned chrome above the scrolling form, not part of it.
		final JComponent toggle = content.toggleButton();
		toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
		toggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, toggle.getPreferredSize().height));
		root.add(toggle);
		root.add(Box.createVerticalStrut(8));
		content.setAlignmentX(Component.LEFT_ALIGNMENT);
		root.add(content);
		final JFrame frame = new JFrame();
		frame.setUndecorated(true);
		frame.getContentPane().add(root);
		frame.pack();
		final int height = Math.max(60, root.getPreferredSize().height);
		frame.setSize(PANEL_WIDTH, height);
		root.setSize(PANEL_WIDTH, height);
		frame.validate();
		root.doLayout();
		layoutDeep(root);
		final BufferedImage image = new BufferedImage(PANEL_WIDTH, height, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = image.createGraphics();
		root.paint(graphics);
		graphics.dispose();
		ImageIO.write(image, "png", new File(OUT + "/" + name + ".png"));
		System.out.println(name + " " + height);
		frame.dispose();
	}

	private static void layoutDeep(Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container)
			{
				layoutDeep((Container) child);
			}
		}
	}

	private static void invoke(Object target, String methodName, Object... args)
	{
		try
		{
			for (Method method : target.getClass().getDeclaredMethods())
			{
				if (method.getName().equals(methodName) && method.getParameterCount() == args.length)
				{
					method.setAccessible(true);
					method.invoke(target, args);
					return;
				}
			}
			throw new AssertionError("Missing method " + methodName);
		}
		catch (ReflectiveOperationException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static <T> T field(Object target, String name)
	{
		try
		{
			final Field declared = target.getClass().getDeclaredField(name);
			declared.setAccessible(true);
			@SuppressWarnings("unchecked")
			final T value = (T) declared.get(target);
			return value;
		}
		catch (ReflectiveOperationException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static void setField(Object target, String name, Object value)
	{
		try
		{
			final Field declared = target.getClass().getDeclaredField(name);
			declared.setAccessible(true);
			declared.set(target, value);
		}
		catch (ReflectiveOperationException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static final class NoopActions implements HostFormPanel.HostActions
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
	}
}
