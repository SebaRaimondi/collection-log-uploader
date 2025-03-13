package com.CollectionLogUploader;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CollectionLogUploaderPluginTest {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		ExternalPluginManager.loadBuiltin(CollectionLogUploaderPlugin.class);
		RuneLite.main(args);
	}
}