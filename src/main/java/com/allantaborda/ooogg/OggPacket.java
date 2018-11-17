/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg;

import java.util.LinkedList;

/**
 * This class represents an OGG packet.
 * @author Allan Taborda dos Santos
 */
public class OggPacket implements OggPackable{
	/** OGG packet content. */
	private byte[] cont;

	/**
	 * Creates a new OGG packet with its content.
	 * @param content The packet content.
	 */
	public OggPacket(byte[] content){
		cont = content;
	}

	/**
	 * Creates a new OGG packet whit its segments.
	 * @param segments The segments of this packet.
	 */
	public OggPacket(byte[]... segments){
		int tl = 0;
		for(byte[] w : segments) tl += w.length;
		cont = new byte[tl];
		tl = 0;
		for(byte[] w : segments){
			System.arraycopy(w, 0, cont, tl, w.length);
			tl += w.length;
		}
	}

	/**
	 * Tests if given header matches with beginning of packet content.
	 * @param header The header to be tested.
	 * @return {@code true} if header matches, {@code false} otherwise.
	 */
	public boolean headerMatches(String header){
		try{
			for(int c = 0; c < header.length(); c++) if(cont[c] != header.charAt(c)) return false;
			return true;
		}catch(IndexOutOfBoundsException e){
			return false;
		}
	}

	/**
	 * Returns the packet content size.
	 * @return The packet content size.
	 */
	public int getSize(){
		return cont.length;
	}

	/**
	 * Returns the OGG packet content.
	 * @return The OGG packet content.
	 */
	public byte[] getContent(){
		return cont;
	}

	/**
	 * Returns the OGG packet segments.
	 * @return The OGG packet segments.
	 */
	public byte[][] getSegments(){
		return split(cont);
	}

	/**
	 * Splits OGG packet content in segments.
	 * @param content The OGG packet content.
	 * @return The OGG packet segments.
	 */
	private byte[][] split(byte[] content){
		if(content.length < 256){
			if(content.length == 255){
				byte[][] op = new byte[2][];
				op[0] = content;
				op[1] = new byte[0];
				return op;
			}else{
				byte[][] op = new byte[1][];
				op[0] = content;
				return op;
			}
		}
		LinkedList<byte[]> bs = new LinkedList<>();
		do{
			byte[] p1 = new byte[255], p2 = new byte[content.length - 255];
			System.arraycopy(content, 0, p1, 0, 255);
			System.arraycopy(content, 255, p2, 0, p2.length);
			bs.add(p1);
			content = p2;
		}while(content.length > 255);
		for(byte[] p : split(content)) bs.add(p);
		byte[][] op = new byte[bs.size()][];
		int c = -1;
		for(byte[] p : bs) op[++c] = p;
		return op;
	}

	/**
	 * Since this object is already an OGG packet, returns this object.
	 * @return This object.
	 */
	public OggPacket toOggPacket(){
		return this;
	}
}