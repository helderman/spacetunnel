package com.helderman.spacetunnel;

import java.util.Random;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

public class AnimationSurfaceView extends SurfaceView implements Callback
{
	private static final int nRingDuration = 500;	// distance in milliseconds between 2 rings
	private static final int nNumberOfRings = 24;	// how far you can see in the distance

	private SurfaceHolder objSurfaceHolder;

	private Drawable imgShip;

	private int nWidthView;
	private int nHeightView;
	private int nWidthRing1 = 0;	// must save
	private int nRadiusRing1;
	private int nThickRing1;

	private volatile boolean bRun = false;

	// touch screen
	private volatile int nStartX = 0;
	private volatile int nStartY = 0;
	private volatile int nStopX  = 0;
	private volatile int nStopY  = 0;

	// trackball
	private volatile int nTrackX = 0;
	private volatile int nTrackY = 0;

	// keyboard
	private static final int dirU = 0;	// up
	private static final int dirD = 1;	// down
	private static final int dirL = 2;	// left
	private static final int dirR = 3;	// right

	private volatile long[] msKeyState = {0, 0, 0, 0};
	private           int[] msDuration = {0, 0, 0, 0};

	private Paint paintStar1;
	private Paint paintStar2;
	private Paint paintRing1;
	private Paint paintRing2;
	private Paint paintEngine;
	private Paint paintDebris;
	private Paint paintTitle1;
	private Paint paintTitle2;
	private Paint paintTextCenter;
	private Paint paintTextCaption;
	private Paint paintTextNumber;

	private volatile int nState;	// must save

	private String[] asStage = {null, null, null, null, null};
	private static final String[] asGameOver = {"GAME", "OVER"};

	private long nShipDirX;			// must save
	private long nShipDirY;			// must save

	private long nShipPosX;			// must save
	private long nShipPosY;			// must save

	private long nTimePrev;
	private long nRingPrev;
	private long nRing0;			// must save difference from nRingCurr
	private long nTimeExplode;		// must save

	private long nRingBendX;		// must save
	private long nRingBendY;		// must save

	private long nRingDirX;			// must save
	private long nRingDirY;			// must save

	private long[] aRingPosX;		// must save
	private long[] aRingPosY;		// must save

	private Rect   rectRing;
	private Rect   rectShip;

	private Random rndBends;
	private Random rndNoise;

	private int    nStage;			// must save
	private int    nBend;			// must save - how sharp the turns are
	private long   nScore;			// must save
	private String sScore, sHiScore;

	public  long   nHiScore;

	public AnimationSurfaceView(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		objSurfaceHolder = getHolder();
		objSurfaceHolder.addCallback(this);

		imgShip = context.getResources().getDrawable(R.drawable.ship);

		rectRing = new Rect();
		rectShip = new Rect();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		synchronized (holder)
		{
			nWidthView  = width;
			nHeightView = height;

			if (nWidthRing1 == 0)
			{
				nWidthRing1 = (width + height) / 2;
				nRadiusRing1 = nRingDuration * nWidthRing1 / 2;
				nThickRing1 = 1 + nWidthRing1 / 32;

				rectShip.set(
					width  / 2 - 160 * nWidthRing1 / 1600,
					height / 2 - 108 * nWidthRing1 / 1600,
					width  / 2 + 160 * nWidthRing1 / 1600,
					height / 2 +  31 * nWidthRing1 / 1600);

				paintStar1 = new Paint();
				paintStar1.setStyle(Paint.Style.FILL);
				paintStar1.setColor(Color.rgb(255, 255, 127));
				paintStar1.setMaskFilter(new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL));

				paintStar2 = new Paint();
				paintStar2.setStyle(Paint.Style.FILL);
				paintStar2.setColor(Color.WHITE);
				paintStar2.setMaskFilter(new BlurMaskFilter(1, BlurMaskFilter.Blur.NORMAL));

				paintRing1 = new Paint();
				paintRing1.setStyle(Paint.Style.STROKE);
				paintRing1.setColor(Color.rgb(0, 255, 255));
				paintRing1.setMaskFilter(new BlurMaskFilter(6, BlurMaskFilter.Blur.NORMAL));

				paintRing2 = new Paint();
				paintRing2.setStyle(Paint.Style.STROKE);
				paintRing2.setColor(Color.rgb(127, 255, 255));

				paintEngine = new Paint();
				paintEngine.setStyle(Paint.Style.FILL);
				paintEngine.setMaskFilter(new BlurMaskFilter(2, BlurMaskFilter.Blur.NORMAL));

				paintDebris = new Paint();
				paintDebris.setStyle(Paint.Style.FILL);
				paintDebris.setMaskFilter(new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL));

				paintTitle1 = new Paint();
				paintTitle1.setColor(Color.WHITE);
				paintTitle1.setTextAlign(Align.CENTER);
				paintTitle1.setTextSize(nWidthRing1 / 20);
				paintTitle1.setAntiAlias(true);

				paintTitle2 = new Paint();
				paintTitle2.setColor(Color.WHITE);
				paintTitle2.setTextAlign(Align.CENTER);
				paintTitle2.setTextSize(nWidthRing1 / 10);
				paintTitle2.setTypeface(Typeface.DEFAULT_BOLD);
				paintTitle2.setAntiAlias(true);

				paintTextCenter = new Paint();
				paintTextCenter.setColor(Color.WHITE);
				paintTextCenter.setTextAlign(Align.CENTER);
				paintTextCenter.setTextSize(24);
				paintTextCenter.setAntiAlias(true);

				paintTextCaption = new Paint();
				paintTextCaption.setColor(Color.LTGRAY);
				paintTextCaption.setTextAlign(Align.LEFT);
				paintTextCaption.setTextSize(18);
				paintTextCaption.setAntiAlias(true);

				paintTextNumber = new Paint();
				paintTextNumber.setColor(Color.WHITE);
				paintTextNumber.setTextAlign(Align.RIGHT);
				paintTextNumber.setTextSize(36);
				paintTextNumber.setAntiAlias(true);

				aRingPosX = new long[nNumberOfRings];
				aRingPosY = new long[nNumberOfRings];

				rndBends = new Random();
				rndNoise = new Random(0);

				nState = 0;
				nScore = 0;
				sScore = "" + nScore;
				sHiScore = "" + nHiScore;
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		bRun = true;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		bRun = false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
		case KeyEvent.KEYCODE_DPAD_CENTER:
			if (nState == 1) nState = 2;
			break;
		case KeyEvent.KEYCODE_DPAD_UP:		// 19
		case KeyEvent.KEYCODE_DPAD_DOWN:	// 20
		case KeyEvent.KEYCODE_DPAD_LEFT:	// 21
		case KeyEvent.KEYCODE_DPAD_RIGHT:	// 22
			int dir = keyCode - KeyEvent.KEYCODE_DPAD_UP;
			if (msKeyState[dir] >= 0)
			{
				msKeyState[dir] -= System.currentTimeMillis();
			}
			break;
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
		case KeyEvent.KEYCODE_DPAD_UP:		// 19
		case KeyEvent.KEYCODE_DPAD_DOWN:	// 20
		case KeyEvent.KEYCODE_DPAD_LEFT:	// 21
		case KeyEvent.KEYCODE_DPAD_RIGHT:	// 22
			int dir = keyCode - KeyEvent.KEYCODE_DPAD_UP;
			if (msKeyState[dir] < 0)
			{
				msKeyState[dir] += System.currentTimeMillis();
			}
			break;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent e)
	{
		int x = (int)e.getX();
		int y = (int)e.getY();

		switch (e.getAction())
		{
		case MotionEvent.ACTION_DOWN:
			if (nState == 1) nState = 2;
			nStartX += x - nStopX;
			nStartY += y - nStopY;
			// fall through
		case MotionEvent.ACTION_MOVE:
		case MotionEvent.ACTION_UP:
			nStopX = x;
			nStopY = y;
			break;
		}
		return true;	// important!
	}

	@Override
	public boolean onTrackballEvent(MotionEvent e)
	{
		switch (e.getAction())
		{
		case MotionEvent.ACTION_MOVE:
			nTrackX += e.getX() * 240;
			nTrackY += e.getY() * 240;
			break;
		}
		return true;
	}

	private void checkpoint()
	{
		// touch
		int x = nStopX - nStartX;
		int y = nStopY - nStartY;
		nStartX += x;
		nStartY += y;
		if (nState == 3)
		{
			nShipDirX += x;
			nShipDirY += y;
		}
		// trackball
		x = nTrackX;
		y = nTrackY;
		nTrackX -= x;
		nTrackY -= y;
		// keyboard
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
		x += msDuration[dirR] - msDuration[dirL];
		y += msDuration[dirD] - msDuration[dirU];
		if (nState == 3)
		{
			nShipDirX += x * nWidthRing1 / 1600;
			nShipDirY += y * nWidthRing1 / 1600;
		}
	}

	public void step(long timeStart)
	{
		if (bRun)
		{
			Canvas c = null;
			try
			{
				c = objSurfaceHolder.lockCanvas(null);
				synchronized (objSurfaceHolder)
				{
					drawFrame(c, timeStart);
				}
			}
			finally
			{
				if (c != null)
				{
					objSurfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}

	private void drawFrame(Canvas c, long timeStart)
	{
		checkpoint();
		movement(timeStart);

		String[] asText = null;

		c.drawColor(Color.BLACK);

		// starry background

		rndNoise.setSeed(6);	// reproduceable number sequence
		for (int nStar = 0; nStar < 16; nStar++)
		{
			float x = (float)(posMod(getNoise(nWidthView  + 20) - nShipDirX, nWidthView  + 20) - 10);
			float y = (float)(posMod(getNoise(nHeightView + 20) - nShipDirY, nHeightView + 20) - 10);
			c.drawCircle(x, y, 4, paintStar1);
			c.drawCircle(x, y, 2, paintStar2);
		}

		// rings

		int nTimeDepth = (nNumberOfRings + 1) * nRingDuration - (int)(timeStart % nRingDuration);
		for (int nRingDepth = nNumberOfRings - 1; nRingDepth >= 0; nRingDepth--)
		{
			if ((nTimeDepth -= nRingDuration) > nRingDuration / 2)
			{
				int nPixelsPosX = (int)((aRingPosX[nRingDepth] - nShipPosX - nShipDirX * (nTimeDepth - nRingDuration)) / nTimeDepth) + nWidthView  / 2;
				int nPixelsPosY = (int)((aRingPosY[nRingDepth] - nShipPosY - nShipDirY * (nTimeDepth - nRingDuration)) / nTimeDepth) + nHeightView / 2;
				int nPixelsRadius = nRadiusRing1 / nTimeDepth;

				rectRing.left   = nPixelsPosX - nPixelsRadius;
				rectRing.right  = nPixelsPosX + nPixelsRadius;
				rectRing.top    = nPixelsPosY - nPixelsRadius;
				rectRing.bottom = nPixelsPosY + nPixelsRadius;

				int nPixelsThick = 1 + nThickRing1 * nRingDuration / nTimeDepth;

				int nBright = nRingDepth == 0
					? nNumberOfRings * (2 * nTimeDepth - nRingDuration)
					: (nNumberOfRings + 1) * nRingDuration - nTimeDepth;

				int nAlpha = (int)(((long)nBright * nBright * 255) / ((long)nRingDuration * nRingDuration * nNumberOfRings * nNumberOfRings));

				paintRing1.setStrokeWidth((float)(nPixelsThick + 2));
				paintRing1.setAlpha(nAlpha);
				c.drawRect(rectRing, paintRing1);

				paintRing2.setStrokeWidth((float)nPixelsThick);
				paintRing2.setAlpha(nAlpha);
				c.drawRect(rectRing, paintRing2);
			}
		}

		// ship & explosion

		switch (nState) 
		{
		case 1:
		case 2:
			c.drawText("S  P  A  C  E",
				nWidthView / 2,
				(nHeightView - paintTitle2.getTextSize()) / 2 - paintTitle1.descent(),
				paintTitle1);
			c.drawText("TUNNEL",
				nWidthView / 2,
				(nHeightView - paintTitle2.ascent() - paintTitle2.descent()) / 2,
				paintTitle2);
			c.drawText("by",
				nWidthView / 2,
				(nHeightView + paintTitle2.getTextSize()) / 2 + paintTitle1.getTextSize() * 2 - 2,
				paintTitle1);
			c.drawText("Ruud Helderman",
				nWidthView / 2,
				(nHeightView + paintTitle2.getTextSize()) / 2 + paintTitle1.getTextSize() * 3,
				paintTitle1);
			break;
		case 3:
			// ship
			int nEngine = (int)((~timeStart) & 255);
			paintEngine.setColor(Color.rgb(nEngine, nEngine, nEngine));
			c.drawCircle(nWidthView / 2, nHeightView / 2, nWidthRing1 / 70, paintEngine);
			imgShip.setBounds(rectShip);
			imgShip.draw(c);
			if (nRingPrev >= nRing0 + 2 && nRingPrev < nRing0 + 10)
			{
				asText = asStage;
				asText[0] = "Stage " + nStage;
			}
			break;
		case 4:
			// explosion
			long nExplSize = timeStart - nTimeExplode + 100;
			if (nExplSize < 2000)
			{
				long nExplX = 0;
				long nExplY = 4000;
				rndNoise.setSeed(0);	// reproduceable number sequence
				for (int nCount = 0; nCount < 50; nCount++)
				{
					// Particle color will fade
					// from white through yellow and red to black.
					int nExplR = 750 + nCount * 25 - (int)nExplSize;
					int nExplG = nExplR - 250;
					int nExplB = nExplG - 250;

					int nCos = -4;
					int nSin = 19 + 3 * getNoise(3);

					long nNewX = (nCos * nExplX - nSin * nExplY) / 23;
					long nNewY = (nSin * nExplX + nCos * nExplY) / 23;

					nExplX = nNewX;
					nExplY = nNewY;

					paintDebris.setColor(Color.rgb(
						nExplR < 0 ? 0 : nExplR > 255 ? 255 : nExplR,
						nExplG < 0 ? 0 : nExplG > 255 ? 255 : nExplG,
						nExplB < 0 ? 0 : nExplB > 255 ? 255 : nExplB));

					c.drawCircle(
						(float)(nWidthView  / 2 + (nExplX * nExplSize * nWidthRing1) / 12000000),
						(float)(nHeightView / 2 + (nExplY * nExplSize * nWidthRing1) / 12000000),
						10, paintDebris);
				}
			}
			else if (nExplSize < 4000)
			{
				asText = asGameOver;
			}
			else
			{
				nState = 0;	// back to menu
				//SaveHiScore();
			}
			break;
		}

		// score

		c.drawText("Score:", 0, -paintTextCaption.ascent(), paintTextCaption);
		c.drawText("  High:", nWidthView / 2, -paintTextCaption.ascent(), paintTextCaption);

		c.drawText(sScore, nWidthView / 2, -paintTextNumber.ascent(), paintTextNumber);
		c.drawText(sHiScore, nWidthView, -paintTextNumber.ascent(), paintTextNumber);

		// text

		if (asText != null)
		{
			int nDistY = (int)paintTextCenter.getTextSize();
			int nPosY = (nHeightView - nDistY * asText.length) / 2 - (int)paintTextCenter.ascent();

			for (int nCount = 0; nCount < asText.length; nCount++)
			{
				if (asText[nCount] != null)
				{
					c.drawText(asText[nCount], nWidthView / 2, nPosY, paintTextCenter);
				}
				nPosY += nDistY;
			}
		}
	}

	private void movement(long nTimeCurr)
	{
		long nRingCurr = nTimeCurr / nRingDuration;

		switch (nState)
		{
		case 0:
			nState = 1;
			initPosAndDir();
			// fall through
		case 1:
			// To make the menu look a little less boring,
			// show the animated tunnel with the camera
			// moving around and through it.
			nShipPosX = Math.abs((nTimeCurr * 41) % 2048000 - 1024000) - 512000;
			nShipPosY = Math.abs((nTimeCurr * 47) % 2048000 - 1024000) - 512000;
			break;
		case 2:
			nState = 3;
			nStage = 1;
			nBend = nRadiusRing1 / 6;
			nScore = 0;
			sScore = "0";
			nRing0 = nRingCurr;
			initPosAndDir();
			// fall through
		case 3:
		case 4:
			// Did we pass through a ring?
			// (Or more than one, in case of a hick-up.)
			while (nRingPrev < nRingCurr)
			{
				nRingPrev++;

				long nDistanceToRing = nRingPrev * nRingDuration - nTimePrev;
				nShipPosX += nShipDirX * nDistanceToRing;
				nShipPosY += nShipDirY * nDistanceToRing;
				nTimePrev += nDistanceToRing;

				long nDiffX = (nShipPosX - aRingPosX[1]) / nRadiusRing1;
				long nDiffY = (nShipPosY - aRingPosY[1]) / nRadiusRing1;

				// Detect collision with the sides of the tunnel.
				if (nState == 3 && (nDiffX != 0 || nDiffY != 0))
				{
					nState = 4;
					nTimeExplode = nTimeCurr;
				}

				System.arraycopy(aRingPosX, 1, aRingPosX, 0, aRingPosX.length - 1);
				System.arraycopy(aRingPosY, 1, aRingPosY, 0, aRingPosY.length - 1);

				/***
				// Save the hickups for the quiet times.
				if (nRingPrev == nRing0 + 8)
				{
					System.gc();
				}
				***/

				// After 100 rings, the stage ends.
				// After a short distance without bends,
				// the next stage starts.
				// TODO: really 100 rings?
				if (nRingPrev >= nRing0 + 100)
				{
					nStage++;
					nBend += nRadiusRing1 / 15;
					nRing0 = nRingPrev + nNumberOfRings;
					nRingBendX = nRingBendY = 0;
				}

				// Every 6 rings, the bend may change.
				if (nRingPrev >= nRing0 && nRingPrev % 6 == 0)
				{
					switch (Math.abs(rndBends.nextInt()) % 6)
					{
					case 0: nRingBendX =      0; nRingBendY =      0; break;
					case 1: nRingBendX =  nBend; nRingBendY =      0; break;
					case 2: nRingBendX = -nBend; nRingBendY =      0; break;
					case 3: nRingBendX =      0; nRingBendY =  nBend; break;
					case 4: nRingBendX =      0; nRingBendY = -nBend; break;
					}
				}

				nRingDirX += nRingBendX;
				nRingDirY += nRingBendY;

				aRingPosX[aRingPosX.length - 1] += nRingDirX;
				aRingPosY[aRingPosY.length - 1] += nRingDirY;

				if (nState == 3)
				{
					sScore = "" + (++nScore);
					if (nScore > nHiScore)
					{
						nHiScore = nScore;
						sHiScore = sScore;
					}
				}
			}
			nShipPosX += nShipDirX * (nTimeCurr - nTimePrev);
			nShipPosY += nShipDirY * (nTimeCurr - nTimePrev);
		}
		nTimePrev = nTimeCurr;
		nRingPrev = nRingCurr;
	}

	private void initPosAndDir()
	{
		nShipPosX  = nShipPosY  = 0;
		nShipDirX  = nShipDirY  = 0;
		nRingDirX  = nRingDirY  = 0;
		nRingBendX = nRingBendY = 0;

		for (int nDepth = 0; nDepth < aRingPosX.length; nDepth++)
		{
			aRingPosX[nDepth] = aRingPosY[nDepth] = 0;
		}
	}

	private int getNoise(int nRange)
	{
		return Math.abs(rndNoise.nextInt()) % nRange;
	}

	private long posMod(long nEnumerator, long nDenominator)
	{
		long nMod = nEnumerator % nDenominator;
		return nMod >= 0 ? nMod : nMod + nDenominator;
	}
}
