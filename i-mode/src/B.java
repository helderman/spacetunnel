// B.java - SpaceTunnel screen

import java.io.*;
import javax.microedition.io.*;
import java.util.Random;
import com.nttdocomo.ui.*;

public class B
	extends Canvas
{
	// nState: simple state machine
	// 0 = going to menu
	// 1 = in menu
	// 2 = going to game
	// 3 = in game
	// 4 = death

	private int     nState;
	private int     nStage;
	private int     nBend;		// how sharp the turns are
	private long    nScore, nHiScore, nSavedHiScore;
	private String  sScore, sHiScore;
	private boolean bExit;
	private boolean bEngine;
	private long    nTimeExplode;
	private Image   oImgShipRear;
	private Font    oFontN;		// normal
	private Font    oFontL;		// large

	private long nShipDirX;
	private long nShipDirY;

	private long nShipPosX;
	private long nShipPosY;

	private long nTimeCurr;
	private long nTimePrev;
	private long nRingCurr;
	private long nRingPrev;
	private long nRing0;

	private long nRingBendX;
	private long nRingBendY;

	private long nRingDirX;
	private long nRingDirY;

	private long[] aRingPosX;
	private long[] aRingPosY;

	private int nRingColorR = 128;
	private int nRingColorG = 256;
	private int nRingColorB = 256;

	private static final long nRingDuration = 500;	// distance in milliseconds between 2 rings
	private static final int  nNumberOfRings = 16;	// how far you can see in the distance
	private static final int  nSensitivity = 100;	// decrease to make the ship's controls more sensitive

	private static final String[] asTitle = {"SPACE", "TUNNEL", null, "by Ruud", "Helderman"};
	private static final String[] asStage = {null, null, null, null};
	private static final String[] asGameOver = {"GAME", "OVER"};

	private static Random rndBends = null;
	private Random rndNoise;

	public B()
	{
		setSoftLabel(Frame.SOFT_KEY_1, "Play");
		setSoftLabel(Frame.SOFT_KEY_2, "Exit");

		aRingPosX = new long[nNumberOfRings];
		aRingPosY = new long[nNumberOfRings];

		if (rndBends == null)
		{
			rndBends = new Random();
		}	
		rndNoise = new Random(0);

		oFontN = Font.getDefaultFont();
		oFontL = Font.getFont(Font.SIZE_LARGE);

		try
		{
			MediaImage oMediaImg = MediaManager.getImage("resource:///ShipRear.gif");
			oMediaImg.use();
			oImgShipRear = oMediaImg.getImage();
		}
		catch (Exception x)
		{
			//System.out.println("Error loading image: " + x);
			oImgShipRear = null;
		}

		nState = 0;
		nScore = nHiScore = nSavedHiScore = 0;
		LoadHiScore();
		sScore = "0";
		sHiScore = "" + nHiScore;
	}

	public void run()
	{
		bExit = false;
		while (!bExit)
		{
			// For better performance on the emulator,
			// call paint() directly instead of repaint().
			paint(getGraphics());
		}
	}

	private void Movement()
	{
		nTimeCurr = System.currentTimeMillis();
		nRingCurr = nTimeCurr / nRingDuration;

		K.Checkpoint();

		switch (nState)
		{
		case 0:
			nState = 1;
			InitPosAndDir();
			// fall through
		case 1:
			// To make the menu look a little less boring,
			// show the animated tunnel with the camera
			// moving around and through it.
			nShipPosX = (Math.abs((nTimeCurr * 64 / 31) % 102400 - 51200) - 25600) * nSensitivity;
			nShipPosY = (Math.abs((nTimeCurr * 64 / 27) % 102400 - 51200) - 25600) * nSensitivity;
			break;
		case 2:
			nState = 3;
			nStage = 1;
			nBend = nSensitivity * 640;
			nScore = 0;
			sScore = "0";
			nRing0 = nRingCurr;
			InitPosAndDir();
			// fall through
		case 3:
			nShipDirX += K.x;
			nShipDirY += K.y;
			// fall through
		case 4:
			// Did we pass through a ring?
			// (Or more than one, in case of a hickup.)
			while (nRingPrev < nRingCurr)
			{
				nRingPrev++;

				long nDistanceToRing = nRingPrev * nRingDuration - nTimePrev;
				nShipPosX += nShipDirX * nDistanceToRing;
				nShipPosY += nShipDirY * nDistanceToRing;
				nTimePrev += nDistanceToRing;

				long nDiffX = (nShipPosX - aRingPosX[0]) / (nRingDuration * nSensitivity * 8);
				long nDiffY = (nShipPosY - aRingPosY[0]) / (nRingDuration * nSensitivity * 8);

				// Detect collision with the sides of the tunnel.
				if (nState == 3 && (nDiffX != 0 || nDiffY != 0))
				{
					nState = 4;
					nTimeExplode = nTimeCurr;
				}

				System.arraycopy(aRingPosX, 1, aRingPosX, 0, aRingPosX.length - 1);
				System.arraycopy(aRingPosY, 1, aRingPosY, 0, aRingPosY.length - 1);

				// Save the hickups for the quiet times.
				if (nRingPrev == nRing0 + 8)
				{
					System.gc();
				}

				// After 100 rings, the stage ends.
				// After a short distance without bends,
				// the next stage starts.
				if (nRingPrev >= nRing0 + 100)
				{
					nStage++;
					nBend += nSensitivity * 256;
					nRing0 = nRingPrev + 16;
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

	public synchronized void paint(Graphics g)
	{
		Movement();

		String[] asText = null;

		g.lock();
		g.setColor(Graphics.getColorOfName(Graphics.BLACK));
		g.fillRect(0, 0, getWidth(), getHeight());

		// starry background

		g.setColor(Graphics.getColorOfName(Graphics.BLUE));
		rndNoise.setSeed(0);	// reproduceable number sequence
		for (int nStar = 0; nStar < 16; nStar++)
		{
			g.fillRect(
				(int)((PosMod(Noise(16 * nSensitivity) - nShipDirX, nSensitivity * 16) * getWidth())  / (nSensitivity * 16)),
				(int)((PosMod(nStar * nSensitivity     - nShipDirY, nSensitivity * 16) * getHeight()) / (nSensitivity * 16)),
				1, 1);
		}

		// rings

		for (int nRingDepth = nNumberOfRings - 1; nRingDepth >= 0; nRingDepth--)
		{
			long nTimeDepth = ((nRingDepth + 2) * nRingDuration) - (nTimeCurr % nRingDuration);

			int nPixelsPosX = (int)((getWidth()  * (aRingPosX[nRingDepth] - nShipPosX - nShipDirX * (nTimeDepth - nRingDuration))) / (nTimeDepth * nSensitivity * 16));
			int nPixelsPosY = (int)((getHeight() * (aRingPosY[nRingDepth] - nShipPosY - nShipDirY * (nTimeDepth - nRingDuration))) / (nTimeDepth * nSensitivity * 16));

			int nPixelsWidth  = (int)((getWidth()  * nRingDuration) / nTimeDepth);
			int nPixelsHeight = (int)((getHeight() * nRingDuration) / nTimeDepth);

			long nBright = (nNumberOfRings + 1) * nRingDuration - nTimeDepth;

			g.setColor(Graphics.getColorOfRGB(
				(int)((nRingColorR * nBright) / (nRingDuration * nNumberOfRings)),
				(int)((nRingColorG * nBright) / (nRingDuration * nNumberOfRings)),
				(int)((nRingColorB * nBright) / (nRingDuration * nNumberOfRings))));

			g.drawRect(
				nPixelsPosX + (getWidth()  - nPixelsWidth)  / 2,
				nPixelsPosY + (getHeight() - nPixelsHeight) / 2,
				nPixelsWidth  - 1,
				nPixelsHeight - 1);
		}

		// ship & explosion

		switch (nState) 
		{
		case 1:
		case 2:
			asText = asTitle;
			break;
		case 3:
			// ship
			if (oImgShipRear != null)
			{
				g.drawImage(oImgShipRear,
					(getWidth()  - oImgShipRear.getWidth())  / 2,
					(getHeight() - oImgShipRear.getHeight()) / 2);

				bEngine = !bEngine;
				if (bEngine)
				{
					g.setColor(Graphics.getColorOfName(Graphics.BLACK));
					g.fillRect(getWidth() / 2 - 2, getHeight() / 2, 3, 3);
				}
			}
			if (nRingPrev >= nRing0 + 2 && nRingPrev < nRing0 + 10)
			{
				asText = asStage;
				asText[0] = "Stage " + nStage;
			}
			break;
		case 4:
			// explosion
			long nExplSize = nTimeCurr - nTimeExplode + 100;
			if (nExplSize < 2000)
			{
				long nExplX = 0;
				long nExplY = 4000;
				rndNoise.setSeed(0);	// reproduceable number sequence
				for (int nCount = 0; nCount < 50; nCount++)
				{
					// Partical color will fade
					// from white through yellow and red to black.
					int nExplR = 750 + nCount * 25 - (int)nExplSize;
					int nExplG = nExplR - 250;
					int nExplB = nExplG - 250;

					int nCos = -4;
					int nSin = 19 + 3 * Noise(3);

					long nNewX = (nCos * nExplX - nSin * nExplY) / 23;
					long nNewY = (nSin * nExplX + nCos * nExplY) / 23;

					nExplX = nNewX;
					nExplY = nNewY;

					g.setColor(Graphics.getColorOfRGB(
						nExplR < 0 ? 0 : nExplR > 255 ? 255 : nExplR,
						nExplG < 0 ? 0 : nExplG > 255 ? 255 : nExplG,
						nExplB < 0 ? 0 : nExplB > 255 ? 255 : nExplB));

					g.fillRect(
						(int)(getWidth()  / 2 - 1 + (nExplX * nExplSize) / 50000),
						(int)(getHeight() / 2 - 1 + (nExplY * nExplSize) / 50000),
						3, 3);
				}
			}
			else if (nExplSize < 4000)
			{
				asText = asGameOver;
			}
			else
			{
				nState = 0;	// back to menu
				SaveHiScore();
			}
			break;
		}

		// score

		g.setFont(oFontN);
		int nPosY = oFontN.getAscent();

		g.setColor(Graphics.getColorOfName(Graphics.SILVER));
		g.drawString("Score:", 0, nPosY);
		g.drawString("  High:", getWidth() / 2, nPosY);

		g.setColor(Graphics.getColorOfName(Graphics.WHITE));
		g.drawString(sScore, getWidth() / 2 - oFontN.stringWidth(sScore), nPosY);
		g.drawString(sHiScore, getWidth() - oFontN.stringWidth(sHiScore), nPosY);

		// text

		if (asText != null)
		{
			g.setFont(oFontL);
			int nDistY = oFontL.getHeight();
			nPosY += (getHeight() - nDistY * asText.length) / 2;

			for (int nCount = 0; nCount < asText.length; nCount++)
			{
				if (asText[nCount] != null)
				{
					int nPosX = (getWidth() - oFontL.stringWidth(asText[nCount])) / 2;
					g.drawString(asText[nCount], nPosX, nPosY);
				}
				nPosY += nDistY;
			}
		}

		g.unlock(true);
	}

	public void processEvent(int type, int param)
	{
		switch (type)
		{
		case Display.KEY_PRESSED_EVENT:
			switch (param)
			{
			case Display.KEY_SOFT2:
				bExit = true;
				break;
			case Display.KEY_SOFT1:
			case Display.KEY_SELECT:
				if (nState == 1)
				{
					nState = 2;	// start the game
				}
				break;
			case Display.KEY_LEFT:
			case Display.KEY_RIGHT:
			case Display.KEY_UP:
			case Display.KEY_DOWN:
				K.KeyPressed(param - 16);
				break;
			}
			break;
		case Display.KEY_RELEASED_EVENT:
			switch (param)
			{
			case Display.KEY_LEFT:
			case Display.KEY_RIGHT:
			case Display.KEY_UP:
			case Display.KEY_DOWN:
				K.KeyReleased(param - 16);
				break;
			}
			break;
		}
	}

	private void InitPosAndDir()
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

	private int Noise(int nRange)
	{
		return Math.abs(rndNoise.nextInt()) % nRange;
	}

	private long PosMod(long nEnumerator, long nDenominator)
	{
		long nMod = nEnumerator % nDenominator;
		if (nMod < 0)
		{
			nMod += nDenominator;
		}
		return nMod;
	}

	private void LoadHiScore()
	{
		DataInputStream s = null;
		try
		{
			s = Connector.openDataInputStream("scratchpad:///0");
			nHiScore = nSavedHiScore = s.readLong();
		}
		catch (Exception x)
		{
		}
		finally
		{
			if (s != null)
			{
				try { s.close(); } catch (Exception x) {}
			}
		}
	}

	private void SaveHiScore()
	{
		if (nHiScore > nSavedHiScore)
		{
			DataOutputStream s = null;
			try
			{
				s = Connector.openDataOutputStream("scratchpad:///0");
				s.writeLong(nHiScore);
				nSavedHiScore = nHiScore;
			}
			catch (Exception x)
			{
			}
			finally
			{
				if (s != null)
				{
					try { s.close(); } catch (Exception x) {}
				}
			}
		}
	}
}
