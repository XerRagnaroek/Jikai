package com.github.xerragnaroek.jikai.user;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.ButtonInteractor;
import com.github.xerragnaroek.jikai.util.btn.ButtonedMessage;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

/**
 * 
 */
public class SubListHandlerBtn implements ButtonInteractor {

	private static String identifier = "slh";
	private static SubListHandlerBtn instance = new SubListHandlerBtn();

	private SubListHandlerBtn() {
		Core.getEventListener().registerButtonInteractor(this);
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public void handleButtonClick(String[] data, ButtonClickEvent event) {
		event.deferEdit().queue();
		JikaiUser ju = JikaiUserManager.getInstance().getUser(event.getUser().getIdLong());
		int id = Integer.parseInt(data[0]);
		if (ju.isSubscribedTo(id)) {
			ju.unsubscribeAnime(id, ju.getLocale().getString("ju_private_list_unsub_cause"));
			event.editButton(event.getButton().withStyle(ButtonStyle.DANGER)).queue();
		} else {
			ju.subscribeAnime(id, ju.getLocale().getString("ju_private_list_sub_cause"));
			event.editButton(event.getButton().withStyle(ButtonStyle.SUCCESS)).queue();
		}
	}

	public static void sendSubList(JikaiUser ju) {
		BotUtils.sendPMs(ju.getUser(), makeSubMessages(ju));
	}

	private static List<Message> makeSubMessages(JikaiUser ju) {
		Set<Anime> anime = ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).collect(Collectors.toCollection(() -> new TreeSet<>(Anime.IGNORE_TITLE_CASE)));
		List<List<Anime>> partitioned = BotUtils.partitionCollection(anime, ButtonedMessage.MAX_BUTTONS);
		List<Message> messages = new LinkedList<>();
		String title = ju.getLocale().getStringFormatted("com_ju_subs_eb_title", Arrays.asList("anime"), anime.size());
		for (int partition = 0; partition < partitioned.size(); partition++) {
			ButtonedMessage msg = new ButtonedMessage();
			List<Anime> l = partitioned.get(partition);
			EmbedBuilder eb = BotUtils.embedBuilder();
			eb.setTitle(title + (partitioned.size() > 1 ? " " + (partition + 1) + "/" + partitioned.size() : ""));
			// TODO Buttons are the wrong way!
			for (int i = 0; i < l.size(); i++) {
				Anime a = l.get(i);
				eb.appendDescription(String.format("**%02d**: [**%s**](%s)%n", i + 1, a.getTitle(ju.getTitleLanguage()), a.getAniUrl()));
				msg.addButton(Button.success(identifier + ":" + a.getId(), String.format("%02d", i + 1)));
			}
			msg.addMessageEmbed(eb.build());
			messages.add(msg.toMessage());
		}
		return messages;
	}
}
