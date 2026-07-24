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
package com.wedoraids.ui;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * We Do Raids palette, built as roles rather than swatches. One role, one meaning.
 *
 * <p>Surfaces and borders come from RuneLite's native {@link ColorScheme} so the panel sits in
 * the client rather than on top of it. Saturated colour is reserved almost entirely for raid
 * identity: the three raid hues are the panel's data dimension and are the only chroma a user
 * sees in a normal feed. Chrome (actions, links, status) is deliberately neutral so it never
 * competes with, or is mistaken for, raid data. Failure states are the one exception.
 *
 * <p>Raid hues live on {@link com.wedoraids.feed.RaidType}; they are equalised in perceptual
 * lightness (OKLCH L=0.76) so no raid outranks another by colour weight alone.
 */
public final class WdrTheme
{
	/** Panel canvas. */
	public static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
	/** Recessed surface for cards and notices, one step below the canvas. */
	public static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
	public static final Color FIELD = ColorScheme.DARKER_GRAY_COLOR;
	public static final Color HOVER = ColorScheme.DARK_GRAY_HOVER_COLOR;
	public static final Color BORDER = ColorScheme.MEDIUM_GRAY_COLOR;

	/** Primary ink. */
	public static final Color TEXT = new Color(226, 226, 226);
	/** Secondary ink: supporting detail that still has to be readable at a glance. */
	public static final Color TEXT_DIM = new Color(158, 158, 158);
	/** Tertiary ink: metadata (timestamps, sources). Still clears AA on both surfaces. */
	public static final Color TEXT_MUTED = new Color(146, 146, 146);

	/**
	 * The single non-data accent. Neutral by design: a chromatic accent would either collide
	 * with a raid hue or add a fourth colour to a 225px column. Reserved for the one primary
	 * action in a view, never for decoration.
	 */
	public static final Color ACCENT = new Color(232, 232, 232);
	/** Ink on {@link #ACCENT}. */
	public static final Color ACCENT_INK = new Color(24, 24, 24);

	/** Failure and destructive intent. The only place chroma appears outside raid identity. */
	public static final Color ERROR = new Color(232, 92, 92);
	/** Muted fill for destructive controls. */
	public static final Color ERROR_FILL = new Color(72, 34, 34);

	private WdrTheme()
	{
	}

	public static void styleButton(JButton b)
	{
		b.setBackground(FIELD);
		b.setForeground(TEXT);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setFocusPainted(false);
		b.setOpaque(true);
		b.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(BORDER),
			BorderFactory.createEmptyBorder(3, 8, 3, 8)));
	}

	public static void styleField(JTextComponent f)
	{
		f.setBackground(FIELD);
		f.setForeground(TEXT);
		f.setCaretColor(TEXT);
		f.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(BORDER),
			BorderFactory.createEmptyBorder(2, 4, 2, 4)));
	}

	public static void styleCombo(JComboBox<?> c)
	{
		c.setBackground(FIELD);
		c.setForeground(TEXT);
		c.setFont(FontManager.getRunescapeSmallFont());
	}
}
