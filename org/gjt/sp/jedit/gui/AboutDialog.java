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
import java.util.*;
import org.gjt.sp.jedit.*;
//}}}

public class AboutDialog extends EnhancedDialog
{
	//{{{ AboutDialog constructor
	public AboutDialog(View view)
	{
		super(view,jEdit.getProperty("about.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		content.add(BorderLayout.CENTER,new AboutPanel());

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
		buttonPanel.setBorder(new EmptyBorder(12,0,0,0));

		buttonPanel.add(Box.createGlue());
		close = new JButton(jEdit.getProperty("common.close"));
		close.addActionListener(new ActionHandler());
		getRootPane().setDefaultButton(close);
		buttonPanel.add(close);
		buttonPanel.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,buttonPanel);

		pack();
		setResizable(false);
		setLocationRelativeTo(view);
		show();
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		dispose();
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		dispose();
	} //}}}

	// private members
	private JButton close;

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			dispose();
		}
	} //}}}

	//{{{ AboutPanel class
	static class AboutPanel extends JComponent
	{
		ImageIcon image;
		Vector text;
		int scrollPosition;
		AnimationThread thread;

		AboutPanel()
		{
			setFont(UIManager.getFont("Label.font"));
			setForeground(new Color(96,96,96));
			image = new ImageIcon(getClass().getResource(
				"/org/gjt/sp/jedit/icons/about.png"));
			setBorder(new MatteBorder(1,1,1,1,Color.black));

			text = new Vector(50);
			StringTokenizer st = new StringTokenizer(
				jEdit.getProperty("about.text"),"\n");
			while(st.hasMoreTokens())
			{
				text.addElement(st.nextToken());
			}

			scrollPosition = -300;

			thread = new AnimationThread();
		}

		public void paintComponent(Graphics _g)
		{
			Graphics2D g = (Graphics2D)_g;

			image.paintIcon(this,g,1,1);

			FontMetrics fm = g.getFontMetrics();
			int height = fm.getHeight();
			int firstLine = scrollPosition / height;

			int firstLineOffset = height - scrollPosition % height;
			int lastLine = (scrollPosition + getHeight()
				- 30 - fm.getHeight() * 2) / height;

			int y = 50 + firstLineOffset;

			for(int i = firstLine; i <= lastLine; i++)
			{
				if(i >= 0 && i < text.size() / 2)
				{
					if(2 * i + 1 != text.size())
					{
						String line2;
						line2 = (String)text.elementAt(2 * i + 1);
						int width2 = fm.stringWidth(line2);
						g.drawString(line2,(getWidth() / 2
							+ 20),y);
					}

					String line1 = (String)text.elementAt(2 * i);
					int width1 = fm.stringWidth(line1);
					g.drawString(line1,(getWidth() / 2
						- width1 - 20),y);
				}
				y += fm.getHeight();
			}

			String[] args = { jEdit.getVersion() };
			String version = jEdit.getProperty("about.version",args);
			g.drawString(version,(getWidth() - fm.stringWidth(version)) / 2,
				getHeight() - 30);
		}

		public Dimension getPreferredSize()
		{
			return new Dimension(1 + image.getIconWidth(),
				1 + image.getIconHeight());
		}

		public void addNotify()
		{
			super.addNotify();
			thread.start();
		}

		public void removeNotify()
		{
			super.removeNotify();
			thread.stop();
		}

		class AnimationThread extends Thread
		{
			AnimationThread()
			{
				super("About box animation thread");
				setPriority(Thread.MIN_PRIORITY);
			}

			public void run()
			{
				FontMetrics fm = getFontMetrics(getFont());
				int max = text.size() * fm.getHeight();

				for(;;)
				{
					long start = System.currentTimeMillis();

					scrollPosition++;

					if(scrollPosition > max)
						scrollPosition = -getHeight();

					try
					{
						int delay = (int)Math.max(0,60 -
							(System.currentTimeMillis() - start));
						Thread.sleep(delay);

						SwingUtilities.invokeAndWait(new Runnable()
						{
							public void run()
							{
								repaint();
							}
						});
					}
					catch(Exception e)
					{
					}
				}
			}
		}
	} //}}}
}
