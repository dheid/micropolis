// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.gui;

import micropolisj.engine.MapListener;
import micropolisj.engine.MapState;
import micropolisj.engine.Micropolis;
import micropolisj.engine.Sprite;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static micropolisj.engine.TileConstants.CLEAR;
import static micropolisj.engine.TileConstants.DIRT;
import static micropolisj.engine.TileConstants.LOMASK;
import static micropolisj.engine.TileConstants.PWRBIT;
import static micropolisj.engine.TileConstants.isCommercialZone;
import static micropolisj.engine.TileConstants.isConductive;
import static micropolisj.engine.TileConstants.isConstructed;
import static micropolisj.engine.TileConstants.isIndustrialZone;
import static micropolisj.engine.TileConstants.isRailAny;
import static micropolisj.engine.TileConstants.isResidentialZoneAny;
import static micropolisj.engine.TileConstants.isRoadAny;
import static micropolisj.engine.TileConstants.isZoneAny;
import static micropolisj.engine.TileConstants.isZoneCenter;

public class OverlayMapView extends JComponent
		implements Scrollable, MapListener
{
	private static final int TILE_WIDTH = 3;
	private static final int TILE_HEIGHT = 3;
	private static final Color VAL_LOW = new Color(0xbfbfbf);
	private static final Color VAL_MEDIUM = new Color(0xffff00);
	private static final Color VAL_HIGH = new Color(0xff7f00);
	private static final Color VAL_VERYHIGH = new Color(0xff0000);
	private static final Color VAL_PLUS = new Color(0x007f00);
	private static final Color VAL_VERYPLUS = new Color(0x00e600);
	private static final Color VAL_MINUS = new Color(0xff7f00);
	private static final Color VAL_VERYMINUS = new Color(0xffff00);
	private static final int UNPOWERED = 0x6666e6;   //lightblue
	private static final int POWERED = 0xff0000;   //red
	private static final int CONDUCTIVE = 0xbfbfbf;   //lightgray
	private final TileImages tileImages;
	private final ArrayList<ConnectedView> views = new ArrayList<>();
	private Micropolis engine;
	private MapState mapState = MapState.ALL;
	public OverlayMapView(Micropolis engine)
	{
		assert engine != null;

		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				onMousePressed(e);
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				onMouseDragged(e);
			}

			private void onMousePressed(MouseEvent ev)
			{
				if (ev.getButton() == MouseEvent.BUTTON1)
					dragViewTo(ev.getPoint());
			}

			private void onMouseDragged(MouseEvent ev)
			{
				if ((ev.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0)
					return;

				dragViewTo(ev.getPoint());
			}

		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);

		setEngine(engine);

		tileImages = TileImages.getInstance(TILE_WIDTH);

	}

	private static Color getCI(int x)
	{
		if (x < 50)
			return null;
		else if (x < 100)
			return VAL_LOW;
		else if (x < 150)
			return VAL_MEDIUM;
		else if (x < 200)
			return VAL_HIGH;
		else
			return VAL_VERYHIGH;
	}

	private static Color getCI_rog(int x)
	{
		if (x > 100)
			return VAL_VERYPLUS;
		else if (x > 20)
			return VAL_PLUS;
		else if (x < -100)
			return VAL_VERYMINUS;
		else if (x < -20)
			return VAL_MINUS;
		else
			return null;
	}

	private static void maybeDrawRect(Graphics gr, Color col, int x, int y, int width, int height)
	{
		if (col != null) {
			gr.setColor(col);
			gr.fillRect(x, y, width, height);
		}
	}

	private static int checkPower(BufferedImage img, int x, int y, int rawTile)
	{
		int pix;

		if ((rawTile & LOMASK) <= 63) {
			return rawTile & LOMASK;
		} else if (isZoneCenter(rawTile)) {
			// zone
			pix = (rawTile & PWRBIT) != 0 ? POWERED : UNPOWERED;
		} else if (isConductive(rawTile)) {
			pix = CONDUCTIVE;
		} else {
			return DIRT;
		}

		for (int yy = 0; yy < TILE_HEIGHT; yy++) {
			for (int xx = 0; xx < TILE_WIDTH; xx++) {
				img.setRGB(x * TILE_WIDTH + xx, y * TILE_HEIGHT + yy, pix);
			}
		}
		return -1; //this special value tells caller to skip the tile bitblt,
		//since it was performed here
	}

	public void setEngine(Micropolis newEngine)
	{
		assert newEngine != null;

		if (engine != null) { //old engine
			engine.removeMapListener(this);
		}
		engine = newEngine;
		//new engine
		engine.addMapListener(this);

		invalidate();  //map size may have changed
		repaint();
		engine.calculateCenterMass();
		dragViewToCityCenter();
	}

	public MapState getMapState()
	{
		return mapState;
	}

	public void setMapState(MapState newState)
	{
		if (mapState == newState)
			return;

		mapState = newState;
		repaint();
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(
				getInsets().left + getInsets().right + TILE_WIDTH * engine.getWidth(),
				getInsets().top + getInsets().bottom + TILE_HEIGHT * engine.getHeight()
		);
	}

	private void drawPollutionMap(Graphics gr)
	{
		int[][] a = engine.getPollutionMem();

		for (int y = 0; y < a.length; y++) {
			for (int x = 0; x < a[y].length; x++) {
				maybeDrawRect(gr, getCI(10 + a[y][x]), x * 6, y * 6, 6, 6);
			}
		}
	}

	private void drawCrimeMap(Graphics gr)
	{
		int[][] a = engine.getCrimeMem();

		for (int y = 0; y < a.length; y++) {
			for (int x = 0; x < a[y].length; x++) {
				maybeDrawRect(gr, getCI(a[y][x]), x * 6, y * 6, 6, 6);
			}
		}
	}

	private void drawPopDensity(Graphics gr)
	{
		int[][] a = engine.getPopDensity();

		for (int y = 0; y < a.length; y++) {
			for (int x = 0; x < a[y].length; x++) {
				maybeDrawRect(gr, getCI(a[y][x]), x * 6, y * 6, 6, 6);
			}
		}
	}

	private void drawRateOfGrowth(Graphics gr)
	{
		int[][] a = engine.getRateOGMem();

		for (int y = 0; y < a.length; y++) {
			for (int x = 0; x < a[y].length; x++) {
				maybeDrawRect(gr, getCI_rog(a[y][x]), x * 24, y * 24, 24, 24);
			}
		}
	}

	private void drawFireRadius(Graphics gr)
	{
		int[][] a = engine.getFireRate();

		for (int y = 0; y < a.length; y++) {
			for (int x = 0; x < a[y].length; x++) {
				maybeDrawRect(gr, getCI(a[y][x]), x * 24, y * 24, 24, 24);
			}
		}
	}

	private void drawPoliceRadius(Graphics gr)
	{
		int[][] a = engine.getPoliceMapEffect();

		for (int y = 0; y < a.length; y++) {
			for (int x = 0; x < a[y].length; x++) {
				maybeDrawRect(gr, getCI(a[y][x]), x * 24, y * 24, 24, 24);
			}
		}
	}

	private int checkLandValueOverlay(BufferedImage img, int xpos, int ypos, int tile)
	{
		int v = engine.getLandValue(xpos, ypos);
		Color c = getCI(v);
		if (c == null) {
			return tile;
		}

		int pix = c.getRGB();
		for (int yy = 0; yy < TILE_HEIGHT; yy++) {
			for (int xx = 0; xx < TILE_WIDTH; xx++) {
				img.setRGB(
						xpos * TILE_WIDTH + xx,
						ypos * TILE_HEIGHT + yy,
						pix);
			}
		}
		return CLEAR;
	}

	private int checkTrafficOverlay(BufferedImage img, int xpos, int ypos, int tile)
	{
		int d = engine.getTrafficDensity(xpos, ypos);
		Color c = getCI(d);
		if (c == null) {
			return tile;
		}

		int pix = c.getRGB();
		for (int yy = 0; yy < TILE_HEIGHT; yy++) {
			for (int xx = 0; xx < TILE_WIDTH; xx++) {
				img.setRGB(
						xpos * TILE_WIDTH + xx,
						ypos * TILE_HEIGHT + yy,
						pix);
			}
		}
		return CLEAR;
	}

	@Override
	public void paintComponent(Graphics g)
	{
		int width = engine.getWidth();
		int height = engine.getHeight();

		BufferedImage img = new BufferedImage(width * TILE_WIDTH, height * TILE_HEIGHT,
				BufferedImage.TYPE_INT_RGB);

		Insets insets = getInsets();
		Rectangle clipRect = g.getClipBounds();
		int minX = Math.max(0, (clipRect.x - insets.left) / TILE_WIDTH);
		int minY = Math.max(0, (clipRect.y - insets.top) / TILE_HEIGHT);
		int maxX = Math.min(width, 1 + (clipRect.x - insets.left + clipRect.width - 1) / TILE_WIDTH);
		int maxY = Math.min(height, 1 + (clipRect.y - insets.top + clipRect.height - 1) / TILE_HEIGHT);

		for (int y = minY; y < maxY; y++) {
			for (int x = minX; x < maxX; x++) {
				int tile = engine.getTile(x, y);
				switch (mapState) {
					case RESIDENTIAL:
						if (isZoneAny(tile) &&
								!isResidentialZoneAny(tile)) {
							tile = DIRT;
						}
						break;
					case COMMERCIAL:
						if (isZoneAny(tile) &&
								!isCommercialZone(tile)) {
							tile = DIRT;
						}
						break;
					case INDUSTRIAL:
						if (isZoneAny(tile) &&
								!isIndustrialZone(tile)) {
							tile = DIRT;
						}
						break;
					case POWER_OVERLAY:
						tile = checkPower(img, x, y, engine.getTile(x, y));
						break;
					case TRANSPORT:
					case TRAFFIC_OVERLAY:
						if (isConstructed(tile)
								&& !isRoadAny(tile)
								&& !isRailAny(tile)) {
							tile = DIRT;
						}
						if (mapState == MapState.TRAFFIC_OVERLAY) {
							tile = checkTrafficOverlay(img, x, y, tile);
						}
						break;

					case LANDVALUE_OVERLAY:
						tile = checkLandValueOverlay(img, x, y, tile);
						break;

					default:
				}

				// tile == -1 means it's already been drawn
				// in the checkPower function

				if (tile != -1) {
					paintTile(img, x, y, tile);
				}
			}
		}

		g.drawImage(img, insets.left, insets.top, null);

		g = g.create();
		g.translate(insets.left, insets.top);

		switch (mapState) {
			case POLICE_OVERLAY:
				drawPoliceRadius(g);
				break;
			case FIRE_OVERLAY:
				drawFireRadius(g);
				break;
			case CRIME_OVERLAY:
				drawCrimeMap(g);
				break;
			case POLLUTE_OVERLAY:
				drawPollutionMap(g);
				break;
			case GROWTHRATE_OVERLAY:
				drawRateOfGrowth(g);
				break;
			case POPDEN_OVERLAY:
				drawPopDensity(g);
				break;
			default:
		}

		for (ConnectedView cv : views) {
			Rectangle rect = getViewRect(cv);
			g.setColor(Color.WHITE);
			g.drawRect(rect.x - 2, rect.y - 2, rect.width + 2, rect.height + 2);

			g.setColor(Color.BLACK);
			g.drawRect(rect.x, rect.y, rect.width + 2, rect.height + 2);

			g.setColor(Color.YELLOW);
			g.drawRect(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2);
		}
	}

	private void paintTile(BufferedImage img, int x, int y, int tile)
	{
		assert tile >= 0;
		BufferedImage tileImage = tileImages.getTileImage(tile);

		for (int yy = 0; yy < TILE_HEIGHT; yy++) {
			for (int xx = 0; xx < TILE_WIDTH; xx++) {
				img.setRGB(x * TILE_WIDTH + xx, y * TILE_HEIGHT + yy,
						tileImage.getRGB(xx, yy));
			}
		}
	}

	private Rectangle getViewRect(ConnectedView cv)
	{
		Rectangle rawRect = cv.scrollPane.getViewport().getViewRect();
		return new Rectangle(
				rawRect.x * 3 / cv.view.getTileWidth(),
				rawRect.y * 3 / cv.view.getTileWidth(),
				rawRect.width * 3 / cv.view.getTileWidth(),
				rawRect.height * 3 / cv.view.getTileWidth()
		);
	}

	private void dragViewTo(Point p)
	{
		if (views.isEmpty())
			return;

		ConnectedView cv = views.get(0);
		Dimension d = cv.scrollPane.getViewport().getExtentSize();
		Dimension mapSize = cv.scrollPane.getViewport().getViewSize();

		Point np = new Point(
				p.x * cv.view.getTileWidth() / 3 - d.width / 2,
				p.y * cv.view.getTileWidth() / 3 - d.height / 2
		);
		np.x = Math.max(0, Math.min(np.x, mapSize.width - d.width));
		np.y = Math.max(0, Math.min(np.y, mapSize.height - d.height));

		cv.scrollPane.getViewport().setViewPosition(np);
	}

	@Override
	public Dimension getPreferredScrollableViewportSize()
	{
		return new Dimension(120, 120);
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
	}

	@Override
	public boolean getScrollableTracksViewportWidth()
	{
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return orientation == SwingConstants.VERTICAL ? TILE_HEIGHT : TILE_WIDTH;
	}

	@Override
	public void mapOverlayDataChanged()
	{
		repaint();
	}

	@Override
	public void spriteMoved(Sprite sprite)
	{
	}

	@Override
	public void tileChanged(int xpos, int ypos)
	{
		Rectangle r = new Rectangle(xpos * TILE_WIDTH, ypos * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
		repaint(r);
	}

	@Override
	public void wholeMapChanged()
	{
		repaint();
		engine.calculateCenterMass();
		dragViewToCityCenter();
	}

	private void dragViewToCityCenter()
	{
		dragViewTo(new Point(TILE_WIDTH * engine.getCenterMassX() + 1,
				TILE_HEIGHT * engine.getCenterMassY() + 1));
	}

	public void connectView(MicropolisDrawingArea view, JScrollPane scrollPane)
	{
		ConnectedView cv = new ConnectedView(view, scrollPane);
		views.add(cv);
		repaint();
	}

	private class ConnectedView implements ChangeListener
	{
		private final MicropolisDrawingArea view;
		private final JScrollPane scrollPane;

		private ConnectedView(MicropolisDrawingArea view, JScrollPane scrollPane)
		{
			this.view = view;
			this.scrollPane = scrollPane;
			scrollPane.getViewport().addChangeListener(this);
		}

		@Override
		public void stateChanged(ChangeEvent e)
		{
			repaint();
		}
	}
}
