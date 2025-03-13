package com.CollectionLogUploader;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("CollectionLogUploader")
public interface CollectionLogUploaderConfig extends Config {
	@ConfigItem(keyName = "uploadURL", name = "Upload URL", description = "The destination URL where the collection log will be uploaded to.")
	String webhook();

}
