/* OOOGG - Object-Oriented OGG Container
 * Copyright (c) 2016, Allan Taborda
 * This software is distributed under the BSD 3-Clause license.
 * See https://github.com/allantaborda/ooogg/blob/master/LICENSE for more details.
 */
package com.allantaborda.ooogg;

/**
 * This interface contains most used keys for Vorbis Comments.
 * @author Allan Taborda dos Santos
 */
public interface TagKeys{
	/** Track/Work name */
	public static final String TITLE = "TITLE";
	/** The version field may be used to differentiate multiple versions of the same track title in a single collection. (e.g. remix info) */
	public static final String VERSION = "VERSION";
	/** The collection name to which this track belongs */
	public static final String ALBUM = "ALBUM";
	/** The track number of this piece if part of a specific larger collection or album */
	public static final String TRACKNUMBER = "TRACKNUMBER";
	/** The artist generally considered responsible for the work. In popular music this is usually the performing band or singer. For classical music it would be the composer. For an audio book it would be the author of the original text. */
	public static final String ARTIST = "ARTIST";
	/** The artist(s) who performed the work. In classical music this would be the conductor, orchestra, soloists. In an audio book it would be the actor who did the reading. In popular music this is typically the same as the ARTIST and is omitted. */
	public static final String PERFORMER = "PERFORMER";
	/** Copyright attribution, e.g., '2001 Nobody's Band' or '1999 Jack Moffitt' */
	public static final String COPYRIGHT = "COPYRIGHT";
	/** License information, e.g., 'All Rights Reserved', 'Any Use Permitted', a URL to a license such as a Creative Commons license ("www.creativecommons.org/blahblah/license.html") or the EFF Open Audio License ('distributed under the terms of the Open Audio License. see http://www.eff.org/IP/Open_licenses/eff_oal.html for details'), etc. */
	public static final String LICENSE = "LICENSE";
	/** Name of the organization producing the track (i.e. the 'record label') */
	public static final String ORGANIZATION = "ORGANIZATION";
	/** A short text description of the contents */
	public static final String DESCRIPTION = "DESCRIPTION";
	/** A short text indication of music genre */
	public static final String GENRE = "GENRE";
	/** Date the track was recorded */
	public static final String DATE = "DATE";
	/** Location where track was recorded */
	public static final String LOCATION = "LOCATION";
	/** Contact information for the creators or distributors of the track. This could be a URL, an email address, the physical address of the producing label. */
	public static final String CONTACT = "CONTACT";
	/** ISRC number for the track; see the ISRC intro page for more information on ISRC numbers. */
	public static final String ISRC = "ISRC";
}