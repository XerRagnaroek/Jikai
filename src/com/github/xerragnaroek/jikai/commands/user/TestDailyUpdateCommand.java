package com.github.xerragnaroek.jikai.commands.user;

import java.time.ZoneId;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

import com.github.xerragnaroek.jikai.anime.schedule.AnimeTable;
import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * @author XerRagnaroek
 */
public class TestDailyUpdateCommand implements JUCommand {

	@Override
	public String getName() {
		return "test_daily";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "Sends the daily update usually send at midnight.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiUserManager.getInstance().getUserUpdater().testDailyUpdate(ju);
		ZoneId zone = ju.getTimeZone();
		AnimeTable at = ScheduleManager.getSchedule(zone).makeUserTable(ju);
		JikaiLocale loc = ju.getLocale();
		StringBuilder bob = new StringBuilder();
		at.toFormatedWeekString(ju.getTitleLanguage(), true, loc.getLocale()).values().forEach(s -> bob.append("\n" + s));
		MessageBuilder mb = new MessageBuilder();
		mb.setContent(bob.toString());
		Queue<MessageEmbed> q = new LinkedList<>();
		Queue<Message> msgs = mb.buildAll(SplitPolicy.NEWLINE);
		EmbedBuilder eb = BotUtils.addJikaiMark(new EmbedBuilder());
		eb.setTitle(loc.getString("ju_eb_weekly_sched_msg"));
		Consumer<Message> setAndAdd = m -> {
			eb.setDescription("```asciidoc\n" + m.getContentRaw() + "\n```");
			q.add(eb.build());
		};
		setAndAdd.accept(msgs.poll());
		eb.setTitle(null);
		msgs.forEach(setAndAdd);
		BotUtils.sendPMsEmbed(ju.getUser(), q);
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
