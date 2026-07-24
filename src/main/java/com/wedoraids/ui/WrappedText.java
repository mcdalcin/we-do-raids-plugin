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

import net.runelite.client.ui.PluginPanel;

/**
 * The panel's text measures, in one place.
 *
 * <p>Swing has no line-wrapping label, so wrapped copy is rendered as HTML with an explicit pixel
 * width. Those widths were previously hand-written per call site, which meant the panel had four
 * different line lengths for the same kind of text.
 *
 * <p>The values are measured against the rendered panel rather than derived from
 * {@link PluginPanel#PANEL_WIDTH}. Swing's HTML renderer adds its own body margin, so a label
 * declaring {@code width:177px} reports a preferred width of 230. When that preferred width
 * exceeds the width the layout actually grants, the label's preferred <em>height</em> is computed
 * for the wider measure, the text wraps to an extra line at render time, and that line is clipped.
 * Keep these conservative, and re-check with a render if the panel's padding changes.
 */
public final class WrappedText
{
	/** Line length for copy inside a card. */
	public static final int CARD = 150;
	/** Line length for copy sitting directly on the panel, such as form status lines. */
	public static final int PANEL = 178;

	private WrappedText()
	{
	}

	/** Escaped, wrapped at the card measure. */
	public static String html(String text)
	{
		return html(text, CARD);
	}

	/** Escaped, wrapped at an explicit measure. */
	public static String html(String text, int width)
	{
		return "<html><body style='width:" + width + "px'>" + HtmlEscape.escape(text) + "</body></html>";
	}

	/** Escaped, wrapped and centred, for empty-state and notice copy. */
	public static String centered(String text, int width)
	{
		return "<html><div style='text-align:center;width:" + width + "px'>"
			+ HtmlEscape.escape(text) + "</div></html>";
	}
}
