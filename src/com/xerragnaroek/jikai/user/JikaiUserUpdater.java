package com.xerragnaroek.jikai.user;

import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.xerragnaroek.jikai.util.prop.MapProperty;

public class JikaiUserUpdater {
	private Map<Long, Set<JikaiUser>> titleNotifyStepsMap = new ConcurrentHashMap<>();
	private Map<String, Map<Long, CompletableFuture<Void>>> futureMap = new ConcurrentHashMap<>();
	private MapProperty<ZoneId, Set<JikaiUser>> updatedDaily = new MapProperty<>();
	private Set<JikaiUser> notifyOnRelease = new HashSet<>();
	private ScheduledExecutorService exec = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

	public JikaiUserUpdater() {}

	public void registerUser(JikaiUser ju) {
		ju.preReleaseNotificationStepsProperty().onAdd(l -> {
			titleNotifyStepsMap.compute(l, (k, s) -> {
				if (s == null) {
					s = Collections.synchronizedSet(new HashSet<>());
				}
				s.add(ju);
				return s;
			});
		});
		ju.preReleaseNotificationStepsProperty().onRemove(l -> {
			titleNotifyStepsMap.computeIfPresent(l, (k, s) -> {
				s.remove(ju);
				return (s.isEmpty() ? null : s);
			});
		});
		ju.isUpdatedDailyProperty().onChange((ov, nv) -> {
			if (nv) {
				updatedDaily.compute(ju.getTimeZone(), (z, s) -> {
					if (s == null) {
						s = Collections.synchronizedSet(new HashSet<>());
					}
					s.add(ju);
					return s;
				});
			} else {
				updatedDaily.computeIfPresent(ju.getTimeZone(), (z, s) -> {
					s.remove(ju);
					return (s.isEmpty() ? null : s);
				});
			}
		});
		ju.isNotifiedOnReleaseProperty().onChange((ov, nv) -> {
			if (nv) {
				notifyOnRelease.add(ju);
			} else {
				notifyOnRelease.remove(ju);
			}
		});
	}

	private void startDailyUpdateThread(ZoneId z) {

	}
}
