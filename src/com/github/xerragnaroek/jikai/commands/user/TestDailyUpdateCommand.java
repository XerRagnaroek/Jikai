package com.github.xerragnaroek.jikai.commands.user;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.github.xerragnaroek.jikai.anime.schedule.AnimeTable;
import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.MessageBuilder.SplitPolicy;

/**
 * @author XerRagnaroek
 *
 */
public class TestDailyUpdateCommand implements JUCommand {

	@Override
	public String getName() {
		return "test_daily";
	}

	@Override
	public String getDescription() {
		return "Sends the daily update usually send at midnight.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		Jikai.getUserManager().getUserUpdater().testDailyUpdate(ju);
		ZoneId zone = ju.getTimeZone();
		LocalDate ld = ZonedDateTime.now(zone).toLocalDate();
		AnimeTable at = ScheduleManager.getSchedule(zone).makeUserTable(ju);
		//BotUtils.sendFile(ju.getUser(), "Your anime schedule for this week:", BotUtils.imageToByteArray(at.toImage()), "Schedule_" + BotUtils.formatTime(ld, "dd.MM.yy") + ".png");
		StringBuilder bob = new StringBuilder();
		String str = "Here's your anime schedule for this week:\n";
		bob.append(str + "=".repeat(str.length()));
		at.toFormatedWeekString(ju.getTitleLanguage(), true).values().forEach(s -> bob.append("\n" + s));
		MessageBuilder mb = new MessageBuilder();
		mb.appendCodeBlock(bob, "asciidoc");
		mb.buildAll(SplitPolicy.NEWLINE).forEach(ju::sendPM);
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
