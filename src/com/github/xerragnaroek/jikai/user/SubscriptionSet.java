package com.github.xerragnaroek.jikai.user;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

@SuppressWarnings("serial")
public class SubscriptionSet extends TreeSet<Integer> {
	private Set<BiConsumer<Integer, String>> onAdd = new HashSet<>();
	private Set<BiConsumer<Integer, String>> onRem = new HashSet<>();

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
}
