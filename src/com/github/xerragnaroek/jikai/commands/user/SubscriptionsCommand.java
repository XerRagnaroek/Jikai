
package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.PrivateList;
import com.github.xerragnaroek.jikai.user.SubscriptionSet;

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
	public void executeCommand(JikaiUser ju, String[] arguments) {
		/*
		 * List<MessageEmbed> embeds =
		 * BotUtils.buildEmbeds(ju.getLocale().getStringFormatted("com_ju_subs_eb_title",
		 * Arrays.asList("anime"), ju.getSubscribedAnime().size()),
		 * ju.getSubscribedAnime().getSubscriptionsFormatted(ju));
		 * if (!embeds.isEmpty()) {
		 * embeds.forEach(e -> ju.sendPM(e));
		 * } else {
		 * ju.sendPM(ju.getLocale().getString("com_ju_subs_none"));
		 * }
		 */
		JikaiLocale loc = ju.getLocale();
		SubscriptionSet set = ju.getSubscribedAnime();
		if (!set.isEmpty()) {
			new PrivateList(ju, loc.getStringFormatted("com_ju_subs_eb_title", Arrays.asList("anime"), set.size()), null, true).sendList(set);
		} else {
			ju.sendPM(loc.getString("com_ju_subs_none"));
		}

	}

	@Override
	public String getLocaleKey() {
		return "com_ju_subs";
	}

}
