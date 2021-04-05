package com.github.xerragnaroek.jikai.commands.guild.set;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

/**
 * The "set" command. Handles executing whatever set commands there are.
 * 
 * @author XerRagnarök
 */
public class SetCommand implements GuildCommand {

	protected Set<GuildCommand> setComs = new TreeSet<>();
	protected Logger log;

	public SetCommand() {
		init();
		log.info("SetCommand initialized");
	}

	@Override
	public String getName() {
		return "set";
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		if (arguments.length >= 1) {
			String com = arguments[0].toLowerCase();
			GuildCommand c = ComUtils.findCommand(setComs, com);
			if (c != null) {
				log.info("Recognized SetCommand '{}'", c.getName());
				arguments = (String[]) ArrayUtils.subarray(arguments, 1, arguments.length);
				c.executeCommand(event, arguments);
			}
		}
	}

	private void init() {
		log = LoggerFactory.getLogger(this.getClass());
		GuildCommand[] commands = new GuildCommand[] { new SetLanguageCommand(), new SetCommandChannelCommand(), new SetScheduleChannelCommand(), new SetPrefixCommand(), new SetAnimeChannelCommand(), new SetTimeZoneCommand(), new SetListChannelCommand(), new SetInfoChannelCommand() };
		setComs.addAll(Arrays.asList(commands));
		log.info("Loaded {} SetCommands", setComs.size());
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getStringFormatted("com_g_set_desc", Arrays.asList("coms"), setComs.stream().map(com -> "**" + com.getName() + "** <value>" + ":\n" + com.getDescription(loc)).collect(Collectors.joining("\n")));
	}

	@Override
	public String getLocaleKey() {
		return "com_g_set";
	}
}
