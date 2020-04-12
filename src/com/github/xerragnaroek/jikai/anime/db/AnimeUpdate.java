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
package com.github.xerragnaroek.jikai.anime.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.util.Pair;

/**
 * Compares the old anime with the new ones and categorises the changes into removed, new and
 * postponed anime
 * 
 * @author XerRagnaroek
 *
 */
public class AnimeUpdate {
	private List<Anime> removed;
	private List<Anime> newA;
	private List<Pair<Anime, Long>> postponed;

	private AnimeUpdate() {}

	private void listChanges(List<Anime> oldA, List<Anime> newAnime) {
		removed = oldA.stream().filter(a -> !newAnime.contains(a)).collect(Collectors.toCollection(() -> Collections.synchronizedList(new ArrayList<>())));
		newA = newAnime.stream().filter(a -> !oldA.contains(a)).collect(Collectors.toCollection(() -> Collections.synchronizedList(new ArrayList<>())));
		handlePostponed(newAnime, oldA);
	}

	private void handlePostponed(List<Anime> newA, List<Anime> old) {
		newA.forEach(a -> {
			if (old.contains(a)) {
				Anime oldA = old.get(old.indexOf(a));
				if (oldA.hasDataForNextEpisode() && a.hasDataForNextEpisode() && oldA.getNextEpisodeNumber() == a.getNextEpisodeNumber()) {
					long delay = a.getNextEpisodesAirsAt() - oldA.getNextEpisodesAirsAt();
					postponed.add(Pair.of(a, delay));
				}
			}
		});
	}

	public List<Anime> getRemovedAnime() {
		return removed;
	}

	public boolean hasRemovedAnime() {
		return !removed.isEmpty();
	}

	public List<Anime> getNewAnime() {
		return newA;
	}

	public boolean hasNewAnime() {
		return !newA.isEmpty();
	}

	public List<Pair<Anime, Long>> getPostponedAnime() {
		return postponed;
	}

	public boolean hasPostponedAnime() {
		return postponed != null && !postponed.isEmpty();
	}

	public static AnimeUpdate categoriseUpdates(List<Anime> oldA, List<Anime> newA) {
		AnimeUpdate au = new AnimeUpdate();
		au.listChanges(oldA, newA);
		return au;
	}
}
