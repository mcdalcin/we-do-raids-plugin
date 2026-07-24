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
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.BeforeClass;
import org.junit.Test;

public class RecruitListPanelTest
{
	@BeforeClass
	public static void enableHeadlessSwing()
	{
		System.setProperty("java.awt.headless", "true");
	}

	@Test
	public void missingKeyShowsOnboardingWithKeyEntry() throws Exception
	{
		onEdt(() ->
		{
			final RecruitListPanel panel = newPanel(config(""), new LinkedHashMap<>());
			panel.rebuild();
			assertNotNull(findLabel(panel, "Verification required"));
			assertNotNull(findComponent(panel, JPasswordField.class));
		});
	}

	@Test
	public void unacceptedKeyIsOnlyClaimedWhenBridgeIsOnline() throws Exception
	{
		onEdt(() ->
		{
			final RecruitListPanel panel = newPanel(config("some-key"), new LinkedHashMap<>());
			panel.setBridgeStatus(BridgeStatus.CONNECTING);
			assertNotNull(findLabel(panel, "Checking verification"));
			assertNull(findLabel(panel, "Key not accepted"));

			panel.setBridgeStatus(BridgeStatus.OFFLINE);
			assertNotNull(findLabel(panel, "Bridge unavailable"));
			assertNull(findLabel(panel, "Key not accepted"));

			panel.setBridgeStatus(BridgeStatus.ONLINE);
			assertNotNull(findLabel(panel, "Key not accepted"));
			assertNotNull(findComponent(panel, JPasswordField.class));
		});
	}

	@Test
	public void savingKeyWritesTheSecretConfigValue() throws Exception
	{
		onEdt(() ->
		{
			final Map<String, String> saved = new LinkedHashMap<>();
			final RecruitListPanel panel = newPanel(config(""), saved);
			panel.rebuild();
			final JPasswordField key = findComponent(panel, JPasswordField.class);
			assertNotNull(key);
			key.setText("  abc123  ");
			final AbstractButton save = findButton(panel, "Save key");
			assertNotNull(save);
			save.doClick();
			assertEquals("abc123", saved.get("remoteFeedKey"));
			assertEquals(0, key.getPassword().length);
		});
	}

	@Test
	public void emptyFeedReportsCallsHiddenByConfigFilters() throws Exception
	{
		onEdt(() ->
		{
			final RecruitListPanel panel = newPanel(config("some-key"), new LinkedHashMap<>());
			panel.setVerified(true);
			panel.setEntries(Collections.emptyList(), 3);
			assertNotNull(findLabel(panel, "Calls hidden by settings"));
			assertNotNull(findLabelContaining(panel, "3 calls are"));

			panel.setEntries(Collections.emptyList(), 0);
			assertNotNull(findLabel(panel, "Waiting for calls"));
		});
	}

	private static RecruitListPanel newPanel(WeDoRaidsConfig config, Map<String, String> saved)
	{
		final RecruitFilterBar filterBar = new RecruitFilterBar(config, (key, value) ->
		{
		}, () ->
		{
		});
		final RecruitListPanel panel = new RecruitListPanel(config, filterBar, saved::put,
			world ->
			{
			}, hub ->
			{
			}, count ->
			{
			});
		panel.setLoggedIn(true);
		return panel;
	}

	private static WeDoRaidsConfig config(String key)
	{
		return new WeDoRaidsConfig()
		{
			@Override
			public String remoteFeedKey()
			{
				return key;
			}
		};
	}

	private static JLabel findLabel(Container root, String text)
	{
		for (JLabel label : labels(root))
		{
			if (text.equals(label.getText()))
			{
				return label;
			}
		}
		return null;
	}

	private static JLabel findLabelContaining(Container root, String fragment)
	{
		for (JLabel label : labels(root))
		{
			if (label.getText() != null && label.getText().contains(fragment))
			{
				return label;
			}
		}
		return null;
	}

	private static List<JLabel> labels(Container root)
	{
		final List<JLabel> labels = new ArrayList<>();
		collect(root, JLabel.class, labels);
		return labels;
	}

	private static AbstractButton findButton(Container root, String text)
	{
		final List<AbstractButton> buttons = new ArrayList<>();
		collect(root, AbstractButton.class, buttons);
		for (AbstractButton button : buttons)
		{
			if (text.equals(button.getText()))
			{
				return button;
			}
		}
		return null;
	}

	private static <T extends Component> T findComponent(Container root, Class<T> type)
	{
		final List<T> matches = new ArrayList<>();
		collect(root, type, matches);
		return matches.isEmpty() ? null : matches.get(0);
	}

	private static <T extends Component> void collect(Container root, Class<T> type, List<T> out)
	{
		for (Component child : root.getComponents())
		{
			if (type.isInstance(child))
			{
				out.add(type.cast(child));
			}
			if (child instanceof Container)
			{
				collect((Container) child, type, out);
			}
		}
	}

	private static void onEdt(Runnable action) throws Exception
	{
		SwingUtilities.invokeAndWait(action);
	}
}
