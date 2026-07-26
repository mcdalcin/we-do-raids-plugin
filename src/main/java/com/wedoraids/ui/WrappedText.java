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


/**
 * HTML line-wrap widths for the panel's text, in one place.
 *
 * <p>Swing has no wrapping label, so wrapped copy is rendered as HTML with an explicit pixel
 * width. Declaring a body width alone is not enough: the HTML renderer ignores it unless the
 * label's own maximum width is also capped. Both must be set together by the consumer.
 *
 * <p>{@link #PANEL} of 150 was measured against a worst-case status string in the design
 * gallery. Re-check with a render if the panel's padding changes.
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
