package com.github.xerragnaroek.jikai.core;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class SetupHelper extends ListenerAdapter {

    private final Guild g;
    private boolean listen = false;
    private TextChannel setTc;
    private final Logger log;

    private SetupHelper(Guild g) {
        this.g = g;
        Core.JDA.addEventListener(this);
        log = LoggerFactory.getLogger(SetupHelper.class + "#" + g.getId());
    }

    public static void runSetup(Guild g) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Core.ERROR_LOG.error("", e);
        }
        new SetupHelper(g).runSetup();
    }

    private TextChannel makeSetupChannel() {
        return g.createTextChannel("jikai_setup").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.EMPTY_PERMISSIONS), List.of(Permission.VIEW_CHANNEL)).addPermissionOverride(g.retrieveOwner().complete(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE), Arrays.asList(Permission.EMPTY_PERMISSIONS)).addPermissionOverride(g.getSelfMember(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE), Arrays.asList(Permission.EMPTY_PERMISSIONS)).complete();
    }

    private void runSetup() {
        log.info("Running setup");
        setTc = makeSetupChannel();
        log.debug("Made setup channel");
        setTc.sendMessage(g.retrieveOwner().complete().getAsMention()).complete();
        String mb = """

                This is where the bot setup will take place.
                The bot will create 4 textchannels:
                **jikai_list** - for the anime list
                **jikai_schedule** - for the anime release schedule
                **jikai_anime** - for upcoming release notifications
                **jikai_info** - for status updates and information concerning the bot
                Furthermore also set these settings:
                **prefix** = '!' - what needs to be written before a command for the bot to recognize it. E.g. !help
                **timezone** = 'Europe/Berlin' - the timezone that will be used to adjust release updates.
                You can change the latter settings via the set commands (see !help for more info)!
                Feel free to move or rename the channels, the bot is using their unique ID, so the name doesn't matter.Once the setup is done, the bot will send the anime list and the schedule.
                Commence setup? *yes(y)*""";
        setTc.sendMessage(MessageCreateData.fromContent(mb)).complete();
        listen = true;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (listen) {
            if (event.getGuild().getIdLong() == g.getIdLong()) {
                MessageChannelUnion chan = event.getChannel();
                if (chan.getType() == ChannelType.TEXT && chan.asTextChannel().equals(setTc)) {
                    if (!event.getAuthor().equals(g.getSelfMember().getUser())) {
                        continueSetup(event.getMessage().getContentDisplay());
                    }
                }
            }
        }
    }

    private void continueSetup(String msg) {
        if (msg.equalsIgnoreCase("y") || msg.equalsIgnoreCase("yes")) {
            listen = false;
            Core.JDA.removeEventListener(this);
            setup();
        }
    }

    private void setup() {
        listen = false;
        log.info("Owner agreed to setup");
        Jikai j = Core.JM.registerNew(g);
        JikaiData jd = j.getJikaiData();
        Category cat = g.createCategory("jikai").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_ADD_REACTION), List.of(Permission.MESSAGE_SEND)).addPermissionOverride(g.getSelfMember(), Permission.ALL_CHANNEL_PERMISSIONS, 0L).complete();
        TextChannel tc = cat.createTextChannel("jikai_list").complete();
        log.debug("Made jikai_list channel");
        jd.setListChannelId(tc.getIdLong(), TitleLanguage.ROMAJI);
        tc = cat.createTextChannel("jikai_schedule").complete();
        log.debug("Made jikai_schedule channel");
        jd.setScheduleChannelId(tc.getIdLong());
        tc = cat.createTextChannel("jikai_anime").complete();
        log.debug("Made jikai_anime channel");
        jd.setAnimeChannelId(tc.getIdLong());
        tc = cat.createTextChannel("jikai_info").complete();
        log.debug("Made jikai_info channel");
        jd.setInfoChannelId(tc.getIdLong());
        jd.setSetupCompleted(true);
        jd.save(true);
        // j.setALRH(Core.JM.getALHRM().registerNew(g));
        log.info("Setup completed");
        setTc.sendMessage("The setup is complete. Commands are by default " + (jd.areCommandsEnabled() ? "enabled" : "disabled") + ".\nYou can change that by calling !enable/disable_commands").complete();
        setTc.sendMessage("Send `!help` for a list of all commands you have permissions to run (which are all because you're the owner).").complete();
        setTc.sendMessage("Also I ask you to set the bot role ('Jikai') color to #12e5a8 or R18 G229 B168. Thank you!").complete();
        // Core.executeLogException(() -> j.getALRHandler().sendList());
        Core.executeLogException(() -> ScheduleManager.sendScheduleToJikai(j));
    }
}
