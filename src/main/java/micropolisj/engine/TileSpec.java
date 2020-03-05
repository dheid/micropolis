// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TileSpec
{
	private static final String[] IMAGES = new String[0];

	private final int tileNumber;

	private final String name;

	private final Map<String, String> attributes;

	private final List<String> images;

	private TileSpec owner;

	private int ownerOffsetX;

	private int ownerOffsetY;

	private TileSpec animNext;

	private TileSpec onPower;

	private TileSpec onShutdown;

	private boolean canBulldoze;

	private boolean canBurn;

	private boolean canConduct;

	private boolean overWater;

	private boolean zone;

	private BuildingInfo buildingInfo;

	private TileSpec(int tileNumber, String tileName)
	{
		this.tileNumber = tileNumber;
		name = tileName;
		attributes = new HashMap<>();
		images = new ArrayList<>();
	}

	public static TileSpec parse(int tileNumber, String tileName, String inStr, Properties tilesRc)
	{
		TileSpec ts = new TileSpec(tileNumber, tileName);
		ts.load(inStr, tilesRc);
		return ts;
	}

	public static String[] generateTileNames(Map recipe)
	{
		int ntiles = recipe.size();
		String[] tileNames = new String[ntiles];
		ntiles = 0;
		for (int i = 0; recipe.containsKey(Integer.toString(i)); i++) {
			tileNames[ntiles++] = Integer.toString(i);
		}
		int naturalNumberTiles = ntiles;

		for (Object key : recipe.keySet()) {
			String n = (String) key;
			if (n.matches("^\\d+$")) {
				int x = Integer.parseInt(n);
				if (x >= 0 && x < naturalNumberTiles) {
					assert tileNames[x].equals(n);
					continue;
				}
			}
			assert ntiles < tileNames.length;
			tileNames[ntiles++] = n;
		}
		assert ntiles == tileNames.length;
		return tileNames;
	}

	public String getAttribute(String key)
	{
		return attributes.get(key);
	}

	public boolean getBooleanAttribute(String key)
	{
		String v = getAttribute(key);
		return "true".equals(v);
	}

	public BuildingInfo getBuildingInfo()
	{
		return buildingInfo;
	}

	private void resolveBuildingInfo()
	{
		String tmp = getAttribute("building");
		if (tmp == null) {
			return;
		}

		BuildingInfo bi = new BuildingInfo();

		String[] p2 = tmp.split("x");
		bi.setWidth(Integer.parseInt(p2[0]));
		bi.setHeight(Integer.parseInt(p2[1]));

		bi.setMembers(new short[bi.getWidth() * bi.getHeight()]);
		int startTile = tileNumber;
		if (bi.getWidth() >= 3) {
			startTile--;
		}
		if (bi.getHeight() >= 3) {
			startTile -= bi.getWidth();
		}

		for (int row = 0; row < bi.getHeight(); row++) {
			for (int col = 0; col < bi.getWidth(); col++) {
				bi.getMembers()[row * bi.getWidth() + col] = (short) startTile;
				startTile++;
			}
		}

		buildingInfo = bi;
	}

	public CityDimension getBuildingSize()
	{
		return buildingInfo != null ? new CityDimension(
				buildingInfo.getWidth(),
				buildingInfo.getHeight()
		) : null;
	}

	public int getDescriptionNumber()
	{
		String v = getAttribute("description");
		if (v != null && !v.isEmpty() && v.charAt(0) == '#') {
			return Integer.parseInt(v.substring(1));
		}
		if (owner != null) {
			return owner.getDescriptionNumber();
		}
		return -1;
	}

	public String[] getImages()
	{
		return images.toArray(IMAGES);
	}

	public int getPollutionValue()
	{
		String v = getAttribute("pollution");
		if (v != null) {
			return Integer.parseInt(v);
		} else if (owner != null) {
			// pollution inherits from building tile
			return owner.getPollutionValue();
		} else {
			return 0;
		}
	}

	public int getPopulation()
	{
		String v = getAttribute("population");
		return v != null ? Integer.parseInt(v) : 0;
	}

	private void load(String inStr, Properties tilesRc)
	{
		Scanner in = new Scanner(inStr);

		while (in.hasMore()) {

			if (in.peekChar() == '(') {
				in.eatChar('(');
				String k = in.readAttributeKey();
				String v = "true";
				if (in.peekChar() == '=') {
					in.eatChar('=');
					v = in.readAttributeValue();
				}
				in.eatChar(')');

				if (attributes.containsKey(k)) {
					attributes.put(k, v);
				} else {
					attributes.put(k, v);
					String sup = tilesRc.getProperty(k);
					if (sup != null) {
						load(sup, tilesRc);
					}
				}
			} else if (in.peekChar() == '|' || in.peekChar() == ',') {
				in.eatChar(in.peekChar());
			} else {
				String v = in.readImageSpec();
				images.add(v);
			}
		}

		canBulldoze = getBooleanAttribute("bulldozable");
		canBurn = !getBooleanAttribute("noburn");
		canConduct = getBooleanAttribute("conducts");
		overWater = getBooleanAttribute("overwater");
		zone = getBooleanAttribute("zone");
	}

	public String toString()
	{
		return "{tile:" + name + "}";
	}

	void resolveReferences()
	{
		String tmp = getAttribute("becomes");
		if (tmp != null) {
			animNext = Tiles.tilesByName.get(tmp);
		}
		tmp = getAttribute("onpower");
		if (tmp != null) {
			onPower = Tiles.tilesByName.get(tmp);
		}
		tmp = getAttribute("onshutdown");
		if (tmp != null) {
			onShutdown = Tiles.tilesByName.get(tmp);
		}
		tmp = getAttribute("building-part");
		if (tmp != null) {
			handleBuildingPart(tmp);
		}

		resolveBuildingInfo();
	}

	private void handleBuildingPart(String text)
	{
		String[] parts = text.split(",");
		if (parts.length != 3) {
			throw new RuntimeException("Invalid building-part specification");
		}

		owner = Tiles.tilesByName.get(parts[0]);
		ownerOffsetX = Integer.parseInt(parts[1]);
		ownerOffsetY = Integer.parseInt(parts[2]);

		assert owner != null;
		assert ownerOffsetX != 0 || ownerOffsetY != 0;
	}

	public int getTileNumber()
	{
		return tileNumber;
	}

	public TileSpec getOwner()
	{
		return owner;
	}

	public void setOwner(TileSpec owner)
	{
		this.owner = owner;
	}

	public int getOwnerOffsetX()
	{
		return ownerOffsetX;
	}

	public void setOwnerOffsetX(int ownerOffsetX)
	{
		this.ownerOffsetX = ownerOffsetX;
	}

	public int getOwnerOffsetY()
	{
		return ownerOffsetY;
	}

	public void setOwnerOffsetY(int ownerOffsetY)
	{
		this.ownerOffsetY = ownerOffsetY;
	}

	public TileSpec getAnimNext()
	{
		return animNext;
	}

	public TileSpec getOnPower()
	{
		return onPower;
	}

	public TileSpec getOnShutdown()
	{
		return onShutdown;
	}

	public boolean isCanBulldoze()
	{
		return canBulldoze;
	}

	public boolean isCanBurn()
	{
		return canBurn;
	}

	public boolean isCanConduct()
	{
		return canConduct;
	}

	public boolean isOverWater()
	{
		return overWater;
	}

	public boolean isZone()
	{
		return zone;
	}

	private static class Scanner
	{
		private final String str;

		private int off;

		private Scanner(String str)
		{
			this.str = str;
		}

		private void skipWhitespace()
		{
			while (off < str.length() && Character.isWhitespace(str.charAt(off))) {
				off++;
			}
		}

		private int peekChar()
		{
			skipWhitespace();
			return off < str.length() ? str.charAt(off) : -1;
		}

		private void eatChar(int ch)
		{
			skipWhitespace();
			assert str.charAt(off) == ch;
			off++;
		}

		private String readAttributeKey()
		{
			skipWhitespace();

			int start = off;
			while (off < str.length() && (str.charAt(off) == '-' || Character.isLetterOrDigit(str.charAt(off)))) {
				off++;
			}

			return off != start ? str.substring(start, off) : null;
		}

		private String readAttributeValue()
		{
			return readString();
		}

		private String readImageSpec()
		{
			return readString();
		}

		private String readString()
		{
			skipWhitespace();

			int endQuote = 0; //any whitespace or certain punctuation
			if (peekChar() == '"') {
				off++;
				endQuote = '"';
			}

			int start = off;
			while (off < str.length()) {
				int c = str.charAt(off);
				if (c == endQuote) {
					int end = off;
					off++;
					return str.substring(start, end);
				} else if (endQuote == 0 && (Character.isWhitespace(c) || c == ')' || c == '|')) {
					int end = off;
					return str.substring(start, end);
				}
				off++;
			}
			return str.substring(start);
		}

		private boolean hasMore()
		{
			return peekChar() != -1;
		}
	}
}
