// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Contains the code for performing a city evaluation.
 */
public class CityEval
{
	private static final CityProblem[] NO_CITY_PROBLEMS = new CityProblem[0];

	private final Micropolis engine;

	private final Random random;

	/**
	 * Score for various problems.
	 */
	private final Map<CityProblem, Integer> problemTable = new EnumMap<>(CityProblem.class);

	private int cityYes;

	private int cityNo;

	private int cityAssValue;

	private int cityScore;

	private int deltaCityScore;

	private int cityPop;

	private int deltaCityPop;

	private int cityClass; // 0..5

	private CityProblem[] problemOrder = NO_CITY_PROBLEMS;

	private EnumMap<CityProblem, Integer> problemVotes = new EnumMap<>(CityProblem.class);

	public CityEval(Micropolis engine)
	{
		this.engine = engine;
		random = engine.getRandom();

		assert random != null;
	}

	private static double clamp(double x)
	{
		return Math.max(0, Math.min(1000, x));
	}

	/**
	 * Perform an evaluation.
	 */
	void cityEvaluation()
	{
		if (engine.getTotalPop() == 0) {
			evalInit();
		} else {
			calculateAssValue();
			doPopNum();
			doProblems();
			calculateScore();
			doVotes();
		}
		engine.fireEvaluationChanged();
	}

	/**
	 * Evaluate an empty city.
	 */
	private void evalInit()
	{
		cityYes = 0;
		cityNo = 0;
		cityAssValue = 0;
		cityClass = 0;
		cityScore = 500;
		deltaCityScore = 0;
		problemVotes.clear();
		problemOrder = NO_CITY_PROBLEMS;
	}

	private void calculateAssValue()
	{
		int z = 0;
		z += engine.getRoadTotal() * 5;
		z += engine.getRailTotal() * 10;
		z += engine.getPoliceCount() * 1000;
		z += engine.getFireStationCount() * 1000;
		z += engine.getHospitalCount() * 400;
		z += engine.getStadiumCount() * 3000;
		z += engine.getSeaportCount() * 5000;
		z += engine.getAirportCount() * 10000;
		z += engine.getCoalCount() * 3000;
		z += engine.getNuclearCount() * 6000;
		cityAssValue = z * 1000;
	}

	private void doPopNum()
	{
		int oldCityPop = cityPop;
		cityPop = engine.getCityPopulation();
		deltaCityPop = cityPop - oldCityPop;

		cityClass =
				cityPop > 500000 ? 5 :    //megalopolis
						cityPop > 100000 ? 4 :    //metropolis
								cityPop > 50000 ? 3 :     //capital
										cityPop > 10000 ? 2 :     //city
												cityPop > 2000 ? 1 :      //town
														0;                  //village
	}

	private void doProblems()
	{
		problemTable.clear();
		problemTable.put(CityProblem.CRIME, engine.getCrimeAverage());
		problemTable.put(CityProblem.POLLUTION, engine.getPollutionAverage());
		problemTable.put(CityProblem.HOUSING, (int) Math.round(engine.getLandValueAverage() * 0.7));
		problemTable.put(CityProblem.TAXES, engine.getCityTax() * 10);
		problemTable.put(CityProblem.TRAFFIC, averageTrf());
		problemTable.put(CityProblem.UNEMPLOYMENT, getUnemployment());
		problemTable.put(CityProblem.FIRE, getFire());

		problemVotes = voteProblems(problemTable);

		CityProblem[] probOrder = CityProblem.values();
		Arrays.sort(probOrder, (a, b) -> -problemVotes.get(a).compareTo(problemVotes.get(b)));

		int c = 0;
		while (c < probOrder.length &&
				problemVotes.get(probOrder[c]) != 0 &&
				c < 4)
			c++;

		problemOrder = new CityProblem[c];
		System.arraycopy(probOrder, 0, problemOrder, 0, c);
	}

	private EnumMap<CityProblem, Integer> voteProblems(Map<CityProblem, Integer> probTab)
	{
		CityProblem[] pp = CityProblem.values();
		int[] votes = new int[pp.length];

		int countVotes = 0;
		for (int i = 0; i < 600; i++) {
			if (random.nextInt(301) < probTab.get(pp[i % pp.length])) {
				votes[i % pp.length]++;
				countVotes++;
				if (countVotes >= 100)
					break;
			}
		}

		EnumMap<CityProblem, Integer> rv = new EnumMap<>(CityProblem.class);
		for (int i = 0; i < pp.length; i++) {
			rv.put(pp[i], votes[i]);
		}
		return rv;
	}

	private int averageTrf()
	{
		int count = 1;
		int total = 0;

		for (int y = 0; y < engine.getHeight(); y++) {
			for (int x = 0; x < engine.getWidth(); x++) {
				// only consider tiles that have nonzero landvalue
				if (engine.getLandValue(x, y) != 0) {
					total += engine.getTrafficDensity(x, y);
					count++;
				}
			}
		}

		engine.setTrafficAverage((int) Math.round((double) total / count * 2.4));
		return engine.getTrafficAverage();
	}

	private int getUnemployment()
	{
		int b = (engine.getComPop() + engine.getIndPop()) * 8;
		if (b == 0)
			return 0;

		double r = (double) engine.getResPop() / b;
		b = (int) Math.floor((r - 1.0) * 255);
		if (b > 255) {
			b = 255;
		}
		return b;
	}

	private int getFire()
	{
		int z = engine.getFirePop() * 5;
		return Math.min(255, z);
	}

	private void calculateScore()
	{
		int oldCityScore = cityScore;

		int x = 0;
		for (Integer z : problemTable.values()) {
			x += z;
		}

		x /= 3;
		x = Math.min(256, x);

		double z = clamp((256 - x) * 4);

		if (engine.isResCap()) {
			z = 0.85 * z;
		}
		if (engine.isComCap()) {
			z = 0.85 * z;
		}
		if (engine.isIndCap()) {
			z = 0.85 * z;
		}
		if (engine.getRoadEffect() < 32) {
			z -= 32 - engine.getRoadEffect();
		}
		if (engine.getPoliceEffect() < 1000) {
			z *= 0.9 + engine.getPoliceEffect() / 10000.1;
		}
		if (engine.getFireEffect() < 1000) {
			z *= 0.9 + engine.getFireEffect() / 10000.1;
		}
		if (engine.getResValve() < -1000) {
			z *= 0.85;
		}
		if (engine.getComValve() < -1000) {
			z *= 0.85;
		}
		if (engine.getIndValve() < -1000) {
			z *= 0.85;
		}

		double sm = 1.0;
		if (cityPop != 0 || deltaCityPop != 0) {
			if (deltaCityPop != cityPop) {
				if (deltaCityPop > 0) {
					sm = (double) deltaCityPop / cityPop + 1.0;
				} else if (deltaCityPop < 0) {
					sm = 0.95 + (double) deltaCityPop / (cityPop - deltaCityPop);
				}
			}
		}
		z *= sm;
		z -= getFire();
		z -= engine.getCityTax();

		int tm = engine.getUnpoweredZoneCount() + engine.getPoweredZoneCount();
		sm = tm != 0 ? (double) engine.getPoweredZoneCount() / tm : 1.0;
		z *= sm;

		z = clamp(z);

		cityScore = (int) Math.round((cityScore + z) / 2.0);
		deltaCityScore = cityScore - oldCityScore;
	}

	private void doVotes()
	{
		cityYes = cityNo = 0;
		for (int i = 0; i < 100; i++) {
			if (random.nextInt(1001) < cityScore) {
				cityYes++;
			} else {
				cityNo++;
			}
		}
	}

	/**
	 * Percentage of population "approving" the mayor. Derived from cityScore.
	 */
	public int getCityYes()
	{
		return cityYes;
	}

	/**
	 * Percentage of population "disapproving" the mayor. Derived from cityScore.
	 */
	public int getCityNo()
	{
		return cityNo;
	}

	/**
	 * City assessment value.
	 */
	public int getCityAssValue()
	{
		return cityAssValue;
	}

	/**
	 * Player's score, 0-1000.
	 */
	public int getCityScore()
	{
		return cityScore;
	}

	public void setCityScore(int cityScore)
	{
		this.cityScore = cityScore;
	}

	/**
	 * Change in cityScore since last evaluation.
	 */
	public int getDeltaCityScore()
	{
		return deltaCityScore;
	}

	/**
	 * City population as of current evaluation.
	 */
	public int getCityPop()
	{
		return cityPop;
	}

	/**
	 * Change in cityPopulation since last evaluation.
	 */
	public int getDeltaCityPop()
	{
		return deltaCityPop;
	}

	/**
	 * Classification of city size. 0==village, 1==town, etc.
	 */
	public int getCityClass()
	{
		return cityClass;
	}

	public void setCityClass(int cityClass)
	{
		this.cityClass = cityClass;
	}

	/**
	 * City's top 4 (or fewer) problems as reported by citizens.
	 */
	public CityProblem[] getProblemOrder()
	{
		return problemOrder;
	}

	/**
	 * Number of votes given for the various problems identified by problemOrder[].
	 */
	public EnumMap<CityProblem, Integer> getProblemVotes()
	{
		return problemVotes;
	}

}
