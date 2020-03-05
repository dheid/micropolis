package micropolisj.engine;

public class History
{
	private final int[] res = new int[240];

	private final int[] com = new int[240];

	private final int[] ind = new int[240];

	private final int[] money = new int[240];

	private final int[] pollution = new int[240];

	private final int[] crime = new int[240];

	private int cityTime;

	public int[] getRes()
	{
		return res;
	}

	public int[] getCom()
	{
		return com;
	}

	public int[] getInd()
	{
		return ind;
	}

	public int[] getMoney()
	{
		return money;
	}

	public int[] getPollution()
	{
		return pollution;
	}

	public int[] getCrime()
	{
		return crime;
	}

	public int getCityTime()
	{
		return cityTime;
	}

	public void setCityTime(int cityTime)
	{
		this.cityTime = cityTime;
	}
}
