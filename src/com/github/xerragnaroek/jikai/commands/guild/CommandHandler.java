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
package com.github.xerragnaroek.jikai.commands.guild;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.commands.guild.set.SetCommand;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;
import com.github.xerragnaroek.jikai.util.Destroyable;
import com.github.xerragnaroek.jikai.util.Initilizable;
import com.github.xerragnaroek.jikai.util.prop.Property;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Handles commands (who'd have thunk?).
 * 
 * @author XerRagnarök
 *
 */
public class CommandHandler implements Initilizable, Destroyable {
	private static Set<GuildCommand> commands = new TreeSet<>();
	private final Logger log;
	private Property<String> trigger = new Property<>();
	private Property<Boolean> comsEnabled = new Property<>(true);
	private AtomicBoolean initialized = new AtomicBoolean(false);
	private Jikai j;
	public static Permission[] MOD_PERMS = new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS };

	static {
		GuildCommand[] coms = new GuildCommand[] { new ForceUserUpdateCommand(), new ForceSaveCommand(), new ForceRegisterCommand(), new StopCommand(), new StatusCommand(), new PingCommand(), new SetCommand(), new ScheduleCommand(), new DebugCommand(), new AnimeListCommand(), new HelpCommand(), new EnableCommandsCommand(), new DisableCommandsCommand(), new RequestAssistanceCommand() };
		commands.addAll(Arrays.asList(coms));
	}

	public CommandHandler(long g, Jikai j) {
		this.j = j;
		log = LoggerFactory.getLogger(CommandHandler.class.getName() + "#" + g);
		init();
	}

	public void init() {
		JikaiData jd = j.getJikaiData();
		jd.triggerProperty().bindAndSet(trigger);
		jd.comsEnabledProperty().bind(comsEnabled);
		if (jd.hasExplicitCommandSetting()) {
			comsEnabled.set(jd.areCommandsEnabled());
		}
		initialized.set(true);
		log.info("Initialized");
	}

	public void handleMessage(MessageReceivedEvent event) {
		//ignore bots
		if (!event.getAuthor().isBot()) {
			String content = event.getMessage().getContentRaw();
			String trigger = this.trigger.get();
			//is it supposed to be a command?
			if (content.startsWith(trigger)) {
				log.debug("Received message: {}", content);
				//remove trigger from content
				content = content.substring(trigger.length());
				//any recognised commands?
				String[] tmp = content.trim().split(" ");
				GuildCommand com = ComUtils.findCommand(commands, tmp[0]);
				if (com != null) {
					log.info("Recognised command {}", com.getName());
					//remove the command from the content and execute it
					tmp = (String[]) ArrayUtils.subarray(tmp, 1, tmp.length);
					if (ComUtils.checkPermissions(com, event.getMember())) {
						if (comsEnabled.get() || com.isAlwaysEnabled()) {
							j.getJikaiData().incrementAndGetExecComs();
							com.executeCommand(event, tmp);
						}
					}
				} else {
					log.debug("No commands recognised");
					//trash talk the user cause they typed some garbage
					//event.getChannel().sendMessageFormat("**Bruh** :triumph: %s, what the **fuck** is that command supposed to mean? :angry: Smh my damn head. :clown:", event.getAuthor().getAsMention()).queue();
				}
			}
		}
	}

	public String getTrigger() {
		return trigger.get();
	}

	@Override
	public boolean isInitialized() {
		return initialized.get();
	}

	@Override
	public void destroy() {
		trigger.destroy();
		comsEnabled.destroy();
	}

	public static Set<GuildCommand> getCommands() {
		return commands;
	}
}
