// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import static micropolisj.engine.TileConstants.AIRPORT;
import static micropolisj.engine.TileConstants.CHURCH;
import static micropolisj.engine.TileConstants.COMCLR;
import static micropolisj.engine.TileConstants.CZB;
import static micropolisj.engine.TileConstants.DIRT;
import static micropolisj.engine.TileConstants.FIRESTATION;
import static micropolisj.engine.TileConstants.FOOTBALLGAME1;
import static micropolisj.engine.TileConstants.FOOTBALLGAME2;
import static micropolisj.engine.TileConstants.FULLSTADIUM;
import static micropolisj.engine.TileConstants.HHTHR;
import static micropolisj.engine.TileConstants.HOSPITAL;
import static micropolisj.engine.TileConstants.HOUSE;
import static micropolisj.engine.TileConstants.INDCLR;
import static micropolisj.engine.TileConstants.IZB;
import static micropolisj.engine.TileConstants.LHTHR;
import static micropolisj.engine.TileConstants.LOMASK;
import static micropolisj.engine.TileConstants.NUCLEAR;
import static micropolisj.engine.TileConstants.POLICESTATION;
import static micropolisj.engine.TileConstants.PORT;
import static micropolisj.engine.TileConstants.POWERPLANT;
import static micropolisj.engine.TileConstants.RESCLR;
import static micropolisj.engine.TileConstants.RZB;
import static micropolisj.engine.TileConstants.STADIUM;
import static micropolisj.engine.TileConstants.commercialZonePop;
import static micropolisj.engine.TileConstants.getZoneSizeFor;
import static micropolisj.engine.TileConstants.industrialZonePop;
import static micropolisj.engine.TileConstants.isAnimated;
import static micropolisj.engine.TileConstants.isIndestructible;
import static micropolisj.engine.TileConstants.isIndestructible2;
import static micropolisj.engine.TileConstants.isRail;
import static micropolisj.engine.TileConstants.isResidentialClear;
import static micropolisj.engine.TileConstants.isRoadAny;
import static micropolisj.engine.TileConstants.isZoneCenter;
import static micropolisj.engine.TileConstants.residentialZonePop;

/**
 * Process individual tiles of the map for each cycle.
 * In each sim cycle each tile will get activated, and this
 * class contains the activation code.
 */
class MapScanner extends TileBehavior
{

	private static final int[] MELTDOWN_TAB = {30000, 20000, 10000};

	private final Behavior behavior;
	private final TrafficGen traffic;

	MapScanner(Micropolis city, Behavior behavior)
	{
		super(city);
		this.behavior = behavior;
		traffic = new TrafficGen(city);
	}

	/**
	 * Evaluates the zone value of the current industrial zone location.
	 *
	 * @return an integer between -3000 and 3000.
	 * Same meaning as evalResidential.
	 */
	private static int evalIndustrial(int traf)
	{
		return traf < 0 ? -1000 : 0;
	}

	@Override
	public void apply()
	{
		switch (behavior) {
			case RESIDENTIAL:
				doResidential();
				return;
			case HOSPITAL_CHURCH:
				doHospitalChurch();
				return;
			case COMMERCIAL:
				doCommercial();
				return;
			case INDUSTRIAL:
				doIndustrial();
				return;
			case COAL:
				doCoalPower();
				return;
			case NUCLEAR:
				doNuclearPower();
				return;
			case FIRESTATION:
				doFireStation();
				return;
			case POLICESTATION:
				doPoliceStation();
				return;
			case STADIUM_EMPTY:
				doStadiumEmpty();
				return;
			case STADIUM_FULL:
				doStadiumFull();
				return;
			case AIRPORT:
				doAirport();
				return;
			case SEAPORT:
				doSeaport();
				return;
			default:
				assert false;
		}
	}

	private boolean checkZonePower()
	{
		boolean zonePwrFlag = setZonePower();

		if (zonePwrFlag) {
			getCity().setPoweredZoneCount(getCity().getPoweredZoneCount() + 1);
		} else {
			getCity().setUnpoweredZoneCount(getCity().getUnpoweredZoneCount() + 1);
		}

		return zonePwrFlag;
	}

	private boolean setZonePower()
	{
		boolean oldPower = getCity().isTilePowered(getXpos(), getYpos());
		boolean newPower = getTile() == NUCLEAR ||
				getTile() == POWERPLANT ||
				getCity().hasPower(getXpos(), getYpos());

		if (newPower && !oldPower) {
			getCity().setTilePower(getXpos(), getYpos(), true);
			getCity().powerZone(getXpos(), getYpos(), getZoneSizeFor(getTile()));
		} else if (!newPower && oldPower) {
			getCity().setTilePower(getXpos(), getYpos(), false);
			getCity().shutdownZone(getXpos(), getYpos(), getZoneSizeFor(getTile()));
		}

		return newPower;
	}

	/**
	 * Place a 3x3 zone on to the map, centered on the current location.
	 * Note: nothing is done if part of this zone is off the edge
	 * of the map or is being flooded or radioactive.
	 *
	 * @param base The "zone" tile value for this zone.
	 */
	private void zonePlop(int base)
	{
		assert isZoneCenter(base);

		BuildingInfo bi = Tiles.get(base).getBuildingInfo();
		assert bi != null;

		for (int y = getYpos() - 1; y < getYpos() - 1 + bi.getHeight(); y++) {
			for (int x = getXpos() - 1; x < getXpos() - 1 + bi.getWidth(); x++) {
				if (!getCity().testBounds(x, y)) {
					return;
				}
				if (isIndestructible2(getCity().getTile(x, y))) {
					// radioactive, on fire, or flooded
					return;
				}
			}
		}

		assert bi.getMembers().length == bi.getWidth() * bi.getHeight();
		int i = 0;
		for (int y = getYpos() - 1; y < getYpos() - 1 + bi.getHeight(); y++) {
			for (int x = getXpos() - 1; x < getXpos() - 1 + bi.getWidth(); x++) {
				getCity().setTile(x, y, (char) bi.getMembers()[i]);
				i++;
			}
		}

		// refresh own tile property
		setTile(getCity().getTile(getXpos(), getYpos()));

		setZonePower();
	}

	private void doCoalPower()
	{
		checkZonePower();
		getCity().setCoalCount(getCity().getCoalCount() + 1);
		if (getCity().getCityTime() % 8 == 0) {
			repairZone(POWERPLANT, 4);
		}

		getCity().getPowerPlants().add(new CityLocation(getXpos(), getYpos()));
	}

	private void doNuclearPower()
	{
		checkZonePower();
		if (!getCity().isNoDisasters() && getRandom().nextInt(MELTDOWN_TAB[getCity().getGameLevel()] + 1) == 0) {
			getCity().doMeltdown(getXpos(), getYpos());
			return;
		}

		getCity().setNuclearCount(getCity().getNuclearCount() + 1);
		if (getCity().getCityTime() % 8 == 0) {
			repairZone(NUCLEAR, 4);
		}

		getCity().getPowerPlants().add(new CityLocation(getXpos(), getYpos()));
	}

	private void doFireStation()
	{
		boolean powerOn = checkZonePower();
		getCity().setFireStationCount(getCity().getFireStationCount() + 1);
		if (getCity().getCityTime() % 8 == 0) {
			repairZone(FIRESTATION, 3);
		}

		int z;
		//if powered, get effect
		// from the funding ratio
		z = powerOn ? getCity().getFireEffect() : getCity().getFireEffect() / 2;

		traffic.setMapX(getXpos());
		traffic.setMapY(getYpos());
		if (!traffic.findPerimeterRoad()) {
			z /= 2;
		}

		getCity().getFireStMap()[getYpos() / 8][getXpos() / 8] += z;
	}

	private void doPoliceStation()
	{
		boolean powerOn = checkZonePower();
		getCity().setPoliceCount(getCity().getPoliceCount() + 1);
		if (getCity().getCityTime() % 8 == 0) {
			repairZone(POLICESTATION, 3);
		}

		int z;
		z = powerOn ? getCity().getPoliceEffect() : getCity().getPoliceEffect() / 2;

		traffic.setMapX(getXpos());
		traffic.setMapY(getYpos());
		if (!traffic.findPerimeterRoad()) {
			z /= 2;
		}

		getCity().getPoliceMap()[getYpos() / 8][getXpos() / 8] += z;
	}

	private void doStadiumEmpty()
	{
		boolean powerOn = checkZonePower();
		getCity().setStadiumCount(getCity().getStadiumCount() + 1);
		if (getCity().getCityTime() % 16 == 0) {
			repairZone(STADIUM, 4);
		}

		if (powerOn) {
			if ((getCity().getCityTime() + getXpos() + getYpos()) % 32 == 0) {
				drawStadium(FULLSTADIUM);
				getCity().setTile(getXpos() + 1, getYpos(), FOOTBALLGAME1);
				getCity().setTile(getXpos() + 1, getYpos() + 1, FOOTBALLGAME2);
			}
		}
	}

	private void doStadiumFull()
	{
		checkZonePower();
		getCity().setStadiumCount(getCity().getStadiumCount() + 1);
		if ((getCity().getCityTime() + getXpos() + getYpos()) % 8 == 0) {
			drawStadium(STADIUM);
		}
	}

	private void doAirport()
	{
		boolean powerOn = checkZonePower();
		getCity().setAirportCount(getCity().getAirportCount() + 1);
		if (getCity().getCityTime() % 8 == 0) {
			repairZone(AIRPORT, 6);
		}

		if (powerOn) {

			if (getRandom().nextInt(6) == 0) {
				getCity().generatePlane(getXpos(), getYpos());
			}

			if (getRandom().nextInt(13) == 0) {
				getCity().generateCopter(getXpos(), getYpos());
			}
		}
	}

	private void doSeaport()
	{
		boolean powerOn = checkZonePower();
		getCity().setSeaportCount(getCity().getSeaportCount() + 1);
		if (getCity().getCityTime() % 16 == 0) {
			repairZone(PORT, 4);
		}

		if (powerOn && !getCity().hasSprite(SpriteKind.SHI)) {
			getCity().generateShip();
		}
	}

	/**
	 * Place hospital or church if needed.
	 */
	private void makeHospital()
	{
		if (getCity().getNeedHospital() > 0) {
			zonePlop(HOSPITAL);
			getCity().setNeedHospital(0);
		}

//FIXME- should be 'else if'
		if (getCity().getNeedChurch() > 0) {
			zonePlop(CHURCH);
			getCity().setNeedChurch(0);
		}
	}

	/**
	 * Called when the current tile is the key tile of a
	 * hospital or church.
	 */
	private void doHospitalChurch()
	{
		checkZonePower();
		if (getTile() == HOSPITAL) {
			getCity().setHospitalCount(getCity().getHospitalCount() + 1);

			if (getCity().getCityTime() % 16 == 0) {
				repairZone(HOSPITAL, 3);
			}
			if (getCity().getNeedHospital() == -1)  //too many hospitals
			{
				if (getRandom().nextInt(21) == 0) {
					zonePlop(RESCLR);
				}
			}
		} else if (getTile() == CHURCH) {
			getCity().setChurchCount(getCity().getChurchCount() + 1);

			if (getCity().getCityTime() % 16 == 0) {
				repairZone(CHURCH, 3);
			}
			if (getCity().getNeedChurch() == -1) //too many churches
			{
				if (getRandom().nextInt(21) == 0) {
					zonePlop(RESCLR);
				}
			}
		}
	}

	/**
	 * Regenerate the tiles that make up the zone, repairing from
	 * fire, etc.
	 * Only tiles that are not rubble, radioactive, flooded, or
	 * on fire will be regenerated.
	 *
	 * @param zoneCenter the tile value for the "center" tile of the zone
	 * @param zoneSize   integer (3-6) indicating the width/height of
	 *                   the zone.
	 */
	private void repairZone(char zoneCenter, int zoneSize)
	{
		// from the given center tile, figure out what the
		// northwest tile should be
		int zoneBase = zoneCenter - 1 - zoneSize;

		for (int y = 0; y < zoneSize; y++) {
			for (int x = 0; x < zoneSize; x++, zoneBase++) {
				int xx = getXpos() - 1 + x;
				int yy = getYpos() - 1 + y;

				if (getCity().testBounds(xx, yy)) {
					int thCh = getCity().getTile(xx, yy);
					if (isZoneCenter(thCh)) {
						continue;
					}

					if (isAnimated(thCh))
						continue;

					if (!isIndestructible(thCh)) {  //not rubble, radiactive, on fire or flooded

						getCity().setTile(xx, yy, (char) zoneBase);
					}
				}
			}
		}
	}

	/**
	 * Called when the current tile is the key tile of a commercial
	 * zone.
	 */
	private void doCommercial()
	{
		boolean powerOn = checkZonePower();
		getCity().setComZoneCount(getCity().getComZoneCount() + 1);

		int tpop = commercialZonePop(getTile());
		getCity().setComPop(getCity().getComPop() + tpop);

		int trafficGood;
		trafficGood = tpop > getRandom().nextInt(6) ? makeTraffic(ZoneType.COMMERCIAL) : 1;

		if (trafficGood == -1) {
			int value = getCRValue();
			doCommercialOut(tpop, value);
			return;
		}

		if (getRandom().nextInt(8) == 0) {
			int locValve = evalCommercial(trafficGood);
			int zscore = getCity().getComValve() + locValve;

			if (!powerOn)
				zscore = -500;

			if (trafficGood != 0 &&
					zscore > -350 &&
					zscore - 26380 > getRandom().nextInt(0x10000) - 0x8000) {
				int value = getCRValue();
				doCommercialIn(tpop, value);
				return;
			}

			if (zscore < 350 && zscore + 26380 < getRandom().nextInt(0x10000) - 0x8000) {
				int value = getCRValue();
				doCommercialOut(tpop, value);
			}
		}
	}

	/**
	 * Called when the current tile is the key tile of an
	 * industrial zone.
	 */
	private void doIndustrial()
	{
		boolean powerOn = checkZonePower();
		getCity().setIndZoneCount(getCity().getIndZoneCount() + 1);

		int tpop = industrialZonePop(getTile());
		getCity().setIndPop(getCity().getIndPop() + tpop);

		int trafficGood;
		trafficGood = tpop > getRandom().nextInt(6) ? makeTraffic(ZoneType.INDUSTRIAL) : 1;

		if (trafficGood == -1) {
			doIndustrialOut(tpop, getRandom().nextInt(2));
			return;
		}

		if (getRandom().nextInt(8) == 0) {
			int locValve = evalIndustrial(trafficGood);
			int zscore = getCity().getIndValve() + locValve;

			if (!powerOn)
				zscore = -500;

			if (zscore > -350 &&
					zscore - 26380 > getRandom().nextInt(0x10000) - 0x8000) {
				int value = getRandom().nextInt(2);
				doIndustrialIn(tpop, value);
				return;
			}

			if (zscore < 350 && zscore + 26380 < getRandom().nextInt(0x10000) - 0x8000) {
				int value = getRandom().nextInt(2);
				doIndustrialOut(tpop, value);
			}
		}
	}

	/**
	 * Called when the current tile is the key tile of a
	 * residential zone.
	 */
	private void doResidential()
	{
		boolean powerOn = checkZonePower();
		getCity().setResZoneCount(getCity().getResZoneCount() + 1);

		int tpop; //population of this zone
		tpop = getTile() == RESCLR ? getCity().doFreePop(getXpos(), getYpos()) : residentialZonePop(getTile());

		getCity().setResPop(getCity().getResPop() + tpop);

		int trafficGood;
		trafficGood = tpop > getRandom().nextInt(36) ? makeTraffic(ZoneType.RESIDENTIAL) : 1;

		if (trafficGood == -1) {
			int value = getCRValue();
			doResidentialOut(tpop, value);
			return;
		}

		if (getTile() == RESCLR || getRandom().nextInt(8) == 0) {
			int locValve = evalResidential(trafficGood);
			int zscore = getCity().getResValve() + locValve;

			if (!powerOn)
				zscore = -500;

			if (zscore > -350 && zscore - 26380 > getRandom().nextInt(0x10000) - 0x8000) {
				if (tpop == 0 && getRandom().nextInt(4) == 0) {
					makeHospital();
					return;
				}

				int value = getCRValue();
				doResidentialIn(tpop, value);
				return;
			}

			if (zscore < 350 && zscore + 26380 < getRandom().nextInt(0x10000) - 0x8000) {
				int value = getCRValue();
				doResidentialOut(tpop, value);
			}
		}
	}

	/**
	 * Consider the value of building a single-lot house at certain
	 * coordinates.
	 *
	 * @return integer; positive number indicates good place for
	 * house to go; zero or a negative number indicates a bad place.
	 */
	private int evalLot(int x, int y)
	{
		// test for clear lot
		int aTile = getCity().getTile(x, y);
		if (aTile != DIRT && !isResidentialClear(aTile)) {
			return -1;
		}

		int score = 1;

		int[] dx = {0, 1, 0, -1};
		int[] dy = {-1, 0, 1, 0};
		for (int z = 0; z < 4; z++) {
			int xx = x + dx[z];
			int yy = y + dy[z];

			// look for road
			if (getCity().testBounds(xx, yy)) {
				int tmp = getCity().getTile(xx, yy);
				if (isRoadAny(tmp) || isRail(tmp)) {
					score++;
				}
			}
		}

		return score;
	}

	/**
	 * Build a single-lot house on the current residential zone.
	 */
	private void buildHouse(int value)
	{
		assert value >= 0 && value <= 3;

		int[] zeX = {0, -1, 0, 1, -1, 1, -1, 0, 1};
		int[] zeY = {0, -1, -1, -1, 0, 0, 1, 1, 1};

		int bestLoc = 0;
		int hscore = 0;

		for (int z = 1; z < 9; z++) {
			int xx = getXpos() + zeX[z];
			int yy = getYpos() + zeY[z];

			if (getCity().testBounds(xx, yy)) {
				int score = evalLot(xx, yy);

				if (score != 0) {
					if (score > hscore) {
						hscore = score;
						bestLoc = z;
					}

					if (score == hscore && getRandom().nextInt(8) == 0) {
						bestLoc = z;
					}
				}
			}
		}

		if (bestLoc != 0) {
			int xx = getXpos() + zeX[bestLoc];
			int yy = getYpos() + zeY[bestLoc];
			int houseNumber = value * 3 + getRandom().nextInt(3);
			assert houseNumber >= 0 && houseNumber < 12;

			assert getCity().testBounds(xx, yy);
			getCity().setTile(xx, yy, (char) (HOUSE + houseNumber));
		}
	}

	private void doCommercialIn(int pop, int value)
	{
		int z = getCity().getLandValue(getXpos(), getYpos()) / 32;
		if (pop > z)
			return;

		if (pop < 5) {
			comPlop(pop, value);
			adjustROG(8);
		}
	}

	private void doIndustrialIn(int pop, int value)
	{
		if (pop < 4) {
			indPlop(pop, value);
			adjustROG(8);
		}
	}

	private void doResidentialIn(int pop, int value)
	{
		assert value >= 0 && value <= 3;

		int z = getCity().getPollutionMem()[getYpos() / 2][getXpos() / 2];
		if (z > 128)
			return;

		if (getTile() == RESCLR) {
			if (pop < 8) {
				buildHouse(value);
				adjustROG(1);
				return;
			}

			if (getCity().getPopulationDensity(getXpos(), getYpos()) > 64) {
				residentialPlop(0, value);
				adjustROG(8);
				return;
			}
			return;
		}

		if (pop < 40) {
			residentialPlop(pop / 8 - 1, value);
			adjustROG(8);
		}
	}

	private void comPlop(int density, int value)
	{
		int base = (value * 5 + density) * 9 + CZB;
		zonePlop(base);
	}

	private void indPlop(int density, int value)
	{
		int base = (value * 4 + density) * 9 + IZB;
		zonePlop(base);
	}

	private void residentialPlop(int density, int value)
	{
		int base = (value * 4 + density) * 9 + RZB;
		zonePlop(base);
	}

	private void doCommercialOut(int pop, int value)
	{
		if (pop > 1) {
			comPlop(pop - 2, value);
			adjustROG(-8);
		} else if (pop == 1) {
			zonePlop(COMCLR);
			adjustROG(-8);
		}
	}

	private void doIndustrialOut(int pop, int value)
	{
		if (pop > 1) {
			indPlop(pop - 2, value);
			adjustROG(-8);
		} else if (pop == 1) {
			zonePlop(INDCLR);
			adjustROG(-8);
		}
	}

	private void doResidentialOut(int pop, int value)
	{
		assert value >= 0 && value < 4;

		char[] border = {0, 3, 6, 1, 4, 7, 2, 5, 8};

		if (pop == 0)
			return;

		if (pop > 16) {
			// downgrade to a lower-density full-size residential zone
			residentialPlop((pop - 24) / 8, value);
			adjustROG(-8);
			return;
		}

		if (pop == 16) {
			// downgrade from full-size zone to 8 little houses

			boolean pwr = getCity().isTilePowered(getXpos(), getYpos());
			getCity().setTile(getXpos(), getYpos(), RESCLR);
			getCity().setTilePower(getXpos(), getYpos(), pwr);

			for (int x = getXpos() - 1; x <= getXpos() + 1; x++) {
				for (int y = getYpos() - 1; y <= getYpos() + 1; y++) {
					if (getCity().testBounds(x, y)) {
						if (!(x == getXpos() && y == getYpos())) {
							// pick a random small house
							int houseNumber = value * 3 + getRandom().nextInt(3);
							getCity().setTile(x, y, (char) (HOUSE + houseNumber));
						}
					}
				}
			}

			adjustROG(-8);
			return;
		}

		// remove one little house
		adjustROG(-1);
		int z = 0;

		for (int x = getXpos() - 1; x <= getXpos() + 1; x++) {
			for (int y = getYpos() - 1; y <= getYpos() + 1; y++) {
				if (getCity().testBounds(x, y)) {
					int loc = getCity().getMap()[y][x] & LOMASK;
					if (loc >= LHTHR && loc <= HHTHR) { //little house
						getCity().setTile(x, y, (char) (border[z] + RESCLR - 4));
						return;
					}
				}
				z++;
			}
		}
	}

	/**
	 * Evaluates the zone value of the current commercial zone location.
	 *
	 * @return an integer between -3000 and 3000
	 * Same meaning as evalResidential.
	 */
	private int evalCommercial(int traf)
	{
		if (traf < 0)
			return -3000;

		return getCity().getComRate()[getYpos() / 8][getXpos() / 8];
	}

	/**
	 * Evaluates the zone value of the current residential zone location.
	 *
	 * @return an integer between -3000 and 3000. The higher the
	 * number, the more likely the zone is to GROW; the lower the
	 * number, the more likely the zone is to SHRINK.
	 */
	private int evalResidential(int traf)
	{
		if (traf < 0)
			return -3000;

		int value = getCity().getLandValue(getXpos(), getYpos());
		value -= getCity().getPollutionMem()[getYpos() / 2][getXpos() / 2];

		if (value < 0)
			value = 0;    //cap at 0
		else
			value *= 32;

		if (value > 6000)
			value = 6000; //cap at 6000

		return value - 3000;
	}

	/**
	 * Gets the land-value class (0-3) for the current
	 * residential or commercial zone location.
	 *
	 * @return integer from 0 to 3, 0 is the lowest-valued
	 * zone, and 3 is the highest-valued zone.
	 */
	private int getCRValue()
	{
		int lval = getCity().getLandValue(getXpos(), getYpos());
		lval -= getCity().getPollutionMem()[getYpos() / 2][getXpos() / 2];

		if (lval < 30)
			return 0;

		if (lval < 80)
			return 1;

		if (lval < 150)
			return 2;

		return 3;
	}

	/**
	 * Record a zone's population change to the rate-of-growth
	 * map.
	 * An adjustment of +/- 1 corresponds to one little house.
	 * An adjustment of +/- 8 corresponds to a full-size zone.
	 *
	 * @param amount the positive or negative adjustment to record.
	 */
	private void adjustROG(int amount)
	{
		getCity().getRateOGMem()[getYpos() / 8][getXpos() / 8] += 4 * amount;
	}

	/**
	 * Place tiles for a stadium (full or empty).
	 *
	 * @param zoneCenter either STADIUM or FULLSTADIUM
	 */
	private void drawStadium(int zoneCenter)
	{
		int zoneBase = zoneCenter - 1 - 4;

		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				getCity().setTile(getXpos() - 1 + x, getYpos() - 1 + y, (char) zoneBase);
				zoneBase++;
			}
		}
		getCity().setTilePower(getXpos(), getYpos(), true);
	}

	/**
	 * @return 1 if traffic "passed", 0 if traffic "failed", -1 if no roads found
	 */
	private int makeTraffic(ZoneType zoneType)
	{
		traffic.setMapX(getXpos());
		traffic.setMapY(getYpos());
		traffic.setSourceZone(zoneType);
		return traffic.makeTraffic();
	}

}
