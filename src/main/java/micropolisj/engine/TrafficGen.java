// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import java.util.Stack;

import static micropolisj.engine.TileConstants.COMBASE;
import static micropolisj.engine.TileConstants.LASTPOWER;
import static micropolisj.engine.TileConstants.LASTRAIL;
import static micropolisj.engine.TileConstants.LHTHR;
import static micropolisj.engine.TileConstants.NUCLEAR;
import static micropolisj.engine.TileConstants.PORT;
import static micropolisj.engine.TileConstants.POWERBASE;
import static micropolisj.engine.TileConstants.ROADBASE;

/**
 * Contains the code for generating city traffic.
 */
public class TrafficGen
{
	private static final int MAX_TRAFFIC_DISTANCE = 30;
	private static final int[] PerimX = {-1, 0, 1, 2, 2, 2, 1, 0, -1, -2, -2, -2};
	private static final int[] PerimY = {-2, -2, -2, -1, 0, 1, 2, 2, 2, 1, 0, -1};
	private static final int[] DX = {0, 1, 0, -1};
	private static final int[] DY = {-1, 0, 1, 0};
	private final Micropolis city;
	private final Stack<CityLocation> positions = new Stack<>();
	private int mapX;
	private int mapY;
	private ZoneType sourceZone;
	private int lastdir;
	public TrafficGen(Micropolis city)
	{
		this.city = city;
	}

	int makeTraffic()
	{
		if (findPerimeterRoad()) //look for road on this zone's perimeter
		{
			if (tryDrive())  //attempt to drive somewhere
			{
				// success; incr trafdensity
				setTrafficMem();
				return 1;
			}

			return 0;
		} else {
			// no road found
			return -1;
		}
	}

	private void setTrafficMem()
	{
		while (!positions.isEmpty()) {
			CityLocation pos = positions.pop();
			mapX = pos.getX();
			mapY = pos.getY();
			assert city.testBounds(mapX, mapY);

			// check for road/rail
			int tile = city.getTile(mapX, mapY);
			if (tile >= ROADBASE && tile < POWERBASE) {
				city.addTraffic(mapX, mapY);
			}
		}
	}

	boolean findPerimeterRoad()
	{
		for (int z = 0; z < 12; z++) {
			int tx = mapX + PerimX[z];
			int ty = mapY + PerimY[z];

			if (roadTest(tx, ty)) {
				mapX = tx;
				mapY = ty;
				return true;
			}
		}
		return false;
	}

	private boolean roadTest(int tx, int ty)
	{
		if (!city.testBounds(tx, ty)) {
			return false;
		}

		char c = city.getTile(tx, ty);

		if (c < ROADBASE)
			return false;
		else if (c > LASTRAIL)
			return false;
		else return c < POWERBASE || c >= LASTPOWER;
	}

	private boolean tryDrive()
	{
		lastdir = 5;
		positions.clear();

		for (int z = 0; z < MAX_TRAFFIC_DISTANCE; z++) //maximum distance to try
		{
			if (tryGo(z)) {
				// got a road
				if (driveDone()) {
					// destination reached
					return true;
				}
			} else {
				// deadend, try backing up
				if (positions.isEmpty()) {
					return false;
				} else {
					positions.pop();
					z += 3;
				}
			}
		}

		// gone maxdis
		return false;
	}

	private boolean tryGo(int z)
	{
		// random starting direction
		int rdir = city.getRandom().nextInt(4);

		for (int d = rdir; d < rdir + 4; d++) {
			int realdir = d % 4;
			if (realdir == lastdir)
				continue;

			if (roadTest(mapX + DX[realdir], mapY + DY[realdir])) {
				mapX += DX[realdir];
				mapY += DY[realdir];
				lastdir = (realdir + 2) % 4;

				if (z % 2 != 0) {
					// save pos every other move
					positions.push(new CityLocation(mapX, mapY));
				}

				return true;
			}
		}

		return false;
	}

	private boolean driveDone()
	{
		int low, high;
		switch (sourceZone) {
			case RESIDENTIAL:
				low = COMBASE;
				high = NUCLEAR;
				break;
			case COMMERCIAL:
				low = LHTHR;
				high = PORT;
				break;
			case INDUSTRIAL:
				low = LHTHR;
				high = COMBASE;
				break;
			default:
				throw new RuntimeException("unreachable");
		}

		if (mapY > 0) {
			int tile = city.getTile(mapX, mapY - 1);
			if (tile >= low && tile <= high)
				return true;
		}
		if (mapX + 1 < city.getWidth()) {
			int tile = city.getTile(mapX + 1, mapY);
			if (tile >= low && tile <= high)
				return true;
		}
		if (mapY + 1 < city.getHeight()) {
			int tile = city.getTile(mapX, mapY + 1);
			if (tile >= low && tile <= high)
				return true;
		}
		if (mapX > 0) {
			int tile = city.getTile(mapX - 1, mapY);
			return tile >= low && tile <= high;
		}
		return false;
	}

	public void setMapX(int mapX)
	{
		this.mapX = mapX;
	}

	public void setMapY(int mapY)
	{
		this.mapY = mapY;
	}

	public void setSourceZone(ZoneType sourceZone)
	{
		this.sourceZone = sourceZone;
	}
}
