// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import static micropolisj.engine.TileConstants.COMCLR;
import static micropolisj.engine.TileConstants.DIRT;
import static micropolisj.engine.TileConstants.FOUNTAIN;
import static micropolisj.engine.TileConstants.INDCLR;
import static micropolisj.engine.TileConstants.LOMASK;
import static micropolisj.engine.TileConstants.RESCLR;
import static micropolisj.engine.TileConstants.RailTable;
import static micropolisj.engine.TileConstants.RoadTable;
import static micropolisj.engine.TileConstants.WOODS2;
import static micropolisj.engine.TileConstants.WireTable;
import static micropolisj.engine.TileConstants.canAutoBulldozeZ;
import static micropolisj.engine.TileConstants.isRailDynamic;
import static micropolisj.engine.TileConstants.isRoadDynamic;
import static micropolisj.engine.TileConstants.isRubble;
import static micropolisj.engine.TileConstants.isWireDynamic;
import static micropolisj.engine.TileConstants.isZoneCenter;
import static micropolisj.engine.TileConstants.railConnectsEast;
import static micropolisj.engine.TileConstants.railConnectsNorth;
import static micropolisj.engine.TileConstants.railConnectsSouth;
import static micropolisj.engine.TileConstants.railConnectsWest;
import static micropolisj.engine.TileConstants.roadConnectsEast;
import static micropolisj.engine.TileConstants.roadConnectsNorth;
import static micropolisj.engine.TileConstants.roadConnectsSouth;
import static micropolisj.engine.TileConstants.roadConnectsWest;
import static micropolisj.engine.TileConstants.wireConnectsEast;
import static micropolisj.engine.TileConstants.wireConnectsNorth;
import static micropolisj.engine.TileConstants.wireConnectsSouth;
import static micropolisj.engine.TileConstants.wireConnectsWest;

public class ToolStroke
{
	private final Micropolis city;
	private final MicropolisTool tool;
	private int xpos;
	private int ypos;
	private int xdest;
	private int ydest;
	private boolean inPreview;

	ToolStroke(Micropolis city, MicropolisTool tool, int xpos, int ypos)
	{
		this.city = city;
		this.tool = tool;
		this.xpos = xpos;
		this.ypos = ypos;
		xdest = xpos;
		ydest = ypos;
	}

	static void fixZone(ToolEffectIfc eff)
	{
		fixSingle(eff);

		// "fix" the cells to the north, west, east, and south
		fixSingle(new TranslatedToolEffect(eff, 0, -1));
		fixSingle(new TranslatedToolEffect(eff, -1, 0));
		fixSingle(new TranslatedToolEffect(eff, 1, 0));
		fixSingle(new TranslatedToolEffect(eff, 0, 1));
	}

	private static void fixSingle(ToolEffectIfc eff)
	{
		int tile = eff.getTile(0, 0);

		if (isRoadDynamic(tile)) {
			// cleanup road
			int adjTile = 0;

			// check road to north
			if (roadConnectsSouth(eff.getTile(0, -1))) {
				adjTile |= 1;
			}

			// check road to east
			if (roadConnectsWest(eff.getTile(1, 0))) {
				adjTile |= 2;
			}

			// check road to south
			if (roadConnectsNorth(eff.getTile(0, 1))) {
				adjTile |= 4;
			}

			// check road to west
			if (roadConnectsEast(eff.getTile(-1, 0))) {
				adjTile |= 8;
			}

			eff.setTile(0, 0, RoadTable[adjTile]);
		} //endif on a road tile

		else if (isRailDynamic(tile)) {
			// cleanup Rail
			int adjTile = 0;

			// check rail to north
			if (railConnectsSouth(eff.getTile(0, -1))) {
				adjTile |= 1;
			}

			// check rail to east
			if (railConnectsWest(eff.getTile(1, 0))) {
				adjTile |= 2;
			}

			// check rail to south
			if (railConnectsNorth(eff.getTile(0, 1))) {
				adjTile |= 4;
			}

			// check rail to west
			if (railConnectsEast(eff.getTile(-1, 0))) {
				adjTile |= 8;
			}

			eff.setTile(0, 0, RailTable[adjTile]);
		} //end if on a rail tile

		else if (isWireDynamic(tile)) {
			// Cleanup Wire
			int adjTile = 0;

			// check wire to north
			if (wireConnectsSouth(eff.getTile(0, -1))) {
				adjTile |= 1;
			}

			// check wire to east
			if (wireConnectsWest(eff.getTile(1, 0))) {
				adjTile |= 2;
			}

			// check wire to south
			if (wireConnectsNorth(eff.getTile(0, 1))) {
				adjTile |= 4;
			}

			// check wire to west
			if (wireConnectsEast(eff.getTile(-1, 0))) {
				adjTile |= 8;
			}

			eff.setTile(0, 0, WireTable[adjTile]);
		} //end if on a rail tile

	}

	public ToolPreview getPreview()
	{
		ToolEffect eff = new ToolEffect(city);
		inPreview = true;
		try {
			applyArea(eff);
		} finally {
			inPreview = false;
		}
		return eff.getPreview();
	}

	public ToolResult apply()
	{
		ToolEffect eff = new ToolEffect(city);
		applyArea(eff);
		return eff.apply();
	}

	void applyArea(ToolEffectIfc eff)
	{
		CityRect r = getBounds();

		for (int i = 0; i < r.getHeight(); i += tool.getSize()) {
			for (int j = 0; j < r.getWidth(); j += tool.getSize()) {
				apply1(new TranslatedToolEffect(eff, r.getX() + j, r.getY() + i));
			}
		}
	}

	void apply1(ToolEffectIfc eff)
	{
		switch (tool) {
			case PARK:
				applyParkTool(eff);
				return;

			case RESIDENTIAL:
				applyZone(eff, RESCLR);
				return;

			case COMMERCIAL:
				applyZone(eff, COMCLR);
				return;

			case INDUSTRIAL:
				applyZone(eff, INDCLR);
				return;

			default:
				// not expected
				throw new RuntimeException("unexpected tool: " + tool);
		}
	}

	public void dragTo(int xdest, int ydest)
	{
		this.xdest = xdest;
		this.ydest = ydest;
	}

	public CityRect getBounds()
	{
		CityRect r = new CityRect();

		r.setX(xpos);
		if (tool.getSize() >= 3) {
			r.setX(r.getX() - 1);
		}
		if (xdest >= xpos) {
			r.setWidth(((xdest - xpos) / tool.getSize() + 1) * tool.getSize());
		} else {
			r.setWidth(((xpos - xdest) / tool.getSize() + 1) * tool.getSize());
			r.setX(r.getX() + tool.getSize() - r.getWidth());
		}

		r.setY(ypos);
		if (tool.getSize() >= 3) {
			r.setY(r.getY() - 1);
		}
		if (ydest >= ypos) {
			r.setHeight(((ydest - ypos) / tool.getSize() + 1) * tool.getSize());
		} else {
			r.setHeight(((ypos - ydest) / tool.getSize() + 1) * tool.getSize());
			r.setY(r.getY() + tool.getSize() - r.getHeight());
		}

		return r;
	}

	public CityLocation getLocation()
	{
		return new CityLocation(xpos, ypos);
	}

	void applyZone(ToolEffectIfc eff, int base)
	{
		assert isZoneCenter(base);

		BuildingInfo bi = Tiles.get(base).getBuildingInfo();
		if (bi == null) {
			throw new RuntimeException("Cannot applyZone to #" + base);
		}

		int cost = tool.getCost();
		boolean canBuild = true;
		for (int rowNum = 0; rowNum < bi.getHeight(); rowNum++) {
			for (int columnNum = 0; columnNum < bi.getWidth(); columnNum++) {
				int tileValue = eff.getTile(columnNum, rowNum);
				tileValue &= LOMASK;

				if (tileValue != DIRT) {
					if (city.isAutoBulldoze() && canAutoBulldozeZ((char) tileValue)) {
						cost++;
					} else {
						canBuild = false;
					}
				}
			}
		}
		if (!canBuild) {
			eff.toolResult(ToolResult.UH_OH);
			return;
		}

		eff.spend(cost);

		int i = 0;
		for (int rowNum = 0; rowNum < bi.getHeight(); rowNum++) {
			for (int columnNum = 0; columnNum < bi.getWidth(); columnNum++) {
				eff.setTile(columnNum, rowNum, (char) bi.getMembers()[i]);
				i++;
			}
		}

		fixBorder(eff, bi.getWidth(), bi.getHeight());
	}

	static void fixBorder(ToolEffectIfc eff, int width, int height)
	{
		for (int x = 0; x < width; x++) {
			fixZone(new TranslatedToolEffect(eff, x, 0));
			fixZone(new TranslatedToolEffect(eff, x, height - 1));
		}
		for (int y = 1; y < height - 1; y++) {
			fixZone(new TranslatedToolEffect(eff, 0, y));
			fixZone(new TranslatedToolEffect(eff, width - 1, y));
		}
	}

	private void applyParkTool(ToolEffectIfc eff)
	{
		int cost = tool.getCost();

		if (eff.getTile(0, 0) != DIRT) {
			// some sort of bulldozing is necessary
			if (!city.isAutoBulldoze()) {
				eff.toolResult(ToolResult.UH_OH);
				return;
			}

			//FIXME- use a canAutoBulldoze-style function here
			if (isRubble(eff.getTile(0, 0))) {
				// this tile can be auto-bulldozed
				cost++;
			} else {
				// cannot be auto-bulldozed
				eff.toolResult(ToolResult.UH_OH);
				return;
			}
		}

		int z = inPreview ? 0 : city.getRandom().nextInt(5);
		int tile;
		tile = z < 4 ? WOODS2 + z : FOUNTAIN;

		eff.spend(cost);
		eff.setTile(0, 0, tile);

	}

	public Micropolis getCity()
	{
		return city;
	}

	public MicropolisTool getTool()
	{
		return tool;
	}

	public int getXpos()
	{
		return xpos;
	}

	public void setXpos(int xpos)
	{
		this.xpos = xpos;
	}

	public int getYpos()
	{
		return ypos;
	}

	public void setYpos(int ypos)
	{
		this.ypos = ypos;
	}

	public int getXdest()
	{
		return xdest;
	}

	public void setXdest(int xdest)
	{
		this.xdest = xdest;
	}

	public int getYdest()
	{
		return ydest;
	}

	public void setYdest(int ydest)
	{
		this.ydest = ydest;
	}

	public boolean isInPreview()
	{
		return inPreview;
	}

}
