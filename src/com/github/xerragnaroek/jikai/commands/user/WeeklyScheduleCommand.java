package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 *
 */
public class WeeklyScheduleCommand implements JUCommand {

	@Override
	public String getName() {
		return "weekly_schedule";
	}

	@Override
	public String getAlternativeName() {
		return "weekly";
	}

	@Override
	public String getDescription() {
		return "Enable/Disable the weekly schedule.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		ComUtils.trueFalseCommand(arguments[0], ju, (b) -> {
			ju.setSentWeeklySchedule(b);
			ju.sendPM(b ? "You will recieve a weekly schedule with all your subscribed anime!" : "You won't recieve a weekly schedule.");
		});
	}

	@Override
	public String getUsage() {
		return "weekly_schedule <true/false>";
	}

}
