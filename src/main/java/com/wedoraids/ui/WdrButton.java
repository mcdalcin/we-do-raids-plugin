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
 * Flat rectangular button in the WDR palette.
 *
 * <p>Five variants by assertion weight: PRIMARY (filled green, the one action a view exists to
 * perform), ENTRY (accent edge on bare surface, the way into a flow), GHOST (plain, a soft
 * action), DANGER (muted red, destructive), QUIET (unboxed, reveals rather than acts).
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
				setHover(true);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setHover(false);
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

	private void setHover(boolean hovering)
	{
		hover = hovering;
		setForeground(textColor());
		repaint();
	}

	/**
	 * Swaps the button's variant. A control that both opens and closes a view is the main action
	 * in one state and secondary in the other; variant belongs to state, not construction.
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
			// Disabled reads as unavailable, not as a soft ghost action.
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
				// Marked by its edge, not filled: a filled ENTRY competes with raid data in the feed.
				return pressed ? WdrTheme.ACCENT_PRESSED : (hover ? towardSurface(WdrTheme.ACCENT) : null);
			case DANGER:
				return pressed ? WdrTheme.ERROR_PRESSED : (hover ? WdrTheme.ERROR_FILL : WdrTheme.FIELD);
			case QUIET:
				// No box at rest so it cannot compete with the view's submit; hover still fills so it reads as clickable.
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
			// Disabled PRIMARY keeps a muted accent edge so its rank survives being unavailable;
			// without it a disabled submit is pixel-identical to the enabled disclosure above it.
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
				// Neutral ink above it; any green light enough to pass 4.5:1 on the canvas lands in the
				// raid band at OKLCH ~0.76 and reads as CoX. The edge sits at 0.618, clear of that band.
				return WdrTheme.ACCENT_EDGE;
			case DANGER:
				// Edge lights up on press because the fill cannot go lighter without dropping ink under 4.5:1.
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
			// setFocusPainted(false) drops the L&F ring; paint one manually for WCAG 2.4.7.
			g2.setColor(WdrTheme.FOCUS_RING);
			g2.drawRect(2, 2, w - 5, h - 5);
		}
		g2.dispose();
		super.paintComponent(g);
	}
}
