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
import com.wedoraids.ui.WdrTheme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.runelite.client.ui.FontManager;

final class HostRaidFormFields extends JPanel
{
	private final HostDependencies dependencies;
	private final Supplier<RaidType> selectedRaid;
	private final BiConsumer<String, Boolean> status;
	private final JComboBox<String> tier = new JComboBox<>(RaidType.TOB.getTiers());
	private final JTextField world = new JTextField();
	private final JComboBox<String> spots = new JComboBox<>(new String[]{"", "+1", "+2", "+3", "+4"});
	private final JComboBox<String> team = new JComboBox<>();
	private final JCheckBox mdps = new JCheckBox("mdps");
	private final JCheckBox rdps = new JCheckBox("rdps");
	private final JCheckBox nfrz = new JCheckBox("nfrz");
	private final JCheckBox sfrz = new JCheckBox("sfrz");
	private final JTextField roles = new JTextField();
	private final JTextField scale = new JTextField();
	private final JTextField fc = new JTextField();
	private final JTextField layout = new JTextField();
	private final JTextField partyHub = new JTextField();
	private final JTextField description = new JTextField();
	private JPanel roleRow;
	private JPanel scaleFcRow;
	private JPanel layoutRow;
	private final JLabel tierHint = new JLabel();

	HostRaidFormFields(HostDependencies dependencies, Supplier<RaidType> selectedRaid,
		BiConsumer<String, Boolean> status)
	{
		this.dependencies = dependencies;
		this.selectedRaid = selectedRaid;
		this.status = status;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		setAlignmentX(Component.LEFT_ALIGNMENT);
		styleFields();
		buildFields();
		tier.addActionListener(e -> updateLayoutVisibility());
		refreshTeamOptions();
		updateRoleVisibility();
		updateLayoutVisibility();
		updateScaleVisibility();
	}

	void prepareExpanded()
	{
		if (world.getText().trim().isEmpty())
		{
			final int currentWorld = dependencies.currentWorld().getAsInt();
			if (currentWorld > 0)
			{
				world.setText(Integer.toString(currentWorld));
			}
		}
		dependencies.requestKc().run();
		updateLayoutVisibility();
		updateScaleVisibility();
	}

	void refreshForRaid()
	{
		refreshTiers();
		refreshTeamOptions();
		updateRoleVisibility();
		updateLayoutVisibility();
		updateScaleVisibility();
	}

	void refreshTiers()
	{
		final RaidType raid = selectedRaid.get();
		final int killCount = dependencies.userKc().applyAsInt(raid);
		final Object previous = tier.getSelectedItem();
		tier.removeAllItems();
		for (String option : raid.getTiers())
		{
			if (killCount < 0 || raid.minKc(option) <= killCount)
			{
				tier.addItem(option);
			}
		}
		if (tier.getItemCount() == 0 && raid.getTiers().length > 0)
		{
			tier.addItem(raid.getTiers()[0]);
		}
		restoreTierSelection(previous);
		refreshTierHint(raid, killCount);
	}

	/** Restores the previous tier only if it is still offered, so an ineligible tier never stays selected. */
	private void restoreTierSelection(Object previous)
	{
		if (previous == null)
		{
			return;
		}
		for (int i = 0; i < tier.getItemCount(); i++)
		{
			if (previous.equals(tier.getItemAt(i)))
			{
				tier.setSelectedIndex(i);
				return;
			}
		}
		if (tier.getItemCount() > 0)
		{
			tier.setSelectedIndex(0);
		}
	}

	/** Explains missing tier options, shown only when the known KC actually hides some. */
	private void refreshTierHint(RaidType raid, int killCount)
	{
		final boolean limited = killCount >= 0 && tier.getItemCount() < raid.getTiers().length;
		tierHint.setVisible(limited);
		if (limited)
		{
			tierHint.setText("Tiers limited by your " + raid.getDisplayName() + " KC: " + killCount);
		}
		revalidate();
		repaint();
	}

	void refreshCoxLayout()
	{
		if (!layoutApplies())
		{
			return;
		}
		final String coxLayout = dependencies.coxLayout().get();
		if (coxLayout != null && !coxLayout.isEmpty())
		{
			layout.setText(coxLayout);
			revalidate();
			repaint();
		}
	}

	Map<String, String> collectValidatedFields()
	{
		final Map<String, String> fields = new LinkedHashMap<>();
		fields.put("raid", selectedRaid.get().name());
		if (tier.getSelectedItem() != null)
		{
			fields.put("tier", (String) tier.getSelectedItem());
		}
		if (!collectWorld(fields) || !collectCox(fields))
		{
			return null;
		}
		putIfPresent(fields, "size", (String) team.getSelectedItem());
		putIfPresent(fields, "spots", (String) spots.getSelectedItem());
		collectRoles(fields);
		if (layoutApplies())
		{
			String value = layout.getText().trim();
			if (value.isEmpty() && dependencies.coxLayout().get() != null)
			{
				value = dependencies.coxLayout().get().trim();
			}
			putIfPresent(fields, "layout", value);
		}
		putIfPresent(fields, "partyHub", partyHub.getText().trim());
		putIfPresent(fields, "desc", description.getText().trim());
		return fields;
	}

	private boolean collectWorld(Map<String, String> fields)
	{
		final String value = world.getText().trim();
		if (value.isEmpty())
		{
			return true;
		}
		if (!value.matches("\\d{1,3}"))
		{
			status.accept("World must be a number.", true);
			return false;
		}
		final String blocked = dependencies.worldBlockReason().apply(Integer.parseInt(value));
		if (blocked != null)
		{
			status.accept("W" + value + " is " + blocked + ", pick a different world.", true);
			return false;
		}
		fields.put("world", value);
		return true;
	}

	private boolean collectCox(Map<String, String> fields)
	{
		if (selectedRaid.get() != RaidType.COX)
		{
			return true;
		}
		final String value = scale.getText().trim();
		if (!value.isEmpty())
		{
			if (!value.matches("\\d{1,3}") || Integer.parseInt(value) > 100)
			{
				status.accept("Scale must be 0-100.", true);
				return false;
			}
			fields.put("scale", value);
		}
		putIfPresent(fields, "fc", fc.getText().trim());
		return true;
	}

	private void collectRoles(Map<String, String> fields)
	{
		final List<String> values = new ArrayList<>();
		if (selectedRaid.get() == RaidType.TOB)
		{
			final JCheckBox[] roleChecks = {mdps, rdps, nfrz, sfrz};
			final String[] names = {"mdps", "rdps", "nfrz", "sfrz"};
			for (int index = 0; index < roleChecks.length; index++)
			{
				if (roleChecks[index].isSelected())
				{
					values.add(names[index]);
				}
			}
		}
		final String extra = roles.getText().trim();
		if (!extra.isEmpty())
		{
			values.add(extra);
		}
		if (!values.isEmpty())
		{
			fields.put("roles", String.join(", ", values));
		}
	}

	private static void putIfPresent(Map<String, String> fields, String key, String value)
	{
		if (value != null && !value.isEmpty())
		{
			fields.put(key, value);
		}
	}

	void populate(Map<String, String> values)
	{
		refreshTiers();
		if (values.get("tier") != null)
		{
			tier.setSelectedItem(values.get("tier"));
		}
		world.setText(values.getOrDefault("world", ""));
		refreshTeamOptions();
		team.setSelectedItem(values.getOrDefault("size", ""));
		spots.setSelectedItem(values.getOrDefault("spots", ""));
		populateRoles(values.get("roles"));
		scale.setText(values.getOrDefault("scale", ""));
		fc.setText(values.getOrDefault("fc", ""));
		layout.setText(values.getOrDefault("layout", ""));
		partyHub.setText(values.getOrDefault("partyHub", ""));
		description.setText(values.getOrDefault("desc", ""));
		updateRoleVisibility();
		updateScaleVisibility();
		updateLayoutVisibility();
	}

	void setTierEnabled(boolean enabled)
	{
		tier.setEnabled(enabled);
	}

	boolean isPartyHubEmpty()
	{
		return partyHub.getText().trim().isEmpty();
	}

	void setPartyHub(String value)
	{
		partyHub.setText(value);
	}

	private void styleFields()
	{
		WdrTheme.styleCombo(tier);
		WdrTheme.styleCombo(spots);
		WdrTheme.styleCombo(team);
		for (JTextField field : new JTextField[]{world, roles, scale, fc, layout, partyHub, description})
		{
			WdrTheme.styleField(field);
		}
	}

	private void buildFields()
	{
		add(pair(labeled("Tier", tier), labeled("World", world)));
		tierHint.setFont(FontManager.getRunescapeSmallFont());
		tierHint.setForeground(WdrTheme.TEXT_DIM);
		tierHint.setAlignmentX(Component.LEFT_ALIGNMENT);
		tierHint.setVisible(false);
		add(tierHint);
		add(pair(labeled("Team size", team), labeled("Open spots", spots)));
		scaleFcRow = pair(labeled("Scale (0-100)", scale), labeled("Friends chat", fc));
		add(scaleFcRow);
		layoutRow = labeled("Layout (auto)", layout);
		add(layoutRow);
		JPanel checkboxes = new JPanel(new GridLayout(2, 2));
		checkboxes.setOpaque(false);
		for (JCheckBox checkbox : new JCheckBox[]{mdps, rdps, nfrz, sfrz})
		{
			checkbox.setOpaque(false);
			checkbox.setForeground(WdrTheme.TEXT);
			checkbox.setFont(FontManager.getRunescapeSmallFont());
			checkboxes.add(checkbox);
		}
		roleRow = labeled("Roles needed", checkboxes);
		add(roleRow);
		add(labeled("Other roles", roles));
		partyHub.setToolTipText("Optional passphrase joiners use with the RuneLite Party plugin");
		add(labeled("Party hub", partyHub));
		description.setToolTipText("e.g. pogstack, max only");
		add(labeled("Description", description));
	}

	private void refreshTeamOptions()
	{
		final RaidType raid = selectedRaid.get();
		final Object previous = team.getSelectedItem();
		team.removeAllItems();
		team.addItem("");
		final int minimum = raid == RaidType.TOB ? 2 : 1;
		final int maximum = raid == RaidType.TOB ? 5 : 8;
		for (int size = minimum; size <= maximum; size++)
		{
			team.addItem(String.valueOf(size));
		}
		if (previous != null)
		{
			team.setSelectedItem(previous);
		}
	}

	private void updateRoleVisibility()
	{
		roleRow.setVisible(selectedRaid.get() == RaidType.TOB);
		revalidate();
		repaint();
	}

	private void updateScaleVisibility()
	{
		final boolean cox = selectedRaid.get() == RaidType.COX;
		scaleFcRow.setVisible(cox);
		if (cox && fc.getText().trim().isEmpty())
		{
			final String ign = dependencies.localIgn().get();
			if (ign != null && !ign.isEmpty())
			{
				fc.setText(ign);
			}
		}
		revalidate();
		repaint();
	}

	private boolean layoutApplies()
	{
		final Object selectedTier = tier.getSelectedItem();
		return selectedRaid.get() == RaidType.COX && selectedTier != null
			&& !selectedTier.toString().contains("CM");
	}

	private void updateLayoutVisibility()
	{
		final boolean show = layoutApplies();
		layoutRow.setVisible(show);
		if (show && layout.getText().trim().isEmpty())
		{
			final String coxLayout = dependencies.coxLayout().get();
			if (coxLayout != null && !coxLayout.isEmpty())
			{
				layout.setText(coxLayout);
			}
		}
		revalidate();
		repaint();
	}

	private void populateRoles(String value)
	{
		mdps.setSelected(false);
		rdps.setSelected(false);
		nfrz.setSelected(false);
		sfrz.setSelected(false);
		final List<String> extras = new ArrayList<>();
		if (value != null)
		{
			for (String part : value.split(","))
			{
				final String role = part.trim().toLowerCase();
				switch (role)
				{
					case "mdps": mdps.setSelected(true); break;
					case "rdps": rdps.setSelected(true); break;
					case "nfrz": nfrz.setSelected(true); break;
					case "sfrz": sfrz.setSelected(true); break;
					default: if (!role.isEmpty()) extras.add(part.trim());
				}
			}
		}
		roles.setText(String.join(", ", extras));
	}

	private static JPanel pair(JPanel left, JPanel right)
	{
		JPanel pair = new JPanel(new GridLayout(1, 2, 6, 0));
		pair.setOpaque(false);
		pair.add(left);
		pair.add(right);
		pair.setAlignmentX(Component.LEFT_ALIGNMENT);
		pair.setMaximumSize(new Dimension(Integer.MAX_VALUE, pair.getPreferredSize().height));
		return pair;
	}

	private static JPanel labeled(String label, Component field)
	{
		JLabel text = new JLabel(label);
		text.setForeground(WdrTheme.TEXT_DIM);
		text.setFont(FontManager.getRunescapeSmallFont());
		JPanel row = new JPanel(new BorderLayout(0, 1));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		row.add(text, BorderLayout.NORTH);
		row.add(field, BorderLayout.CENTER);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}
}
