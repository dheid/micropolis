// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static micropolisj.engine.TileConstants.CLEAR;

public class ToolPreview implements ToolEffectIfc
{
	private static final short[][] NO_TILES = new short[0][0];

	private final List<SoundInfo> sounds;

	private int offsetX;

	private int offsetY;

	private short[][] tiles;

	private int cost;

	private ToolResult toolResult;

	ToolPreview()
	{
		tiles = NO_TILES;
		sounds = new ArrayList<>();
		toolResult = ToolResult.NONE;
	}

	@Override
	public int getTile(int dx, int dy)
	{
		return inRange(dx, dy) ? tiles[offsetY + dy][offsetX + dx] : CLEAR;
	}

	public CityRect getBounds()
	{
		return new CityRect(
				-offsetX,
				-offsetY,
				getWidth(),
				getHeight()
		);
	}

	int getWidth()
	{
		return tiles.length != 0 ? tiles[0].length : 0;
	}

	int getHeight()
	{
		return tiles.length;
	}

	private boolean inRange(int dx, int dy)
	{
		return offsetY + dy >= 0 &&
				offsetY + dy < getHeight() &&
				offsetX + dx >= 0 &&
				offsetX + dx < getWidth();
	}

	private void expandTo(int dx, int dy)
	{
		if (tiles == null || tiles.length == 0) {
			tiles = new short[1][1];
			tiles[0][0] = CLEAR;
			offsetX = -dx;
			offsetY = -dy;
			return;
		}

		// expand each existing row as needed
		for (int i = 0; i < tiles.length; i++) {
			short[] a = tiles[i];
			if (offsetX + dx >= a.length) {
				int newLen = offsetX + dx + 1;
				short[] aa = new short[newLen];
				System.arraycopy(a, 0, aa, 0, a.length);
				Arrays.fill(aa, a.length, newLen, CLEAR);
				tiles[i] = aa;
			} else if (offsetX + dx < 0) {
				int addl = -(offsetX + dx);
				int newLen = a.length + addl;
				short[] aa = new short[newLen];
				System.arraycopy(a, 0, aa, addl, a.length);
				Arrays.fill(aa, 0, addl, CLEAR);
				tiles[i] = aa;
			}
		}

		if (offsetX + dx < 0) {
			int addl = -(offsetX + dx);
			offsetX += addl;
		}

		int width = tiles[0].length;
		if (offsetY + dy >= tiles.length) {
			int newLen = offsetY + dy + 1;
			short[][] newTiles = new short[newLen][width];
			System.arraycopy(tiles, 0, newTiles, 0, tiles.length);
			for (int i = tiles.length; i < newLen; i++) {
				Arrays.fill(newTiles[i], CLEAR);
			}
			tiles = newTiles;
		} else if (offsetY + dy < 0) {
			int addl = -(offsetY + dy);
			int newLen = tiles.length + addl;
			short[][] newTiles = new short[newLen][width];
			System.arraycopy(tiles, 0, newTiles, addl, tiles.length);
			for (int i = 0; i < addl; i++) {
				Arrays.fill(newTiles[i], CLEAR);
			}
			tiles = newTiles;

			offsetY += addl;
		}
	}

	@Override
	public void makeSound(int dx, int dy, Sound sound)
	{
		sounds.add(new SoundInfo(dx, dy, sound));
	}

	@Override
	public void setTile(int dx, int dy, int tileValue)
	{
		expandTo(dx, dy);
		tiles[offsetY + dy][offsetX + dx] = (short) tileValue;
	}

	@Override
	public void spend(int amount)
	{
		cost += amount;
	}

	@Override
	public void toolResult(ToolResult tr)
	{
		toolResult = tr;
	}

	public List<SoundInfo> getSounds()
	{
		return sounds;
	}

	public int getOffsetX()
	{
		return offsetX;
	}

	public int getOffsetY()
	{
		return offsetY;
	}

	public short[][] getTiles()
	{
		return tiles;
	}

	public int getCost()
	{
		return cost;
	}

	public ToolResult getToolResult()
	{
		return toolResult;
	}

}
