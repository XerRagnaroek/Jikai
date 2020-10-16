package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 */
public class WeeklyScheduleCommand implements JUCommand {

	@Override
	public String getName() {
		return "weekly_schedule";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("weekly");
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_ju_weekly_desc");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiLocale loc = ju.getLocale();
		ComUtils.trueFalseCommand(arguments[0], ju, (b) -> {
			ju.setSentWeeklySchedule(b);
			ju.sendPM(b ? loc.getString("ju_weekly_sched_true") : loc.getString("ju_weekly_sched_false"));
		});
	}

	@Override
	public String getUsage(JikaiLocale loc) {
		return loc.getStringFormatted("com_ju_weekly_use", Arrays.asList("com"), getName());
	}

}
