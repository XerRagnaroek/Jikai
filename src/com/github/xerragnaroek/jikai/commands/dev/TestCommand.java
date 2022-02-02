package com.github.xerragnaroek.jikai.commands.dev;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import com.github.xerragnaroek.jikai.util.pagi.PaginationTmp;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.Component;

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
		PaginationTmp test = new PaginationTmp("testPagi");
		test.setArrowButtonStyle(ButtonStyle.DANGER);
		for (int i = 0; i < 23; i++) {
			MessageBuilder mb = new MessageBuilder("Page " + (i + 1));
			List<Component> btns = new LinkedList<>();
			for (int n = 0; n <= i; n++) {
				btns.add(Button.primary("test" + n, "" + (n + 1)));
			}
			mb.setActionRows(BotUtils.makeActionRows(btns));
			test.addPage(mb.build());
		}
		ju.getUser().openPrivateChannel().queue(pc -> test.sendPagination(pc));
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
}
