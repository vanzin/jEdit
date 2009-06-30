/*
 * PropertiesBean.java: a "serializable" Java Bean that uses a
 *                      java.util.Properties backend.
 * :noTabs=false:
 *
 * Copyright (C) 2006 Marcelo Vanzin
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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * A "java bean" that can serialize itself into a java.util.Properties
 * instance. For the serialization, the class uses the java beans
 * instrospection mechanism to figure out the class's available
 * properties, and saves all the properties as strings in the properties
 * object.
 *
 * <p>Properties are saved based on a "root", which is set up during the
 * instantiation of the object. The properties will be set as
 * <code>root.property_name</code>.</p>
 *
 * <p>Only native types (boolean, char, double, float, int, long, short),
 * Strings, and arrays of those types are supported. Also, nested
 * beans are not supported presently.</p>
 *
 * @author Marcelo Vanzin
 * @since jEdit 4.3pre7
 */
public abstract class PropertiesBean
{

	// Constructors

	/**
	 * Creates a new instance with the given root and the default array
	 * separator char (':').
	 *
	 * @param root A non-null string that will be the "root" of the
	 *             serialized properties.
	 */
	protected PropertiesBean(String root)
	{
		this(root, ':');
	}

	/**
	 * Creates a new instance with the given root and the given array
	 * separator character.
	 *
	 * @param root A non-null string that will be the "root" of the
	 *             serialized properties.
	 * @param arraysep A character that will be used to define the
	 *                 separator of elements of an array property.
	 */
	protected PropertiesBean(String root, char arraysep)
	{
		if (root == null)
			throw new IllegalArgumentException("root cannot be null");
		this.root = root;
		this.arraysep = arraysep;
	}

	// Public methods

	/**
	 * Loads the bean's properties from the given object.
	 */
	public void load(Properties p)
	{
		try
		{
			PropertyDescriptor[] _props = getPropertyDescriptors();
			for (int i = 0; i < _props.length; i++)
			{
				if ("class".equals(_props[i].getName()))
					continue;

				Method _set = _props[i].getWriteMethod();
				if (_set != null)
				{
					String _pname = root + "." + _props[i].getName();
					Object _val = p.getProperty(_pname);
					if (_val != null)
						_val = parse((String)_val, _props[i].getPropertyType());
					try
					{
						_set.invoke(this, _val);
					}
					catch (IllegalArgumentException iae)
					{
						/* Ignore these. */
					}
				}
			}
		}
		catch (Exception e)
		{
			// These exceptions shouldn't occur during normal runtime,
			// so we catch them and print an error message. Users of this
			// class should fix these before releasing the code.
			Log.log(Log.ERROR, this, e);
		}
	}

	/**
	 * Saves the bean's properties into the given object.
	 */
	public void save(Properties p)
	{
		try
		{
			PropertyDescriptor[] _props = getPropertyDescriptors();
			for (int i = 0; i < _props.length; i++)
			{
				if ("class".equals(_props[i].getName()))
					continue;

				Method _get = _props[i].getReadMethod();
				if (_get != null)
				{
					Object _val = _get.invoke(this);
					String _pname = root + "." + _props[i].getName();
					if (_val != null)
						p.setProperty(_pname, encode(_val));
					else
						p.remove(_pname);
				}
			}
		}
		catch (Exception e)
		{
			// These exceptions shouldn't occur during normal runtime,
			// so we catch them and print an error message. Users of this
			// class should fix these before releasing the code.
			Log.log(Log.ERROR, this, e);
		}
	}

	/**
	 * Cleans the entries related to this object from the given object.
	 */
	public void clean(Properties p)
	{

		try
		{
			PropertyDescriptor[] _props = getPropertyDescriptors();
			for (int i = 0; i < _props.length; i++)
			{
				if ("class".equals(_props[i].getName()))
					continue;

				String _pname = root + "." + _props[i].getName();
				p.remove(_pname);
			}
		}
		catch (Exception e)
		{
			// These exceptions shouldn't occur during normal runtime,
			// so we catch them and print an error message. Users of this
			// class should fix these before releasing the code.
			Log.log(Log.ERROR, this, e);
		}
	}

	// Private methods

	private PropertyDescriptor[] getPropertyDescriptors()
		throws IntrospectionException
	{
		BeanInfo _info = Introspector.getBeanInfo(getClass());
		return _info.getPropertyDescriptors();
	}

	private String encode(Object value)
	{
		Class _class = value.getClass();
		if (_class.isArray())
		{
			StringBuilder _val = new StringBuilder();
			int _len = Array.getLength(value);
			for (int i = 0; i < _len; i++)
			{
				String _str = encode(Array.get(value, i));
				if (_str == null)
					return null;
				_val.append(_str);
				if (i < _len - 1)
					_val.append(arraysep);
			}
			return _val.toString();
		}
		else
		{
			// just make sure it's a supported type.
			if (_class != Boolean.class && _class != Boolean.TYPE
			    && _class != Character.class && _class != Character.TYPE
			    && _class != Double.class && _class != Double.TYPE
			    && _class != Float.class && _class != Float.TYPE
			    && _class != Integer.class && _class != Integer.TYPE
			    && _class != Long.class && _class != Long.TYPE
			    && _class != Short.class && _class != Short.TYPE
			    && _class != String.class)
			{
				Log.log(Log.WARNING, this, "unsupported type: " + _class.getName());
				return null;
			}
			return value.toString();
		}
	}

	private Object parse(String value, Class<?> _class)
	{
		Object _ret = null;
		if (_class.isArray())
		{
			StringTokenizer st = new StringTokenizer(value, String.valueOf(arraysep));
			Class _type = _class.getComponentType();
			_ret = Array.newInstance(_type, st.countTokens());
			int _cnt = st.countTokens();
			for (int i = 0; i < _cnt; i++)
			{
				Object _val = parse(st.nextToken(), _type);
				if (_val == null)
					return null;
				Array.set(_ret, i, _val);
			}
		}
		else
		{
			if (_class == Boolean.class || _class == Boolean.TYPE)
				_ret = Boolean.valueOf(value);
			else if (_class == Character.class || _class == Character.TYPE)
				_ret = Character.valueOf(value.charAt(0));
			else if (_class == Double.class || _class == Double.TYPE)
				_ret = Double.valueOf(value);
			else if (_class == Float.class || _class == Float.TYPE)
				_ret = Float.valueOf(value);
			else if (_class == Integer.class || _class == Integer.TYPE)
				_ret = Integer.valueOf(value);
			else if (_class == Long.class || _class == Long.TYPE)
				_ret = Long.valueOf(value);
			else if (_class == Short.class || _class == Short.TYPE)
				_ret = Short.valueOf(value);
			else if (_class == String.class)
				_ret = value;
			else
				Log.log(Log.WARNING, this, "unsupported type: " + _class.getName());

		}
		return _ret;
	}

	// Instance variables

	private final char		arraysep;
	private final String 	root;

}

