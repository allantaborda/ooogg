/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg;

import java.util.AbstractList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Tags implements OggPackable{
	private static String defaultVendor = "OOOGG - Object-Oriented OGG Container";
	private Map<String, List<String>> comments = new HashMap<>();
	private String vendorField = "", packetHeader;
	private boolean includeFramingBit, valid = true;

	public Tags(){
		this(null, false);
	}

	public Tags(String header){
		this(header, false);
	}

	public Tags(String header, boolean framingBit){
		packetHeader = header;
		includeFramingBit = framingBit;
	}

	public List<String> getKeys(){
		return new CommentList(comments.keySet().toArray(new String[comments.size()]));
	}

	public List<String> getList(String key){
		List<String> coms = comments.get(key.toUpperCase());
		return coms == null ? new CommentList(null) : new CommentList(coms.toArray(new String[coms.size()]));
	}

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

	public void addAll(String key, List<String> values){
		for(String value : values) add(key, value);
	}

	public void removeAll(String key){
		comments.remove(key.toUpperCase());
	}

	public String getVendor(){
		return vendorField.trim().isEmpty() ? defaultVendor : vendorField;
	}

	public void setVendor(String vendor){
		vendorField = vendor == null ? "" : vendor;
	}

	public String getPacketHeader(){
		return packetHeader;
	}

	public boolean isIncludeFramingBit(){
		return includeFramingBit;
	}

	public void writeIntoMap(Map<String, Object> props){
		for(String key : comments.keySet()){
			String value = getString(key);
			if(value != null) props.put(key, value);
		}
	}

	public boolean isValid(){
		return valid;
	}

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
			setVendor(OggUtils.getStringFromUTF8Bytes(b));
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
				String kv = OggUtils.getStringFromUTF8Bytes(b);
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

	private void addField(List<Byte> bs, String field){
		byte[] f = OggUtils.getUTF8BytesFromString(field);
		for(byte b : OggUtils.getByteArrayFromInt(f.length)) bs.add(b);
		for(byte b : f) bs.add(b);
	}

	public static void setDefaultVendor(String vendor){
		if(vendor == null || vendor.trim().isEmpty()) throw new IllegalArgumentException();
		defaultVendor = vendor;
	}

	private class CommentList extends AbstractList<String>{
		private String[] values;

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