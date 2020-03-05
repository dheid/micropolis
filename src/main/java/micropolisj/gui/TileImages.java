// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.gui;

import micropolisj.engine.SpriteKind;
import micropolisj.engine.TileSpec;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static micropolisj.engine.TileConstants.LOMASK;
import static micropolisj.engine.TileSpec.generateTileNames;

public class TileImages
{

	private static final int STD_SIZE = 16;

	final int tileWidth;
	final int tileHeight;
	BufferedImage[] images;
	Map<SpriteKind, Map<Integer, Image>> spriteImages;

	private TileImages(int size)
	{
		this.tileWidth = size;
		this.tileHeight = size;

		this.images = loadTileImages();
		loadSpriteImages();
	}

	static Map<Integer, TileImages> savedInstances = new HashMap<>();

	public static TileImages getInstance(int size)
	{
		if (!savedInstances.containsKey(size)) {
			savedInstances.put(size, new TileImages(size));
		}
		return savedInstances.get(size);
	}

	public BufferedImage getTileImage(int cell)
	{
		int tile = (cell & LOMASK) % images.length;
		return images[tile];
	}

	private BufferedImage[] loadTileImages()
	{

		InputStream recipeFile = TileImages.class.getResourceAsStream("/graphics/tiles.rc");

		Properties recipe = new Properties();
		try {
			recipe.load(new InputStreamReader(recipeFile, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException("Could not load tiles recipe file tiles.rc", e);
		}

		String[] tileNames = generateTileNames(recipe);
		int ntiles = tileNames.length;
		BufferedImage[] images = new BufferedImage[ntiles];

		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice dev = env.getDefaultScreenDevice();
		GraphicsConfiguration conf = dev.getDefaultConfiguration();

		for (int tileNumber = 0; tileNumber < ntiles; tileNumber++) {

			BufferedImage bi = conf.createCompatibleImage(tileWidth, tileHeight, Transparency.OPAQUE);
			Graphics2D gr = bi.createGraphics();

			String tileName = tileNames[tileNumber];
			String rawSpec = recipe.getProperty(tileName);
			assert rawSpec != null;

			TileSpec tileSpec = TileSpec.parse(tileNumber, tileName, rawSpec, recipe);
			FrameSpec ref = parseFrameSpec(tileSpec);
			if (ref == null) {
				// tile is defined, but it has no images
				continue;
			}

			SourceImage sourceImg = ref.image;
			gr.drawImage(sourceImg.image, 0, 0, tileWidth, tileHeight,
					ref.offsetX * sourceImg.basisSize / STD_SIZE,
					ref.offsetY * sourceImg.basisSize / STD_SIZE,
					(ref.offsetX + STD_SIZE) * sourceImg.basisSize / STD_SIZE,
					(ref.offsetY + STD_SIZE) * sourceImg.basisSize / STD_SIZE,
					null);

			images[tileNumber] = bi;
		}
		return images;
	}

	private static FrameSpec parseFrameSpec(TileSpec spec)
	{
		FrameSpec result = null;

		for (String layerStr : spec.getImages()) {

			FrameSpec rv = new FrameSpec();
			result = rv;

			String[] parts = layerStr.split("@", 2);
			rv.image = loadImage(parts[0]);

			if (parts.length >= 2) {
				String offsetInfo = parts[1];
				parts = offsetInfo.split(",");
				if (parts.length >= 1) {
					rv.offsetX = Integer.parseInt(parts[0]);
				}
				if (parts.length >= 2) {
					rv.offsetY = Integer.parseInt(parts[1]);
				}
			}//endif something given after '@' in image specifier

		}//end foreach layer in image specification

		return result;
	}

	private static SourceImage loadImage(String fileName)
	{
		URL pngFile = TileImages.class.getResource("/graphics/" + fileName + ".png");
		ImageIcon ii = new ImageIcon(pngFile);
		return new SourceImage(ii.getImage(), STD_SIZE);
	}

	public Image getSpriteImage(SpriteKind kind, int frameNumber)
	{
		return spriteImages.get(kind).get(frameNumber);
	}

	private void loadSpriteImages()
	{
		spriteImages = new EnumMap<>(SpriteKind.class);
		for (SpriteKind kind : SpriteKind.values()) {
			HashMap<Integer, Image> imgs = new HashMap<>();
			for (int i = 0; i < kind.numFrames; i++) {
				Image img = loadSpriteImage(kind, i);
				if (img != null) {
					imgs.put(i, img);
				}
			}
			spriteImages.put(kind, imgs);
		}
	}

	Image loadSpriteImage(SpriteKind kind, int frameNo)
	{
		String resourceName = "/obj" + kind.objectId + "-" + frameNo;

		// first, try to load specific size image
		URL iconUrl = TileImages.class.getResource(resourceName + "_" + tileWidth + "x" + tileHeight + ".png");
		if (iconUrl != null) {
			return new ImageIcon(iconUrl).getImage();
		}

		iconUrl = TileImages.class.getResource(resourceName + ".png");
		if (iconUrl == null)
			return null;

		if (tileWidth == 16 && tileHeight == 16) {
			return new ImageIcon(iconUrl).getImage();
		}

		// scale the image ourselves
		ImageIcon ii = new ImageIcon(iconUrl);
		int destWidth = ii.getIconWidth() * tileWidth / 16;
		int destHeight = ii.getIconHeight() * tileHeight / 16;

		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice dev = env.getDefaultScreenDevice();
		GraphicsConfiguration conf = dev.getDefaultConfiguration();
		BufferedImage bi = conf.createCompatibleImage(destWidth, destHeight, Transparency.TRANSLUCENT);
		Graphics2D gr = bi.createGraphics();

		gr.drawImage(ii.getImage(),
				0, 0, destWidth, destHeight,
				0, 0,
				ii.getIconWidth(), ii.getIconHeight(),
				null);
		return bi;
	}

}
