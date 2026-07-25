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

import com.wedoraids.ui.WdrButton;
import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.FontManager;

public class HostFormPanel extends JPanel
{
	public interface HostActions
	{
		void submit(Map<String, String> fields, Consumer<String> status);

		void update(Map<String, String> fields, Consumer<String> status);

		void close(Map<String, String> fields, Consumer<String> status);
	}

	private final HostActions actions;
	private final WdrButton toggle = new WdrButton("Host raid", WdrButton.Variant.PRIMARY);
	private final HostRaidForm raidForm;
	private final HostLivePostPanel livePostPanel;
	private final HostInactivityGuard inactivityGuard;
	private final JLabel liveStatus;
	private final HostLiveState liveState = new HostLiveState();
	private boolean expanded;
	private boolean editingLive;
	private Map<String, String> lastSubmittedFields;
	/** Test-visible alias of {@link HostLivePostPanel}'s displayed fields, synchronized by callback. */
	private Map<String, String> displayedLiveFields;

	public HostFormPanel(HostDependencies dependencies)
	{
		this.actions = dependencies.actions();
		setLayout(new BorderLayout(0, 4));
		setOpaque(false);

		// Deliberately not added to this panel. The toggle is pinned as fixed chrome by the owning
		// panel so a long feed cannot scroll hosting out of reach; only the form below it scrolls.
		toggle.addActionListener(e -> setExpanded(!expanded));
		// Bold and taller than the default control. At the shared small font and padding it measured the
		// same 20px as the filter combo directly beneath it, so the panel's one action read as an input.
		toggle.setFont(FontManager.getRunescapeBoldFont());
		toggle.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));

		raidForm = new HostRaidForm(dependencies, this::doSubmit, this::cancelEdit);
		inactivityGuard = new HostInactivityGuard(liveState,
			() -> displayedLiveFields != null, this::rebuildLiveControls, this::closeForInactivity,
			dependencies.onIdleWarn());
		livePostPanel = new HostLivePostPanel(actions, liveState, inactivityGuard,
			this::beginEdit, this::setDisplayedLiveFields);
		liveStatus = livePostPanel.statusLabel();

		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		center.setOpaque(false);
		center.add(raidForm);
		center.add(livePostPanel);
		add(center, BorderLayout.CENTER);
	}

	public void refreshTiersPublic()
	{
		raidForm.refreshTiers();
	}

	public void refreshCoxLayout()
	{
		raidForm.refreshCoxLayout();
	}

	public void enterLivePost(String messageId)
	{
		if (liveState.isStopped() || lastSubmittedFields == null)
		{
			return;
		}
		livePostPanel.enterLivePost(lastSubmittedFields, messageId);
		raidForm.setVisible(false);
		livePostPanel.setVisible(true);
		revalidate();
		repaint();
	}

	/**
	 * Enters the live post directly from a set of submitted values.
	 *
	 * <p>The public entry point replays whatever this panel last submitted, which a caller cannot
	 * supply from outside. The design gallery needs to render a live post without walking the whole
	 * submit path first, and this lets it do that while {@code lastSubmittedFields} stays private.
	 */
	void enterLivePost(Map<String, String> submitted, String messageId)
	{
		lastSubmittedFields = new LinkedHashMap<>(submitted);
		enterLivePost(messageId);
	}

	/**
	 * The three operations below exist so the design gallery can reach a state through this panel's
	 * own API rather than through its children. Delegating keeps {@code raidForm}, {@code livePostPanel}
	 * and {@code inactivityGuard} private: the gallery asks the panel to do something, it does not get
	 * handed the panel's internals.
	 */
	void selectRaidTab(int index)
	{
		raidForm.selectRaidTab(index);
	}

	void offerUndo(Map<String, String> previous)
	{
		livePostPanel.offerUndo(previous);
	}

	void promptInactivity()
	{
		inactivityGuard.showPrompt();
	}

	public void exitLivePost()
	{
		livePostPanel.exitLivePost();
		resetHostFormControls();
		raidForm.setVisible(expanded);
		revalidate();
		repaint();
	}

	public void stopTimers()
	{
		liveState.stop();
		inactivityGuard.stop();
	}

	static String generatePartyHub()
	{
		return HostRaidForm.generatePartyHub();
	}

	/**
	 * The disclosure control, handed to the owning panel so it can be pinned outside the scroll area.
	 */
	public WdrButton toggleButton()
	{
		return toggle;
	}

	/**
	 * Package-private rather than private so the design gallery can open and close the form the way a
	 * click does, instead of reaching through reflection to do it.
	 */
	void setExpanded(boolean expanded)
	{
		this.expanded = expanded;
		toggle.setText(expanded ? "Hide host form" : "Host raid");
		// Collapsed, this is the only button-shaped action in the feed, since joining happens through
		// the card links; expanded, the form's own submit takes that rank and closing is the soft move.
		toggle.setVariant(expanded ? WdrButton.Variant.GHOST : WdrButton.Variant.PRIMARY);
		raidForm.setVisible(expanded && (displayedLiveFields == null || editingLive));
		livePostPanel.setVisible(expanded && displayedLiveFields != null && !editingLive);
		if (expanded)
		{
			raidForm.prepareExpanded();
		}
		revalidate();
		repaint();
	}

	private void doSubmit()
	{
		if (!liveState.canStartOperation())
		{
			return;
		}
		final Map<String, String> fields = raidForm.collectValidatedFields();
		if (fields == null)
		{
			return;
		}
		if (editingLive && displayedLiveFields != null)
		{
			submitLiveEdit(fields);
			return;
		}

		raidForm.setStatus("Posting…", false);
		final Map<String, String> submitted = new LinkedHashMap<>(fields);
		lastSubmittedFields = new LinkedHashMap<>(submitted);
		final long operation = liveState.beginOperation();
		actions.submit(submitted, message ->
		{
			if (!liveState.isCurrentOperation(operation))
			{
				return;
			}
			liveState.completeOperation(operation);
			raidForm.setStatus(message, !message.startsWith("Posted"));
		});
	}

	private void submitLiveEdit(Map<String, String> fields)
	{
		final Map<String, String> update = new LinkedHashMap<>(fields);
		update.put("messageId", displayedLiveFields.get("messageId"));
		final long operation = liveState.beginOperation();
		raidForm.setStatus("Saving…", false);
		actions.update(update, message ->
		{
			if (!liveState.isCurrentOperation(operation))
			{
				return;
			}
			liveState.completeOperation(operation);
			raidForm.setStatus(message, !message.startsWith("Updated"));
			if (message.startsWith("Updated"))
			{
				finishEdit(fields);
			}
		});
	}

	private void beginEdit()
	{
		if (!liveState.canStartOperation() || displayedLiveFields == null)
		{
			return;
		}
		editingLive = true;
		inactivityGuard.pause();
		raidForm.populate(displayedLiveFields);
		raidForm.beginEdit();
		livePostPanel.setVisible(false);
		raidForm.setVisible(true);
		raidForm.setStatus("Editing your live raid.", false);
		revalidate();
		repaint();
	}

	private void cancelEdit()
	{
		if (!liveState.canStartOperation() || !editingLive)
		{
			return;
		}
		resetHostFormControls();
		raidForm.setStatus(" ", false);
		raidForm.setVisible(false);
		livePostPanel.setVisible(true);
		inactivityGuard.reset();
		revalidate();
		repaint();
	}

	private void finishEdit(Map<String, String> fields)
	{
		resetHostFormControls();
		final String messageId = displayedLiveFields != null ? displayedLiveFields.get("messageId") : null;
		lastSubmittedFields = new LinkedHashMap<>(fields);
		raidForm.setVisible(false);
		enterLivePost(messageId);
	}

	private void resetHostFormControls()
	{
		editingLive = false;
		raidForm.resetEdit();
	}

	private void fillRole(String role)
	{
		livePostPanel.fillRole(role);
	}

	private void decrementSpot()
	{
		livePostPanel.decrementSpot();
	}

	private void doClose()
	{
		livePostPanel.close();
	}

	private void closeForInactivity()
	{
		livePostPanel.closeForInactivity();
	}

	private void rebuildLiveControls()
	{
		if (displayedLiveFields != null)
		{
			livePostPanel.rebuildControls();
		}
	}

	private void setDisplayedLiveFields(Map<String, String> fields)
	{
		displayedLiveFields = fields;
	}
}
