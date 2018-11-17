/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg.spi;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.FormatConversionProvider;

/**
 * Unified {@code FormatConversionProvider} for all OGG-based audio file formats.<br/><br/>
 * This implementation converts only OGG-based audio formats to uncompressed PCM audio. For conversion
 * of uncompressed PCM audio to OGG-based audio formats, use {@code OggAudioFileWriter} instead.
 * @author Allan Taborda dos Santos
 */
public class OggFormatConversionProvider extends FormatConversionProvider{
	/** Array of all OGG-based encodings provided by installed service providers. */
	private static final AudioFormat.Encoding[] srcEnc = FormatProviderService.getInstance().getEncodings();
	/** Array containing the only target encoding, which is {@code AudioFormat.Encoding.PCM_SIGNED}. */
	private static final AudioFormat.Encoding[] trgEnc = {AudioFormat.Encoding.PCM_SIGNED};
	/** Array of all OGG-based source formats provided by installed service providers. */
	private static final AudioFormat[] srcFormats;

	static{
		int c = -1;
		srcFormats = new AudioFormat[srcEnc.length * 2];
		for(AudioFormat.Encoding enc : srcEnc){
			srcFormats[++c] = new AudioFormat(enc, -1.0F, -1, 1, -1, -1.0F, false);
			srcFormats[++c] = new AudioFormat(enc, -1.0F, -1, 2, -1, -1.0F, false);
		}
	}

	public AudioFormat.Encoding[] getSourceEncodings(){
		return srcEnc;
	}

	public AudioFormat.Encoding[] getTargetEncodings(){
		return trgEnc;
	}

	public AudioFormat.Encoding[] getTargetEncodings(AudioFormat srcFormat){
		for(AudioFormat sf : srcFormats) if(sf.matches(srcFormat)) return trgEnc;
		return new AudioFormat.Encoding[0];
	}

	public AudioFormat[] getTargetFormats(AudioFormat.Encoding trgEnc, AudioFormat srcFormat){
		int ch = srcFormat.getChannels();
		if(AudioFormat.Encoding.PCM_SIGNED.equals(trgEnc) && (ch == 2 || ch == 1)){
			for(AudioFormat.Encoding enc : srcEnc){
				if(enc.equals(srcFormat.getEncoding())){
					return new AudioFormat[]{new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, -1.0F, 16, ch, ch * 2, -1.0F, false)};
				}
			}
		}
		return new AudioFormat[0];
	}

	public AudioInputStream getAudioInputStream(AudioFormat.Encoding trgEnc, AudioInputStream srcStream){
		return getAudioInputStream(new AudioFormat(trgEnc, -1.0F, 16, srcStream.getFormat().getChannels(), srcStream.getFormat().getChannels() * 2, -1.0F, false), srcStream);
	}

	public AudioInputStream getAudioInputStream(AudioFormat trgFormat, AudioInputStream srcStream){
		try{
			OggAudioInputStream ais = FormatProviderService.getInstance().getFormatProvider(srcStream.getFormat().getEncoding()).getAudioInputStream(trgFormat, srcStream);
			ais.initBuffer();
			return ais;
		}catch(IOException e){
			throw new IllegalArgumentException("Invalid OGG file", e);
		}
	}
}