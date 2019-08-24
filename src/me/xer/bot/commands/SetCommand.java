package me.xer.bot.commands;

import java.util.HashMap;
import java.util.Map;

import com.github.xerragnaroek.xlog.XLogger;

import me.xer.bot.commands.set.SetChannelCommand;
import me.xer.bot.commands.set.SetTriggerCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * The "set" command. Handles executing whatever set commands there are.
 * 
 * @author XerRagnarök
 *
 */
public class SetCommand implements Command {

	private Map<String, Command> setComs = new HashMap<>();
	private final XLogger log = XLogger.getInstance();

	public SetCommand() {
		initCommands();
		log.log("SetCommand initialized");
	}

	@Override
	public String getCommandName() {
		return "set";
	}

	@Override
	public String getUsage() {
		return String.format("set <%s> <value>", String.join(":", setComs.keySet()));
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String arguments) {
		String[] tmp = arguments.split(" ");
		if (tmp.length > 1) {
			Command c;
			String com = tmp[0].toLowerCase();
			if ((c = setComs.get(com)) != null) {
				log.logf("Recognized SetCommand '%s'", c.getCommandName());
				c.executeCommand(event, arguments.substring(com.length()).trim());
			}
		}
	}

	private void initCommands() {
		Command[] commands = new Command[] { new SetTriggerCommand(), new SetChannelCommand() };
		for (Command c : commands) {
			setComs.put(c.getCommandName(), c);
			log.log("Loaded SetCommand " + c.getCommandName());
		}
	}

}
