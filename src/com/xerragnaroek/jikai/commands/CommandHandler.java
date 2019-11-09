package com.xerragnaroek.jikai.commands;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.jikai.Jikai;
import com.xerragnaroek.jikai.jikai.JikaiData;
import com.xerragnaroek.jikai.util.Destroyable;
import com.xerragnaroek.jikai.util.Initilizable;
import com.xerragnaroek.jikai.util.prop.Property;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Handles commands (who'd have thunk?).
 * 
 * @author XerRagnarök
 *
 */
public class CommandHandler implements Initilizable, Destroyable {
	private Set<Command> commands;
	private final Logger log;
	private Property<String> trigger = new Property<>();
	private Property<Boolean> comsEnabled = new Property<>();
	private AtomicBoolean initialized = new AtomicBoolean(false);
	private Jikai j;

	CommandHandler(long g) {
		log = LoggerFactory.getLogger(CommandHandler.class.getName() + "#" + g);
		j = Core.JM.get(g);
		j.setCH(this);
		init();
	}

	public void init() {
		JikaiData jd = j.getJikaiData();
		jd.triggerProperty().bindAndSet(trigger);
		jd.comsEnabledProperty().bind(comsEnabled);
		if (jd.hasExplicitCommandSetting()) {
			comsEnabled.set(jd.areCommandsEnabled());
		} else {
			comsEnabled.set(Core.JM.getCHM().areCommandsEnabledByDefault());
		}
		commands = Core.JM.getCHM().getCommands();
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
						if (comsEnabled.get() || com.isAlwaysEnabled()) {
							j.getJikaiData().incrementAndGetExecComs();
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

	@Override
	public void destroy() {
		trigger.destroy();
		comsEnabled.destroy();
	}

}
