package com.github.xerragnaroek.jikai.commands.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.UnicodeUtils;
import com.github.xerragnaroek.jikai.util.pagi.Pagination;
import com.github.xerragnaroek.jikai.util.pagi.PrivatePagination;

import net.dv8tion.jda.api.EmbedBuilder;

/**
 * 
 */
public class LinksCommand implements JUCommand {

	@Override
	public String getName() {
		return "links";
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_links";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		Set<Long> linkedUsers = ju.getLinkedToUsers();
		Collection<List<Long>> partitioned = BotUtils.partitionCollection(linkedUsers, 10);
		Pagination page = new PrivatePagination();
		Iterator<List<Long>> it = partitioned.iterator();
		int size = partitioned.size();
		for (int c = 0; c < size; c++) {
			List<Long> l = it.next();
			int[] count = { 1 };
			Map<String, Long> uniIdMap = new HashMap<>();
			l.stream().map(id -> JikaiUserManager.getInstance().getUser(id)).map(JikaiUser::getUser).sorted((u1, u2) -> u1.getName().compareTo(u2.getName())).forEach(u -> {
				String uni = UnicodeUtils.getNumberCodePoints(count[0]++);
				uniIdMap.put(uni, u.getIdLong());
			});
			EmbedBuilder eb = buildEmbed(uniIdMap, ju);
			eb.setTitle(ju.getLocale().getString("com_ju_links_eb_title") + (size == 1 ? "" : "[" + (c + 1) + "/" + size + "]"));
			page.addStage(eb.build(), new ArrayList<>(uniIdMap.keySet()), str -> {
				if (ju.linkToUser(uniIdMap.get(str))) {
					page.editCurrentMessage(buildEmbed(uniIdMap, ju).build());
				}
			}, str -> {
				if (ju.unlinkFromUser(uniIdMap.get(str))) {
					page.editCurrentMessage(buildEmbed(uniIdMap, ju).build());
				}
			});
		}
		ju.getUser().openPrivateChannel().submit().thenAccept(pc -> {
			page.send(pc);
			Core.EXEC.schedule(() -> page.end(), 5, TimeUnit.MINUTES);
		});

	}

	private EmbedBuilder buildEmbed(Map<String, Long> uniUserMap, JikaiUser ju) {
		EmbedBuilder eb = BotUtils.embedBuilder();
		uniUserMap.forEach((uni, u) -> eb.appendDescription(String.format("%s: %s %s\n", BotUtils.processUnicode(uni), ju.isLinkedToUser(u) ? UnicodeUtils.YES_EMOJI : UnicodeUtils.NO_EMOJI, Core.JDA.getUserById(u).getName())));
		return eb;
	}

}
