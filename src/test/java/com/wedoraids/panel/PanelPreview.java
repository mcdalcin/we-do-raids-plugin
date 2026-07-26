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

import com.wedoraids.host.HostPreview;
import java.util.Map;

/**
 * Reaches the host form through {@link WeDoRaidsPanel} for the design gallery, without reflection.
 *
 * <p>Two packages are involved because Java's access rules split the work. The panel's own state,
 * feed contents, verification, ban and bridge status, is already public API, so the gallery drives
 * that directly and needs nothing from here. Reaching the host form is different: the field is
 * private, so a driver in this package asks for it, then hands it to {@link HostPreview}, which lives
 * beside the host classes and can call their package-private methods. Neither half needs to guess at
 * a field name, so a rename breaks the build instead of the render.
 */
public final class PanelPreview
{
	private PanelPreview()
	{
	}

	/** Opens the host form on a raid tab. */
	public static void expandHostForm(WeDoRaidsPanel panel, int raidTab)
	{
		HostPreview.expand(panel.hostForm(), raidTab);
	}

	/** Opens the host form on a raid tab with the More options disclosure expanded. */
	public static void expandHostFormWithMoreOptions(WeDoRaidsPanel panel, int raidTab)
	{
		HostPreview.expandWithMoreOptions(panel.hostForm(), raidTab);
	}

	/** Opens the host form on a raid tab and fills the draft. */
	public static void expandHostFormFilled(WeDoRaidsPanel panel, int raidTab, Map<String, String> values)
	{
		HostPreview.expandFilled(panel.hostForm(), raidTab, values);
	}

	/** A filled draft with the More options disclosure expanded. */
	public static void expandHostFormFilledWithMoreOptions(WeDoRaidsPanel panel, int raidTab,
		Map<String, String> values)
	{
		HostPreview.expandFilledWithMoreOptions(panel.hostForm(), raidTab, values);
	}

	/** Clicks Post to Discord on the current draft; the panel's actions decide the reply. */
	public static void submitHostForm(WeDoRaidsPanel panel)
	{
		HostPreview.submitDraft(panel.hostForm());
	}

	/** A live post reopened for editing, as clicking "Edit details" would. */
	public static void editLivePost(WeDoRaidsPanel panel, String spots, String roles)
	{
		HostPreview.editLivePost(panel.hostForm(), spots, roles);
	}

	/** Clicks "Close raid" on a live post; the panel's actions decide the reply. */
	public static void closeLivePost(WeDoRaidsPanel panel, String spots, String roles)
	{
		HostPreview.closeLivePost(panel.hostForm(), spots, roles);
	}

	/** Renders a card whose party hub is already joined. */
	public static void markHubJoined(WeDoRaidsPanel panel, String hub)
	{
		panel.markHubJoined(hub);
	}

	/** Renders a live post, optionally with an undo on offer. */
	public static void livePost(WeDoRaidsPanel panel, String spots, String roles, boolean undo)
	{
		HostPreview.livePost(panel.hostForm(), spots, roles, undo);
	}

	/** Shows the idle prompt without waiting out the timer. */
	public static void promptInactivity(WeDoRaidsPanel panel)
	{
		HostPreview.promptInactivity(panel.hostForm());
	}
}
