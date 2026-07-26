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

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

/**
 * Finds and drives real controls in a rendered panel, the way a user reaches them.
 *
 * <p>Everything here goes through public Swing API: the tree is walked with
 * {@link Container#getComponents()} and controls are driven with {@link AbstractButton#doClick()} or
 * their own setters. Nothing reflects on private state, so a rename breaks the compile instead of
 * rendering a state the panel cannot produce.
 *
 * <p>Every lookup throws rather than returning null. A miss means the control moved; an ambiguous
 * match means the label is no longer unique, and picking one at random would make the result
 * meaningless.
 */
public final class SwingProbe
{
	private SwingProbe()
	{
	}

	/** Every descendant of the given type, in tree order. */
	public static <T> List<T> all(Container root, Class<T> type)
	{
		final List<T> found = new ArrayList<>();
		collect(root, type, found);
		return found;
	}

	private static <T> void collect(Container root, Class<T> type, List<T> into)
	{
		for (Component child : root.getComponents())
		{
			if (type.isInstance(child))
			{
				into.add(type.cast(child));
			}
			if (child instanceof Container)
			{
				collect((Container) child, type, into);
			}
		}
	}

	/** The only descendant of the given type, or an error naming how many were found. */
	public static <T> T only(Container root, Class<T> type)
	{
		final List<T> found = all(root, type);
		if (found.size() != 1)
		{
			throw new AssertionError("Expected exactly one " + type.getSimpleName()
				+ ", found " + found.size());
		}
		return found.get(0);
	}

	/** The button carrying this exact label. */
	public static AbstractButton button(Container root, String text)
	{
		final List<AbstractButton> hits = new ArrayList<>();
		for (AbstractButton button : all(root, AbstractButton.class))
		{
			if (text.equals(button.getText()))
			{
				hits.add(button);
			}
		}
		return single(hits, "button labelled \"" + text + "\"");
	}

	/** True when a button carrying this exact label exists, for controls that toggle their text. */
	public static boolean hasButton(Container root, String text)
	{
		for (AbstractButton button : all(root, AbstractButton.class))
		{
			if (text.equals(button.getText()))
			{
				return true;
			}
		}
		return false;
	}

	/** Clicks the button carrying this exact label, firing its listeners. */
	public static void click(Container root, String text)
	{
		button(root, text).doClick();
	}

	/** The label carrying this exact text. */
	public static JLabel label(Container root, String text)
	{
		final List<JLabel> hits = new ArrayList<>();
		for (JLabel label : all(root, JLabel.class))
		{
			if (text.equals(label.getText()))
			{
				hits.add(label);
			}
		}
		return single(hits, "label reading \"" + text + "\"");
	}

	/** The label whose text contains this fragment. */
	public static JLabel labelContaining(Container root, String fragment)
	{
		final List<JLabel> hits = new ArrayList<>();
		for (JLabel label : all(root, JLabel.class))
		{
			if (label.getText() != null && label.getText().contains(fragment))
			{
				hits.add(label);
			}
		}
		return single(hits, "label containing \"" + fragment + "\"");
	}

	/**
	 * The control sitting under a caption, as {@code HostFormLayout.labeled} arranges it.
	 *
	 * <p>That helper puts the caption north of its control in one row, so the control is the only
	 * descendant of the wanted type inside the caption's parent.
	 */
	public static <T> T labelled(Container root, String caption, Class<T> type)
	{
		final Container row = label(root, caption).getParent();
		final List<T> found = all(row, type);
		return single(found, type.getSimpleName() + " under \"" + caption + "\"");
	}

	/** The combo box offering this item, chosen by an option only it carries. */
	public static JComboBox<?> comboOffering(Container root, String item)
	{
		final List<JComboBox<?>> hits = new ArrayList<>();
		for (JComboBox<?> combo : all(root, JComboBox.class))
		{
			for (int i = 0; i < combo.getItemCount(); i++)
			{
				if (item.equals(String.valueOf(combo.getItemAt(i))))
				{
					hits.add(combo);
					break;
				}
			}
		}
		return single(hits, "combo offering \"" + item + "\"");
	}

	/** Selects an item on the combo that offers it, firing its listeners. */
	public static void selectOffered(Container root, String item)
	{
		comboOffering(root, item).setSelectedItem(item);
	}

	/**
	 * Presses a label that carries its own mouse handling, such as the party hub link.
	 *
	 * <p>Labels are not buttons, so there is no {@code doClick()}; the press is delivered to the
	 * listeners the label registered, which is the same call Swing makes on a real press.
	 */
	public static void press(JLabel label)
	{
		final MouseEvent event = new MouseEvent(label, MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), 0, 1, 1, 1, false);
		for (MouseListener listener : label.getMouseListeners())
		{
			listener.mousePressed(event);
		}
	}

	private static <T> T single(List<T> hits, String what)
	{
		if (hits.isEmpty())
		{
			throw new AssertionError("No " + what);
		}
		if (hits.size() > 1)
		{
			throw new AssertionError("Ambiguous " + what + ": found " + hits.size());
		}
		return hits.get(0);
	}
}
