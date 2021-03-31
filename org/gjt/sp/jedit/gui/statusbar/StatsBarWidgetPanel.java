/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2021 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.gui.statusbar;

//{{{ Imports
import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.StringTokenizer;
//}}}

/**
 * @author Matthieu Casanova
 */
public class StatsBarWidgetPanel extends Box
{
	private final String propertyName;
	private final View view;
	private final java.util.List<Widget> widgets;

	private String statusBarProperty;

	//{{{ StatsBarWidgetPanel constructor
	public StatsBarWidgetPanel(String propertyName, View view)
	{
		super(BoxLayout.X_AXIS);
		this.propertyName = propertyName;
		this.view = view;
		widgets = new ArrayList<>();
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		Color fg = jEdit.getColorProperty("view.status.foreground");
		Color bg = jEdit.getColorProperty("view.status.background");
		String statusBarProperty = jEdit.getProperty(propertyName);
		if (!Objects.equals(this.statusBarProperty, statusBarProperty))
		{
			removeAll();
			this.statusBarProperty = statusBarProperty;
			StringTokenizer tokenizer = new StringTokenizer(statusBarProperty);
			while (tokenizer.hasMoreTokens())
			{
				String token = tokenizer.nextToken();
				Widget widget = getWidget(token);
				if (widget != null)
				{
					widgets.add(widget);
					Component widgetComponent = widget.getComponent();
					widgetComponent.setBackground(bg);
					widgetComponent.setForeground(fg);
					add(widgetComponent);
					widget.update();
					widget.propertiesChanged();
				}
			}
		}
	} //}}}

	//{{{ updateEvent() method
	/**
	 * Update the widgets that are interested in the given event type
	 * @param statusBarEventType the event type
	 */
	public void updateEvent(StatusBarEventType statusBarEventType)
	{
		widgets
			.stream()
			.filter(widget -> widget.test(statusBarEventType))
			.forEach(Widget::update);
	} //}}}

	//{{{ getWidget() method
	private Widget getWidget(String name)
	{
		StatusWidgetFactory widgetFactory = ServiceManager.getService(StatusWidgetFactory.class, name);
		if (widgetFactory == null)
		{
			return null;
		}
		return widgetFactory.getWidget(view);
	} //}}}
}
