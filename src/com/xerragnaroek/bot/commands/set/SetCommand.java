package com.xerragnaroek.bot.commands.set;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.commands.Command;
import com.xerragnaroek.bot.commands.CommandHandlerImpl;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * The "set" command. Handles executing whatever set commands there are.
 * 
 * @author XerRagnarök
 *
 */
public class SetCommand implements Command {

	private Map<String, Command> setComs = new HashMap<>();
	private final Logger log = LoggerFactory.getLogger(SetCommand.class);

	public SetCommand() {
		initCommands();
		log.info("SetCommand initialized");
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
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		if (arguments.length > 1) {
			Command c;
			String com = arguments[0].toLowerCase();
			if ((c = setComs.get(com)) != null) {
				log.info("Recognized SetCommand '{}'", c.getCommandName());
				c.executeCommand(chi, event, Arrays.copyOfRange(arguments, 1, arguments.length));
			}
		}
	}

	private void initCommands() {
		Command[] commands = new Command[] { new SetTriggerCommand(), new SetAnimeChannelCommand(), new SetTimeZoneCommand(), new SetListChannelCommand() };
		for (Command c : commands) {
			setComs.put(c.getCommandName(), c);
			log.debug("Loaded SetCommand " + c.getCommandName());
		}
	}

}
