package com.xerragnaroek.bot.responses;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class PremadeResponses {

	public static Message lackingPermissions(User user) {
		MessageBuilder bob = new MessageBuilder();
		bob.append(user).append(" you're lacking the permissions to call this command.");
		return bob.build();
	}
}
