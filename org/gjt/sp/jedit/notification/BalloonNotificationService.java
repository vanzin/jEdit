package org.gjt.sp.jedit.notification;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class BalloonNotificationService implements NotificationService
{
	private Object errorLock = new Object();
	private boolean error = false;
	private BalloonFrame frame;
	private final Vector<ErrorEntry> errors = new Vector<ErrorEntry>();

	@SuppressWarnings("serial")
	public class BalloonFrame extends JFrame
	{
		int num = 0;

		public class Balloon extends JPanel
		{
			private Timer timer;
			private static final int MaxMessageLines = 2;
			Balloon(ErrorEntry entry)
			{
				setBorder(BorderFactory.createEtchedBorder());
				setLayout(new BorderLayout());
				setBackground(Color.yellow);
				JLabel path = new JLabel("<html><body><b>" + entry.path + "</b></html>");
				path.setBorder(BorderFactory.createLineBorder(Color.black));
				add(path, BorderLayout.NORTH);
				JTextArea ta = new JTextArea();
				for (int i = 0; i < MaxMessageLines; i++)
				{
					ta.append(entry.messages[i]);
					if (i < MaxMessageLines - 1)
						ta.append("\n");
				}
				ta.setEditable(false);
				add(ta, BorderLayout.CENTER);
				timer = new Timer(2000, new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent arg0)
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								removeBalloon(Balloon.this);
							}
						});
					}
				});
				timer.setRepeats(false);
				timer.restart();
			}
		}

		private JPanel p;
		private final static int BalloonWidth = 500;
		private final static int BalloonHeight = 80;
		private final static int MaxHeight = 400;
		private final Dimension size = new Dimension(BalloonWidth, 0);

		public BalloonFrame()
		{
			setAlwaysOnTop(true);
			setUndecorated(true);
			setLayout(new BorderLayout());
			p = new JPanel();
			add(new JScrollPane(p, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
			p.setLayout(new GridLayout(0, 1));
		}
		
		public void addBalloon(ErrorEntry entry)
		{
			num++;
			p.add(new Balloon(entry));
			adjustSize();
			setVisible(true);
		}

		private void adjustSize() {
			size.height = num * BalloonHeight;
			if (size.height > MaxHeight)
				size.height = MaxHeight;
			setSize(size);
		}

		public void removeBalloon(Balloon b)
		{
			p.remove(b);
			if (num > 0)
				num--;
			if (num == 0)
			{
				dispose();
				setVisible(false);
				clear();
			}
			else
				adjustSize();
		}
	}

	private void clear()
	{
		synchronized (errorLock)
		{
			error = false;
		}
	}

	@Override
	public boolean errorOccurred()
	{
		return error;
	}

	@Override
	public void error(IOException e, String path, Component comp)
	{
		error(comp, path, null, new String [] { e.toString() });
	}

	@Override
	public void error(Component comp, String path, String messageProp,
		Object[] args)
	{
		//final Frame frame = JOptionPane.getFrameForComponent(comp);
		synchronized(errorLock)
		{
			error = true;
			errors.add(new ErrorEntry(path,messageProp,args));
			if(errors.size() == 1)
			{
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run()
					{
						if (frame == null)
							frame = new BalloonFrame();
						synchronized(errorLock)
						{
							for (ErrorEntry e: errors)
								frame.addBalloon(e);
							errors.clear();
						}
					}
				});
			}
		}
	}

}
