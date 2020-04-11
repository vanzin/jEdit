/*
 * :noTabs=false:
 *
 * Copyright (C) 2008 Kazutoshi Satoda
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.gjt.sp.util;

import javax.annotation.Nonnull;

/**
 * Reversed view of a given CharSequence.
 */
public class ReverseCharSequence implements CharSequence
{
	public ReverseCharSequence(CharSequence base)
	{
		this.base = base;
	}

	public CharSequence baseSequence()
	{
		return base;
	}

	@Override
	public char charAt(int index)
	{
		return base.charAt(base.length() - index - 1);
	}

	@Override
	public int length()
	{
		return base.length();
	}

	@Override
	public CharSequence subSequence(int start, int end)
	{
		int baseLength = base.length();
		return new ReverseCharSequence(
			base.subSequence(baseLength - end, baseLength - start));
	}

	@Nonnull
	public String toString()
	{
		int baseLength = base.length();
		StringBuilder builder = new StringBuilder(baseLength);
		for (int i = baseLength - 1; i >= 0; --i)
		{
			builder.append(base.charAt(i));
		}
		return builder.toString();
	}

	private final CharSequence base;
}
