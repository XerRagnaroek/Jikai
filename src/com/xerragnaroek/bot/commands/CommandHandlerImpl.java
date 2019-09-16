package com.xerragnaroek.bot.commands;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.data.GuildData;
import com.xerragnaroek.bot.data.GuildDataManager;
import com.xerragnaroek.bot.data.GuildDataKey;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Handles commands (who'd have thunk?). Singleton.
 * 
 * @author XerRagnarök
 *
 */
public class CommandHandlerImpl {
	private Map<String, Command> commands = new HashMap<>();
	private final Logger log;
	private final String gId;
	private String trigger;

	CommandHandlerImpl(String g) {
		gId = g;
		log = LoggerFactory.getLogger(CommandHandlerImpl.class.getName() + "#" + gId);
		init();
	}

	private void init() {
		GuildData c = GuildDataManager.getDataForGuild(gId);
		setTrigger(c.get(GuildDataKey.TRIGGER));
		c.registerDataChangedConsumer(GuildDataKey.TRIGGER, this::setTrigger);
		commands = CommandHandlerManager.getCommands();
		log.info("Initialized");
	}

	public void handleMessage(MessageReceivedEvent event) {
		//ignore bots
		if (!event.getAuthor().isBot()) {
			String content = event.getMessage().getContentRaw();
			//is it supposed to be a command?
			if (content.startsWith(trigger)) {
				log.debug("Received message: {}", content);
				//remove trigger from content
				content = content.substring(trigger.length());
				//any recognised commands?
				Command com = checkForCommand(content);
				if (com != null) {
					log.info("Recognised command {}", com.getCommandName());
					//remove the command from the content and execute it
					content = content.substring(com.getCommandName().length()).trim();
					com.executeCommand(this, event, content);
				} else {
					log.debug("No commands recognised");
					//trash talk the user cause they typed some garbage
					//event.getChannel().sendMessageFormat("**Bruh** :triumph: %s, what the **fuck** is that command supposed to mean? :angry: Smh my damn head. :clown:", event.getAuthor().getAsMention()).queue();
				}
			}
		}
	}

	private void setTrigger(String trigger) {
		this.trigger = trigger;
		log.debug("Trigger was set to {}", trigger);
	}

	public String getTrigger() {
		return trigger;
	}

	/**
	 * Check if content starts with a known command.
	 * 
	 * @param content
	 *            - the supposed command message
	 * @return a {@link Command} or null if none was recognised
	 */
	private Command checkForCommand(String content) {
		for (String com : commands.keySet()) {
			if (content.toLowerCase().startsWith(com)) {
				return commands.get(com);
			}
		}
		return null;
	}

}
