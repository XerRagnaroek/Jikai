package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 *
 */
public class CustomTitleCommand implements JUCommand {

    @Override
    public String getName() {
        return "custom_title";
    }

    @Override
    public String getLocaleKey() {
        return "com_ju_custom_title";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        JikaiLocale loc = ju.getLocale();
        if (arguments.length == 0) {
            EmbedBuilder eb = BotUtils.embedBuilder();
            eb.setTitle(loc.getString("com_ju_custom_title_list_eb_title"));
            if (ju.customAnimeTitlesProperty().isEmpty()) {
                ju.sendPM(loc.getString("com_ju_custom_title_none"));
            } else {
                List<String> strings = new ArrayList<>(ju.customAnimeTitlesProperty().size());
                ju.customAnimeTitlesProperty().forEach((id, title) -> {
                    Anime a = AnimeDB.getAnime(id);
                    strings.add(String.format("**[%s](%s)**->**%s**", a.getTitle(ju.getTitleLanguage()), a.getAniUrl(), title));
                });
                eb.setDescription(BotUtils.padEquallyAndJoin("->", "\n", null, strings.toArray(new String[strings.size()])));
                ju.sendPM(eb.build());
            }
        } else if (arguments.length >= 1) {
            Anime a = null;
            int titleStartsAt = 0;
            try {
                a = AnimeDB.getAnime(Integer.parseInt(arguments[0]));
                titleStartsAt = 1;
            } catch (NumberFormatException e) {
                String title = "";
                int c = 0;
                for (; c < arguments.length; c++) {
                    if (arguments[c].equals("%%")) {
                        break;
                    }
                    title += " " + arguments[c];
                }
                titleStartsAt = c + 1;
                try {
                    a = AnimeDB.getAnime(title.trim());
                } catch (NoSuchElementException e1) {
                }
            }
            if (a != null) {
                if (titleStartsAt > arguments.length) {
                    if (ju.hasCustomTitle(a.getId())) {
                        String oldtitle = ju.removeCustomTitle(a.getId());
                        ju.sendPM(loc.getStringFormatted("com_ju_custom_title_rem", Arrays.asList("custom", "anime"), oldtitle, a.getTitle(ju.getTitleLanguage())));
                    } else {
                        ju.sendPM(loc.getStringFormatted("com_ju_custom_title_no", List.of("anime"), a.getTitle(ju.getTitleLanguage())));
                    }
                } else {
                    String title = "";
                    for (int i = titleStartsAt; i < arguments.length; i++) {
                        title += " " + arguments[i];
                    }
                    title = title.trim();
                    ju.addCustomTitle(a.getId(), title);
                    ju.sendPM(loc.getStringFormatted("com_ju_custom_title_add", Arrays.asList("anime", "custom"), a.getTitle(ju.getTitleLanguage()), title));
                }
            } else {
                ju.sendPM(loc.getStringFormatted("com_ju_custom_title_invalid", List.of("input"), String.join(" ", arguments)));
            }
        }
    }

}
