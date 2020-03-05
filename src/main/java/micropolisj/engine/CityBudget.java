// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.engine;

public class CityBudget
{

	private int totalFunds;

	private int taxFund;

	private int roadFundEscrow;

	private int fireFundEscrow;

	private int policeFundEscrow;

	/**
	 * The amount of cash on hand.
	 */
	public int getTotalFunds()
	{
		return totalFunds;
	}

	public void setTotalFunds(int totalFunds)
	{
		this.totalFunds = totalFunds;
	}

	/**
	 * Amount of taxes collected so far in the current financial
	 * period (in 1/TAXFREQ's).
	 */
	public int getTaxFund()
	{
		return taxFund;
	}

	public void setTaxFund(int taxFund)
	{
		this.taxFund = taxFund;
	}

	/**
	 * Amount of prepaid road maintenance (in 1/TAXFREQ's).
	 */
	public int getRoadFundEscrow()
	{
		return roadFundEscrow;
	}

	public void setRoadFundEscrow(int roadFundEscrow)
	{
		this.roadFundEscrow = roadFundEscrow;
	}

	/**
	 * Amount of prepaid fire station maintenance (in 1/TAXFREQ's).
	 */
	public int getFireFundEscrow()
	{
		return fireFundEscrow;
	}

	public void setFireFundEscrow(int fireFundEscrow)
	{
		this.fireFundEscrow = fireFundEscrow;
	}

	/**
	 * Amount of prepaid police station maintenance (in 1/TAXFREQ's).
	 */
	public int getPoliceFundEscrow()
	{
		return policeFundEscrow;
	}

	public void setPoliceFundEscrow(int policeFundEscrow)
	{
		this.policeFundEscrow = policeFundEscrow;
	}
}
