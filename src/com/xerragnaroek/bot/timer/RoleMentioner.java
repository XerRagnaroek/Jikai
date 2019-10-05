package com.xerragnaroek.bot.timer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;
import com.xerragnaroek.bot.anime.alrh.ALRHandler;
import com.xerragnaroek.bot.anime.db.AnimeDayTime;
import com.xerragnaroek.bot.core.Core;
import com.xerragnaroek.bot.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

public class RoleMentioner {

	private static final Logger log = LoggerFactory.getLogger(RoleMentioner.class);
	private static DateTimeFormatter date = DateTimeFormatter.ofPattern("dd.MM.uuuu");
	private static DateTimeFormatter timeF = DateTimeFormatter.ofPattern("HH:mm");

	public static void mentionUpdate(Guild g, AnimeDayTime adt, ReleaseTime time, Consumer<Message> whenComplete) {
		TextChannel tc = g.getTextChannelById(Core.GDM.get(g).getAnimeChannelId());
		if (tc != null) {
			log.debug("Sending to TextChannel {}", tc.getName());
			ALRHandler h = Core.ALRHM.get(g.getId());
			String rId = h.getRoleId(adt.getAnime().title);
			if (rId != null) {
				tc.sendMessage(g.getRoleById(rId).getAsMention()).queue();
			}
			tc.sendMessage(makeMessage(g, adt, time)).queue(whenComplete.andThen(v -> log.debug("Successfully sent message to guild {}, textchannel {}", g.getId(), tc.getName())), e -> BotUtils.logAndSendToDev(log, "Failed sending message", e));
		}
	}

	private static MessageEmbed makeMessage(Guild g, AnimeDayTime adt, ReleaseTime time) {
		EmbedBuilder eb = new EmbedBuilder();
		Anime a = adt.getAnime();
		ZonedDateTime zdt = adt.getZonedDateTime();
		eb.setThumbnail(a.imageURL).setTitle("**" + a.title + "**", a.url).setDescription(String.format("**%s, %s** at %s\n**%s** \n%02d:%02d:%02d", adt.getDayOfWeek(), date.format(zdt), timeF.format(zdt), time, time.days(), time.hours(), time.mins())).setTimestamp(ZonedDateTime.now(Core.GDM.get(g).getTimeZone()));
		log.debug("Made MessageEmbed");
		return eb.build();
	}
}
