/*
 * DisplayTokenHandler.java - converts tokens to chunks
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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

package org.gjt.sp.jedit.syntax;

//{{{ Imports
import javax.swing.text.*;
import java.awt.font.*;
import java.util.List;
import java.text.BreakIterator;
import java.text.CharacterIterator;
//}}}

/**
 * Creates {@link Chunk} objects that can be painted on screen.
 * @version $Id$
 */
public class DisplayTokenHandler extends DefaultTokenHandler
{
	//{{{ init() method
	/**
	 * Init some variables that will be used when marking tokens.
	 * This is called before {@link org.gjt.sp.jedit.buffer.JEditBuffer#markTokens(int, TokenHandler)}
	 * to store some data that will be required and that we don't want
	 * to put in the parameters
	 *
	 * @param styles
	 * @param fontRenderContext
	 * @param expander
	 * @param out
	 * @param wrapMargin
	 * @param physicalLineOffset offset of the physical lines which these chunks belong to required for implementing elastic tabstops
	 */
	public void init(SyntaxStyle[] styles,
		FontRenderContext fontRenderContext,
		TabExpander expander, List<Chunk> out,
		float wrapMargin, int physicalLineOffset)
	{
		super.init();
		this.styles = styles;
		this.fontRenderContext = fontRenderContext;
		this.expander = expander;
		this.out = out;

		// SILLY: allow for anti-aliased characters' "fuzz"
		if(wrapMargin != 0.0f)
			this.wrapMargin = wrapMargin + 2.0f;
		else
			this.wrapMargin = 0.0f;

		this.physicalLineOffset = physicalLineOffset;
	} //}}}

	//{{{ getChunkList() method
	/**
	 * Returns the list of chunks.
	 * Each element is a head of linked chunks and represents a
	 * screen line.
	 * @since jEdit 4.1pre7
	 */
	public List<Chunk> getChunkList()
	{
		return out;
	} //}}}

	//{{{ handleToken() method
	/**
	 * Called by the token marker when a syntax token has been parsed.
	 * @param seg The segment containing the text
	 * @param id The token type (one of the constants in the
	 * {@link Token} class).
	 * @param offset The start offset of the token
	 * @param length The number of characters in the token
	 * @param context The line context
	 * @since jEdit 4.2pre3
	 */
	@Override
	public void handleToken(Segment seg, byte id, int offset, int length,
		TokenMarker.LineContext context)
	{
		if(id == Token.END)
		{
			makeScreenLine(seg);
			return;
		}

		// first branch to avoid unnecessary instansiation of
		// BreakIterator.
		if(length <= MAX_CHUNK_LEN)
		{
			Chunk chunk = createChunk(id, offset, length, context);
			addToken(chunk, context);
			return;
		}

		// split the token but at character breaks not to affect
		// the result of painting.
		final BreakIterator charBreaker = BreakIterator.getCharacterInstance();
		charBreaker.setText(seg);
		final int tokenBeinIndex = seg.offset + offset;
		final int tokenEndIndex = tokenBeinIndex + length;
		int splitOffset = 0;
		do
		{
			final int beginIndex = tokenBeinIndex + splitOffset;
			int charBreakIndex = charBreaker.preceding(beginIndex + MAX_CHUNK_LEN + 1);
			// {{{ care for unrealistic case, to be complete ...
			// There must be a char break at beginning of token.
			assert charBreakIndex != BreakIterator.DONE;
			if(charBreakIndex <= beginIndex)
			{
				// try splitting after the limit, to
				// make the chunk shorter anyway.
				charBreakIndex = charBreaker.following(beginIndex + MAX_CHUNK_LEN);
				// There must be a char break at end of token.
				assert charBreakIndex != BreakIterator.DONE;
				if(charBreakIndex >= tokenEndIndex)
				{
					// can't split
					break;
				}
			} //}}}
			final int splitLength = charBreakIndex - beginIndex;
			Chunk chunk = createChunk(id, offset + splitOffset,
					splitLength, context);
			addToken(chunk, context);
			splitOffset += splitLength;
		}
		while(splitOffset + MAX_CHUNK_LEN < length);
		Chunk chunk = createChunk(id, offset + splitOffset,
				length - splitOffset, context);
		addToken(chunk, context);
	} //}}}

	//{{{ Private members

	// Don't have chunks longer than a limit to avoid slowing things down.
	// For example, too long chunks are hardly clipped out at rendering.
	private static final int MAX_CHUNK_LEN = 100;

	//{{{ Instance variables
	private SyntaxStyle[] styles;
	private FontRenderContext fontRenderContext;
	private TabExpander expander;
	private List<Chunk> out;
	private float wrapMargin;
	private int physicalLineOffset;
	//}}}

	//{{{ createChunk() method
	private Chunk createChunk(byte id, int offset, int length,
		TokenMarker.LineContext context)
	{
		return new Chunk(id,offset,length,
			getParserRuleSet(context),styles,
			context.rules.getDefault());
	} //}}}

	//{{{ initChunk() method
	private void initChunk(Chunk chunk, float x, Segment lineText)
	{
		chunk.init(lineText,expander,x,fontRenderContext, physicalLineOffset);
	} //}}}

	//{{{ initChunks() method
	private float initChunks(Chunk lineHead, Segment lineText)
	{
		float x = 0.0f;
		for(Chunk chunk = lineHead; chunk != null; chunk = (Chunk)chunk.next)
		{
			initChunk(chunk, x, lineText);
			x += chunk.width;
		}
		return x;
	} //}}}

	//{{{ mergeAdjucentChunks() method
	/**
	 * Merges each adjucent chunks if possible, to reduce the number
	 * of chunks for rendering performance.
	 */
	private void mergeAdjucentChunks(Chunk lineHead, Segment lineText)
	{
		Chunk chunk = lineHead;
		while(chunk.next != null)
		{
			Chunk next = (Chunk)chunk.next;
			if(canMerge(chunk,next,lineText))
			{
				chunk.length += next.length;
				chunk.next = next.next;
			}
			else
			{
				chunk = next;
			}
		}
	} //}}}

	//{{{ canMerge() method
	private static boolean canMerge(Chunk c1, Chunk c2, Segment lineText)
	{
		return c1.style == c2.style
			&& c1.isAccessible() && !c1.isTab(lineText)
			&& c2.isAccessible() && !c2.isTab(lineText)
			&& (c1.length + c2.length) <= MAX_CHUNK_LEN;
	} //}}}

	//{{{ makeWrappedLine() method
	private Chunk makeWrappedLine(Chunk lineHead,
		float virtualIndentWidth, Segment lineText)
	{
		if(virtualIndentWidth > 0)
		{
			final Chunk virtualIndent = new Chunk(virtualIndentWidth,
				lineHead.offset, lineHead.rules);
			initChunk(virtualIndent, 0, lineText);
			virtualIndent.next = lineHead;
			return virtualIndent;
		}
		else
		{
			return lineHead;
		}
	} //}}}

	//{{{ recalculateTabWidth() method
	// Returns true if all chunks are recaluculated and the total
	// width fits in wrap margin.
	private boolean recalculateTabWidthInWrapMargin(Chunk lineHead, Segment lineText)
	{
		float x = 0.0f;
		for(Chunk chunk = lineHead; chunk != null; chunk = (Chunk)chunk.next)
		{
			if(chunk.isTab(lineText))
			{
				initChunk(chunk, x, lineText);
			}
			x += chunk.width;
			if(x > wrapMargin)
			{
				return false;
			}
		}
		return true;
	} //}}}

	//{{{ countLeadingWhitespaces() method
	private static int countLeadingWhitespaces(Segment lineText)
	{
		int count = 0;
		while((count < lineText.count)
			&& Character.isWhitespace(
				lineText.array[lineText.offset + count]))
		{
			++count;
		}
		return count;
	} //}}}

	//{{{ makeScreenLineInWrapMargin() method
	/**
	 * Do the main job for soft wrap feature.
	 */
	private void makeScreenLineInWrapMargin(Chunk lineHead, Segment lineText)
	{
		final int endOfWhitespace = countLeadingWhitespaces(lineText);
		final float virtualIndentWidth = Chunk.offsetToX(lineHead, endOfWhitespace);
		final LineBreaker lineBreaker = new LineBreaker(lineText, endOfWhitespace);
		if(lineBreaker.currentBreak() == LineBreaker.DONE)
		{
			// There is no line break. Can't wrap.
			out.add(lineHead);
			return;
		}
		for(;;)
		{
			final int offsetInMargin = Chunk.xToOffset(lineHead, wrapMargin, false);
			assert offsetInMargin != -1;
			lineBreaker.skipToNearest(offsetInMargin);
			final int lineBreak = lineBreaker.currentBreak();
			if(lineBreak == LineBreaker.DONE)
			{
				// There is no more line break. Can't wrap.
				out.add(lineHead);
				return;
			}
			lineBreaker.advance();
			Chunk linePreEnd = null;
			Chunk lineEnd = lineHead;
			float endX = 0.0f;
			while((lineEnd.offset + lineEnd.length) < lineBreak)
			{
				endX += lineEnd.width;
				linePreEnd = lineEnd;
				lineEnd = (Chunk)lineEnd.next;
			}
			if((lineEnd.offset + lineEnd.length) == lineBreak)
			{
				final Token nextHead = lineEnd.next;
				lineEnd.next = null;
				out.add(lineHead);
				if(nextHead == null)
				{
					return;
				}
				lineHead = (Chunk)nextHead;
			}
			else
			{
				final Chunk shortened = lineEnd.snippetBeforeLineOffset(lineBreak);
				initChunk(shortened, endX, lineText);
				if(linePreEnd != null)
				{
					linePreEnd.next = shortened;
				}
				else
				{
					lineHead = shortened;
				}
				out.add(lineHead);
				Chunk remaining = lineEnd.snippetAfter(shortened.length);
				// {{{ The remaining chunk may be split again.
				// To avoid quadratic repeatation of initChunk() which happens when the
				// wrap margin is too small or the virtual space is too wide, split it
				// using an assumption that the split at a line break doesn't change
				// the widths of parts before and after the break.
				final float remainingRoom = wrapMargin - virtualIndentWidth;
				float processedWidth = shortened.width;
				while (lineEnd.width - processedWidth > remainingRoom
					&& lineBreaker.currentBreak() != LineBreaker.DONE
					&& lineBreaker.currentBreak() < (remaining.offset + remaining.length))
				{
					final int offsetInRoom = lineEnd.xToOffset(processedWidth + remainingRoom, false);
					assert offsetInRoom != -1;
					lineBreaker.skipToNearest(offsetInRoom);
					final int moreBreak = lineBreaker.currentBreak();
					assert moreBreak != LineBreaker.DONE;
					lineBreaker.advance();
					final Chunk moreShortened = remaining.snippetBeforeLineOffset(moreBreak);
					initChunk(moreShortened, virtualIndentWidth, lineText);
					out.add(makeWrappedLine(moreShortened, virtualIndentWidth, lineText));
					remaining = remaining.snippetAfter(moreShortened.length);
					processedWidth += moreShortened.width;
				}
				//}}}
				initChunk(remaining, virtualIndentWidth, lineText);
				remaining.next = lineEnd.next;
				lineHead = remaining;
			}
			lineHead = makeWrappedLine(lineHead, virtualIndentWidth, lineText);
			if(recalculateTabWidthInWrapMargin(lineHead, lineText))
			{
				// Fits in the margin. No more need to wrap.
				out.add(lineHead);
				return;
			}
		}
	} //}}}

	//{{{ makeScreenLine() method
	private void makeScreenLine(Segment lineText)
	{
		if(firstToken == null)
		{
			assert out.isEmpty();
		}
		else
		{
			Chunk lineHead = (Chunk)firstToken;
			mergeAdjucentChunks(lineHead, lineText);
			float endX = initChunks(lineHead, lineText);
			if(wrapMargin > 0.0f && endX > wrapMargin)
			{
				makeScreenLineInWrapMargin(lineHead, lineText);
			}
			else
			{
				out.add(lineHead);
			}
		}
	} //}}}

	//{{{ class LineBreaker
	private static class LineBreaker
	{
		public static final int DONE = -1;

		public LineBreaker(Segment lineText, int startOffset)
		{
			iterator = new LineBreakIterator();
			iterator.setText(lineText);
			offsetOrigin = lineText.offset;
			current = (startOffset < lineText.count)
					? iterator.following(offsetOrigin
							+ startOffset)
					: BreakIterator.DONE;
			next = (current != BreakIterator.DONE)
					? iterator.next()
					: BreakIterator.DONE;
		}

		public int currentBreak()
		{
			return outerOffset(current);
		}

		public void advance()
		{
			current = next;
			next = iterator.next();
		}

		public void skipToNearest(int offset)
		{
			while(next != BreakIterator.DONE
				&& ((next - offsetOrigin) <= offset))
			{
				advance();
			}
		}

		//{{{ Private members
		private final BreakIterator iterator;
		private final int offsetOrigin;
		private int current;
		private int next;

		private int outerOffset(int iteratorOffset)
		{
			return (iteratorOffset != BreakIterator.DONE)
					? (iteratorOffset - offsetOrigin)
					: DONE;
		}
		//}}}
	} //}}}

	//{{{ class LineBreakIterator
	/**
	 * Custom break iterator to unify jEdit's line breaking rules
	 * and natural language rules.
	 */
	private static class LineBreakIterator extends BreakIterator
	{
		public LineBreakIterator()
		{
			base = BreakIterator.getLineInstance();
		}

		private LineBreakIterator(LineBreakIterator other)
		{
			base = (BreakIterator)(other.base.clone());
		}

		@Override
		public Object clone()
		{
			return new LineBreakIterator(this);
		}

		@Override
		public int current()
		{
			int baseBreak = base.current();
			if (isAcceptableBreak(baseBreak))
			{
				return baseBreak;
			}
			// can have reached the end of text during
			// baseOrNext() or baseOrPrevious() which returned
			// DONE.
			// Here, current() should return last() or first()
			// based on which was the last direction.
			return (base.next() == DONE) ? last() : first();
		}

		@Override
		public int first()
		{
			return baseOrNext(base.first());
		}

		@Override
		public int following(int offset)
		{
			return baseOrNext(base.following(offset));
		}

		@Override
		public CharacterIterator getText()
		{
			return base.getText();
		}

		@Override
		public int last()
		{
			return baseOrPrevious(base.last());
		}

		@Override
		public int next()
		{
			return baseOrNext(base.next());
		}

		@Override
		public int next(int n)
		{
			while (n > 1)
			{
				if (next() == DONE)
					return DONE;
				--n;
			}
			return next();
		}

		@Override
		public int previous()
		{
			return baseOrPrevious(base.previous());
		}

		@Override
		public void setText(CharacterIterator newText)
		{
			base.setText(newText);
			baseOrNext(base.first());
		}

		private final BreakIterator base;

		private int baseOrNext(int baseBreak)
		{
			while(!isAcceptableBreak(baseBreak))
				baseBreak = base.next();
			return baseBreak;
		}

		private int baseOrPrevious(int baseBreak)
		{
			while(!isAcceptableBreak(baseBreak))
				baseBreak = base.previous();
			return baseBreak;
		}

		private boolean isAcceptableBreak(int baseBreak)
		{
			if (baseBreak == DONE)
				return true;
			CharacterIterator text = getText();
			if (baseBreak <= text.getBeginIndex() || baseBreak > text.getEndIndex())
				return true;
			// get characters surrounding the break without
			// altering the current index of underlying text.
			int originalIndex = text.getIndex();
			char next = text.setIndex(baseBreak);
			char prev = text.previous();
			text.setIndex(originalIndex);
			// When breaking at whitespace, jEdit treat the
			// whitespaces as belonging to the previous line and
			// make them editable.
			return !Character.isWhitespace(next)
				// Assuming that breaking without white spaces
				// are wanted only for some natural languages
				// which uses non-ASCII characters. Otherwise
				// keep traditional jEdit behavior (break only
				// at whitespaces).
				&& (Character.isWhitespace(prev)
					|| prev > 0x7f || next > 0x7f)
				// Workarounds for the problem reported at
				// SF.net bug #3488310; unexpected soft wrap
				// happens at closing "&ldquo;".
				// Probably the cause is in the implementation
				// of BreakIterator for line breaks. Some
				// similer problems are also reported in
				// bugs.sun.com.
				// http://www.google.co.jp/search?q=site%3Abugs.sun.com+BreakIterator+getLineInstance
				// There seems to be some problems in handling
				// of quotation marks.
				&& !isUnacceptableBreakInsideQuote(baseBreak,
					text, prev, next);
		}

		// Retrieves char at specified index without altering
		// the current index of CharacterIterator.
		private static char charAt(CharacterIterator text, int index)
		{
			int originalIndex = text.getIndex();
			char c = text.setIndex(index);
			text.setIndex(originalIndex);
			return c;
		}

		private static boolean isUnacceptableBreakInsideQuote(
			int baseBreak, CharacterIterator text,
			char prev, char next)
		{
			// The following quotation marks are accumulated
			// cases that exhibits the problem under a local
			// test on JRE 7u3 with samples taken from Wikipedia.
			// http://en.wikipedia.org/wiki/Non-English_usage_of_quotation_marks
			//
			// The last check for enclosing whitespace avoids
			// unwanted rejection of line breaks in CJK text
			// (which don't have such whitespace) where default
			// behavior of BreakIterator is reasonable.
			//
			if ("”’»›".indexOf(prev) >= 0
				&& !Character.isWhitespace(next))
			{
				int beforeQuote = baseBreak - 2;
				return beforeQuote < text.getBeginIndex()
					|| Character.isWhitespace(charAt(text,
						beforeQuote));
			}
			else if (!Character.isWhitespace(prev)
					&& "“„‘‚«‹".indexOf(next) >= 0)
			{
				int afterQuote = baseBreak + 1;
				return afterQuote >= text.getEndIndex()
					|| Character.isWhitespace(charAt(text,
						afterQuote));
			}
			return false;
		}
	} //}}}

	//}}}
}
