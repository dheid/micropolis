// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

/**
 * Implements the helicopter.
 * The helicopter appears if the city contains an airport.
 * It usually flies to the location in the city with the highest
 * traffic density, but sometimes flies to other locations.
 */
public class HelicopterSprite extends Sprite
{
	private static final int[] CDx = {0, 0, 3, 5, 3, 0, -3, -5, -3};

	private static final int[] CDy = {0, -5, -3, 0, 3, 5, 3, 0, -3};

	private static final int SOUND_FREQ = 200;

	private final int origX;

	private final int origY;

	private int destX;

	private int destY;

	private int count;

	public HelicopterSprite(Micropolis engine, int xpos, int ypos)
	{
		super(engine, SpriteKind.COP);
		setX(xpos * 16 + 8);
		setY(ypos * 16 + 8);
		setWidth(32);
		setHeight(32);
		setOffx(-16);
		setOffy(-16);

		destX = getCity().getRandom().nextInt(getCity().getWidth()) * 16 + 8;
		destY = getCity().getRandom().nextInt(getCity().getHeight()) * 16 + 8;

		origX = getX();
		origY = getY();
		count = 1500;
		setFrame(5);
	}

	@Override
	public void moveImpl()
	{
		if (count > 0) {
			count--;
		}

		if (count == 0) {

			// attract copter to monster and tornado so it blows up more often
			if (getCity().hasSprite(SpriteKind.GOD)) {

				MonsterSprite monster = (MonsterSprite) getCity().getSprite(SpriteKind.GOD);
				destX = monster.getX();
				destY = monster.getY();

			} else if (getCity().hasSprite(SpriteKind.TOR)) {

				TornadoSprite tornado = (TornadoSprite) getCity().getSprite(SpriteKind.TOR);
				destX = tornado.getX();
				destY = tornado.getY();

			} else {
				destX = origX;
				destY = origY;
			}

			if (getDis(getX(), getY(), origX, origY) < 30) {
				// made it back to airport, go ahead and land.
				setFrame(0);
				return;
			}
		}

		if (getCity().getAcycle() % SOUND_FREQ == 0) {
			// send report, if hovering over high traffic area
			int xpos = getX() / 16;
			int ypos = getY() / 16;

			if (getCity().getTrafficDensity(xpos, ypos) > 170 &&
					getCity().getRandom().nextInt(8) == 0) {
				getCity().sendMessageAt(MicropolisMessage.HEAVY_TRAFFIC_REPORT,
						xpos, ypos);
				getCity().makeSound(xpos, ypos, Sound.HEAVYTRAFFIC);
			}
		}

		int z = getFrame();
		if (getCity().getAcycle() % 3 == 0) {
			int d = getDir(getX(), getY(), destX, destY);
			z = turnTo(z, d);
			setFrame(z);
		}
		setX(getX() + CDx[z]);
		setY(getY() + CDy[z]);
	}

	public void setDestX(int destX)
	{
		this.destX = destX;
	}

	public void setDestY(int destY)
	{
		this.destY = destY;
	}
}
