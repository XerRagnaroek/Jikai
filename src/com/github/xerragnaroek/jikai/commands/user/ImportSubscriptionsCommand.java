package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.user.SubscriptionExportHandler;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

/**
 * 
 */
public class ImportSubscriptionsCommand implements JUCommand {

	@Override
	public String getName() {
		return "import";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		if (arguments.length < 1) {
			ju.sendPM("You need to suppy an export key! Ask the other user for theirs!");
			return;
		}
		String key = arguments[0];
		SubscriptionExportHandler seh = SubscriptionExportHandler.getInstance();
		if (seh.hasIdForKey(key)) {
			long id = seh.getJikaiUserIdFromKey(key);
			JikaiUser user = JikaiUserManager.getInstance().getUser(id);
			User dUser = user.getUser();
			MessageEmbed msg = user.getSubscribedAnime().getSubscriptionsFormatted(user);
			if (msg != null) {
				EmbedBuilder eb = new EmbedBuilder(msg);
				if (dUser != null) {
					eb.setThumbnail(dUser.getAvatarUrl()).setTitle(dUser.getName() + "'s subscribed anime are:");
				} else {
					eb.setTitle("This user's subscribed anime are:");
				}
				ju.sendPM(eb.build());
			} else {
				ju.sendPMFormat("%s hasn't subscribed to any anime yet!", dUser == null ? "This user" : dUser.getAsTag());
			}
		} else {
			ju.sendPM("Invalid key!");
		}
	}

}
