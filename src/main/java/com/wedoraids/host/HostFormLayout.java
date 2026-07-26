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

import com.wedoraids.ui.SplitGrid;
import com.wedoraids.ui.WdrTheme;
import com.wedoraids.ui.WrappedText;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.FontManager;

/**
 * Shared layout idioms for the host form, kept in one place so the draft card and the More section
 * speak the same grid. Rows carry a small dim label above the control, matching the panel's
 * label-over-field convention.
 */
final class HostFormLayout
{
	private HostFormLayout()
	{
	}

	/** A vertical box whose maximum height tracks its preferred height, so BoxLayout never stretches it. */
	static final class CappedPanel extends JPanel
	{
		CappedPanel()
		{
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setOpaque(false);
			setAlignmentX(Component.LEFT_ALIGNMENT);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}
	}

	static CappedPanel column()
	{
		return new CappedPanel();
	}

	static JPanel labeled(String label, Component field)
	{
		final JLabel text = new JLabel(label);
		// Scaffolding, not data: a label is read once to find its control, the value every time after.
		// Both sat at TEXT_DIM, which is what flattened the card to a single tone; TEXT_MUTED is the
		// tier the feed beside it already uses for supporting detail, so this borrows rather than adds.
		text.setForeground(WdrTheme.TEXT_MUTED);
		text.setFont(FontManager.getRunescapeSmallFont());
		final JPanel row = new JPanel(new BorderLayout(0, 1));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		// Space above, none below. The label belongs to the field beneath it, so the gap has to fall
		// between groups instead of inside one; symmetric padding spent it in both places at once and
		// left the card a uniform stack. Costs 2px a row and doubles the separation between groups.
		row.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		row.add(text, BorderLayout.NORTH);
		row.add(field, BorderLayout.CENTER);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	/** A labelled control with a wrapped, dim hint line beneath it, so guidance is visible, not a tooltip. */
	static JPanel labeledWithHint(String label, Component field, JLabel hint)
	{
		final CappedPanel box = column();
		box.add(labeled(label, field));
		box.add(hint);
		return box;
	}

	static JPanel pair(JPanel left, JPanel right)
	{
		final JPanel pair = new JPanel(new SplitGrid(6, 0));
		pair.setOpaque(false);
		pair.add(left);
		pair.add(right);
		pair.setAlignmentX(Component.LEFT_ALIGNMENT);
		pair.setMaximumSize(new Dimension(Integer.MAX_VALUE, pair.getPreferredSize().height));
		return pair;
	}

	static void fullWidth(JComponent component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
	}

	/** A dim, wrapped hint line at the panel measure, styled like {@code HostLivePostView.hint}. */
	static JLabel hint(String text)
	{
		final JLabel hint = new JLabel(WrappedText.html(text, WrappedText.PANEL));
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setForeground(WdrTheme.TEXT_DIM);
		hint.setAlignmentX(Component.LEFT_ALIGNMENT);
		return hint;
	}

	static DocumentListener onChange(Runnable callback)
	{
		return new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				callback.run();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				callback.run();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				callback.run();
			}
		};
	}
}
