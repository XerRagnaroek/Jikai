package com.xerragnaroek.bot.commands;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.data.GuildData;
import com.xerragnaroek.bot.data.GuildDataManager;
import com.xerragnaroek.bot.util.Initilizable;
import com.xerragnaroek.bot.util.Property;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Handles commands (who'd have thunk?). Singleton.
 * 
 * @author XerRagnarök
 *
 */
public class CommandHandlerImpl implements Initilizable {
	private Set<Command> commands;
	private final Logger log;
	private final String gId;
	private Property<String> trigger = new Property<>();
	private Property<Boolean> comsEnabled = new Property<>();
	private AtomicBoolean initialized = new AtomicBoolean(false);

	CommandHandlerImpl(String g) {
		gId = g;
		log = LoggerFactory.getLogger(CommandHandlerImpl.class.getName() + "#" + gId);
		init();
	}

	public void init() {
		GuildData gd = GuildDataManager.getDataForGuild(gId);
		gd.triggerProperty().bindAndSet(trigger);
		gd.comsEnabledProperty().bind(comsEnabled);
		if (gd.hasExplicitCommandSetting()) {
			comsEnabled.set(gd.areCommandsEnabled());
		} else {
			comsEnabled.set(CommandHandlerManager.areCommandsEnabledByDefault());
		}
		commands = CommandHandlerManager.getCommands();
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
				Command com = CommandHandlerManager.findCommand(commands, tmp[0]);
				if (com != null) {
					log.info("Recognised command {}", com.getName());
					//remove the command from the content and execute it
					tmp = (String[]) ArrayUtils.subarray(tmp, 1, tmp.length);
					if (CommandHandlerManager.checkPermissions(com, event.getMember())) {
						if (comsEnabled.get()) {
							com.executeCommand(this, event, tmp);
						} else if (com.isCommand("ecc")) {
							com.executeCommand(this, event, tmp);
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

}
