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

@SuppressWarnings("serial")
public class SubscriptionSet extends TreeSet<Integer> {
	@JsonIgnore
	private Set<BiConsumer<Integer, String>> onAdd = new HashSet<>();
	@JsonIgnore
	private Set<BiConsumer<Integer, String>> onRem = new HashSet<>();

	public SubscriptionSet() {}

	public SubscriptionSet(Set<Integer> set) {
		super(set);
	}

	@Override
	public boolean add(Integer id) {
		if (super.add(id)) {
			runCons(onAdd, id, "No cause");
			return true;
		}
		return false;
	}

	public boolean add(int id, String cause) {
		if (super.add(id)) {
			runCons(onAdd, id, cause);
			return true;
		}
		return false;
	}

	@Override
	public boolean remove(Object o) {
		if (super.remove(o)) {
			runCons(onRem, (Integer) o, "No cause");
			return true;
		}
		return false;
	}

	public boolean remove(int id, String cause) {
		if (super.remove(id)) {
			runCons(onRem, id, cause);
			return true;
		}
		return false;
	}

	private void runCons(Set<BiConsumer<Integer, String>> cons, int i, String cause) {
		cons.forEach(con -> con.accept(i, cause));
	}

	public void onAdd(BiConsumer<Integer, String> idCauseCon) {
		onAdd.add(idCauseCon);
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
		 * stream().map(AnimeDB::getAnime).map(a -> "- " + a.getTitle(ju.getTitleLanguage()) +
		 * "\n").sorted().forEach(bob::append);
		 * bob.append("```");
		 */

		return stream().map(AnimeDB::getAnime).map(a -> "[**" + a.getTitle(ju.getTitleLanguage()) + "**](" + a.getAniUrl() + ")\n").sorted().collect(Collectors.toList());

	}
}
