package com.xerragnaroek.bot.anime.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;
import com.xerragnaroek.bot.anime.alrh.ALRHManager;
import com.xerragnaroek.bot.data.GuildDataManager;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

public class RoleMentioner {

	private static final Logger log = LoggerFactory.getLogger(RoleMentioner.class);

	public static void mentionUpdate(Guild g, Anime anime, ReleaseTime time) {
		TextChannel tc = g.getTextChannelById(GuildDataManager.getDataForGuild(g).getAnimeChannelId());
		if (tc != null) {
			log.debug("Sending to TextChannel {}", tc.getName());
			Role r = g.getRoleById(ALRHManager.getAnimeListReactionHandlerForGuild(g.getId()).getRoleId(anime.title));
			tc.sendMessage(r.getAsMention()).queue();
			tc.sendMessage(makeMessage(g, anime, time)).queue(	v -> log
					.info("Successfully sent message to guild {}, textchannel {}", g.getId(), tc.getName()),
																e -> log.error("Failed sending message", e));
		}
	}

	private static MessageEmbed makeMessage(Guild g, Anime a, ReleaseTime time) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setImage(a.imageURL).addField(String
				.format("**%s**\nreleases in **%d** days, **%d** hours and **%d** %s\n%2$02d:%3$02d:%4$02d", a.title,
						time.days(), time.hours(), time.mins(), (time.mins() == 1) ? "minute" : "minutes"), "", false);
		log.debug("Made MessageEmbed");
		return eb.build();
	}
}
