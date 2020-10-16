
package com.github.xerragnaroek.jikai.commands.user;

import java.awt.Color;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import net.dv8tion.jda.api.EmbedBuilder;

/**
 * @author XerRagnaroek
 */
public class SubscriptionsCommand implements JUCommand {

	@Override
	public String getName() {
		return "subscriptions";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("subs");
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_ju_subs_desc");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		Set<Integer> subs = ju.getSubscribedAnime();
		if (!subs.isEmpty()) {
			StringBuilder bob = new StringBuilder();
			bob.append("```asciidoc\n");
			subs.stream().map(AnimeDB::getAnime).map(a -> "- " + a.getTitle(ju.getTitleLanguage()) + "\n").sorted().forEach(bob::append);
			bob.append("```");
			EmbedBuilder eb = new EmbedBuilder();
			eb.setColor(new Color(240, 240, 240)).setTitle(ju.getLocale().getStringFormatted("com_ju_subs_eb_title", Arrays.asList("anime"), subs.size())).setDescription(bob).setTimestamp(Instant.now()).setFooter("Made with <3 by Jikai");
			ju.sendPM(eb.build());
		} else {
			ju.sendPM(ju.getLocale().getString("com_ju_subs_none"));
		}
	}

}
