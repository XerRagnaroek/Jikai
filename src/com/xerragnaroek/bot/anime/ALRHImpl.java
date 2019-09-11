package com.xerragnaroek.bot.anime;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.config.Config;
import com.xerragnaroek.bot.config.ConfigManager;
import com.xerragnaroek.bot.config.ConfigOption;
import com.xerragnaroek.bot.main.Core;
import com.xerragnaroek.bot.util.BotUtils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

public class ALRHImpl {
	private final Map<String, List<String>> aniAndUser = Collections.synchronizedMap(new TreeMap<>());
	private final String gId;
	private final Config config;
	private String tcId;
	private final Logger log;
	private Map<String, String> roleMap = Collections.synchronizedSortedMap(new TreeMap<>()); /*Title, RoleId*/
	private Map<String, Map<String, String>> msgUcTitMap = Collections.synchronizedMap(new HashMap<>()); /*Message Unicode Role Map*/

	ALRHImpl(String gId) {
		this.gId = gId;
		log = LoggerFactory.getLogger(ALRHImpl.class.getName() + "#" + gId);
		config = ConfigManager.getConfigForGuild(gId);
		init();
		log.debug("AnimeListReactionHandler#{} initialized", gId);
	}

	void init() {
		setTextChannelId(config.getOption(ConfigOption.ROLE_CHANNEL));
		config.registerOptionChangedConsumer(ConfigOption.ROLE_CHANNEL, this::setTextChannelId);
		initMap();
	}

	private void setTextChannelId(String id) {
		tcId = BotUtils.getChannelOrDefault(id, gId).getId();
	}

	public void sendList() {
		Guild g = Core.getJDA().getGuildById(gId);
		TextChannel tc = g.getTextChannelById(tcId);
		log.info("Sending list messages to channel {}", tc.getName() + "#" + tcId);
		ALRHManager.getListMessages().forEach(dto -> handleDTO(g, tc, dto));
	}

	private void handleDTO(Guild g, TextChannel tc, DTO dto) {
		MessageEmbed me = dto.getMessageEmbed();
		Map<String, String> map = dto.getUnicodeTitleMap();
		tc.sendMessage(me).queue(m -> {
			msgUcTitMap.put(m.getId(), map);
			map.forEach((uni, title) -> {
				if (!roleExists(g, title)) {
					log.info("Creating role for {}", title);
					g.createRole().setName(title).setMentionable(true).queue(r -> roleMap.put(title, r.getId()));
				}
				log.debug("Adding reaction {} to message", uni);
				m.addReaction(uni).queue();
			});
		}, e -> log.error("Error occured while setting up the animes role lists", e));
	}

	private void initMap() {
		AnimeBase.getSeasonalAnimes().forEach(a -> {
			aniAndUser.put(a.title, new LinkedList<String>());
		});
	}

	private boolean roleExists(Guild g, String name) {
		return !g.getRolesByName(name, false).isEmpty();
	}
}
