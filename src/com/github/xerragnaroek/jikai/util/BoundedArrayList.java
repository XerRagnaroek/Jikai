package com.github.xerragnaroek.jikai.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.iterators.ReverseListIterator;

/**
 * An ArrayList that's bounded to N number of max elements, where the oldest one gets removed if
 * capacity should be exceeded.
 */
public class BoundedArrayList<E> extends ArrayList<E> {

	private static final long serialVersionUID = 1L;
	private int maxSize;

	/**
	 * A new BoundedArrayList with a maximum capacity of maxSize
	 */
	public BoundedArrayList(int maxSize) {
		super(maxSize);
		this.maxSize = maxSize;
	}

	/**
	 * A new BoundedArrayList with default capacity of 32
	 */
	public BoundedArrayList() {
		this(32);
	}

	/**
	 * Set the new maximum capacity. Removes the oldest elements until within the new capacity.
	 * 
	 * @param newMax
	 *            - the new max capacity
	 */
	public void setMaxCapacity(int newMax) {
		if (newMax < maxSize) {
			int sizeDif = size() - newMax;
			if (sizeDif > 0) {
				removeRange(0, sizeDif);
			}
		}
		maxSize = newMax;
	}

	public int getMaxCapacity() {
		return maxSize;
	}

	@Override
	public boolean add(E e) {
		assureCapacity();
		return super.add(e);
	}

	@Override
	public void add(int index, E element) {
		assureCapacity();
		super.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		int cSize = c.size();
		if (cSize > maxSize) {
			super.clear();
			List<E> tmp = new ArrayList<>(c.size());
			tmp.addAll(c);
			return super.addAll(tmp.subList((cSize - 1) - maxSize, cSize));
		} else if (cSize == maxSize) {
			super.clear();
			return super.addAll(c);
		} else {
			if (cSize > spareCapacity()) {
				removeRange(0, cSize - spareCapacity());
				return super.addAll(c);
			} else {
				return super.addAll(c);
			}

		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException("addAll(int index, Collection c) is not supported by BoundedArrayList!");
	}

	public int spareCapacity() {
		return maxSize - size();
	}

	private void assureCapacity() {
		if (size() + 1 > maxSize) {
			// remove the first element
			remove(0);
		}
	}

	/**
	 * @return A {@link ReverseListIterator} for traversing this list backwards.
	 */
	public ReverseListIterator<E> reverseListIterator() {
		return new ReverseListIterator<E>(this);
	}
}
