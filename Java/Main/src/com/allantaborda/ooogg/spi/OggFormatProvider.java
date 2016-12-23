/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg.spi;

import com.allantaborda.ooogg.OggPackable;
import com.allantaborda.ooogg.OggPacket;
import com.allantaborda.ooogg.Tags;
import java.io.IOException;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * Provider for audio file reading (decoding) and writing (encoding) services. Classes providing concrete implementations can read
 * or write (or both) one OGG-based type of audio file from an audio stream. Implementations of this class should only handle the
 * code concerning the format contained in the OGG container without worrying about OGG container manipulation itself.
 * @author Allan Taborda dos Santos
 */
public abstract class OggFormatProvider{
	/** Flag indicating if the service provider implements a decoder. */
	private boolean dec;
	/** Flag indicating if the service provider implements a encoder. */
	private boolean enc;

	/**
	 * Constructs a provider that support file reading (decoding) and writing (encoding) services (or both).
	 * @param hasDecoder {@code true} if provider supports reading services, {@code false} otherwise.
	 * @param hasEncoder {@code true} if provider supports writing services, {@code false} otherwise.
	 */
	public OggFormatProvider(boolean hasDecoder, boolean hasEncoder){
		dec = hasDecoder;
		enc = hasEncoder;
	}

	/**
	 * Returns if provider supports reading (decoding) services.
	 * @return {@code true} if provider supports reading services, {@code false} otherwise.
	 */
	public boolean hasDecoder(){
		return dec;
	}

	/**
	 * Returns if provider supports writing (encoding) services.
	 * @return {@code true} if provider supports writing services, {@code false} otherwise.
	 */
	public boolean hasEncoder(){
		return enc;
	}

	/**
	 * Returns the audio file type provided by this provider.
	 * @return The audio file type provided by this provider.
	 */
	public abstract Type getType();

	/**
	 * Returns the audio encoding provided by this provider.
	 * @return The audio encoding provided by this provider.
	 */
	public abstract AudioFormat.Encoding getEncoding();

	/**
	 * Returns the name of encoder used by provider. Providers that implement writing services must override this method.
	 * @return The name of encoder used by provider.
	 */
	public String getEncoderName(){
		return null;
	}

	/**
	 * Returns a new instance of {@code Tags} class, according to the file format. If file format provided by the
	 * implementation of this class has tags with header and/or framing bit, the provider must override this method.
	 * @return A new instance of {@code Tags} class.
	 */
	public Tags getTags(){
		return new Tags();
	}

	/**
	 * Returns an instance of the class that implement the header structure contained in the first OGG packet in an supported OGG file.
	 * This class must implement {@code OggPackable} interface. Providers that implement reading services must override this method.
	 * @param packet An OGG packet that contains the header structuret of file format provided by provider.
	 * @return The OGG file header according the supported file format.
	 */
	public OggPackable getHeader(OggPacket packet){
		throw new UnsupportedOperationException();
	}

	/**
	 * Obtains the audio file format according the given header. Providers that implement reading services must override this method.
	 * @param header The OGG file header according the supported file format.
	 * @param afProps The audio format properties.
	 * @param affProps The audio file format properties.
	 * @return An {@code AudioFileFormat} object describing the audio file format.
	 */
	public AudioFileFormat getAudioFileFormat(OggPackable header, Map<String, Object> afProps, Map<String, Object> affProps){
		throw new UnsupportedOperationException();
	}

	/**
	 * Obtains an audio input stream with the specified format from the given audio input stream. Providers that implement reading services must override this method.
	 * @param trgFormat The format of this stream's audio data.
	 * @param srcStream The stream from which data to be processed should be read.
	 * @return The stream from which processed data with the specified format may be read.
	 * @throws IOException If I/O error occurs.
	 */
	public OggAudioInputStream getAudioInputStream(AudioFormat trgFormat, AudioInputStream srcStream) throws IOException{
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns a new instance of the encoder box according the supported file format. Providers that implement writing services must override this method.
	 * @return A new instance of the encoder box.
	 */
	public EncoderBox newEncoderBox(){
		throw new UnsupportedOperationException();
	}

	/**
	 * Base class for all encoder box implementtions. Providers that implement writing services must create a concrete implementation of this class.
	 */
	public static abstract class EncoderBox implements OggPackable{
		/** The granule position to be recorded in OGG packet. */
		private long granulePosision;
		/** The PCM buffer where uncompressed data is stored temporarily. */
		private byte[] pcmBuffer;
		/** The processed data generated by encoder. */
		private byte[] processedData;

		/**
		 * Returns an instance of the class that implement the header structure contained in the first OGG packet in an supported OGG file.
		 * @param fmt The audio format to be encoded.
		 * @return The OGG file header according the supported file format.
		 */
		public abstract OggPackable getHeader(AudioFormat fmt);

		/**
		 * Initializes the encoder used to encode audio data in OGG packet.
		 * @throws Exception If any error occurs.
		 */
		public abstract void initEncoder() throws Exception;

		/**
		 * Encodes audio data in OGG packet. This method must call {@code setProcessedData(byte[])} and {@code incrementGranulePosision(long)} methods after encoding.
		 * @throws Exception If any error occurs.
		 */
		public abstract void encode() throws Exception;

		/**
		 * Creates a new PCM buffer with the given size per channel and chnnel number.
		 * @param sizePerChannel The size per channel.
		 * @param channels The channel number.
		 */
		public void initPCMBuffer(int sizePerChannel, int channels){
			pcmBuffer = new byte[sizePerChannel * channels];
		}

		/**
		 * Returns the PCM buffer where uncompressed data is stored temporarily.
		 * @return The PCM buffer.
		 */
		public byte[] getPCMBuffer(){
			return pcmBuffer;
		}

		/**
		 * Returns the granule position to be recorded in OGG packet.
		 * @return The granule position to be recorded in OGG packet.
		 */
		public long getGranulePosision(){
			return granulePosision;
		}

		/**
		 * Sets the processed data generated by encoder.
		 * @param pData The processed data.
		 */
		public void setProcessedData(byte[] pData){
			processedData = pData;
		}

		/**
		 * Increments the granule position.
		 * @param increment The value to be incremented in granule position.
		 */
		public void incrementGranulePosision(long increment){
			granulePosision += increment;
		}

		/**
		 * Returns the packet size.
		 * @return The packet size.
		 */
		public int getSize(){
			return processedData.length;
		}

		/**
		 * Returns the number of segments to be added to OGG page.
		 * @return The number of segments to be added to OGG page.
		 */
		public int getSegmentNumber(){
			return processedData.length / 255 + 1;
		}

		public OggPacket toOggPacket(){
			return new OggPacket(processedData);
		}
	}
}