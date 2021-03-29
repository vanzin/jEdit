/*
 * TipOfTheDay.java - Tip of the day window
 * :tabSize=4:indentSize=4:noTabs=false:
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
import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.security.SecureRandom;
import java.util.Random;

import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}
/** Tip of the day window */
public class TipOfTheDay extends EnhancedDialog
{
	//{{{ TipOfTheDay constructor
	public TipOfTheDay(View view)
	{
		super(view,jEdit.getProperty("tip.title"),false);
		random = new SecureRandom();
		JPanel content = new JPanel(new BorderLayout(12,12));
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JLabel label = new JLabel(jEdit.getProperty("tip.caption"));
		label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() * 2.0f));
		label.setForeground(UIManager.getColor("Button.foreground"));
		content.add(BorderLayout.NORTH,label);

		tipText = new JEditorPane();
		tipText.setEditable(false);
		tipText.setContentType("text/html");
		tipText.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		tipText.setFont(jEdit.getFontProperty("helpviewer.font"));

		nextTip();

		JScrollPane scroller = new JScrollPane(tipText);
		scroller.setPreferredSize(new Dimension(250,250));
		content.add(BorderLayout.CENTER,scroller);

		Box buttons = new Box(BoxLayout.X_AXIS);

		showNextTime = new JCheckBox(jEdit.getProperty("tip.show-next-time"),
			jEdit.getBooleanProperty("tip.show"));
		showNextTime.addActionListener(e -> jEdit.setBooleanProperty("tip.show",showNextTime.isSelected()));
		buttons.add(showNextTime);

		buttons.add(Box.createHorizontalStrut(6));
		buttons.add(Box.createGlue());

		JButton nextTip = new JButton(jEdit.getProperty("tip.next-tip"));
		nextTip.addActionListener(e -> nextTip());
		buttons.add(nextTip);

		buttons.add(Box.createHorizontalStrut(6));

		JButton close = new JButton(jEdit.getProperty("common.close"));
		close.addActionListener(e -> dispose());
		buttons.add(close);
		content.getRootPane().setDefaultButton(close);

		Dimension dim = nextTip.getPreferredSize();
		dim.width = Math.max(dim.width, close.getPreferredSize().width);
		nextTip.setPreferredSize(dim);
		close.setPreferredSize(dim);

		content.add(BorderLayout.SOUTH,buttons);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(view);
		setVisible(true);
	} //}}}

	//{{{ ok() method
	@Override
	public void ok()
	{
		dispose();
	} //}}}

	//{{{ cancel() method
	@Override
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private final JCheckBox showNextTime;
	private final JEditorPane tipText;
	private int currentTip = -1;
	private final Random random;
	//}}}

	//{{{ nextTip() method
	private void nextTip()
	{
		File[] tips = new File(MiscUtilities.constructPath(
			jEdit.getJEditHome(),"doc","tips")).listFiles();
		if(tips == null || tips.length == 0)
		{
			tipText.setText(jEdit.getProperty("tip.not-found"));
			return;
		}

		int count = tips.length;

		// so that we don't see the same tip again if the user
		// clicks 'Next Tip'
		int tipToShow = currentTip;
		while(tipToShow == currentTip || !tips[tipToShow].getName().endsWith(".html"))
			tipToShow = (random.nextInt(Integer.MAX_VALUE)) % count;
		try
		{
			tipText.setPage(tips[tipToShow].toURI().toURL());
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
	} //}}}

	//}}}
}
