/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg.spi;

import com.allantaborda.ooogg.OggPacket;
import com.allantaborda.ooogg.OggPage;
import com.allantaborda.ooogg.OggUtils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.ArrayDeque;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * Unified {@code AudioInputStream} for all OGG-based audio file formats.
 * @author Allan Taborda dos Santos
 */
public abstract class OggAudioInputStream extends AudioInputStream{
	/** OGG packet queue. */
	private ArrayDeque<OggPacket> queue = new ArrayDeque<>(120);
	/** {@code AudioInputStream} which contains undecoded audio data. */
	private BufferedInputStream is;
	/** Flag indicating whether new OGG packages still need to be extracted. */
	private boolean extractMore = true;
	/** Position in buffer where audio data is read. */
	private int pRead;
	/** Position in buffer where audio data is written. */
	private int pWrite;
	/** Channel number. */
	private int channels;
	/** Buffer where decoded audio data is stored temporarily. */
	private byte[] buf;

	/**
	 * Constructs an {@code OggAudioInputStream} that has the requested format, using audio data from the specified audio input stream. 
	 * @param fmt The format of this stream's audio data.
	 * @param ais The audio input stream on which this {@code OggAudioInputStream} object is based.
	 * @throws StreamCorruptedException If OGG structure is corrupted or data structure is not an OGG container.
	 * @throws IOException If I/O error occurs.
	 */
	public OggAudioInputStream(AudioFormat fmt, AudioInputStream ais) throws StreamCorruptedException, IOException{
		this(fmt, new BufferedInputStream(ais));
	}

	/**
	 * Private constructor used by public construtor above. Used to encapsulate {@code AudioInputStream} in a {@code BufferedInputStream}.
	 * @param fmt The format of this stream's audio data.
	 * @param bis The {@code BufferedInputStream} that encapsulates the {@code AudioInputStream}.
	 * @throws StreamCorruptedException If OGG structure is corrupted or data structure is not an OGG container.
	 * @throws IOException If I/O error occurs.
	 */
	private OggAudioInputStream(AudioFormat fmt, BufferedInputStream bis) throws StreamCorruptedException, IOException{
		super(bis, fmt, -1);
		channels = fmt.getChannels();
		is = bis;
		OggPage[] pages = OggUtils.readOggPages(is);
		while(pages[0].getGranulePosition() < 1L) pages = OggUtils.readOggPages(is);
		for(OggPacket p : OggUtils.getPacketsFromPages(pages)) queue.offer(p);
	}

	/**
	 * Retorns the channel number.
	 * @return The channel number.
	 */
	public int getChannels(){
		return channels;
	}

	public synchronized int read() throws IOException{
		if(pRead < 0) return -1;
		byte b = buf[pRead++];
		if(pRead == buf.length) initBuffer();
		return b;
	}

	public synchronized int read(byte[] b, int off, int len) throws IOException{
		if(pRead < 0) return -1;
		int c;
		for(c = 0; c < len; c++) b[off++] = (byte) read();
		return c;
	}

	/**
	 * Initializes the decoded audio buffer and decodes an OGG packet.
	 */
	public final void initBuffer(){
		boolean decode;
		OggPacket packet = queue.poll();
		if(extractMore && queue.size() < 8){
			try{
				for(OggPacket p : OggUtils.getPacketsFromPages(is)) queue.offer(p);
			}catch(Exception e){
				extractMore = false;
			}
		}
		if(packet == null) decode = false;
		else decode = decode(packet);
		pRead = decode ? 0 : -1;
		pWrite = -1;
	}

	/**
	 * Creates the buffer where decoded audio data is stored temporarily, with specified capacity.
	 * @param capacity The buffer capacity.
	 */
	public final void createBuffer(int capacity){
		buf = new byte[capacity];
	}

	/**
	 * Puts a byte value in the decoded audio buffer.
	 * @param value The byte value to be inserted.
	 */
	public final void putInBuffer(byte value){
		if(++pWrite < buf.length) buf[pWrite] = value;
	}

	/**
	 * Puts a short value in the decoded audio buffer.
	 * @param value The short value to be inserted.
	 */
	public final void putInBuffer(short value){
		putInBuffer((byte) (value & 0xff));
		putInBuffer((byte) ((value >> 8) & 0xff));
	}

	/**
	 * Decodes an OGG audio packet in uncompressed PCM audio.
	 * @param packet The OGG audio packet to be decoded.
	 * @return {@code true} if decoding is successfully, {@code false} otherwise.
	 */
	protected abstract boolean decode(OggPacket packet);
}