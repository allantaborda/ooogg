/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.io.StreamCorruptedException;
import java.util.LinkedList;

public class OggUtils{
	public static OggPage readOggPage(InputStream is) throws StreamCorruptedException, IOException{
		if(!OggPage.MAGIC_NUMBER.equals(new String(readByteArray(is, 4))) || readByte(is) != 0) throw new StreamCorruptedException("This is not an OGG page");
		OggPage page = new OggPage();
		page.setHeaderType(readByte(is));
		page.setGranulePosition(readByteArray(is, 8));
		page.setSerialNumber(readByteArray(is, 4));
		page.setPageNumber(readByteArray(is, 4));
		page.setCrcChecksum(readByteArray(is, 4));
		for(byte ss : readByteArray(is, is.read())) page.addSegment(readByteArray(is, ss & 0xff));
		if(!page.isCrcChecksumValid()) throw new StreamCorruptedException("CRC is not valid");
		return page;
	}

	public static OggPage[] readOggPages(InputStream is) throws StreamCorruptedException, IOException{
		OggPage page = readOggPage(is);
		if(!page.contentContinuesInNextPage()) return new OggPage[]{page};
		LinkedList<OggPage> pages = new LinkedList<>();
		pages.add(page);
		do{
			page = readOggPage(is);
			pages.add(page);
		}while(page.contentContinuesInNextPage());
		return pages.toArray(new OggPage[pages.size()]);
	}

	public static OggPage readOggPage(RandomAccessFile file) throws StreamCorruptedException, IOException{
		if(!OggPage.MAGIC_NUMBER.equals(new String(readByteArray(file, 4))) || file.readByte() != 0) throw new StreamCorruptedException("This is not an OGG page");
		OggPage page = new OggPage();
		page.setHeaderType(file.readByte());
		page.setGranulePosition(readByteArray(file, 8));
		page.setSerialNumber(readByteArray(file, 4));
		page.setPageNumber(readByteArray(file, 4));
		page.setCrcChecksum(readByteArray(file, 4));
		for(byte ss : readByteArray(file, file.read())) page.addSegment(readByteArray(file, ss & 0xff));
		if(!page.isCrcChecksumValid()) throw new StreamCorruptedException("CRC is not valid");
		return page;
	}

	public static OggPage[] readOggPages(RandomAccessFile file) throws StreamCorruptedException, IOException{
		OggPage page = readOggPage(file);
		if(!page.contentContinuesInNextPage()) return new OggPage[]{page};
		LinkedList<OggPage> pages = new LinkedList<>();
		pages.add(page);
		do{
			page = readOggPage(file);
			pages.add(page);
		}while(page.contentContinuesInNextPage());
		return pages.toArray(new OggPage[pages.size()]);
	}

	public static OggPage[] toOggPages(int serialNumber, int firstPageNmber, OggPackable... packets){
		OggPage page = new OggPage();
		page.setGranulePosition(0L);
		page.setSerialNumber(serialNumber);
		page.setPageNumber(firstPageNmber++);
		byte[][] overplus = page.addPackets(packets);
		page.computeAndSetCrcChecksum();
		if(overplus == null) return new OggPage[]{page};
		LinkedList<OggPage> pages = new LinkedList<>();
		pages.add(page);
		while(overplus != null){
			OggPacket op = new OggPacket(overplus);
			page = new OggPage();
			page.setGranulePosition(0L);
			page.setSerialNumber(serialNumber);
			page.setPageNumber(firstPageNmber++);
			overplus = page.addPacket(op);
			page.computeAndSetCrcChecksum();
			pages.add(page);
		}
		return pages.toArray(new OggPage[pages.size()]);
	}

	public static void skipOggPage(InputStream is) throws IOException{
		if(is.markSupported()) is.mark(5);
		byte[] b = readByteArray(is, 4);
		if(OggPage.MAGIC_NUMBER.equals(new String(b))){
			is.skip(22);
			for(byte ss : readByteArray(is, is.read())) is.skip(ss & 0xff);
		}else if(is instanceof PushbackInputStream){
			do{
				((PushbackInputStream) is).unread(b);
				readByte(is);
				b = readByteArray(is, 4);
			}while(OggPage.MAGIC_NUMBER.equals(new String(b)));
			((PushbackInputStream) is).unread(b);
		}else if(is.markSupported()){
			do{
				is.reset();
				is.skip(1);
				is.mark(5);
				b = readByteArray(is, 4);
			}while(OggPage.MAGIC_NUMBER.equals(new String(b)));
			is.reset();
		}else throw new IOException("Unable to skip OGG page");
	}

	public static void skipOggPage(RandomAccessFile file) throws IOException{
		if(OggPage.MAGIC_NUMBER.equals(new String(readByteArray(file, 4)))){
			file.skipBytes(22);
			for(byte ss : readByteArray(file, file.read())) file.skipBytes(ss & 0xff);
		}else{
			byte[] b;
			do{
				file.seek(file.getFilePointer() - 3);
				b = readByteArray(file, 4);
			}while(OggPage.MAGIC_NUMBER.equals(new String(b)));
			file.seek(file.getFilePointer() - 4);
		}
	}

	public static void goToLastOggPageFilePointer(RandomAccessFile file) throws IOException{
		file.seek(file.length() - 27);
		while(!OggPage.MAGIC_NUMBER.equals(new String(readByteArray(file, 4)))) file.seek(file.getFilePointer() - 5);
		file.seek(file.getFilePointer() - 4);
	}

	public static byte readByte(InputStream is) throws IOException{
		int b = is.read();
		if(b < 0) throw new EOFException();
		return (byte) b;
	}

	public static byte[] readByteArray(InputStream is, int length) throws IOException{
		if(length < 0) throw new EOFException();
		byte[] b = new byte[length];
		if(is.read(b) < 0) throw new EOFException();
		return b;
	}

	public static byte[] readByteArray(RandomAccessFile file, int length) throws IOException{
		if(length < 0) throw new EOFException();
		byte[] b = new byte[length];
		if(file.read(b) < 0) throw new EOFException();
		return b;
	}

	public static OggPacket[] getPacketsFromPages(InputStream is) throws StreamCorruptedException, IOException{
		return getPacketsFromPages(readOggPages(is));
	}

	public static OggPacket[] getPacketsFromPages(RandomAccessFile file) throws StreamCorruptedException, IOException{
		return getPacketsFromPages(readOggPages(file));
	}

	public static OggPacket[] getPacketsFromPages(OggPage... pages){
		LinkedList<OggPacket> op = new LinkedList<>();
		byte[] temp = null;
		for(OggPage page : pages){
			byte[][] st = page.getSegmentTable();
			for(int c = 0; c < st.length; c++){
				if(temp == null){
					if(st[c].length < 255) op.add(new OggPacket(st[c]));
					else temp = st[c];
				}else{
					byte[] temp2 = new byte[temp.length + st[c].length];
					System.arraycopy(temp, 0, temp2, 0, temp.length);
					System.arraycopy(st[c], 0, temp2, temp.length, st[c].length);
					temp = temp2;
					if(st[c].length < 255){
						op.add(new OggPacket(temp));
						temp = null;
					}
				}
			}
		}
		return op.toArray(new OggPacket[op.size()]);
	}

	public static short getShortFromByteArray(byte[] array){
		return getShortFromByteArray(array, 0);
	}

	public static short getShortFromByteArray(byte[] array, int offset){
		if(array == null) return 0;
		return new Integer((array[offset] & 0xff) | ((array[offset + 1] & 0xff) << 8)).shortValue();
	}

	public static byte[] getByteArrayFromShort(short i){
		return new byte[]{(byte) i, (byte) (i >>> 8)};
	}

	public static int getIntFromByteArray(byte[] array){
		return getIntFromByteArray(array, 0);
	}

	public static int getIntFromByteArray(byte[] array, int offset){
		if(array == null) return 0;
		return (array[offset] & 0xff) | ((array[offset + 1] & 0xff) << 8) | ((array[offset + 2] & 0xff) << 16) | ((array[offset +3] & 0xff) << 24);
	}

	public static byte[] getByteArrayFromInt(int i){
		return new byte[]{(byte) i, (byte) (i >>> 8), (byte) (i >>> 16), (byte) (i >>> 24)};
	}

	public static long getLongFromByteArray(byte[] array){
		if(array == null) return 0L;
		long gp = array[7] & 0xff;
		for(int c = 6; c > -1; c--) gp = (gp << 8) | (array[c] & 0xff);
		return gp;
	}

	public static byte[] getByteArrayFromLong(long l){
		return new byte[]{(byte) l, (byte) (l >>> 8), (byte) (l >>> 16), (byte) (l >>> 24), (byte) (l >>> 32), (byte) (l >>> 40), (byte) (l >>> 44), (byte) (l >>> 48)};
	}

	public static String getStringFromUTF8Bytes(byte[] utf8Bytes){
		return new String(utf8Bytes, UTF_8);
	}

	public static byte[] getUTF8BytesFromString(String str){
		return str.getBytes(UTF_8);
	}
}