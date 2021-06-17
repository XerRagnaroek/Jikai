package com.github.xerragnaroek.jikai.user;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.util.TriConsumer;

@SuppressWarnings("serial")
public class SubscriptionSet extends TreeSet<Integer> {
	@JsonIgnore
	private Set<TriConsumer<Integer, String, Boolean>> onAdd = new HashSet<>();
	@JsonIgnore
	private Set<BiConsumer<Integer, String>> onRem = new HashSet<>();

	public SubscriptionSet() {}

	public SubscriptionSet(Set<Integer> set) {
		super(set);
	}

	@Override
	public boolean add(Integer id) {
		if (super.add(id)) {
			runAddCons(id, "No cause", false);
			return true;
		}
		return false;
	}

	public boolean add(int id, String cause, boolean linked) {
		if (super.add(id)) {
			runAddCons(id, cause, linked);
			return true;
		}
		return false;
	}

	@Override
	public boolean remove(Object o) {
		if (super.remove(o)) {
			runRemCons((Integer) o, "No cause");
			return true;
		}
		return false;
	}

	public boolean remove(int id, String cause) {
		if (super.remove(id)) {
			runRemCons(id, cause);
			return true;
		}
		return false;
	}

	private void runAddCons(int i, String cause, boolean linked) {
		onAdd.forEach(c -> c.accept(i, cause, linked));
	}

	private void runRemCons(int i, String cause) {
		onRem.forEach(c -> c.accept(i, cause));
	}

	public void onAdd(TriConsumer<Integer, String, Boolean> idCauseLinkedCon) {
		onAdd.add(idCauseLinkedCon);
	}

	public void onRemove(BiConsumer<Integer, String> idCauseCon) {
		onRem.add(idCauseCon);
	}

	public int removeInvalidAnime() {
		Set<Integer> copy = new TreeSet<>(this);
		AtomicInteger count = new AtomicInteger(0);
		copy.stream().filter(i -> AnimeDB.getAnime(i) == null).peek(i -> count.incrementAndGet()).forEach(this::remove);
		return count.get();
	}

	@JsonIgnore
	public List<String> getSubscriptionsFormatted(JikaiUser ju) {
		if (isEmpty()) {
			Collections.emptyList();
		}
		/*
		 * StringBuilder bob = new StringBuilder();
		 * bob.append("```asciidoc\n");
		 * stream().map(AnimeDB::getAnime).map(a -> "- " +
		 * (ju.hasCustomName(a.getId())?ju.getCustomName(a.getId()): a.getTitle(ju.getTitleLanguage())) +
		 * "\n").sorted().forEach(bob::append);
		 * bob.append("```");
		 */

		return stream().map(AnimeDB::getAnime).map(a -> "[**" + (ju.hasCustomTitle(a.getId()) ? ju.getCustomTitle(a.getId()) : a.getTitle(ju.getTitleLanguage())) + "**](" + a.getAniUrl() + ")\n").sorted().collect(Collectors.toList());

	}
}
