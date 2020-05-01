/*
 * HelpSearchPanel.java - Help search GUI
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Slava Pestov
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

package org.gjt.sp.jedit.help;

//{{{ Imports
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.ThreadUtilities;
//}}}

public class HelpSearchPanel extends JPanel
{
	//{{{ HelpSearchPanel constructor
	public HelpSearchPanel(HelpViewerInterface helpViewer)
	{
		super(new BorderLayout(6,6));

		this.helpViewer = helpViewer;

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(new JLabel(jEdit.getProperty("helpviewer.search.caption")));
		box.add(Box.createHorizontalStrut(6));
		box.add(searchField = new HistoryTextField("helpviewer.search"));
		searchField.addActionListener(new ActionHandler());

		add(BorderLayout.NORTH,box);

		results = new JList<>();
		results.addMouseListener(new MouseHandler());
		results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		results.setCellRenderer(new ResultRenderer());
		add(BorderLayout.CENTER,new JScrollPane(results));
	} //}}}

	//{{{ Private members
	private final HelpViewerInterface helpViewer;
	private final HistoryTextField searchField;
	private final JList<Result> results;
	private HelpIndex index;

	private HelpIndex getHelpIndex()
	{
		if(index == null)
		{
			index = new HelpIndex();
			try
			{
				index.indexEditorHelp();
			}
			catch(Exception e)
			{
				index = null;
				Log.log(Log.ERROR,this,e);
				GUIUtilities.error(helpViewer.getComponent(),"helpviewer.search.error",
					new String[] { e.toString() });
			}
		}

		return index;
	} //}}}

	//{{{ ResultIcon class
	static class ResultIcon implements Icon
	{
		private static final RenderingHints renderingHints;

		static
		{
			Map<RenderingHints.Key, Object> hints = new HashMap<>();

			hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			renderingHints = new RenderingHints(hints);
		}

		private final int rank;

		ResultIcon(int rank)
		{
			this.rank = rank;
		}

		@Override
		public int getIconWidth()
		{
			return 40;
		}

		@Override
		public int getIconHeight()
		{
			return 9;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			Graphics2D g2d = (Graphics2D)g.create();
			g2d.setRenderingHints(renderingHints);

			for(int i = 0; i < 4; i++)
			{
				if(rank > i)
					g2d.setColor(UIManager.getColor("Label.foreground"));
				else
					g2d.setColor(UIManager.getColor("Label.disabledForeground"));
				g2d.fillOval(x+i*10,y,9,9);
			}
		}
	} //}}}

	//{{{ ResultRenderer class
	static class ResultRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(
			JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,null,index,
				isSelected,cellHasFocus);

			if(value instanceof String)
			{
				setIcon(null);
				setText((String)value);
			}
			else
			{
				Result result = (Result)value;
				setIcon(new ResultIcon(result.rank));
				setText(result.title);
			}

			return this;
		}
	} //}}}

	//{{{ Result class
	static class Result
	{
		String file;
		String title;
		int rank;

		Result(String title)
		{
			this.title = title;	
		}
		
		Result(HelpIndex.HelpFile file, int count)
		{
			this.file = file.file;
			this.title = file.title;
			rank = count;
		}
	} //}}}

	//{{{ ResultCompare class
	static class ResultCompare implements Comparator<Result>
	{
		@Override
		public int compare(Result r1, Result r2)
		{
			if(r1.rank == r2.rank)
				return r1.title.compareTo(r2.title);
			else
				return r2.rank - r1.rank;
		}
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			final HelpIndex index = getHelpIndex();
			if(index == null)
				return;

			results.setListData(new Result[] { 
				new Result(jEdit.getProperty("helpviewer.searching")) 
			});

			final String text = searchField.getText();
			final java.util.List<Result> resultModel = new ArrayList<>();

			ThreadUtilities.runInBackground(() ->
			{
				StringTokenizer st = new StringTokenizer(text,",.;:-? ");

				// we later use this to compute a relative ranking
				int maxRank = 0;

				while(st.hasMoreTokens())
				{
					String word = st.nextToken().toLowerCase();
					HelpIndex.Word lookup = index.lookupWord(word);
					if(lookup == null)
						continue;

					for(int i = 0; i < lookup.occurCount; i++)
					{
						HelpIndex.Word.Occurrence occur = lookup.occurrences[i];

						boolean ok = false;

						HelpIndex.HelpFile file = index.getFile(occur.file);
						for (Result result : resultModel)
						{
							if (result.file.equals(file.file))
							{
								result.rank += occur.count;
								result.rank += 20; // multiple files w/ word bonus
								maxRank = Math.max(result.rank, maxRank);
								ok = true;
								break;
							}
						}

						if(!ok)
						{
							maxRank = Math.max(occur.count,maxRank);
							resultModel.add(new Result(file,occur.count));
						}
					}
				}

				if(maxRank != 0)
				{
					// turn the rankings into relative rankings, from 1 to 4
					for (Result result : resultModel)
					{
						result.rank = (int) Math.ceil((double) result.rank * 4 / maxRank);
					}

					resultModel.sort(new ResultCompare());
				}

				EventQueue.invokeLater(() ->
				{
					if(resultModel.isEmpty())
					{
						results.setListData(new Result[] {
								new Result(jEdit.getProperty("helpviewer.no-results"))
						});

						UIManager.getLookAndFeel().provideErrorFeedback(null);
					}
					else
						results.setListData(resultModel.toArray(new Result[0]));
				});
			});



		}
	} //}}}

	//{{{ MouseHandler class
	public class MouseHandler extends MouseAdapter
	{
		@Override
		public void mouseReleased(MouseEvent evt)
		{
			int row = results.locationToIndex(evt.getPoint());
			if(row != -1)
			{
				Result result = results.getModel().getElementAt(row);
				helpViewer.gotoURL(result.file,true, 0);
			}
		}
	} //}}}
}
