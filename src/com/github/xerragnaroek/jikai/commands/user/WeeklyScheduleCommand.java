package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

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
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiLocale loc = ju.getLocale();
		if (arguments.length == 0) {
			BotUtils.sendPMsEmbed(ju.getUser(), BotUtils.createWeeklySchedule(ju, ScheduleManager.getSchedule(ju.getTimeZone()).makeUserTable(ju)));
		} else {
			ComUtils.trueFalseCommand(arguments[0], ju, (b) -> {
				ju.setSendWeeklySchedule(b);
				ju.sendPM(b ? loc.getString("ju_weekly_sched_true") : loc.getString("ju_weekly_sched_false"));
			});
		}
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_weekly";
	}

}
