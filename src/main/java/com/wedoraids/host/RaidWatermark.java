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

import com.wedoraids.WeDoRaidsPlugin;
import com.wedoraids.feed.RaidType;
import com.wedoraids.ui.WdrTheme;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ImageUtil;

/**
 * The raid's artwork, bled into the top band of the draft card behind the headline.
 *
 * <p>Optional by construction: each raid reads {@code raid-<name>.jpg} from
 * {@code src/main/resources/com/wedoraids/} and paints nothing when absent, so art can be added or
 * removed as files without touching code. Wide crops with their subject on the right read best, since
 * the band scales to cover and anchors right.
 */
@Slf4j
final class RaidWatermark
{
	/**
	 * Ceiling on how strongly the art mixes into the card, set by the ink above it rather than by taste.
	 * At this value the shipped crops read as atmosphere while the world number stays above 7:1; higher
	 * and the headline's contrast starts to depend on which image is loaded.
	 */
	private static final float MAX_ALPHA = 0.18f;

	/** Where the mask finishes clearing the band. The raid name sits left of this, on bare card. */
	private static final float FADE_END = 0.55f;

	private static final Map<RaidType, BufferedImage> CACHE = new EnumMap<>(RaidType.class);
	private static final Map<RaidType, Boolean> LOOKED_UP = new EnumMap<>(RaidType.class);

	private RaidWatermark()
	{
	}

	/** Resource name for a raid, e.g. {@code raid-tob.jpg}. */
	private static String resourceName(RaidType raid)
	{
		return "raid-" + raid.getDisplayName().toLowerCase(java.util.Locale.ROOT) + ".jpg";
	}

	/** Art for a raid, or null when none ships. Looked up once and remembered, misses included. */
	private static BufferedImage image(RaidType raid)
	{
		if (Boolean.TRUE.equals(LOOKED_UP.get(raid)))
		{
			return CACHE.get(raid);
		}
		LOOKED_UP.put(raid, Boolean.TRUE);
		try
		{
			CACHE.put(raid, ImageUtil.loadImageResource(WeDoRaidsPlugin.class, resourceName(raid)));
		}
		catch (RuntimeException exception)
		{
			log.debug("We Do Raids: no watermark art for {}, painting none", raid.getDisplayName(), exception);
			CACHE.put(raid, null);
		}
		return CACHE.get(raid);
	}

	/** Paints the band, if there is art for this raid. Leaves {@code graphics} as found. */
	static void paint(Graphics2D graphics, RaidType raid, int width, int height)
	{
		final BufferedImage art = image(raid);
		if (art == null || width <= 0 || height <= 0 || art.getWidth() <= 0 || art.getHeight() <= 0)
		{
			return;
		}
		final Graphics2D scoped = (Graphics2D) graphics.create();
		scoped.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		scoped.clipRect(0, 0, width, height);
		// Cover rather than fit by height: art narrower than the card would start to the right of where the
		// mask ends, giving a hard seam instead of a fade.
		final double scale = Math.max(
			(double) width / art.getWidth(),
			(double) height / art.getHeight());
		final int drawWidth = Math.max(1, (int) Math.ceil(art.getWidth() * scale));
		final int drawHeight = Math.max(1, (int) Math.ceil(art.getHeight() * scale));
		scoped.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, MAX_ALPHA));
		scoped.drawImage(art, width - drawWidth, (height - drawHeight) / 2, drawWidth, drawHeight, null);
		// Mask the left back to card colour. Exact here because the band has nothing behind it but the
		// card's own fill, and it costs one gradient instead of an offscreen DstIn pass.
		scoped.setComposite(AlphaComposite.SrcOver);
		final Color opaque = WdrTheme.CARD;
		final Color clear = new Color(opaque.getRed(), opaque.getGreen(), opaque.getBlue(), 0);
		scoped.setPaint(new LinearGradientPaint(
			0f, 0f, Math.max(1f, width * FADE_END), 0f,
			new float[]{0f, 1f},
			new Color[]{opaque, clear},
			MultipleGradientPaint.CycleMethod.NO_CYCLE));
		scoped.fillRect(0, 0, (int) Math.ceil(width * FADE_END), height);
		scoped.dispose();
	}
}
