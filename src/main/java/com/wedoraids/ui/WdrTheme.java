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
	 * The fill of the one primary action in a view: the WDR green, deepened into a surface.
	 *
	 * <p>A primary control on a dark panel earns its rank through chroma, not brightness. The
	 * previous neutral fills tried to earn it through brightness and failed twice over: near-white
	 * glared at 62 times the card's luminance, and the softened mid-grey still sat at 36 times while
	 * carrying no chroma at all, which made it read as a disabled control and forced dark ink,
	 * inverting the panel's dark-surface/light-text language. Measured, the dark-tool primaries this
	 * resembles sit near 14 times: GitHub's green and Discord's blurple both do. This ramp lands at
	 * 11 to 14 times with white ink above it, so the button matches the panel instead of fighting it.
	 *
	 * <p>Green is the WDR brand, and using it here does not break the rule that hue means raid.
	 * That rule governs the information layer, where a raid's hue appears as light text and a card
	 * edge around OKLCH lightness 0.76. This is a control, filled, at lightness 0.50: a different
	 * channel and a different band, never rendered inside a raid card. The alternatives were worse.
	 * Amber and purple sit closer to ToA and ToB in role as well as hue, red is spoken for by
	 * failure, and blue is the generic tech-tool accent PRODUCT.md lists as an anti-reference.
	 *
	 * <p>Sized from the hover step down: hover is the lightest state, so it sets the ceiling that
	 * keeps white ink past 4.5:1 everywhere.
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
	 * <p>Splitting boundary from state is what makes the ramp legal. A filled control has to satisfy
	 * two bounds at once: 3:1 against the surface it sits on, so its edge is findable, and 4.5:1 under
	 * its own ink. Against the panel canvas those bounds leave a window only 1.12 times wide in
	 * luminance, which is narrower than a hover step a user can actually see. A fill carrying both is
	 * therefore either non-compliant or inert, and the previous ramp was the former: it measured 2.69:1
	 * on the canvas, where the largest primary in the plugin sits. This edge holds the boundary at a
	 * fixed 4.29:1 on the canvas and 4.85:1 on a card, in every state, which frees the fill beneath it
	 * to move purely for feedback.
	 *
	 * <p>At OKLCH lightness 0.62 it stays below the raid band at 0.76, so the information layer remains
	 * the only place a hue means a raid.
	 */
	public static final Color ACCENT_EDGE = new Color(42, 158, 82);
	/** Keyboard focus ring. White, so a single focus treatment reads on every variant's fill. */
	public static final Color FOCUS_RING = new Color(255, 255, 255);

	/**
	 * Failure and destructive intent. The only place chroma appears outside raid identity.
	 *
	 * <p>Lightened from its previous value, which measured 4.29:1 on the panel canvas and so missed
	 * the 4.5:1 body minimum in the two places it actually renders there: the offline status in the
	 * footer and the demo banner, both of which draw unfilled straight onto the canvas. At 5.28:1 it
	 * now clears on the canvas and 5.97:1 on a card, and lightening the ink also repaired the
	 * destructive button's hover state for free, which had been sitting at 4.00:1.
	 *
	 * <p>Hue 22 keeps it well clear of ToA's amber at 73, so failure never reads as a raid.
	 */
	public static final Color ERROR = new Color(242, 116, 116);
	/** Hover fill for destructive controls: a red tint at twice the card's luminance. */
	public static final Color ERROR_FILL = new Color(72, 34, 34);
	/**
	 * Pressed fill for destructive controls.
	 *
	 * <p>It deepens rather than lightens because the ink caps how light the fill may go: every
	 * candidate above the hover step fell under 4.5:1. The press therefore reads on two channels at
	 * once, the fill sinking and the edge lighting up to {@link #ERROR}, which is clearer than a
	 * luminance step alone and replaces the previous neutral grey that erased the control's identity.
	 */
	public static final Color ERROR_PRESSED = new Color(56, 26, 26);

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
