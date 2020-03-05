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
import static micropolisj.engine.TileConstants.POWERBASE;
import static micropolisj.engine.TileConstants.RAILBASE;
import static micropolisj.engine.TileConstants.RIVER;

/**
 * Implements the cargo ship.
 * The cargo ship is created if the city contains a sea port.
 * It follows the river "channel" that was originally generated.
 * It frequently turns around.
 */
public class ShipSprite extends Sprite
{
	private static final int[] BDx = {0, 0, 1, 1, 1, 0, -1, -1, -1};
	private static final int[] BDy = {0, -1, -1, 0, 1, 1, 1, 0, -1};
	private static final int[] BPx = {0, 0, 2, 2, 2, 0, -2, -2, -2};
	private static final int[] BPy = {0, -2, -2, 0, 2, 2, 2, 0, -2};
	private static final int[] BtClrTab = {RIVER, CHANNEL, POWERBASE, POWERBASE + 1,
			RAILBASE, RAILBASE + 1, BRWH, BRWV};
	private int newDir;
	private int count;
	private int soundCount;

	public ShipSprite(Micropolis engine, int xpos, int ypos, int edge)
	{
		super(engine, SpriteKind.SHI);
		setX(xpos * 16 + 8);
		setY(ypos * 16 + 8);
		setWidth(48);
		setHeight(48);
		setOffx(-24);
		setOffy(-24);
		setFrame(edge);
		newDir = edge;
		setDir(10);
		count = 1;
	}

	private static boolean tryOther(int tile, int oldDir, int newDir)
	{
		int z = oldDir + 4;
		if (z > 8) z -= 8;
		if (newDir != z) return false;

		return tile == POWERBASE || tile == POWERBASE + 1 ||
				tile == RAILBASE || tile == RAILBASE + 1;
	}

	@Override
	public void moveImpl()
	{
		int t = RIVER;

		soundCount--;
		if (soundCount <= 0) {
			if (getCity().getRandom().nextInt(4) == 0) {
				getCity().makeSound(getX() / 16, getY() / 16, Sound.HONKHONK_LOW);
			}
			soundCount = 200;
		}

		count--;
		if (count <= 0) {
			count = 9;
			if (newDir != getFrame()) {
				setFrame(turnTo(getFrame(), newDir));
				return;
			}
			int tem = getCity().getRandom().nextInt(8);
			int pem;
			for (pem = tem; pem < tem + 8; pem++) {
				int z = pem % 8 + 1;
				if (z == getDir())
					continue;

				int xpos = getX() / 16 + BDx[z];
				int ypos = getY() / 16 + BDy[z];

				if (getCity().testBounds(xpos, ypos)) {
					t = getCity().getTile(xpos, ypos);
					if (t == CHANNEL || t == BRWH || t == BRWV ||
							tryOther(t, getDir(), z)) {
						newDir = z;
						setFrame(turnTo(getFrame(), newDir));
						setDir(z + 4);
						if (getDir() > 8) {
							setDir(getDir() - 8);
						}
						break;
					}
				}
			}

			if (pem == tem + 8) {
				setDir(10);
				newDir = getCity().getRandom().nextInt(8) + 1;
			}
		} else {
			int z = getFrame();
			if (z == newDir) {
				setX(getX() + BPx[z]);
				setY(getY() + BPy[z]);
			}
		}

		if (!spriteInBounds()) {
			setFrame(0);
			return;
		}

		boolean found = false;
		for (int z : BtClrTab) {
			if (t == z) {
				found = true;
				break;
			}
		}
		if (!found) {
			if (!getCity().isNoDisasters()) {
				explodeSprite();
				destroyTile(getX() / 16, getY() / 16);
			}
		}
	}

	private boolean spriteInBounds()
	{
		int xpos = getX() / 16;
		int ypos = getY() / 16;
		return getCity().testBounds(xpos, ypos);
	}
}
