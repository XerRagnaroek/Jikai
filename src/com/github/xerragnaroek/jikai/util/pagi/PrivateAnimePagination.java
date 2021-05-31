package com.github.xerragnaroek.jikai.util.pagi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;
import com.github.xerragnaroek.jikai.util.UnicodeUtils;

import net.dv8tion.jda.api.entities.MessageEmbed;

public class PrivateAnimePagination extends PrivatePagination {

	private Map<Integer, Map<String, Anime>> stageReactionIdMap = new HashMap<>();
	private Predicate<Anime> yes;
	private boolean useNums;
	private JikaiUser ju;
	private String title;
	private Consumer<Anime> addCon;
	private Consumer<Anime> remCon;
	private boolean refresh;

	public PrivateAnimePagination(JikaiUser ju) {
		super();
		this.ju = ju;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setYesPredicate(Predicate<Anime> yes) {
		this.yes = yes;
	}

	public void setOnReactionAdded(Consumer<Anime> addCon) {
		this.addCon = addCon;
	}

	public void setOnReactionRemoved(Consumer<Anime> remCon) {
		this.remCon = remCon;
	}

	public void setRefreshOnReaction(boolean refresh) {
		this.refresh = refresh;
	}

	public void populate(Collection<Anime> anime) {
		List<Anime> ani = new ArrayList<>(anime.size());
		ani.addAll(anime);
		Collections.sort(ani);
		useNums = ani.size() <= 20;
		Collection<List<Anime>> partitioned = BotUtils.partitionCollection(ani, useNums ? 10 : 18);
		int stage = 0;
		for (List<Anime> l : partitioned) {
			Pair<MessageEmbed, Map<String, Anime>> pair = makeEmbed(l, stage + 1, partitioned.size());
			stageReactionIdMap.put(stage, pair.getRight());
			List<String> cps = List.copyOf(pair.getRight().keySet());
			super.addStage(pair.getLeft(), cps, str -> handleCon(str, addCon), str -> handleCon(str, remCon));
			stage++;
		}
	}

	private void handleCon(String str, Consumer<Anime> con) {
		con.accept(stageReactionIdMap.get(getCurrentStageInt()).get(str));
		if (refresh) {
			Collection<Anime> anime = stageReactionIdMap.get(getCurrentStageInt()).values();
			super.editStage(getCurrentStageInt(), makeEmbed(anime, getCurrentStageInt(), stageReactionIdMap.size()).getLeft());
		}
	}

	private Pair<MessageEmbed, Map<String, Anime>> makeEmbed(Collection<Anime> anime, int curNum, int maxN) {
		List<String> strs = new ArrayList<>(anime.size());
		Map<String, Anime> map = new LinkedHashMap<>();
		int c = 0;
		for (Anime a : anime) {
			String uniCode = (useNums ? UnicodeUtils.getNumberCodePoints(c + 1) : UnicodeUtils.getStringCodePoints(Character.toString('a' + c)).get(0));
			String yesOrNo = this.yes.test(a) ? UnicodeUtils.YES_EMOJI : UnicodeUtils.NO_EMOJI;
			strs.add(String.format("%s:%s:**[%s](%s)**", BotUtils.processUnicode(uniCode), yesOrNo, (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())), a.getAniUrl()));
			map.put(uniCode, a);
			c++;
		}
		return Pair.of(BotUtils.titledEmbed(maxN == 1 ? title : title + " [" + curNum + "/" + maxN + "]", String.join("\n", strs)), map);
	}

	public void send(long misUntilEnd) {
		ju.getUser().openPrivateChannel().queue(pc -> {
			super.send(pc);
			Core.EXEC.schedule(() -> end(), misUntilEnd, TimeUnit.MINUTES);
		});
	}
}
