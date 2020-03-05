package micropolisj.engine;

public class SoundInfo
{
    private final int x;
    private final int y;
    private final Sound sound;

	public SoundInfo(int x, int y, Sound sound)
    {
        this.x = x;
        this.y = y;
        this.sound = sound;
    }

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public Sound getSound()
	{
		return sound;
	}
}
