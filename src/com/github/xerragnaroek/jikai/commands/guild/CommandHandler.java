package com.github.xerragnaroek.jikai.commands.guild;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.BugCommand;
import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.commands.HelpCommand;
import com.github.xerragnaroek.jikai.commands.ReloadLocalesCommand;
import com.github.xerragnaroek.jikai.commands.StopCommand;
import com.github.xerragnaroek.jikai.commands.guild.set.SetCommand;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;
import com.github.xerragnaroek.jikai.util.Initilizable;
import com.github.xerragnaroek.jikai.util.prop.Property;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

/**
 * Handles commands (who'd have thunk?).
 * 
 * @author XerRagnarök
 */
public class CommandHandler implements Initilizable {
	private static Set<GuildCommand> commands = new TreeSet<>();
	private final Logger log;
	private Property<String> prefix = new Property<>();
	private Property<Boolean> comsEnabled = new Property<>(true);
	private AtomicBoolean initialized = new AtomicBoolean(false);
	private Jikai j;
	public static Permission[] MOD_PERMS = new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS };

	static {
		GuildCommand[] coms = new GuildCommand[] { new ReloadLocalesCommand(), new BugCommand(), new SetupJikaiServerCommand(), new ClearChannelCommand(), new ForceSaveCommand(), new ForceRegisterCommand(), new StopCommand(), new StatusCommand(), /*
																																																														 * new
																																																														 * PingCommand
																																																														 * (
																																																														 * )
																																																														 * ,
																																																														 */ new SetCommand(), new ScheduleCommand(), new AnimeListCommand(), new HelpCommand(), new EnableCommandsCommand(), new DisableCommandsCommand(),/*
																																																																																																			 * new
																																																																																																			 * RequestAssistanceCommand
																																																																																																			 * (
																																																																																																			 * )
																																																																																																			 */ };
		commands.addAll(Arrays.asList(coms));
	}

	public CommandHandler(long g, Jikai j) {
		this.j = j;
		log = LoggerFactory.getLogger(CommandHandler.class.getName() + "#" + g);
		init();
	}

	public void init() {
		JikaiData jd = j.getJikaiData();
		jd.prefixProperty().bindAndSet(prefix);
		jd.comsEnabledProperty().bind(comsEnabled);
		if (jd.hasExplicitCommandSetting()) {
			comsEnabled.set(jd.areCommandsEnabled());
		}
		initialized.set(true);
		log.info("Initialized");
	}

	public void handleMessage(GuildMessageReceivedEvent event) {
		// ignore bots
		JikaiData jd = j.getJikaiData();
		if (!event.getAuthor().isBot()) {
			if (!jd.hasCommandChannelId() || event.getChannel().getIdLong() == jd.getCommandChannelId() || event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
				String content = event.getMessage().getContentRaw();
				String prefix = this.prefix.get();
				// is it supposed to be a command?
				if (content.startsWith(prefix)) {
					log.debug("Received message: {}", content);
					// remove prefix from content
					content = content.substring(prefix.length());
					// any recognised commands?
					String[] tmp = content.trim().split(" ");
					GuildCommand com = ComUtils.findCommand(commands, tmp[0]);
					if (com != null && com.isEnabled()) {
						log.info("Recognised command {}", com.getName());
						// remove the command from the content and execute it
						tmp = (String[]) ArrayUtils.subarray(tmp, 1, tmp.length);
						if (ComUtils.checkPermissions(com, event.getMember())) {
							if (comsEnabled.get() || com.isAlwaysEnabled() || event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
								j.getJikaiData().incrementAndGetExecComs();
								com.executeCommand(event, tmp);
							}
						}
					} else {
						log.debug("No commands recognised");
						// trash talk the user cause they typed some garbage
						// event.getChannel().sendMessageFormat("**Bruh** :triumph: %s, what the **fuck** is that
						// command
						// supposed to mean? :angry: Smh my damn head. :clown:",
						// event.getAuthor().getAsMention()).queue();
					}
				}
			}
		}
	}

	public String getPrefix() {
		return prefix.get();
	}

	@Override
	public boolean isInitialized() {
		return initialized.get();
	}

	public static Set<GuildCommand> getCommands() {
		return commands;
	}
}
