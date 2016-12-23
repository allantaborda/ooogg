/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg;

/**
 * Classes that implement this interface represent data structures that can be converted into OGG packets.
 * @author Allan Taborda dos Santos
 */
public interface OggPackable{
	/**
	 * Checks if data contained in this data structure is valid.
	 * @return {@code true} if data is valid, {@code false} otherwise.
	 */
	default boolean isValid(){
		return true;
	}

	/**
	 * Converts this object to an OGG packet.
	 * @return The converted OGG packet.
	 */
	OggPacket toOggPacket();
}