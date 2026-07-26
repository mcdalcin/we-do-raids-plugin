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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Puts a {@link HostFormPanel} into a named state for the design gallery, without reflection.
 *
 * <p>This class earns its existence by living in the same package as the panel it drives. Everything
 * it calls is package-private production API, so the gallery reaches a state the way a click would
 * rather than by prising open private fields. The alternative was a reflection helper, which silently
 * survives renames and refactors: it compiles, then fails at runtime, or worse, renders a state the
 * real panel cannot produce. That already happened once here, when a live post was rendered above a
 * collapsed toggle because the harness set visibility directly instead of expanding the form.
 *
 * <p>Kept public so the panel package's own preview driver can call it; the states it composes are
 * host-internal, so the knowledge of how to reach them belongs here.
 */
public final class HostPreview
{
	private HostPreview()
	{
	}

	/** Opens the form on a raid tab, exactly as clicking the toggle and then that tab would. */
	public static void expand(HostFormPanel form, int raidTab)
	{
		form.setExpanded(true);
		form.selectRaidTab(raidTab);
	}

	/** Opens the form on a raid tab with the More options disclosure expanded, as clicking both would. */
	public static void expandWithMoreOptions(HostFormPanel form, int raidTab)
	{
		expand(form, raidTab);
		form.openMoreOptions();
	}

	/** Opens the form on a raid tab and fills the draft, as picking those values by hand would. */
	public static void expandFilled(HostFormPanel form, int raidTab, Map<String, String> values)
	{
		expand(form, raidTab);
		form.populateDraft(values);
	}

	/** A filled draft with the More options disclosure expanded. */
	public static void expandFilledWithMoreOptions(HostFormPanel form, int raidTab, Map<String, String> values)
	{
		expandFilled(form, raidTab, values);
		form.openMoreOptions();
	}

	/** Clicks Post to Discord on the current draft; the panel's own actions decide the reply. */
	public static void submitDraft(HostFormPanel form)
	{
		form.submitDraft();
	}

	/** A live post reopened for editing, exactly as clicking "Edit details" would. */
	public static void editLivePost(HostFormPanel form, String spots, String roles)
	{
		livePost(form, spots, roles, false);
		form.beginLiveEdit();
	}

	/** Clicks "Close raid" on a live post; the panel's actions decide the reply. */
	public static void closeLivePost(HostFormPanel form, String spots, String roles)
	{
		livePost(form, spots, roles, false);
		form.requestCloseLivePost();
	}

	/**
	 * Renders a live post for a ToB raid.
	 *
	 * <p>The form is expanded first because that is the only route a host has to a live post: they
	 * open the form, fill it, and submit. Skipping that step renders a collapsed toggle sitting above
	 * a visible live post, which the panel cannot actually produce, and which quietly invalidates any
	 * review of those states.
	 *
	 * @param roles comma-separated role names, or {@code null} for a spots-only post
	 * @param undo  whether a previous value is on offer, as it would be after a real update
	 */
	public static void livePost(HostFormPanel form, String spots, String roles, boolean undo)
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
		form.setExpanded(true);
		form.enterLivePost(fields, "msg-1");
		if (undo)
		{
			final Map<String, String> previous = new LinkedHashMap<>(fields);
			previous.put("spots", "+2");
			form.offerUndo(previous);
		}
	}

	/** Shows the idle prompt now instead of waiting out the inactivity timer. */
	public static void promptInactivity(HostFormPanel form)
	{
		form.promptInactivity();
	}
}
