// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import java.util.Random;

abstract class TileBehavior
{
	private final Micropolis city;

	private final Random random;

	private int xpos;

	private int ypos;

	private int tile;

	TileBehavior(Micropolis city)
	{
		this.city = city;
		random = city.getRandom();
	}

	public void processTile(int xpos, int ypos)
	{
		this.xpos = xpos;
		this.ypos = ypos;
		tile = city.getTile(xpos, ypos);
		apply();
	}

	/**
	 * Activate the tile identified by xpos and ypos properties.
	 */
	protected abstract void apply();

	public Micropolis getCity()
	{
		return city;
	}

	public Random getRandom()
	{
		return random;
	}

	public int getXpos()
	{
		return xpos;
	}

	public int getYpos()
	{
		return ypos;
	}

	public int getTile()
	{
		return tile;
	}

	public void setTile(int tile)
	{
		this.tile = tile;
	}
}
