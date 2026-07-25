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
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import net.runelite.client.ui.FontManager;

/**
 * A flat rectangular button in the WDR palette, matching the client's utilitarian look.
 * Five variants, ordered by how much they claim: PRIMARY (filled green, the one action a view exists
 * to perform), ENTRY (accent edge on the bare surface, for the way into a flow), GHOST (plain, a soft
 * action), DANGER (muted red, destructive) and QUIET (unboxed, for chrome that reveals rather than
 * acts).
 */
public class WdrButton extends JButton
{
	public enum Variant
	{
		PRIMARY, ENTRY, GHOST, DANGER, QUIET
	}

	private Variant variant;
	private boolean hover;

	public WdrButton(String text, Variant variant)
	{
		super(text);
		this.variant = variant;
		setFont(FontManager.getRunescapeSmallFont());
		setFocusPainted(false);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setOpaque(false);
		setForeground(textColor());
		setCursor(new Cursor(Cursor.HAND_CURSOR));
		setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				hover = true;
				setForeground(textColor());
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover = false;
				setForeground(textColor());
				repaint();
			}
		});
		addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusGained(FocusEvent e)
			{
				repaint();
			}

			@Override
			public void focusLost(FocusEvent e)
			{
				repaint();
			}
		});
	}

	/**
	 * Swaps the button's rank. A control that both opens and closes a view is the main action in one
	 * of those states and a secondary one in the other, so its variant belongs to the state rather
	 * than to construction, and the view keeps exactly one primary either way.
	 */
	public void setVariant(Variant variant)
	{
		if (this.variant == variant)
		{
			return;
		}
		this.variant = variant;
		setForeground(textColor());
		repaint();
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		setCursor(new Cursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
		setForeground(textColor());
		repaint();
	}

	private Color textColor()
	{
		if (!isEnabled())
		{
			// A disabled control has to read as unavailable, not as a soft ghost action.
			return WdrTheme.TEXT_MUTED;
		}
		switch (variant)
		{
			case PRIMARY:
				return WdrTheme.ACCENT_INK; // light ink on the deep accent fill
			case DANGER:
				return WdrTheme.ERROR;
			case ENTRY:
				return hover ? Color.WHITE : WdrTheme.TEXT;
			case QUIET:
				return hover ? WdrTheme.TEXT : WdrTheme.TEXT_DIM;
			case GHOST:
			default:
				return hover ? Color.WHITE : WdrTheme.TEXT;
		}
	}

	private Color fillColor()
	{
		if (!isEnabled())
		{
			return variant == Variant.QUIET ? null : WdrTheme.FIELD;
		}
		final boolean pressed = getModel().isArmed() && getModel().isPressed();
		switch (variant)
		{
			case PRIMARY:
				return pressed ? WdrTheme.ACCENT_PRESSED : (hover ? WdrTheme.ACCENT_HOVER : WdrTheme.ACCENT);
			case ENTRY:
				// Marked by its edge, not filled. Filling this read as the view's submit and, measured, put
				// 6466px of chroma in the chrome against 1642px across every raid hue in the feed below it,
				// which inverts the rule that chrome never competes with raid data. The edge costs 480px.
				return pressed ? WdrTheme.ACCENT_PRESSED : (hover ? towardSurface(WdrTheme.ACCENT) : null);
			case DANGER:
				return pressed ? WdrTheme.ERROR_PRESSED : (hover ? WdrTheme.ERROR_FILL : WdrTheme.FIELD);
			case QUIET:
				// Chrome, not an action: no box at rest, so it cannot compete with the view's submit. Hover
				// still fills, because a control that responds to nothing does not read as clickable.
				return hover ? WdrTheme.HOVER : null;
			case GHOST:
			default:
				return pressed ? WdrTheme.BORDER : (hover ? WdrTheme.HOVER : WdrTheme.FIELD);
		}
	}

	private Color outlineColor()
	{
		if (!isEnabled())
		{
			// Rank has to survive being unavailable. A disabled primary keeps a muted trace of its accent
			// edge, because rendering it in the same neutral edge as everything else made the submit
			// pixel-identical to the enabled disclosure directly above it: same fill, same border, same
			// height, differing only in ink. The one action the view exists for was the hardest to find.
			if (variant == Variant.PRIMARY)
			{
				return towardSurface(WdrTheme.ACCENT_EDGE);
			}
			return variant == Variant.QUIET ? null : WdrTheme.BORDER;
		}
		switch (variant)
		{
			case PRIMARY:
				return WdrTheme.ACCENT_EDGE; // carries the 3:1 boundary so the fill is free to signal state
			case ENTRY:
				// Neutral ink above it, because any green light enough to pass 4.5:1 on the canvas lands in
				// the raid band around OKLCH lightness 0.76 and would read as CoX. The edge sits at 0.618,
				// clear of it, so the accent still says "this is the way in" without borrowing a raid's hue.
				return WdrTheme.ACCENT_EDGE;
			case DANGER:
				// The edge lights up on press because the fill cannot: ink caps how light the fill may go,
				// so the press reads on the edge instead of a luminance step that would fail contrast.
				return getModel().isArmed() && getModel().isPressed() ? WdrTheme.ERROR : WdrTheme.ERROR_FILL;
			case QUIET:
				return null;
			case GHOST:
			default:
				return WdrTheme.BORDER;
		}
	}

	/** Halfway from a colour to the recessed surface: keeps the hue, drops the assertion. */
	private static Color towardSurface(Color color)
	{
		final Color surface = WdrTheme.FIELD;
		return new Color(
			(color.getRed() + surface.getRed()) / 2,
			(color.getGreen() + surface.getGreen()) / 2,
			(color.getBlue() + surface.getBlue()) / 2);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		final Graphics2D g2 = (Graphics2D) g.create();
		final int w = getWidth();
		final int h = getHeight();

		final Color fill = fillColor();
		if (fill != null)
		{
			g2.setColor(fill);
			g2.fillRect(0, 0, w, h);
		}
		final Color outline = outlineColor();
		if (outline != null)
		{
			g2.setColor(outline);
			g2.drawRect(0, 0, w - 1, h - 1);
		}
		if (isFocusOwner())
		{
			// setFocusPainted(false) drops the look and feel's own ring, so paint one: WCAG 2.4.7 is AA,
			// and the key field in the verification notice is reached by keyboard before this button is.
			g2.setColor(WdrTheme.FOCUS_RING);
			g2.drawRect(2, 2, w - 5, h - 5);
		}
		g2.dispose();
		super.paintComponent(g);
	}
}
