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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Locks the wire contract of the host form: whatever the confirmed live map holds, a
 * {@code populate} then {@code collectValidatedFields} must return the same keys and value formats,
 * unknown role tokens must survive, and switching raid must not leak raid-specific keys.
 */
public class HostFormRoundTripTest
{
	@BeforeClass
	public static void enableHeadlessSwing()
	{
		System.setProperty("java.awt.headless", "true");
	}

	@Test
	public void tobRoundTripsExactly() throws Exception
	{
		final Map<String, String> map = new LinkedHashMap<>();
		map.put("raid", "TOB");
		map.put("tier", "Standard");
		map.put("world", "416");
		map.put("size", "5");
		map.put("spots", "+2");
		map.put("roles", "mdps, rdps");
		map.put("partyHub", "catdog");
		map.put("desc", "pogstack");
		assertRoundTrip(map);
	}

	@Test
	public void coxRoundTripsExactly() throws Exception
	{
		final Map<String, String> map = new LinkedHashMap<>();
		map.put("raid", "COX");
		map.put("tier", "Scaled");
		map.put("world", "308");
		map.put("size", "4");
		map.put("spots", "+3");
		map.put("scale", "50");
		map.put("fc", "Zezima");
		map.put("layout", "olm 30");
		map.put("partyHub", "catdog");
		map.put("desc", "max only");
		assertRoundTrip(map);
	}

	@Test
	public void toaRoundTripsExactly() throws Exception
	{
		final Map<String, String> map = new LinkedHashMap<>();
		map.put("raid", "TOA");
		map.put("tier", "300-445");
		map.put("world", "420");
		map.put("size", "4");
		map.put("spots", "+3");
		map.put("partyHub", "catdog");
		map.put("desc", "invo only");
		assertRoundTrip(map);
	}

	@Test
	public void unknownRoleTokensSurvive() throws Exception
	{
		final Map<String, String> map = new LinkedHashMap<>();
		map.put("raid", "TOB");
		map.put("tier", "Standard");
		map.put("world", "416");
		map.put("size", "5");
		map.put("spots", "+2");
		map.put("roles", "mdps, bgs spec");
		final Map<String, String> out = roundTrip(map);
		final Set<String> roles = new HashSet<>(Arrays.asList(out.get("roles").split("\\s*,\\s*")));
		assertTrue("known chip role kept", roles.contains("mdps"));
		assertTrue("unknown role kept", roles.contains("bgs spec"));
	}

	@Test
	public void switchingRaidDoesNotLeakCoxKeys() throws Exception
	{
		final Map<String, String> cox = new LinkedHashMap<>();
		cox.put("raid", "COX");
		cox.put("tier", "Scaled");
		cox.put("world", "308");
		cox.put("size", "4");
		cox.put("spots", "+3");
		cox.put("scale", "50");
		cox.put("fc", "Zezima");
		cox.put("layout", "olm 30");

		final Map<String, String>[] result = collectAfterSwitch(cox, 0);
		final Map<String, String> asCox = result[0];
		final Map<String, String> asTob = result[1];
		assertEquals("COX", asCox.get("raid"));
		assertNotNull("cox keeps scale", asCox.get("scale"));
		assertEquals("TOB", asTob.get("raid"));
		assertFalse("no scale leak", asTob.containsKey("scale"));
		assertFalse("no fc leak", asTob.containsKey("fc"));
		assertFalse("no layout leak", asTob.containsKey("layout"));
	}

	private static void assertRoundTrip(Map<String, String> map) throws Exception
	{
		final Map<String, String> out = roundTrip(map);
		for (Map.Entry<String, String> entry : map.entrySet())
		{
			assertEquals("key " + entry.getKey(), entry.getValue(), out.get(entry.getKey()));
		}
	}

	private static Map<String, String> roundTrip(Map<String, String> map) throws Exception
	{
		final Map<String, String>[] holder = newHolder();
		SwingUtilities.invokeAndWait(() ->
		{
			final HostRaidForm form = newForm();
			form.populate(map);
			holder[0] = form.collectValidatedFields();
		});
		assertNotNull("collect returned a map", holder[0]);
		return holder[0];
	}

	private static Map<String, String>[] collectAfterSwitch(Map<String, String> map, int toRaidIndex)
		throws Exception
	{
		@SuppressWarnings("unchecked")
		final Map<String, String>[] out = new Map[2];
		SwingUtilities.invokeAndWait(() ->
		{
			final HostRaidForm form = newForm();
			form.populate(map);
			out[0] = form.collectValidatedFields();
			invoke(form, "selectRaidTab", toRaidIndex);
			// Switching raid resets the tier to the sentinel by design, so re-choose a valid ToB tier and
			// spots; the concealed CoX values must still be omitted from the ToB submission, not leaked.
			final Object fields = field(form, "fields");
			final Object card = field(fields, "card");
			invoke(card, "selectTier", "Standard");
			invoke(card, "selectSpots", "+2");
			out[1] = form.collectValidatedFields();
		});
		assertNotNull(out[0]);
		assertNotNull(out[1]);
		return out;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String>[] newHolder()
	{
		return new Map[1];
	}

	private static HostRaidForm newForm()
	{
		final HostDependencies dependencies = new HostDependencies(new NoopActions(), () -> 416,
			() -> "", () -> "Zezima", raid -> -1, () ->
			{
			}, () ->
			{
			}, () -> false, world -> null);
		return new HostRaidForm(dependencies, () ->
		{
		}, () ->
		{
		});
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

	private static Object field(Object target, String name)
	{
		try
		{
			final java.lang.reflect.Field declared = target.getClass().getDeclaredField(name);
			declared.setAccessible(true);
			return declared.get(target);
		}
		catch (ReflectiveOperationException e)
		{
			throw new AssertionError(e);
		}
	}

	private static final class NoopActions implements HostFormPanel.HostActions
	{
		@Override
		public void submit(Map<String, String> fields, java.util.function.Consumer<String> status)
		{
		}

		@Override
		public void update(Map<String, String> fields, java.util.function.Consumer<String> status)
		{
		}

		@Override
		public void close(Map<String, String> fields, java.util.function.Consumer<String> status)
		{
		}
	}
}
