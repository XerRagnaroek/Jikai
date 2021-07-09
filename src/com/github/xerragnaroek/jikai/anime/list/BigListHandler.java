package com.github.xerragnaroek.jikai.anime.list;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.github.xerragnaroek.jikai.jikai.Jikai;

/**
 * 
 */
public class BigListHandler {

	private Jikai j;
	private Map<Integer, Long> messages = Collections.synchronizedMap(new TreeMap<>());

	public BigListHandler(Jikai j) {
		this.j = j;
	}

}
