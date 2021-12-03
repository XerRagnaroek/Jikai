package com.github.xerragnaroek.jikai.commands;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.list.btn.AnimeListHandler;
import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.btn.ButtonedMessage;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;

/**
 * 
 */
public class TestCommand implements JUCommand, GuildCommand {

	private final Logger log = LoggerFactory.getLogger(TestCommand.class);

	@Override
	public String getName() {
		return "test";
	}

	@Override
	public String getLocaleKey() {
		return "";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		ButtonedMessage mes = new ButtonedMessage();
		mes.setMessageEmbeds(BotUtils.makeSimpleEmbed("Two Embeds, 5 rows"), BotUtils.makeSimpleEmbed("2nd embed"));
		for (int r = 0; r < 5; r++) {
			for (int b = 0; b <= r; b++) {
				mes.addButton(r, Button.danger(new Random().nextInt() + "", (r + b + 2) + ""));
			}
		}
		ju.sendPM(mes.toMessage());
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		/*
		 * MessageBuilder bob = new MessageBuilder();
		 * EmbedBuilder eb = BotUtils.embedBuilder();
		 * BiFunction<Integer, Integer, Button> btnCreator = (i, r) -> Button.danger("test",
		 * "FXmMeqDD9iHDWqA6d3ND9HKoPVwLEricjD9verqAzXGneRKDYdSHPS9PSXGSGssNpMeDpYjnDws7PAyL");
		 * if (arguments.length > 0) {
		 * switch (Integer.parseInt(arguments[0])) {
		 * case 0 -> btnCreator = (i, r) -> Button.danger("test", "" + (i + (5 * r)));
		 * case 1 -> btnCreator = (i, r) -> Button.primary("test", "" + (i + (5 * r)));
		 * case 2 -> btnCreator = (i, r) -> Button.secondary("test", "" + (i + (5 * r)));
		 * case 3 -> btnCreator = (i, r) -> Button.success("test", "" + (i + (5 * r)));
		 * case 4 -> btnCreator = (i, r) -> {
		 * Button b = null;
		 * switch (r) {
		 * case 0 -> b = Button.danger("test", "" + (i + (5 * r)));
		 * case 1 -> b = Button.primary("test", "" + (i + (5 * r)));
		 * case 2 -> b = Button.secondary("test", "" + (i + (5 * r)));
		 * case 3 -> b = Button.success("test", "" + (i + (5 * r)));
		 * case 4 -> b = Button.danger("test", "" + (i + (5 * r)));
		 * }
		 * return b;
		 * };
		 * }
		 * }
		 * List<ActionRow> arow = new LinkedList<>();
		 * for (int row = 0; row < 5; row++) {
		 * List<Button> btns = new LinkedList<>();
		 * for (int i = 1; i <= 5; i++) {
		 * btns.add(btnCreator.apply(i, row));
		 * eb.appendDescription("**" + (i + (5 * row)) +
		 * "**: [Anime Title](https://anilist.co/anime/130445)\n");
		 * }
		 * arow.add(ActionRow.of(btns));
		 * }
		 * bob.setEmbed(eb.build());
		 * bob.setActionRows(arow);
		 * event.getChannel().sendMessage(bob.build()).queue();
		 */
		AnimeListHandler alh = new AnimeListHandler(event.getGuild().getIdLong(), event.getChannel());
		AtomicInteger counter = new AtomicInteger();
		List<Anime> ani = AnimeDB.getLoadedAnime().stream().filter(a -> !a.isAdult() && (a.isReleasing() || a.isNotYetReleased() || a.isOnHiatus() || a.hasDataForNextEpisode())).collect(Collectors.toList());
		alh.groupingBy(e -> String.format("%02d", (counter.getAndIncrement() / 25) + 1));
		alh.setAnimeTitleFunction(a -> String.format("**[%s](%s)**", a.getTitleNative(), a.getAniUrl()));
		alh.setFilter(a -> !a.isAdult() && (a.isReleasing() || a.isNotYetReleased() || a.isOnHiatus() || a.hasDataForNextEpisode()));
		alh.sortingBy(Anime.IGNORE_TITLE_CASE);
		alh.sendList(ani).thenAccept(v -> System.out.println(alh.getMessageIdAnimeIdMap())).join();
		alh.setMessageIdAnimeIdMap(Map.of(255l, List.of()));
		alh.validateList();
	}

	public static void testButton(ButtonClickEvent event) {
		event.deferEdit().queue();
	}
}
