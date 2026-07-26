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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.BeforeClass;
import org.junit.Test;

public class HostFormPanelTest
{
	@BeforeClass
	public static void enableHeadlessSwing()
	{
		System.setProperty("java.awt.headless", "true");
	}

	@Test
	public void repeatedSubmitWhileCallbackPendingInvokesSubmitOnce() throws Exception
	{
		final HostFormPanel[] panel = new HostFormPanel[1];
		final FakeActions actions = new FakeActions();
		onEdt(() ->
		{
			panel[0] = newPanel(actions);
			seedValidDraft(panel[0]);
		});

		onEdt(() ->
		{
			invoke(panel[0], "doSubmit");
			invoke(panel[0], "doSubmit");
		});
		assertEquals(1, actions.submitCalls.get());
	}

	@Test
	public void pendingSecondSpotClickDoesNotTriggerPrematureClose() throws Exception
	{
		final HostFormPanel[] panel = new HostFormPanel[1];
		final FakeActions actions = new FakeActions();
		onEdt(() ->
		{
			panel[0] = newPanel(actions);
			enterLive(panel[0], "+2", "mdps");
			invoke(panel[0], "decrementSpot");
			invoke(panel[0], "decrementSpot");
		});

		assertEquals(1, actions.updateCalls.get());
		assertEquals(0, actions.closeCalls.get());
	}

	@Test
	public void failedSpotUpdateRestoresPriorDisplayedState() throws Exception
	{
		final HostFormPanel[] panel = new HostFormPanel[1];
		final FakeActions actions = new FakeActions();
		onEdt(() ->
		{
			panel[0] = newPanel(actions);
			enterLive(panel[0], "+2", "mdps");
			invoke(panel[0], "fillRole", "mdps");
		});

		actions.completeUpdate("Could not reach the bridge.");
		onEdt(() ->
		{
			final Map<String, String> displayedLiveFields = field(panel[0], "displayedLiveFields");
			assertEquals("+2", displayedLiveFields.get("spots"));
			assertEquals("mdps", displayedLiveFields.get("roles"));
		});
	}

	@Test
	public void callbackAfterShutdownDoesNotMutateLiveStatus() throws Exception
	{
		final HostFormPanel[] panel = new HostFormPanel[1];
		final FakeActions actions = new FakeActions();
		onEdt(() ->
		{
			panel[0] = newPanel(actions);
			enterLive(panel[0], "+2", "mdps");
			invoke(panel[0], "decrementSpot");
		});

		final JLabel liveStatus = field(panel[0], "liveStatus");
		final String before = liveStatus.getText();
		onEdt(() -> panel[0].stopTimers());
		final HostLiveState state = field(panel[0], "liveState");
		assertTrue(state.isStopped());
		actions.completeUpdate("Updated");

		assertEquals(before, liveStatus.getText());
	}

	@Test
	public void reachingZeroSpotsUpdatesInsteadOfClosing() throws Exception
	{
		final HostFormPanel[] panel = new HostFormPanel[1];
		final FakeActions actions = new FakeActions();
		onEdt(() ->
		{
			panel[0] = newPanel(actions);
			enterLive(panel[0], "+1", "mdps");
			invoke(panel[0], "fillRole", "mdps");
		});

		assertEquals(1, actions.updateCalls.get());
		assertEquals(0, actions.closeCalls.get());
		actions.completeUpdate("Updated");
		assertEquals(0, actions.closeCalls.get());
		onEdt(() ->
		{
			final Map<String, String> displayedLiveFields = field(panel[0], "displayedLiveFields");
			assertEquals("+0", displayedLiveFields.get("spots"));
		});
	}

	@Test
	public void undoRevertsLastConfirmedUpdate() throws Exception
	{
		final HostFormPanel[] panel = new HostFormPanel[1];
		final FakeActions actions = new FakeActions();
		onEdt(() ->
		{
			panel[0] = newPanel(actions);
			enterLive(panel[0], "+2", "mdps");
			invoke(panel[0], "decrementSpot");
		});
		actions.completeUpdate("Updated");

		// Click the real control rather than reaching into the panel, so the test covers the
		// button the host actually presses.
		onEdt(() -> clickButton(panel[0], "Undo last change"));
		assertEquals(2, actions.updateCalls.get());
		assertEquals("+2", actions.lastUpdateFields.get("spots"));
		actions.completeUpdate("Updated");
		onEdt(() ->
		{
			final Map<String, String> displayedLiveFields = field(panel[0], "displayedLiveFields");
			assertEquals("+2", displayedLiveFields.get("spots"));
			assertEquals("mdps", displayedLiveFields.get("roles"));
		});
	}

	private static HostFormPanel newPanel(FakeActions actions)
	{
		return new HostFormPanel(new HostDependencies(actions, () -> 0, () -> "", () -> "tester", raid -> -1,
			() ->
			{
			}, () ->
			{
			}, () -> false, world -> null));
	}

	private static void enterLive(HostFormPanel panel, String spots, String roles)
	{
		final Map<String, String> fields = new LinkedHashMap<>();
		fields.put("raid", "TOB");
		fields.put("tier", "Standard");
		fields.put("spots", spots);
		fields.put("roles", roles);
		setField(panel, "lastSubmittedFields", fields);
		panel.enterLivePost("message-id");
	}

	/** Post is gated on an explicit tier and spot count, so seed both before exercising submit. */
	private static void seedValidDraft(HostFormPanel panel)
	{
		final Object raidForm = field(panel, "raidForm");
		final Object fields = field(raidForm, "fields");
		final Object card = field(fields, "card");
		invoke(card, "selectTier", "Standard");
		invoke(card, "selectSpots", "+2");
	}

	/** Finds a button by its label anywhere in the panel and presses it. */
	private static void clickButton(java.awt.Container root, String text)
	{
		final AbstractButton button = findButton(root, text);
		if (button == null)
		{
			throw new AssertionError("No button labelled " + text);
		}
		button.doClick();
	}

	private static AbstractButton findButton(java.awt.Container root, String text)
	{
		for (java.awt.Component child : root.getComponents())
		{
			if (child instanceof AbstractButton && text.equals(((AbstractButton) child).getText()))
			{
				return (AbstractButton) child;
			}
			if (child instanceof java.awt.Container)
			{
				final AbstractButton found = findButton((java.awt.Container) child, text);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static void onEdt(ThrowingRunnable action) throws Exception
	{
		SwingUtilities.invokeAndWait(action::run);
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
			throw new AssertionError(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T field(Object target, String name)
	{
		try
		{
			final Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return (T) field.get(target);
		}
		catch (ReflectiveOperationException e)
		{
			throw new AssertionError(e);
		}
	}

	private static void setField(Object target, String name, Object value)
	{
		try
		{
			final Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		}
		catch (ReflectiveOperationException e)
		{
			throw new AssertionError(e);
		}
	}

	@FunctionalInterface
	private interface ThrowingRunnable
	{
		void run();
	}

	private static final class FakeActions implements HostFormPanel.HostActions
	{
		private final AtomicInteger submitCalls = new AtomicInteger();
		private final AtomicInteger updateCalls = new AtomicInteger();
		private final AtomicInteger closeCalls = new AtomicInteger();
		private Consumer<String> pendingUpdate;
		private Map<String, String> lastUpdateFields;

		@Override
		public void submit(Map<String, String> fields, Consumer<String> status)
		{
			submitCalls.incrementAndGet();
		}

		@Override
		public void update(Map<String, String> fields, Consumer<String> status)
		{
			updateCalls.incrementAndGet();
			lastUpdateFields = fields;
			pendingUpdate = status;
		}

		@Override
		public void close(Map<String, String> fields, Consumer<String> status)
		{
			closeCalls.incrementAndGet();
		}

		private void completeUpdate(String message) throws Exception
		{
			final Consumer<String> callback = pendingUpdate;
			assertTrue(callback != null);
			onEdt(() -> callback.accept(message));
		}
	}
}
