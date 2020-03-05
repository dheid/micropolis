// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.gui;

import micropolisj.engine.CityLocation;
import micropolisj.engine.CityRect;
import micropolisj.engine.MapListener;
import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisTool;
import micropolisj.engine.Sprite;
import micropolisj.engine.ToolPreview;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import static micropolisj.engine.TileConstants.CLEAR;
import static micropolisj.engine.TileConstants.LIGHTNINGBOLT;
import static micropolisj.engine.TileConstants.isZoneCenter;
import static micropolisj.gui.ColorParser.parseColor;

public class MicropolisDrawingArea extends JComponent
		implements Scrollable, MapListener
{
	static final int SHAKE_STEPS = 40;
	private static final Dimension PREFERRED_VIEWPORT_SIZE = new Dimension(640, 640);
	private static final ResourceBundle strings = MainWindow.strings;
	private static final int DEFAULT_TILE_SIZE = 16;
	private final Collection<Point> unpoweredZones = new HashSet<>();
	private Micropolis m;
	private boolean blink;
	private Timer blinkTimer;
	private ToolCursor toolCursor;
	private ToolPreview toolPreview;
	private int shakeStep;
	private TileImages tileImages;
	private int tileWidth;
	private int tileHeight;
	private int dragX;
	private int dragY;
	private boolean dragging;

	public MicropolisDrawingArea(Micropolis engine)
	{
		m = engine;
		selectTileSize(DEFAULT_TILE_SIZE);
		m.addMapListener(this);

		addAncestorListener(new AncestorListener()
		{
			@Override
			public void ancestorAdded(AncestorEvent event)
			{
				startBlinkTimer();
			}

			@Override
			public void ancestorRemoved(AncestorEvent event)
			{
				stopBlinkTimer();
			}

			private void startBlinkTimer()
			{
				assert blinkTimer == null;

				ActionListener callback = evt -> doBlink();

				blinkTimer = new Timer(500, callback);
				blinkTimer.start();
			}

			private void doBlink()
			{
				if (!unpoweredZones.isEmpty()) {
					blink = !blink;
					for (Point loc : unpoweredZones) {
						repaint(getTileBounds(loc.x, loc.y));
					}
					unpoweredZones.clear();
				}
			}

			private void stopBlinkTimer()
			{
				if (blinkTimer != null) {
					blinkTimer.stop();
					blinkTimer = null;
				}
			}

			@Override
			public void ancestorMoved(AncestorEvent event)
			{
			}
		});

		addMouseListener(new MouseAdapter()
		{

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON2)
					startDrag(e.getX(), e.getY());
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON2)
					endDrag();
			}

			private void startDrag(int x, int y)
			{
				dragging = true;
				dragX = x;
				dragY = y;
			}

			private void endDrag()
			{
				dragging = false;
			}

		});

		addMouseMotionListener(new MouseMotionAdapter()
		{

			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (dragging)
					continueDrag(e.getX(), e.getY());
			}


			private void continueDrag(int x, int y)
			{
				int dx = x - dragX;
				int dy = y - dragY;
				JScrollPane js = (JScrollPane) getParent().getParent();
				js.getHorizontalScrollBar().setValue(
						js.getHorizontalScrollBar().getValue() - dx);
				js.getVerticalScrollBar().setValue(
						js.getVerticalScrollBar().getValue() - dy);
			}

		});
	}

	public void selectTileSize(int newTileSize)
	{
		tileImages = TileImages.getInstance(newTileSize);
		tileWidth = tileImages.getTileWidth();
		tileHeight = tileImages.getTileHeight();
		revalidate();
	}

	public int getTileWidth()
	{
		return tileWidth;
	}

	public CityLocation getCityLocation(int x, int y)
	{
		return new CityLocation(x / tileWidth, y / tileHeight);
	}

	@Override
	public Dimension getPreferredSize()
	{
		assert m != null;

		return new Dimension(tileWidth * m.getWidth(), tileHeight * m.getHeight());
	}

	public void setEngine(Micropolis newEngine)
	{
		assert newEngine != null;

		if (m != null) { //old engine
			m.removeMapListener(this);
		}
		m = newEngine;
		//new engine
		m.addMapListener(this);

		// size may have changed
		invalidate();
		repaint();
	}

	private void drawSprite(Graphics gr, Sprite sprite)
	{
		assert sprite.isVisible();

		Point p = new Point(
				(sprite.getX() + sprite.getOffx()) * tileWidth / 16,
				(sprite.getY() + sprite.getOffy()) * tileHeight / 16
		);

		Image img = tileImages.getSpriteImage(sprite.getKind(), sprite.getFrame() - 1);
		if (img != null) {
			gr.drawImage(img, p.x, p.y, null);
		} else {
			gr.setColor(Color.RED);
			gr.fillRect(p.x, p.y, 16, 16);
			gr.setColor(Color.WHITE);
			gr.drawString(Integer.toString(sprite.getFrame() - 1), p.x, p.y);
		}
	}

	@Override
	public void paintComponent(Graphics g)
	{
		int width = m.getWidth();
		int height = m.getHeight();

		Rectangle clipRect = g.getClipBounds();
		int minX = Math.max(0, clipRect.x / tileWidth);
		int minY = Math.max(0, clipRect.y / tileHeight);
		int maxX = Math.min(width, 1 + (clipRect.x + clipRect.width - 1) / tileWidth);
		int maxY = Math.min(height, 1 + (clipRect.y + clipRect.height - 1) / tileHeight);

		for (int y = minY; y < maxY; y++) {
			for (int x = maxX - 1; x >= minX; x--) {
				int cell = m.getTile(x, y);
				boolean blinkUnpoweredZones = true;
				if (blinkUnpoweredZones &&
						isZoneCenter(cell) &&
						!m.isTilePowered(x, y)) {
					unpoweredZones.add(new Point(x, y));
					if (blink)
						cell = LIGHTNINGBOLT;
				}

				if (toolPreview != null) {
					int c = toolPreview.getTile(x, y);
					if (c != CLEAR) {
						cell = c;
					}
				}

				g.drawImage(tileImages.getTileImage(cell),
						x * tileWidth + (shakeStep != 0 ? getShakeModifier(y) : 0),
						y * tileHeight,
						null);
			}
		}

		for (Sprite sprite : m.allSprites()) {
			if (sprite.isVisible()) {
				drawSprite(g, sprite);
			}
		}

		if (toolCursor != null) {
			int x0 = toolCursor.rect.getX() * tileWidth;
			int x1 = (toolCursor.rect.getX() + toolCursor.rect.getWidth()) * tileWidth;
			int y0 = toolCursor.rect.getY() * tileHeight;
			int y1 = (toolCursor.rect.getY() + toolCursor.rect.getHeight()) * tileHeight;

			g.setColor(Color.BLACK);
			g.fillRect(x0 - 1, y0 - 1, x1 - (x0 - 1), 1);
			g.fillRect(x0 - 1, y0, 1, y1 - y0);
			g.fillRect(x0 - 3, y1 + 3, x1 + 4 - (x0 - 3), 1);
			g.fillRect(x1 + 3, y0 - 3, 1, y1 + 3 - (y0 - 3));

			g.setColor(Color.WHITE);
			g.fillRect(x0 - 4, y0 - 4, x1 + 4 - (x0 - 4), 1);
			g.fillRect(x0 - 4, y0 - 3, 1, y1 + 4 - (y0 - 3));
			g.fillRect(x0 - 1, y1, x1 + 1 - (x0 - 1), 1);
			g.fillRect(x1, y0 - 1, 1, y1 - (y0 - 1));

			g.setColor(toolCursor.borderColor);
			g.fillRect(x0 - 3, y0 - 3, x1 + 1 - (x0 - 3), 2);
			g.fillRect(x1 + 1, y0 - 3, 2, y1 + 1 - (y0 - 3));
			g.fillRect(x0 - 1, y1 + 1, x1 + 3 - (x0 - 1), 2);
			g.fillRect(x0 - 3, y0 - 1, 2, y1 + 3 - (y0 - 1));

			if (toolCursor.fillColor != null) {
				g.setColor(toolCursor.fillColor);
				g.fillRect(x0, y0, x1 - x0, y1 - y0);
			}
		}
	}

	public void setToolCursor(CityRect newRect, MicropolisTool tool)
	{
		ToolCursor tp = new ToolCursor();
		tp.rect = newRect;
		tp.borderColor = parseColor(
				strings.containsKey("tool." + tool.name() + ".border") ?
						strings.getString("tool." + tool.name() + ".border") :
						strings.getString("tool.*.border")
		);
		tp.fillColor = parseColor(
				strings.containsKey("tool." + tool.name() + ".bgcolor") ?
						strings.getString("tool." + tool.name() + ".bgcolor") :
						strings.getString("tool.*.bgcolor")
		);
		setToolCursor(tp);
	}

	public void setToolCursor(ToolCursor newCursor)
	{
		if (toolCursor == newCursor)
			return;
		if (toolCursor != null && toolCursor.equals(newCursor))
			return;

		if (toolCursor != null) {
			repaint(new Rectangle(
					toolCursor.rect.getX() * tileWidth - 4,
					toolCursor.rect.getY() * tileHeight - 4,
					toolCursor.rect.getWidth() * tileWidth + 8,
					toolCursor.rect.getHeight() * tileHeight + 8
			));
		}
		toolCursor = newCursor;
		if (toolCursor != null) {
			repaint(new Rectangle(
					toolCursor.rect.getX() * tileWidth - 4,
					toolCursor.rect.getY() * tileHeight - 4,
					toolCursor.rect.getWidth() * tileWidth + 8,
					toolCursor.rect.getHeight() * tileHeight + 8
			));
		}
	}

	public void setToolPreview(ToolPreview newPreview)
	{
		if (toolPreview != null) {
			CityRect b = toolPreview.getBounds();
			Rectangle r = new Rectangle(
					b.getX() * tileWidth,
					b.getY() * tileHeight,
					b.getWidth() * tileWidth,
					b.getHeight() * tileHeight
			);
			repaint(r);
		}

		toolPreview = newPreview;
		if (toolPreview != null) {

			CityRect b = toolPreview.getBounds();
			Rectangle r = new Rectangle(
					b.getX() * tileWidth,
					b.getY() * tileHeight,
					b.getWidth() * tileWidth,
					b.getHeight() * tileHeight
			);
			repaint(r);
		}
	}

	@Override
	public Dimension getPreferredScrollableViewportSize()
	{
		return PREFERRED_VIEWPORT_SIZE;
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
		return orientation == SwingConstants.VERTICAL ? tileHeight * 3 : tileWidth * 3;
	}

	private Rectangle getSpriteBounds(Sprite sprite, int x, int y)
	{
		return new Rectangle(
				(x + sprite.getOffx()) * tileWidth / 16,
				(y + sprite.getOffy()) * tileHeight / 16,
				sprite.getWidth() * tileWidth / 16,
				sprite.getHeight() * tileHeight / 16
		);
	}

	public Rectangle getTileBounds(int xpos, int ypos)
	{
		return new Rectangle(xpos * tileWidth, ypos * tileHeight,
				tileWidth, tileHeight);
	}

	@Override
	public void mapOverlayDataChanged()
	{
	}

	@Override
	public void spriteMoved(Sprite sprite)
	{
		repaint(getSpriteBounds(sprite, sprite.getLastX(), sprite.getLastY()));
		repaint(getSpriteBounds(sprite, sprite.getX(), sprite.getY()));
	}

	@Override
	public void tileChanged(int xpos, int ypos)
	{
		repaint(getTileBounds(xpos, ypos));
	}

	@Override
	public void wholeMapChanged()
	{
		repaint();
	}

	void shake(int i)
	{
		shakeStep = i;
		repaint();
	}

	private int getShakeModifier(int row)
	{
		return (int) Math.round(4.0 * StrictMath.sin((shakeStep + row / 2) / 2.0));
	}

	private static class ToolCursor
	{
		private CityRect rect;
		private Color borderColor;
		private Color fillColor;
	}
}
