package com.xerragnaroek.bot.anime.alrh;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.core.Core;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class ALHandler {

	private ALRHandler alrh;
	private AtomicInteger successes = new AtomicInteger(0); //Successful RestActions
	private int expectedNumSuccesses = 0;
	private final Logger log;

	ALHandler(ALRHandler alrh) {
		this.alrh = alrh;
		log = LoggerFactory.getLogger(ALHandler.class.getName() + "#" + alrh.gId);
	}

	void sendList() {
		Guild g = Core.getJDA().getGuildById(alrh.gId);
		TextChannel tc = g.getTextChannelById(alrh.tcId);
		log.info("Sending list messages to channel {}", tc.getName() + "#" + alrh.tcId);
		Set<DTO> dtos = ALRHManager.getListMessages();
		successes.set(0);
		expectedNumSuccesses = calcExpectedSuccesses(dtos);
		dtos.forEach(dto -> handleDTO(g, tc, dto));
	}

	private int calcExpectedSuccesses(Set<DTO> dtos) {
		int c = 0;
		for (DTO dto : dtos) {
			//roles,reactions and 1 for the message
			c += dto.getUnicodeTitleMap().size() * 2 + 1;
		}
		return c;
	}

	private void handleDTO(Guild g, TextChannel tc, DTO dto) {
		Message me = dto.getMessage();
		Map<String, String> map = dto.getUnicodeTitleMap();
		log.debug("Sending list message...");
		tc.sendMessage(me).queue(m -> {
			incrementSuccesses();
			alrh.msgUcTitMap.put(m.getId(), map);
			map.forEach((uni, title) -> {
				if (!alrh.roleExists(g, title)) {
					log.info("Creating role for {}", title);
					g.createRole().setName(title).setMentionable(true).setPermissions(0l).queue(r -> {
						if (r != null) {
							log.debug("Storing role {} with id {}", r.getName(), r.getId());
							alrh.roleMap.put(title, r.getId());
							incrementSuccesses();
						}
					}, rerr -> log.error("Failed creating role for {}", title, rerr));
				} else {
					incrementSuccesses();
				}
				log.debug("Adding reaction {} to message", uni);
				m.addReaction(uni).queue(v -> {
					incrementSuccesses();
					log.debug("Added reaction {} to message", uni);
					if (successes.get() == expectedNumSuccesses) {
						alrh.storeData();
						successes.set(0);
					}
				});
			});
		}, e -> log.error("Failed sending list message", e));
	}

	private void incrementSuccesses() {
		log.debug("Successful sends: {}/{}", successes.incrementAndGet(), expectedNumSuccesses);
	}

}
