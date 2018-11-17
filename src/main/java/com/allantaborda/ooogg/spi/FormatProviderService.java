/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg.spi;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ServiceLoader;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

/**
 * Service that returns installed service providers that provide support for audio formats stored in OGG containers, as well as data about these formats.
 * @author Allan Taborda dos Santos
 */
class FormatProviderService{
	/** Unique instance of this class. */
	private static FormatProviderService singleton;
	/** Map containing audio file types and its respective service providers. */
	private HashMap<Type, OggFormatProvider> providerMap;

	/** Private constructor that retrieves the service providers and stores its in map. */
	private FormatProviderService(){
		providerMap = new HashMap<>();
		for(OggFormatProvider prov : ServiceLoader.load(OggFormatProvider.class)) providerMap.put(prov.getType(), prov);
	}

	/**
	 * Returns the unique instance of this class.
	 * @return The unique instance of this class.
	 */
	static synchronized FormatProviderService getInstance(){
		if(singleton == null) singleton = new FormatProviderService();
		return singleton;
	}

	/**
	 * Returns the encodings related to the supported audio formats for playback.
	 * @return The supported encodings for audio playback.
	 */
	Encoding[] getEncodings(){
		LinkedList<Encoding> x = new LinkedList<>();
		for(Type t : providerMap.keySet()){
			OggFormatProvider prov = providerMap.get(t);
			if(prov.hasDecoder()) x.add(prov.getEncoding());
		}
		return x.toArray(new Encoding[x.size()]);
	}

	/**
	 * Returns the supported audio file types for playback.
	 * @return The supported audio file types for playback.
	 */
	Type[] getFormatsForDecoding(){
		LinkedList<Type> x = new LinkedList<>();
		for(Type t : providerMap.keySet()) if(providerMap.get(t).hasDecoder()) x.add(t);
		return x.toArray(new Type[x.size()]);
	}

	/**
	 * Returns the supported audio file types for recording.
	 * @return The supported audio file types for recording.
	 */
	Type[] getFormatsForEncoding(){
		LinkedList<Type> x = new LinkedList<>();
		for(Type t : providerMap.keySet()) if(providerMap.get(t).hasEncoder()) x.add(t);
		return x.toArray(new Type[x.size()]);
	}

	/**
	 * Returns the service provider related to selected audio file type.
	 * @param type The audio file type.
	 * @return The service provider.
	 * @throws IllegalArgumentException If selected audio file type is unsupported.
	 */
	OggFormatProvider getFormatProvider(Type type){
		if(providerMap.containsKey(type)) return providerMap.get(type);
		throw new IllegalArgumentException("Unknown type: " + type.toString());
	}

	/**
	 * Returns the service provider related to selected encoding.
	 * @param type The encoding.
	 * @return The service provider.
	 * @throws IllegalArgumentException If selected encoding is unsupported.
	 */
	OggFormatProvider getFormatProvider(Encoding enc){
		for(Type t : providerMap.keySet()){
			OggFormatProvider prov = providerMap.get(t);
			if(prov.getEncoding().equals(enc)) return prov;
		}
		throw new IllegalArgumentException("Unknown encoding: " + enc.toString());
	}
}