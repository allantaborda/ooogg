/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg;

import java.util.ArrayList;

/**
 * This class represents an OGG page.
 * @author Allan Taborda dos Santos
 */
public class OggPage{
	public static final String MAGIC_NUMBER = "OggS";
	private static final long[] crcLookup = new long[256];
	private boolean continuation, beginningOfStream, endOfStream;
	private byte[] granulePosition; //8 bytes (long)
	private byte[] serialNumber; //4 bytes (int)
	private byte[] pageNumber; //4 bytes (int)
	private byte[] crcChecksum; //4 bytes (int)
	private SegmentList segmentTable = new SegmentList();

	static{
		for(int c = 0; c < crcLookup.length; c++){
			long r = c << 24;
			for(int d = 0; d < 8; d++){
				if((r & 0x80000000L) != 0) r = (r << 1) ^ 0x04c11db7L;
				else r <<= 1;
			}
			crcLookup[c] = r & 0xffffffff;
		}
	}

	public boolean isContinuation(){
		return continuation;
	}

	public void setContinuation(boolean cont){
		continuation = cont;
	}

	public boolean isBeginningOfStream(){
		return beginningOfStream;
	}

	public void setBeginningOfStream(boolean bos){
		beginningOfStream = bos;
	}

	public boolean isEndOfStream(){
		return endOfStream;
	}

	public void setEndOfStream(boolean eos){
		endOfStream = eos;
	}

	public byte getHeaderType(){
		if(continuation){
			if(beginningOfStream){
				if(endOfStream) return 0b111;
				return 0b11;
			}else if(endOfStream) return 0b101;
			return 0b1;
		}else if(beginningOfStream){
			if(endOfStream) return 0b110;
			return 0b10;
		}else if(endOfStream) return 0b100;
		return 0;
	}

	public void setHeaderType(byte hType){
		continuation = (hType & 0b1) != 0;
		beginningOfStream = (hType & 0b10) != 0;
		endOfStream = (hType & 0b100) != 0;
	}

	public long getGranulePosition(){
		return OggUtils.getLongFromByteArray(granulePosition);
	}

	public void setGranulePosition(long gPosition){
		granulePosition = OggUtils.getByteArrayFromLong(gPosition);
	}

	public void setGranulePosition(byte[] gPosition){
		granulePosition = gPosition;
	}

	public int getSerialNumber(){
		return OggUtils.getIntFromByteArray(serialNumber);
	}

	public void setSerialNumber(int sNumber){
		serialNumber = OggUtils.getByteArrayFromInt(sNumber);
	}

	public void setSerialNumber(byte[] sNumber){
		serialNumber = sNumber;
	}

	public int getPageNumber(){
		return OggUtils.getIntFromByteArray(pageNumber);
	}

	public void setPageNumber(int pNumber){
		pageNumber = OggUtils.getByteArrayFromInt(pNumber);
	}

	public void setPageNumber(byte[] pNumber){
		pageNumber = pNumber;
	}

	public int getCrcChecksum(){
		return OggUtils.getIntFromByteArray(crcChecksum);
	}

	public void setCrcChecksum(byte[] crc32){
		crcChecksum = crc32;
	}

	public boolean isCrcChecksumValid(){
		return getCrcChecksum() == OggUtils.getIntFromByteArray(computeCRC());
	}

	public void computeAndSetCrcChecksum(){
		crcChecksum = computeCRC();
	}

	public byte[][] getSegmentTable(){
		return segmentTable.toArray(new byte[segmentTable.size()][]);
	}

	/**
	 * Adds an OGG packet to OGG page.
	 * @param packet The OGG packet.
	 * @return The overplus segments that don't fit in OGG page, or {@code null} if no overplus segments.
	 * @throws IllegalArgumentException If OGG packet is not valid.
	 */
	public byte[][] addPacket(OggPackable packet){
		if(packet.isValid()){
			boolean full = false;
			ArrayList<byte[]> overplus = new ArrayList<>(100);
			for(byte[] seg : packet.toOggPacket().getSegments()){
				if(!full) full = !addSegment(seg);
				if(full) overplus.add(seg);
			}
			return overplus.isEmpty() ? null : overplus.toArray(new byte[overplus.size()][]);
		}else throw new IllegalArgumentException("Invalid OGG packet");
	}

	/**
	 * Adds one or more OGG packet to OGG page.
	 * @param packets The OGG packets.
	 * @return The overplus segments that don't fit in OGG page, or {@code null} if no overplus segments.
	 * @throws IllegalArgumentException If any OGG packet is not valid.
	 */
	public byte[][] addPackets(OggPackable... packets){
		if(packets.length == 1) return addPacket(packets[0]);
		ArrayList<byte[][]> tOverplus = new ArrayList<>(packets.length);
		for(OggPackable p : packets){
			byte[][] overplus = addPacket(p);
			if(overplus != null) tOverplus.add(overplus);
		}
		if(tOverplus.isEmpty()) return null;
		else if(tOverplus.size() == 1) return tOverplus.get(0);
		else{
			int sum = 0;
			for(byte[][] overplus : tOverplus) sum += overplus.length;
			byte[][] sOverplus = new byte[sum][];
			int pos = 0;
			for(byte[][] overplus : tOverplus){
				System.arraycopy(overplus, 0, sOverplus, pos, overplus.length);
				pos += overplus.length;
			}
			return sOverplus;
		}
	}

	/**
	 * Adds a segment to OGG page.
	 * @param segment The segment to be added.
	 * @return {@code true} if segment was added successfully, {@code false} if OGG page is full and segment should be added to next page.
	 */
	public boolean addSegment(byte[] segment){
		if(segment.length > 255) throw new IllegalArgumentException("Segment size greater than 255 bytes");
		return segmentTable.add(segment);
	}

	/**
	 * Returns the sum of all segment sizes.
	 * @return The sum of all segment sizes.
	 */
	public int getTotalSegmentSize(){
		int tss = 0;
		for(byte[] s : segmentTable) tss += s.length;
		return tss;
	}

	/**
	 * Checks if last packet content continues in next OGG page.
	 * @return {@code true} if last packet content continues in next OGG page, {@code false} otherwise.
	 */
	public boolean contentContinuesInNextPage(){
		return segmentTable.lastSegmentSize == 255;
	}

	/**
	 * Returns the OGG page content in bytes.
	 * @return The OGG page content in bytes.
	 * @throws IllegalStateException If granule position, serial number, page number or CRC fields is not set.
	 */
	public byte[] getBytes(){
		return getBytes(true);
	}

	/**
	 * Computes the CRC checksum based in OGG page content.
	 * @return The computed CRC checksum.
	 */
	private byte[] computeCRC(){
		long crcReg = 0L;
		for(byte b : getBytes(false)){
			int tmp = ((int) ((crcReg >>> 24) & 0xff)) ^ (b & 0xff);
			crcReg = (crcReg << 8) ^ crcLookup[tmp];
			crcReg &= 0xffffffff;
		}
		return new byte[]{(byte) (crcReg & 0xff), (byte) ((crcReg >>> 8) & 0xff), (byte) ((crcReg >>> 16) & 0xff), (byte) ((crcReg >>> 24) & 0xff)};
	}

	/**
	 * Returns the OGG page content in bytes.
	 * @param includeCRC If CRC field should be included in the byte array.
	 * @return The OGG page content in bytes.
	 * @throws IllegalStateException If granule position, serial number, page number or CRC (if boolean parameter is {@code true}) fields is not set.
	 */
	private byte[] getBytes(boolean includeCRC){
		if(granulePosition == null || serialNumber == null || pageNumber == null || (includeCRC && crcChecksum == null)) throw new IllegalStateException();
		byte[] sSizes = new byte[segmentTable.size()];
		for(int c = 0; c < sSizes.length; c++) sSizes[c] = (byte) segmentTable.get(c).length;
		byte[] bytes = new byte[sSizes.length + getTotalSegmentSize() + 27];
		System.arraycopy(MAGIC_NUMBER.getBytes(), 0, bytes, 0, 4);
		bytes[5] = getHeaderType();
		System.arraycopy(granulePosition, 0, bytes, 6, 8);
		System.arraycopy(serialNumber, 0, bytes, 14, 4);
		System.arraycopy(pageNumber, 0, bytes, 18, 4);
		if(includeCRC) System.arraycopy(crcChecksum, 0, bytes, 22, 4);
		bytes[26] = (byte) sSizes.length;
		System.arraycopy(sSizes, 0, bytes, 27, sSizes.length);
		int p = sSizes.length + 27;
		for(byte[] s : segmentTable){
			System.arraycopy(s, 0, bytes, p, s.length);
			p += s.length;
		}
		return bytes;
	}

	/** This extended version of {@code ArrayList} stores a maximum of 255 segments (the maximum segment number allowed per OGG page). */
	private class SegmentList extends ArrayList<byte[]>{
		int lastSegmentSize;

		SegmentList(){
			super(255);
		}

		public boolean add(byte[] b){
			if(size() == 255) return false;
			lastSegmentSize = b.length;
			return super.add(b);
		}
	}
}