/*
 * FirewallOptionPane.java - Firewall plugin options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 Dirk Moebius
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

package org.gjt.sp.jedit.options;

 //{{{ Imports
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import org.gjt.sp.jedit.*;
//}}}

public class FirewallOptionPane extends AbstractOptionPane {

	//{{{ FirewallOptionPane constructor
	public FirewallOptionPane()
	{
		super("firewall");
	} //}}}

	//{{{ _init() method
	public void _init()
	{
		// checkbox "Enable firewall authentication"
		addComponent(cEnabled = new JCheckBox(jEdit.getProperty(
			"options.firewall.enabled")));
		// proxy host
		addComponent(jEdit.getProperty("options.firewall.host"), 
			tHost = new JTextField(jEdit.getProperty("firewall.host"), 15));
		// proxy port
		addComponent(jEdit.getProperty("options.firewall.port"), 
			tPort = new JTextField(jEdit.getProperty("firewall.port"), 15));
		// proxy username
		addComponent(jEdit.getProperty("options.firewall.user"),
			tUser = new JTextField(jEdit.getProperty("firewall.user"), 15));
		// proxy password
		addComponent(jEdit.getProperty("options.firewall.password"),
			tPass = new JPasswordField(jEdit.getProperty("firewall.password"), 15));
		// no proxy for
		addComponent(jEdit.getProperty("options.firewall.nonProxy"),
			tNonProxy = new JTextField(jEdit.getProperty("firewall.nonProxyHosts"), 15));

		boolean enabled = jEdit.getBooleanProperty("firewall.enabled");
		cEnabled.setSelected(enabled);
		tHost.setEnabled(enabled);
		tPort.setEnabled(enabled);
		tUser.setEnabled(enabled);
		tPass.setEnabled(enabled);
		tNonProxy.setEnabled(enabled);

		cEnabled.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean enbld = cEnabled.isSelected();
				cEnabled.setSelected(enbld);
				tHost.setEnabled(enbld);
				tPort.setEnabled(enbld);
				tUser.setEnabled(enbld);
				tPass.setEnabled(enbld);
				tNonProxy.setEnabled(enbld);
			}
		});
	} //}}}

	//{{{ _save() method
	public void _save() {
		jEdit.setBooleanProperty("firewall.enabled", cEnabled.isSelected());
		jEdit.setProperty("firewall.host", tHost.getText());			
		jEdit.setProperty("firewall.port", tPort.getText());
		jEdit.setProperty("firewall.user", tUser.getText());
		jEdit.setProperty("firewall.password", new String(tPass.getPassword()));
		jEdit.setProperty("firewall.nonProxyHosts", tNonProxy.getText());
	} //}}}

	//{{{ Private members
	private JCheckBox cEnabled;
	private JTextField tHost;
	private JTextField tPort;
	private JTextField tUser;
	private JPasswordField tPass;
	private JTextField tNonProxy;
	//}}}
}
