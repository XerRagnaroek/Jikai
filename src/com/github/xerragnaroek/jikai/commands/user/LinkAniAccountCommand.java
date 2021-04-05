package com.github.xerragnaroek.jikai.commands.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.anime.ani.AniLinker;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class LinkAniAccountCommand implements JUCommand {
	private final Logger log = LoggerFactory.getLogger(LinkAniAccountCommand.class);

	@Override
	public String getName() {
		return "link_ani";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiLocale loc = ju.getLocale();
		if (arguments.length < 1) {
			log.debug("No arguments!");
			ju.sendPM(loc.getString("com_ju_link_ani_invalid"));
			return;
		}
		AniLinker.linkAniAccount(ju, arguments[0]);
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_link_ani";
	}

}
