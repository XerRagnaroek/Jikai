package com.github.xerragnaroek.jikai.user.link;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class JikaiUserLinkHandler {
	// Initially I planned to store links in their own objects, hence this class. Might be needed to
	// expand functionality tho so for now I'm keeping it.

	private static final Logger log = LoggerFactory.getLogger(JikaiUserLinkHandler.class);
	private static Map<Long, Set<Long>> activeRequests = Collections.synchronizedMap(new TreeMap<>());

	/**
	 * This links initiator to target, meaning that any syncable features (e.g. subs) performed by
	 * target will
	 * be synced to initiator
	 * 
	 * @param initiator
	 * @param target
	 * @param direction
	 * @return 0 = already linked, 1 = link established or request sent, 2 = init link request still
	 *         active, 3 = tgt has link request to tgt
	 */
	public static int initiateLink(JikaiUser initiator, JikaiUser target, int direction, String msg) {
		int tmp = 0;
		if (target.getLinkedUsers().contains(initiator.getId())) {
			return 0;
		} else if ((tmp = isLinkRequestActive(initiator, target)) > 0) {
			return 1 + tmp;
		} else {
			LinkRequest.handleLinkRequest(initiator, target, direction, msg);
			return 1;
		}
	}

	static void registerRequest(JikaiUser init, JikaiUser tgt) {
		log.debug("regestering link request {}->{}", init.getId(), tgt.getId());
		activeRequests.compute(init.getId(), (l, s) -> {
			if (s == null) {
				s = new TreeSet<>();
			}
			s.add(tgt.getId());
			return s;
		});
	}

	static void removeRequest(JikaiUser init, JikaiUser tgt) {
		log.debug("removing link request {}->{}", init.getId(), tgt.getId());
		activeRequests.computeIfPresent(init.getId(), (id, s) -> {
			s.remove(tgt.getId());
			return s.isEmpty() ? null : s;
		});
	}

	/**
	 * @param init
	 * @param tgt
	 * @return 0 = no request active, 1 = request active, 2 = tgt requested link to init
	 */
	public static int isLinkRequestActive(JikaiUser init, JikaiUser tgt) {
		if (activeRequests.containsKey(init.getId()) && activeRequests.get(init.getId()).contains(tgt.getId())) {
			return 1;
		} else if (activeRequests.containsKey(tgt.getId()) && activeRequests.get(tgt.getId()).contains(init.getId())) {
			return 2;
		}
		return 0;
	}
}
