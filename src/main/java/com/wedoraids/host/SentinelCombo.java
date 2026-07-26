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
import java.util.List;
import javax.swing.JComboBox;

/**
 * A combo box with a sentinel contract: index 0 is always the sentinel ("unset") entry.
 * {@link #read} returns {@code null} while the sentinel is selected, so callers cannot mistake
 * an empty selection for a real value. Programmatic writes run under a guard and do not fire
 * the change callback.
 */
final class SentinelCombo extends JComboBox<String>
{
	private final String sentinel;
	private final Runnable onChange;
	private boolean guard;

	SentinelCombo(String sentinel, Runnable onChange)
	{
		this.sentinel = sentinel;
		this.onChange = onChange;
		WdrTheme.styleCombo(this);
		addItem(sentinel);
		addActionListener(event ->
		{
			if (!guard)
			{
				onChange.run();
			}
		});
	}

	/** Replaces the options, keeping the sentinel on top and restoring the prior choice if still offered. */
	void setOptions(List<String> options)
	{
		guarded(() ->
		{
			final Object previous = getSelectedItem();
			removeAllItems();
			addItem(sentinel);
			for (String option : options)
			{
				addItem(option);
			}
			restore(previous);
		});
	}

	/** Selects the value if offered, otherwise the sentinel; a null or empty value selects the sentinel. */
	void select(String value)
	{
		guarded(() ->
		{
			final String target = value == null || value.isEmpty() ? sentinel : value;
			setSelectedItem(target);
			if (!target.equals(getSelectedItem()))
			{
				setSelectedIndex(0);
			}
		});
	}

	/** The chosen value, or {@code null} while the sentinel is selected. */
	String read()
	{
		final Object item = getSelectedItem();
		return item == null || sentinel.equals(item) ? null : (String) item;
	}

	/** Returns to the sentinel without firing the change callback. */
	void clear()
	{
		guarded(() ->
		{
			if (getItemCount() > 0)
			{
				setSelectedIndex(0);
			}
		});
	}

	private void restore(Object previous)
	{
		if (previous != null && !sentinel.equals(previous))
		{
			for (int index = 0; index < getItemCount(); index++)
			{
				if (previous.equals(getItemAt(index)))
				{
					setSelectedIndex(index);
					return;
				}
			}
		}
		setSelectedIndex(0);
	}

	private void guarded(Runnable write)
	{
		guard = true;
		try
		{
			write.run();
		}
		finally
		{
			guard = false;
		}
	}
}
