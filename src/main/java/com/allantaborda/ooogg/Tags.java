/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class represents a Vorbis Comments tag set. This metadata structure is used in most common audio and video OGG-based file formats, such as Vorbis, Speex and Theora.
 * @author Allan Taborda dos Santos
 */
public class Tags implements OggPackable{
	/** Default application vendor, if no application vendor is specified. */
	private static final String defaultVendor = "OOOGG - Object-Oriented OGG Container";
	/** The set of properties. Each property can have one or more values. */
	private Map<String, List<String>> comments = new HashMap<>();
	/** The application vendor field. */
	private String vendorField = "";
	/** The packet header, according the specification for Vorbis Comments for the file format. It must be null if no packet header in specification. */
	private String packetHeader;
	/** Flag indicating framing bit inclusion after the properties. */
	private boolean includeFramingBit;
	/** Flag indicating if data contained in this data structure is valid. */
	private boolean valid = true;

	/** Creates a Vorbis Comments tag set with no packet header and no framing bit. */
	public Tags(){
		this(null, false);
	}

	/**
	 * Creates a Vorbis Comments tag set with specified packet header and no framing bit.
	 * @param header The packet header.
	 */
	public Tags(String header){
		this(header, false);
	}

	/**
	 * Creates a Vorbis Comments tag set with specified packet header and if includes framing bit or not.
	 * @param header The packet header.
	 * @param framingBit {@code true} if includes framing bit, {@code false} otherwise.
	 */
	public Tags(String header, boolean framingBit){
		packetHeader = header;
		includeFramingBit = framingBit;
	}

	/**
	 * Returns a list with all keys contained in the tag set. The returned list is immutable.
	 * @return A list with all keys contained in the tag set.
	 */
	public List<String> getKeys(){
		return new CommentList(comments.keySet().toArray(new String[comments.size()]));
	}

	/**
	 * Given the key, returns a list with all values associated with this key. If no values are associated with the key, {@code null} is returned.
	 * @param key The key (e.g., TITLE, ALBUM, ARTIST, etc.)
	 * @return The list with all values associated with this key.
	 */
	public List<String> getList(String key){
		List<String> coms = comments.get(key.toUpperCase());
		return coms == null ? new CommentList(null) : new CommentList(coms.toArray(new String[coms.size()]));
	}

	/**
	 * Given the key, returns a string with all values associated with this key, with each value
	 * separated by a semicolon. If no values are associated with the key, {@code null} is returned.
	 * @param key The key (e.g., TITLE, ALBUM, ARTIST, etc.)
	 * @return A string with all values associated with this key.
	 */
	public String getString(String key){
		List<String> lst = comments.get(key.toUpperCase());
		if(lst == null) return null;
		StringBuilder sb = new StringBuilder(512);
		int c = 0;
		for(String s : lst){
			sb.append(s);
			if(++c != lst.size()) sb.append("; ");
		}
		return sb.toString();
	}

	/**
	 * Given the key, associates a new value to this key. If the value contains one or more semicolons, it will be splitted
	 * in two or more values, and all are associated to key.
	 * @param key The key (e.g., TITLE, ALBUM, ARTIST, etc.)
	 * @param value The value (or the values, if the string contains one or more semicolons) to be associated with the key.
	 */
	public void add(String key, String value){
		if(value.contains(";")) addAll(key, new CommentList(value.split(";")));
		else{
			key = key.toUpperCase();
			List<String> lst = comments.get(key);
			if(lst == null) lst = new LinkedList<>();
			lst.add(value.trim());
			comments.put(key, lst);
		}
	}

	/**
	 * Given the key, associates a list of new values to this key.
	 * @param key The key (e.g., TITLE, ALBUM, ARTIST, etc.)
	 * @param values The values to be associated with the key.
	 */
	public void addAll(String key, List<String> values){
		for(String value : values) add(key, value);
	}

	/**
	 * Given the key, removes all values associated to this key.
	 * @param key The key (e.g., TITLE, ALBUM, ARTIST, etc.)
	 */
	public void removeAll(String key){
		comments.remove(key.toUpperCase());
	}

	/**
	 * Returns the application vendor field.
	 * @return The application vendor field.
	 */
	public String getVendor(){
		return vendorField.trim().isEmpty() ? defaultVendor : vendorField;
	}

	/**
	 * Changes the value for the application vendor field.
	 * @param vendor The new value for the application vendor field.
	 */
	public void setVendor(String vendor){
		vendorField = vendor == null ? "" : vendor;
	}

	/**
	 * Returns the packet header.
	 * @return The packet header.
	 */
	public String getPacketHeader(){
		return packetHeader;
	}

	/**
	 * Returns the flag indicating framing bit inclusion after the properties.
	 * @return The flag indicating framing bit inclusion after the properties.
	 */
	public boolean isIncludeFramingBit(){
		return includeFramingBit;
	}

	/**
	 * Given a map, writes all properties in this map. If one or more keys have more than one value associated to them, the values are stored separated by semicolons.
	 * @param props The map where all properties will be written.
	 */
	public void writeIntoMap(Map<String, Object> props){
		for(String key : comments.keySet()){
			String value = getString(key);
			if(value != null) props.put(key, value);
		}
	}

	public boolean isValid(){
		return valid;
	}

	/**
	 * Loads the content from an OGG packet.
	 * @param packet The OGG packet in which data will be loaded.
	 */
	public void fromOggPacket(OggPacket packet){
		byte[] content = packet.getContent();
		int pos = packetHeader == null ? 0 : packetHeader.length();
		if(pos > 0){
			byte[] b = new byte[pos];
			System.arraycopy(content, 0, b, 0, pos);
			if(!packetHeader.equals(new String(b))) valid = false;
		}
		if(valid){
			byte[] b = new byte[4];
			System.arraycopy(content, pos, b, 0, b.length);
			pos += b.length;
			b = new byte[OggUtils.getIntFromByteArray(b)];
			System.arraycopy(content, pos, b, 0, b.length);
			pos += b.length;
			setVendor(new String(b, UTF_8));
			b = new byte[4];
			System.arraycopy(content, pos, b, 0, b.length);
			pos += b.length;
			for(int c = 0, nrValues = OggUtils.getIntFromByteArray(b); c < nrValues; c++){
				b = new byte[4];
				System.arraycopy(content, pos, b, 0, b.length);
				pos += b.length;
				b = new byte[OggUtils.getIntFromByteArray(b)];
				System.arraycopy(content, pos, b, 0, b.length);
				pos += b.length;
				String kv = new String(b, UTF_8);
				int eq = kv.indexOf('=');
				add(kv.substring(0, eq), kv.substring(eq + 1));
			}
		}
		if(includeFramingBit){
			if(packet.getSize() <= pos || content[pos] != (byte) 0x01 || packet.getSize() > ++pos) valid = false;
		}else if(packet.getSize() > pos) valid = false;
	}

	public OggPacket toOggPacket(){
		List<Byte> bs = new LinkedList<>();
		if(packetHeader != null && !packetHeader.isEmpty()) for(byte b : packetHeader.getBytes()) bs.add(b);
		addField(bs, getVendor());
		List<String> coms = new LinkedList<>();
		for(String key : comments.keySet()) for(String com : comments.get(key)) coms.add(key + "=" + com);
		for(byte b : OggUtils.getByteArrayFromInt(coms.size())) bs.add(b);
		for(String com : coms) addField(bs, com);
		if(includeFramingBit) bs.add((byte) 0x01);
		byte[] pc = new byte[bs.size()];
		int c = -1;
		for(Byte b : bs) pc[++c] = b;
		return new OggPacket(pc);
	}

	/**
	 * Internal method that stores a field content in a list of bytes. Used in {@link #toOggPacket()} method.
	 * @param bs The list of bytes.
	 * @param field The field to me stored in the list of bytes.
	 */
	private void addField(List<Byte> bs, String field){
		byte[] f = field.getBytes(UTF_8);
		for(byte b : OggUtils.getByteArrayFromInt(f.length)) bs.add(b);
		for(byte b : f) bs.add(b);
	}

	/** A simple immutable list that encapsulates an array of strings. */
	private class CommentList extends AbstractList<String>{
		/** The encapsulated array of strings */
		private String[] values;

		/**
		 * Creates a list with the given array of Strings.
		 * @param coms An array of strings.
		 */
		CommentList(String[] coms){
			values = coms;
		}

		public String get(int index){
			if(values == null) throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
			if(index < 0 || index >= values.length) throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + values.length);
			return values[index];
		}

		public int size(){
			return values == null ? 0 : values.length;
		}
	}
}