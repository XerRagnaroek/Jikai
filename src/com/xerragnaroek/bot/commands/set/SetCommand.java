package com.xerragnaroek.bot.commands.set;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.commands.Command;
import com.xerragnaroek.bot.commands.CommandHandlerImpl;
import com.xerragnaroek.bot.commands.CommandHandlerManager;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * The "set" command. Handles executing whatever set commands there are.
 * 
 * @author XerRagnarök
 *
 */
public class SetCommand implements Command {

	private Set<Command> setComs = new TreeSet<>();
	private final Logger log = LoggerFactory.getLogger(SetCommand.class);

	public SetCommand() {
		initCommands();
		log.info("SetCommand initialized");
	}

	@Override
	public String getName() {
		return "set";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		if (arguments.length > 1) {
			String com = arguments[0].toLowerCase();
			Command c = CommandHandlerManager.findCommand(setComs, com);
			if (c != null) {
				log.info("Recognized SetCommand '{}'", c.getName());
				arguments = (String[]) ArrayUtils.subarray(arguments, 1, arguments.length);
				if (arguments.length >= 1) {
					c.executeCommand(chi, event, arguments);
				} else {
					log.debug("Missing argument for {}" + com);
				}
			}
		}
	}

	private void initCommands() {
		Command[] commands =
				new Command[] {	new SetTriggerCommand(), new SetAnimeChannelCommand(), new SetTimeZoneCommand(),
								new SetListChannelCommand(), new SetInfoChannelCommand() };
		setComs.addAll(Arrays.asList(commands));
		log.info("Loaded {} SetCommands", setComs.size());
	}

	@Override
	public String getIdentifier() {
		return "sec";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "**set** <option>\n"
				+ setComs.stream().map(com -> "**" + com.getName() + "** <value>" + ":" + com.getDescription())
						.collect(Collectors.joining("\n"));
	}
}
