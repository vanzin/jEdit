/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
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
 * There is currently only one concrete implementation of this interface and no
 * general way to add new MigrationServices yet. This class is not finished.
 *
 * The goal of migration is usually to replace old properties or data files
 * with new one. The fact is that according to the data that will be updated
 * the migration may need to be done at various places during jEdit's startup
 * This prevent from calling all migration services at a specific time during
 * startup.
 * However the service exists so it doesn't add a compilation dependency
 * to jEdit.java class
 *
 * @author Matthieu Casanova
 */
public interface MigrationService
{
	void migrate();
}
