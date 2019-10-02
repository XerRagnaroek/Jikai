package com.xerragnaroek.bot.timer;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;
import com.xerragnaroek.bot.anime.alrh.ALRHManager;
import com.xerragnaroek.bot.anime.alrh.ALRHandler;
import com.xerragnaroek.bot.data.GuildDataManager;
import com.xerragnaroek.bot.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

public class RoleMentioner {

	private static final Logger log = LoggerFactory.getLogger(RoleMentioner.class);

	public static void mentionUpdate(Guild g, Anime anime, ReleaseTime time, Consumer<Message> whenComplete) {
		TextChannel tc = g.getTextChannelById(GuildDataManager.getDataForGuild(g).getAnimeChannelId());
		if (tc != null) {
			log.debug("Sending to TextChannel {}", tc.getName());
			ALRHandler h = ALRHManager.getAnimeListReactionHandlerForGuild(g.getId());
			String rId = h.getRoleId(anime.title);
			if (rId != null) {
				tc.sendMessage(g.getRoleById(rId).getAsMention()).queue();
			}
			tc.sendMessage(makeMessage(g, anime, time))
					.queue(	whenComplete.andThen(v -> log.debug("Successfully sent message to guild {}, textchannel {}",
																g.getId(), tc.getName())),
							e -> BotUtils.logAndSendToDev(log, "Failed sending message", e));
		}
	}

	private static MessageEmbed makeMessage(Guild g, Anime a, ReleaseTime time) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setImage(a.imageURL)
				.addField(	String.format("**%s**\nreleases in **%s** %s\n%02d:%02d:%02d", a.title, time,
										(time.mins() == 1) ? "minute" : "minutes", time.days(), time.hours(),
										time.mins()),
							"", false);
		log.debug("Made MessageEmbed");
		return eb.build();
	}
}
