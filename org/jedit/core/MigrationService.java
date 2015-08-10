/* MigrationService.java
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2011 Matthieu Casanova
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

package org.jedit.core;

/** A Migration service.
 * <br>
 * The goal of migration is usually to replace old properties or data files
 * with a new set, located elsewhere. The fact is that depending on where
 * the data needs to be updated, the migration may need to be done at
*  different places during jEdit's startup. <br>
 * There is no specific time that all migration services are called currently.
 * The service and interface exists primarily so you don't need to add a compilation
 * dependency of the org.gjt.sp.jEdit class to your MigrationService class. <br>
 * <br>
 * Concrete instances need to guarantee that the migration itself is only done
 * once.
 * @see org.jedit.migration.OneTimeMigrationService
 *
 * @author Matthieu Casanova
 */
public interface MigrationService
{
	void migrate();
}
