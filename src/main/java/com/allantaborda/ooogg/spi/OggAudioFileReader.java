/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg.spi;

import com.allantaborda.ooogg.OggPackable;
import com.allantaborda.ooogg.OggPacket;
import com.allantaborda.ooogg.OggUtils;
import com.allantaborda.ooogg.Tags;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.util.HashMap;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

/**
 * Unified {@code AudioFileReader} for all OGG-based audio file formats.
 * @author Allan Taborda dos Santos
 */
public class OggAudioFileReader extends AudioFileReader{
	/** Array of all OGG-based file types provided by installed service providers. */
	public static final AudioFileFormat.Type[] TYPES = FormatProviderService.getInstance().getFormatsForDecoding();

	public AudioFileFormat getAudioFileFormat(InputStream is) throws UnsupportedAudioFileException, IOException{
		return getAudioFileFormat(is, AudioSystem.NOT_SPECIFIED);
	}

	public AudioFileFormat getAudioFileFormat(File f) throws UnsupportedAudioFileException, IOException{
		return getAudioFileFormat(new FileInputStream(f), f.length());
	}

	public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException{
		return getAudioFileFormat(url.openStream(), AudioSystem.NOT_SPECIFIED);
	}

	public AudioInputStream getAudioInputStream(InputStream is) throws UnsupportedAudioFileException, IOException{
		return getAudioInputStream(is, AudioSystem.NOT_SPECIFIED);
	}

	public AudioInputStream getAudioInputStream(File f) throws UnsupportedAudioFileException, IOException{
		return getAudioInputStream(new FileInputStream(f), f.length());
	}

	public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException{
		return getAudioInputStream(url.openStream(), AudioSystem.NOT_SPECIFIED);
	}

	private AudioFileFormat getAudioFileFormat(InputStream is, long length) throws UnsupportedAudioFileException, IOException{
		BufferedInputStream bis = null;
		try{
			bis = is instanceof BufferedInputStream ? (BufferedInputStream) is : new BufferedInputStream(is, 8192);
			bis.mark(8192);
			try{
				OggPacket packet = OggUtils.getPacketsFromPages(bis)[0];
				for(AudioFileFormat.Type t : TYPES){
					OggFormatProvider fps = FormatProviderService.getInstance().getFormatProvider(t);
					OggPackable header = fps.getHeader(packet);
					if(header.isValid()){
						HashMap<String, Object> afProps = new HashMap<>(), affProps = new HashMap<>();
						Tags coms = fps.getTags();
						coms.fromOggPacket(OggUtils.getPacketsFromPages(bis)[0]);
						coms.writeIntoMap(affProps);
						afProps.put("vendor", coms.getVendor());
						return fps.getAudioFileFormat(header, length, afProps, affProps);
					}
				}
			}catch(StreamCorruptedException | EOFException e){}
			throw new UnsupportedAudioFileException("OGG file is not valid");
		}finally{
			if(bis != null) bis.reset();
		}
	}

	private AudioInputStream getAudioInputStream(InputStream is, long length) throws UnsupportedAudioFileException, IOException{
		BufferedInputStream bis = new BufferedInputStream(is, 8192);
		bis.mark(8192);
		AudioFileFormat fmt = getAudioFileFormat(bis, length);
		bis.reset();
		return new AudioInputStream(bis, fmt.getFormat(), fmt.getFrameLength());
	}
}