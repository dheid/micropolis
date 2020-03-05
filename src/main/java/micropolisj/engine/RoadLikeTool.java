// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import static micropolisj.engine.TileConstants.CHANNEL;
import static micropolisj.engine.TileConstants.DIRT;
import static micropolisj.engine.TileConstants.HBRIDGE;
import static micropolisj.engine.TileConstants.HPOWER;
import static micropolisj.engine.TileConstants.HRAIL;
import static micropolisj.engine.TileConstants.HRAILROAD;
import static micropolisj.engine.TileConstants.HROADPOWER;
import static micropolisj.engine.TileConstants.INTERSECTION;
import static micropolisj.engine.TileConstants.LHPOWER;
import static micropolisj.engine.TileConstants.LHRAIL;
import static micropolisj.engine.TileConstants.LVPOWER;
import static micropolisj.engine.TileConstants.LVRAIL;
import static micropolisj.engine.TileConstants.RAILHPOWERV;
import static micropolisj.engine.TileConstants.RAILVPOWERH;
import static micropolisj.engine.TileConstants.REDGE;
import static micropolisj.engine.TileConstants.RIVER;
import static micropolisj.engine.TileConstants.ROADS2;
import static micropolisj.engine.TileConstants.VBRIDGE;
import static micropolisj.engine.TileConstants.VPOWER;
import static micropolisj.engine.TileConstants.VRAIL;
import static micropolisj.engine.TileConstants.VRAILROAD;
import static micropolisj.engine.TileConstants.VROADPOWER;
import static micropolisj.engine.TileConstants.canAutoBulldozeRRW;
import static micropolisj.engine.TileConstants.isConductive;
import static micropolisj.engine.TileConstants.neutralizeRoad;

class RoadLikeTool extends ToolStroke
{
	RoadLikeTool(Micropolis city, MicropolisTool tool, int xpos, int ypos)
	{
		super(city, tool, xpos, ypos);
	}

	@Override
	protected void applyArea(ToolEffectIfc eff)
	{
		while (true) {
			if (!applyForward(eff)) {
				break;
			}
			if (!applyBackward(eff)) {
				break;
			}
		}
	}

	private boolean applyBackward(ToolEffectIfc eff)
	{
		boolean anyChange = false;

		CityRect b = getBounds();
		for (int i = b.getHeight() - 1; i >= 0; i--) {
			for (int j = b.getWidth() - 1; j >= 0; j--) {
				ToolEffectIfc tte = new TranslatedToolEffect(eff, b.getX() + j, b.getY() + i);
				anyChange = anyChange || applySingle(tte);
			}
		}
		return anyChange;
	}

	private boolean applyForward(ToolEffectIfc eff)
	{
		boolean anyChange = false;

		CityRect b = getBounds();
		for (int i = 0; i < b.getHeight(); i++) {
			for (int j = 0; j < b.getWidth(); j++) {
				ToolEffectIfc tte = new TranslatedToolEffect(eff, b.getX() + j, b.getY() + i);
				anyChange = anyChange || applySingle(tte);
			}
		}
		return anyChange;
	}

	@Override
	public CityRect getBounds()
	{
		// constrain bounds to be a rectangle with
		// either width or height equal to one.

		assert getTool().getSize() == 1;
		if (Math.abs(getXdest() - getXpos()) >= Math.abs(getYdest() - getYpos())) {
			// horizontal line
			CityRect r = new CityRect();
			r.setX(Math.min(getXpos(), getXdest()));
			r.setWidth(Math.abs(getXdest() - getXpos()) + 1);
			r.setY(getYpos());
			r.setHeight(1);
			return r;
		} else {
			// vertical line
			CityRect r = new CityRect();
			r.setX(getXpos());
			r.setWidth(1);
			r.setY(Math.min(getYpos(), getYdest()));
			r.setHeight(Math.abs(getYdest() - getYpos()) + 1);
			return r;
		}
	}

	private boolean applySingle(ToolEffectIfc eff)
	{
		switch (getTool()) {
			case RAIL:
				return applyRailTool(eff);

			case ROADS:
				return applyRoadTool(eff);

			case WIRE:
				return applyWireTool(eff);

			default:
				throw new RuntimeException("Unexpected tool: " + getTool());
		}
	}

	private boolean applyRailTool(ToolEffectIfc eff)
	{
		if (layRail(eff)) {
			fixZone(eff);
			return true;
		} else {
			return false;
		}
	}

	private boolean applyRoadTool(ToolEffectIfc eff)
	{
		if (layRoad(eff)) {
			fixZone(eff);
			return true;
		} else {
			return false;
		}
	}

	private boolean applyWireTool(ToolEffectIfc eff)
	{
		if (layWire(eff)) {
			fixZone(eff);
			return true;
		} else {
			return false;
		}
	}

	private boolean layRail(ToolEffectIfc eff)
	{
		final int railCost = 20;
		final int tunnelCost = 100;

		int cost = railCost;

		char tile = (char) eff.getTile(0, 0);
		tile = neutralizeRoad(tile);

		switch (tile) {
			case RIVER:        // rail on water
			case REDGE:
			case CHANNEL:

				cost = tunnelCost;

				// check east
			{
				char eTile = neutralizeRoad(eff.getTile(1, 0));
				if (eTile == RAILHPOWERV ||
						eTile == HRAIL ||
						eTile >= LHRAIL && eTile <= HRAILROAD) {
					eff.setTile(0, 0, HRAIL);
					break;
				}
			}

			// check west
			{
				char wTile = neutralizeRoad(eff.getTile(-1, 0));
				if (wTile == RAILHPOWERV ||
						wTile == HRAIL ||
						wTile > VRAIL && wTile < VRAILROAD) {
					eff.setTile(0, 0, HRAIL);
					break;
				}
			}

			// check south
			{
				char sTile = neutralizeRoad(eff.getTile(0, 1));
				if (sTile == RAILVPOWERH ||
						sTile == VRAILROAD ||
						sTile > HRAIL && sTile < HRAILROAD) {
					eff.setTile(0, 0, VRAIL);
					break;
				}
			}

			// check north
			{
				char nTile = neutralizeRoad(eff.getTile(0, -1));
				if (nTile == RAILVPOWERH ||
						nTile == VRAILROAD ||
						nTile > HRAIL && nTile < HRAILROAD) {
					eff.setTile(0, 0, VRAIL);
					break;
				}
			}

			// cannot do road here
			return false;

			case LHPOWER: // rail on power
				eff.setTile(0, 0, RAILVPOWERH);
				break;

			case LVPOWER: // rail on power
				eff.setTile(0, 0, RAILHPOWERV);
				break;

			case TileConstants.ROADS:    // rail on road (case 1)
				eff.setTile(0, 0, VRAILROAD);
				break;

			case ROADS2:    // rail on road (case 2)
				eff.setTile(0, 0, HRAILROAD);
				break;

			default:
				if (tile != DIRT) {
					if (getCity().isAutoBulldoze() && canAutoBulldozeRRW(tile)) {
						cost += 1; //autodoze cost
					} else {
						// cannot do rail here
						return false;
					}
				}

				//rail on dirt
				eff.setTile(0, 0, LHRAIL);
				break;
		}

		eff.spend(cost);
		return true;
	}

	private boolean layRoad(ToolEffectIfc eff)
	{
		final int roadCost = 10;
		final int bridgeCost = 50;

		int cost = roadCost;

		char tile = (char) eff.getTile(0, 0);
		switch (tile) {
			case RIVER:        // road on water
			case REDGE:
			case CHANNEL:    // check how to build bridges, if possible.

				cost = bridgeCost;

				// check east
			{
				char eTile = neutralizeRoad(eff.getTile(1, 0));
				if (eTile == VRAILROAD ||
						eTile == HBRIDGE ||
						eTile >= TileConstants.ROADS && eTile <= HROADPOWER) {
					eff.setTile(0, 0, HBRIDGE);
					break;
				}
			}

			// check west
			{
				char wTile = neutralizeRoad(eff.getTile(-1, 0));
				if (wTile == VRAILROAD ||
						wTile == HBRIDGE ||
						wTile >= TileConstants.ROADS && wTile <= INTERSECTION) {
					eff.setTile(0, 0, HBRIDGE);
					break;
				}
			}

			// check south
			{
				char sTile = neutralizeRoad(eff.getTile(0, 1));
				if (sTile == HRAILROAD ||
						sTile == VROADPOWER ||
						sTile >= VBRIDGE && sTile <= INTERSECTION) {
					eff.setTile(0, 0, VBRIDGE);
					break;
				}
			}

			// check north
			{
				char nTile = neutralizeRoad(eff.getTile(0, -1));
				if (nTile == HRAILROAD ||
						nTile == VROADPOWER ||
						nTile >= VBRIDGE && nTile <= INTERSECTION) {
					eff.setTile(0, 0, VBRIDGE);
					break;
				}
			}

			// cannot do road here
			return false;

			case LHPOWER: //road on power
				eff.setTile(0, 0, VROADPOWER);
				break;

			case LVPOWER: //road on power #2
				eff.setTile(0, 0, HROADPOWER);
				break;

			case LHRAIL: //road on rail
				eff.setTile(0, 0, HRAILROAD);
				break;

			case LVRAIL: //road on rail #2
				eff.setTile(0, 0, VRAILROAD);
				break;

			default:
				if (tile != DIRT) {
					if (getCity().isAutoBulldoze() && canAutoBulldozeRRW(tile)) {
						cost += 1; //autodoze cost
					} else {
						// cannot do road here
						return false;
					}
				}

				// road on dirt;
				// just build a plain road, fixZone will fix it.
				eff.setTile(0, 0, TileConstants.ROADS);
				break;
		}

		eff.spend(cost);
		return true;
	}

	private boolean layWire(ToolEffectIfc eff)
	{
		final int wireCost = 5;
		final int underwaterWireCost = 25;

		int cost = wireCost;

		char tile = (char) eff.getTile(0, 0);
		tile = neutralizeRoad(tile);

		switch (tile) {
			case RIVER:        // wire on water
			case REDGE:
			case CHANNEL:

				cost = underwaterWireCost;

				// check east
			{
				int tmp = eff.getTile(1, 0);
				char tmpn = neutralizeRoad(tmp);

				if (isConductive(tmp) &&
						tmpn != HROADPOWER &&
						tmpn != RAILHPOWERV &&
						tmpn != HPOWER) {
					eff.setTile(0, 0, VPOWER);
					break;
				}
			}

			// check west
			{
				int tmp = eff.getTile(-1, 0);
				char tmpn = neutralizeRoad(tmp);

				if (isConductive(tmp) &&
						tmpn != HROADPOWER &&
						tmpn != RAILHPOWERV &&
						tmpn != HPOWER) {
					eff.setTile(0, 0, VPOWER);
					break;
				}
			}

			// check south
			{
				int tmp = eff.getTile(0, 1);
				char tmpn = neutralizeRoad(tmp);

				if (isConductive(tmp) &&
						tmpn != VROADPOWER &&
						tmpn != RAILVPOWERH &&
						tmpn != VPOWER) {
					eff.setTile(0, 0, HPOWER);
					break;
				}
			}

			// check north
			{
				int tmp = eff.getTile(0, -1);
				char tmpn = neutralizeRoad(tmp);

				if (isConductive(tmp) &&
						tmpn != VROADPOWER &&
						tmpn != RAILVPOWERH &&
						tmpn != VPOWER) {
					eff.setTile(0, 0, HPOWER);
					break;
				}
			}

			// cannot do wire here
			return false;

			case TileConstants.ROADS: // wire on E/W road
				eff.setTile(0, 0, HROADPOWER);
				break;

			case ROADS2: // wire on N/S road
				eff.setTile(0, 0, VROADPOWER);
				break;

			case LHRAIL:    // wire on E/W railroad tracks
				eff.setTile(0, 0, RAILHPOWERV);
				break;

			case LVRAIL:    // wire on N/S railroad tracks
				eff.setTile(0, 0, RAILVPOWERH);
				break;

			default:
				if (tile != DIRT) {
					if (getCity().isAutoBulldoze() && canAutoBulldozeRRW(tile)) {
						cost += 1; //autodoze cost
					} else {
						//cannot do wire here
						return false;
					}
				}

				//wire on dirt
				eff.setTile(0, 0, LHPOWER);
				break;
		}

		eff.spend(cost);
		return true;
	}
}
