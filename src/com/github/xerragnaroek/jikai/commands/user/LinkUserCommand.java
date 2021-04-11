package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.link.JikaiUserLinkHandler;
import com.github.xerragnaroek.jikai.user.link.LinkRequest;
import com.github.xerragnaroek.jikai.util.BotUtils;

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
			ju.sendPM(BotUtils.localedEmbed(ju.getLocale(), "com_ju_link_fail", ""));
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
		if (!JikaiUserLinkHandler.initiateLink(ju, tgt, dir, msg)) {
			JikaiLocale loc = ju.getLocale();
			ju.sendPM(BotUtils.embedBuilder().setTitle(loc.getStringFormatted("com_ju_link_fail2", Arrays.asList("name"), tgt.getUser().getName())).build());
		}
	}

}
