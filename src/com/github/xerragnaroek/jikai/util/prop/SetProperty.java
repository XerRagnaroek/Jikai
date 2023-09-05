package com.github.xerragnaroek.jikai.util.prop;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SetProperty<T> implements Set<T> {

    private Set<T> set = new HashSet<>();
    private final Set<Consumer<T>> addCon = new HashSet<>();
    private final Set<Consumer<T>> remCon = new HashSet<>();
    private final AtomicBoolean changed = new AtomicBoolean(false);

    public SetProperty() {
        set = new HashSet<>();
    }

    public SetProperty(Set<T> set) {
        this.set = set;
    }

    public void bind(SetProperty<T> setP) {
        onAdd(setP::add);
        onRemove(setP::remove);
    }

    public void bindAndSet(SetProperty<T> setP) {
        setP.set = set;
        bind(setP);
    }

    public boolean add(T entry) {
        boolean add = set.add(entry);
        if (add) {
            changed.set(true);
            runConsumer(addCon, entry);
        }
        return add;
    }

    public boolean remove(Object entry) {
        boolean rem = set.remove(entry);
        if (rem) {
            changed.set(true);
            runConsumer(remCon, (T) entry);
        }
        return rem;
    }

    public boolean onAdd(Consumer<T> con) {
        return addCon.add(con);
    }

    public boolean onRemove(Consumer<T> con) {
        return remCon.add(con);
    }

    private synchronized void runConsumer(Set<Consumer<T>> cons, T value) {
        cons.forEach(c -> c.accept(value));
    }

    public Set<T> get() {
        return set;
    }

    public boolean hasChanged() {
        boolean b = changed.get();
        if (b) {
            changed.set(false);
        }
        return b;
    }

    public void clearConsumer() {
        addCon.clear();
        remCon.clear();
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return set.iterator();
    }

    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return set.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean added = false;
        for (T t : c) {
            added = add(t);
        }
        if (added) {
            changed.set(true);
        }
        return added;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        AtomicBoolean changed = new AtomicBoolean(false);
        Set<T> tmp = new HashSet<>(set);
        tmp.forEach(t -> {
            if (!c.contains(t)) {
                remove(t);
                changed.set(true);
            }
        });
        if (changed.get()) {
            this.changed.set(true);
        }
        return changed.get();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        int size = size();
        c.forEach(o -> {
            remove(o);
        });
        return size() < size;
    }

    @Override
    public void clear() {
        Set<T> tmp = new HashSet<>(set);
        tmp.forEach(t -> remove(t));
    }

    @Override
    public String toString() {
        return "[" + set.stream().map(T::toString).collect(Collectors.joining(",")) + "]";
    }
}
