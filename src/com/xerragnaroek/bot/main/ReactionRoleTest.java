package com.xerragnaroek.bot.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ReactionRoleTest extends ListenerAdapter {
	private final String id = "621042296774852619";
	private final Logger log = LoggerFactory.getLogger(ReactionRoleTest.class);

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		log.debug("Reaction added to msg {}", event.getMessageId());
		if (event.getMessageId().equals(id)) {
			Guild g = event.getGuild();
			log.debug("Reacted with {}", event.getReactionEmote().getAsCodepoints());
		}
	}

	@Override
	public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
		log.debug("Reaction removed from msg {}", event.getMessageId());
		if (event.getMessageId().equals(id)) {
			Guild g = event.getGuild();
			g.removeRoleFromMember(event.getMember(), g.getRolesByName("test123", false).get(0)).submit().whenComplete((v, e) -> {
				log.error("Error removing role", e);
			});
		}
	}
}
