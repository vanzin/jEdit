/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2020 jEdit contributors
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

package org.gjt.sp.jedit.pluginmgr;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;

/**
 * @author Matthieu Casanova
 */
class CachePluginList
{
	public static final long MILLISECONDS_PER_MINUTE = 60L * 1000L;

	private final String id;

	//{{{ CachePluginList constructor
	CachePluginList(String id)
	{
		this.id = id;
	} //}}}

	//{{{ getPluginList() method
	@Nullable
	String getPluginList()
	{
		if (!id.equals(jEdit.getProperty("plugin-manager.mirror.cached-id")))
			return null;

		String xml = getCacheFile()
			.filter(Files::isReadable)
			.filter(CachePluginList::isAcceptableCache)
			.map(this::readCache)
			.orElse(null);
		return xml;
	} //}}}

	//{{{ saveCache() method
	void saveCache(@Nullable CharSequence xml)
	{
		if (xml == null)
			return;

		getCacheFile()
			.ifPresent(path ->
			{
				try
				{
					Files.writeString(path, xml);
				}
				catch (IOException e)
				{
					Log.log(Log.ERROR, this, "Error saving cache data", e);
				}
			});
	} //}}}

	//{{{ getCacheFile() method
	private Optional<Path> getCacheFile()
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if (settingsDirectory == null)
			return Optional.empty();
		return Optional.of(Path.of(settingsDirectory, "pluginMgr-Cached.xml"));
	} //}}}

	//{{{ isAcceptableCache() method
	private static boolean isAcceptableCache(Path path)
	{
		try
		{
			FileTime lastModifiedTime = Files.getLastModifiedTime(path);
			Instant lastModified = lastModifiedTime.toInstant();
			long interval = jEdit.getIntegerProperty("plugin-manager.list-cache.minutes", 5) * MILLISECONDS_PER_MINUTE;
			Instant lowerTimeLimit = Instant.now().minusMillis(interval);
			return lastModified.isAfter(lowerTimeLimit);
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, PluginList.class, "Unable to get last modified time for " + path, e);
			return false;
		}
	} //}}}

	//{{{ deleteCache() method
	void deleteCache()
	{
		getCacheFile().ifPresent(path ->
		{
			Log.log(Log.DEBUG, PluginList.class, "Unable to read plugin list, deleting cached file and try again");
			try
			{
				Files.delete(path);
			}
			catch (IOException e)
			{
				Log.log(Log.WARNING, PluginList.class, "Error while deleting cache file" + path, e);
			}
		});
	} //}}}

	//{{{ readCache() method
	private String readCache(Path path)
	{
		try
		{
			return Files.readString(path);
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, PluginList.class, "Unable to read stream", e);
			deleteCache();
		}
		return null;
	} //}}}
}
