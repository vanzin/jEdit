/*
 * Install.java - Main class of the installer
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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

package installer;

import javax.swing.plaf.metal.MetalLookAndFeel;
import java.io.*;
import java.util.Properties;

public class Install
{
	public static void main(String[] args)
	{
		String javaVersion = System.getProperty("java.version");
		if(javaVersion.compareTo("1.3") < 0)
		{
			System.err.println("You are running Java version "
				+ javaVersion + ".");
			System.err.println("This installer requires Java 1.3 or later.");
			System.exit(1);
		}

		if(args.length == 0)
		{
			MetalLookAndFeel.setCurrentTheme(new JEditMetalTheme());
			new SwingInstall();
		}
		else if(args.length == 1 && args[0].equals("text"))
			new ConsoleInstall();
		else if((args.length == 2 || args.length == 3)
			&& args[0].equals("auto"))
		{
			new NonInteractiveInstall(args[1]);
		}
		else
		{
			System.err.println("Usage:");
			System.err.println("java -jar <installer JAR>");
			System.err.println("java -jar <installer JAR> text");
			System.err.println("java -jar <installer JAR> auto"
				+ " <install dir>");
			System.err.println("text parameter starts installer in text-only mode.");
			System.err.println("auto parameter starts installer in non-interactive mode.");
		}
	}

	public Install()
	{
		props = new Properties();
		try
		{
			InputStream in = getClass().getResourceAsStream("install.props");
			props.load(in);
			in.close();
		}
		catch(IOException io)
		{
			System.err.println("Error loading 'install.props':");
			io.printStackTrace();
		}

		buf = new byte[32768];
	}

	public String getProperty(String name)
	{
		return props.getProperty(name);
	}

	public int getIntegerProperty(String name)
	{
		try
		{
			return Integer.parseInt(props.getProperty(name));
		}
		catch(Exception e)
		{
			return -1;
		}
	}

	public void copy(InputStream in, String outfile, Progress progress)
		throws IOException
	{
		File outFile = new File(outfile);

		OperatingSystem.getOperatingSystem().mkdirs(outFile.getParent());

		BufferedOutputStream out = new BufferedOutputStream(
			new FileOutputStream(outFile));

		int count;

		for(;;)
		{
			count = in.read(buf,0,buf.length);
			if(count == -1)
				break;

			out.write(buf,0,count);
			if(progress != null)
				progress.advance(count);
		}

		in.close();
		out.close();
	}

	// private members
	private Properties props;
	private byte[] buf;
}
