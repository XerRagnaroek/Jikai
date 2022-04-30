package com.github.xerragnaroek.jikai.anime.list;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.GeneralSubHandler;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.db.AnimeUpdate;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.DetailedAnimeMessageBuilder;
import com.github.xerragnaroek.jikai.util.prop.LongProperty;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

/**
 * 
 */
public class BigListHandler {

	private static Map<Long, Map<String, Map<Integer, Long>>> loadedData = new HashMap<>();
	private Jikai j;
	private BidiMap<Integer, Long> messages = new TreeBidiMap<>();
	private Predicate<Anime> filter = a -> true;
	private long tc;
	private String identifier;
	private final Logger log;
	private Queue<Anime> missingMessage = new LinkedList<>();

	public BigListHandler(Jikai j, String identifier) {
		this.j = j;
		this.identifier = identifier;
		log = LoggerFactory.getLogger(BigListHandler.class + "#" + j.getJikaiData().getGuildId() + "#" + identifier);
		AnimeDB.runOnDBUpdate(this::update);
	}

	public BigListHandler(Jikai j, long textChannel, String identifier) {
		this(j, identifier);
		tc = textChannel;
	}

	public void setAnimeFilter(Predicate<Anime> pred) {
		filter = pred;
	}

	public void bindToChannelProperty(LongProperty id) {
		id.onChange((o, n) -> {
			tc = n;
			try {
				TextChannel tc = j.getGuild().getTextChannelById(o);
				if (tc != null) {
					BotUtils.clearChannel(tc);
				}
			} catch (Exception e) {
				log.error("Couldn't get guild!", e);
			}
			sendList();
		});
	}

	private Message makeMessage(Anime a, boolean addBtn) {
		DetailedAnimeMessageBuilder damb = new DetailedAnimeMessageBuilder(a, j.getJikaiData().getTimeZone(), j.getLocale());
		damb.ignoreEmptyFields().withAll(false);
		MessageBuilder bob = new MessageBuilder(damb.build());
		if (addBtn) {
			bob.setActionRows(ActionRow.of(Button.secondary(GeneralSubHandler.IDENTIFIER + ":" + a.getId(), Emoji.fromUnicode("U+1F514"))));
		}
		return bob.build();
	}

	public CompletableFuture<?> sendList() {
		try {
			TextChannel tc = j.getGuild().getTextChannelById(this.tc);
			return BotUtils.clearChannel(tc).thenAccept(vo -> {
				List<CompletableFuture<Message>> cfs = new LinkedList<>();
				AnimeDB.getAiringOrUpcomingAnime().stream().filter(filter).sorted(Anime.SORT_BY_RELEASE_DATE).forEach(a -> cfs.add(sendMessage(a, tc)));
				CompletableFuture.allOf(cfs.toArray(new CompletableFuture<?>[cfs.size()])).whenComplete((v, t) -> {
					if (t != null) {
						log.debug("Failed sending the {} list!", identifier, t);
					} else {
						log.debug("Successfully sent the {} list!", identifier);
					}
				}).join();
			});
		} catch (Exception e) {
			log.error("Couldn't get guild!", e);
			return CompletableFuture.failedFuture(e);
		}
	}

	private CompletableFuture<Message> sendMessage(Anime a, TextChannel tc) {
		Message m = makeMessage(a, true);
		return tc.sendMessage(m).submit().whenComplete((msg, t) -> {
			if (t != null) {
				log.error("Failed sending message for {},{}", a.getTitleRomaji(), a.getId(), t);
			} else {
				log.debug("Successfully sent message for {},{}", a.getTitleRomaji(), a.getId());
				synchronized (messages) {
					messages.put(a.getId(), msg.getIdLong());
				}
			}
		});
	}

	public void setMessageData(Map<Integer, Long> data) {
		messages.putAll(data);
	}

	public void validateList() {
		log.debug("Validating list...");
		loadedData.compute(j.getJikaiData().getGuildId(), (l, m) -> {
			if (m != null) {
				m.compute(identifier, (s, data) -> {
					if (data != null) {
						messages.putAll(data);
					}
					return null;
				});
				return m.isEmpty() ? null : m;
			}
			return null;
		});
		try {
			TextChannel tc = j.getGuild().getTextChannelById(this.tc);
			if (tc != null) {
				tc.getIterableHistory().submit().thenAccept(l -> {
					log.debug("Validating list via messages in channel");
					// validate via message ids
					List<String> delMsgs = new LinkedList<>();
					l.forEach(m -> {
						if (!messages.containsValue(m.getIdLong())) {
							log.debug("Found invalid msg {} because it's not in the loaded ones", m.getIdLong());
							delMsgs.add(m.getId());
						} else {
							Anime a = AnimeDB.getAnime(messages.getKey(m.getIdLong()));
							if (a == null) {
								log.debug("Found invalid msg {} because it's anime is null", m.getIdLong());
								delMsgs.add(m.getId());
								messages.removeValue(m.getIdLong());
							}
						}
					});

					log.debug("Deleting {} invalid messages", delMsgs.size());
					long minAge = (long) ((Instant.now().getEpochSecond() - 14 * 24 * 60 * 60) * 1000.0 - 1420070400000L) << 22;
					Map<Boolean, List<String>> allowedIds = delMsgs.stream().collect(Collectors.partitioningBy(id -> Long.parseLong(id) > minAge));
					log.debug("{} messages older than 2 weeks, {} more recent", allowedIds.get(false).size(), allowedIds.get(true).size());
					List<String> older = allowedIds.get(false);
					List<String> recent = allowedIds.get(true);
					if (recent.size() > 1) {
						if (recent.size() <= 100) {
							tc.deleteMessagesByIds(recent).queue(v -> log.debug("Deleted {} messages", recent.size()), t -> BotUtils.logAndSendToDev(log, "Failed deleting " + recent.size() + " messages", t));
						} else {
							BotUtils.partitionCollection(recent, 100).forEach(list -> tc.deleteMessagesByIds(list).queue(v -> log.debug("Deleted {} messages", list.size()), t -> BotUtils.logAndSendToDev(log, "Failed deleting " + list.size() + " messages", t)));
						}
					} else {
						if (!recent.isEmpty()) {
							older.add(recent.get(0));
						}
					}
					older.forEach(id -> tc.deleteMessageById(id).queue(v -> log.debug("successfully deleted msg {}", id), t -> log.error("failed deleting msg {}", id, t)));
					List<Long> ids = l.stream().map(Message::getIdLong).collect(Collectors.toList());
					List<Long> loadedIds = messages.values().stream().collect(Collectors.toList());
					loadedIds.stream().filter(id -> !ids.contains(id)).peek(id -> log.debug("Removing invalid mapping for {}", id)).forEach(id -> messages.removeValue(id));

					// validate via anime
					log.debug("Validating via anime");
					List<Integer> curAnime = AnimeDB.getAiringOrUpcomingAnime().stream().filter(filter).sorted(Anime.SORT_BY_RELEASE_DATE).map(Anime::getId).collect(Collectors.toList());
					List<Integer> mappedAnime = messages.keySet().stream().collect(Collectors.toList());
					List<Integer> delAnime = new LinkedList<>(mappedAnime);
					List<Integer> sendAnime = new LinkedList<>(curAnime);
					delAnime.removeAll(curAnime);
					sendAnime.removeAll(mappedAnime);
					delAnime.forEach(id -> deleteAnime(id, tc));
					sendAnime.stream().map(AnimeDB::getAnime).forEach(a -> sendMessage(a, tc));
				});
			}
		} catch (Exception e) {
			log.error("Couldn't get guild!", e);
		}
	}

	private void deleteAnime(int id, TextChannel tc) {
		log.debug("Deleting anime message for anime {}", id);
		long msgId = messages.get(id);
		synchronized (messages) {
			messages.remove(id);
		}
		tc.deleteMessageById(msgId).queue(v -> log.debug("Successfully deleted msg {} for {}", msgId, id), t -> log.error("Failed deleting message for {}", id, t));
	}

	public Map<Integer, Long> getMessageData() {
		return messages;
	}

	private void update(AnimeUpdate au) {
		log.debug("Handling update");
		au.withFilter(filter);
		if (au.hasChange()) {
			try {
				TextChannel tc = j.getGuild().getTextChannelById(this.tc);
				if (tc != null) {
					if (au.hasCancelledAnime()) {
						handleObsolete(au.getCancelledAnime(), tc);
					}
					if (au.hasFinishedAnime()) {
						handleObsolete(au.getFinishedAnime(), tc);
					}
					if (au.hasRemovedAnime()) {
						handleObsolete(au.getRemovedAnime(), tc);
					}
					if (au.hasInfoChangedAnime()) {
						handleInfoChange(au.getInfoChangedAnime(), tc);
					}
					if (au.hasNewAnime()) {
						handleNew(au.getNewAnime(), tc);
					}
				}
			} catch (Exception e) {
				log.error("Couldn't get guild!", e);
			}
		} else {
			log.debug("No change!");
		}
	}

	private void handleInfoChange(List<Anime> list, TextChannel tc) {
		log.debug("Handling {} info changed animes", list.size());
		list.forEach(a -> {
			if (messages.containsKey(a.getId())) {
				tc.editMessageById(messages.get(a.getId()), makeMessage(a, true)).queue(v -> log.debug("Successfully edited message {} for {},{}", v.getIdLong(), a.getId(), a.getTitleRomaji()));
			} else {
				log.debug("{},{} is missing a message, sending it with the new anime!", a.getTitleRomaji(), a.getId());
				missingMessage.add(a);
			}
		});
	}

	private void handleObsolete(List<Anime> list, TextChannel tc) {
		log.debug("Handling {} obsolete animes", list.size());
		list.forEach(a -> deleteAnime(a.getId(), tc));
	}

	private void handleNew(List<Anime> list, TextChannel tc) {
		log.debug("Handling {} new animes and {} missing a message", list.size());
		List<Anime> newAnime = list.stream().filter(a -> !messages.containsKey(a.getId())).collect(Collectors.toList());
		newAnime.addAll(missingMessage);
		newAnime.stream().sorted(Anime.SORT_BY_RELEASE_DATE).forEach(a -> sendMessage(a, tc));
	}

	public static void addLoadedData(long gId, Map<String, Map<Integer, Long>> data) {
		loadedData.put(gId, data);
	}

}
