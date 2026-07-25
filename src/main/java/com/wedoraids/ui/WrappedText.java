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
 * {@link PluginPanel#PANEL_WIDTH}, because Swing's HTML renderer does not treat the declared body
 * width as a wrap boundary. Left uncapped, a status label is granted the full panel width and the
 * HTML fills it, so the declared width has no effect and the widest line inks to the panel edge:
 * measured, a 178px body still inked to column 214 on the 225px panel (content edge 215), clipping
 * the tail. Reducing the body width alone did nothing (165px still inked to 213). The declared
 * width only takes effect once the label's own maximum width is capped; the consumer must do both.
 *
 * <p>{@link #PANEL} of 150 was measured by rendering a worst-case status in the design gallery: at a
 * 150px body the copy wraps to two lines, and with the status label capped at 190px the widest line
 * inks to column 199, a clear right margin inside the 205px measure. Keep these conservative, and
 * re-check with a render if the panel's padding changes.
 */
public final class WrappedText
{
	/** Line length for copy inside a card. */
	public static final int CARD = 150;
	/** Line length for copy sitting directly on the panel, such as form status lines. */
	public static final int PANEL = 150;

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
