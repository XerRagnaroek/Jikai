package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.link.JikaiUserLinkHandler;
import com.github.xerragnaroek.jikai.user.link.LinkRequest;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.entities.User;

/**
 * 
 */
public class LinkUserCommand implements JUCommand {

	@Override
	public String getName() {
		return "link";
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_link";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		if (arguments.length < 1) {
			ju.sendPM(getUsage(ju.getLocale()));
			return;
		}
		int index = 0;
		int dir = LinkRequest.UNIDIRECTIONAL;
		if (arguments[0].equals("bidi")) {
			index++;
			dir = LinkRequest.BIDIRECTIONAL;
		}
		JikaiUser tgt = BotUtils.resolveUser(arguments[index]);
		if (tgt == null) {
			ju.sendPM(BotUtils.localedEmbedTitleDescription(ju.getLocale(), "com_ju_link_fail", ""));
			return;
		}

		if (ju.getId() == tgt.getId()) {
			ju.sendPM(BotUtils.localedEmbed(ju.getLocale(), "com_ju_link_eb_selflink"));
			return;
		}
		String msg = null;
		if (index == 1 && arguments.length >= 3) {
			msg = Arrays.stream(arguments, 2, arguments.length).collect(Collectors.joining(" "));
		}
		int res = JikaiUserLinkHandler.initiateLink(ju, tgt, dir, msg);
		JikaiLocale loc = ju.getLocale();
		User u = tgt.getUser();
		switch (res) {
			case 0 -> ju.sendPM(BotUtils.embedBuilder().setTitle(loc.getStringFormatted("com_ju_link_fail2", Arrays.asList("name"), tgt.getUser().getName())).build());
			case 2 -> ju.sendPM(BotUtils.embedBuilder().setDescription(loc.getStringFormatted("ju_link_req_dupe", Arrays.asList("name"), u.getName())).setThumbnail(u.getEffectiveAvatarUrl()).build());
			case 3 -> ju.sendPM(BotUtils.embedBuilder().setDescription(loc.getStringFormatted("ju_link_req_dupe_tgt", Arrays.asList("name"), u.getName())).setThumbnail(u.getEffectiveAvatarUrl()).build());
			// 1 means successful link or request
		}
		if (res == 0) {

			ju.sendPM(BotUtils.embedBuilder().setTitle(loc.getStringFormatted("com_ju_link_fail2", Arrays.asList("name"), tgt.getUser().getName())).build());
		}
	}

}
