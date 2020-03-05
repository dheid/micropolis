package micropolisj.engine;

public class FinancialHistory
{
	private int cityTime;

	private int totalFunds;

	private int taxIncome;

	private int operatingExpenses;

	public int getCityTime()
	{
		return cityTime;
	}

	public void setCityTime(int cityTime)
	{
		this.cityTime = cityTime;
	}

	public int getTotalFunds()
	{
		return totalFunds;
	}

	public void setTotalFunds(int totalFunds)
	{
		this.totalFunds = totalFunds;
	}

	public int getTaxIncome()
	{
		return taxIncome;
	}

	public void setTaxIncome(int taxIncome)
	{
		this.taxIncome = taxIncome;
	}

	public int getOperatingExpenses()
	{
		return operatingExpenses;
	}

	public void setOperatingExpenses(int operatingExpenses)
	{
		this.operatingExpenses = operatingExpenses;
	}
}
