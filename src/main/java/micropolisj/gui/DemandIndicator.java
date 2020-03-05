// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.gui;

import micropolisj.engine.CityListener;
import micropolisj.engine.CityLocation;
import micropolisj.engine.Micropolis;
import micropolisj.engine.MicropolisMessage;
import micropolisj.engine.Sound;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.net.URL;

public class DemandIndicator extends JComponent
		implements CityListener
{
	private static final BufferedImage backgroundImage = loadImage();
	private static final Dimension MY_SIZE = new Dimension(
			backgroundImage.getWidth(),
			backgroundImage.getHeight()
	);
	private static final int UPPER_EDGE = 19;
	private static final int LOWER_EDGE = 28;
	private static final int MAX_LENGTH = 16;
	private static final int RES_LEFT = 8;
	private static final int COM_LEFT = 17;
	private static final int IND_LEFT = 26;
	private static final int BAR_WIDTH = 6;
	private Micropolis engine;

	private static BufferedImage loadImage()
	{
		URL iconUrl = MicropolisDrawingArea.class.getResource("/demandg.png");
		Image refImage = new ImageIcon(iconUrl).getImage();

		BufferedImage bi = new BufferedImage(refImage.getWidth(null), refImage.getHeight(null),
				BufferedImage.TYPE_INT_RGB);
		Graphics2D gr = bi.createGraphics();
		gr.drawImage(refImage, 0, 0, null);

		return bi;
	}

	public void setEngine(Micropolis newEngine)
	{
		if (engine != null) { //old engine
			engine.removeListener(this);
		}

		engine = newEngine;

		if (engine != null) { //new engine
			engine.addListener(this);
		}
		repaint();
	}

	@Override
	public Dimension getMinimumSize()
	{
		return MY_SIZE;
	}

	@Override
	public Dimension getPreferredSize()
	{
		return MY_SIZE;
	}

	@Override
	public Dimension getMaximumSize()
	{
		return MY_SIZE;
	}

	@Override
	public void paintComponent(Graphics g)
	{
		Graphics2D gr = (Graphics2D) g;
		gr.drawImage(backgroundImage, 0, 0, null);

		if (engine == null)
			return;

		int resValve = engine.getResValve();
		int ry0 = resValve <= 0 ? LOWER_EDGE : UPPER_EDGE;
		int ry1 = ry0 - resValve / 100;

		if (ry1 - ry0 > MAX_LENGTH) {
			ry1 = ry0 + MAX_LENGTH;
		}
		if (ry1 - ry0 < -MAX_LENGTH) {
			ry1 = ry0 - MAX_LENGTH;
		}

		int comValve = engine.getComValve();
		int cy0 = comValve <= 0 ? LOWER_EDGE : UPPER_EDGE;
		int cy1 = cy0 - comValve / 100;

		int indValve = engine.getIndValve();
		int iy0 = indValve <= 0 ? LOWER_EDGE : UPPER_EDGE;
		int iy1 = iy0 - indValve / 100;

		if (ry0 != ry1) {
			Shape resRect = new Rectangle(RES_LEFT, Math.min(ry0, ry1), BAR_WIDTH, Math.abs(ry1 - ry0));
			gr.setColor(Color.GREEN);
			gr.fill(resRect);
			gr.setColor(Color.BLACK);
			gr.draw(resRect);
		}

		if (cy0 != cy1) {
			Shape comRect = new Rectangle(COM_LEFT, Math.min(cy0, cy1), BAR_WIDTH, Math.abs(cy1 - cy0));
			gr.setColor(Color.BLUE);
			gr.fill(comRect);
			gr.setColor(Color.BLACK);
			gr.draw(comRect);
		}

		if (iy0 != iy1) {
			Shape indRect = new Rectangle(IND_LEFT, Math.min(iy0, iy1), BAR_WIDTH, Math.abs(iy1 - iy0));
			gr.setColor(Color.YELLOW);
			gr.fill(indRect);
			gr.setColor(Color.BLACK);
			gr.draw(indRect);
		}
	}

	@Override
	public void demandChanged()
	{
		repaint();
	}

	@Override
	public void cityMessage(MicropolisMessage message, CityLocation loc)
	{
	}

	@Override
	public void citySound(Sound sound, CityLocation loc)
	{
	}

	@Override
	public void fundsChanged()
	{
	}

	@Override
	public void optionsChanged()
	{
	}
}
