package com.github.xerragnaroek.jikai.user;

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
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 *
 */
public class JikaiUserSetup extends ListenerAdapter {
    private static final String yesUni = "U+2705";
    private static final String noUni = "U+274c";
    private final JikaiUser ju;
    private final Jikai j;
    private Pagination setup;
    private final Logger log;
    private final AtomicBoolean ignoreMsgs = new AtomicBoolean(true);
    private AniLinker aniLinker;
    private ScheduledFuture<?> timeOut;

    private JikaiUserSetup(JikaiUser ju, Jikai j) {
        this.ju = ju;
        this.j = j;
        log = LoggerFactory.getLogger(JikaiUserSetup.class + "#" + ju.getId());
        makePagination();
    }

    private void makePagination() {
        setup = new PrivatePagination();
        // greetings
        setup.addStage(BotUtils.localedEmbed(ju.getLocale(), "setup_greetings_eb", Pair.of(List.of("name"), new Object[]{ju.getUser().getName()})));
        stageLang();
        stageConsent();
        stageSkipSetup();
        stageTimeZone();
        stageTitleLanguage();
        stageDaily();
        stageWeekly();
        stageNotifyOnRelease();
        stageNextEpMessage();
        stageShowAdult();
        stageReleaseSteps();
        stageLinkAni();
        stageDone();
    }

    private void editCurStage(int i) {
        MessageEmbed meb = null;
        switch (i) {
            case 1 -> meb = langMeb().getLeft();
            case 2 -> meb = consentMeb();
            case 3 -> meb = skipMeb();
            case 4 -> meb = makeTimeZoneEmbed();
            case 5 -> meb = titleLangMeb();
            case 6 -> meb = dailyMeb();
            case 7 -> meb = weeklyMeb();
            case 8 -> meb = notifyReleaseMeb();
            case 9 -> meb = nextEpMeb();
            case 10 -> meb = adultMeb();
            case 11 -> meb = makeReleaseStepsEmbed();
            case 12 -> meb = linkAniMeb();
            case 13 -> meb = doneMeb();
        }
        setup.editStage(i, meb);
    }

    private void stageLang() {
        Pair<MessageEmbed, List<String>> pair = langMeb();
        setup.addStage(pair.getLeft(), pair.getRight(), this::langSelect, null, true, false, this::editCurStage);
    }

    private Pair<MessageEmbed, List<String>> langMeb() {
        EmbedBuilder eb = BotUtils.embedBuilder();
        eb.setTitle(ju.getLocale().getString("setup_lang_eb_title"));
        List<JikaiLocale> locs = new LinkedList<>(JikaiLocaleManager.getInstance().getLocales());
        Collections.sort(locs);
        List<String> flags = new ArrayList<>(locs.size());
        locs.forEach(jl -> {
            String flag = BotUtils.processUnicode(jl.getString("u_flag_uni"));
            flags.add(flag);
            eb.addField(flag, jl.getLanguageName(), true);
        });
        return Pair.of(eb.build(), flags);
    }

    private void langSelect(String cp) {
        log.debug(cp);
        JikaiLocale loc = JikaiLocaleManager.getInstance().getLocaleViaFlagUnicode(cp);
        if (loc != null) {
            log.debug(loc.getIdentifier());
            ju.setLocale(loc);
            setup.nextStage();
        }
    }

    private void stageConsent() {
        setup.addStage(consentMeb(), Arrays.asList(yesUni, noUni), cp -> yesNoOption(cp, () -> setup.nextStage(), this::noConsent), null, true, false, this::editCurStage);
    }

    private MessageEmbed consentMeb() {
        return BotUtils.localedEmbed(ju.getLocale(), "setup_consent_eb", null);
    }

    private void noConsent() {
        log.debug("no consent");
        ju.sendPM(ju.getLocale().getString("setup_consent_no"));
        cancelSetup();
    }

    private void stageSkipSetup() {
        setup.addStage(skipMeb(), Arrays.asList("U+2699", "U+23ed"), this::skipCheck, null, true, false, i -> {
            ignoreMsgs.set(true);
            editCurStage(i);
        });
    }

    private MessageEmbed skipMeb() {
        return BotUtils.localedEmbed(ju.getLocale(), "setup_skip_eb", null);
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
        ju.setShowAdult(false);
    }

    private void stageTimeZone() {
        setup.addStage(makeTimeZoneEmbed(), Collections.emptyList(), null, null, false, false, i -> {
            ignoreMsgs.set(false);
            editCurStage(i);
        });
    }

    private MessageEmbed makeTimeZoneEmbed() {
        return BotUtils.embedBuilder().setTitle(ju.getLocale().getString("setup_time_zone_eb_title")).setDescription(ju.getLocale().getStringFormatted("setup_time_zone_eb_desc", List.of("tz"), ju.getTimeZone().getId())).build();
    }

    private void stageTitleLanguage() {
        setup.addStage(BotUtils.makeSimpleEmbed("Should never be seen!"), Arrays.asList("U+0031U+fe0fU+20e3", "U+0032U+fe0fU+20e3", "U+0033U+fe0fU+20e3"), this::checkTitleLanguage, null, true, false, i -> {
            ignoreMsgs.set(true);
            editCurStage(i);
        });
    }

    private MessageEmbed titleLangMeb() {
        return BotUtils.embedBuilder().setTitle(ju.getLocale().getString("setup_title_lang_eb_title")).setDescription(ju.getLocale().getString("setup_title_lang_eb_desc")).build();
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
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE) {
            if (!ignoreMsgs.get() && event.getAuthor().getIdLong() == ju.getId()) {
                switch (setup.getCurrentStageInt()) {
                    case 4 -> {
                        try {
                            ZoneId z = ZoneId.of(event.getMessage().getContentStripped());
                            ju.setTimeZone(z);
                            ignoreMsgs.set(true);
                            setup.nextStage();
                        } catch (Exception e) {
                        }
                    }
                    case 11 -> {
                        addStep(event.getMessage().getContentStripped());
                    }
                    case 12 -> {
                        ignoreMsgs.set(true);
                        aniLinker = AniLinker.linkAniAccount(ju, event.getMessage().getContentStripped());
                        aniLinker.getFuture().thenAccept(b -> setup.nextStage());
                    }
                }
            }
        }
    }

    private void stageDaily() {
        setup.addStage(dailyMeb(), Arrays.asList(yesUni, noUni), str -> yesNoOption(str, (b) -> {
            ju.setUpdateDaily(b);
            setup.nextStage();
        }), null, true, false, i -> {
            ignoreMsgs.set(true);
            editCurStage(i);
        });
    }

    private MessageEmbed dailyMeb() {
        EmbedBuilder eb = BotUtils.embedBuilder();
        eb.setTitle(ju.getLocale().getString("setup_daily_eb_title")).setDescription(ju.getLocale().getString("setup_daily_eb_desc"));
        eb.setThumbnail("https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/dailyExample.jpg");
        return eb.build();
    }

    private void stageWeekly() {
        setup.addStage(weeklyMeb(), Arrays.asList(yesUni, noUni), str -> yesNoOption(str, (b) -> {
            ju.setSendWeeklySchedule(b);
            setup.nextStage();
        }), null, true, false, this::editCurStage);
    }

    private MessageEmbed weeklyMeb() {
        EmbedBuilder eb = BotUtils.embedBuilder();
        eb.setTitle(ju.getLocale().getString("setup_weekly_eb_title")).setDescription(ju.getLocale().getString("setup_weekly_eb_desc"));
        eb.setThumbnail("https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/weeklyExample.jpg");
        return eb.build();
    }

    private void stageNotifyOnRelease() {
        setup.addStage(notifyReleaseMeb(), Arrays.asList(yesUni, noUni), str -> yesNoOption(str, (b) -> {
            ju.setNotifyToRelease(b);
            setup.nextStage();
        }), null, true, false, this::editCurStage);
    }

    private MessageEmbed notifyReleaseMeb() {
        EmbedBuilder eb = BotUtils.embedBuilder();
        eb.setTitle(ju.getLocale().getString("setup_notify_eb_title")).setDescription(ju.getLocale().getString("setup_notify_eb_desc"));
        eb.setThumbnail("https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/releaseExample.jpg");
        return eb.build();
    }

    private void stageNextEpMessage() {
        setup.addStage(nextEpMeb(), Arrays.asList(yesUni, noUni), str -> yesNoOption(str, (b) -> {
            ju.setSendNextEpMessage(b);
            setup.nextStage();
        }), null, true, false, this::editCurStage);
    }

    private MessageEmbed nextEpMeb() {
        EmbedBuilder eb = BotUtils.embedBuilder();
        eb.setTitle(ju.getLocale().getString("setup_next_ep_msg_eb_title")).setDescription(ju.getLocale().getString("setup_next_ep_msg_eb_desc"));
        eb.setThumbnail("https://raw.githubusercontent.com/XerRagnaroek/Jikai/dev/doc/nextEpMsgExample.jpg");
        return eb.build();
    }

    private MessageEmbed adultMeb() {
        return BotUtils.localedEmbed(ju.getLocale(), "setup_adult_eb", Pair.of(List.of("adult"), new Object[]{ju.getLocale().getYesOrNo(ju.isShownAdult())}));
    }

    private void stageShowAdult() {
        setup.addStage(adultMeb(), Arrays.asList(yesUni, noUni), str -> yesNoOption(str, b -> {
            ju.setShowAdult(b);
            setup.nextStage();
        }), null, true, false, i -> editCurStage(i));
    }

    private void stageReleaseSteps() {
        setup.addStage(BotUtils.makeSimpleEmbed("You shouldn't see this :)"), Collections.emptyList(), null, null, false, false, i -> {
            editCurStage(i);
            ignoreMsgs.set(false);
            if (aniLinker != null && !aniLinker.getFuture().isDone()) {
                aniLinker.stop(false);
            }
        });
    }

    private MessageEmbed makeReleaseStepsEmbed() {
        return BotUtils.embedBuilder().setTitle(ju.getLocale().getString("setup_release_steps_eb_title")).setDescription(ju.getLocale().getStringFormatted("setup_release_steps_eb_desc", List.of("steps"), ju.getPreReleaseNotifcationSteps().stream().map(i -> i / 60).collect(Collectors.toSet()))).build();
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
        setup.addStage(linkAniMeb(), Collections.emptyList(), null, null, false, false, i -> {
            ignoreMsgs.set(false);
            editCurStage(i);
        });
    }

    private MessageEmbed linkAniMeb() {
        return BotUtils.embedBuilder().setTitle(ju.getLocale().getString("setup_link_ani_eb_title")).setDescription(ju.getLocale().getString("setup_link_ani_eb_desc")).build();
    }

    private void stageDone() {
        setup.addStage(BotUtils.makeSimpleEmbed("You shouldn't see this :)"), Collections.emptyList(), null, null, true, true, i -> {
            ignoreMsgs.set(true);
            if (aniLinker != null && !aniLinker.getFuture().isDone()) {
                aniLinker.stop(false);
            }
            editCurStage(i);
            endSetup(false);
        });
    }

    private MessageEmbed doneMeb() {
        EmbedBuilder eb = BotUtils.makeConfigEmbed(ju);
        eb.appendDescription(ju.getLocale().getString("setup_finished"));
        return eb.build();
    }

    public void startSetup() {
        log.debug("Running setup");
        ju.getUser().openPrivateChannel().submit().thenAccept(setup::send);
        Core.JDA.addEventListener(this);
        timeOut = Core.EXEC.schedule(() -> timeOut(), 1, TimeUnit.HOURS);
    }

    private void endSetup(boolean skip) {
        timeOut.cancel(true);
        Core.JDA.removeEventListener(this);
        ju.setSetupCompleted(true);
        if (skip) {
            setup.skipToStage(setup.getStages() - 1);
        }
        BotUtils.addJikaiUserRole(ju);
        setup.end();
    }

    private void cancelSetup() {
        setup.end();
        JikaiUserManager.getInstance().removeUser(ju.getId());
        timeOut.cancel(true);
    }

    private void timeOut() {
        log.debug("Setup timed out!");
        setup.addStage(BotUtils.makeSimpleEmbed(ju.getLocale().getString("setup_timed_out")), Collections.emptyList(), null, null, true, true);
        setup.skipToStage(setup.getStages() - 1);
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
