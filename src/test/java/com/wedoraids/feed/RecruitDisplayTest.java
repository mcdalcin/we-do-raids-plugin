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
package com.wedoraids.feed;

import java.time.Instant;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecruitDisplayTest
{
	@Test
	public void firstInt_returnsZero_whenValueExceedsIntegerRange()
	{
		assertEquals(0, RecruitDisplay.firstInt("2147483648"));
	}

	@Test
	public void firstInt_returnsZero_whenDigitRunIsMuchLongerThanIntegerRange()
	{
		assertEquals(0, RecruitDisplay.firstInt("999999999999999999999999999999999999999999999999999"));
	}

	@Test
	public void firstInt_returnsFirstEmbeddedInteger_whenValueFits()
	{
		assertEquals(350, RecruitDisplay.firstInt("ToA 350 invo"));
	}

	@Test
	public void teamTotal_returnsNamedAndNumericPartySizes()
	{
		assertEquals(3, RecruitDisplay.teamTotal("trio"));
		assertEquals(5, RecruitDisplay.teamTotal("5 man"));
	}

	@Test
	public void firstInt_returnsUnicodeDecimalNumber_whenTextUsesNonAsciiDigits()
	{
		assertEquals(3, RecruitDisplay.firstInt("team \u0663"));
	}

	@Test
	public void wdrTierNumber_returnsHighestApplicableTier()
	{
		assertEquals("T5", RecruitDisplay.wdrTierNumber(RaidType.COX, 500));
		assertEquals("T1", RecruitDisplay.wdrTierNumber(RaidType.TOB, 1));
	}

	@Test
	public void messageIsRedundant_whenMessageOnlyRestatesTheParsedFields()
	{
		assertTrue(RecruitDisplay.messageIsRedundant(
			entry("trio w447 mdps/rdps +2 phub olm", "+2", "mdps, rdps", "trio", 447, "olm")));
	}

	@Test
	public void messageIsRedundant_whenMessageIsTheWorldAndSpotsRunTogether()
	{
		assertTrue(RecruitDisplay.messageIsRedundant(entry("507+1 range", "+1", "range", null, 507, null)));
	}

	@Test
	public void messageIsNotRedundant_whenMessageAddsAnythingNew()
	{
		assertFalse(RecruitDisplay.messageIsRedundant(
			entry("trio w447 mdps/rdps pogstack", "+2", "mdps, rdps", "trio", 447, null)));
	}

	@Test
	public void messageIsRedundant_whenMessageIsMissing()
	{
		assertTrue(RecruitDisplay.messageIsRedundant(null));
	}

	private static RecruitEntry entry(String message, String spots, String roles, String partySize,
		int world, String host)
	{
		return new RecruitEntry("Sender", "WDR", RaidType.TOB, "Standard", null, spots, roles,
			partySize, world, null, host, 0, "RAID", message, Instant.now());
	}
}
