package com.github.xerragnaroek.jikai.commands.dev;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

/**
 * 
 */
public class MaxRequestsCommand implements GuildCommand, JUCommand {

	@Override
	public String getName() {
		return "max_requests";
	}

	@Override
	public String getLocaleKey() {
		return null;
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		handleCommand(ju.getUser().openPrivateChannel().complete(), arguments);
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		handleCommand(event.getChannel(), arguments);
	}

	private void handleCommand(MessageChannel tc, String[] input) {
		if (input.length < 1) {
			tc.sendMessage("Maximum concurrent requests to the api allowed: " + AnimeDB.getJASA().getMaxConcurrentRequests()).queue();
		} else {
			try {
				int maxR = Integer.parseInt(input[0]);
				int oldMax = AnimeDB.getJASA().getMaxConcurrentRequests();
				AnimeDB.getJASA().setMaxConcurrentRequests(maxR);
				tc.sendMessage("Maximum concurrent requests updated from " + oldMax + " to " + maxR).queue();
			} catch (Exception e) {
				tc.sendMessage(e.getMessage()).queue();
			}
		}
	}

}
