package com.xerragnaroek.bot.util;

public class BotException extends Exception {

	public static final int SCHEDULE = 0;
	public static final int ALH = 1;
	public static final int ARH = 2;
	public static final int ALRH = 3;
	public static final int UTIL = 4;
	private int type;
	private String guildId;

	public BotException(String msg, String gId, int type) {
		super(msg);
		this.type = type;
		guildId = gId;
	}

	public int getType() {
		return type;
	}

	public String getGuildId() {
		return guildId;
	}

	public BotException updateType(int type) {
		this.type = type;
		return this;
	}
}
