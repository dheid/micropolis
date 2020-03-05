// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import static micropolisj.engine.TileConstants.BRWH;
import static micropolisj.engine.TileConstants.BRWV;
import static micropolisj.engine.TileConstants.CHANNEL;
import static micropolisj.engine.TileConstants.DIRT;
import static micropolisj.engine.TileConstants.FIRE;
import static micropolisj.engine.TileConstants.FLOOD;
import static micropolisj.engine.TileConstants.HBRDG0;
import static micropolisj.engine.TileConstants.HBRDG1;
import static micropolisj.engine.TileConstants.HBRDG2;
import static micropolisj.engine.TileConstants.HBRDG3;
import static micropolisj.engine.TileConstants.HBRIDGE;
import static micropolisj.engine.TileConstants.HTRFBASE;
import static micropolisj.engine.TileConstants.IZB;
import static micropolisj.engine.TileConstants.LTRFBASE;
import static micropolisj.engine.TileConstants.RIVER;
import static micropolisj.engine.TileConstants.ROADBASE;
import static micropolisj.engine.TileConstants.RUBBLE;
import static micropolisj.engine.TileConstants.VBRDG0;
import static micropolisj.engine.TileConstants.VBRDG1;
import static micropolisj.engine.TileConstants.VBRDG2;
import static micropolisj.engine.TileConstants.VBRDG3;
import static micropolisj.engine.TileConstants.VBRIDGE;
import static micropolisj.engine.TileConstants.WOODS5;
import static micropolisj.engine.TileConstants.isCombustible;
import static micropolisj.engine.TileConstants.isConductive;
import static micropolisj.engine.TileConstants.isOverWater;
import static micropolisj.engine.TileConstants.isZoneCenter;

class TerrainBehavior extends TileBehavior
{
	private static final int[] TRAFFIC_DENSITY_TAB = {ROADBASE, LTRFBASE, HTRFBASE};
	private final B behavior;

	TerrainBehavior(Micropolis city, B behavior)
	{
		super(city);
		this.behavior = behavior;
	}

	@Override
	public void apply()
	{
		switch (behavior) {
			case FIRE:
				doFire();
				return;
			case FLOOD:
				doFlood();
				return;
			case RADIOACTIVE:
				doRadioactiveTile();
				return;
			case ROAD:
				doRoad();
				return;
			case RAIL:
				doRail();
				return;
			case EXPLOSION:
				doExplosion();
				return;
			default:
				assert false;
		}
	}

	private void doFire()
	{
		getCity().setFirePop(getCity().getFirePop() + 1);

		// one in four times
		if (getRandom().nextInt(4) != 0) {
			return;
		}

		int[] dx = {0, 1, 0, -1};
		int[] dy = {-1, 0, 1, 0};

		for (int dir = 0; dir < 4; dir++) {
			if (getRandom().nextInt(8) == 0) {
				int xtem = getXpos() + dx[dir];
				int ytem = getYpos() + dy[dir];
				if (!getCity().testBounds(xtem, ytem))
					continue;

				int c = getCity().getTile(xtem, ytem);
				if (isCombustible(c)) {
					if (isZoneCenter(c)) {
						getCity().killZone(xtem, ytem, c);
						if (c > IZB) { //explode
							getCity().makeExplosion(xtem, ytem);
						}
					}
					getCity().setTile(xtem, ytem, (char) (FIRE + getRandom().nextInt(4)));
				}
			}
		}

		int cov = getCity().getFireStationCoverage(getXpos(), getYpos());
		int rate = cov > 100 ? 1 :
				cov > 20 ? 2 :
						cov != 0 ? 3 : 10;

		if (getRandom().nextInt(rate + 1) == 0) {
			getCity().setTile(getXpos(), getYpos(), (char) (RUBBLE + getRandom().nextInt(4)));
		}
	}

	/**
	 * Called when the current tile is a flooding tile.
	 */
	private void doFlood()
	{
		int[] dx = {0, 1, 0, -1};
		int[] dy = {-1, 0, 1, 0};

		if (getCity().getFloodCnt() == 0) {
			if (getRandom().nextInt(16) == 0) {
				getCity().setTile(getXpos(), getYpos(), DIRT);
			}
		} else {
			for (int z = 0; z < 4; z++) {
				if (getRandom().nextInt(8) == 0) {
					int xx = getXpos() + dx[z];
					int yy = getYpos() + dy[z];
					if (getCity().testBounds(xx, yy)) {
						int t = getCity().getTile(xx, yy);
						if (isCombustible(t)
								|| t == DIRT
								|| t >= WOODS5 && t < FLOOD) {
							if (isZoneCenter(t)) {
								getCity().killZone(xx, yy, t);
							}
							getCity().setTile(xx, yy, (char) (FLOOD + getRandom().nextInt(3)));
						}
					}
				}
			}
		}
	}

	/**
	 * Called when the current tile is a radioactive tile.
	 */
	private void doRadioactiveTile()
	{
		if (getRandom().nextInt(4096) == 0) {
			// radioactive decay
			getCity().setTile(getXpos(), getYpos(), DIRT);
		}
	}

	/**
	 * Called when the current tile is a road tile.
	 */
	private void doRoad()
	{
		getCity().setRoadTotal(getCity().getRoadTotal() + 1);

		if (getCity().getRoadEffect() < 30) {
			// deteriorating roads
			if (getRandom().nextInt(512) == 0) {
				if (!isConductive(getTile())) {
					if (getCity().getRoadEffect() < getRandom().nextInt(32)) {
						if (isOverWater(getTile()))
							getCity().setTile(getXpos(), getYpos(), RIVER);
						else
							getCity().setTile(getXpos(), getYpos(), (char) (RUBBLE + getRandom().nextInt(4)));
						return;
					}
				}
			}
		}

		if (!isCombustible(getTile())) //bridge
		{
			getCity().setRoadTotal(getCity().getRoadTotal() + 4);
			if (doBridge())
				return;
		}

		int tden;
		if (getTile() < LTRFBASE)
			tden = 0;
		else if (getTile() < HTRFBASE)
			tden = 1;
		else {
			getCity().setRoadTotal(getCity().getRoadTotal() + 1);
			tden = 2;
		}

		int trafficDensity = getCity().getTrafficDensity(getXpos(), getYpos());
		int newLevel = trafficDensity < 64 ? 0 :
				trafficDensity < 192 ? 1 : 2;

		if (tden != newLevel) {
			int z = (getTile() - ROADBASE & 15) + TRAFFIC_DENSITY_TAB[newLevel];
			getCity().setTile(getXpos(), getYpos(), (char) z);
		}
	}

	/**
	 * Called when the current tile is railroad.
	 */
	private void doRail()
	{
		getCity().setRailTotal(getCity().getRailTotal() + 1);
		getCity().generateTrain(getXpos(), getYpos());

		if (getCity().getRoadEffect() < 30) { // deteriorating rail
			if (getRandom().nextInt(512) == 0) {
				if (!isConductive(getTile())) {
					if (getCity().getRoadEffect() < getRandom().nextInt(32)) {
						if (isOverWater(getTile())) {
							getCity().setTile(getXpos(), getYpos(), RIVER);
						} else {
							getCity().setTile(getXpos(), getYpos(), (char) (RUBBLE + getRandom().nextInt(4)));
						}
					}
				}
			}
		}
	}

	/**
	 * Called when the current tile is a road bridge over water.
	 * Handles the draw bridge. For the draw bridge to appear,
	 * there must be a boat on the water, the boat must be
	 * within a certain distance of the bridge, it must be where
	 * the map generator placed 'channel' tiles (these are tiles
	 * that look just like regular river tiles but have a different
	 * numeric value), and you must be a little lucky.
	 *
	 * @return true if the draw bridge is open; false otherwise
	 */
	private boolean doBridge()
	{
		int[] hdx = {-2, 2, -2, -1, 0, 1, 2};
		int[] hdy = {-1, -1, 0, 0, 0, 0, 0};
		char[] hbrtab = {
				HBRDG1, HBRDG3,
				HBRDG0, RIVER,
				BRWH, RIVER,
				HBRDG2};
		char[] hbrtab2 = {
				RIVER, RIVER,
				HBRIDGE, HBRIDGE,
				HBRIDGE, HBRIDGE,
				HBRIDGE};

		int[] vdx = {0, 1, 0, 0, 0, 0, 1};
		int[] vdy = {-2, -2, -1, 0, 1, 2, 2};
		char[] vbrtab = {
				VBRDG0, VBRDG1,
				RIVER, BRWV,
				RIVER, VBRDG2,
				VBRDG3};
		char[] vbrtab2 = {
				VBRIDGE, RIVER,
				VBRIDGE, VBRIDGE,
				VBRIDGE, VBRIDGE,
				RIVER};

		if (getTile() == BRWV) {
			// vertical bridge, open
			if (getRandom().nextInt(4) == 0 && getBoatDis() > 340 / 16) {
				//close the bridge
				applyBridgeChange(vdx, vdy, vbrtab, vbrtab2);
			}
			return true;
		} else if (getTile() == BRWH) {
			// horizontal bridge, open
			if (getRandom().nextInt(4) == 0 && getBoatDis() > 340 / 16) {
				// close the bridge
				applyBridgeChange(hdx, hdy, hbrtab, hbrtab2);
			}
			return true;
		}

		if (getBoatDis() < 300 / 16 && getRandom().nextInt(8) == 0) {
			if ((getTile() & 1) == 0) {
				// horizontal bridge
				if (getYpos() > 0) {
					// look for CHANNEL tile just above
					// bridge. the CHANNEL tiles are only
					// found in the very center of the
					// river
					if (getCity().getTile(getXpos(), getYpos() - 1) == CHANNEL) {
						// open it up
						applyBridgeChange(hdx, hdy, hbrtab2, hbrtab);
						return true;
					}
				}
			} else {
				// vertical bridge
				if (getXpos() < getCity().getWidth() - 1) {
					// look for CHANNEL tile to right of
					// bridge. the CHANNEL tiles are only
					// found in the very center of the
					// river
					if (getCity().getTile(getXpos() + 1, getYpos()) == CHANNEL) {
						// vertical bridge, open it up
						applyBridgeChange(vdx, vdy, vbrtab2, vbrtab);
						return true;
					}
				}
			}
			return false;
		}

		return false;
	}

	/**
	 * Helper function for doBridge- it toggles the draw-bridge.
	 */
	private void applyBridgeChange(int[] dx, int[] dy, char[] fromTab, char[] toTab)
	{
		//FIXME- a closed bridge with traffic on it is not
		// correctly handled by this subroutine, because the
		// the tiles representing traffic on a bridge do not match
		// the expected tile values of fromTab

		for (int z = 0; z < 7; z++) {
			int x = getXpos() + dx[z];
			int y = getYpos() + dy[z];
			if (getCity().testBounds(x, y)) {
				if (getCity().getTile(x, y) == fromTab[z] ||
						getCity().getTile(x, y) == CHANNEL
				) {
					getCity().setTile(x, y, toTab[z]);
				}
			}
		}
	}

	/**
	 * Calculate how far away the boat currently is from the
	 * current tile.
	 */
	private int getBoatDis()
	{
		int dist = 99999;
		for (Sprite sprite : getCity().getSprites()) {
			if (sprite.isVisible() && sprite.getKind() == SpriteKind.SHI) {
				int x = sprite.getX() / 16;
				int y = sprite.getY() / 16;
				int d = Math.abs(getXpos() - x) + Math.abs(getYpos() - y);
				dist = Math.min(d, dist);
			}
		}
		return dist;
	}

	private void doExplosion()
	{
		// clear AniRubble
		getCity().setTile(getXpos(), getYpos(), (char) (RUBBLE + getRandom().nextInt(4)));
	}

	enum B
	{
		FIRE,
		FLOOD,
		RADIOACTIVE,
		ROAD,
		RAIL,
		EXPLOSION
	}
}
