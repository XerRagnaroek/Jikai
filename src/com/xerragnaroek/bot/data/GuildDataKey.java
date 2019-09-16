package com.xerragnaroek.bot.data;

public enum GuildDataKey {
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
	 * The TextChannel the list messages were posted in.
	 */
	LIST_MESSAGES_TC,
	/**
	 * The list messages' ids.
	 */
	LIST_MESSAGES,
	/**
	 * The AnimeBase's version when the list was posted.
	 */
	LIST_MESSAGES_AB_VERSION,
	/**
	 * [title:roleId]
	 */
	ANIME_ROLES,
	/**
	 * The hash of the most up to date season search.
	 */
	CURRENT_SEASON_HASH(true),

	/**
	 * The current version of the anime database.
	 */
	ANIME_BASE_VERSION(true);

	//only options affecting the bot have this set;
	private boolean bot = false;

	GuildDataKey() {}

	GuildDataKey(boolean bot) {
		this.bot = bot;
	}

	public boolean isBotOnly() {
		return bot;
	}
}
