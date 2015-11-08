package com.helderman.spacetunnel;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

// TODO: changing orientation - save instance state
// TODO: rotate ship along with orientation

public class Main extends Activity implements Runnable
{
	private final long timeFrame = 40;
	private final long timeMinDelay = 5;

	private volatile boolean mAlive = false;
	private volatile boolean mRun = false;

	private AnimationSurfaceView mView;
	private Thread mThread = null;

	@Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mView = (AnimationSurfaceView)findViewById(R.id.Surface);

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        mView.nHiScore = settings.getLong("HiScore", 0);

        mAlive = true;
		mThread = new Thread(this);
		mThread.start();
    }

	@Override
	protected void onStop()
	{
		super.onStop();

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong("HiScore", mView.nHiScore);
		editor.commit();
	}

	@Override
	protected void onDestroy()
	{
		mAlive = false;
		while (mThread != null)
		{
			try
			{
				mThread.join();
				mThread = null;
			}
			catch (InterruptedException e)
			{
				// ignore
			}
		}
		super.onDestroy();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mRun = true;
	}

	@Override
	protected void onPause()
	{
		mRun = false;
		super.onPause();
	}

	@Override
	public void run()
	{
		while (mAlive)
		{
			if (mRun)
			{
				long timeStart = System.currentTimeMillis();
				mView.step(timeStart);
				long timeLeft = Math.max(timeMinDelay, timeFrame + timeStart - System.currentTimeMillis());
				if (timeLeft > 0)
				{
					try
					{
						Thread.sleep(timeLeft);
					}
					catch (InterruptedException e)
					{
						// ignore
					}
				}
			}
		}
	}
}
