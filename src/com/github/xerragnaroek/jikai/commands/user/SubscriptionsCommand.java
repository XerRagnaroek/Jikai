
package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

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
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_ju_subs_desc");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		MessageEmbed msg = ju.getSubscribedAnime().getSubscriptionsFormatted(ju);
		if (msg != null) {
			ju.sendPM(msg);
		} else {
			ju.sendPM(ju.getLocale().getString("com_ju_subs_none"));
		}
	}

}
