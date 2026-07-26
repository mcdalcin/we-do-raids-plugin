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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * A two-column grid that keeps both rails flush.
 *
 * <p>{@link java.awt.GridLayout} floors {@code (width - hgap) / 2} and discards the remainder,
 * leaving the trailing rail one pixel short of the rows above it. The defect also flips with the
 * scrollbar, so no fixed gap parity is right in both states. This layout gives the left column
 * the floor and the right column the remainder; a column one pixel wider is invisible, a ragged
 * shared rail is not.
 *
 * <p>Children flow left-to-right into rows of two. A sole child spans the full width.
 */
public final class SplitGrid implements LayoutManager
{
	private final int hgap;
	private final int vgap;

	public SplitGrid(int hgap, int vgap)
	{
		this.hgap = hgap;
		this.vgap = vgap;
	}

	@Override
	public void addLayoutComponent(String name, Component component)
	{
	}

	@Override
	public void removeLayoutComponent(Component component)
	{
	}

	@Override
	public Dimension preferredLayoutSize(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			final Insets insets = parent.getInsets();
			final int count = parent.getComponentCount();
			final int rows = (count + 1) / 2;
			int cellWidth = 0;
			int cellHeight = 0;
			for (int i = 0; i < count; i++)
			{
				final Dimension size = parent.getComponent(i).getPreferredSize();
				cellWidth = Math.max(cellWidth, size.width);
				cellHeight = Math.max(cellHeight, size.height);
			}
			final int cols = Math.min(count, 2);
			return new Dimension(
				insets.left + insets.right + cols * cellWidth + (cols > 1 ? hgap : 0),
				insets.top + insets.bottom + rows * cellHeight + Math.max(0, rows - 1) * vgap);
		}
	}

	@Override
	public Dimension minimumLayoutSize(Container parent)
	{
		return preferredLayoutSize(parent);
	}

	@Override
	public void layoutContainer(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			final Insets insets = parent.getInsets();
			final int count = parent.getComponentCount();
			if (count == 0)
			{
				return;
			}
			final int width = parent.getWidth() - insets.left - insets.right;
			final int rows = (count + 1) / 2;
			final int rowHeight = (parent.getHeight() - insets.top - insets.bottom
				- Math.max(0, rows - 1) * vgap) / rows;
			final int leftWidth = (width - hgap) / 2;
			final int rightWidth = width - hgap - leftWidth;
			for (int i = 0; i < count; i++)
			{
				final int y = insets.top + (i / 2) * (rowHeight + vgap);
				if (i == count - 1 && i % 2 == 0)
				{
					// A sole child in the last row owns the whole row, rails included.
					parent.getComponent(i).setBounds(insets.left, y, width, rowHeight);
				}
				else if (i % 2 == 0)
				{
					parent.getComponent(i).setBounds(insets.left, y, leftWidth, rowHeight);
				}
				else
				{
					parent.getComponent(i).setBounds(insets.left + leftWidth + hgap, y, rightWidth, rowHeight);
				}
			}
		}
	}
}
