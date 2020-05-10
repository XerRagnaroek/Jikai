package com.github.xerragnaroek.jikai.anime.schedule;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;

/**
 * Maps anime to the day and time they air on, creating a timetable. Then turns that into an image.
 * Or text.
 * 
 * @author XerRagnaroek
 *
 */
public class AnimeTable {

	private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
	private ZoneId zone;
	private Map<DayOfWeek, Map<LocalTime, Cell>> table = new TreeMap<>();
	private final Logger log = LoggerFactory.getLogger(AnimeTable.class);
	private float fontSize = 18f;
	private int padding = 5;
	private Font font;
	private int lineThickness = 2;
	private String fontFile = "animeace_b.ttf";

	public AnimeTable(ZoneId z) {
		zone = z;
		initMap();
		loadFont();
		//font = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).getGraphics().getFont();
	}

	private void loadFont() {
		try {
			font = Font.createFont(Font.TRUETYPE_FONT, AnimeTable.class.getResourceAsStream("/" + fontFile)).deriveFont(fontSize);
			log.debug("Loaded font {}", fontFile);
		} catch (FontFormatException | IOException e) {
			log.error("", e);
		}
	}

	private void initMap() {
		for (DayOfWeek day : DayOfWeek.values()) {
			table.put(day, new TreeMap<>());
		}
	}

	public boolean addAnime(Anime a) {
		Optional<LocalDateTime> tmp = a.getNextEpisodeDateTime(zone);
		if (tmp.isPresent()) {
			LocalDateTime ldt = tmp.get();
			table.get(ldt.getDayOfWeek()).compute(ldt.toLocalTime(), (lt, cell) -> {
				cell = cell == null ? new Cell(lt) : cell;
				cell.addAnime(a);
				return cell;
			});
		}
		return false;
	}

	public void setTable(Map<DayOfWeek, Map<LocalTime, Set<Anime>>> map) {
		Map<DayOfWeek, Map<LocalTime, Cell>> table = new TreeMap<>();
		map.forEach((day, lsa) -> {
			Map<LocalTime, Cell> tmp = new TreeMap<>();
			lsa.forEach((lt, sa) -> {
				Cell c = new Cell(lt);
				sa.forEach(c::addAnime);
				tmp.put(lt, c);
			});
			table.put(day, tmp);
		});
		this.table = table;
	}

	public BufferedImage toImage() {
		log.debug("Creating AnimeTable img");
		List<BufferedImage> imgs = new LinkedList<>();
		table.forEach((day, map) -> imgs.add(columnImage(day, map)));
		int width = lineThickness * (imgs.size() - 1);
		int height = 0;
		log.debug("Calculating dimensions");
		for (BufferedImage img : imgs) {
			width += img.getWidth();
			int imgH = img.getHeight();
			height = height > imgH ? height : imgH;
		}
		log.debug("Image has width={}, height={}", width, height);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int curX = 0;
		Graphics g = img.getGraphics();
		int total = imgs.size();
		log.debug("Merging images");
		for (int i = 0; i < total; i++) {
			BufferedImage dImg = imgs.get(i);
			g.drawImage(dImg, curX, 0, null);
			curX += dImg.getWidth();
			log.debug("Drawn image #{}", i);
			if (i < total - 1) {
				g.fillRect(curX, 0, lineThickness, height);
				curX += lineThickness;
			}
		}
		return img;
	}

	private BufferedImage columnImage(DayOfWeek day, Map<LocalTime, Cell> map) {
		log.debug("Making column image for " + day.toString());
		BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		Font dayFont = font.deriveFont(fontSize + 4);
		double[] height = { padding * 2 };
		int[] width = { 0 };
		Graphics g = tmp.getGraphics();
		Rectangle2D dayBounds = g.getFontMetrics(dayFont).getStringBounds(day.toString(), g);
		height[0] += dayBounds.getHeight();
		width[0] += dayBounds.getWidth();
		List<BufferedImage> cellImgs = map.values().stream().sorted().map(c -> c.toImage(padding, font)).peek(img -> {
			height[0] += img.getHeight() + lineThickness * 2;
			int imgW = img.getWidth();
			width[0] = width[0] > imgW ? width[0] : imgW;
		}).collect(Collectors.toList());
		BufferedImage img = new BufferedImage(width[0], (int) height[0], BufferedImage.TYPE_INT_RGB);
		log.debug("Image has dimensions: width={}, height={}", img.getWidth(), img.getHeight());
		g = img.getGraphics();
		g.setColor(Color.black);
		g.drawRect(0, 0, img.getWidth(), img.getHeight());
		g.setColor(Color.white);
		g.setFont(dayFont);
		int curY = (int) dayBounds.getHeight();
		log.debug("Drawing day");
		g.drawString(day.toString(), (int) ((width[0] / 2) - (dayBounds.getWidth() / 2)), curY);
		curY += padding;
		log.debug("Drawing cells");
		for (BufferedImage cell : cellImgs) {
			g.drawImage(cell, 0, curY, null);
			curY += lineThickness;
			g.fillRect(0, curY, img.getWidth(), lineThickness);
			curY += cell.getHeight() + lineThickness;
		}
		log.debug("Image successfully created");
		return img;
		//return map.values().stream().findAny().get().toImage(padding, font);
	}

	public Map<DayOfWeek, String> toFormatedWeekString(TitleLanguage tl, boolean includeDay) {
		Map<DayOfWeek, String> m = new TreeMap<>();
		table.forEach((day, map) -> {
			StringBuilder bob = new StringBuilder();
			if (includeDay) {
				bob.append(day + "\n");
				bob.append(StringUtils.repeat("=", day.toString().length()) + "\n");
			}
			map.forEach((lt, cell) -> {
				bob.append(String.format("[%s]%n", dtf.format(lt)));
				cell.getAnime().forEach(a -> bob.append(String.format("\t%s%n", a.getTitle(tl))));
			});
			m.put(day, bob.toString());
		});
		return m;
	}

	@Override
	public String toString() {
		return StringUtils.joinWith("\n", toFormatedWeekString(TitleLanguage.ROMAJI, true).values());
	}
}

class Cell implements Comparable<Cell> {
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
	private LocalTime lt;
	private TreeMap<Anime, BufferedImage> ani = new TreeMap<>();
	private final Logger log;

	Cell(LocalTime lt) {
		this.lt = lt;
		log = LoggerFactory.getLogger(Cell.class + "#" + dtf.format(lt));
	}

	void addAnime(Anime a) {
		try {
			log.debug("Adding {} to cell", a.getId());
			BufferedImage bi = ImageIO.read(new URL(a.getCoverImageMedium()));
			ani.put(a, bi);
			log.debug("Added {} to cell and loaded image w={}, h={}", a.getTitleRomaji(), bi.getWidth(), bi.getHeight());
		} catch (IOException e) {
			log.error("", e);
		}
	}

	Set<Anime> getAnime() {
		return ani.keySet();
	}

	BufferedImage toImage(int padding, Font f) {
		log.debug("Creating image");
		BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		String timeStr = dtf.format(lt);
		Graphics g = tmp.getGraphics();
		Font timeFont = f.deriveFont(f.getSize() + 2f);
		FontMetrics fm = g.getFontMetrics(timeFont);
		double height = padding + fm.getStringBounds(timeStr, g).getHeight();
		double width = 0;
		fm = g.getFontMetrics(f);
		for (Entry<Anime, BufferedImage> entry : ani.entrySet()) {
			Anime a = entry.getKey();
			BufferedImage img = entry.getValue();
			Rectangle2D titleBounds = fm.getStringBounds(a.getTitle(TitleLanguage.ROMAJI), g);
			double titleWidth = titleBounds.getWidth();
			double imgWidth = img.getWidth();
			//greatest width is the width of the cell
			double cellWidth = (titleWidth > imgWidth ? titleWidth : imgWidth) + padding * 2;
			//width of image is width of widest cell
			width = width > cellWidth ? width : cellWidth;
			height += titleBounds.getHeight() + padding * 2 + img.getHeight();
		}

		log.debug("Image has dimensions: width={}, height={}", (int) width, (int) height);
		BufferedImage img = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_RGB);
		g = img.getGraphics();
		g.setColor(Color.black);
		g.drawRect(0, 0, img.getWidth(), img.getHeight());
		g.setColor(Color.white);
		g.setFont(timeFont);
		fm = g.getFontMetrics(f);
		int curX = padding;
		int curY = padding + (int) fm.getStringBounds(timeStr, g).getHeight();
		g.drawString(timeStr, curX, curY);
		g.setFont(f);
		fm = g.getFontMetrics();
		//curY += padding;
		for (Entry<Anime, BufferedImage> entry : ani.entrySet()) {
			if (entry.getKey().getId() == 115136) {
				log.debug("debug point :)");
			}
			String title = entry.getKey().getTitle(TitleLanguage.ROMAJI);
			curY += (int) fm.getStringBounds(title, g).getHeight() + padding;
			g.drawString(title, curX, curY);
			curY += padding;
			g.drawImage(entry.getValue(), curX, curY, null);
			curY += entry.getValue().getHeight();
		}
		log.debug("Image successfully created");
		return img;
	}

	@Override
	public int compareTo(Cell o) {
		return lt.compareTo(o.lt);
	}
}
