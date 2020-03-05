// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

class TranslatedToolEffect implements ToolEffectIfc
{
	private final ToolEffectIfc base;
	private final int dx;
	private final int dy;

	TranslatedToolEffect(ToolEffectIfc base, int dx, int dy)
	{
		this.base = base;
		this.dx = dx;
		this.dy = dy;
	}

	@Override
	public int getTile(int dx, int dy)
	{
		return base.getTile(dx + this.dx, dy + this.dy);
	}

	@Override
	public void makeSound(int dx, int dy, Sound sound)
	{
		base.makeSound(dx + this.dx, dy + this.dy, sound);
	}

	@Override
	public void setTile(int dx, int dy, int tileValue)
	{
		base.setTile(dx + this.dx, dy + this.dy, tileValue);
	}

	@Override
	public void spend(int amount)
	{
		base.spend(amount);
	}

	@Override
	public void toolResult(ToolResult tr)
	{
		base.toolResult(tr);
	}
}
