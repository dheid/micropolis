// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import static micropolisj.engine.TileConstants.AIRPORT;
import static micropolisj.engine.TileConstants.FIRESTATION;
import static micropolisj.engine.TileConstants.NUCLEAR;
import static micropolisj.engine.TileConstants.POLICESTATION;
import static micropolisj.engine.TileConstants.PORT;
import static micropolisj.engine.TileConstants.POWERPLANT;
import static micropolisj.engine.TileConstants.STADIUM;

public class BuildingTool extends ToolStroke
{
	public BuildingTool(Micropolis engine, MicropolisTool tool, int xpos, int ypos)
	{
		super(engine, tool, xpos, ypos);
	}

	@Override
	public void dragTo(int xdest, int ydest)
	{
		setXpos(xdest);
		setYpos(ydest);
		this.setXdest(xdest);
		this.setYdest(ydest);
	}

	@Override
	void apply1(ToolEffectIfc eff)
	{
		switch (getTool()) {
			case FIRE:
				applyZone(eff, FIRESTATION);
				return;

			case POLICE:
				applyZone(eff, POLICESTATION);
				return;

			case POWERPLANT:
				applyZone(eff, POWERPLANT);
				return;

			case STADIUM:
				applyZone(eff, STADIUM);
				return;

			case SEAPORT:
				applyZone(eff, PORT);
				return;

			case NUCLEAR:
				applyZone(eff, NUCLEAR);
				return;

			case AIRPORT:
				applyZone(eff, AIRPORT);
				return;

			default:
				// not expected
				throw new RuntimeException("unexpected tool: " + getTool());
		}
	}
}
