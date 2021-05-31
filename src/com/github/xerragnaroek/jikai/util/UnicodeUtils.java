package com.github.xerragnaroek.jikai.util;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 */
public class UnicodeUtils {
	public static final String YES_EMOJI = ":white_check_mark:";
	public static final String NO_EMOJI = ":x:";

	public static String getNumberCodePoints(int n) {
		String str = String.valueOf(n);
		String codePoints = "";
		for (int i = 0; i < str.length(); i++) {
			char digit = str.charAt(i);
			if (digit == '1') {
				if ((i + 1) < str.length() && str.charAt(i + 1) == '0') {
					codePoints += "U+1f51f";
					i++;
				} else {
					codePoints += "U+31U+fe0fU+20e3";
				}
			} else {
				codePoints += "U+3" + (digit - 48) + "U+fe0fU+20e3";
			}
		}
		return codePoints;
	}

	public static List<String> getStringCodePoints(String str) {
		// regional_indicator_a
		int cp = 0x1F1E6;
		str = str.toLowerCase();
		return str.chars().map(c -> cp - ('a' - c)).mapToObj(Integer::toHexString).map(s -> "U+" + s).collect(Collectors.toList());
	}
}
