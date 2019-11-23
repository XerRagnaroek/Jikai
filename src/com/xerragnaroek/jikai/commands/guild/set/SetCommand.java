/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xerragnaroek.jikai.commands.guild.set;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.commands.ComUtils;
import com.xerragnaroek.jikai.commands.guild.GuildCommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * The "set" command. Handles executing whatever set commands there are.
 * 
 * @author XerRagnarök
 *
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
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		if (arguments.length > 1) {
			String com = arguments[0].toLowerCase();
			GuildCommand c = ComUtils.findCommand(setComs, com);
			if (c != null) {
				log.info("Recognized SetCommand '{}'", c.getName());
				arguments = (String[]) ArrayUtils.subarray(arguments, 1, arguments.length);
				if (arguments.length >= 1) {
					c.executeCommand(event, arguments);
				} else {
					log.debug("Missing argument for {}" + com);
				}
			}
		}
	}

	private void init() {
		log = LoggerFactory.getLogger(this.getClass());
		GuildCommand[] commands = new GuildCommand[] { new SetTriggerCommand(), new SetAnimeChannelCommand(), new SetTimeZoneCommand(), new SetListChannelCommand(), new SetInfoChannelCommand() };
		setComs.addAll(Arrays.asList(commands));
		log.info("Loaded {} SetCommands", setComs.size());
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "**set** <option>\n" + setComs.stream().map(com -> "**" + com.getName() + "** <value>" + ":" + com.getDescription()).collect(Collectors.joining("\n"));
	}
}
