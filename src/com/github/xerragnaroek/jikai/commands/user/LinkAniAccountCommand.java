package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;

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
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_ju_link_ani_desc");
	}

	@Override
	public String getUsage(JikaiLocale loc) {
		return loc.getStringFormatted("com_ju_link_ani_use", Arrays.asList("com"), getName());
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

}
