/*
 * AboutDialog.java - About jEdit dialog box
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001, 2002 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.gui;

//{{{ Imports
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import org.gjt.sp.jedit.*;
//}}}

public class AboutDialog extends JDialog implements ActionListener
{
	//{{{ AboutDialog constructor
	public AboutDialog(View view)
	{
		super(view,jEdit.getProperty("about.title"),true);
		setResizable(false);
		JButton closeBtn = new JButton(jEdit.getProperty("common.close"));
		closeBtn.addActionListener( this );
		getRootPane().setDefaultButton( closeBtn );

		JPanel p = new JPanel( new BorderLayout() );
		AboutPanel aboutPanel = new AboutPanel();
		JPanel flowP = new JPanel( new FlowLayout() );
		flowP.add( closeBtn );
		flowP.add( Box.createRigidArea( new Dimension( 40, 40 ) ) );
		Dimension dim = new Dimension( 10, 0 );
		p.add( BorderLayout.WEST, Box.createRigidArea( dim ) );
		p.add( BorderLayout.EAST, Box.createRigidArea( dim ) );
		p.add( BorderLayout.NORTH, Box.createRigidArea( new Dimension( 10, 10 ) ) );
		p.add( BorderLayout.SOUTH, flowP );
		p.add( BorderLayout.CENTER, aboutPanel );

		setContentPane( p );
		pack();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation( (d.width-getWidth())/2, (d.height-getHeight())/2 );
		addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e )
			{
				AboutPanel.stopThread();
				dispose();
			}
		});
		setVisible(true);
	} //}}}


	public void actionPerformed( ActionEvent e )
	{
		AboutPanel.stopThread();
		dispose();
	}

	//{{{ AboutPanel class
	static class AboutPanel extends JComponent implements Runnable
	{
		private BufferedImage bufImage = null;
		private Graphics2D g = null;
		private static final Font defaultFont = UIManager.getFont("Label.font");
		private Font bottomLineFont = defaultFont.deriveFont(9.8f);
		private String sBottomLine;
		private ImageIcon image;
		private Vector<String> vLines;
		private static boolean doWork = false;
		private Thread th = null;
		private FontMetrics fm = null;
		private int iLineHeight = 0, iListHeight, iLineCount = 0,
			iBottomLineXOffset = 0, iBottomLineYOffset = 0,
			iPipeLineCount = 0, w = 0, h = 0, y = 0;
		private static final int
			SLEEP_TIME = 30,
			iBottomPadding = 56,
			iTopPadding = 100;
		private static Rectangle2D.Float rectangle;
		private static GradientPaint gradientPaint = null;

		AboutPanel()
		{
			String[] args = { jEdit.getVersion(), System.getProperty("java.version") };
			sBottomLine = jEdit.getProperty("about.version",args);
			setFont( defaultFont );
			fm = getFontMetrics( defaultFont );
			FontMetrics fmBottom = getFontMetrics( bottomLineFont );
			iLineHeight = fm.getHeight();
			vLines = new Vector<String>(50);
			image = (ImageIcon)GUIUtilities.loadIcon("about.png");
			MediaTracker tracker = new MediaTracker( this );
			tracker.addImage( image.getImage(), 0 );

			try
			{
				tracker.waitForID( 0 );
			}
			catch( Exception exc )
			{
				tell( "AboutPanel: " + exc );
			}

			Dimension d = new Dimension( image.getIconWidth(), image.getIconHeight() );
			setSize( d );
			setPreferredSize( d );
			w = d.width;
			h = d.height;
			iBottomLineXOffset = (w / 2) - (fmBottom.stringWidth( sBottomLine ) / 2);
			iBottomLineYOffset = h-iLineHeight/2;
			StringTokenizer st = new StringTokenizer(
				jEdit.getProperty("about.text"),"\n");
			while(st.hasMoreTokens())
			{
				vLines.add( st.nextToken() );
			}

			iLineCount = vLines.size();
			iListHeight = iLineCount * iLineHeight;
			startThread();
			updateUI();
		}

		private final void drain() {

			if( bufImage == null )
			{
				//pre-computing all data that can be known at this time
				Dimension d = getSize();
				bufImage = new BufferedImage( d.width, d.height,
					BufferedImage.TYPE_INT_RGB );
				g = bufImage.createGraphics();
				rectangle = new Rectangle2D.Float( 0, iTopPadding,
					d.width, d.height-iBottomPadding-iTopPadding );
				//"+1" makes sure every new line from below comes up smoothly
				//cause it gets pre-painted and clipped as needed
				iPipeLineCount = 1 + (int)(rectangle.height/iLineHeight);
				y = (int) d.height+iBottomPadding;
				g.setFont( defaultFont );
				gradientPaint = new GradientPaint(
					rectangle.width/2, iTopPadding+80, new Color( 80, 80, 80 ),
					rectangle.width/2, iTopPadding, new Color( 205, 205, 205 )
					);
				g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			}

			g.drawImage( image.getImage(), 0, 0, w, h, this );
			
			g.setFont( bottomLineFont );
			
			g.setPaint( new Color(55, 55, 55) );
			g.drawString( sBottomLine, iBottomLineXOffset, iBottomLineYOffset );
			// Draw a highlight effect
			g.setPaint( new Color(255, 255, 255, 50) );
			g.drawString( sBottomLine, iBottomLineXOffset + 1, iBottomLineYOffset + 1 );
			
			g.setFont( defaultFont );
			g.setPaint( Color.black );
			
			
			g.drawRect( 0, 0, w-1, h-1 );
			g.clip( rectangle );
			g.setPaint( gradientPaint );
			int iDrawnLinesCount = 0, yCoor = 0;

			for( int i=0; i<iLineCount; i++ )
			{
				//check whether the text line is above the canvas, if so, the code skips it
				yCoor = y+(i*iLineHeight);
				if( yCoor < iTopPadding )
				{
					continue;
				}

				//good to go, now draw only iPipeLineCount lines and get out from loop
				String sLine = vLines.get( i );
				int x = ( w - fm.stringWidth( sLine ) )/2;
				g.drawString( sLine, x, yCoor );
				if( ++iDrawnLinesCount >= iPipeLineCount )
				{
					break;
				}
			}

			y--;
			paint( getGraphics() );

			//check if the end of the list has been reached,
			//if so rewind
			if( (y + iListHeight) < iTopPadding )
			{
				y = (int) h+iBottomPadding;
			}
		}

		public void update( Graphics g )
		{
			paint( g );
		}

		public void paint( Graphics panelGraphics )
		{
			if( (panelGraphics != null) && (bufImage != null) )
			{
				panelGraphics.drawImage( bufImage, 0, 0, w, h, this );
			}
		}

		public void run()
		{
			try
			{
				while( doWork )
				{
					drain();
					th.sleep( SLEEP_TIME );
				}
			}
			catch( Exception exc )
			{
				exc.printStackTrace();
			}

			doWork = false;
			th = null;
		}

		public void startThread()
		{
			if( th == null )
			{
				th = new Thread( this );
				doWork = true;
				th.start();
			}
		}

		public static void stopThread()
		{
			doWork = false;
		}

		public static void tell( Object obj )
		{
			String str = (obj == null) ? "NULL" : obj.toString();
			JOptionPane.showMessageDialog( jEdit.getActiveView(), str, "Title", 1 );
		}
	} //}}}
}
