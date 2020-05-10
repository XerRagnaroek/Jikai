
package com.github.xerragnaroek.jikai.commands.user;

import java.time.Instant;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import net.dv8tion.jda.api.EmbedBuilder;

/**
 * @author XerRagnaroek
 *
 */
public class SubscriptionsCommand implements JUCommand {

	@Override
	public String getName() {
		return "subscriptions";
	}

	@Override
	public String getAlternativeName() {
		return "subs";
	}

	@Override
	public String getDescription() {
		return "Lists your subscriptions";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		StringBuilder bob = new StringBuilder();
		ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).map(a -> a.getTitle(ju.getTitleLanguage()) + "\n").sorted().forEach(bob::append);
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Your subscriptions:").setDescription(bob).setTimestamp(Instant.now());
		ju.sendPM(eb.build());
	}

}
