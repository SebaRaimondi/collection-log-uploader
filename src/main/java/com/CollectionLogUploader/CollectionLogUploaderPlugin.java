package com.CollectionLogUploader;

import com.google.gson.JsonObject;
import com.google.inject.Provides;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.Files;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import static net.runelite.client.RuneLite.RUNELITE_DIR;

@Slf4j
@PluginDescriptor(name = "CollectionLogUploader")
public class CollectionLogUploaderPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private CollectionLogUploaderConfig config;

	File COLLECTION_LOG_DIR = new File(RUNELITE_DIR, "collectionlog");
	File COLLECTION_LOG_SAVE_DATA_DIR = new File(this.COLLECTION_LOG_DIR, "data");

	@Provides
	CollectionLogUploaderConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(CollectionLogUploaderConfig.class);
	}

	@Override
	protected void startUp() {
		log.info("CollectionLogUploader started!");
	}

	private String getFilePath(String fileName, String playerName) {
		String filePath = fileName + "-" + playerName + ".json";
		File directory = new File(this.COLLECTION_LOG_SAVE_DATA_DIR + File.separator +
				playerName);
		return directory + File.separator + filePath;

	}

	private String getCollectionLogFilePathForPlayer(Player player) {
		return this.getFilePath("collectionlog", player.getName());
	}

	private String getConfigFilePathForPlayer(Player player) {
		return this.getFilePath("settings", player.getName());
	}

	private String getCollectionLogJSON(Player player) throws IOException {
		return Files.readString(new File(this.getCollectionLogFilePathForPlayer(player)).toPath());
	}

	private String getUserDataJSON(Player player) {
		String accountHash = String.valueOf(client.getAccountHash());
		String username = player.getName();
		boolean isFemale = player.getPlayerComposition().getGender() == 1;

		return String.format("{\"accountHash\": \"%s\", \"username\": \"%s\", \"isFemale\": %s}",
				accountHash, username, isFemale);
	}

	private void uploadCollectionLog(Player player) throws IOException {
		log.info("Uploading collection log");

		String URL = config.webhook();
		log.info("Webhook URL: {}", URL);

		HttpClient client = HttpClient.newHttpClient();

		String userDataJSON = this.getUserDataJSON(player);
		String configJSON = this.getConfigFilePathForPlayer(player);
		String collectionLogJSON = this.getCollectionLogJSON(player);

		JsonObject JSON = new JsonObject();
		JSON.addProperty("userData", userDataJSON);
		JSON.addProperty("config", configJSON);
		JSON.addProperty("collectionLog", collectionLogJSON);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(URL)) // Replace with your API endpoint
				.header("Content-Type", "application/json") // Set header
				.POST(BodyPublishers.ofString(JSON.toString())) // Attach JSON payload
				.build();

		CompletableFuture<HttpResponse<String>> futureResponse = client.sendAsync(request,
				HttpResponse.BodyHandlers.ofString());

		// Handle response asynchronously
		futureResponse.thenAccept(response -> {
			log.info("Response Code: {}", response.statusCode()); // Log status code
			log.info("Response Body: {}", response.body()); // Log response body
		}).exceptionally(e -> {
			log.info("Request failed: {}", e.getMessage()); // Log error message
			return null;
		});

		// Prevents the program from exiting immediately
		futureResponse.join();

	}

	@Override
	protected void shutDown() {
		log.info("CollectionLogUploader stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		GameState state = gameStateChanged.getGameState();

		if (state != GameState.LOGIN_SCREEN)
			return;

		Player player = client.getLocalPlayer();
		if (player == null)
			return;

		log.info("GameState {} reached. Uploading {}'s collection log", state, player.getName());

		try {
			this.uploadCollectionLog(player);
			log.info("Collection log uploaded successfully");
		} catch (IOException e) {
			log.info("Failed to upload collection log, IOException: {}", e.getMessage());
		}
	}

}
