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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import net.runelite.client.ui.FontManager;

/**
 * A flat rectangular button in the WDR palette, matching the client's utilitarian look.
 * Three variants: PRIMARY (filled green, the one main action of a view), GHOST (plain,
 * a soft action) and DANGER (muted red, a destructive action).
 */
public class WdrButton extends JButton
{
	public enum Variant
	{
		PRIMARY, GHOST, DANGER
	}

	private final Variant variant;
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
	}

	private Color textColor()
	{
		switch (variant)
		{
			case PRIMARY:
				return WdrTheme.ACCENT_INK; // dark ink on the bright accent fill
			case DANGER:
				return WdrTheme.ERROR;
			case GHOST:
			default:
				return hover ? Color.WHITE : WdrTheme.TEXT;
		}
	}

	private Color fillColor()
	{
		final boolean pressed = getModel().isArmed() && getModel().isPressed();
		switch (variant)
		{
			case PRIMARY:
				return pressed ? WdrTheme.TEXT_DIM : (hover ? Color.WHITE : WdrTheme.ACCENT);
			case DANGER:
				return pressed ? WdrTheme.BORDER : (hover ? WdrTheme.ERROR_FILL : WdrTheme.FIELD);
			case GHOST:
			default:
				return pressed ? WdrTheme.BORDER : (hover ? WdrTheme.HOVER : WdrTheme.FIELD);
		}
	}

	private Color outlineColor()
	{
		switch (variant)
		{
			case PRIMARY:
				return null;
			case DANGER:
				return WdrTheme.ERROR_FILL;
			case GHOST:
			default:
				return WdrTheme.BORDER;
		}
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		final Graphics2D g2 = (Graphics2D) g.create();
		final int w = getWidth();
		final int h = getHeight();

		g2.setColor(fillColor());
		g2.fillRect(0, 0, w, h);
		final Color outline = outlineColor();
		if (outline != null)
		{
			g2.setColor(outline);
			g2.drawRect(0, 0, w - 1, h - 1);
		}
		g2.dispose();
		super.paintComponent(g);
	}
}
