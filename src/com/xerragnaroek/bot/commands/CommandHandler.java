package com.xerragnaroek.bot.commands;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.commands.set.SetCommand;
import com.xerragnaroek.bot.config.ConfigManager;
import com.xerragnaroek.bot.config.ConfigOption;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Handles commands (who'd have thunk?). Singleton.
 * 
 * @author XerRagnarök
 *
 */
public class CommandHandler extends ListenerAdapter {
	private static CommandHandler inst;
	private Map<String, Command> commands = new HashMap<>();
	private final Logger log = LoggerFactory.getLogger(CommandHandler.class);

	private CommandHandler() {
		//trigger[0] = Config.getOption(ConfigOption.TRIGGER);
		//Config.registerOnOptionChange(ConfigOption.TRIGGER, str -> trigger[0] = str);
		initCommands();
		log.info("Initialized CommandHandler");
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		String trigger = ConfigManager.getConfigForGuild(event.getGuild().getId()).getOption(ConfigOption.TRIGGER);
		//ignore bots
		if (!event.getAuthor().isBot()) {
			String content = event.getMessage().getContentRaw();
			//is it supposed to be a command?
			if (content.startsWith(trigger)) {
				log.debug("Received message: {}", content);
				//remove trigger from content
				content = content.substring(trigger.length());

				//help command
				if (content.equals("help")) {
					helpCommand(event);
					return;
				}
				//any recognised commands?
				Command com = checkForCommand(content);
				if (com != null) {
					log.info("Recognised command {}", com.getCommandName());
					//remove the command from the content and execute it
					content = content.substring(com.getCommandName().length()).trim();
					com.executeCommand(event, content);
				} else {
					log.debug("No commands recognised");
					//trash talk the user cause they typed some garbage
					//event.getChannel().sendMessageFormat("**Bruh** :triumph: %s, what the **fuck** is that command supposed to mean? :angry: Smh my damn head. :clown:", event.getAuthor().getAsMention()).queue();
				}
			}
		}
	}

	/**
	 * Get the instance of the Handler. There's only one cause this is a singleton class.
	 * 
	 * @return the instance of the CommandHandler
	 */
	public static CommandHandler getInstance() {
		init();
		return inst;
	}

	/**
	 * Initialise the CommandHandler. When not calling this method it lazily loads instead.
	 */
	public static void init() {
		if (inst == null) {
			inst = new CommandHandler();
		}
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

	/**
	 * Load the commands
	 */
	private void initCommands() {
		Command[] commands = new Command[] { new PingCommand(), new SetCommand(), new ScheduleCommand(), new EmbedTestCommand(), new DebugCommand(), new AnimeListCommand() };
		for (Command c : commands) {
			this.commands.put(c.getCommandName(), c);
			log.debug("Loaded command " + c.getCommandName());
		}
	}

	/**
	 * Lists all known commands. Has to be in this class because I don't want to expose the commands
	 * map.
	 * 
	 * @param event
	 *            - needed to reply
	 */
	private void helpCommand(MessageReceivedEvent event) {
		String trigger = ConfigManager.getConfigForGuild(event.getGuild().getId()).getOption(ConfigOption.TRIGGER);
		log.info("Executing help command");
		StringBuilder bob = new StringBuilder();
		for (Command com : commands.values()) {
			bob.append(trigger + com.getUsage() + "\n");
		}
		MessageBuilder mb = new MessageBuilder();
		mb.appendCodeBlock("My commands are:\n" + bob.toString(), "css");
		mb.sendTo(event.getChannel()).queue();
	}
}
