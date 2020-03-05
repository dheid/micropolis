// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

import static micropolisj.engine.TileConstants.ALLBITS;
import static micropolisj.engine.TileConstants.CHANNEL;
import static micropolisj.engine.TileConstants.COMBASE;
import static micropolisj.engine.TileConstants.DIRT;
import static micropolisj.engine.TileConstants.FIRE;
import static micropolisj.engine.TileConstants.FLOOD;
import static micropolisj.engine.TileConstants.HHTHR;
import static micropolisj.engine.TileConstants.INDBASE;
import static micropolisj.engine.TileConstants.LASTZONE;
import static micropolisj.engine.TileConstants.LHTHR;
import static micropolisj.engine.TileConstants.LOMASK;
import static micropolisj.engine.TileConstants.NUCLEAR;
import static micropolisj.engine.TileConstants.PORTBASE;
import static micropolisj.engine.TileConstants.POWERPLANT;
import static micropolisj.engine.TileConstants.PWRBIT;
import static micropolisj.engine.TileConstants.RADTILE;
import static micropolisj.engine.TileConstants.RESCLR;
import static micropolisj.engine.TileConstants.RIVER;
import static micropolisj.engine.TileConstants.RUBBLE;
import static micropolisj.engine.TileConstants.commercialZonePop;
import static micropolisj.engine.TileConstants.getDescriptionNumber;
import static micropolisj.engine.TileConstants.getPollutionValue;
import static micropolisj.engine.TileConstants.getTileBehavior;
import static micropolisj.engine.TileConstants.getZoneSizeFor;
import static micropolisj.engine.TileConstants.industrialZonePop;
import static micropolisj.engine.TileConstants.isAnimated;
import static micropolisj.engine.TileConstants.isArsonable;
import static micropolisj.engine.TileConstants.isCombustible;
import static micropolisj.engine.TileConstants.isConductive;
import static micropolisj.engine.TileConstants.isConstructed;
import static micropolisj.engine.TileConstants.isFloodable;
import static micropolisj.engine.TileConstants.isRiverEdge;
import static micropolisj.engine.TileConstants.isVulnerable;
import static micropolisj.engine.TileConstants.isZoneCenter;
import static micropolisj.engine.TileConstants.residentialZonePop;

/**
 * The main simulation engine for Micropolis.
 * The front-end should call animate() periodically
 * to move the simulation forward in time.
 */
public class Micropolis
{
	private static final int NORTH_EDGE = 5;
	private static final int EAST_EDGE = 7;
	private static final int SOUTH_EDGE = 1;

	public static final int CENSUSRATE = 4;
	static final Random DEFAULT_PRNG = new Random();
	private static final int DEFAULT_WIDTH = 120;

	// half-size arrays
	private static final int DEFAULT_HEIGHT = 100;
	private static final int TAXFREQ = 48;
	private static final int[] TaxTable = {
			200, 150, 120, 100, 80, 50, 30, 0, -10, -40, -100,
			-150, -200, -250, -300, -350, -400, -450, -500, -550, -600};
	/**
	 * Road/rail maintenance cost multiplier, for various difficulty settings.
	 */
	private static final double[] RLevels = {0.7, 0.9, 1.2};
	/**
	 * Tax income multiplier, for various difficulty settings.
	 */
	private static final double[] FLevels = {1.4, 1.2, 0.8};

	// quarter-size arrays
	/**
	 * Annual maintenance cost of each police station.
	 */
	private static final int POLICE_STATION_MAINTENANCE = 100;

	// eighth-size arrays
	/**
	 * Annual maintenance cost of each fire station.
	 */
	private static final int FIRE_STATION_MAINTENANCE = 100;
	private static final Sprite[] SPRITES = new Sprite[0];
	private final CityBudget budget = new CityBudget();
	private final CityEval evaluation;
	private final History history = new History();
	private final List<FinancialHistory> financialHistory = new ArrayList<>();
	private final Random random;
	private final List<Sprite> sprites = new ArrayList<>();
	private final Stack<CityLocation> powerPlants = new Stack<>();
	private final Collection<CityListener> cityListeners = new ArrayList<>();
	private final Collection<MapListener> mapListeners = new ArrayList<>();
	private final Collection<EarthquakeListener> earthquakeListeners = new ArrayList<>();
	private int[][] pollutionMem;
	private int[][] crimeMem;
	private int[][] popDensity;
	private int[][] rateOGMem; //rate of growth?
	private int[][] fireRate;       //firestations reach- used for overlay graphs
	private int[][] policeMapEffect;//police stations reach- used for overlay graphs
	private boolean autoBulldoze = true;
	private boolean autoBudget;
	private Speed simSpeed = Speed.NORMAL;
	private boolean noDisasters;
	private int gameLevel;
	private int centerMassX;
	private int centerMassY;
	//
	// budget stuff
	//
	private int cityTax = 7;
	private double roadPercent = 1.0;
	private double policePercent = 1.0;
	private double firePercent = 1.0;
	private int cityTime;  //counts "weeks" (actually, 1/48'ths years)
	// full size arrays
	private char[][] map;
	private int[][] fireStMap;      //firestations- cleared and rebuilt each sim cycle
	private int[][] policeMap;      //police stations- cleared and rebuilt each sim cycle
	private int[][] comRate;
	// census numbers, reset in phase 0 of each cycle, summed during map scan
	private int poweredZoneCount;
	private int unpoweredZoneCount;
	private int roadTotal;
	private int railTotal;
	private int firePop;
	private int resZoneCount;
	private int comZoneCount;
	private int indZoneCount;
	private int resPop;
	private int comPop;
	private int indPop;
	private int hospitalCount;
	private int churchCount;
	private int policeCount;
	private int fireStationCount;
	private int stadiumCount;
	private int coalCount;
	private int nuclearCount;
	private int seaportCount;
	private int airportCount;
	private int totalPop;
	private int needHospital; // -1 too many already, 0 just right, 1 not enough
	private int needChurch;   // -1 too many already, 0 just right, 1 not enough
	private int crimeAverage;
	private int pollutionAverage;
	private int landValueAverage;
	private int trafficAverage;
	private int resValve;   // ranges between -2000 and 2000, updated by setValves
	private int comValve;   // ranges between -1500 and 1500
	private int indValve;   // ranges between -1500 and 1500
	private boolean resCap;  // residents demand a stadium, caps resValve at 0
	private boolean comCap;  // commerce demands airport,   caps comValve at 0
	private boolean indCap;  // industry demands sea port,  caps indValve at 0
	private int roadEffect = 32;
	private int policeEffect = 1000;
	private int fireEffect = 1000;
	private int floodCnt; //number of turns the flood will last
	private int acycle; //animation cycle (mod 960)
	private boolean[][] powerMap;
	/**
	 * For each 2x2 section of the city, the land value of the city (0-250).
	 * 0 is lowest land value; 250 is maximum land value.
	 * Updated each cycle by ptlScan().
	 */
	private int[][] landValueMem;
	/**
	 * For each 2x2 section of the city, the traffic density (0-255).
	 * If less than 64, no cars are animated.
	 * If between 64 and 192, then the "light traffic" animation is used.
	 * If 192 or higher, then the "heavy traffic" animation is used.
	 */
	private int[][] trfDensity;
	/**
	 * For each 4x4 section of the city, an integer representing the natural
	 * land features in the vicinity of this part of the city.
	 */
	private int[][] terrainMem;
	private boolean autoGo;
	private int cityPopulation;
	// used in generateBudget()
	private int lastRoadTotal;
	private int lastRailTotal;
	private int lastTotalPop;
	private int lastFireStationCount;
	private int lastPoliceCount;
	private int pollutionMaxLocationX;
	private int pollutionMaxLocationY;
	private int crimeRamp;
	private int polluteRamp;
	private int taxEffect = 7;
	private int cashFlow; //net change in totalFunds in previous year
	private int scycle; //same as cityTime, except mod 1024
	private int fcycle; //counts simulation steps (mod 1024)
	private Map<String, TileBehavior> tileBehaviors;

	public Micropolis()
	{
		random = DEFAULT_PRNG;
		evaluation = new CityEval(this);
		init(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		initTileBehaviors();
	}

	static boolean isTileDozeable(ToolEffectIfc eff)
	{
		int myTile = eff.getTile(0, 0);
		TileSpec ts = Tiles.get(myTile);
		if (ts.isCanBulldoze()) {
			return true;
		}

		if (ts.getOwner() != null) {
			// part of a zone; only bulldozeable if the owner tile is
			// no longer intact.

			int baseTile = eff.getTile(-ts.getOwnerOffsetX(), -ts.getOwnerOffsetY());
			return ts.getOwner().getTileNumber() != baseTile;
		}

		return false;
	}

	private static int[][] doSmooth(int[][] tem)
	{
		int h = tem.length;
		int w = tem[0].length;
		int[][] tem2 = new int[h][w];

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int z = tem[y][x];
				if (x > 0)
					z += tem[y][x - 1];
				if (x + 1 < w)
					z += tem[y][x + 1];
				if (y > 0)
					z += tem[y - 1][x];
				if (y + 1 < h)
					z += tem[y + 1][x];
				z /= 4;
				if (z > 255)
					z = 255;
				tem2[y][x] = z;
			}
		}

		return tem2;
	}

	private static int[][] smoothFirePoliceMap(int[][] omap)
	{
		int smX = omap[0].length;
		int smY = omap.length;
		int[][] nmap = new int[smY][smX];
		for (int sy = 0; sy < smY; sy++) {
			for (int sx = 0; sx < smX; sx++) {
				int edge = 0;
				if (sx > 0) {
					edge += omap[sy][sx - 1];
				}
				if (sx + 1 < smX) {
					edge += omap[sy][sx + 1];
				}
				if (sy > 0) {
					edge += omap[sy - 1][sx];
				}
				if (sy + 1 < smY) {
					edge += omap[sy + 1][sx];
				}
				edge = edge / 4 + omap[sy][sx];
				nmap[sy][sx] = edge / 2;
			}
		}
		return nmap;
	}

	private static int[][] smoothTerrain(int[][] qtem)
	{
		int qwx = qtem[0].length;
		int qwy = qtem.length;

		int[][] mem = new int[qwy][qwx];
		for (int y = 0; y < qwy; y++) {
			for (int x = 0; x < qwx; x++) {
				int z = 0;
				if (x > 0)
					z += qtem[y][x - 1];
				if (x + 1 < qwx)
					z += qtem[y][x + 1];
				if (y > 0)
					z += qtem[y - 1][x];
				if (y + 1 < qwy)
					z += qtem[y + 1][x];
				mem[y][x] = z / 4 + qtem[y][x] / 2;
			}
		}
		return mem;
	}

	private static void loadHistoryArray(int[] array, DataInput dis)
			throws IOException
	{
		for (int i = 0; i < 240; i++) {
			array[i] = dis.readShort();
		}
	}

	private static void writeHistoryArray(int[] array, DataOutput out)
			throws IOException
	{
		for (int i = 0; i < 240; i++) {
			out.writeShort(array[i]);
		}
	}

	public void spend(int amount)
	{
		budget.setTotalFunds(budget.getTotalFunds() - amount);
		fireFundsChanged();
	}

	private void init(int width, int height)
	{
		map = new char[height][width];
		powerMap = new boolean[height][width];

		int hX = (width + 1) / 2;
		int hY = (height + 1) / 2;

		landValueMem = new int[hY][hX];
		pollutionMem = new int[hY][hX];
		crimeMem = new int[hY][hX];
		popDensity = new int[hY][hX];
		trfDensity = new int[hY][hX];

		int qX = (width + 3) / 4;
		int qY = (height + 3) / 4;

		terrainMem = new int[qY][qX];

		int smX = (width + 7) / 8;
		int smY = (height + 7) / 8;

		rateOGMem = new int[smY][smX];
		fireStMap = new int[smY][smX];
		policeMap = new int[smY][smX];
		policeMapEffect = new int[smY][smX];
		fireRate = new int[smY][smX];
		comRate = new int[smY][smX];

		centerMassX = hX;
		centerMassY = hY;
	}

	private void fireCensusChanged()
	{
		for (CityListener l : cityListeners) {
			l.censusChanged();
		}
	}

	private void fireCityMessage(MicropolisMessage message, CityLocation loc)
	{
		for (CityListener l : cityListeners) {
			l.cityMessage(message, loc);
		}
	}

	private void fireCitySound(Sound sound, CityLocation loc)
	{
		for (CityListener l : cityListeners) {
			l.citySound(sound, loc);
		}
	}

	private void fireDemandChanged()
	{
		for (CityListener l : cityListeners) {
			l.demandChanged();
		}
	}

	private void fireEarthquakeStarted()
	{
		for (EarthquakeListener l : earthquakeListeners) {
			l.earthquakeStarted();
		}
	}

	void fireEvaluationChanged()
	{
		for (CityListener l : cityListeners) {
			l.evaluationChanged();
		}
	}

	private void fireFundsChanged()
	{
		for (CityListener l : cityListeners) {
			l.fundsChanged();
		}
	}

	private void fireMapOverlayDataChanged()
	{
		for (MapListener l : mapListeners) {
			l.mapOverlayDataChanged();
		}
	}

	private void fireOptionsChanged()
	{
		for (CityListener l : cityListeners) {
			l.optionsChanged();
		}
	}

	void fireSpriteMoved(Sprite sprite)
	{
		for (MapListener l : mapListeners) {
			l.spriteMoved(sprite);
		}
	}

	private void fireTileChanged(int xpos, int ypos)
	{
		for (MapListener l : mapListeners) {
			l.tileChanged(xpos, ypos);
		}
	}

	void fireWholeMapChanged()
	{
		for (MapListener l : mapListeners) {
			l.wholeMapChanged();
		}
	}

	public void addListener(CityListener l)
	{
		cityListeners.add(l);
	}

	public void removeListener(CityListener l)
	{
		cityListeners.remove(l);
	}

	public void addEarthquakeListener(EarthquakeListener l)
	{
		earthquakeListeners.add(l);
	}

	public void removeEarthquakeListener(EarthquakeListener l)
	{
		earthquakeListeners.remove(l);
	}

	public void addMapListener(MapListener l)
	{
		mapListeners.add(l);
	}

	public void removeMapListener(MapListener l)
	{
		mapListeners.remove(l);
	}

	public int getWidth()
	{
		return map[0].length;
	}

	public int getHeight()
	{
		return map.length;
	}

	public char getTile(int xpos, int ypos)
	{
		return (char) (map[ypos][xpos] & LOMASK);
	}

	private char getTileRaw(int xpos, int ypos)
	{
		return map[ypos][xpos];
	}

	private boolean isTileDozeable(int xpos, int ypos)
	{
		return isTileDozeable(
				new ToolEffect(this, xpos, ypos)
		);
	}

	public boolean isTilePowered(int xpos, int ypos)
	{
		return (getTileRaw(xpos, ypos) & PWRBIT) == PWRBIT;
	}

	/**
	 * Note: this method clears the PWRBIT of the given location.
	 */
	public void setTile(int xpos, int ypos, char newTile)
	{
		// check to make sure we aren't setting an upper bit using
		// this method
		assert (newTile & LOMASK) == newTile;

		if (map[ypos][xpos] != newTile) {
			map[ypos][xpos] = newTile;
			fireTileChanged(xpos, ypos);
		}
	}

	public void setTilePower(int xpos, int ypos, boolean power)
	{
		map[ypos][xpos] = (char) (map[ypos][xpos] & ~PWRBIT | (power ? PWRBIT : 0));
	}

	public boolean testBounds(int xpos, int ypos)
	{
		return xpos >= 0 && xpos < getWidth() &&
				ypos >= 0 && ypos < getHeight();
	}

	boolean hasPower(int x, int y)
	{
		return powerMap[y][x];
	}

	/**
	 * Checks whether the next call to animate() will collect taxes and
	 * process the budget.
	 */
	public boolean isBudgetTime()
	{
		return cityTime != 0 &&
				cityTime % TAXFREQ == 0 &&
				(fcycle + 1) % 16 == 10 &&
				(acycle + 1) % 2 == 0;
	}

	private void step()
	{
		fcycle = (fcycle + 1) % 1024;
		simulate(fcycle % 16);
	}

	private void clearCensus()
	{
		poweredZoneCount = 0;
		unpoweredZoneCount = 0;
		firePop = 0;
		roadTotal = 0;
		railTotal = 0;
		resPop = 0;
		comPop = 0;
		indPop = 0;
		resZoneCount = 0;
		comZoneCount = 0;
		indZoneCount = 0;
		hospitalCount = 0;
		churchCount = 0;
		policeCount = 0;
		fireStationCount = 0;
		stadiumCount = 0;
		coalCount = 0;
		nuclearCount = 0;
		seaportCount = 0;
		airportCount = 0;
		powerPlants.clear();

		for (int y = 0; y < fireStMap.length; y++) {
			for (int x = 0; x < fireStMap[y].length; x++) {
				fireStMap[y][x] = 0;
				policeMap[y][x] = 0;
			}
		}
	}

	private void simulate(int mod16)
	{
		int band = getWidth() / 8;

		switch (mod16) {
			case 0:
				scycle = (scycle + 1) % 1024;
				cityTime++;
				if (scycle % 2 == 0) {
					setValves();
				}
				clearCensus();
				break;

			case 1:
				mapScan(0, band);
				break;

			case 2:
				mapScan(band, 2 * band);
				break;

			case 3:
				mapScan(2 * band, 3 * band);
				break;

			case 4:
				mapScan(3 * band, 4 * band);
				break;

			case 5:
				mapScan(4 * band, 5 * band);
				break;

			case 6:
				mapScan(5 * band, 6 * band);
				break;

			case 7:
				mapScan(6 * band, 7 * band);
				break;

			case 8:
				mapScan(7 * band, getWidth());
				break;

			case 9:
				if (cityTime % CENSUSRATE == 0) {
					takeCensus();

					if (cityTime % (CENSUSRATE * 12) == 0) {
						takeCensus2();
					}

					fireCensusChanged();
				}

				collectTaxPartial();

				if (cityTime % TAXFREQ == 0) {
					collectTax();
					evaluation.cityEvaluation();
				}
				break;

			case 10:
				if (scycle % 5 == 0) {  // every ~10 weeks
					decROGMem();
				}
				decTrafficMem();
				fireMapOverlayDataChanged(); //TDMAP
				fireMapOverlayDataChanged();       //RDMAP
				fireMapOverlayDataChanged();             //ALMAP
				fireMapOverlayDataChanged();     //REMAP
				fireMapOverlayDataChanged();      //COMAP
				fireMapOverlayDataChanged();      //INMAP
				doMessages();
				break;

			case 11:
				powerScan();
				fireMapOverlayDataChanged();
				break;

			case 12:
				ptlScan();
				break;

			case 13:
				crimeScan();
				break;

			case 14:
				popDenScan();
				break;

			case 15:
				fireAnalysis();
				doDisasters();
				break;

			default:
				throw new RuntimeException("unreachable");
		}
	}

	private int computePopDen(int x, int y, char tile)
	{
		if (tile == RESCLR)
			return doFreePop(x, y);

		if (tile < COMBASE)
			return residentialZonePop(tile);

		if (tile < INDBASE)
			return commercialZonePop(tile) * 8;

		if (tile < PORTBASE)
			return industrialZonePop(tile) * 8;

		return 0;
	}

	public void calculateCenterMass()
	{
		popDenScan();
	}

	private void popDenScan()
	{
		int xtot = 0;
		int ytot = 0;
		int zoneCount = 0;
		int width = getWidth();
		int height = getHeight();
		int[][] tem = new int[(height + 1) / 2][(width + 1) / 2];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				char tile = getTile(x, y);
				if (isZoneCenter(tile)) {
					int den = computePopDen(x, y, tile) * 8;
					if (den > 254)
						den = 254;
					tem[y / 2][x / 2] = den;
					xtot += x;
					ytot += y;
					zoneCount++;
				}
			}
		}

		tem = doSmooth(tem);
		tem = doSmooth(tem);
		tem = doSmooth(tem);

		for (int x = 0; x < (width + 1) / 2; x++) {
			for (int y = 0; y < (height + 1) / 2; y++) {
				popDensity[y][x] = 2 * tem[y][x];
			}
		}

		distIntMarket(); //set ComRate

		// find center of mass for city
		if (zoneCount == 0) {
			centerMassX = (width + 1) / 2;
			centerMassY = (height + 1) / 2;
		} else {
			centerMassX = xtot / zoneCount;
			centerMassY = ytot / zoneCount;
		}

		fireMapOverlayDataChanged();     //PDMAP
		fireMapOverlayDataChanged(); //RGMAP
	}

	private void distIntMarket()
	{
		for (int y = 0; y < comRate.length; y++) {
			for (int x = 0; x < comRate[y].length; x++) {
				int z = getDisCC(x * 4, y * 4);
				z /= 4;
				z = 64 - z;
				comRate[y][x] = z;
			}
		}
	}

	//tends to empty RateOGMem[][]
	private void decROGMem()
	{
		for (int y = 0; y < rateOGMem.length; y++) {
			for (int x = 0; x < rateOGMem[y].length; x++) {
				int z = rateOGMem[y][x];
				if (z == 0)
					continue;

				if (z > 0) {
					rateOGMem[y][x]--;
					if (z > 200) {
						rateOGMem[y][x] = 200; //prevent overflow?
					}
					continue;
				}

				rateOGMem[y][x]++;
				if (z < -200) {
					rateOGMem[y][x] = -200;
				}
			}
		}
	}

	//tends to empty trfDensity
	private void decTrafficMem()
	{
		for (int y = 0; y < trfDensity.length; y++) {
			for (int x = 0; x < trfDensity[y].length; x++) {
				int z = trfDensity[y][x];
				if (z != 0) {
					if (z > 200)
						trfDensity[y][x] = z - 34;
					else if (z > 24)
						trfDensity[y][x] = z - 24;
					else
						trfDensity[y][x] = 0;
				}
			}
		}
	}

	private void crimeScan()
	{
		policeMap = smoothFirePoliceMap(policeMap);
		policeMap = smoothFirePoliceMap(policeMap);
		policeMap = smoothFirePoliceMap(policeMap);

		for (int sy = 0; sy < policeMap.length; sy++) {
			System.arraycopy(policeMap[sy], 0, policeMapEffect[sy], 0, policeMap[sy].length);
		}

		int count = 0;
		int sum = 0;
		int cmax = 0;
		for (int hy = 0; hy < landValueMem.length; hy++) {
			for (int hx = 0; hx < landValueMem[hy].length; hx++) {
				int val = landValueMem[hy][hx];
				if (val == 0) {
					crimeMem[hy][hx] = 0;
				} else {
					count++;
					int z = 128 - val + popDensity[hy][hx];
					z = Math.min(300, z);
					z -= policeMap[hy / 4][hx / 4];
					z = Math.min(250, z);
					z = Math.max(0, z);
					crimeMem[hy][hx] = z;

					sum += z;
					if (z > cmax || z == cmax && random.nextInt(4) == 0) {
						cmax = z;
					}
				}
			}
		}

		crimeAverage = count != 0 ? sum / count : 0;

		fireMapOverlayDataChanged();
	}

	private void doDisasters()
	{
		if (floodCnt > 0) {
			floodCnt--;
		}

		int[] disChance = {480, 240, 60};
		if (noDisasters)
			return;

		if (random.nextInt(disChance[gameLevel] + 1) != 0)
			return;

		switch (random.nextInt(9)) {
			case 0:
			case 1:
				setFire();
				break;
			case 2:
			case 3:
				makeFlood();
				break;
			case 4:
				break;
			case 5:
				makeTornado();
				break;
			case 6:
				makeEarthquake();
				break;
			case 7:
			case 8:
				if (pollutionAverage > 60) {
					makeMonster();
				}
				break;
		}
	}

	private void fireAnalysis()
	{
		fireStMap = smoothFirePoliceMap(fireStMap);
		fireStMap = smoothFirePoliceMap(fireStMap);
		fireStMap = smoothFirePoliceMap(fireStMap);
		for (int sy = 0; sy < fireStMap.length; sy++) {
			System.arraycopy(fireStMap[sy], 0, fireRate[sy], 0, fireStMap[sy].length);
		}

		fireMapOverlayDataChanged();
	}

	private boolean testForCond(CityLocation loc, int dir)
	{
		int xsave = loc.getX();
		int ysave = loc.getY();

		boolean rv = false;
		if (movePowerLocation(loc, dir)) {
			char t = getTile(loc.getX(), loc.getY());
			rv = isConductive(t) &&
					t != NUCLEAR &&
					t != POWERPLANT &&
					!hasPower(loc.getX(), loc.getY());
		}

		loc.setX(xsave);
		loc.setY(ysave);
		return rv;
	}

	private boolean movePowerLocation(CityLocation loc, int dir)
	{
		switch (dir) {
			case 0:
				if (loc.getY() > 0) {
					loc.setY(loc.getY() - 1);
					return true;
				} else
					return false;
			case 1:
				if (loc.getX() + 1 < getWidth()) {
					loc.setX(loc.getX() + 1);
					return true;
				} else
					return false;
			case 2:
				if (loc.getY() + 1 < getHeight()) {
					loc.setY(loc.getY() + 1);
					return true;
				} else
					return false;
			case 3:
				if (loc.getX() > 0) {
					loc.setX(loc.getX() - 1);
					return true;
				} else
					return false;
			case 4:
				return true;
		}
		return false;
	}

	private void powerScan()
	{
		// clear powerMap
		for (boolean[] bb : powerMap) {
			Arrays.fill(bb, false);
		}

		//
		// Note: brownouts are based on total number of power plants, not the number
		// of powerplants connected to your city.
		//

		int maxPower = coalCount * 700 + nuclearCount * 2000;
		int numPower = 0;

		// This is kind of odd algorithm, but I haven't the heart to rewrite it at
		// this time.

		while (!powerPlants.isEmpty()) {
			CityLocation loc = powerPlants.pop();

			int aDir = 4;
			int conNum;
			do {
				if (++numPower > maxPower) {
					// trigger notification
					sendMessage(MicropolisMessage.BROWNOUTS_REPORT);
					return;
				}
				movePowerLocation(loc, aDir);
				powerMap[loc.getY()][loc.getX()] = true;

				conNum = 0;
				int dir = 0;
				while (dir < 4 && conNum < 2) {
					if (testForCond(loc, dir)) {
						conNum++;
						aDir = dir;
					}
					dir++;
				}
				if (conNum > 1) {
					powerPlants.add(new CityLocation(loc.getX(), loc.getY()));
				}
			}
			while (conNum != 0);
		}
	}

	/**
	 * Increase the traffic-density measurement at a particular
	 * spot.
	 */
	void addTraffic(int mapX, int mapY)
	{
		int z = trfDensity[mapY / 2][mapX / 2];
		z += 50;

		//FIXME- why is this only capped to 240
		// by random chance. why is there no cap
		// the rest of the time?

		if (z > 240 && random.nextInt(6) == 0) {
			z = 240;

			HelicopterSprite copter = (HelicopterSprite) getSprite(SpriteKind.COP);
			if (copter != null) {
				copter.setDestX(mapX);
				copter.setDestY(mapY);
			}
		}

		trfDensity[mapY / 2][mapX / 2] = z;
	}

	/**
	 * Accessor method for fireRate[].
	 */
	public int getFireStationCoverage(int xpos, int ypos)
	{
		return fireRate[ypos / 8][xpos / 8];
	}

	/**
	 * Accessor method for landValueMem overlay.
	 */
	public int getLandValue(int xpos, int ypos)
	{
		return testBounds(xpos, ypos) ? landValueMem[ypos / 2][xpos / 2] : 0;
	}

	public int getTrafficDensity(int xpos, int ypos)
	{
		return testBounds(xpos, ypos) ? trfDensity[ypos / 2][xpos / 2] : 0;
	}

	//power, terrain, land value
	private void ptlScan()
	{
		int qX = (getWidth() + 3) / 4;
		int qY = (getHeight() + 3) / 4;
		int[][] qtem = new int[qY][qX];

		int landValueTotal = 0;
		int landValueCount = 0;

		int hwldx = (getWidth() + 1) / 2;
		int hwldy = (getHeight() + 1) / 2;
		int[][] tem = new int[hwldy][hwldx];
		for (int x = 0; x < hwldx; x++) {
			for (int y = 0; y < hwldy; y++) {
				int plevel = 0;
				int lvflag = 0;
				int zx = 2 * x;
				int zy = 2 * y;

				for (int mx = zx; mx <= zx + 1; mx++) {
					for (int my = zy; my <= zy + 1; my++) {
						int tile = getTile(mx, my);
						if (tile != DIRT) {
							if (tile < RUBBLE) //natural land features
							{
								//inc terrainMem
								qtem[y / 2][x / 2] += 15;
								continue;
							}
							plevel += getPollutionValue(tile);
							if (isConstructed(tile))
								lvflag++;
						}
					}
				}

				if (plevel < 0)
					plevel = 250; //?

				if (plevel > 255)
					plevel = 255;

				tem[y][x] = plevel;

				if (lvflag == 0) {
					landValueMem[y][x] = 0;
				} else {
					//land value equation


					int dis = 34 - getDisCC(x, y);
					dis *= 4;
					dis += terrainMem[y / 2][x / 2];
					dis -= pollutionMem[y][x];
					if (crimeMem[y][x] > 190) {
						dis -= 20;
					}
					if (dis > 250)
						dis = 250;
					if (dis < 1)
						dis = 1;
					landValueMem[y][x] = dis;
					landValueTotal += dis;
					landValueCount++;
				}
			}
		}

		landValueAverage = landValueCount != 0 ? landValueTotal / landValueCount : 0;

		tem = doSmooth(tem);
		tem = doSmooth(tem);

		int pcount = 0;
		int ptotal = 0;
		int pmax = 0;
		for (int x = 0; x < hwldx; x++) {
			for (int y = 0; y < hwldy; y++) {
				int z = tem[y][x];
				pollutionMem[y][x] = z;

				if (z != 0) {
					pcount++;
					ptotal += z;

					if (z > pmax ||
							z == pmax && random.nextInt(4) == 0) {
						pmax = z;
						pollutionMaxLocationX = 2 * x;
						pollutionMaxLocationY = 2 * y;
					}
				}
			}
		}

		pollutionAverage = pcount != 0 ? ptotal / pcount : 0;

		terrainMem = smoothTerrain(qtem);

		fireMapOverlayDataChanged();   //PLMAP
		fireMapOverlayDataChanged(); //LVMAP
	}

	public CityLocation getLocationOfMaxPollution()
	{
		return new CityLocation(pollutionMaxLocationX, pollutionMaxLocationY);
	}

	private void setValves()
	{
		double normResPop = resPop / 8.0;
		totalPop = (int) (normResPop + comPop + indPop);

		double employment;
		employment = normResPop != 0.0 ? (history.getCom()[1] + history.getInd()[1]) / normResPop : 1;

		double migration = normResPop * (employment - 1);
		final double birthRate = 0.02;
		double births = normResPop * birthRate;
		double projectedResPop = normResPop + migration + births;

		double temp = history.getCom()[1] + history.getInd()[1];
		double laborBase;
		laborBase = temp != 0.0 ? history.getRes()[1] / temp : 1;

		// clamp laborBase to between 0.0 and 1.3
		laborBase = Math.max(0.0, Math.min(1.3, laborBase));

		double internalMarket = (normResPop + comPop + indPop) / 3.7;
		double projectedComPop = internalMarket * laborBase;

		int z = gameLevel;
		temp = 1.0;
		switch (z) {
			case 0:
				temp = 1.2;
				break;
			case 1:
				temp = 1.1;
				break;
			case 2:
				temp = 0.98;
				break;
		}

		double projectedIndPop = indPop * laborBase * temp;
		if (projectedIndPop < 5.0)
			projectedIndPop = 5.0;

		double resRatio;
		resRatio = normResPop != 0 ? projectedResPop / normResPop : 1.3;

		double comRatio;
		comRatio = comPop != 0 ? projectedComPop / comPop : projectedComPop;

		double indRatio;
		indRatio = indPop != 0 ? projectedIndPop / indPop : projectedIndPop;

		if (resRatio > 2.0)
			resRatio = 2.0;

		if (comRatio > 2.0)
			comRatio = 2.0;

		if (indRatio > 2.0)
			indRatio = 2.0;

		int z2 = taxEffect + gameLevel;
		if (z2 > 20)
			z2 = 20;

		resRatio = (resRatio - 1) * 600 + TaxTable[z2];
		comRatio = (comRatio - 1) * 600 + TaxTable[z2];
		indRatio = (indRatio - 1) * 600 + TaxTable[z2];

		// ratios are velocity changes to valves
		resValve += (int) resRatio;
		comValve += (int) comRatio;
		indValve += (int) indRatio;

		if (resValve > 2000)
			resValve = 2000;
		else if (resValve < -2000)
			resValve = -2000;

		if (comValve > 1500)
			comValve = 1500;
		else if (comValve < -1500)
			comValve = -1500;

		if (indValve > 1500)
			indValve = 1500;
		else if (indValve < -1500)
			indValve = -1500;


		if (resCap && resValve > 0) {
			// residents demand stadium
			resValve = 0;
		}

		if (comCap && comValve > 0) {
			// commerce demands airport
			comValve = 0;
		}

		if (indCap && indValve > 0) {
			// industry demands sea port
			indValve = 0;
		}

		fireDemandChanged();
	}

	// calculate manhatten distance (in 2-units) from center of city
	// capped at 32
	private int getDisCC(int x, int y)
	{
		assert x >= 0 && x <= getWidth() / 2;
		assert y >= 0 && y <= getHeight() / 2;

		int xdis = Math.abs(x - centerMassX / 2);
		int ydis = Math.abs(y - centerMassY / 2);

		int z = xdis + ydis;
		return Math.min(z, 32);
	}

	private void initTileBehaviors()
	{
		Map<String, TileBehavior> bb;
		bb = new HashMap<>();

		bb.put("FIRE", new TerrainBehavior(this, TerrainBehavior.B.FIRE));
		bb.put("FLOOD", new TerrainBehavior(this, TerrainBehavior.B.FLOOD));
		bb.put("RADIOACTIVE", new TerrainBehavior(this, TerrainBehavior.B.RADIOACTIVE));
		bb.put("ROAD", new TerrainBehavior(this, TerrainBehavior.B.ROAD));
		bb.put("RAIL", new TerrainBehavior(this, TerrainBehavior.B.RAIL));
		bb.put("EXPLOSION", new TerrainBehavior(this, TerrainBehavior.B.EXPLOSION));
		bb.put("RESIDENTIAL", new MapScanner(this, Behavior.RESIDENTIAL));
		bb.put("HOSPITAL_CHURCH", new MapScanner(this, Behavior.HOSPITAL_CHURCH));
		bb.put("COMMERCIAL", new MapScanner(this, Behavior.COMMERCIAL));
		bb.put("INDUSTRIAL", new MapScanner(this, Behavior.INDUSTRIAL));
		bb.put("COAL", new MapScanner(this, Behavior.COAL));
		bb.put("NUCLEAR", new MapScanner(this, Behavior.NUCLEAR));
		bb.put("FIRESTATION", new MapScanner(this, Behavior.FIRESTATION));
		bb.put("POLICESTATION", new MapScanner(this, Behavior.POLICESTATION));
		bb.put("STADIUM_EMPTY", new MapScanner(this, Behavior.STADIUM_EMPTY));
		bb.put("STADIUM_FULL", new MapScanner(this, Behavior.STADIUM_FULL));
		bb.put("AIRPORT", new MapScanner(this, Behavior.AIRPORT));
		bb.put("SEAPORT", new MapScanner(this, Behavior.SEAPORT));

		tileBehaviors = bb;
	}

	private void mapScan(int x0, int x1)
	{
		for (int x = x0; x < x1; x++) {
			for (int y = 0; y < getHeight(); y++) {
				mapScanTile(x, y);
			}
		}
	}

	private void mapScanTile(int xpos, int ypos)
	{
		int tile = getTile(xpos, ypos);
		String behaviorStr = getTileBehavior(tile);
		if (behaviorStr == null) {
			return; //nothing to do
		}

		TileBehavior b = tileBehaviors.get(behaviorStr);
		if (b != null) {
			b.processTile(xpos, ypos);
		} else {
			throw new RuntimeException("Unknown behavior: " + behaviorStr);
		}
	}

	void generateShip()
	{
		int edge = random.nextInt(4);

		if (edge == 0) {
			for (int x = 4; x < getWidth() - 2; x++) {
				if (getTile(x, 0) == CHANNEL) {
					makeShipAt(x, 0, NORTH_EDGE);
					return;
				}
			}
		} else if (edge == 1) {
			for (int y = 1; y < getHeight() - 2; y++) {
				if (getTile(0, y) == CHANNEL) {
					makeShipAt(0, y, EAST_EDGE);
					return;
				}
			}
		} else if (edge == 2) {
			for (int x = 4; x < getWidth() - 2; x++) {
				if (getTile(x, getHeight() - 1) == CHANNEL) {
					makeShipAt(x, getHeight() - 1, SOUTH_EDGE);
					return;
				}
			}
		} else {
			for (int y = 1; y < getHeight() - 2; y++) {
				if (getTile(getWidth() - 1, y) == CHANNEL) {
					makeShipAt(getWidth() - 1, y, EAST_EDGE);
					return;
				}
			}
		}
	}

	Sprite getSprite(SpriteKind kind)
	{
		for (Sprite s : sprites) {
			if (s.getKind() == kind)
				return s;
		}
		return null;
	}

	boolean hasSprite(SpriteKind kind)
	{
		return getSprite(kind) != null;
	}

	private void makeShipAt(int xpos, int ypos, int edge)
	{
		assert !hasSprite(SpriteKind.SHI);

		sprites.add(new ShipSprite(this, xpos, ypos, edge));
	}

	void generateCopter(int xpos, int ypos)
	{
		if (!hasSprite(SpriteKind.COP)) {
			sprites.add(new HelicopterSprite(this, xpos, ypos));
		}
	}

	void generatePlane(int xpos, int ypos)
	{
		if (!hasSprite(SpriteKind.AIR)) {
			sprites.add(new AirplaneSprite(this, xpos, ypos));
		}
	}

	void generateTrain(int xpos, int ypos)
	{
		if (totalPop > 20 &&
				!hasSprite(SpriteKind.TRA) &&
				random.nextInt(26) == 0) {
			sprites.add(new TrainSprite(this, xpos, ypos));
		}
	}

	// counts the population in a certain type of residential zone
	int doFreePop(int xpos, int ypos)
	{
		int count = 0;

		for (int x = xpos - 1; x <= xpos + 1; x++) {
			for (int y = ypos - 1; y <= ypos + 1; y++) {
				if (testBounds(x, y)) {
					char loc = getTile(x, y);
					if (loc >= LHTHR && loc <= HHTHR)
						count++;
				}
			}
		}

		return count;
	}

	// called every several cycles; this takes the census data collected in this
	// cycle and records it to the history
	//
	private void takeCensus()
	{
		int resMax = 0;
		int comMax = 0;
		int indMax = 0;

		for (int i = 118; i >= 0; i--) {
			if (history.getRes()[i] > resMax)
				resMax = history.getRes()[i];
			if (history.getCom()[i] > comMax)
				comMax = history.getRes()[i];
			if (history.getInd()[i] > indMax)
				indMax = history.getInd()[i];

			history.getRes()[i + 1] = history.getRes()[i];
			history.getCom()[i + 1] = history.getCom()[i];
			history.getInd()[i + 1] = history.getInd()[i];
			history.getCrime()[i + 1] = history.getCrime()[i];
			history.getPollution()[i + 1] = history.getPollution()[i];
			history.getMoney()[i + 1] = history.getMoney()[i];
		}

		history.getRes()[0] = resPop / 8;
		history.getCom()[0] = comPop;
		history.getInd()[0] = indPop;

		crimeRamp += (crimeAverage - crimeRamp) / 4;
		history.getCrime()[0] = Math.min(255, crimeRamp);

		polluteRamp += (pollutionAverage - polluteRamp) / 4;
		history.getPollution()[0] = Math.min(255, polluteRamp);

		int moneyScaled = cashFlow / 20 + 128;
		if (moneyScaled < 0)
			moneyScaled = 0;
		if (moneyScaled > 255)
			moneyScaled = 255;
		history.getMoney()[0] = moneyScaled;

		history.setCityTime(cityTime);

		needHospital = Integer.compare(resPop / 256, hospitalCount);

		needChurch = Integer.compare(resPop / 256, churchCount);
	}

	private void takeCensus2()
	{
		// update long term graphs
		int resMax = 0;
		int comMax = 0;
		int indMax = 0;

		for (int i = 238; i >= 120; i--) {
			if (history.getRes()[i] > resMax)
				resMax = history.getRes()[i];
			if (history.getCom()[i] > comMax)
				comMax = history.getRes()[i];
			if (history.getInd()[i] > indMax)
				indMax = history.getInd()[i];

			history.getRes()[i + 1] = history.getRes()[i];
			history.getCom()[i + 1] = history.getCom()[i];
			history.getInd()[i + 1] = history.getInd()[i];
			history.getCrime()[i + 1] = history.getCrime()[i];
			history.getPollution()[i + 1] = history.getPollution()[i];
			history.getMoney()[i + 1] = history.getMoney()[i];
		}

		history.getRes()[120] = resPop / 8;
		history.getCom()[120] = comPop;
		history.getInd()[120] = indPop;
		history.getCrime()[120] = history.getCrime()[0];
		history.getPollution()[120] = history.getPollution()[0];
		history.getMoney()[120] = history.getMoney()[0];
	}

	private void collectTaxPartial()
	{
		lastRoadTotal = roadTotal;
		lastRailTotal = railTotal;
		lastTotalPop = totalPop;
		lastFireStationCount = fireStationCount;
		lastPoliceCount = policeCount;

		BudgetNumbers b = generateBudget();

		budget.setTaxFund(budget.getTaxFund() + b.getTaxIncome());
		budget.setRoadFundEscrow(budget.getRoadFundEscrow() - b.getRoadFunded());
		budget.setFireFundEscrow(budget.getFireFundEscrow() - b.getFireFunded());
		budget.setPoliceFundEscrow(budget.getPoliceFundEscrow() - b.getPoliceFunded());

		taxEffect = b.getTaxRate();
		roadEffect = b.getRoadRequest() != 0 ?
				(int) Math.floor(32.0 * b.getRoadFunded() / b.getRoadRequest()) :
				32;
		policeEffect = b.getPoliceRequest() != 0 ?
				(int) Math.floor(1000.0 * b.getPoliceFunded() / b.getPoliceRequest()) :
				1000;
		fireEffect = b.getFireRequest() != 0 ?
				(int) Math.floor(1000.0 * b.getFireFunded() / b.getFireRequest()) :
				1000;
	}

	private void collectTax()
	{
		int revenue = budget.getTaxFund() / TAXFREQ;
		int expenses = -(budget.getRoadFundEscrow() + budget.getFireFundEscrow() + budget.getPoliceFundEscrow()) / TAXFREQ;

		FinancialHistory hist = new FinancialHistory();
		hist.setCityTime(cityTime);
		hist.setTaxIncome(revenue);
		hist.setOperatingExpenses(expenses);

		cashFlow = revenue - expenses;
		spend(-cashFlow);

		hist.setTotalFunds(budget.getTotalFunds());
		financialHistory.add(0, hist);

		budget.setTaxFund(0);
		budget.setRoadFundEscrow(0);
		budget.setFireFundEscrow(0);
		budget.setPoliceFundEscrow(0);
	}

	/**
	 * Calculate the current budget numbers.
	 */
	public BudgetNumbers generateBudget()
	{
		BudgetNumbers b = new BudgetNumbers();
		b.setTaxRate(Math.max(0, cityTax));
		b.setRoadPercent(Math.max(0.0, roadPercent));
		b.setFirePercent(Math.max(0.0, firePercent));
		b.setPolicePercent(Math.max(0.0, policePercent));

		b.setTaxIncome((int) Math.round(lastTotalPop * landValueAverage / 120.0 * b.getTaxRate() * FLevels[gameLevel]));
		assert b.getTaxIncome() >= 0;

		b.setRoadRequest((int) Math.round((lastRoadTotal + lastRailTotal * 2) * RLevels[gameLevel]));
		b.setFireRequest(FIRE_STATION_MAINTENANCE * lastFireStationCount);
		b.setPoliceRequest(POLICE_STATION_MAINTENANCE * lastPoliceCount);

		b.setRoadFunded((int) Math.round(b.getRoadRequest() * b.getRoadPercent()));
		b.setFireFunded((int) Math.round(b.getFireRequest() * b.getFirePercent()));
		b.setPoliceFunded((int) Math.round(b.getPoliceRequest() * b.getPolicePercent()));

		int yumDuckets = budget.getTotalFunds() + b.getTaxIncome();
		assert yumDuckets >= 0;

		if (yumDuckets >= b.getRoadFunded()) {
			yumDuckets -= b.getRoadFunded();
			if (yumDuckets >= b.getFireFunded()) {
				yumDuckets -= b.getFireFunded();
				if (yumDuckets < b.getPoliceFunded()) {
					assert b.getPoliceRequest() != 0;

					b.setPoliceFunded(yumDuckets);
					b.setPolicePercent((double) b.getPoliceFunded() / b.getPoliceRequest());
				}
			} else {
				assert b.getFireRequest() != 0;

				b.setFireFunded(yumDuckets);
				b.setFirePercent((double) b.getFireFunded() / b.getFireRequest());
				b.setPoliceFunded(0);
				b.setPolicePercent(0.0);
			}
		} else {
			assert b.getRoadRequest() != 0;

			b.setRoadFunded(yumDuckets);
			b.setRoadPercent((double) b.getRoadFunded() / b.getRoadRequest());
			b.setFireFunded(0);
			b.setFirePercent(0.0);
			b.setPoliceFunded(0);
			b.setPolicePercent(0.0);
		}

		return b;
	}

	int getPopulationDensity(int xpos, int ypos)
	{
		return popDensity[ypos / 2][xpos / 2];
	}

	void doMeltdown(int xpos, int ypos)
	{

		makeExplosion(xpos - 1, ypos - 1);
		makeExplosion(xpos - 1, ypos + 2);
		makeExplosion(xpos + 2, ypos - 1);
		makeExplosion(xpos + 2, ypos + 2);

		for (int x = xpos - 1; x < xpos + 3; x++) {
			for (int y = ypos - 1; y < ypos + 3; y++) {
				setTile(x, y, (char) (FIRE + random.nextInt(4)));
			}
		}

		for (int z = 0; z < 200; z++) {
			int x = xpos - 20 + random.nextInt(41);
			int y = ypos - 15 + random.nextInt(31);
			if (!testBounds(x, y))
				continue;

			int t = map[y][x];
			if (isZoneCenter(t)) {
				continue;
			}
			if (isCombustible(t) || t == DIRT) {
				setTile(x, y, RADTILE);
			}
		}

		sendMessageAt(MicropolisMessage.MELTDOWN_REPORT, xpos, ypos);
	}

	private void loadMisc(DataInput dis)
			throws IOException
	{
		dis.readShort(); //[0]... ignored?
		dis.readShort(); //[1] externalMarket, ignored
		resPop = dis.readShort();  //[2-4] populations
		comPop = dis.readShort();
		indPop = dis.readShort();
		resValve = dis.readShort(); //[5-7] valves
		comValve = dis.readShort();
		indValve = dis.readShort();
		cityTime = dis.readInt();   //[8-9] city time
		crimeRamp = dis.readShort(); //[10]
		polluteRamp = dis.readShort();
		landValueAverage = dis.readShort(); //[12]
		crimeAverage = dis.readShort();
		pollutionAverage = dis.readShort(); //[14]
		gameLevel = dis.readShort();
		evaluation.setCityClass(dis.readShort());  //[16]
		evaluation.setCityScore(dis.readShort());

		for (int i = 18; i < 50; i++) {
			dis.readShort();
		}

		budget.setTotalFunds(dis.readInt());   //[50-51] total funds
		autoBulldoze = dis.readShort() != 0;    //52
		autoBudget = dis.readShort() != 0;
		autoGo = dis.readShort() != 0;          //54
		dis.readShort();  // userSoundOn (this setting not saved to game file
		// in this edition of the game)
		cityTax = dis.readShort();              //56
		taxEffect = cityTax;
		int simSpeedAsInt = dis.readShort();
		simSpeed = simSpeedAsInt >= 0 && simSpeedAsInt <= 4 ? Speed.values()[simSpeedAsInt] : Speed.NORMAL;

		// read budget numbers, convert them to percentages
		//
		long n = dis.readInt();                   //58,59... police percent
		policePercent = n / 65536.0;
		n = dis.readInt();                     //60,61... fire percent
		firePercent = n / 65536.0;
		n = dis.readInt();                     //62,63... road percent
		roadPercent = n / 65536.0;

		for (int i = 64; i < 120; i++) {
			dis.readShort();
		}

		if (cityTime < 0) {
			cityTime = 0;
		}
		if (cityTax < 0 || cityTax > 20) {
			cityTax = 7;
		}
		if (gameLevel < 0 || gameLevel > 2) {
			gameLevel = 0;
		}
		if (evaluation.getCityClass() < 0 || evaluation.getCityClass() > 5) {
			evaluation.setCityClass(0);
		}
		if (evaluation.getCityScore() < 1 || evaluation.getCityScore() > 999) {
			evaluation.setCityScore(500);
		}

		resCap = false;
		comCap = false;
		indCap = false;
	}

	private void writeMisc(DataOutput out)
			throws IOException
	{
		out.writeShort(0);
		out.writeShort(0);
		out.writeShort(resPop);
		out.writeShort(comPop);
		out.writeShort(indPop);
		out.writeShort(resValve);
		out.writeShort(comValve);
		out.writeShort(indValve);
		//8
		out.writeInt(cityTime);
		out.writeShort(crimeRamp);
		out.writeShort(polluteRamp);
		//12
		out.writeShort(landValueAverage);
		out.writeShort(crimeAverage);
		out.writeShort(pollutionAverage);
		out.writeShort(gameLevel);
		//16
		out.writeShort(evaluation.getCityClass());
		out.writeShort(evaluation.getCityScore());
		//18
		for (int i = 18; i < 50; i++) {
			out.writeShort(0);
		}
		//50
		out.writeInt(budget.getTotalFunds());
		out.writeShort(autoBulldoze ? 1 : 0);
		out.writeShort(autoBudget ? 1 : 0);
		//54
		out.writeShort(autoGo ? 1 : 0);
		out.writeShort(1); //userSoundOn
		out.writeShort(cityTax);
		out.writeShort(simSpeed.ordinal());

		//58
		out.writeInt((int) (policePercent * 65536));
		out.writeInt((int) (firePercent * 65536));
		out.writeInt((int) (roadPercent * 65536));

		//64
		for (int i = 64; i < 120; i++) {
			out.writeShort(0);
		}
	}

	private void loadMap(DataInput dis)
			throws IOException
	{
		for (int x = 0; x < DEFAULT_WIDTH; x++) {
			for (int y = 0; y < DEFAULT_HEIGHT; y++) {
				int z = dis.readShort();
				z &= ~(1024 | 2048 | 4096 | 8192 | 16384); // clear ZONEBIT,ANIMBIT,BULLBIT,BURNBIT,CONDBIT on import
				map[y][x] = (char) z;
			}
		}
	}

	private void writeMap(DataOutput out)
			throws IOException
	{
		for (int x = 0; x < DEFAULT_WIDTH; x++) {
			for (int y = 0; y < DEFAULT_HEIGHT; y++) {
				int z = map[y][x];
				if (isConductive(z & LOMASK)) {
					z |= 16384;  //synthesize CONDBIT on export
				}
				if (isCombustible(z & LOMASK)) {
					z |= 8192;   //synthesize BURNBIT on export
				}
				if (isTileDozeable(x, y)) {
					z |= 4096;   //synthesize BULLBIT on export
				}
				if (isAnimated(z & LOMASK)) {
					z |= 2048;   //synthesize ANIMBIT on export
				}
				if (isZoneCenter(z & LOMASK)) {
					z |= 1024;   //synthesize ZONEBIT
				}
				out.writeShort(z);
			}
		}
	}

	public void load(File filename)
			throws IOException
	{
		try (FileInputStream fis = new FileInputStream(filename); DataInputStream dis = new DataInputStream(fis)) {
			if (fis.getChannel().size() > 27120) {
				// some editions of the classic Simcity game
				// start the file off with a 128-byte header,
				// but otherwise use the same format as us,
				// so read in that 128-byte header and continue
				// as before.
				byte[] bbHeader = new byte[128];
				fis.read(bbHeader);
			}
			load(dis);
		}
	}

	private void checkPowerMap()
	{
		coalCount = 0;
		nuclearCount = 0;

		powerPlants.clear();
		for (int y = 0; y < map.length; y++) {
			for (int x = 0; x < map[y].length; x++) {
				int tile = getTile(x, y);
				if (tile == NUCLEAR) {
					nuclearCount++;
					powerPlants.add(new CityLocation(x, y));
				} else if (tile == POWERPLANT) {
					coalCount++;
					powerPlants.add(new CityLocation(x, y));
				}
			}
		}

		powerScan();
	}

	private void load(DataInput dis)
			throws IOException
	{

		loadHistoryArray(history.getRes(), dis);
		loadHistoryArray(history.getCom(), dis);
		loadHistoryArray(history.getInd(), dis);
		loadHistoryArray(history.getCrime(), dis);
		loadHistoryArray(history.getPollution(), dis);
		loadHistoryArray(history.getMoney(), dis);
		loadMisc(dis);
		loadMap(dis);

		checkPowerMap();

		fireWholeMapChanged();
		fireDemandChanged();
		fireFundsChanged();
	}

	public void save(File filename)
			throws IOException
	{
		save(new FileOutputStream(filename));
	}

	private void save(OutputStream outStream)
			throws IOException
	{
		DataOutputStream out = new DataOutputStream(outStream);
		writeHistoryArray(history.getRes(), out);
		writeHistoryArray(history.getCom(), out);
		writeHistoryArray(history.getInd(), out);
		writeHistoryArray(history.getCrime(), out);
		writeHistoryArray(history.getPollution(), out);
		writeHistoryArray(history.getMoney(), out);
		writeMisc(out);
		writeMap(out);
		out.close();
	}

	public void toggleAutoBudget()
	{
		autoBudget = !autoBudget;
		fireOptionsChanged();
	}

	public void toggleAutoBulldoze()
	{
		autoBulldoze = !autoBulldoze;
		fireOptionsChanged();
	}

	public void toggleDisasters()
	{
		noDisasters = !noDisasters;
		fireOptionsChanged();
	}

	public void setSpeed(Speed newSpeed)
	{
		simSpeed = newSpeed;
		fireOptionsChanged();
	}

	public void animate()
	{
		acycle = (acycle + 1) % 960;
		if (acycle % 2 == 0) {
			step();
		}
		moveObjects();
		animateTiles();
	}

	public Sprite[] allSprites()
	{
		return sprites.toArray(SPRITES);
	}

	private void moveObjects()
	{
		for (Sprite sprite : allSprites()) {
			sprite.move();

			if (sprite.getFrame() == 0) {
				sprites.remove(sprite);
			}
		}
	}

	private void animateTiles()
	{
		for (int y = 0; y < map.length; y++) {
			for (int x = 0; x < map[y].length; x++) {
				char tilevalue = map[y][x];
				TileSpec spec = Tiles.get(tilevalue & LOMASK);
				if (spec != null && spec.getAnimNext() != null) {
					int flags = tilevalue & ALLBITS;
					setTile(x, y, (char)
							(spec.getAnimNext().getTileNumber() | flags)
					);
				}
			}
		}
	}

	public int getCityPopulation()
	{
		return cityPopulation;
	}

	void makeSound(int x, int y, Sound sound)
	{
		fireCitySound(sound, new CityLocation(x, y));
	}

	public void makeEarthquake()
	{
		makeSound(centerMassX, centerMassY, Sound.EXPLOSION_LOW);
		fireEarthquakeStarted();

		sendMessageAt(MicropolisMessage.EARTHQUAKE_REPORT, centerMassX, centerMassY);
		int time = random.nextInt(701) + 300;
		for (int z = 0; z < time; z++) {
			int x = random.nextInt(getWidth());
			int y = random.nextInt(getHeight());
			assert testBounds(x, y);

			if (isVulnerable(getTile(x, y))) {
				if (random.nextInt(4) == 0) {
					setTile(x, y, (char) (FIRE + random.nextInt(8)));
				} else {
					setTile(x, y, (char) (RUBBLE + random.nextInt(4)));
				}
			}
		}
	}

	private void setFire()
	{
		int x = random.nextInt(getWidth());
		int y = random.nextInt(getHeight());
		int t = getTile(x, y);

		if (isArsonable(t)) {
			setTile(x, y, (char) (FIRE + random.nextInt(8)));
			sendMessageAt(MicropolisMessage.FIRE_REPORT, x, y);
		}
	}

	public void makeFire()
	{
		// forty attempts at finding place to start fire
		for (int t = 0; t < 40; t++) {
			int x = random.nextInt(getWidth());
			int y = random.nextInt(getHeight());
			int tile = getTile(x, y);
			if (!isZoneCenter(tile) && isCombustible(tile)) {
				if (tile > 21 && tile < LASTZONE) {
					setTile(x, y, (char) (FIRE + random.nextInt(8)));
					sendMessageAt(MicropolisMessage.FIRE_REPORT, x, y);
					return;
				}
			}
		}
	}

	/**
	 * Force a meltdown to occur.
	 *
	 * @return true if a metldown was initiated.
	 */
	public boolean makeMeltdown()
	{
		ArrayList<CityLocation> candidates = new ArrayList<>();
		for (int y = 0; y < map.length; y++) {
			for (int x = 0; x < map[y].length; x++) {
				if (getTile(x, y) == NUCLEAR) {
					candidates.add(new CityLocation(x, y));
				}
			}
		}

		if (candidates.isEmpty()) {
			// tell caller that no nuclear plants were found
			return false;
		}

		int i = random.nextInt(candidates.size());
		CityLocation p = candidates.get(i);
		doMeltdown(p.getX(), p.getY());
		return true;
	}

	public void makeMonster()
	{
		MonsterSprite monster = (MonsterSprite) getSprite(SpriteKind.GOD);
		if (monster != null) {
			// already have a monster in town
			monster.setSoundCount(1);
			monster.setCount(1000);
			monster.setFlag(false);
			monster.setDestX(pollutionMaxLocationX);
			monster.setDestY(pollutionMaxLocationY);
			return;
		}

		// try to find a suitable starting spot for monster

		for (int i = 0; i < 300; i++) {
			int x = random.nextInt(getWidth() - 19) + 10;
			int y = random.nextInt(getHeight() - 9) + 5;
			int t = getTile(x, y);
			if (t == RIVER) {
				makeMonsterAt(x, y);
				return;
			}
		}

		// no "nice" location found, just start in center of map then
		makeMonsterAt(getWidth() / 2, getHeight() / 2);
	}

	private void makeMonsterAt(int xpos, int ypos)
	{
		assert !hasSprite(SpriteKind.GOD);
		sprites.add(new MonsterSprite(this, xpos, ypos));
		sendMessageAt(MicropolisMessage.MONSTER_REPORT, xpos, ypos);
	}

	public void makeTornado()
	{
		TornadoSprite tornado = (TornadoSprite) getSprite(SpriteKind.TOR);
		if (tornado != null) {
			// already have a tornado, so extend the length of the
			// existing tornado
			tornado.setCount(200);
			return;
		}

		//FIXME- this is not exactly like the original code
		int xpos = random.nextInt(getWidth() - 19) + 10;
		int ypos = random.nextInt(getHeight() - 19) + 10;
		sprites.add(new TornadoSprite(this, xpos, ypos));
		sendMessageAt(MicropolisMessage.TORNADO_REPORT, xpos, ypos);
	}

	public void makeFlood()
	{
		int[] dx = {0, 1, 0, -1};
		int[] dy = {-1, 0, 1, 0};

		for (int z = 0; z < 300; z++) {
			int x = random.nextInt(getWidth());
			int y = random.nextInt(getHeight());
			int tile = getTile(x, y);
			if (isRiverEdge(tile)) {
				for (int t = 0; t < 4; t++) {
					int xx = x + dx[t];
					int yy = y + dy[t];
					if (testBounds(xx, yy)) {
						int c = map[yy][xx];
						if (isFloodable(c)) {
							setTile(xx, yy, FLOOD);
							floodCnt = 30;
							sendMessageAt(MicropolisMessage.FLOOD_REPORT, xx, yy);
							return;
						}
					}
				}
			}
		}
	}

	/**
	 * Makes all component tiles of a zone bulldozable.
	 * Should be called whenever the key zone tile of a zone is destroyed,
	 * since otherwise the user would no longer have a way of destroying
	 * the zone.
	 *
	 * @see #shutdownZone
	 */
	void killZone(int xpos, int ypos, int zoneTile)
	{
		rateOGMem[ypos / 8][xpos / 8] -= 20;

		assert isZoneCenter(zoneTile);
		CityDimension dim = getZoneSizeFor(zoneTile);
		assert dim != null;
		assert dim.width >= 3;
		assert dim.height >= 3;

		// this will take care of stopping smoke animations
		shutdownZone(xpos, ypos, dim);
	}

	/**
	 * If a zone has a different image (animation) for when it is
	 * powered, switch to that different image here.
	 * Note: pollution is not accumulated here; see ptlScan()
	 * instead.
	 *
	 * @see #shutdownZone
	 */
	void powerZone(int xpos, int ypos, CityDimension zoneSize)
	{
		assert zoneSize.width >= 3;
		assert zoneSize.height >= 3;

		for (int dx = 0; dx < zoneSize.width; dx++) {
			for (int dy = 0; dy < zoneSize.height; dy++) {
				int x = xpos - 1 + dx;
				int y = ypos - 1 + dy;
				int tile = getTileRaw(x, y);
				TileSpec ts = Tiles.get(tile & LOMASK);
				if (ts != null && ts.getOnPower() != null) {
					setTile(x, y,
							(char) (ts.getOnPower().getTileNumber() | tile & ALLBITS)
					);
				}
			}
		}
	}

	/**
	 * If a zone has a different image (animation) for when it is
	 * powered, switch back to the original image.
	 *
	 * @see #powerZone
	 * @see #killZone
	 */
	void shutdownZone(int xpos, int ypos, CityDimension zoneSize)
	{
		assert zoneSize.width >= 3;
		assert zoneSize.height >= 3;

		for (int dx = 0; dx < zoneSize.width; dx++) {
			for (int dy = 0; dy < zoneSize.height; dy++) {
				int x = xpos - 1 + dx;
				int y = ypos - 1 + dy;
				int tile = getTileRaw(x, y);
				TileSpec ts = Tiles.get(tile & LOMASK);
				if (ts != null && ts.getOnShutdown() != null) {
					setTile(x, y,
							(char) (ts.getOnShutdown().getTileNumber() | tile & ALLBITS)
					);
				}
			}
		}
	}

	void makeExplosion(int xpos, int ypos)
	{
		makeExplosionAt(xpos * 16 + 8, ypos * 16 + 8);
	}

	/**
	 * Uses x,y coordinates as 1/16th-length tiles.
	 */
	void makeExplosionAt(int x, int y)
	{
		sprites.add(new ExplosionSprite(this, x, y));
	}

	private void checkGrowth()
	{
		if (cityTime % 4 == 0) {
			int newPop = (resPop + comPop * 8 + indPop * 8) * 20;
			if (cityPopulation != 0) {
				MicropolisMessage z = null;
				if (cityPopulation < 500000 && newPop >= 500000) {
					z = MicropolisMessage.POP_500K_REACHED;
				} else if (cityPopulation < 100000 && newPop >= 100000) {
					z = MicropolisMessage.POP_100K_REACHED;
				} else if (cityPopulation < 50000 && newPop >= 50000) {
					z = MicropolisMessage.POP_50K_REACHED;
				} else if (cityPopulation < 10000 && newPop >= 10000) {
					z = MicropolisMessage.POP_10K_REACHED;
				} else if (cityPopulation < 2000 && newPop >= 2000) {
					z = MicropolisMessage.POP_2K_REACHED;
				}
				if (z != null) {
					sendMessage(z);
				}
			}
			cityPopulation = newPop;
		}
	}

	private void doMessages()
	{
		//MORE (scenario stuff)

		checkGrowth();

		int totalZoneCount = resZoneCount + comZoneCount + indZoneCount;
		int powerCount = nuclearCount + coalCount;

		int z = cityTime % 64;
		switch (z) {
			case 1:
				if (totalZoneCount / 4 >= resZoneCount) {
					sendMessage(MicropolisMessage.NEED_RES);
				}
				break;
			case 5:
				if (totalZoneCount / 8 >= comZoneCount) {
					sendMessage(MicropolisMessage.NEED_COM);
				}
				break;
			case 10:
				if (totalZoneCount / 8 >= indZoneCount) {
					sendMessage(MicropolisMessage.NEED_IND);
				}
				break;
			case 14:
				if (totalZoneCount > 10 && totalZoneCount * 2 > roadTotal) {
					sendMessage(MicropolisMessage.NEED_ROADS);
				}
				break;
			case 18:
				if (totalZoneCount > 50 && totalZoneCount > railTotal) {
					sendMessage(MicropolisMessage.NEED_RAILS);
				}
				break;
			case 22:
				if (totalZoneCount > 10 && powerCount == 0) {
					sendMessage(MicropolisMessage.NEED_POWER);
				}
				break;
			case 26:
				resCap = resPop > 500 && stadiumCount == 0;
				if (resCap) {
					sendMessage(MicropolisMessage.NEED_STADIUM);
				}
				break;
			case 28:
				indCap = indPop > 70 && seaportCount == 0;
				if (indCap) {
					sendMessage(MicropolisMessage.NEED_SEAPORT);
				}
				break;
			case 30:
				comCap = comPop > 100 && airportCount == 0;
				if (comCap) {
					sendMessage(MicropolisMessage.NEED_AIRPORT);
				}
				break;
			case 32:
				int tm = unpoweredZoneCount + poweredZoneCount;
				if (tm != 0) {
					if ((double) poweredZoneCount / tm < 0.7) {
						sendMessage(MicropolisMessage.BLACKOUTS);
					}
				}
				break;
			case 35:
				if (pollutionAverage > 60) { // FIXME, consider changing threshold to 80
					sendMessage(MicropolisMessage.HIGH_POLLUTION);
				}
				break;
			case 42:
				if (crimeAverage > 100) {
					sendMessage(MicropolisMessage.HIGH_CRIME);
				}
				break;
			case 45:
				if (totalPop > 60 && fireStationCount == 0) {
					sendMessage(MicropolisMessage.NEED_FIRESTATION);
				}
				break;
			case 48:
				if (totalPop > 60 && policeCount == 0) {
					sendMessage(MicropolisMessage.NEED_POLICE);
				}
				break;
			case 51:
				if (cityTax > 12) {
					sendMessage(MicropolisMessage.HIGH_TAXES);
				}
				break;
			case 54:
				if (roadEffect < 20 && roadTotal > 30) {
					sendMessage(MicropolisMessage.ROADS_NEED_FUNDING);
				}
				break;
			case 57:
				if (fireEffect < 700 && totalPop > 20) {
					sendMessage(MicropolisMessage.FIRE_NEED_FUNDING);
				}
				break;
			case 60:
				if (policeEffect < 700 && totalPop > 20) {
					sendMessage(MicropolisMessage.POLICE_NEED_FUNDING);
				}
				break;
			case 63:
				if (trafficAverage > 60) {
					sendMessage(MicropolisMessage.HIGH_TRAFFIC);
				}
				break;
			default:
				//nothing
		}
	}

	private void sendMessage(MicropolisMessage message)
	{
		fireCityMessage(message, null);
	}

	void sendMessageAt(MicropolisMessage message, int x, int y)
	{
		fireCityMessage(message, new CityLocation(x, y));
	}

	public ZoneStatus queryZoneStatus(int xpos, int ypos)
	{
		ZoneStatus zs = new ZoneStatus();
		zs.setBuilding(getDescriptionNumber(getTile(xpos, ypos)));

		int z;
		z = popDensity[ypos / 2][xpos / 2] / 64 % 4;
		zs.setPopDensity(z + 1);

		z = landValueMem[ypos / 2][xpos / 2];
		z = z < 30 ? 4 : z < 80 ? 5 : z < 150 ? 6 : 7;
		zs.setLandValue(z + 1);

		z = crimeMem[ypos / 2][xpos / 2] / 64 % 4 + 8;
		zs.setCrimeLevel(z + 1);

		z = Math.max(13, pollutionMem[ypos / 2][xpos / 2] / 64 % 4 + 12);
		zs.setPollution(z + 1);

		z = rateOGMem[ypos / 8][xpos / 8];
		z = z < 0 ? 16 : z == 0 ? 17 : z <= 100 ? 18 : 19;
		zs.setGrowthRate(z + 1);

		return zs;
	}

	public int getResValve()
	{
		return resValve;
	}

	public int getComValve()
	{
		return comValve;
	}

	public int getIndValve()
	{
		return indValve;
	}

	public void setGameLevel(int newLevel)
	{
		assert GameLevel.isValid(newLevel);

		gameLevel = newLevel;
		fireOptionsChanged();
	}

	public void setFunds(int totalFunds)
	{
		budget.setTotalFunds(totalFunds);
	}

	/**
	 * For each 2x2 section of the city, the pollution level of the city (0-255).
	 * 0 is no pollution; 255 is maximum pollution.
	 * Updated each cycle by ptlScan(); affects land value.
	 */
	public int[][] getPollutionMem()
	{
		return pollutionMem;
	}

	/**
	 * For each 2x2 section of the city, the crime level of the city (0-250).
	 * 0 is no crime; 250 is maximum crime.
	 * Updated each cycle by crimeScan(); affects land value.
	 */
	public int[][] getCrimeMem()
	{
		return crimeMem;
	}

	/**
	 * For each 2x2 section of the city, the population density (0-?).
	 * Used for map overlays and as a factor for crime rates.
	 */
	public int[][] getPopDensity()
	{
		return popDensity;
	}

	/**
	 * For each 8x8 section of the city, the rate of growth.
	 * Capped to a number between -200 and 200.
	 * Used for reporting purposes only; the number has no affect.
	 */
	public int[][] getRateOGMem()
	{
		return rateOGMem;
	}

	public int[][] getFireRate()
	{
		return fireRate;
	}

	public int[][] getPoliceMapEffect()
	{
		return policeMapEffect;
	}

	public boolean isAutoBulldoze()
	{
		return autoBulldoze;
	}

	public boolean isAutoBudget()
	{
		return autoBudget;
	}

	public Speed getSimSpeed()
	{
		return simSpeed;
	}

	public boolean isNoDisasters()
	{
		return noDisasters;
	}

	public int getGameLevel()
	{
		return gameLevel;
	}

	public int getCenterMassX()
	{
		return centerMassX;
	}

	public int getCenterMassY()
	{
		return centerMassY;
	}

	public int getCityTax()
	{
		return cityTax;
	}

	public void setCityTax(int cityTax)
	{
		this.cityTax = cityTax;
	}

	public double getRoadPercent()
	{
		return roadPercent;
	}

	public void setRoadPercent(double roadPercent)
	{
		this.roadPercent = roadPercent;
	}

	public double getPolicePercent()
	{
		return policePercent;
	}

	public void setPolicePercent(double policePercent)
	{
		this.policePercent = policePercent;
	}

	public double getFirePercent()
	{
		return firePercent;
	}

	public void setFirePercent(double firePercent)
	{
		this.firePercent = firePercent;
	}

	public int getCityTime()
	{
		return cityTime;
	}

	public char[][] getMap()
	{
		return map;
	}

	public int[][] getFireStMap()
	{
		return fireStMap;
	}

	public int[][] getPoliceMap()
	{
		return policeMap;
	}

	/**
	 * For each 8x8 section of city, this is an integer between 0 and 64,
	 * with higher numbers being closer to the center of the city.
	 */
	public int[][] getComRate()
	{
		return comRate;
	}

	public int getPoweredZoneCount()
	{
		return poweredZoneCount;
	}

	public void setPoweredZoneCount(int poweredZoneCount)
	{
		this.poweredZoneCount = poweredZoneCount;
	}

	public int getUnpoweredZoneCount()
	{
		return unpoweredZoneCount;
	}

	public void setUnpoweredZoneCount(int unpoweredZoneCount)
	{
		this.unpoweredZoneCount = unpoweredZoneCount;
	}

	public int getRoadTotal()
	{
		return roadTotal;
	}

	public void setRoadTotal(int roadTotal)
	{
		this.roadTotal = roadTotal;
	}

	public int getRailTotal()
	{
		return railTotal;
	}

	public void setRailTotal(int railTotal)
	{
		this.railTotal = railTotal;
	}

	public int getFirePop()
	{
		return firePop;
	}

	public void setFirePop(int firePop)
	{
		this.firePop = firePop;
	}

	public int getResZoneCount()
	{
		return resZoneCount;
	}

	public void setResZoneCount(int resZoneCount)
	{
		this.resZoneCount = resZoneCount;
	}

	public int getComZoneCount()
	{
		return comZoneCount;
	}

	public void setComZoneCount(int comZoneCount)
	{
		this.comZoneCount = comZoneCount;
	}

	public int getIndZoneCount()
	{
		return indZoneCount;
	}

	public void setIndZoneCount(int indZoneCount)
	{
		this.indZoneCount = indZoneCount;
	}

	public int getResPop()
	{
		return resPop;
	}

	public void setResPop(int resPop)
	{
		this.resPop = resPop;
	}

	public int getComPop()
	{
		return comPop;
	}

	public void setComPop(int comPop)
	{
		this.comPop = comPop;
	}

	public int getIndPop()
	{
		return indPop;
	}

	public void setIndPop(int indPop)
	{
		this.indPop = indPop;
	}

	public int getHospitalCount()
	{
		return hospitalCount;
	}

	public void setHospitalCount(int hospitalCount)
	{
		this.hospitalCount = hospitalCount;
	}

	public int getChurchCount()
	{
		return churchCount;
	}

	public void setChurchCount(int churchCount)
	{
		this.churchCount = churchCount;
	}

	public int getPoliceCount()
	{
		return policeCount;
	}

	public void setPoliceCount(int policeCount)
	{
		this.policeCount = policeCount;
	}

	public int getFireStationCount()
	{
		return fireStationCount;
	}

	public void setFireStationCount(int fireStationCount)
	{
		this.fireStationCount = fireStationCount;
	}

	public int getStadiumCount()
	{
		return stadiumCount;
	}

	public void setStadiumCount(int stadiumCount)
	{
		this.stadiumCount = stadiumCount;
	}

	public int getCoalCount()
	{
		return coalCount;
	}

	public void setCoalCount(int coalCount)
	{
		this.coalCount = coalCount;
	}

	public int getNuclearCount()
	{
		return nuclearCount;
	}

	public void setNuclearCount(int nuclearCount)
	{
		this.nuclearCount = nuclearCount;
	}

	public int getSeaportCount()
	{
		return seaportCount;
	}

	public void setSeaportCount(int seaportCount)
	{
		this.seaportCount = seaportCount;
	}

	public int getAirportCount()
	{
		return airportCount;
	}

	public void setAirportCount(int airportCount)
	{
		this.airportCount = airportCount;
	}

	public int getTotalPop()
	{
		return totalPop;
	}

	public int getNeedHospital()
	{
		return needHospital;
	}

	public void setNeedHospital(int needHospital)
	{
		this.needHospital = needHospital;
	}

	public int getNeedChurch()
	{
		return needChurch;
	}

	public void setNeedChurch(int needChurch)
	{
		this.needChurch = needChurch;
	}

	public int getCrimeAverage()
	{
		return crimeAverage;
	}

	public int getPollutionAverage()
	{
		return pollutionAverage;
	}

	public int getLandValueAverage()
	{
		return landValueAverage;
	}

	public int getTrafficAverage()
	{
		return trafficAverage;
	}

	public void setTrafficAverage(int trafficAverage)
	{
		this.trafficAverage = trafficAverage;
	}

	public boolean isResCap()
	{
		return resCap;
	}

	public boolean isComCap()
	{
		return comCap;
	}

	public boolean isIndCap()
	{
		return indCap;
	}

	public int getRoadEffect()
	{
		return roadEffect;
	}

	public int getPoliceEffect()
	{
		return policeEffect;
	}

	public int getFireEffect()
	{
		return fireEffect;
	}

	public int getFloodCnt()
	{
		return floodCnt;
	}

	public int getAcycle()
	{
		return acycle;
	}

	public CityBudget getBudget()
	{
		return budget;
	}

	public CityEval getEvaluation()
	{
		return evaluation;
	}

	public History getHistory()
	{
		return history;
	}

	public List<FinancialHistory> getFinancialHistory()
	{
		return financialHistory;
	}

	public Random getRandom()
	{
		return random;
	}

	public Iterable<Sprite> getSprites()
	{
		return sprites;
	}

	public List<CityLocation> getPowerPlants()
	{
		return powerPlants;
	}
}
