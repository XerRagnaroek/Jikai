package com.xerragnaroek.bot.commands;

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.anime.AnimeBase;
import com.xerragnaroek.bot.anime.AnimeDayTime;
import com.xerragnaroek.bot.config.ConfigManager;
import com.xerragnaroek.bot.config.ConfigOption;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * The "schedule" command
 * 
 * @author XerRagnaroek
 *
 */
public class ScheduleCommand implements Command {
	private final Logger log = LoggerFactory.getLogger(ScheduleCommand.class);
	private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

	@Override
	public String getCommandName() {
		return "schedule";
	}

	@Override
	public String getUsage() {
		return getCommandName();
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String arguments) {
		Guild g = event.getGuild();
		TextChannel channel;
		if ((channel = g.getTextChannelById(ConfigManager.getConfigForGuild(g.getId()).getOption(ConfigOption.ANIME_CHANNEL))) != null) {
			dumpSchedule(channel);
		} else {
			channel = event.getTextChannel();
			dumpSchedule(channel);
		}
		log.debug("Dumping schedule in channel {} on guild {}({})", channel.getId(), g.getName(), g.getId());
	}

	private void dumpSchedule(TextChannel tc) {
		for (DayOfWeek day : DayOfWeek.values()) {
			tc.sendMessage(dayOfTheWeekEmbed(day, tc.getGuild())).queue();
		}
	}

	private Message dayOfTheWeek(DayOfWeek day, Guild g) {
		Map<String, Set<String>> map = new TreeMap<>();
		AnimeBase.getAnimesAiringOnWeekday(day, g).stream().forEach(adt -> putInMap(adt, map));
		StringBuilder sb = new StringBuilder();
		map.forEach((time, titles) -> {
			sb.append("* " + time + ": ");
			sb.append(String.join("\n         ", titles) + "\n");
		});
		MessageBuilder bob = new MessageBuilder();
		bob.appendCodeBlock(String.format("%s%n%s%n%s", day, "=".repeat(day.toString().length() + 1), String.join("\n", sb.toString())), "asciidoc");
		return bob.build();
	}

	private MessageEmbed dayOfTheWeekEmbed(DayOfWeek day, Guild g) {
		Map<String, Set<String>> map = new TreeMap<>();
		AnimeBase.getAnimesAiringOnWeekday(day, g).stream().forEach(adt -> putInMap(adt, map));
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(day.toString()).setColor(Color.red);
		map.forEach((time, titles) -> {
			eb.addField(time, "**-" + String.join("\n-", titles) + "**", false);
		});
		return eb.build();
	}

	private void putInMap(AnimeDayTime adt, Map<String, Set<String>> map) {
		map.compute((adt.hasBroadcastTime()) ? timeFormat.format(adt.getBroadcastTime()) : "Unknown", (k, s) -> {
			if (s == null) {
				s = new TreeSet<>();
			}
			s.add(adt.getAnime().title);
			return s;
		});
	}
}
