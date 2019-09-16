package com.xerragnaroek.bot.commands.set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.commands.Command;
import com.xerragnaroek.bot.commands.CommandHandlerImpl;
import com.xerragnaroek.bot.data.GuildDataManager;
import com.xerragnaroek.bot.data.GuildDataKey;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.internal.utils.PermissionUtil;

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
	public String getCommandName() {
		return "trigger";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String content) {
		//only high ranking lads can use this command
		if (PermissionUtil.checkPermission(event.getMember(), Permission.MANAGE_ROLES)) {
			//pad it with a whitespace at the end so words can be used better: foo bar instead of foobar
			if (content.length() > 1) {
				content += " ";
			}
			if (content.length() >= 1) {
				GuildDataManager.getDataForGuild(event.getGuild().getId()).set(GuildDataKey.TRIGGER, content);
				event.getChannel().sendMessageFormat("Trigger was changed to \"%s\"", content).queue();
			}
		} else {
			//obligatory trash talk
			event.getChannel().sendMessageFormat("Who the **fuck** do you think you are, %s? Permission **DENIED**", event.getAuthor().getAsMention()).queue();
			log.info("User {} has insufficient permissions", event.getAuthor());
		}

	}

	@Override
	public String getUsage() {
		return getCommandName() + " <new trigger>";
	}

}
