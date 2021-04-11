package com.github.xerragnaroek.jikai.user.link;

import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class JikaiUserLinkHandler {
	// Initially I planned to store links in their own objects, hence this class. Might be needed to
	// expand functionality tho so for now I'm keeping it.

	/**
	 * This links initiator to target, meaning that any syncable features (e.g. subs) performed by
	 * target will
	 * be synced to initiator
	 * 
	 * @param initiator
	 * @param target
	 * @param direction
	 * @return
	 */
	public static boolean initiateLink(JikaiUser initiator, JikaiUser target, int direction, String msg) {
		if (target.getLinkedUsers().contains(initiator.getId())) {
			return false;
		} else {
			LinkRequest.handleLinkRequest(initiator, target, direction, msg);
			return true;
		}
	}
}
