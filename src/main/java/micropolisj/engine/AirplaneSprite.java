// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

/**
 * Implements the airplane.
 * The airplane appears if the city contains an airport.
 * It first takes off, then flies around randomly,
 * occassionally crashing.
 */
public class AirplaneSprite extends Sprite
{
	// Note: frames 1-8 used for regular movement
	//    9-11 used for Taking off
	private static final int[] CDx = {0, 0, 6, 8, 6, 0, -6, -8, -6, 8, 8, 8};
	private static final int[] CDy = {0, -8, -6, 0, 6, 8, 6, 0, -6, 0, 0, 0};
	private int destX;
	private int destY;

	public AirplaneSprite(Micropolis engine, int xpos, int ypos)
	{
		super(engine, SpriteKind.AIR);
		setX(xpos * 16 + 8);
		setY(ypos * 16 + 8);
		setWidth(48);
		setHeight(48);
		setOffx(-24);
		setOffy(-24);

		destY = getY();
		if (xpos > engine.getWidth() - 20) {
			// not enough room to east of airport for taking off
			destX = getX() - 200;
			setFrame(7);
		} else {
			destX = getX() + 200;
			setFrame(11);
		}
	}

	@Override
	public void moveImpl()
	{
		int z = getFrame();

		if (getCity().getAcycle() % 5 == 0) {
			if (z > 8) { //plane is still taking off
				z--;
				if (z < 9) {
					z = 3;
				}
			} else { // go to destination
				int d = getDir(getX(), getY(), destX, destY);
				z = turnTo(z, d);
			}
			setFrame(z);
		}

		if (getDis(getX(), getY(), destX, destY) < 50) {        // at destination
			//FIXME- original code allows destination to be off-the-map
			destX = getCity().getRandom().nextInt(getCity().getWidth()) * 16 + 8;
			destY = getCity().getRandom().nextInt(getCity().getHeight()) * 16 + 8;
		}

		if (!getCity().isNoDisasters()) {
			boolean explode = false;

			for (Sprite s : getCity().allSprites()) {
				if (s != this &&
						(s.getKind() == SpriteKind.AIR || s.getKind() == SpriteKind.COP) &&
						checkSpriteCollision(s)) {
					s.explodeSprite();
					explode = true;
				}
			}
			if (explode) {
				explodeSprite();
			}
		}

		setX(getX() + CDx[z]);
		setY(getY() + CDy[z]);
	}
}
