package com.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.commands.ComUtils;
import com.xerragnaroek.jikai.user.JikaiUser;

public class JUCommandHandler {

	private static Set<JUCommand> commands = Collections.synchronizedSet(new TreeSet<>());
	private static final Logger log = LoggerFactory.getLogger(JUCommandHandler.class);
	static {
		JUCommand coms[] = new JUCommand[] { new StopCommand(), new HelpCommand(), new ConfigCommand(), new DailyUpdateCommand(), new NotifyReleaseCommand(), new ReleaseStepCommand(), new TimeZoneCommand(), new TitleLanguageCommand() };
		commands.addAll(Arrays.asList(coms));
	}

	private JUCommandHandler() {}

	public static Set<JUCommand> getCommands() {
		return commands;
	}

	public static void handleMessage(JikaiUser ju, String content) {
		content = content.substring(1);
		String[] tmp = content.trim().split(" ");
		JUCommand com = ComUtils.findCommand(commands, tmp[0]);
		if (com != null) {
			log.debug("Found command: '{}'", com.getName());
			//remove the command from the content and execute it
			tmp = (String[]) ArrayUtils.subarray(tmp, 1, tmp.length);
			com.executeCommand(ju, tmp);
		}
	}
}
