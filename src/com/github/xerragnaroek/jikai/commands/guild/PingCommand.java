
package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class PingCommand implements GuildCommand {

	@Override
	public String getName() {
		return "ping";
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] content) {
		MessageChannel channel = event.getChannel();
		long time = System.currentTimeMillis();
		channel.sendMessage("Pong") /* => RestAction<Message> */
				.queue(response /* => Message */ -> {
					response.editMessageFormat("Pong: %d ms", System.currentTimeMillis() - time).queue(msg -> msg.addReaction("U+1F44C").queue());
				});
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandler.MOD_PERMS;
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "Shows the latency of the bot.";
	}

}
