
package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 *
 */
public class TitleLanguageCommand implements JUCommand {

	@Override
	public String getName() {
		return "title_language";
	}

	@Override
	public String getAlternativeName() {
		return "tl";
	}

	@Override
	public String getDescription() {
		return "How your titles are displayed:\n**1. English** - 'Is This a Zombie?'\n**2. Native** - 'これはゾンビですか?'\n**3. Romanji** - 'Kore wa Zombie Desu ka?'";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		try {
			int i = Integer.parseInt(arguments[0]);
			if (i < 1 || i > 3) {
				ju.sendPMFormat("'%d' is not in the acceptable range of 1-3!", i);
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
					ju.sendPMFormat("'%s' isn't a valid title language. Supported Languages: 1/English, 2/Native or 3/Romanji.", str);
					return;
			}
		}
		ju.sendPMFormat("You will now see titles displayed in %s", ju.getTitleLanguage());
	}

	@Override
	public String getUsage() {
		return "title_language <1/2/3 or English/Native/Romanji>";
	}

}
