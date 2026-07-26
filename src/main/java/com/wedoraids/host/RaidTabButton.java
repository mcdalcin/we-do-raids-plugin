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

import com.wedoraids.feed.RaidType;
import com.wedoraids.ui.WdrTheme;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.BooleanSupplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import net.runelite.client.ui.FontManager;

/**
 * One segment of the raid selector.
 *
 * <p>Picking a raid is exclusive, so the segments share edges inside a single outline: one bounded
 * control reads as "pick one", where three separate pills read the same as the multi-select role chips
 * below them.
 *
 * <p>The raid rides on the fill rather than the label. A fill cannot both clear the surface it sits on
 * and stay dark enough to carry the hue as ink — {@link WdrTheme#ACCENT_EDGE} records the same conflict
 * for the primary control — so the fill carries the raid and the ink goes to {@link WdrTheme#TEXT}.
 */
final class RaidTabButton extends JButton
{
	/** Position in the selector, which decides where the bar is rounded and where it is divided. */
	enum Segment
	{
		FIRST, MIDDLE, LAST
	}

	/**
	 * How far the selected fill travels from the canvas toward the raid hue. Bounded on both sides: below
	 * 0.30 it drops under the 1.82:1 {@link WdrTheme#CHIP_CHOSEN} sets as the floor for a glanceable
	 * state, above 0.40 the white ink falls through 4.5:1. Measures 2.24:1 to 2.36:1.
	 */
	private static final double RAID_BLEND = 0.40;

	private static final int ARC = 10;

	private final RaidType raid;
	private final BooleanSupplier selected;
	private final Segment segment;

	RaidTabButton(RaidType raid, Segment segment, BooleanSupplier selected, Runnable onSelected)
	{
		super(raid.getDisplayName());
		this.raid = raid;
		this.segment = segment;
		this.selected = selected;
		setFont(FontManager.getRunescapeSmallFont());
		setForeground(WdrTheme.TEXT_DIM);
		setFocusPainted(false);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setOpaque(false);
		setCursor(new Cursor(Cursor.HAND_CURSOR));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		addActionListener(e -> onSelected.run());
	}

	void refreshSelection()
	{
		setForeground(selected.getAsBoolean() ? WdrTheme.TEXT : WdrTheme.TEXT_DIM);
		repaint();
	}

	/** The selected fill: the canvas walked {@value #RAID_BLEND} of the way to this raid's hue. */
	private Color raidFill()
	{
		final Color hue = raid.getColor();
		final Color base = WdrTheme.BACKGROUND;
		return new Color(
			blend(base.getRed(), hue.getRed()),
			blend(base.getGreen(), hue.getGreen()),
			blend(base.getBlue(), hue.getBlue()));
	}

	private static int blend(int from, int to)
	{
		return (int) Math.round(from + RAID_BLEND * (to - from));
	}

	/**
	 * The rounded rectangle for this segment, overhung past whichever edges are shared. Java2D rounds all
	 * four corners or none, so overhanging by the radius and letting the clip discard it leaves the outer
	 * side rounded and the inner side square.
	 */
	private int spanStart()
	{
		return segment == Segment.FIRST ? 0 : -ARC;
	}

	private int spanWidth(int width)
	{
		if (segment == Segment.MIDDLE)
		{
			return width + 2 * ARC;
		}
		return width + ARC;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		final Graphics2D graphics2d = (Graphics2D) graphics.create();
		graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final int width = getWidth();
		final int height = getHeight();
		final boolean isSelected = selected.getAsBoolean();
		final Color fill;
		if (isSelected)
		{
			fill = raidFill();
		}
		else if (getModel().isRollover())
		{
			fill = WdrTheme.HOVER;
		}
		else
		{
			fill = WdrTheme.FIELD;
		}
		graphics2d.clipRect(0, 0, width, height);
		graphics2d.setColor(fill);
		graphics2d.fillRoundRect(spanStart(), 0, spanWidth(width), height, ARC, ARC);
		// One outline around the whole bar, so the shared edges carry a single divider instead of two
		// abutting borders. The raid stays on the fill; the boundary stays neutral in every state.
		graphics2d.setColor(WdrTheme.BORDER);
		graphics2d.drawRoundRect(spanStart(), 0, spanWidth(width) - 1, height - 1, ARC, ARC);
		if (segment != Segment.LAST)
		{
			graphics2d.drawLine(width - 1, 0, width - 1, height - 1);
		}
		graphics2d.dispose();
		super.paintComponent(graphics);
	}
}
