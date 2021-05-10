package com.github.xerragnaroek.jikai.user;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.ani.AniLinker;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;
import com.github.xerragnaroek.jikai.util.pagi.Pagination;
import com.github.xerragnaroek.jikai.util.pagi.PrivatePagination;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * 
 */
public class JikaiUserSetup extends ListenerAdapter {
	private static final String yesUni = "U+2705";
	private static final String noUni = "U+274c";
	private JikaiUser ju;
	private Jikai j;
	private Pagination setup;
	private final Logger log;
	private AtomicBoolean ignoreMsgs = new AtomicBoolean(true);
	private AniLinker aniLinker;

	private JikaiUserSetup(JikaiUser ju, Jikai j) {
		this.ju = ju;
		this.j = j;
		log = LoggerFactory.getLogger(JikaiUserSetup.class + "#" + ju.getId());
		makePagination();
	}

	private void makePagination() {
		setup = new PrivatePagination();
		// greetings
		setup.addStage(BotUtils.localedEmbed(ju.getLocale(), "setup_greetings_eb", Pair.of(Arrays.asList("name"), new Object[] { ju.getUser().getName() })));
		stageLang();
		stageConsent();
		stageSkipSetup();
		stageTimeZone();
		stageTitleLanguage();
		stageDaily();
		stageWeekly();
		stageNotifyOnRelease();
		stageNextEpMessage();
		stageReleaseSteps();
		stageLinkAni();
		stageDone();
	}

	private void stageLang() {
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(ju.getLocale().getString("setup_lang_eb_title"));
		List<JikaiLocale> locs = new LinkedList<>(JikaiLocaleManager.getInstance().getLocales());
		Collections.sort(locs);
		List<String> flags = new ArrayList<>(locs.size());
		locs.forEach(jl -> {
			String flag = BotUtils.processUnicode(ju.getLocale().getString("u_flag_uni"));
			flags.add(flag);
			eb.addField(flag, jl.getLanguageName(), true);
		});
		setup.addStage(eb.build(), flags, this::langSelect, null, true);
	}

	private void langSelect(String cp) {
		log.debug(cp);
		JikaiLocale loc = JikaiLocaleManager.getInstance().getLocaleViaFlagUnicode(cp);
		if (loc != null) {
			log.debug(loc.getIdentifier());
			setup.nextStage();
		}
	}

	private void stageConsent() {
		setup.addStage(BotUtils.localedEmbed(ju.getLocale(), "setup_consent_eb", null), Arrays.asList(yesUni, noUni), cp -> yesNoOption(cp, () -> setup.nextStage(), this::noConsent), null, true);
	}

	private void noConsent() {
		log.debug("no consent");
		ju.sendPM(ju.getLocale().getString("setup_consent_no"));
		cancelSetup();
	}

	private void stageSkipSetup() {
		setup.addStage(BotUtils.localedEmbed(ju.getLocale(), "setup_skip_eb", null), Arrays.asList("U+2699", "U+23ed"), this::skipCheck, null, true, false, i -> ignoreMsgs.set(true));
	}

	private void skipCheck(String cp) {
		switch (cp) {
			case "U+2699" -> setup.nextStage();
			case "U+23ed" -> skipSetup();
		}
	}

	private void skipSetup() {
		log.debug("Skip setup!");
		defaultConfig();
		endSetup(true);
	}

	private void defaultConfig() {
		ju.setNotifyToRelease(true);
		ju.setTimeZone(j.getJikaiData().getTimeZone());
		ju.setTitleLanguage(TitleLanguage.ROMAJI);
		ju.setUpdateDaily(true);
		ju.setSendWeeklySchedule(true);
	}

	private void stageTimeZone() {
		setup.addStage(makeTimeZoneEmbed(), Collections.emptyList(), null, null, false, false, i -> {
			ignoreMsgs.set(false);
		});
	}

	private MessageEmbed makeTimeZoneEmbed() {
		return BotUtils.embedBuilder().setTitle(ju.getLocale().getString("setup_time_zone_eb_title")).setDescription(ju.getLocale().getStringFormatted("setup_time_zone_eb_desc", Arrays.asList("tz"), ju.getTimeZone().getId())).build();
	}

	private void stageTitleLanguage() {
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(ju.getLocale().getString("setup_title_lang_eb_title")).setDescription(ju.getLocale().getStringFormatted("setup_title_lang_eb_desc", Arrays.asList("tz"), ju.getTimeZone().getId()));
		setup.addStage(eb.build(), Arrays.asList("U+0031U+fe0fU+20e3", "U+0032U+fe0fU+20e3", "U+0033U+fe0fU+20e3"), this::checkTitleLanguage, null, true, false, i -> ignoreMsgs.set(true));
	}

	private void checkTitleLanguage(String cp) {
		switch (cp) {
			case "U+31U+fe0fU+20e3" -> setTitleLang(TitleLanguage.ENGLISH);
			case "U+32U+fe0fU+20e3" -> setTitleLang(TitleLanguage.NATIVE);
			case "U+33U+fe0fU+20e3" -> setTitleLang(TitleLanguage.ROMAJI);
		}
	}

	private void setTitleLang(TitleLanguage tt) {
		ju.setTitleLanguage(tt);
		setup.nextStage();
	}

	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if (!ignoreMsgs.get() && event.getAuthor().getIdLong() == ju.getId()) {
			switch (setup.getCurrentStageInt()) {
				case 4 -> {
					try {
						ZoneId z = ZoneId.of(event.getMessage().getContentStripped());
						ju.setTimeZone(z);
						ignoreMsgs.set(true);
						setup.nextStage();
					} catch (Exception e) {}
				}
				case 10 -> {
					addStep(event.getMessage().getContentStripped());
				}
				case 11 -> {
					ignoreMsgs.set(true);
					aniLinker = AniLinker.linkAniAccount(ju, event.getMessage().getContentStripped());
					aniLinker.getFuture().thenAccept(b -> setup.nextStage());
				}
			}
		}
	}

	private void stageDaily() {
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(ju.getLocale().getString("setup_daily_eb_title")).setDescription(ju.getLocale().getString("setup_daily_eb_desc"));
		eb.setThumbnail("https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/dailyExample.jpg");
		setup.addStage(eb.build(), Arrays.asList(yesUni, noUni), str -> yesNoOption(str, (b) -> {
			ju.setUpdateDaily(b);
			setup.nextStage();
		}), null, true, false, i -> ignoreMsgs.set(true));
	}

	private void stageWeekly() {
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(ju.getLocale().getString("setup_weekly_eb_title")).setDescription(ju.getLocale().getString("setup_weekly_eb_desc"));
		eb.setThumbnail("https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/weeklyExample.jpg");
		setup.addStage(eb.build(), Arrays.asList(yesUni, noUni), str -> yesNoOption(str, (b) -> {
			ju.setSendWeeklySchedule(b);
			setup.nextStage();
		}), null, true);
	}

	private void stageNotifyOnRelease() {
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(ju.getLocale().getString("setup_notify_eb_title")).setDescription(ju.getLocale().getString("setup_notify_eb_desc"));
		eb.setThumbnail("https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/releaseExample.jpg");
		setup.addStage(eb.build(), Arrays.asList(yesUni, noUni), str -> yesNoOption(str, (b) -> {
			ju.setNotifyToRelease(b);
			setup.nextStage();
		}), null, true);
	}

	private void stageNextEpMessage() {
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(ju.getLocale().getString("setup_next_ep_msg_eb_title")).setDescription(ju.getLocale().getString("setup_next_ep_msg_eb_desc"));
		eb.setThumbnail("https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/nextEpMsgExample.jpg");
		setup.addStage(eb.build(), Arrays.asList(yesUni, noUni), str -> yesNoOption(str, (b) -> {
			ju.setSendNextEpMessage(b);
			setup.nextStage();
		}), null, true);
	}

	private void stageReleaseSteps() {
		setup.addStage(BotUtils.makeSimpleEmbed("You shouldn't see this :)"), Collections.emptyList(), null, null, false, false, i -> {
			setup.editStage(i, makeReleaseStepsEmbed());
			ignoreMsgs.set(false);
			if (aniLinker != null && !aniLinker.getFuture().isDone()) {
				aniLinker.stop(false);
			}
		});
	}

	private MessageEmbed makeReleaseStepsEmbed() {
		return BotUtils.embedBuilder().setTitle(ju.getLocale().getString("setup_release_steps_eb_title")).setDescription(ju.getLocale().getStringFormatted("setup_release_steps_eb_desc", Arrays.asList("steps"), ju.getPreReleaseNotifcationSteps().stream().map(i -> i / 60).collect(Collectors.toSet()))).build();
	}

	private void addStep(String str) {
		try {
			int seconds = BotUtils.stepStringToMins(str) * 60;
			if (!ju.addReleaseStepNoMsg(seconds)) {
				ju.remReleaseStepNoMsg(seconds);
			}
			setup.editCurrentMessage(makeReleaseStepsEmbed());
		} catch (IllegalArgumentException e) {

		}
	}

	private void stageLinkAni() {
		EmbedBuilder eb = BotUtils.embedBuilder();
		eb.setTitle(ju.getLocale().getString("setup_link_ani_eb_title")).setDescription(ju.getLocale().getString("setup_link_ani_eb_desc"));
		setup.addStage(eb.build(), Collections.emptyList(), null, null, false, false, i -> ignoreMsgs.set(false));
	}

	private void stageDone() {
		setup.addStage(BotUtils.makeSimpleEmbed("You shouldn't see this :)"), Collections.emptyList(), null, null, true, true, i -> {
			ignoreMsgs.set(true);
			if (aniLinker != null && !aniLinker.getFuture().isDone()) {
				aniLinker.stop(false);
			}
			EmbedBuilder eb = BotUtils.makeConfigEmbed(ju);
			eb.appendDescription(ju.getLocale().getString("setup_finished"));
			setup.editStage(i, eb.build());
			endSetup(false);
		});
	}

	public void startSetup() {
		log.debug("Running setup");
		ju.getUser().openPrivateChannel().submit().thenAccept(setup::send);
		Core.JDA.addEventListener(this);
	}

	private void endSetup(boolean skip) {
		Core.JDA.removeEventListener(this);
		ju.setSetupCompleted(true);
		if (skip) {
			setup.skipToStage(setup.getStages() - 1);
		}
		setup.end();
	}

	private void cancelSetup() {
		setup.end();
		JikaiUserManager.getInstance().removeUser(ju.getId());
	}

	private void yesNoOption(String cp, Runnable yes, Runnable no) {
		switch (cp) {
			case noUni -> no.run();
			case yesUni -> yes.run();
		}
	}

	private void yesNoOption(String cp, Consumer<Boolean> check) {
		switch (cp) {
			case noUni -> check.accept(false);
			case yesUni -> check.accept(true);
		}
	}

	public static void runSetup(JikaiUser ju, Jikai j) {
		ju.setTimeZone(j.getJikaiData().getTimeZone());
		new JikaiUserSetup(ju, j).startSetup();
	}
}
