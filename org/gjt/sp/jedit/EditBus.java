/*
 * EditBus.java - The EditBus
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 Slava Pestov
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

package org.gjt.sp.jedit;

import java.awt.*;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import org.gjt.sp.util.Log;

/**
 * jEdit's global event notification mechanism.<p>
 *
 * Plugins register with the EditBus to receive messages reflecting
 * changes in the application's state, including changes in buffers,
 * views and edit panes, changes in the set of properties maintained
 * by the application, and the closing of the application.<p>
 *
 * The EditBus maintains a list of objects that have requested to receive
 * messages. When a message is sent using this class, all registered
 * components receive it in turn. Classes for objects that subscribe to
 * the EditBus must implement the {@link EBComponent} interface, which
 * defines the single method {@link EBComponent#handleMessage(EBMessage)}.<p>
 *
 * Alternatively, since jEdit4.3pre19, EditBus components can be any
 * object. Handlers for EditBus messages are created by annotating
 * methods with the {@link EBHandler} annotation. Such methods should
 * expect a single parameter - an edit bus message of any desired type.
 * If a message matching the type (or any of its super-types, unless the
 * annotation requests exact type matching) is being sent, the annotated
 * method will be called instead of the default {@link
 * EBComponent#handleMessage(EBMessage)}. If a handler exists for a
 * specific message type, the default handler will not be called.<p>
 *
 * A plugin core class that extends the
 * {@link EBPlugin} abstract class (and whose name ends with
 * <code>Plugin</code> for identification purposes) will automatically be
 * added to the EditBus during jEdit's startup routine.  Any other
 * class - for example, a dockable window that needs to receive
 * notification of buffer changes - must perform its own registration by calling
 * {@link #addToBus(Object)} during its initialization.
 * A convenient place to register in a class derived from <code>JComponent</code>
 * would be in an implementation of the <code>JComponent</code> method
 * <code>addNotify()</code>.<p>
 *
 * Message types sent by jEdit can be found in the
 * {@link org.gjt.sp.jedit.msg} package.<p>
 *
 * Plugins can also send their own messages - any object can send a message to
 * the EditBus by calling the static method {@link #send(EBMessage)}.
 * Most plugins, however, only concern themselves with receiving, not
 * sending, messages.
 *
 * @see org.gjt.sp.jedit.EBComponent
 * @see org.gjt.sp.jedit.EBMessage
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class EditBus
{

	//{{{ EBHandler annotation
	/**
	 * This annotation should be used in methods that are to be
	 * considered "edit bus message handlers". When registering
	 * an object using {@link #addToBus(Object)}, all methods
	 * tagged with this annotation will be considered as handlers
	 * for specific edit bus messages.<p>
	 *
	 * Each method should expect a single argument (an object of
	 * some type derived from EBMessage, inclusive). When
	 * delivering an EBMessage, the bus will search for and invoke
	 * all handlers matching the outgoing message type.<p>
	 *
	 * Since jEdit 4.4pre1, this annotation can also be added to
	 * classes extending EditPlugin. This will make the plugin
	 * be added to the bus automatically, similarly to how
	 * EBPlugin works, but without having to implement the
	 * EBComponent interface.
	 *
	 * @since jEdit 4.3pre19
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE, ElementType.METHOD})
	public static @interface EBHandler
	{
		/**
		 * Whether the message should match the exact type of
		 * the parameter, instead of a compatible type.
		 */
		boolean exact() default false;
	} //}}}

	//{{{ addToBus() method
	/**
	 * Adds a component to the bus. It will receive all messages sent
	 * on the bus.
	 *
	 * @param comp The component to add
	 */
	public static void addToBus(EBComponent comp)
	{
		addToBus((Object)comp);
	} //}}}

	//{{{ addToBus() method
	/**
	 * Adds a component to the bus. Methods annotated with the
	 * {@link EBHandler} annotation found in the component will
	 * be used as EditBus message handlers if a message of a
	 * matching type is sent on the bus.<p>
	 *
	 * If the component implements {@link EBComponent}, then the
	 * {@link EBComponent#handleMessage(EBMessage)} method will be
	 * called for every message sent on the bus.
	 *
	 * @param comp The component to add
	 *
	 * @since jEdit 4.3pre19
	 */
	public static void addToBus(Object comp)
	{
		components.addComponent(comp);
	} //}}}

	//{{{ removeFromBus() method
	/**
	 * Removes a component from the bus.
	 * @param comp The component to remove
	 */
	public static void removeFromBus(EBComponent comp)
	{
		removeFromBus((Object) comp);
	} //}}}

	//{{{ removeFromBus() method
	/**
	 * Removes a component from the bus.
	 * @param comp The component to remove
	 * @since 4.3pre19
	 */
	public static void removeFromBus(Object comp)
	{
		components.removeComponent(comp);
	} //}}}

	//{{{ send() method
	/**
	 * Sends a message to all components on the bus in turn.
	 * The message is delivered to components in the AWT thread,
	 * and this method will wait until all handlers receive the
	 * message before returning.
	 *
	 * <p><b>NOTE:</b>
	 * If the calling thread is not the AWT thread and the
	 * thread is interrupted before or while the call of this
	 * method, this method can return before completion of handlers.
	 * However, the interruption state is set in this case, so the
	 * caller can detect the interruption after the call. If you
	 * really need the completion of handlers, you should make sure
	 * the call is in the AWT thread or the calling thread is never
	 * interrupted. If you don't care about the completion of
	 * handlers, it is recommended to use
	 * {@link #sendAsync(EBMessage)} instead.
	 * </p>
	 *
	 * @param message The message
	 */
	public static void send(EBMessage message)
	{
		Runnable sender = new SendMessage(message);

		if (EventQueue.isDispatchThread())
		{
			sender.run();
			return;
		}

		/*
		 * We can't throw any checked exceptions from this
		 * method. It will break all source that currently
		 * expects this method to not throw them. So we catch
		 * them and log them instead.
		 */
		boolean interrupted = false;
		try
		{
			EventQueue.invokeAndWait(sender);
		}
		catch (InterruptedException ie)
		{
			interrupted = true;
			Log.log(Log.ERROR, EditBus.class, ie);
		}
		catch (InvocationTargetException ite)
		{
			Log.log(Log.ERROR, EditBus.class, ite);
		}
		finally
		{
			if (interrupted)
			{
				Thread.currentThread().interrupt();
			}
		}
	} //}}}

	//{{{ sendAsync() method
	/**
	 * Schedules a message to be sent on the edit bus as soon as
	 * the AWT thread is done processing current events. The
	 * method returns immediately (i.e., before the message is
	 * sent).
	 *
	 * @param message The message
	 *
	 * @since jEdit 4.4pre1
	 */
	public static void sendAsync(EBMessage message)
	{
		EventQueue.invokeLater(new SendMessage(message));
	} //}}}

	//{{{ Private members
	private static final HandlerList components = new HandlerList();

	// can't create new instances
	private EditBus() {}

	//{{{ dispatch() method
	private static void dispatch(EBMessageHandler emh,
				     EBMessage msg)
		throws Exception
	{
		if (emh.handler != null)
			emh.handler.invoke(emh.comp, msg);
		else
		{
			assert (emh.comp instanceof EBComponent);
			((EBComponent)emh.comp).handleMessage(msg);
		}
	} //}}}

	//{{{ sendImpl() method
	private static void sendImpl(EBMessage message)
	{
		boolean isExact = true;
		Class<?> type = message.getClass();
		while (!type.equals(Object.class))
		{
			List<EBMessageHandler> handlers = components.get(type);
			if (handlers != null)
			{
				try
				{
					for (EBMessageHandler emh : handlers)
					{
						if (!isExact &&
						    emh.source != null &&
						    emh.source.exact())
						{
							continue;
						}
						if(Debug.EB_TIMER)
						{
							long start = System.nanoTime();
							dispatch(emh, message);
							long time = System.nanoTime() - start;
							if(time >= 1000000)
							{
								Log.log(Log.DEBUG,EditBus.class,emh.comp + ": " + time + " ns");
							}
						}
						else
							dispatch(emh, message);
					}
				}
				catch(Throwable t)
				{
					Log.log(Log.ERROR,EditBus.class,"Exception"
						+ " while sending message on EditBus:");
					Log.log(Log.ERROR,EditBus.class,t);
				}
			}
			type = type.getSuperclass();
			isExact = false;
		}
	} //}}}

	//}}}

	//{{{ EBMessageHandler class
	private static class EBMessageHandler
	{

		EBMessageHandler(Object comp,
				 Method handler,
				 EBHandler source)
		{
			this.comp = comp;
			this.handler = handler;
			this.source = source;
		}

		Object comp;
		Method handler;
		EBHandler source;
	} //}}}

	//{{{ HandlerList class
	/**
	 * A "special" hash map that has some optimizations for use by
	 * the EditBus. Notably, it allows setting a "read only" mode
	 * where modifications to the map are postponed until the map
	 * is unlocked.
	 */
	private static class HandlerList
		extends HashMap<Class<?>, List<EBMessageHandler>>
	{

		public List<EBMessageHandler> safeGet(Class<?> type)
		{
			List<EBMessageHandler> lst = super.get(type);
			if (lst == null) {
				lst = new LinkedList<EBMessageHandler>();
				super.put(type, lst);
			}
			return lst;
		}


		public synchronized void lock()
		{
			lock++;
		}


		public synchronized void unlock()
		{
			lock--;
			if (lock == 0)
			{
				for (Object comp : add)
					addComponent(comp);
				for (Object comp : remove)
					removeComponent(comp);
				add.clear();
				remove.clear();
			}
		}


		public synchronized void removeComponent(Object comp)
		{
			if (lock != 0)
			{
				remove.add(comp);
				return;
			}

			for (Map.Entry<Class<?>, List<EBMessageHandler>> entry: entrySet())
			{
				Class<?> msg = entry.getKey();
				List<EBMessageHandler> handlers = entry.getValue();
				if (handlers == null)
					continue;
				for (Iterator<EBMessageHandler> it = handlers.iterator();
				     it.hasNext(); )
				{
					EBMessageHandler emh = it.next();
					if (emh.comp == comp)
						it.remove();
				}
			}
		}


		public synchronized void addComponent(Object comp)
		{
			if (lock != 0)
			{
				add.add(comp);
				return;
			}

			for (Method m : comp.getClass().getMethods())
			{
				EBHandler source = m.getAnnotation(EBHandler.class);
				if (source == null)
					continue;

				Class[] params = m.getParameterTypes();

				if (params.length != 1)
				{
					Log.log(Log.ERROR, EditBus.class,
						"Invalid EBHandler method " + m.getName() +
						" in class " + comp.getClass().getName() +
						": too many parameters.");
					continue;
				}

				if (!EBMessage.class.isAssignableFrom(params[0]))
				{
					Log.log(Log.ERROR, EditBus.class,
						"Invalid parameter " + params[0].getName() +
						" in method " + m.getName() +
						" of class " + comp.getClass().getName());
					continue;
				}

				synchronized (components)
				{
					safeGet(params[0]).add(new EBMessageHandler(comp, m, source));
				}
			}

			/*
			 * If the component implements EBComponent, then add the
			 * default handler for backwards compatibility.
			 */
			if (comp instanceof EBComponent)
				safeGet(EBMessage.class).add(new EBMessageHandler(comp, null, null));
		}


		private int lock;
		private List<Object> add = new LinkedList<Object>();
		private List<Object> remove = new LinkedList<Object>();
	} //}}}

	//{{{ SendMessage class
	private static class SendMessage implements Runnable
	{

		public SendMessage(EBMessage message)
		{
			this.message = message;
		}


		public void run()
		{
			Log.log(Log.DEBUG,EditBus.class,message.toString());

			components.lock();
			try
			{
				sendImpl(message);
			}
			finally
			{
				components.unlock();
			}
		}

		private EBMessage message;
	} //}}}

}
