package me.xer.bot.commands.set;

import com.github.xerragnaroek.xlog.XLogger;

import me.xer.bot.commands.Command;
import me.xer.bot.config.Config;
import me.xer.bot.config.ConfigOption;
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
	private static final XLogger log = XLogger.getInstance();

	@Override
	public String getCommandName() {
		return "trigger";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String content) {
		//only high ranking lads can use this command
		if (PermissionUtil.checkPermission(event.getMember(), Permission.MANAGE_ROLES)) {
			//pad it with a whitespace at the end so words can be used better: foo bar instead of foobar
			if (content.length() > 1) {
				content += " ";
			}
			if (content.length() >= 1) {
				Config.setOption(ConfigOption.TRIGGER, content);
				event.getChannel().sendMessageFormat("Trigger was changed to \"%s\"", content).queue();
			}
		} else {
			//obligatory trash talk
			event.getChannel().sendMessageFormat(	"Who the **fuck** do you think you are, %s? Permission **DENIED**",
													event.getAuthor().getAsMention()).queue();
			log.logf("User %s has insufficient permissions", event.getAuthor());
		}

	}

	@Override
	public String getUsage() {
		return getCommandName() + " <new trigger>";
	}

}
