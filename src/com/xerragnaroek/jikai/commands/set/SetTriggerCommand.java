package com.xerragnaroek.jikai.commands.set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.commands.Command;
import com.xerragnaroek.jikai.commands.CommandHandler;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Command that changes the trigger String.
 * 
 * @author XerRagnarök
 *
 */
public class SetTriggerCommand implements Command {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	SetTriggerCommand() {}

	@Override
	public String getName() {
		return "trigger";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		String content = arguments[0];
		//only high ranking lads can use this command
		//pad it with a whitespace at the end so words can be used better: foo bar instead of foobar
		if (content.length() > 1) {
			content += " ";
		}
		if (content.length() >= 1) {
			Jikai j = Core.JM.get(event.getGuild());
			j.getJikaiData().setTrigger(content);
			try {
				j.getInfoChannel().sendMessageFormat("Trigger was changed to \"%s\"", content).queue();
			} catch (Exception e) {}
		}

	}

	@Override
	public String getUsage() {
		return getName() + " <new trigger>";
	}

	@Override
	public String getIdentifier() {
		return "stcc";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "The string of characters that will trigger a command. Default is \"!\". Can be as long as you want.";
	}
}
