// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

/**
 * Implements a monster (one of the Micropolis disasters).
 */
public class MonsterSprite extends Sprite
{
	// movement deltas
	private static final int[] Gx = {2, 2, -2, -2, 0};
	private static final int[] Gy = {-2, 2, 2, -2, 0};
	private static final int[] ND1 = {0, 1, 2, 3};
	private static final int[] ND2 = {1, 2, 3, 0};
	private static final int[] nn1 = {2, 5, 8, 11};
	private static final int[] nn2 = {11, 2, 5, 8};
	private final int origX;
	private final int origY;

	//GODZILLA FRAMES
	//   1...3 : northeast
	//   4...6 : southeast
	//   7...9 : southwest
	//  10..12 : northwest
	//      13 : north
	//      14 : east
	//      15 : south
	//      16 : west
	private int count;
	private int soundCount;
	private int destX;
	private int destY;
	private boolean flag; //true if the monster wants to return home
	private int step;

	public MonsterSprite(Micropolis engine, int xpos, int ypos)
	{
		super(engine, SpriteKind.GOD);
		setX(xpos * 16 + 8);
		setY(ypos * 16 + 8);
		setWidth(48);
		setHeight(48);
		setOffx(-24);
		setOffy(-24);

		origX = getX();
		origY = getY();

		setFrame(xpos > getCity().getWidth() / 2 ?
				ypos > getCity().getHeight() / 2 ? 10 : 7 :
				ypos > getCity().getHeight() / 2 ? 1 : 4);

		count = 1000;
		CityLocation p = getCity().getLocationOfMaxPollution();
		destX = p.getX() * 16 + 8;
		destY = p.getY() * 16 + 8;
		flag = false;
		step = 1;
	}

	@Override
	public void moveImpl()
	{
		if (getFrame() == 0) {
			return;
		}

		if (soundCount > 0) {
			soundCount--;
		}

		int d = (getFrame() - 1) / 3;   // basic direction
		int z = (getFrame() - 1) % 3;   // step index (only valid for d<4)

		if (d < 4) { //turn n s e w
			assert step == -1 || step == 1;
			if (z == 2) step = -1;
			if (z == 0) step = 1;
			z += step;

			if (getDis(getX(), getY(), destX, destY) < 60) {

				// reached destination

				if (flag) {
					// destination was origX, origY;
					// hide the sprite
					setFrame(0);
					return;
				} else {
					// destination was the pollution center;
					// now head for home
					flag = true;
					destX = origX;
					destY = origY;
				}
			}

			int c = getDir(getX(), getY(), destX, destY);
			c = (c - 1) / 2;   //convert to one of four basic headings
			assert c >= 0 && c < 4;

			if (c != d && getCity().getRandom().nextInt(11) == 0) {
				// randomly determine direction to turn
				z = getCity().getRandom().nextInt(2) == 0 ? ND1[d] : ND2[d];
				d = 4;  //transition heading

				if (soundCount == 0) {
					getCity().makeSound(getX() / 16, getY() / 16, Sound.MONSTER);
					soundCount = 50 + getCity().getRandom().nextInt(101);
				}
			}
		} else {
			assert getFrame() >= 13 && getFrame() <= 16;

			int z2 = (getFrame() - 13) % 4;

			if (getCity().getRandom().nextInt(4) == 0) {
				int newFrame;
				newFrame = getCity().getRandom().nextInt(2) == 0 ? nn1[z2] : nn2[z2];
				d = (newFrame - 1) / 3;
				z = (newFrame - 1) % 3;

				assert d < 4;
			} else {
				d = 4;
			}
		}

		setFrame(d * 3 + z + 1);

		assert getFrame() >= 1 && getFrame() <= 16;

		setX(getX() + Gx[d]);
		setY(getY() + Gy[d]);

		if (count > 0) {
			count--;
		}

		int c = getChar(getX(), getY());
		if (c == -1) {
			setFrame(0); //kill zilla
		}

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

		destroyTile(getX() / 16, getY() / 16);
	}

	public void setCount(int count)
	{
		this.count = count;
	}

	public void setSoundCount(int soundCount)
	{
		this.soundCount = soundCount;
	}

	public void setDestX(int destX)
	{
		this.destX = destX;
	}

	public void setDestY(int destY)
	{
		this.destY = destY;
	}

	public void setFlag(boolean flag)
	{
		this.flag = flag;
	}
}
