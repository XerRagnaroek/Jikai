package com.xerragnaroek.bot.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.data.GuildData;
import com.xerragnaroek.bot.data.GuildDataManager;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EnableCommandsCommand implements Command {
	private final Logger log = LoggerFactory.getLogger(EnableCommandsCommand.class);

	@Override
	public String getName() {
		return "enable_commands";
	}

	@Override
	public String getAlternativeName() {
		return "enable_coms";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		log.debug("Executing EnableCommandsCommand on guild {}#{}", g.getName(), g.getId());
		GuildData gd = GuildDataManager.getDataForGuild(g);
		if (!gd.hasExplicitCommandSetting()) {
			gd.setCommandsEnabled(true);
			String id = gd.getInfoChannelId();
			TextChannel tc = null;
			if (id != null) {
				tc = g.getTextChannelById(id);
			}
			tc = (tc == null) ? event.getTextChannel() : tc;
			tc.sendMessage("Commands have been enabled. Call ```" + gd.getTrigger()
					+ "disable_commands``` to disable them.").queue();
		}
	}

	@Override
	public String getIdentifier() {
		return "ecc";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "Enables this bot's commands.";
	}

}
