
package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 */
public class TitleLanguageCommand implements JUCommand {

	@Override
	public String getName() {
		return "title_language";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("tl");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiLocale loc = ju.getLocale();
		try {
			int i = Integer.parseInt(arguments[0]);
			if (i < 1 || i > 3) {
				ju.sendPM(loc.getStringFormatted("com_ju_title_lang_range", Arrays.asList("num"), i));
				return;
			}
			ju.setTitleLanguage(TitleLanguage.values()[i]);
		} catch (NumberFormatException e) {
			String str = arguments[0];
			switch (str.toLowerCase()) {
				case "english":
					ju.setTitleLanguage(TitleLanguage.ENGLISH);
					break;
				case "native":
					ju.setTitleLanguage(TitleLanguage.NATIVE);
					break;
				case "romanji":
					ju.setTitleLanguage(TitleLanguage.ROMAJI);
					break;
				default:
					ju.sendPM(loc.getString("com_ju_title_lang_invalid"));
					return;
			}
		}
		ju.sendPM(loc.getStringFormatted("com_ju_title_lang_msg", Arrays.asList("lang"), ju.getTitleLanguage()));
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_title_lang";
	}

}
