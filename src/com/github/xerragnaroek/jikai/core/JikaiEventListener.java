
package com.github.xerragnaroek.jikai.core;

import static com.github.xerragnaroek.jikai.core.Core.JM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.user.JUCommandHandler;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiSetup;
import com.github.xerragnaroek.jikai.user.EpisodeTracker;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class JikaiEventListener extends ListenerAdapter {
	private final Logger log = LoggerFactory.getLogger(JikaiEventListener.class);

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
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

	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		JikaiUser ju = JikaiUserManager.getInstance().getUser(event.getAuthor().getIdLong());
		if (ju != null && ju.isSetupCompleted()) {
			log.debug("Received pm from known jikai user");
			JUCommandHandler.handleMessage(ju, event.getMessage().getContentRaw());
		}
	}

	@Override
	public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
		if (!event.getUser().isBot()) {
			Jikai j = JM.get(event.getGuild());
			if (j.hasCompletedSetup() && JikaiUserManager.getInstance().isKnownJikaiUser(event.getUserIdLong())) {
				j.getALRHandler().handleReactionAdded(event);
			}
		}
	}

	@Override
	public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
		Jikai j = JM.get(event.getGuild());
		if (j.hasCompletedSetup() && JikaiUserManager.getInstance().isKnownJikaiUser(event.getUserIdLong())) {
			j.getALRHandler().handleReactionRemoved(event);
		}
	}

	@Override
	public void onGuildMessageReactionRemoveAll(GuildMessageReactionRemoveAllEvent event) {
		Jikai j = JM.get(event.getGuild());
		if (j.hasCompletedSetup()) {
			j.getALRHandler().handleReactionRemovedAll(event);
		}
	}

	@Override
	public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
		JikaiUser ju = JikaiUserManager.getInstance().getUser(event.getUserIdLong());
		if (ju != null) {
			ReactionEmote re = event.getReactionEmote();
			if (re.isEmoji()) {
				if (re.getAsCodepoints().equals(EpisodeTracker.WATCHED_EMOJI_UNICODE)) {
					EpisodeTracker.getTracker(ju).handleEmojiReacted(event);
				}
			}
		}
	}

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
			jum.registerUser(id, Core.JM.get(event.getGuild().getIdLong()));
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
	public void onReconnected(ReconnectedEvent event) {
		log.debug("Reconnected!");
		JikaiUserManager.getInstance().cachePrivateChannels();
	}
}
