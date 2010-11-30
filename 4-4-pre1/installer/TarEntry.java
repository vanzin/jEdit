/*
** Authored by Timothy Gerard Endres
** <mailto:time@gjt.org>  <http://www.trustice.com>
** 
** This work has been placed into the public domain.
** You may use this work in any way and for any purpose you wish.
**
** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
** REDISTRIBUTION OF THIS SOFTWARE. 
** 
*/

package installer;

import java.io.*;
import java.util.Date;


/**
 *
 * This class represents an entry in a Tar archive. It consists
 * of the entry's header, as well as the entry's File. Entries
 * can be instantiated in one of three ways, depending on how
 * they are to be used.
 * <p>
 * TarEntries that are created from the header bytes read from
 * an archive are instantiated with the TarEntry( byte[] )
 * constructor. These entries will be used when extracting from
 * or listing the contents of an archive. These entries have their
 * header filled in using the header bytes. They also set the File
 * to null, since they reference an archive entry not a file.
 * <p>
 * TarEntries that are created from Files that are to be written
 * into an archive are instantiated with the TarEntry( File )
 * constructor. These entries have their header filled in using
 * the File's information. They also keep a reference to the File
 * for convenience when writing entries.
 * <p>
 * Finally, TarEntries can be constructed from nothing but a name.
 * This allows the programmer to construct the entry by hand, for
 * instance when only an InputStream is available for writing to
 * the archive, and the header information is constructed from
 * other information. In this case the header fields are set to
 * defaults and the File is set to null.
 *
 * <p>
 * The C structure for a Tar Entry's header is:
 * <pre>
 * struct header {
 *		char	name[NAMSIZ];
 *		char	mode[8];
 *		char	uid[8];
 *		char	gid[8];
 *		char	size[12];
 *		char	mtime[12];
 *		char	chksum[8];
 *		char	linkflag;
 *		char	linkname[NAMSIZ];
 *		char	magic[8];
 *		char	uname[TUNMLEN];
 *		char	gname[TGNMLEN];
 *		char	devmajor[8];
 *		char	devminor[8];
 *	} header;
 * </pre>
 *
 * @see TarHeader
 *
 */

public
class		TarEntry
extends		Object
	{
	/**
	 * If this entry represents a File, this references it.
	 */
	protected File				file;

	/**
	 * This is the entry's header information.
	 */
	protected TarHeader			header;

	/**
	 * Construct an entry with only a name. This allows the programmer
	 * to construct the entry's header "by hand". File is set to null.
	 */
	public
	TarEntry( String name )
		{
		this.initialize();
		this.nameTarHeader( this.header, name );
		}

	/**
	 * Construct an entry for a file. File is set to file, and the
	 * header is constructed from information from the file.
	 *
	 * @param file The file that the entry represents.
	 */
	public
	TarEntry( File file )
		throws InvalidHeaderException
		{
		this.initialize();
		this.getFileTarHeader( this.header, file );
		}

	/**
	 * Construct an entry from an archive's header bytes. File is set
	 * to null.
	 *
	 * @param headerBuf The header bytes from a tar archive entry.
	 */
	public
	TarEntry( byte[] headerBuf )
		throws InvalidHeaderException
		{
		this.initialize();
		this.parseTarHeader( this.header, headerBuf );
		}

	/**
	 * Initialization code common to all constructors.
	 */
	private void
	initialize()
		{
		this.file = null;
		this.header = new TarHeader();
		}

	/**
	 * Determine if the two entries are equal. Equality is determined
	 * by the header names being equal.
	 *
	 * @return it Entry to be checked for equality.
	 * @return True if the entries are equal.
	 */
	public boolean
	equals( TarEntry it )
		{
		return
			this.header.name.toString().equals
				( it.header.name.toString() );
		}

	/**
	 * Determine if the given entry is a descendant of this entry.
	 * Descendancy is determined by the name of the descendant
	 * starting with this entry's name.
	 *
	 * @param desc Entry to be checked as a descendent of this.
	 * @return True if entry is a descendant of this.
	 */
	public boolean
	isDescendent( TarEntry desc )
		{
		return
			desc.header.name.toString().startsWith
				( this.header.name.toString() );
		}

	/**
	 * Get this entry's header.
	 *
	 * @return This entry's TarHeader.
	 */
	public TarHeader
	getHeader()
		{
		return this.header;
		}

	/**
	 * Get this entry's name.
	 *
	 * @return This entry's name.
	 */
	public String
	getName()
		{
		return this.header.name.toString();
		}

	/**
	 * Set this entry's name.
	 *
	 * @param name This entry's new name.
	 */
	public void
	setName( String name )
		{
		this.header.name =
			new StringBuffer( name );
		}

	/**
	 * Get this entry's user id.
	 *
	 * @return This entry's user id.
	 */
	public int
	getUserId()
		{
		return this.header.userId;
		}

	/**
	 * Set this entry's user id.
	 *
	 * @param userId This entry's new user id.
	 */
	public void
	setUserId( int userId )
		{
		this.header.userId = userId;
		}

	/**
	 * Get this entry's group id.
	 *
	 * @return This entry's group id.
	 */
	public int
	getGroupId()
		{
		return this.header.groupId;
		}

	/**
	 * Set this entry's group id.
	 *
	 * @param groupId This entry's new group id.
	 */
	public void
	setGroupId( int groupId )
		{
		this.header.groupId = groupId;
		}

	/**
	 * Get this entry's user name.
	 *
	 * @return This entry's user name.
	 */
	public String
	getUserName()
		{
		return this.header.userName.toString();
		}

	/**
	 * Set this entry's user name.
	 *
	 * @param userName This entry's new user name.
	 */
	public void
	setUserName( String userName )
		{
		this.header.userName =
			new StringBuffer( userName );
		}

	/**
	 * Get this entry's group name.
	 *
	 * @return This entry's group name.
	 */
	public String
	getGroupName()
		{
		return this.header.groupName.toString();
		}

	/**
	 * Set this entry's group name.
	 *
	 * @param groupName This entry's new group name.
	 */
	public void
	setGroupName( String groupName )
		{
		this.header.groupName =
			new StringBuffer( groupName );
		}

	/**
	 * Convenience method to set this entry's group and user ids.
	 *
	 * @param userId This entry's new user id.
	 * @param groupId This entry's new group id.
	 */
	public void
	setIds( int userId, int groupId )
		{
		this.setUserId( userId );
		this.setGroupId( groupId );
		}

	/**
	 * Convenience method to set this entry's group and user names.
	 *
	 * @param userName This entry's new user name.
	 * @param groupName This entry's new group name.
	 */
	public void
	setNames( String userName, String groupName )
		{
		this.setUserName( userName );
		this.setGroupName( groupName );
		}

	/**
	 * Set this entry's modification time. The parameter passed
	 * to this method is in "Java time".
	 *
	 * @param time This entry's new modification time.
	 */
	public void
	setModTime( long time )
		{
		this.header.modTime = time / 1000;
		}

	/**
	 * Set this entry's modification time.
	 *
	 * @param time This entry's new modification time.
	 */
	public void
	setModTime( Date time )
		{
		this.header.modTime = time.getTime() / 1000;
		}

	/**
	 * Set this entry's modification time.
	 *
	 * @param time This entry's new modification time.
	 */
	public Date
	getModTime()
		{
		return new Date( this.header.modTime * 1000 );
		}

	/**
	 * Get this entry's file.
	 *
	 * @return This entry's file.
	 */
	public File
	getFile()
		{
		return this.file;
		}

	/**
	 * Get this entry's file size.
	 *
	 * @return This entry's file size.
	 */
	public long
	getSize()
		{
		return this.header.size;
		}

	/**
	 * Set this entry's file size.
	 *
	 * @param size This entry's new file size.
	 */
	public void
	setSize( long size )
		{
		this.header.size = size;
		}

	/**
	 * Convenience method that will modify an entry's name directly
	 * in place in an entry header buffer byte array.
	 *
	 * @param outbuf The buffer containing the entry header to modify.
	 * @param newName The new name to place into the header buffer.
	 */
	public void
	adjustEntryName( byte[] outbuf, String newName )
		{
		int offset = 0;
		offset = TarHeader.getNameBytes
			( new StringBuffer( newName ),
				outbuf, offset, TarHeader.NAMELEN );
		}

	/**
	 * Return whether or not this entry represents a directory.
	 *
	 * @return True if this entry is a directory.
	 */
	public boolean
	isDirectory()
		{
		if ( this.file != null )
			return this.file.isDirectory();

		if ( this.header != null )
			{
			if ( this.header.linkFlag == TarHeader.LF_DIR )
				return true;

			if ( this.header.name.toString().endsWith( "/" ) )
				return true;
			}

		return false;
		}

	/**
	 * Fill in a TarHeader with information from a File.
	 *
	 * @param hdr The TarHeader to fill in.
	 * @param file The file from which to get the header information.
	 */
	public void
	getFileTarHeader( TarHeader hdr, File file )
		throws InvalidHeaderException
		{
		this.file = file;

		String name = file.getPath();
		String osname = System.getProperty( "os.name" );
		if ( osname != null )
			{
			// Strip off drive letters!
			// REVIEW Would a better check be "(File.separator == '\')"?

			// String Win32Prefix = "Windows";
			// String prefix = osname.substring( 0, Win32Prefix.length() );
			// if ( prefix.equalsIgnoreCase( Win32Prefix ) )

			// if ( File.separatorChar == '\\' )

			// Per Patrick Beard:
			String Win32Prefix = "windows";
			if ( osname.toLowerCase().startsWith( Win32Prefix ) )
				{
				if ( name.length() > 2 )
					{
					char ch1 = name.charAt(0);
					char ch2 = name.charAt(1);
					if ( ch2 == ':'
						&& ( (ch1 >= 'a' && ch1 <= 'z')
							|| (ch1 >= 'A' && ch1 <= 'Z') ) )
						{
						name = name.substring( 2 );
						}
					}
				}
			}

		name = name.replace( File.separatorChar, '/' );

		// No absolute pathnames
		// Windows (and Posix?) paths can start with "\\NetworkDrive\",
		// so we loop on starting /'s.
		
		for ( ; name.startsWith( "/" ) ; )
			name = name.substring( 1 );

 		hdr.linkName = new StringBuffer( "" );

		hdr.name = new StringBuffer( name );

		if ( file.isDirectory() )
			{
			hdr.mode = 040755;
			hdr.linkFlag = TarHeader.LF_DIR;
			if ( hdr.name.charAt( hdr.name.length() - 1 ) != '/' )
				hdr.name.append( "/" );
			}
		else
			{
			hdr.mode = 0100644;
			hdr.linkFlag = TarHeader.LF_NORMAL;
			}

		// UNDONE When File lets us get the userName, use it!

		hdr.size = file.length();
		hdr.modTime = file.lastModified() / 1000;
		hdr.checkSum = 0;
		hdr.devMajor = 0;
		hdr.devMinor = 0;
		}

	/**
	 * If this entry represents a file, and the file is a directory, return
	 * an array of TarEntries for this entry's children.
	 *
	 * @return An array of TarEntry's for this entry's children.
	 */
	public TarEntry[]
	getDirectoryEntries()
		throws InvalidHeaderException
		{
		if ( this.file == null
				|| ! this.file.isDirectory() )
			{
			return new TarEntry[0];
			}

		String[] list = this.file.list();

		TarEntry[] result = new TarEntry[ list.length ];

		for ( int i = 0 ; i < list.length ; ++i )
			{
			result[i] =
				new TarEntry
					( new File( this.file, list[i] ) );
			}

		return result;
		}

	/**
	 * Compute the checksum of a tar entry header.
	 *
	 * @param buf The tar entry's header buffer.
	 * @return The computed checksum.
	 */
	public long
	computeCheckSum( byte[] buf )
		{
		long sum = 0;

		for ( int i = 0 ; i < buf.length ; ++i )
			{
			sum += 255 & buf[ i ];
			}

		return sum;
		}

	/**
	 * Write an entry's header information to a header buffer.
	 *
	 * @param outbuf The tar entry header buffer to fill in.
	 */
	public void
	writeEntryHeader( byte[] outbuf )
		{
		int offset = 0;

		offset = TarHeader.getNameBytes
			( this.header.name, outbuf, offset, TarHeader.NAMELEN );

		offset = TarHeader.getOctalBytes
			( this.header.mode, outbuf, offset, TarHeader.MODELEN );

		offset = TarHeader.getOctalBytes
			( this.header.userId, outbuf, offset, TarHeader.UIDLEN );

		offset = TarHeader.getOctalBytes
			( this.header.groupId, outbuf, offset, TarHeader.GIDLEN );

		long size = this.header.size;

		offset = TarHeader.getLongOctalBytes
			( size, outbuf, offset, TarHeader.SIZELEN );

		offset = TarHeader.getLongOctalBytes
			( this.header.modTime, outbuf, offset, TarHeader.MODTIMELEN );

		int csOffset = offset;
		for ( int c = 0 ; c < TarHeader.CHKSUMLEN ; ++c )
			outbuf[ offset++ ] = (byte) ' ';

		outbuf[ offset++ ] = this.header.linkFlag;

		offset = TarHeader.getNameBytes
			( this.header.linkName, outbuf, offset, TarHeader.NAMELEN );

		offset = TarHeader.getNameBytes
			( this.header.magic, outbuf, offset, TarHeader.MAGICLEN );

		offset = TarHeader.getNameBytes
			( this.header.userName, outbuf, offset, TarHeader.UNAMELEN );

		offset = TarHeader.getNameBytes
			( this.header.groupName, outbuf, offset, TarHeader.GNAMELEN );

		offset = TarHeader.getOctalBytes
			( this.header.devMajor, outbuf, offset, TarHeader.DEVLEN );

		offset = TarHeader.getOctalBytes
			( this.header.devMinor, outbuf, offset, TarHeader.DEVLEN );

		for ( ; offset < outbuf.length ; )
			outbuf[ offset++ ] = 0;

		long checkSum = this.computeCheckSum( outbuf );

		TarHeader.getCheckSumOctalBytes
			( checkSum, outbuf, csOffset, TarHeader.CHKSUMLEN );
		}

	/**
	 * Parse an entry's TarHeader information from a header buffer.
	 *
	 * @param hdr The TarHeader to fill in from the buffer information.
	 * @param header The tar entry header buffer to get information from.
	 */
	public void
	parseTarHeader( TarHeader hdr, byte[] header )
		throws InvalidHeaderException
		{
		int offset = 0;

		hdr.name =
			TarHeader.parseName( header, offset, TarHeader.NAMELEN );

		offset += TarHeader.NAMELEN;

		hdr.mode = (int)
			TarHeader.parseOctal( header, offset, TarHeader.MODELEN );

		offset += TarHeader.MODELEN;

		hdr.userId = (int)
			TarHeader.parseOctal( header, offset, TarHeader.UIDLEN );

		offset += TarHeader.UIDLEN;

		hdr.groupId = (int)
			TarHeader.parseOctal( header, offset, TarHeader.GIDLEN );

		offset += TarHeader.GIDLEN;

		hdr.size =
			TarHeader.parseOctal( header, offset, TarHeader.SIZELEN );

		offset += TarHeader.SIZELEN;

		hdr.modTime =
			TarHeader.parseOctal( header, offset, TarHeader.MODTIMELEN );

		offset += TarHeader.MODTIMELEN;

		hdr.checkSum = (int)
			TarHeader.parseOctal( header, offset, TarHeader.CHKSUMLEN );

		offset += TarHeader.CHKSUMLEN;

		hdr.linkFlag = header[ offset++ ];

		hdr.linkName =
			TarHeader.parseName( header, offset, TarHeader.NAMELEN );

		offset += TarHeader.NAMELEN;

		hdr.magic =
			TarHeader.parseName( header, offset, TarHeader.MAGICLEN );

		offset += TarHeader.MAGICLEN;

		hdr.userName =
			TarHeader.parseName( header, offset, TarHeader.UNAMELEN );

		offset += TarHeader.UNAMELEN;

		hdr.groupName =
			TarHeader.parseName( header, offset, TarHeader.GNAMELEN );

		offset += TarHeader.GNAMELEN;

		hdr.devMajor = (int)
			TarHeader.parseOctal( header, offset, TarHeader.DEVLEN );

		offset += TarHeader.DEVLEN;

		hdr.devMinor = (int)
			TarHeader.parseOctal( header, offset, TarHeader.DEVLEN );
		}

	/**
	 * Fill in a TarHeader given only the entry's name.
	 *
	 * @param hdr The TarHeader to fill in.
	 * @param name The tar entry name.
	 */
	public void
	nameTarHeader( TarHeader hdr, String name )
		{
		boolean isDir = name.endsWith( "/" );

		hdr.checkSum = 0;
		hdr.devMajor = 0;
		hdr.devMinor = 0;

		hdr.name = new StringBuffer( name );
		hdr.mode = isDir ? 040755 : 0100644;
		hdr.userId = 0;
		hdr.groupId = 0;
		hdr.size = 0;
		hdr.checkSum = 0;

		hdr.modTime =
			(new java.util.Date()).getTime() / 1000;

		hdr.linkFlag =
			isDir ? TarHeader.LF_DIR : TarHeader.LF_NORMAL;

		hdr.linkName = new StringBuffer( "" );
		hdr.userName = new StringBuffer( "" );
		hdr.groupName = new StringBuffer( "" );

		hdr.devMajor = 0;
		hdr.devMinor = 0;
		}

	}

