
package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.entities.MessageEmbed;

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
		List<MessageEmbed> embeds = BotUtils.buildEmbeds(ju.getLocale().getStringFormatted("com_ju_subs_eb_title", Arrays.asList("anime"), ju.getSubscribedAnime().size()), ju.getSubscribedAnime().getSubscriptionsFormatted(ju));
		if (!embeds.isEmpty()) {
			embeds.forEach(e -> ju.sendPM(e));
		} else {
			ju.sendPM(ju.getLocale().getString("com_ju_subs_none"));
		}
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_subs";
	}

}
