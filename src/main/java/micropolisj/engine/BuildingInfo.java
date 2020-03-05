package micropolisj.engine;

public class BuildingInfo
{
    private int width;
    private int height;
    private short[] members;

	public int getWidth()
	{
		return width;
	}

	public void setWidth(int width)
	{
		this.width = width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(int height)
	{
		this.height = height;
	}

	public short[] getMembers()
	{
		return members;
	}

	public void setMembers(short[] members)
	{
		this.members = members;
	}
}
