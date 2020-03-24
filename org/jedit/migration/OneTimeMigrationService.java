/* OneTimeMigrationService.java
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2012 Alan Ezust
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

package org.jedit.migration;

import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import org.jedit.core.MigrationService;

//{{{ OneTimeMigrationService class
/** Base class from which one can more easily implement a migration step
    that should be executed only once per installation.
    <p>
    Concrete implementations of this class should register themselves in services.xml.
    jEdit will call doMigration() on each object, which will skip the ones that
    have been done before.
    <p>
    The time that these services are automatically executed by jEdit is during the
    "initializing properties" step. This means that implementations that need to
    update or remove certain properties during upgrades can take advantage of this class.
    <p> NOTE: This happens after plugins are started, which might be too late for some
    plugins, such as the XMLPlugin, so using the services API to start them is not always
    desireable.
    <p>
    Example:
    <pre>
    &lt;SERVICE CLASS=&quot;org.jedit.migration.OneTimeMigrationService&quot; NAME=&quot;checkFileStatus&quot; &gt;
	  new org.jedit.migration.CheckFileStatus();
	&lt;/SERVICE&gt;
	</pre>
    @author Alan Ezust
    @since jEdit 5.1
*/
public abstract class OneTimeMigrationService implements MigrationService
{
	/**
	 * Performs doMigrate() on each installed OneTimeMigrationService
	 */
	public static void execute()
	{
		String[] migrations = ServiceManager.getServiceNames(OneTimeMigrationService.class);
		if (migrations.length == 0)
			return;
		for (String migration: migrations)
		{
			OneTimeMigrationService svc = ServiceManager.getService(OneTimeMigrationService.class, migration);
			svc.doMigration();
		}
	}

	protected String name;

	/**
	 * @param name of this service. Used to identify and determine if it's been done eariler.
	 */
	protected OneTimeMigrationService(String name)
	{
		this.name = name;
	}

	/**
	 * Calls migrate() but only once per installation.
	 */
	public void doMigration()
	{
		if (!jEdit.getBooleanProperty("migration.step." + name))
		{
			Log.log(Log.MESSAGE, this, "Performing migration step: " + name);
			migrate();
			jEdit.setBooleanProperty("migration.step." + name, true);
		}
	}
}//}}}
