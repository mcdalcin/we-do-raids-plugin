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
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * We Do Raids palette, organised by role rather than by swatch: one role, one meaning.
 *
 * <p>Surfaces and borders come from RuneLite's {@link ColorScheme} so the panel sits in the client
 * rather than on top of it. Chroma belongs to raid identity — the hues on
 * {@link com.wedoraids.feed.RaidType}, equalised at OKLCH lightness 0.76 so no raid outranks
 * another. Chrome stays neutral so it is never mistaken for raid data; failure is the one exception.
 */
public final class WdrTheme
{
	/** Panel canvas. */
	public static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
	/** Recessed surface for cards and notices, one step below the canvas. */
	public static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
	/** Input and control fill; the same step as {@link #CARD}. */
	public static final Color FIELD = ColorScheme.DARKER_GRAY_COLOR;
	/** Hover step for neutral controls. */
	public static final Color HOVER = ColorScheme.DARK_GRAY_HOVER_COLOR;
	/** Neutral outline for cards, fields and controls. */
	public static final Color BORDER = ColorScheme.MEDIUM_GRAY_COLOR;
	/**
	 * Raised fill of a chosen toggle chip. Neutral by rule — raid hue means raid identity — and held
	 * at 1.82:1 against the {@link #FIELD} surface it replaces so the state reads at a glance.
	 */
	public static final Color CHIP_CHOSEN = new Color(72, 72, 72);

	/** Primary ink. */
	public static final Color TEXT = new Color(226, 226, 226);
	/** Secondary ink: supporting detail that still has to be readable at a glance. */
	public static final Color TEXT_DIM = new Color(158, 158, 158);
	/** Tertiary ink: metadata (timestamps, sources). Still clears AA on both surfaces. */
	public static final Color TEXT_MUTED = new Color(146, 146, 146);

	/**
	 * Fill of the one primary action in a view: the WDR green, deepened into a surface.
	 *
	 * <p>A primary control on a dark panel earns rank through chroma, not brightness; this ramp sits
	 * at 11-14x the card's luminance under white ink. Green is brand, not data: raid hues render as
	 * light text and card edges near OKLCH lightness 0.76, while this is a filled control at 0.50 and
	 * never appears inside a raid card. Sized from {@link #ACCENT_HOVER} down, the lightest state.
	 */
	public static final Color ACCENT = new Color(0, 122, 53);
	/** Hover step. The lightest state, and still 4.64:1 under white ink. */
	public static final Color ACCENT_HOVER = new Color(26, 134, 65);
	/** Pressed step: deeper than the base, so the ramp reads in one direction. */
	public static final Color ACCENT_PRESSED = new Color(0, 103, 44);
	/** Ink on the accent ramp. White, so the button keeps the panel's light-on-dark language. */
	public static final Color ACCENT_INK = new Color(255, 255, 255);
	/**
	 * The 1px edge of the primary control, and the only part of it required to clear 3:1.
	 *
	 * <p>Separating boundary from state is what makes the ramp legal: one fill cannot hold both 3:1
	 * against its surface and 4.5:1 under its own ink without going inert. The edge pins the boundary
	 * at 4.29:1 on the canvas, freeing the fill beneath it to move purely for feedback. At OKLCH
	 * lightness 0.62 it stays below the raid band at 0.76.
	 */
	public static final Color ACCENT_EDGE = new Color(42, 158, 82);
	/** Keyboard focus ring. White, so a single focus treatment reads on every variant's fill. */
	public static final Color FOCUS_RING = new Color(255, 255, 255);

	/**
	 * Failure and destructive intent: the only chroma outside raid identity.
	 *
	 * <p>Renders unfilled straight onto the canvas in the footer status and demo banner, so it has to
	 * clear 4.5:1 there; it measures 5.28:1 on the canvas and 5.97:1 on a card. Hue 22 keeps it clear
	 * of ToA's amber at 73, so failure never reads as a raid.
	 */
	public static final Color ERROR = new Color(242, 116, 116);
	/** Hover fill for destructive controls: a red tint at twice the card's luminance. */
	public static final Color ERROR_FILL = new Color(72, 34, 34);
	/**
	 * Pressed fill for destructive controls. Deepens rather than lightens, because any fill above the
	 * hover step drops its ink under 4.5:1; the press instead reads on two channels at once, the fill
	 * sinking while the edge lights up to {@link #ERROR}.
	 */
	public static final Color ERROR_PRESSED = new Color(56, 26, 26);

	/**
	 * The edge and inset that make a panel read as one card rather than one of many.
	 *
	 * <p>A surface step alone cannot hold a card: {@link #CARD} measures 1.14:1 against the canvas, so
	 * an unbordered card dissolves into it. Outlined means "one thing", railed means "one of many".
	 */
	public static final Border CARD_BORDER = BorderFactory.createCompoundBorder(
		BorderFactory.createLineBorder(BORDER),
		BorderFactory.createEmptyBorder(8, 9, 9, 9));

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
		f.setBorder(fieldBorder(BORDER));
	}

	/**
	 * Marks a field whose value failed validation, and clears itself on the first edit.
	 *
	 * <p>The status line naming the failure can sit a fold away from the field it names, so the border
	 * carries an association the message alone cannot. It restores {@link #styleField}'s border on the
	 * first keystroke, because a mark that outlives the mistake becomes noise.
	 */
	public static void flagInvalid(JTextComponent f)
	{
		f.setBorder(fieldBorder(ERROR));
		f.getDocument().addDocumentListener(new DocumentListener()
		{
			private void clear()
			{
				f.getDocument().removeDocumentListener(this);
				f.setBorder(fieldBorder(BORDER));
			}

			@Override
			public void insertUpdate(DocumentEvent event)
			{
				clear();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				clear();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				clear();
			}
		});
	}

	/** The shared field outline, tinted by state: {@link #BORDER} at rest, {@link #ERROR} when invalid. */
	private static Border fieldBorder(Color line)
	{
		return BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(line),
			BorderFactory.createEmptyBorder(2, 4, 2, 4));
	}

	public static void styleCombo(JComboBox<?> c)
	{
		c.setBackground(FIELD);
		c.setForeground(TEXT);
		c.setFont(FontManager.getRunescapeSmallFont());
	}
}
