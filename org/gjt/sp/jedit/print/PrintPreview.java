/*
 * PrintPreview.java 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
 * Portions copyright (C) 2002 Thomas Dilts
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
 *
 *
 * Written by 20020428/Thomas Dilts 
 */
 
package org.gjt.sp.jedit.print;

//{{{ Imports

import java.awt.*;
import java.text.*;
import java.lang.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.awt.print.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;
//}}}

public class PrintPreview extends JDialog 
{

	//{{{ PrintPreview constructor
	public PrintPreview(View view, Buffer buffer, boolean bSelected, BufferPrintable target, PrinterJob prnJob,
			PageFormat pageFormat) 
	{
		super(view, jEdit.getProperty("print.preview.title"), true);
		ChangeListener listener;
		this.view = view;
		this.buffer = buffer;
		this.bSelected = bSelected;
		setSize(600, 400);
		m_target = target;
		m_prnJob = prnJob;
		m_pageFormat = pageFormat;

		JToolBar tb = new JToolBar();

		//{{{ Creating the scale combo boxes
		String[] scales = {35 + sPercent,
		                   50 + sPercent,
		                   100 + sPercent,
		                   150 + sPercent};
		m_cbScale = new JComboBox(scales);

		//{{{ Scale combo boxes action listener

		ActionListener lst = new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				Thread runner = new Thread() 
				{
					public void run() 
					{
						String str = m_cbScale.getSelectedItem().
							     toString();
						if (str.endsWith(sPercent))
							str = str.substring(0, str.length() - 1);
						str = str.trim();
						int scale = 0;
						try
						{
							scale = Integer.parseInt(str);
						}
						catch (NumberFormatException ex)
						{
							return;
						}
						int w = (int) (m_wPage * scale / 100);
						int h = (int) (m_hPage * scale / 100);

						Component[] comps = m_preview.getComponents();
						for (int k = 0; k < comps.length; k++)
						{
							if (!(comps[k] instanceof PagePreview))
								continue;
							PagePreview pp = (PagePreview) comps[k];
							pp.setScaledSize(w, h);
						}
						m_preview.doLayout();
						m_preview.getParent().getParent().validate();
					}
				};
				runner.start();
			}
		};
		//}}}

		m_cbScale.addActionListener(lst);
		m_cbScale.setMaximumSize(m_cbScale.getPreferredSize());
		m_cbScale.setEditable(true);
		tb.addSeparator();
		tb.add(m_cbScale);
		getContentPane().add(tb, BorderLayout.NORTH);
		//}}}

		//{{{ Creating the slider for page selection

		JPanel p = new JPanel();
		listener=new SliderListener(p);
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBorder(new TitledBorder(new Integer(0) + " - " + new Integer(PAGES_PER_VIEW)));
		JSlider s = new JSlider(1, PAGES_PER_VIEW*20+1, 1);

		s.putClientProperty("JSlider.isFilled", Boolean.TRUE );

		s.setPaintTicks(true);
		s.setMajorTickSpacing(PAGES_PER_VIEW);
		s.setMinorTickSpacing(PAGES_PER_VIEW);
		s.setExtent(PAGES_PER_VIEW);
		s.createStandardLabels(PAGES_PER_VIEW);
		s.setPaintLabels( true );
		s.setSnapToTicks( true );
		s.setExtent(PAGES_PER_VIEW);

		s.addChangeListener(listener);

		p.add(Box.createRigidArea(new Dimension(5,5)));
		p.add(s);
		p.add(Box.createRigidArea(new Dimension(5,5)));
		tb.addSeparator();
		tb.add(p);
		//}}}

		m_preview = new PreviewContainer();
		JScrollPane ps = new JScrollPane(m_preview);
		getContentPane().add(ps, BorderLayout.CENTER);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		renderPreview(0);
	}
	//}}}

	//{{{ renderPreview
	private int renderPreview(int pageIndexStart)
	{
		Component[] comps = m_preview.getComponents();
		for (int k = 0; k < comps.length; k++)
		{
			if (!(comps[k] instanceof PagePreview))
				continue;
			PagePreview pp = (PagePreview) comps[k];
			pp.flush();
			pp.removeAll();
		}
		m_preview.removeAll();

		m_wPage = (int) (m_pageFormat.getWidth());
		m_hPage = (int) (m_pageFormat.getHeight());

		int scale = 0;
		String str = m_cbScale.getSelectedItem().
		             toString();
		if (str.endsWith(sPercent))
			str = str.substring(0, str.length() - 1);
		str = str.trim();
		try
		{
			scale = Integer.parseInt(str);
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
		int w = (int) (m_wPage * scale / 100);
		int h = (int) (m_hPage * scale / 100);

		int pageCounter = 0;
		int pageIndex=pageIndexStart;
		try
		{
			while (pageCounter<PAGES_PER_VIEW)
			{
				BufferedImage img = new BufferedImage(m_wPage,
				                                      m_hPage, BufferedImage.TYPE_INT_RGB);
				Graphics g = img.getGraphics();
				g.setColor(Color.white);
				g.fillRect(0, 0, m_wPage, m_hPage);
				if (m_target.print(g, m_pageFormat, pageIndex) !=
				                Printable.PAGE_EXISTS)
					break;

				if(pageIndex>=pageIndexStart)
				{
					PagePreview pp = new PagePreview(w, h, img);
					m_preview.add(pp);

					pageCounter++;
				}
				else
				{
					img.flush();
				}
				g.dispose();
				pageIndex++;
			}
		}
		catch (PrinterException e)
		{
			e.printStackTrace();
		}

		m_preview.doLayout();
		m_preview.getParent().getParent().validate();
		System.gc();
		return pageIndex;
	}
	//}}}

	//{{{ Internal class PreviewContainer
	class PreviewContainer extends JPanel 
	{
		protected int H_GAP = 16;
		protected int V_GAP = 10;

		/**
		 *  Gets the preferredSize attribute of the PreviewContainer object
		 *
		 *@return    The preferredSize value
		 */
		public Dimension getPreferredSize() 
		{
			int n = getComponentCount();
			if (n == 0)
				return new Dimension(H_GAP, V_GAP);
			Component comp = getComponent(0);
			Dimension dc = comp.getPreferredSize();
			int w = dc.width;
			int h = dc.height;

			Dimension dp = getParent().getSize();
			int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
			int nRow = n / nCol;
			if (nRow * nCol < n)
				nRow++;

			int ww = nCol * (w + H_GAP) + H_GAP;
			int hh = nRow * (h + V_GAP) + V_GAP;
			Insets ins = getInsets();
			return new Dimension(ww + ins.left + ins.right,
			                     hh + ins.top + ins.bottom);
		}

		/**
		 *  Gets the maximumSize attribute of the PreviewContainer object
		 *
		 *@return    The maximumSize value
		 */
		public Dimension getMaximumSize() 
		{
			return getPreferredSize();
		}

		/**
		 *  Gets the minimumSize attribute of the PreviewContainer object
		 *
		 *@return    The minimumSize value
		 */
		public Dimension getMinimumSize() 
		{
			return getPreferredSize();
		}

		public void doLayout() 
		{
			Insets ins = getInsets();
			int x = ins.left + H_GAP;
			int y = ins.top + V_GAP;

			int n = getComponentCount();
			if (n == 0)
				return;
			Component comp = getComponent(0);
			Dimension dc = comp.getPreferredSize();
			int w = dc.width;
			int h = dc.height;

			Dimension dp = getParent().getSize();
			int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
			int nRow = n / nCol;
			if (nRow * nCol < n)
				nRow++;

			int index = 0;
			for (int k = 0; k < nRow; k++)
			{
				for (int m = 0; m < nCol; m++)
				{
					if (index >= n)
						return;
					comp = getComponent(index++);
					comp.setBounds(x, y, w, h);
					x += w + H_GAP;
				}
				y += h + V_GAP;
				x = ins.left + H_GAP;
			}
		}
	}
	//}}}

	//{{{ Internal class PagePreview
	class PagePreview extends JPanel 
	{
		protected int m_w;
		protected int m_h;
		protected Image m_source;
		protected Image m_img;
		public void flush()
		{
			if (m_img!=null)
				m_img.flush();
			if (m_source!=null)
				m_source.flush();
			m_img=null;
			m_source=null;
		}

		public PagePreview(int w, int h, Image source) 
		{
			m_w = w;
			m_h = h;
			m_source = source;
			m_img = m_source.getScaledInstance(m_w, m_h,
			                                   Image.SCALE_SMOOTH);
			m_img.flush();
			setBackground(Color.white);
			setBorder(new MatteBorder(1, 1, 2, 2, Color.black));
		}

		public void setScaledSize(int w, int h) 
		{
			m_w = w;
			m_h = h;
			m_img = m_source.getScaledInstance(m_w, m_h,
			                                   Image.SCALE_SMOOTH);
			repaint();
		}

		public Dimension getPreferredSize() 
		{
			Insets ins = getInsets();
			return new Dimension(m_w + ins.left + ins.right,
			                     m_h + ins.top + ins.bottom);
		}

		public Dimension getMaximumSize() 
		{
			return getPreferredSize();
		}

		public Dimension getMinimumSize() 
		{
			return getPreferredSize();
		}

		public void paint(Graphics g) 
		{
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			g.drawImage(m_img, 0, 0, this);
			paintBorder(g);
		}
	}
	//}}}

	//{{{ Internal class SliderListener
	class SliderListener implements ChangeListener 
	{
		JPanel tf;
		public SliderListener(JPanel f) 
		{
			tf = f;
		}
		public void stateChanged(ChangeEvent e) 
		{
			JSlider s1 = (JSlider)e.getSource();
			if (s1.getValueIsAdjusting())
				return;
			int lastPage=renderPreview(s1.getValue()-1);
			tf.setBorder(new TitledBorder( new Integer(s1.getValue()) + " - " + new Integer(lastPage)));
		}
	}
	//}}}

	//{{{ Members
	protected int m_wPage;
	protected int m_hPage;
	protected BufferPrintable m_target;
	protected JComboBox m_cbScale;
	protected PreviewContainer m_preview;
	protected PrinterJob m_prnJob;
	protected PageFormat m_pageFormat;
	protected View view;
	protected Buffer buffer;
	protected boolean bSelected;
	private static final int PAGES_PER_VIEW=5;
	protected final String sPercent = new String(new Character(new DecimalFormat().
	                                  getDecimalFormatSymbols().getPercent()).toString());
	//}}}
}

