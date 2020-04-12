/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
					ju.sendPMFormat("'%s' isn't a valid title language. Supported Languages: 1/English, 2/Japanese or 3/Romanji.", str);
					return;
			}
		}
		ju.sendPMFormat("You will now see titles displayed in %s", ju.getTitleLanguage());
	}

	@Override
	public String getUsage() {
		return "title_language <1/2/3 or English/Japanese/Romanji>";
	}

}
