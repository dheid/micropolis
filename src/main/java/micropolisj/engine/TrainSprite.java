// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import static micropolisj.engine.TileConstants.LASTRAIL;
import static micropolisj.engine.TileConstants.RAILBASE;
import static micropolisj.engine.TileConstants.RAILHPOWERV;
import static micropolisj.engine.TileConstants.RAILVPOWERH;

/**
 * Implements the commuter train.
 * The commuter train appears if the city has a certain amount of
 * railroad track. It wanders around the city's available track
 * randomly.
 */
public class TrainSprite extends Sprite
{
	private static final int[] Cx = {0, 16, 0, -16};
	private static final int[] Cy = {-16, 0, 16, 0};
	private static final int[] Dx = {0, 4, 0, -4, 0};
	private static final int[] Dy = {-4, 0, 4, 0, 0};
	private static final int[] TrainPic2 = {1, 2, 1, 2, 5};
	private static final int TRA_GROOVE_X = 8;
	private static final int TRA_GROOVE_Y = 8;

	private static final int FRAME_NW_SE = 3;
	private static final int FRAME_SW_NE = 4;
	private static final int FRAME_UNDERWATER = 5;

	private static final int DIR_NONE = 4; //not moving

	public TrainSprite(Micropolis engine, int xpos, int ypos)
	{
		super(engine, SpriteKind.TRA);
		setX(xpos * 16 + TRA_GROOVE_X);
		setY(ypos * 16 + TRA_GROOVE_Y);
		setOffx(-16);
		setOffy(-16);
		setDir(DIR_NONE);   //not moving
	}

	@Override
	public void moveImpl()
	{
		if (getFrame() == 3 || getFrame() == 4) {
			setFrame(TrainPic2[getDir()]);
		}
		setX(getX() + Dx[getDir()]);
		setY(getY() + Dy[getDir()]);
		if (getCity().getAcycle() % 4 == 0) {
			// should be at the center of a cell, if not, correct it
			setX(getX() / 16 * 16 + TRA_GROOVE_X);
			setY(getY() / 16 * 16 + TRA_GROOVE_Y);
			int d1 = getCity().getRandom().nextInt(4);
			for (int z = d1; z < d1 + 4; z++) {
				int d2 = z % 4;
				if (getDir() != DIR_NONE) { //impossible?
					if (d2 == (getDir() + 2) % 4)
						continue;
				}

				int c = getChar(getX() + Cx[d2], getY() + Cy[d2]);
				if (c >= RAILBASE && c <= LASTRAIL || //track?
						c == RAILVPOWERH ||
						c == RAILHPOWERV) {
					if (getDir() != d2 && getDir() != DIR_NONE) {
						setFrame(getDir() + d2 == 3 ? FRAME_NW_SE : FRAME_SW_NE);
					} else {
						setFrame(TrainPic2[d2]);
					}

					if (c == RAILBASE || c == RAILBASE + 1) {
						//underwater
						setFrame(FRAME_UNDERWATER);
					}
					setDir(d2);
					return;
				}
			}
			if (getDir() == DIR_NONE) {
				// train has nowhere to go, so retire
				setFrame(0);
				return;
			}
			setDir(DIR_NONE);
		}
	}
}
