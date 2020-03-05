// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import java.util.Arrays;
import java.util.Random;

import static micropolisj.engine.TileConstants.CHANNEL;
import static micropolisj.engine.TileConstants.DIRT;
import static micropolisj.engine.TileConstants.LOMASK;
import static micropolisj.engine.TileConstants.REDGE;
import static micropolisj.engine.TileConstants.RIVEDGE;
import static micropolisj.engine.TileConstants.RIVER;
import static micropolisj.engine.TileConstants.WOODS;
import static micropolisj.engine.TileConstants.WOODS_HIGH;
import static micropolisj.engine.TileConstants.WOODS_LOW;
import static micropolisj.engine.TileConstants.isTree;

/**
 * Contains the code for generating a random map terrain.
 */
public class MapGenerator
{
	private static final char[][] BRMatrix = {
			{0, 0, 0, 3, 3, 3, 0, 0, 0},
			{0, 0, 3, 2, 2, 2, 3, 0, 0},
			{0, 3, 2, 2, 2, 2, 2, 3, 0},
			{3, 2, 2, 2, 2, 2, 2, 2, 3},
			{3, 2, 2, 2, 4, 2, 2, 2, 3},
			{3, 2, 2, 2, 2, 2, 2, 2, 3},
			{0, 3, 2, 2, 2, 2, 2, 3, 0},
			{0, 0, 3, 2, 2, 2, 3, 0, 0},
			{0, 0, 0, 3, 3, 3, 0, 0, 0}
	};
	private static final char[][] SRMatrix = {
			{0, 0, 3, 3, 0, 0},
			{0, 3, 2, 2, 3, 0},
			{3, 2, 2, 2, 2, 3},
			{3, 2, 2, 2, 2, 3},
			{0, 3, 2, 2, 3, 0},
			{0, 0, 3, 3, 0, 0}
	};
	private static final char[] REdTab = {
			RIVEDGE + 8, RIVEDGE + 8, RIVEDGE + 12, RIVEDGE + 10,
			RIVEDGE, RIVER, RIVEDGE + 14, RIVEDGE + 12,
			RIVEDGE + 4, RIVEDGE + 6, RIVER, RIVEDGE + 8,
			RIVEDGE + 2, RIVEDGE + 4, RIVEDGE, RIVER
	};
	private static final int[] DIRECTION_TABX = {0, 1, 1, 1, 0, -1, -1, -1};
	private static final int[] DIRECTION_TABY = {-1, -1, 0, 1, 1, 1, 0, -1};
	private static final int[] DX = {-1, 0, 1, 0};
	private static final int[] DY = {0, 1, 0, -1};
	private static final char[] TEdTab = {
			0, 0, 0, 34,
			0, 0, 36, 35,
			0, 32, 0, 33,
			30, 31, 29, 37
	};
	private final Micropolis engine;
	private final char[][] map;
	private final CreateIsland createIsland = CreateIsland.SELDOM;
	private Random random;
	private int xStart;
	private int yStart;
	private int mapX;
	private int mapY;
	private int dir;
	private int lastDir;
	public MapGenerator(Micropolis engine)
	{
		assert engine != null;
		this.engine = engine;
		map = engine.getMap();
	}

	private int getWidth()
	{
		return map[0].length;
	}

	private int getHeight()
	{
		return map.length;
	}

	/**
	 * Generate a random map terrain.
	 */
	public void generateNewCity()
	{
		long r = Micropolis.DEFAULT_PRNG.nextLong();
		generateSomeCity(r);
	}

	private void generateSomeCity(long r)
	{
		generateMap(r);
		engine.fireWholeMapChanged();
	}

	private void generateMap(long r)
	{
		random = new Random(r);

		if (createIsland == CreateIsland.SELDOM) {
			if (random.nextInt(100) < 10) //chance that island is generated
			{
				makeIsland();
				return;
			}
		}

		if (createIsland == CreateIsland.ALWAYS) {
			makeNakedIsland();
		} else {
			clearMap();
		}

		getRandStart();

		doRivers();

		makeLakes();

		smoothRiver();

		doTrees();
	}

	private void makeIsland()
	{
		makeNakedIsland();
		smoothRiver();
		doTrees();
	}

	private int erand()
	{
		return Math.min(
				random.nextInt(19),
				random.nextInt(19)
		);
	}

	private void makeNakedIsland()
	{
		int worldX = getWidth();
		int worldY = getHeight();

		for (int y = 0; y < worldY; y++) {
			for (int x = 0; x < worldX; x++) {
				map[y][x] = RIVER;
			}
		}

		for (int y = 5; y < worldY - 5; y++) {
			for (int x = 5; x < worldX - 5; x++) {
				map[y][x] = DIRT;
			}
		}

		for (int x = 0; x < worldX - 5; x += 2) {
			mapX = x;
			mapY = erand();
			BRivPlop();
			mapY = worldY - 10 - erand();
			BRivPlop();
			mapY = 0;
			SRivPlop();
			mapY = worldY - 6;
			SRivPlop();
		}

		for (int y = 0; y < worldY - 5; y += 2) {
			mapY = y;
			mapX = erand();
			BRivPlop();
			mapX = worldX - 10 - erand();
			BRivPlop();
			mapX = 0;
			SRivPlop();
			mapX = worldX - 6;
			SRivPlop();
		}
	}

	private void clearMap()
	{
		for (char[] chars : map) {
			Arrays.fill(chars, DIRT);
		}
	}

	private void getRandStart()
	{
		xStart = 40 + random.nextInt(getWidth() - 79);
		yStart = 33 + random.nextInt(getHeight() - 66);

		mapX = xStart;
		mapY = yStart;
	}

	private void makeLakes()
	{
		int lim1;
		lim1 = random.nextInt(11);

		for (int t = 0; t < lim1; t++) {
			int x = random.nextInt(getWidth() - 20) + 10;
			int y = random.nextInt(getHeight() - 19) + 10;
			int lim2 = random.nextInt(13) + 2;

			for (int z = 0; z < lim2; z++) {
				mapX = x - 6 + random.nextInt(13);
				mapY = y - 6 + random.nextInt(13);

				if (random.nextInt(5) == 0) BRivPlop();
				else SRivPlop();
			}
		}
	}

	private void doRivers()
	{
		dir = lastDir = random.nextInt(4);
		doBRiv();

		mapX = xStart;
		mapY = yStart;
		dir = lastDir ^= 4;
		doBRiv();

		mapX = xStart;
		mapY = yStart;
		lastDir = random.nextInt(4);
		doSRiv();
	}

	private void doBRiv()
	{
		int r1, r2;
		r1 = 100;
		r2 = 200;

		while (engine.testBounds(mapX + 4, mapY + 4)) {
			BRivPlop();
			if (random.nextInt(r1 + 1) < 10) {
				dir = lastDir;
			} else {
				if (random.nextInt(r2 + 1) > 90) {
					dir++;
				}
				if (random.nextInt(r2 + 1) > 90) {
					dir--;
				}
			}
			moveMap(dir);
		}
	}

	private void doSRiv()
	{
		int r1, r2;
		r1 = 100;
		r2 = 200;

		while (engine.testBounds(mapX + 3, mapY + 3)) {
			SRivPlop();
			if (random.nextInt(r1 + 1) < 10) {
				dir = lastDir;
			} else {
				if (random.nextInt(r2 + 1) > 90) {
					dir++;
				}
				if (random.nextInt(r2 + 1) > 90) {
					dir--;
				}
			}
			moveMap(dir);
		}
	}

	private void BRivPlop()
	{
		for (int x = 0; x < 9; x++) {
			for (int y = 0; y < 9; y++) {
				putOnMap(BRMatrix[y][x], x, y);
			}
		}
	}

	private void SRivPlop()
	{
		for (int x = 0; x < 6; x++) {
			for (int y = 0; y < 6; y++) {
				putOnMap(SRMatrix[y][x], x, y);
			}
		}
	}

	private void putOnMap(char mapChar, int xoff, int yoff)
	{
		if (mapChar == 0)
			return;

		int xloc = mapX + xoff;
		int yloc = mapY + yoff;

		if (!engine.testBounds(xloc, yloc))
			return;

		char tmp = map[yloc][xloc];
		if (tmp != DIRT) {
			tmp &= LOMASK;
			if (tmp == RIVER && mapChar != CHANNEL)
				return;
			if (tmp == CHANNEL)
				return;
		}
		map[yloc][xloc] = mapChar;
	}

	private void smoothRiver()
	{
		for (int mapY = 0; mapY < map.length; mapY++) {
			for (int mapX = 0; mapX < map[mapY].length; mapX++) {
				if (map[mapY][mapX] == REDGE) {
					int bitindex = 0;

					for (int z = 0; z < 4; z++) {
						bitindex <<= 1;
						int xtem = mapX + DX[z];
						int ytem = mapY + DY[z];
						if (engine.testBounds(xtem, ytem) &&
								(map[ytem][xtem] & LOMASK) != DIRT &&
								((map[ytem][xtem] & LOMASK) < WOODS_LOW ||
										(map[ytem][xtem] & LOMASK) > WOODS_HIGH)) {
							bitindex |= 1;
						}
					}

					char temp = REdTab[bitindex & 15];
					if (temp != RIVER && random.nextInt(2) != 0)
						temp++;
					map[mapY][mapX] = temp;
				}
			}
		}
	}

	private void doTrees()
	{
		int amount;

		amount = random.nextInt(101) + 50;

		for (int x = 0; x < amount; x++) {
			int xloc = random.nextInt(getWidth());
			int yloc = random.nextInt(getHeight());
			treeSplash(xloc, yloc);
		}

		smoothTrees();
		smoothTrees();
	}

	private void treeSplash(int xloc, int yloc)
	{
		int dis;
		dis = random.nextInt(151) + 50;

		mapX = xloc;
		mapY = yloc;

		for (int z = 0; z < dis; z++) {
			int dir = random.nextInt(8);
			moveMap(dir);

			if (!engine.testBounds(mapX, mapY))
				return;

			if ((map[mapY][mapX] & LOMASK) == DIRT) {
				map[mapY][mapX] = WOODS;
			}
		}
	}

	private void moveMap(int dir)
	{
		dir &= 7;
		mapX += DIRECTION_TABX[dir];
		mapY += DIRECTION_TABY[dir];
	}

	private void smoothTrees()
	{
		for (int mapY = 0; mapY < map.length; mapY++) {
			for (int mapX = 0; mapX < map[mapY].length; mapX++) {
				if (isTree(map[mapY][mapX])) {
					int bitindex = 0;
					for (int z = 0; z < 4; z++) {
						bitindex <<= 1;
						int xtem = mapX + DX[z];
						int ytem = mapY + DY[z];
						if (engine.testBounds(xtem, ytem) &&
								isTree(map[ytem][xtem])) {
							bitindex |= 1;
						}
					}
					char temp = TEdTab[bitindex & 15];
					if (temp != 0) {
						if (temp != WOODS) {
							if ((mapX + mapY & 1) != 0) {
								temp -= 8;
							}
						}
					}
					map[mapY][mapX] = temp;
				}
			}
		}
	}

	/**
	 * Three settings on whether to generate a new map as an island.
	 */
	enum CreateIsland
	{
		ALWAYS,
		SELDOM   // seldom == 10% of the time
	}

}
