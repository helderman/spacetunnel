/**
 * @(#)Main.java
 *
 * Space Tunnel - Java Applet version
 *
 * @author Ruud Helderman
 * @version 1.00 07/11/18
 */

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.*;

public class Main
	extends Applet
	implements KeyListener, MouseListener
{
	private Image imgBuf;

	// nState: simple state machine
	// 0 = going to menu
	// 1 = in menu
	// 2 = going to game
	// 3 = in game
	// 4 = death

	private int     nState;
	private int     nStage;
	private int     nBend;		// how sharp the turns are
	private long    nScore, nHiScore;
	private String  sScore, sHiScore;
	private boolean bEngine;
	private long    nTimeExplode;
	private Image   imgShipRear;
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
	private static final String[] asGameOver = {"GAME", "OVER"};
	private String[] asStage = {null, null, null, null};

	private Random rndBends = null;
	private Random rndNoise;

	// Keyboard

	private static final int K_dirL = 0;	// left
	private static final int K_dirU = 1;	// up
	private static final int K_dirR = 2;	// right
	private static final int K_dirD = 3;	// down

	private int K_x;
	private int K_y;

	private long[] K_msKeyState = {0, 0, 0, 0};
	private int[]  K_msDuration = {0, 0, 0, 0};

	public void init()
	{
		imgBuf = null;

		aRingPosX = new long[nNumberOfRings];
		aRingPosY = new long[nNumberOfRings];

		if (rndBends == null)
		{
			rndBends = new Random();
		}	
		rndNoise = new Random(0);

		oFontN = new Font("Monospaced", Font.PLAIN, 12);
		oFontL = new Font("Sans-serif", Font.PLAIN, 14);
		//oFontN = Font.getDefaultFont();
		//oFontL = Font.getFont(Font.SIZE_LARGE);

		try
		{
			imgShipRear = getImage(getCodeBase(), "ShipRear.gif");
			//imgShipRear = getImage(getDocumentBase(), "ShipRear.gif");
		}
		catch (Exception x)
		{
			//System.out.println("Error loading image: " + x);
			imgShipRear = null;
		}

		nState = 0;
		nScore = nHiScore = 0;
		//LoadHiScore();
		sScore = "0";
		sHiScore = "" + nHiScore;

		addKeyListener(this);
		addMouseListener(this);

		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	public void paint(Graphics g)
	{
		update(g);
	}

	public void update(Graphics g1)
	{
		nTimeCurr = System.currentTimeMillis();
		nRingCurr = nTimeCurr / nRingDuration;

		// K_Checkpoint
		for (int dir = 0; dir < K_msKeyState.length; dir++)
		{
			long ms = K_msKeyState[dir];
			if (ms < 0)
			{
				ms += System.currentTimeMillis();
			}
			K_msKeyState[dir] -= ms;
			K_msDuration[dir] = (int)ms;
		}
		K_x = K_msDuration[K_dirR] - K_msDuration[K_dirL];
		K_y = K_msDuration[K_dirU] - K_msDuration[K_dirD];

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
			nShipDirX += K_x;
			nShipDirY += K_y;
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
				//if (nRingPrev == nRing0 + 8)
				//{
				//	System.gc();
				//}

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

		// Double buffering
		
		if (imgBuf == null || imgBuf.getWidth(this) != getWidth() || imgBuf.getHeight(this) != getHeight())
		{
			imgBuf = createImage(getWidth(), getHeight());
		}
		Graphics g = imgBuf.getGraphics();

		String[] asText = null;

		//g.lock();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());

		// starry background

		g.setColor(Color.BLUE);
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

			g.setColor(new Color(
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
			if (imgShipRear != null)
			{
				g.drawImage(imgShipRear,
					(getWidth()  - imgShipRear.getWidth(this))  / 2,
					(getHeight() - imgShipRear.getHeight(this)) / 2,
					this);

				bEngine = !bEngine;
				if (bEngine)
				{
					g.setColor(Color.BLACK);
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

					g.setColor(new Color(
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
				//SaveHiScore();
			}
			break;
		}

		// score

		g.setFont(oFontN);

		FontMetrics fm = getFontMetrics(oFontN);
		int nPosY = fm.getAscent();

		g.setColor(Color.LIGHT_GRAY);
		g.drawString("Score:", 0, nPosY);
		g.drawString("  High:", getWidth() / 2, nPosY);

		g.setColor(Color.WHITE);
		g.drawString(sScore, getWidth() / 2 - fm.stringWidth(sScore), nPosY);
		g.drawString(sHiScore, getWidth() - fm.stringWidth(sHiScore), nPosY);

		// text

		if (asText != null)
		{
			g.setFont(oFontL);
			fm = getFontMetrics(oFontL);
			int nDistY = fm.getHeight();
			nPosY += (getHeight() - nDistY * asText.length) / 2;

			for (int nCount = 0; nCount < asText.length; nCount++)
			{
				if (asText[nCount] != null)
				{
					int nPosX = (getWidth() - fm.stringWidth(asText[nCount])) / 2;
					g.drawString(asText[nCount], nPosX, nPosY);
				}
				nPosY += nDistY;
			}
		}

		//g.unlock(true);
		g1.drawImage(imgBuf, 0, 0, this);
		repaint();
	}

	public void keyPressed(KeyEvent e)
	{
/***
		int dir = e.getKeyCode();
		if (dir == KeyEvent.VK_ENTER && nState == 1)
		{
			nState = 2;	// start the game
		}
***/
		int dir = e.getKeyCode() - KeyEvent.VK_LEFT;
		if (dir >= K_dirL && dir <= K_dirD && K_msKeyState[dir] >= 0)
		{
			K_msKeyState[dir] -= System.currentTimeMillis();
		}
	}

	public void keyReleased(KeyEvent e)
	{
		int dir = e.getKeyCode() - KeyEvent.VK_LEFT;
		if (dir >= K_dirL && dir <= K_dirD && K_msKeyState[dir] < 0)
		{
			K_msKeyState[dir] += System.currentTimeMillis();
		}
	}

	public void keyTyped(KeyEvent e)
	{
	}

	public void mouseClicked(MouseEvent e)
	{
		if (nState == 1)
		{
			nState = 2;	// start the game
		}
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
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

	public String getAppletInfo()
	{
		return "Space Tunnel 1.0 - Copyright 2005-2007 Ruud Helderman";
	}
}
