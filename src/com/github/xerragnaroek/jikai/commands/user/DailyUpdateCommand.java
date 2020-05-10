
package com.github.xerragnaroek.jikai.commands.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 *
 */
public class DailyUpdateCommand implements JUCommand {
	private final Logger log = LoggerFactory.getLogger(DailyUpdateCommand.class);

	@Override
	public String getName() {
		return "daily_update";
	}

	@Override
	public String getAlternativeName() {
		return "daily";
	}

	@Override
	public String getDescription() {
		return "Enable/Disable the daily overview.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		ComUtils.trueFalseCommand(arguments[0], ju, (b) -> {
			ju.setUpdateDaily(b);
			ju.sendPM(b ? "You will recieve a daily overview of all your subscribed anime releasing on that day!" : "You won't recieve the daily overview.");
		});
	}

	@Override
	public String getUsage() {
		return "daily_update <true/false>";
	}
}
