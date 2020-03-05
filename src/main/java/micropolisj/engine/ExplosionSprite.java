// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import static micropolisj.engine.TileConstants.DIRT;
import static micropolisj.engine.TileConstants.FIRE;
import static micropolisj.engine.TileConstants.isCombustible;
import static micropolisj.engine.TileConstants.isZoneCenter;

/**
 * Implements an explosion.
 * An explosion occurs when certain sprites collide,
 * or when a zone is demolished by fire.
 */
public class ExplosionSprite extends Sprite
{
	public ExplosionSprite(Micropolis engine, int x, int y)
	{
		super(engine, SpriteKind.EXP);
		this.setX(x);
		this.setY(y);
		setWidth(48);
		setHeight(48);
		setOffx(-24);
		setOffy(-24);
		setFrame(1);
	}

	@Override
	public void moveImpl()
	{
		if (getCity().getAcycle() % 2 == 0) {
			if (getFrame() == 1) {
				getCity().makeSound(getX() / 16, getY() / 16, Sound.EXPLOSION_HIGH);
				getCity().sendMessageAt(MicropolisMessage.EXPLOSION_REPORT, getX() / 16, getY() / 16);
			}
			setFrame(getFrame() + 1);
		}

		if (getFrame() > 6) {
			setFrame(0);

			startFire(getX() / 16, getY() / 16);
			startFire(getX() / 16 - 1, getY() / 16 - 1);
			startFire(getX() / 16 + 1, getY() / 16 - 1);
			startFire(getX() / 16 - 1, getY() / 16 + 1);
			startFire(getX() / 16 + 1, getY() / 16 + 1);
		}
	}

	private void startFire(int xpos, int ypos)
	{
		if (!getCity().testBounds(xpos, ypos))
			return;

		int t = getCity().getTile(xpos, ypos);
		if (!isCombustible(t) && t != DIRT)
			return;
		if (isZoneCenter(t))
			return;
		getCity().setTile(xpos, ypos, (char) (FIRE + getCity().getRandom().nextInt(4)));
	}
}
