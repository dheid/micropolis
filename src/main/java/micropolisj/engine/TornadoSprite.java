// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

/**
 * Implements a tornado (one of the Micropolis disasters).
 */
public class TornadoSprite extends Sprite
{
	private static final int[] CDx = {2, 3, 2, 0, -2, -3};
	private static final int[] CDy = {-2, 0, 2, 3, 2, 0};
	private int count;
	private boolean flag;

	public TornadoSprite(Micropolis engine, int xpos, int ypos)
	{
		super(engine, SpriteKind.TOR);
		setX(xpos * 16 + 8);
		setY(ypos * 16 + 8);
		setWidth(48);
		setHeight(48);
		setOffx(-24);
		setOffy(-40);

		setFrame(1);
		count = 200;
	}

	@Override
	public void moveImpl()
	{
		int z = getFrame();

		if (z == 2) {
			//cycle animation

			z = flag ? 3 : 1;
		} else {
			flag = z == 1;
			z = 2;
		}

		if (count > 0) {
			count--;
		}

		setFrame(z);

		for (Sprite s : getCity().allSprites()) {
			if (checkSpriteCollision(s) &&
					(s.getKind() == SpriteKind.AIR ||
							s.getKind() == SpriteKind.COP ||
							s.getKind() == SpriteKind.SHI ||
							s.getKind() == SpriteKind.TRA)
			) {
				s.explodeSprite();
			}
		}

		int zz = getCity().getRandom().nextInt(CDx.length);
		setX(getX() + CDx[zz]);
		setY(getY() + CDy[zz]);

		if (!getCity().testBounds(getX() / 16, getY() / 16)) {
			// out of bounds
			setFrame(0);
			return;
		}

		if (count == 0 && getCity().getRandom().nextInt(501) == 0) {
			// early termination
			setFrame(0);
			return;
		}

		destroyTile(getX() / 16, getY() / 16);
	}

	public void setCount(int count)
	{
		this.count = count;
	}
}
