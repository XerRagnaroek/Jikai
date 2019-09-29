package com.xerragnaroek.bot.anime.alrh;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.core.Core;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
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
		log.info("Deleting old messages and data");
		alrh.alrhDB.forEachMessage((id, dat) -> {
			tc.deleteMessageById(id).queue(	v -> log.info("Deleted old list message"),
											e -> log.error("Failed deleting old message"));
		});
		alrh.alrhDB.clearData();
		dtos.forEach(dto -> handleDTO(g, tc, dto));
	}

	private int calcExpectedSuccesses(Set<DTO> dtos) {
		int c = 0;
		for (DTO dto : dtos) {
			//roles,reactions and 1 for the message
			c += dto.getALRHData().size() * 2 + 1;
		}
		return c;
	}

	private void handleDTO(Guild g, TextChannel tc, DTO dto) {
		Message me = dto.getMessage();
		Set<ALRHData> data = dto.getALRHData();
		log.debug("Sending list message...");
		tc.sendMessage(me).queue(m -> {
			incrementSuccesses();
			alrh.alrhDB.setDataForMessage(m.getId(), data);
			data.forEach(alrhd -> {
				alrhd.setTextChannelId(alrh.tcId);
				String title = alrhd.getTitle();
				List<Role> roles = g.getRolesByName(title, false);
				if (roles.isEmpty()) {
					log.info("Creating role for {}", title);
					g.createRole().setName(title).setMentionable(true).setPermissions(0l).queue(r -> {
						if (r != null) {
							log.debug("Storing role {} with id {}", r.getName(), r.getId());
							alrhd.setRoleId(r.getId());
							alrh.alrhDB.addALRHData(alrhd);
							incrementSuccesses();
						}
					}, rerr -> log.error("Failed creating role for {}", title, rerr));
				} else {
					log.debug("Role {} already exists, getting id", title);
					alrhd.setRoleId(roles.get(0).getId());
					incrementSuccesses();
				}
				String uniCP = alrhd.getUnicodeCodePoint();
				log.debug("Adding reaction {} to message", uniCP);
				m.addReaction(uniCP).queue(v -> {
					incrementSuccesses();
					log.debug("Added reaction {} to message", uniCP);
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
