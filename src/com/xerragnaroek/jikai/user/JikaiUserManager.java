package com.xerragnaroek.jikai.user;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.xerragnaroek.jikai.data.Jikai;

public class JikaiUserManager {

	private Map<Long, JikaiUser> user = new ConcurrentHashMap<>();
	private Map<String, Set<JikaiUser>> subscriptionMap = new ConcurrentHashMap<>();
	private JikaiUserUpdater juu = new JikaiUserUpdater();

	public JikaiUserManager() {}

	public JikaiUser registerUser(long id) {
		JikaiUser ju = new JikaiUser(id);
		handleSubscriptions(ju);
		handleTimeZoneChange(ju);
		juu.registerUser(ju);
		user.put(id, ju);
		JikaiUserSetup.runSetup(ju);
		return ju;
	}

	public JikaiUser getUser(long id) {
		return user.get(id);
	}

	private void handleSubscriptions(JikaiUser ju) {
		ju.subscribedAnimesProperty().onAdd(title -> {
			subscriptionMap.compute(title, (t, s) -> {
				if (s == null) {
					s = Collections.synchronizedSet(new HashSet<>());
				}
				s.add(ju);
				return s;
			});
		});
		ju.subscribedAnimesProperty().onRemove(title -> {
			subscriptionMap.computeIfPresent(title, (t, s) -> {
				s.remove(ju);
				return s.isEmpty() ? null : s;
			});
		});
	}

	private void handleTimeZoneChange(JikaiUser ju) {
		ju.timeZoneProperty().onChange((oz, nz) -> {
			Jikai.removeTimeZone(oz);
			if (nz != null) {
				Jikai.addTimeZone(nz);
			}
		});
	}

	public void removeUser(long id) {
		JikaiUser ju = user.remove(id);
		if (ju != null) {
			ju.destroy();
		}
	}

}
