package com.github.xerragnaroek.jikai.anime;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.AniException;
import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.ButtonInteractor;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

/**
 * 
 */
public class GeneralSubHandler implements ButtonInteractor {

	private final Logger log = LoggerFactory.getLogger(GeneralSubHandler.class);
	public static final String IDENTIFIER = "gsh";

	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public void handleButtonClick(String[] data, ButtonClickEvent event) {
		event.deferEdit().queue();
		JikaiUser ju = JikaiUserManager.getInstance().getUser(event.getUser().getIdLong());
		if (ju != null) {
			try {
				int id = Integer.parseInt(data[0]);
				Anime a = AnimeDB.loadAnimeViaId(id).get(0);
				log.debug("{} wants to subscribe to {},{}", ju.getId(), a.getTitleRomaji(), a.getId());
				if (a.isReleasing() || a.isOnHiatus() || a.isNotYetReleased() || a.hasDataForNextEpisode()) {
					if (ju.isSubscribedTo(id)) {
						ju.unsubscribeAnime(id, ju.getLocale().getString("ju_sub_rem_cause_user"));
					} else {
						ju.subscribeAnime(id, ju.getLocale().getString("ju_sub_add_cause_user"));
					}
				} else {
					log.debug("{},{} isn't a valid anime to subscribe!", a.getTitleRomaji(), a.getId());
				}
			} catch (NumberFormatException | AniException | IOException e) {
				BotUtils.logAndSendToDev(log, "Failed loading anime via id!", e);
			}
		}

	}

}
