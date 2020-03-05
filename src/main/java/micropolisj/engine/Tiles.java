// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides global methods for loading tile specifications.
 */
class Tiles
{
	static final Map<String, TileSpec> tilesByName = new HashMap<>();
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static TileSpec[] tiles;

	static {
		try {
			readTiles();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void readTiles()
			throws IOException
	{
		Properties tilesRc = new Properties();
		tilesRc.load(
				new InputStreamReader(
						Tiles.class.getResourceAsStream("/graphics/tiles.rc"),
						UTF8
				)
		);

		String[] tileNames = TileSpec.generateTileNames(tilesRc);
		tiles = new TileSpec[tileNames.length];

		for (int i = 0; i < tileNames.length; i++) {
			String tileName = tileNames[i];
			String rawSpec = tilesRc.getProperty(tileName);
			if (rawSpec == null) {
				break;
			}

			TileSpec ts = TileSpec.parse(i, tileName, rawSpec, tilesRc);
			tilesByName.put(tileName, ts);
			tiles[i] = ts;
		}

		for (TileSpec tile : tiles) {
			tile.resolveReferences();

			BuildingInfo bi = tile.getBuildingInfo();
			if (bi != null) {
				for (int j = 0; j < bi.getMembers().length; j++) {
					int tid = bi.getMembers()[j];
					int offx = (bi.getWidth() >= 3 ? -1 : 0) + j % bi.getWidth();
					int offy = (bi.getHeight() >= 3 ? -1 : 0) + j / bi.getWidth();

					if (tiles[tid].getOwner() == null &&
							(offx != 0 || offy != 0)
					) {
						tiles[tid].setOwner(tile);
						tiles[tid].setOwnerOffsetX(offx);
						tiles[tid].setOwnerOffsetY(offy);
					}
				}
			}
		}
	}

	/**
	 * Access a tile specification by index number.
	 *
	 * @return a tile specification, or null if there is no tile
	 * with the given number
	 */
	public static TileSpec get(int tileNumber)
	{
		return tileNumber >= 0 && tileNumber < tiles.length ? tiles[tileNumber] : null;
	}

}
