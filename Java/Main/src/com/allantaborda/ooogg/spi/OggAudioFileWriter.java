/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg.spi;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import com.allantaborda.ooogg.OggPage;
import com.allantaborda.ooogg.OggUtils;
import com.allantaborda.ooogg.Tags;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.AudioFileWriter;

/**
 * Unified {@code AudioFileWriter} for all OGG-based audio file formats.
 * @author Allan Taborda dos Santos
 */
public class OggAudioFileWriter extends AudioFileWriter{
	/** Array of all OGG-based file types provided by installed service providers. */
	public static final Type[] TYPES = FormatProviderService.getInstance().getFormatsForEncoding();

	public Type[] getAudioFileTypes(){
		return TYPES;
	}

	public Type[] getAudioFileTypes(AudioInputStream ais){
		if(!PCM_SIGNED.equals(ais.getFormat().getEncoding()) || ais.getFormat().getChannels() > 2 || ais.getFormat().getChannels() < 1) return new Type[0];
		return TYPES;
	}

	public int write(AudioInputStream ais, Type type, File out) throws IOException{
		return write(ais, type, new BufferedOutputStream(new FileOutputStream(out), 131072));
	}

	public int write(AudioInputStream ais, Type type, OutputStream out) throws IOException{
		boolean supported = false;
		for(Type t : TYPES){
			if(t.equals(type)){
				supported = true;
				break;
			}
		}
		if(supported){
			OggFormatProvider prov = FormatProviderService.getInstance().getFormatProvider(type);
			OggFormatProvider.EncoderBox eb = prov.newEncoderBox();
			int sn = (int) (Math.random() * Integer.MAX_VALUE);
			OggPage page = new OggPage();
			page.setBeginningOfStream(true);
			page.setGranulePosition(0L);
			page.setSerialNumber(sn);
			page.setPageNumber(0);
			page.addPacket(eb.getHeader(ais.getFormat()));
			page.computeAndSetCrcChecksum();
			Tags coms = prov.getTags();
			String encName = prov.getEncoderName();
			if(encName != null) coms.setVendor(encName + " (using OOOGG - Object-Oriented OGG Container)");
			int totalBytesWritten = 0, pageNumber = 1, segmentCount = 0, pageContentSize = 0;
			try{
				byte[] b = page.getBytes();
				out.write(b);
				totalBytesWritten += b.length;
				for(OggPage p : OggUtils.toOggPages(sn, 1, coms)){
					b = p.getBytes();
					out.write(b);
					totalBytesWritten += b.length;
				}
				page = new OggPage();
				page.setSerialNumber(sn);
				page.setPageNumber(++pageNumber);
				eb.initEncoder();
				while(ais.read(eb.getPCMBuffer()) > 0){
					if(pageContentSize > 4250 || segmentCount > 240){
						pageContentSize = 0;
						segmentCount = 0;
						page.setGranulePosition(eb.getGranulePosision());
						page.computeAndSetCrcChecksum();
						b = page.getBytes();
						out.write(b);
						totalBytesWritten += b.length;
						page = new OggPage();
						page.setSerialNumber(sn);
						page.setPageNumber(++pageNumber);
					}
					eb.encode();
					pageContentSize += eb.getSize();
					segmentCount += eb.getSegmentNumber();
					page.addPacket(eb);
				}
				page.setGranulePosition(eb.getGranulePosision());
				page.setEndOfStream(true);
				page.computeAndSetCrcChecksum();
				b = page.getBytes();
				out.write(b);
				totalBytesWritten += b.length;
			}catch(IOException e){
				throw e;
			}catch(Exception e){
				throw new IOException("Error while encoding audio file", e);
			}finally{
				out.flush();
				out.close();
			}
			return totalBytesWritten;
		}
		throw new IllegalArgumentException("File type " + type + " not supported");
	}
}