/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) 2009-2010 Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package fi.opc.ua.server;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.Enumeration;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;

/**
 * A sample enumeration type to be used with an OPC UA DataType.
 * <p>
 * OPC UA enum types are expected to be zero-based, i.e. the first element is
 * supposed to correspond to 0. getEnumStrings() here is coded so that it will
 * return null values, though, if not all integer in between values are defined.
 * <p>
 * Note that it is usually easier to define new data types and use them with the
 * information model files and code generation, based on those.
 * <p>
 * The various {@link #valueOf} methods are useful for converting integer values
 * to the enumeration. In OPC UA communication, the enumeration values are
 * always passed as integers.
 */
public enum ASEnumType implements Enumeration {
	One(1), Three(3), Two(2), Zero(0);

	public static EnumSet<ASEnumType> ALL = EnumSet.allOf(ASEnumType.class);
	public static EnumSet<ASEnumType> NONE = EnumSet.noneOf(ASEnumType.class);

	private static final Map<Integer, ASEnumType> map;

	static {
		map = new HashMap<Integer, ASEnumType>();
		for (ASEnumType i : ASEnumType.values())
			map.put(i.value, i);
	}

	public static LocalizedText[] getEnumStrings() {
		ASEnumType[] values = ASEnumType.values();
		List<LocalizedText> enumStrings = new ArrayList<LocalizedText>(
				values.length);
		for (ASEnumType t : values) {
			int index = t.getValue();
			while (enumStrings.size() < (index + 1))
				enumStrings.add(null);
			enumStrings.set(index, new LocalizedText(t.name(),
					LocalizedText.NO_LOCALE));
		}
		return enumStrings.toArray(new LocalizedText[enumStrings.size()]);
	}

	public static ASEnumType valueOf(int value) {
		return map.get(value);
	}

	public static ASEnumType[] valueOf(int[] value) {
		ASEnumType[] result = new ASEnumType[value.length];
		for (int i = 0; i < value.length; i++)
			result[i] = valueOf(value[i]);
		return result;
	}

	public static ASEnumType valueOf(Integer value) {
		return value == null ? null : valueOf(value.intValue());
	}

	public static ASEnumType[] valueOf(Integer[] value) {
		ASEnumType[] result = new ASEnumType[value.length];
		for (int i = 0; i < value.length; i++)
			result[i] = valueOf(value[i]);
		return result;
	}

	public static ASEnumType valueOf(UnsignedInteger value) {
		return value == null ? null : valueOf(value.intValue());
	}

	public static ASEnumType[] valueOf(UnsignedInteger[] value) {
		ASEnumType[] result = new ASEnumType[value.length];
		for (int i = 0; i < value.length; i++)
			result[i] = valueOf(value[i]);
		return result;
	}

	private int value;

	private ASEnumType(int value) {
		this.value = value;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.opcfoundation.ua.builtintypes.Enumeration#getValue()
	 */
	@Override
	public int getValue() {
		return value;
	}
}
