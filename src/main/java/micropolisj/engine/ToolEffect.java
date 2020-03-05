// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import static micropolisj.engine.TileConstants.CLEAR;

class ToolEffect implements ToolEffectIfc
{
	private final ToolPreview preview;
	private final Micropolis city;
	private final int originX;
	private final int originY;

	ToolEffect(Micropolis city)
	{
		this(city, 0, 0);
	}

	ToolEffect(Micropolis city, int xpos, int ypos)
	{
		this.city = city;
		preview = new ToolPreview();
		originX = xpos;
		originY = ypos;
	}

	@Override
	public int getTile(int dx, int dy)
	{
		int c = preview.getTile(dx, dy);
		if (c != CLEAR) {
			return c;
		}

		// tiles outside city's boundary assumed to be
		// tile #0 (dirt).
		return city.testBounds(originX + dx, originY + dy) ? city.getTile(originX + dx, originY + dy) : 0;
	}

	@Override
	public void makeSound(int dx, int dy, Sound sound)
	{
		preview.makeSound(dx, dy, sound);
	}

	@Override
	public void setTile(int dx, int dy, int tileValue)
	{
		preview.setTile(dx, dy, tileValue);
	}

	@Override
	public void spend(int amount)
	{
		preview.spend(amount);
	}

	@Override
	public void toolResult(ToolResult tr)
	{
		preview.toolResult(tr);
	}

	ToolResult apply()
	{
		if (originX - preview.getOffsetX() < 0 ||
				originX - preview.getOffsetX() + preview.getWidth() > city.getWidth() ||
				originY - preview.getOffsetY() < 0 ||
				originY - preview.getOffsetY() + preview.getHeight() > city.getHeight()) {
			return ToolResult.UH_OH;
		}

		if (city.getBudget().getTotalFunds() < preview.getCost()) {
			return ToolResult.INSUFFICIENT_FUNDS;
		}

		boolean anyFound = false;
		for (int y = 0; y < preview.getTiles().length; y++) {
			for (int x = 0; x < preview.getTiles()[y].length; x++) {
				int c = preview.getTiles()[y][x];
				if (c != CLEAR) {
					city.setTile(originX + x - preview.getOffsetX(), originY + y - preview.getOffsetY(), (char) c);
					anyFound = true;
				}
			}
		}

		for (SoundInfo si : preview.getSounds()) {
			city.makeSound(si.getX(), si.getY(), si.getSound());
		}

		if (anyFound && preview.getCost() != 0) {
			city.spend(preview.getCost());
			return ToolResult.SUCCESS;
		} else {
			return preview.getToolResult();
		}
	}

	public ToolPreview getPreview()
	{
		return preview;
	}
}
