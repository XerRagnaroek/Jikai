package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import java.util.List;

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
        return List.of("tl");
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        JikaiLocale loc = ju.getLocale();
        try {
            int i = Integer.parseInt(arguments[0]);
            if (i < 1 || i > 3) {
                ju.sendPM(loc.getStringFormatted("com_ju_title_lang_range", List.of("num"), i));
                return;
            }
            ju.setTitleLanguage(TitleLanguage.values()[i - 1]);
        } catch (NumberFormatException e) {
            String str = arguments[0];
            switch (str.toLowerCase()) {
                case "english":
                    ju.setTitleLanguage(TitleLanguage.ENGLISH);
                    break;
                case "native":
                    ju.setTitleLanguage(TitleLanguage.NATIVE);
                    break;
                case "romaji":
                    ju.setTitleLanguage(TitleLanguage.ROMAJI);
                    break;
                default:
                    ju.sendPM(loc.getString("com_ju_title_lang_invalid"));
                    return;
            }
        }
        ju.sendPM(loc.getStringFormatted("com_ju_title_lang_msg", List.of("lang"), ju.getTitleLanguage()));
    }

    @Override
    public String getLocaleKey() {
        return "com_ju_title_lang";
    }

}
