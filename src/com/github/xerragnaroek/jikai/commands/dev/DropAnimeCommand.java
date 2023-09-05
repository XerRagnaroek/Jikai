package com.github.xerragnaroek.jikai.commands.dev;

import com.github.xerragnaroek.jasa.AniException;
import com.github.xerragnaroek.jasa.JASA;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.token.JikaiUserAniTokenManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 *
 */
public class DropAnimeCommand implements JUCommand {

    @Override
    public String getName() {
        return "drop";
    }

    @Override
    public String getLocaleKey() {
        return "";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        int id = Integer.parseInt(arguments[0]);
        if (ju.getAniId() > 0 && JikaiUserAniTokenManager.hasToken(ju)) {
            JASA jasa = AnimeDB.getJASA();
            try {
                jasa.updateMediaListEntryToDroppedList(JikaiUserAniTokenManager.getAniToken(ju).getAccessToken(), jasa.getMediaListEntryIdForUserFromAniId(ju.getAniId(), id));
                ju.sendPM("Dropped " + AnimeDB.getAnime(id).getTitle(ju.getTitleLanguage()));
            } catch (AniException | IOException e) {
                BotUtils.logAndSendToDev(LoggerFactory.getLogger(this.getClass()), "Failed dropping anime", e);
            }
        }
    }

    @Override
    public boolean isDevOnly() {
        return true;
    }
}
