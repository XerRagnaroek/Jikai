
package com.github.xerragnaroek.jikai.commands.guild;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * The "schedule" command
 * 
 * @author XerRagnaroek
 *
 */
public class ScheduleCommand implements GuildCommand {

	@Override
	public String getName() {
		return "schedule";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandler.MOD_PERMS;
	}

	@Override
	public String getDescription() {
		return "Sends a schedule of when the animes air in the week, as per the previously set timezone.";
	}
}
