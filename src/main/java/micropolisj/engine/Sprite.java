// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import static micropolisj.engine.TileConstants.RIVER;
import static micropolisj.engine.TileConstants.RZB;
import static micropolisj.engine.TileConstants.TINYEXP;
import static micropolisj.engine.TileConstants.TREEBASE;
import static micropolisj.engine.TileConstants.checkWet;
import static micropolisj.engine.TileConstants.isBridge;
import static micropolisj.engine.TileConstants.isCombustible;
import static micropolisj.engine.TileConstants.isZoneCenter;

/**
 * Represents a mobile entity on the city map, such as a tornado
 * or a train. There can be any number present in a city, and each one
 * gets a chance to act on every tick of the simulation.
 */
public abstract class Sprite
{
	private final SpriteKind kind;

	//TODO- enforce read-only nature of the following properties
	// (i.e. do not let them be modified directly by other classes)
	private final Micropolis city;
	private int offx;
	private int offy;
	private int width = 32;
	private int height = 32;

	private int frame;
	private int x;
	private int y;

	private int lastX;
	private int lastY;

	private int dir;

	Sprite(Micropolis engine, SpriteKind kind)
	{
		city = engine;
		this.kind = kind;
	}

	/**
	 * Computes direction from one point to another.
	 *
	 * @return integer between 1 and 8, with
	 * 1 == north,
	 * 3 == east,
	 * 5 == south,
	 * 7 == west.
	 */
	static int getDir(int orgX, int orgY, int desX, int desY)
	{
		int[] gdtab = {0, 3, 2, 1, 3, 4, 5, 7, 6, 5, 7, 8, 1};
		int dispX = desX - orgX;
		int dispY = desY - orgY;

		int z = dispX < 0 ? dispY < 0 ? 11 : 8 : dispY < 0 ? 2 : 5;

		dispX = Math.abs(dispX);
		dispY = Math.abs(dispY);

		if (dispX * 2 < dispY) z++;
		else if (dispY * 2 < dispX) z--;

		return gdtab[z];
	}

	/**
	 * Computes manhatten distance between two points.
	 */
	static int getDis(int x0, int y0, int x1, int y1)
	{
		return Math.abs(x0 - x1) + Math.abs(y0 - y1);
	}

	/**
	 * Helper function for rotating a sprite.
	 *
	 * @param p the sprite's current attitude (1-8)
	 * @param d the desired attitude (1-8)
	 * @return the new attitude
	 */
	static int turnTo(int p, int d)
	{
		if (p == d)
			return p;
		if (p < d) {
			if (d - p < 4) p++;
			else p--;
		} else {
			if (p - d < 4) p--;
			else p++;
		}
		if (p > 8) return 1;
		if (p < 1) return 8;
		return p;
	}

	int getChar(int x, int y)
	{
		int xpos = x / 16;
		int ypos = y / 16;
		return city.testBounds(xpos, ypos) ? city.getTile(xpos, ypos) : -1;
	}

	/**
	 * For subclasses to override. Actually does the movement and animation
	 * of this particular sprite. Setting this.frame to zero will cause the
	 * sprite to be unallocated.
	 */
	protected abstract void moveImpl();

	/**
	 * Perform this agent's movement and animation.
	 */
	public void move()
	{
		lastX = x;
		lastY = y;
		moveImpl();
		city.fireSpriteMoved(this);
	}

	/**
	 * Tells whether this sprite is visible.
	 */
	public boolean isVisible()
	{
		return frame != 0;
	}

	/**
	 * Replaces this sprite with an exploding sprite.
	 */
	void explodeSprite()
	{
		frame = 0;

		city.makeExplosionAt(x, y);
		int xpos = x / 16;
		int ypos = y / 16;

		switch (kind) {
			case AIR:
				city.sendMessageAt(MicropolisMessage.PLANECRASH_REPORT, xpos, ypos);
				break;
			case SHI:
				city.sendMessageAt(MicropolisMessage.SHIPWRECK_REPORT, xpos, ypos);
				break;
			case TRA:
			case BUS:
				city.sendMessageAt(MicropolisMessage.TRAIN_CRASH_REPORT, xpos, ypos);
				break;
			case COP:
				city.sendMessageAt(MicropolisMessage.COPTER_CRASH_REPORT, xpos, ypos);
				break;
		}

		city.makeSound(xpos, ypos, Sound.EXPLOSION_HIGH);
	}

	/**
	 * Checks whether another sprite is in collision ranges.
	 *
	 * @return true iff the sprite is in collision range
	 */
	boolean checkSpriteCollision(Sprite otherSprite)
	{
		if (!isVisible()) return false;
		if (!otherSprite.isVisible()) return false;

		return getDis(x, y, otherSprite.x, otherSprite.y) < 30;
	}

	/**
	 * Destroys whatever is at the specified location,
	 * replacing it with fire, rubble, or water as appropriate.
	 */
	void destroyTile(int xpos, int ypos)
	{
		if (!city.testBounds(xpos, ypos))
			return;

		int t = city.getTile(xpos, ypos);

		if (t >= TREEBASE) {
			if (isBridge(t)) {
				city.setTile(xpos, ypos, RIVER);
				return;
			}
			if (!isCombustible(t)) {
				return; //cannot destroy it
			}
			if (isZoneCenter(t)) {
				city.killZone(xpos, ypos, t);
				if (t > RZB) {
					city.makeExplosion(xpos, ypos);
				}
			}
			if (checkWet(t)) {
				city.setTile(xpos, ypos, RIVER);
			} else {
				city.setTile(xpos, ypos, TINYEXP);
			}
		}
	}

	public SpriteKind getKind()
	{
		return kind;
	}

	public Micropolis getCity()
	{
		return city;
	}

	public int getOffx()
	{
		return offx;
	}

	public void setOffx(int offx)
	{
		this.offx = offx;
	}

	public int getOffy()
	{
		return offy;
	}

	public void setOffy(int offy)
	{
		this.offy = offy;
	}

	public int getWidth()
	{
		return width;
	}

	public void setWidth(int width)
	{
		this.width = width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(int height)
	{
		this.height = height;
	}

	public int getFrame()
	{
		return frame;
	}

	public void setFrame(int frame)
	{
		this.frame = frame;
	}

	public int getX()
	{
		return x;
	}

	public void setX(int x)
	{
		this.x = x;
	}

	public int getY()
	{
		return y;
	}

	public void setY(int y)
	{
		this.y = y;
	}

	public int getLastX()
	{
		return lastX;
	}

	public int getLastY()
	{
		return lastY;
	}

	public int getDir()
	{
		return dir;
	}

	public void setDir(int dir)
	{
		this.dir = dir;
	}
}
