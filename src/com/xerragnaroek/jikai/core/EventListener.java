/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xerragnaroek.jikai.core;

import static com.xerragnaroek.jikai.core.Core.JM;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.commands.user.JUCommandHandler;
import com.xerragnaroek.jikai.jikai.Jikai;
import com.xerragnaroek.jikai.user.JikaiUser;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventListener extends ListenerAdapter {
	private final Logger log = LoggerFactory.getLogger(EventListener.class);

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		//ignore bots
		if (!event.getAuthor().isBot()) {
			if (event.isFromGuild()) {
				Jikai j = JM.get(event.getGuild());
				if (j.hasCompletedSetup()) {
					runAsync(() -> j.getCommandHandler().handleMessage(event));
				}
			} else if (event.isFromType(ChannelType.PRIVATE)) {
				JikaiUser ju = Jikai.getUserManager().getUser(event.getAuthor().getIdLong());
				if (ju != null && ju.isSetupCompleted()) {
					log.debug("Received pm from known jikai user");
					runAsync(() -> JUCommandHandler.handleMessage(ju, event.getMessage().getContentRaw()));
				}
			}
		}
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!event.getUser().isBot() && event.isFromType(ChannelType.TEXT)) {
			Jikai j = JM.get(event.getGuild());
			if (j.hasCompletedSetup()) {
				runAsync(() -> j.getALRHandler().handleReactionAdded(event));
			}
		}
	}

	@Override
	public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
		if (event.isFromGuild()) {
			Jikai j = JM.get(event.getGuild());
			if (j.hasCompletedSetup()) {
				runAsync(() -> j.getALRHandler().handleReactionRemoved(event));
			}
		}
	}

	@Override
	public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
		if (event.isFromGuild()) {
			Jikai j = JM.get(event.getGuild());
			if (j.hasCompletedSetup()) {
				runAsync(() -> j.getALRHandler().handleReactionRemovedAll(event));
			}
		}
	}

	@Override
	public void onReady(ReadyEvent event) {
		/*
		 * log.info("Initializing ALRHs"); //runAsync(() -> ALRHManager.init()); ALRHManager.init();
		 * GDM.startSaveThread(10, TimeUnit.SECONDS);
		 */
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		long gId = event.getGuild().getIdLong();
		if (!JM.isKnownGuild(gId)) {
			onNewGuild(event.getGuild());
		}
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		long id = event.getGuild().getIdLong();
		log.info("Guild {} left", event.getGuild().getName());
		JM.remove(id);

	}

	private void onNewGuild(Guild g) {
		log.info("New Guild {}#{}, running setup", g.getName(), g.getId());
		runAsync(() -> SetupHelper.runSetup(g));
	}

	private void runAsync(Runnable r) {
		CompletableFuture.runAsync(r, Core.EXEC).whenComplete((v, e) -> {
			if (e != null) {
				Core.logThrowable(e);
			}
		});
	}
}
