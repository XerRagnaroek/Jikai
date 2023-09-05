package com.github.xerragnaroek.jikai.core;

import com.github.xerragnaroek.jikai.commands.user.JUCommandHandler;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiSetup;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.ButtonInteractor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.SessionRecreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.github.xerragnaroek.jikai.core.Core.JM;

public class JikaiEventListener extends ListenerAdapter {
    private final Logger log = LoggerFactory.getLogger(JikaiEventListener.class);
    private final Map<String, ButtonInteractor> btnInteractors = new HashMap<>();

    public void registerButtonInteractor(ButtonInteractor bi) {
        if (btnInteractors.containsKey(bi.getIdentifier())) {
            throw new IllegalStateException("This identifier is already in use!");
        }
        btnInteractors.put(bi.getIdentifier(), bi);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromGuild()) {
            onGuildMessageReceived(event);
        } else {
            onPrivateMessageReceived(event);
        }
    }


    private void onGuildMessageReceived(MessageReceivedEvent event) {
        // ignore bots
        if (!event.getAuthor().isBot()) {
            Guild g = event.getGuild();
            if (JM.hasManagerFor(g)) {
                Jikai j = JM.get(g);
                if (j.hasCompletedSetup()) {
                    j.getCommandHandler().handleMessage(event);
                }
            }
        }

    }


    private void onPrivateMessageReceived(MessageReceivedEvent event) {
        JikaiUser ju = JikaiUserManager.getInstance().getUser(event.getAuthor().getIdLong());
        if (ju != null && ju.isSetupCompleted()) {
            log.debug("Received pm from known jikai user");
            JUCommandHandler.handleMessage(ju, event.getMessage().getContentRaw());
        }
    }

    /*
     * @Override
     * public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
     * log.debug(event.getReactionEmote().getName());
     * if (!event.getUser().isBot()) {
     * Jikai j = JM.get(event.getGuild());
     * if (j.hasCompletedSetup() &&
     * JikaiUserManager.getInstance().isKnownJikaiUser(event.getUserIdLong())) {
     * ALRHandler alrh = j.geAnimeListHandler(event.getChannel().getIdLong());
     * if (alrh != null) {
     * // reaction was added in a list channel
     * alrh.handleReactionAdded(event);
     * }
     * }
     * }
     * }
     * @Override
     * public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
     * Jikai j = JM.get(event.getGuild());
     * if (j.hasCompletedSetup() &&
     * JikaiUserManager.getInstance().isKnownJikaiUser(event.getUserIdLong())) {
     * ALRHandler alrh = j.geAnimeListHandler(event.getChannel().getIdLong());
     * if (alrh != null) {
     * // reaction was added in a list channel
     * alrh.handleReactionRemoved(event);
     * }
     * }
     * }
     * @Override
     * public void onGuildMessageReactionRemoveAll(GuildMessageReactionRemoveAllEvent event) {
     * Jikai j = JM.get(event.getGuild());
     * if (j.hasCompletedSetup()) {
     * j.geAnimeListHandler(event.getChannel().getIdLong()).handleReactionRemovedAll(event);
     * }
     * }
     */

    /*
     * @Override
     * public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
     * JikaiUser ju = JikaiUserManager.getInstance().getUser(event.getUserIdLong());
     * if (ju != null) {
     * ReactionEmote re = event.getReactionEmote();
     * if (re.isEmoji()) {
     * }
     * }
     * }
     */
    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        long gId = event.getGuild().getIdLong();
        if (!JM.isKnownGuild(gId)) {
            Guild g = event.getGuild();
            log.info("New Guild {}#{}, running setup", g.getName(), g.getId());
            // SetupHelper.runSetup(g);
            JikaiSetup.runSetup(g);
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        long id = event.getGuild().getIdLong();
        log.info("Guild {} left", event.getGuild().getName());
        JM.remove(id);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        long id = event.getUser().getIdLong();
        JikaiUserManager jum = JikaiUserManager.getInstance();
        if (!jum.isKnownJikaiUser(id)) {
            log.info("New member joined, regestering new JikaiUser! [{}]", id);
            jum.registerNewUser(id, Core.JM.get(event.getGuild().getIdLong()));
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        long id = event.getUser().getIdLong();
        JikaiUserManager jum = JikaiUserManager.getInstance();
        if (jum.isKnownJikaiUser(id)) {
            log.info("Member {} has been removed!", id);
            jum.removeUser(id);
        }
    }


    @Override
    public void onSessionRecreate(SessionRecreateEvent event) {
        log.debug("Reconnected!");
        JikaiUserManager.getInstance().cachePrivateChannels();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        log.debug("Button clicked: {}", event.getId());
        String id = event.getButton().getId();
        String[] split = id.split(":");
        if (btnInteractors.containsKey(split[0])) {
            btnInteractors.get(split[0]).handleButtonClick(ArrayUtils.subarray(split, 1, split.length), event);
        }
    }


}
