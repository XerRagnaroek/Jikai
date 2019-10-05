package com.xerragnaroek.jikai.anime.alrh;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.core.Core;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Handles the sending and updating of the anime list. <br>
 * Only used by the ALRHandler.
 * 
 * @author XerRagnaroek
 *
 */
class ALHandler {

	private ALRHandler alrh;
	private int expectedNumSuccesses = 0;
	private final Logger log;
	private ALRHDataBase alrhDB;
	private AtomicBoolean sending = new AtomicBoolean(false);
	private AtomicInteger successes = new AtomicInteger(0); //Successful RestActions

	ALHandler(ALRHandler alrh) {
		this.alrh = alrh;
		log = LoggerFactory.getLogger(ALHandler.class.getName() + "#" + alrh.gId);
		alrhDB = alrh.alrhDB;
	}

	/**
	 * Whether the list is currently being sent, or not.
	 * 
	 * @return true - if the list is currently being sent <br>
	 *         false - nothing is being sent
	 */
	boolean isSending() {
		return sending.get();
	}

	/**
	 * Send the anime list.<br>
	 * Deletes the old list (if existing) and replaces it with the new one. <br>
	 * Once all RestActions have successfully completed, ALRH is set to updated.
	 */
	void sendList() {
		sending.set(true);
		Guild g = Core.JDA.getGuildById(alrh.gId);
		TextChannel tc = g.getTextChannelById(alrh.tcId.get());
		log.info("Sending list messages to channel {}", tc.getName() + "#" + alrh.tcId);
		Set<DTO> dtos = Core.ALRHM.getListMessages();
		successes.set(0);
		expectedNumSuccesses = calcExpectedSuccesses(dtos);
		log.info("Deleting old messages and data");
		alrhDB.forEachMessage((id, dat) -> {
			tc.deleteMessageById(id).queue(v -> log.info("Deleted old list message"));
		});
		alrhDB.clearUcMsgMap();
		dtos.forEach(dto -> handleDTO(g, tc, dto));
	}

	/**
	 * Calculates how many RestActions will be made by sending the list and thus how many successes
	 * are to be expected.
	 * 
	 * @param dtos
	 *            - a set containing the DTOs with the data to send
	 * @return How many successful RestActions will be had once everything is sent
	 */
	private int calcExpectedSuccesses(Set<DTO> dtos) {
		int c = 0;
		for (DTO dto : dtos) {
			//roles,reactions and 1 for the message
			//c += dto.getALRHData().size() * 2 + 1;
			c += dto.getALRHData().size() + 1;
		}
		return c;
	}

	/**
	 * Handles the actual sending of the list messages.
	 * 
	 * @param g
	 *            - The Guild to send to
	 * @param tc
	 *            - The TextChannel to send to
	 * @param dto
	 *            - The DTO containing the data to send
	 */
	private void handleDTO(Guild g, TextChannel tc, DTO dto) {
		Message me = dto.getMessage();
		Set<ALRHData> data = dto.getALRHData();
		log.debug("Sending list message...");
		tc.sendMessage(me).queue(m -> {
			incrementSuccesses();
			data.forEach(alrhd -> {
				if (alrhDB.isReacted(alrhd)) {
					alrhd.setReacted(true);
				}
				alrhd.setTextChannelId(alrh.tcId.get());
				/*List<Role> roles = g.getRolesByName(title, false);
				if (roles.isEmpty()) {
					log.info("Creating role for {}", title);
					g.createRole().setName(title).setMentionable(true).setPermissions(0l).queue(r -> {
						if (r != null) {
							log.debug("Storing role {} with id {}", r.getName(), r.getId());
							alrhd.setRoleId(r.getId());
							alrhDB.addALRHData(alrhd);
							incrementSuccesses();
						}
					}, rerr -> log.error("Failed creating role for {}", title, rerr));
				} else {
					log.debug("Role {} already exists, getting id", title);
					alrhd.setRoleId(roles.get(0).getId());
					incrementSuccesses();
				}*/
				String uniCP = alrhd.getUnicodeCodePoint();
				log.debug("Adding reaction {} to message", uniCP);
				m.addReaction(uniCP).queue(v -> {
					incrementSuccesses();
					log.debug("Added reaction {} to message", uniCP);
					if (successes.get() == expectedNumSuccesses) {
						alrh.dataChanged();
						successes.set(0);
						alrh.gData.save(true);
						sending.set(false);
					}
				});
			});
			alrhDB.setDataForMessage(m.getId(), data);
		});
	}

	private void incrementSuccesses() {
		log.debug("Successful sends: {}/{}", successes.incrementAndGet(), expectedNumSuccesses);
	}

	void update() {
		sendList();
	}
}
