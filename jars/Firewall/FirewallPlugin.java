/*
 * FirewallPlugin.java - a firewall authenticator plugin for jEdit
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

import java.net.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.OptionsDialog;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.util.Log;


/**
 * FirewallPlugin - a firewall authenticator plugin for jEdit
 * @author Dirk Moebius <dmoebius@gmx.net>
 * @version 0.2.1
 */
public class FirewallPlugin extends EBPlugin {
    
    // begin EBPlugin implementation
    public void start() { 
        propertiesChanged();
    }
    
    public void createOptionPanes(OptionsDialog optionsDialog) {
        optionsDialog.addOptionPane(new FirewallOptionPane());
    }

    public void handleMessage(EBMessage msg) {
        if (msg instanceof PropertiesChanged) {
            propertiesChanged();
        }
    }
    // end EBPlugin implementation

    
    private void propertiesChanged() {
        boolean enabled = jEdit.getBooleanProperty("firewall.enabled");
        if (!enabled) {
            Log.log(Log.DEBUG, this, "Firewall disabled");
            System.getProperties().remove("proxySet");
            System.getProperties().remove("proxyHost");
            System.getProperties().remove("proxyPort");        
            System.getProperties().remove("http.proxyHost");
            System.getProperties().remove("http.proxyPort");
            System.getProperties().remove("http.nonProxyHosts");
            Authenticator.setDefault(null);
        } else {
            // set proxy host
            String host = jEdit.getProperty("firewall.host");
            if (host == null) {
                return;
            }
            System.setProperty("http.proxyHost", host);
            Log.log(Log.DEBUG, this, "Firewall enabled: " + host);
            // set proxy port
            String port = jEdit.getProperty("firewall.port");
            if (port != null) {
                System.setProperty("http.proxyPort", port);
            }
            // set non proxy hosts list
            String nonProxyHosts = jEdit.getProperty("firewall.nonProxyHosts");
            if (nonProxyHosts != null) {
                System.setProperty("http.nonProxyHosts", nonProxyHosts);
            }
            // set proxy authentication
            String username = jEdit.getProperty("firewall.user");
            if (username == null || username.length()==0) {
                Log.log(Log.DEBUG, this, "Firewall without user");
                Authenticator.setDefault(new FirewallAuthenticator(null));
            } else {
                Log.log(Log.DEBUG, this, "Firewall user: " + username);
                PasswordAuthentication pw = new PasswordAuthentication(
                    username, 
                    jEdit.getProperty("firewall.password").toCharArray()
                );
                Authenticator.setDefault(new FirewallAuthenticator(pw));
            }
        }
    }
}


class FirewallAuthenticator extends Authenticator {
    PasswordAuthentication pw = null;
    public FirewallAuthenticator(PasswordAuthentication pw) {
        this.pw = pw;
    }
    protected PasswordAuthentication getPasswordAuthentication() {
        return pw;
    }
}

