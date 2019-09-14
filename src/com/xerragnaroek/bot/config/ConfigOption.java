package com.xerragnaroek.bot.config;

public enum ConfigOption {
	/**
	 * Whatever string or char triggers commands.
	 */
	TRIGGER,
	/**
	 * The channel the bot posts automated stuff in.
	 */
	ANIME_CHANNEL,

	/**
	 * The channel the bot posts the anime list in.
	 */
	ROLE_CHANNEL,

	/**
	 * The server's timezone.
	 */
	TIMEZONE,

	/**
	 * The list messages' ids.
	 */
	LIST_MESSAGES,
	/**
	 * The hash of the latest season search.
	 */
	LATEST_SEASON_HASH(true);

	//only options affecting the bot have this set;
	private boolean bot = false;

	ConfigOption() {}

	ConfigOption(boolean bot) {
		this.bot = bot;
	}

	public boolean isBotOnly() {
		return bot;
	}
}
