// K.java - keyboard controls

public class K
{
	public static boolean Pilot = true;

	public static final int dirL = 0;	// left
	public static final int dirU = 1;	// up
	public static final int dirR = 2;	// right
	public static final int dirD = 3;	// down

	public static int x;
	public static int y;

	private static long[] msKeyState = {0, 0, 0, 0};
	private static int[]  msDuration = {0, 0, 0, 0};

	public static void KeyPressed(int dir)
	{
		if (msKeyState[dir] >= 0)
		{
			msKeyState[dir] -= System.currentTimeMillis();
		}
	}

	public static void KeyReleased(int dir)
	{
		if (msKeyState[dir] < 0)
		{
			msKeyState[dir] += System.currentTimeMillis();
		}
	}

	public static void Checkpoint()
	{
		for (int dir = 0; dir < msKeyState.length; dir++)
		{
			long ms = msKeyState[dir];
			if (ms < 0)
			{
				ms += System.currentTimeMillis();
			}
			msKeyState[dir] -= ms;
			msDuration[dir] = (int)ms;
		}
		x = msDuration[dirR] - msDuration[dirL];
		y = msDuration[dirD] - msDuration[dirU];
		if (Pilot)
		{
			y = -y;
		}
	}
}
