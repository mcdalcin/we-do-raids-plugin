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
import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * Coordinator for the host form. It owns the frozen wire contract ({@link #collectValidatedFields},
 * {@link #populate}) and raid applicability, and delegates presentation to the draft card
 * (section B) and the More disclosure (section C). Sentinel choices never reach the wire map, and
 * new-draft auto-behaviour is kept out of the populate path so an edit round-trips untouched.
 */
final class HostRaidFormFields extends JPanel
{
	private final HostDependencies dependencies;
	private final Supplier<RaidType> selectedRaid;
	private final BiConsumer<String, Boolean> status;
	private final Runnable onReadyChanged;
	private final HostDraftCard card;
	private final HostMoreOptions more;

	HostRaidFormFields(HostDependencies dependencies, Supplier<RaidType> selectedRaid,
		BiConsumer<String, Boolean> status, Runnable onReadyChanged)
	{
		this.dependencies = dependencies;
		this.selectedRaid = selectedRaid;
		this.status = status;
		this.onReadyChanged = onReadyChanged;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		setAlignmentX(Component.LEFT_ALIGNMENT);
		card = new HostDraftCard(selectedRaid, this::onCardChanged);
		more = new HostMoreOptions(this::onMoreChanged);
		add(card);
		add(Box.createVerticalStrut(4));
		add(more);
		applyRaid();
	}

	// --- lifecycle called by HostRaidForm ---

	void prepareExpanded()
	{
		if (more.getWorld().trim().isEmpty())
		{
			final int currentWorld = dependencies.currentWorld().getAsInt();
			if (currentWorld > 0)
			{
				more.setWorld(Integer.toString(currentWorld));
			}
		}
		dependencies.requestKc().run();
		refreshHeadlineAndTruth();
	}

	void refreshForRaid()
	{
		applyRaid();
		onReadyChanged.run();
	}

	void refreshTiers()
	{
		card.refreshTiers(dependencies.userKc().applyAsInt(selectedRaid.get()));
		onReadyChanged.run();
	}

	void refreshCoxLayout()
	{
		syncLayoutState();
	}

	/** Fills the friends chat with the local IGN for a fresh CoX draft only; never during populate. */
	void captureFriendsChat()
	{
		if (selectedRaid.get() == RaidType.COX && more.getFriendsChat().trim().isEmpty())
		{
			final String ign = dependencies.localIgn().get();
			if (ign != null && !ign.isEmpty())
			{
				more.setFriendsChat(ign);
				refreshHeadlineAndTruth();
			}
		}
	}

	boolean isPartyHubEmpty()
	{
		return more.isPartyHubEmpty();
	}

	void setGeneratedPartyHub(String value)
	{
		more.setPartyHub(value, true);
		refreshHeadlineAndTruth();
	}

	void setTierEnabled(boolean enabled)
	{
		card.setTierEnabled(enabled);
	}

	boolean isSubmittable()
	{
		return card.getTier() != null && card.getSpots() != null;
	}

	void resetPresentation()
	{
		card.reset();
		more.reset();
		refreshHeadlineAndTruth();
		onReadyChanged.run();
	}

	// --- wire contract ---

	Map<String, String> collectValidatedFields()
	{
		final Map<String, String> fields = new LinkedHashMap<>();
		fields.put("raid", selectedRaid.get().name());
		final String tier = card.getTier();
		final String spots = card.getSpots();
		if (tier == null || spots == null)
		{
			status.accept("Pick a tier and open spots.", true);
			return null;
		}
		fields.put("tier", tier);
		if (!collectWorld(fields) || !collectCox(fields))
		{
			return null;
		}
		putIfPresent(fields, "size", card.getTeamSize());
		fields.put("spots", spots);
		collectRoles(fields);
		if (layoutApplies())
		{
			String value = more.getLayoutText().trim();
			final String scout = dependencies.coxLayout().get();
			if (value.isEmpty() && scout != null)
			{
				value = scout.trim();
			}
			putIfPresent(fields, "layout", value);
		}
		putIfPresent(fields, "partyHub", more.getPartyHub().trim());
		putIfPresent(fields, "desc", more.getDescription().trim());
		return fields;
	}

	private boolean collectWorld(Map<String, String> fields)
	{
		final String value = more.getWorld().trim();
		if (value.isEmpty())
		{
			return true;
		}
		if (!value.matches("\\d{1,3}"))
		{
			more.openAndFocus(HostMoreOptions.Field.WORLD);
			status.accept("World must be a number.", true);
			return false;
		}
		final String blocked = dependencies.worldBlockReason().apply(Integer.parseInt(value));
		if (blocked != null)
		{
			more.openAndFocus(HostMoreOptions.Field.WORLD);
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
		final String value = card.getScale();
		if (!value.isEmpty())
		{
			if (!value.matches("\\d{1,3}") || Integer.parseInt(value) > 100)
			{
				status.accept("Scale must be 0-100.", true);
				return false;
			}
			fields.put("scale", value);
		}
		putIfPresent(fields, "fc", more.getFriendsChat().trim());
		return true;
	}

	private void collectRoles(Map<String, String> fields)
	{
		final List<String> values = new ArrayList<>();
		if (selectedRaid.get() == RaidType.TOB)
		{
			values.addAll(card.getSelectedRoles());
		}
		final String extra = more.getRoles().trim();
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
		final RaidType raid = selectedRaid.get();
		card.setRaid(raid);
		card.refreshTiers(dependencies.userKc().applyAsInt(raid));
		card.selectTier(values.get("tier"));
		more.setRaid(raid);
		more.setWorld(values.getOrDefault("world", ""));
		card.selectSize(values.getOrDefault("size", ""));
		card.selectSpots(values.getOrDefault("spots", ""));
		populateRoles(values.get("roles"));
		card.setScale(values.getOrDefault("scale", ""));
		more.setFriendsChat(values.getOrDefault("fc", ""));
		more.setLayout(values.getOrDefault("layout", ""));
		more.setPartyHub(values.getOrDefault("partyHub", ""), false);
		more.setDescription(values.getOrDefault("desc", ""));
		syncLayoutState();
		refreshHeadlineAndTruth();
		onReadyChanged.run();
	}

	/** Known roles become chips; anything else survives verbatim in the More "Other roles" field. */
	private void populateRoles(String value)
	{
		final List<String> known = new ArrayList<>();
		final List<String> extras = new ArrayList<>();
		if (value != null)
		{
			for (String part : value.split(","))
			{
				final String role = part.trim().toLowerCase();
				switch (role)
				{
					case "mdps":
					case "rdps":
					case "nfrz":
					case "sfrz":
						known.add(role);
						break;
					default:
						if (!role.isEmpty())
						{
							extras.add(part.trim());
						}
				}
			}
		}
		card.setSelectedRoles(known);
		more.setRoles(String.join(", ", extras));
	}

	// --- applicability + presentation glue ---

	private void applyRaid()
	{
		final RaidType raid = selectedRaid.get();
		card.setRaid(raid);
		card.refreshTiers(dependencies.userKc().applyAsInt(raid));
		more.setRaid(raid);
		syncLayoutState();
		refreshHeadlineAndTruth();
	}

	private void onCardChanged()
	{
		syncLayoutState();
		refreshHeadlineAndTruth();
		onReadyChanged.run();
	}

	private void onMoreChanged()
	{
		refreshHeadlineAndTruth();
	}

	private void refreshHeadlineAndTruth()
	{
		card.refreshHeadline(more.getWorld().trim());
		final String hub = more.getPartyHub().trim();
		final String friendsChat = selectedRaid.get() == RaidType.COX ? more.getFriendsChat().trim() : "";
		card.refreshTruth(hub, friendsChat);
	}

	private boolean layoutApplies()
	{
		final String tier = card.getTier();
		return selectedRaid.get() == RaidType.COX && tier != null && !tier.contains("CM");
	}

	/** Pushes the scout state to the card, and the editor's own explanation to More. */
	private void syncLayoutState()
	{
		final boolean applies = layoutApplies();
		card.setLayoutState(applies, scoutText());
		more.setLayoutEditorVisible(applies);
		more.setLayoutScout(layoutEditorHint());
	}

	private String scoutText()
	{
		final String scout = dependencies.coxLayout().get();
		return scout != null && !scout.isEmpty() ? "Scout: " + scout : "Scout: not detected yet";
	}

	/**
	 * The editor's hint answers what the field is for, never what the scout currently says. The card
	 * already states the scout, and repeating it here put the same sentence on screen twice whenever
	 * More was open.
	 */
	private String layoutEditorHint()
	{
		final String scout = dependencies.coxLayout().get();
		return scout != null && !scout.isEmpty()
			? "Scouted for you. Edit to override."
			: "Fills in once you scout the raid.";
	}
}
