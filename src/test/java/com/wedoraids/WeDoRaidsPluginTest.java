package com.wedoraids;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WeDoRaidsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WeDoRaidsPlugin.class);
		RuneLite.main(args);
	}
}
