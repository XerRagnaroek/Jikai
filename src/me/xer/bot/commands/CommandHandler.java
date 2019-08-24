package me.xer.bot.commands;

import java.util.HashMap;
import java.util.Map;

import com.github.xerragnaroek.xlog.XLogger;

import me.xer.bot.config.Config;
import me.xer.bot.config.ConfigOption;
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
	private String[] trigger = new String[1];
	private Map<String, Command> commands = new HashMap<>();
	private static final XLogger log = XLogger.getInstance();

	private CommandHandler() {
		trigger[0] = Config.getOption(ConfigOption.TRIGGER);
		Config.registerOnOptionChange(ConfigOption.TRIGGER, str -> trigger[0] = str);
		initCommands();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		//ignore bots
		if (!event.getAuthor().isBot()) {
			String content = event.getMessage().getContentRaw();
			//is it supposed to be a command?
			if (content.startsWith(trigger[0])) {
				log.logf("Received message: %s", content);
				//remove trigger from content
				content = content.substring(trigger[0].length());

				//help command
				if (content.equals("help")) {
					helpCommand(event);
					return;
				}
				//any recognised commands?
				Command com = checkForCommand(content);
				if (com != null) {
					log.logf("Recognised command %s", com.getCommandName());
					//remove the command from the content and execute it
					content = content.substring(com.getCommandName().length()).trim();
					com.executeCommand(event, content);
				} else {
					log.log("No commands recognised, commencing trashtalk");
					//trash talk the user cause they typed some garbage
					event.getChannel().sendMessageFormat(	"**Bruh** :triumph: %s, what the **fuck** is that command supposed to mean? :angry: Smh my damn head. :clown:",
															event.getAuthor().getAsMention()).queue();
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
			log.log("Initialized CommandHandler");
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
		Command[] commands = new Command[] {	new PingCommand(), new SetCommand(), new ScheduleCommand(),
												new EmbedTestCommand() };
		for (Command c : commands) {
			this.commands.put(c.getCommandName(), c);
			log.log("Loaded command " + c.getCommandName());
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
		log.log("Executing help command");
		StringBuilder bob = new StringBuilder();
		for (Command com : commands.values()) {
			bob.append(trigger[0] + com.getUsage() + "\n");
		}
		MessageBuilder mb = new MessageBuilder();
		mb.appendCodeBlock("My commands are:\n" + bob.toString(), "css");
		mb.sendTo(event.getChannel()).queue();
	}
}
