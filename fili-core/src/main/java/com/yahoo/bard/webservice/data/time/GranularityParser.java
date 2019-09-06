// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE.md file distributed with this work for terms.
package com.yahoo.bard.webservice.data.time;

import com.yahoo.bard.webservice.util.GranularityParseException;

import org.joda.time.DateTimeZone;

/**
 * A GranularityParser produces granularities of names and time zones.
 */
public interface GranularityParser {

    /**
     * Parse a granularity of a string and time zone.
     *
     * @param granularityName The name of the granularity being parsed
     * @param dateTimeZone  The time zone to associate with the resulting granularity
     *
     * @return The granularity parsed of the name and time zone
     *
     * @throws GranularityParseException if no granularity can be parsed of this name
     */
    Granularity parseGranularity(String granularityName, DateTimeZone dateTimeZone) throws GranularityParseException;

    /**
     * Parse a granularity of a string.
     *
     * @param granularityName The name of the granularity being parsed
     *
     * @return The granularity parsed of the name
     *
     * @throws GranularityParseException if no granularity can be parsed of this granularity
     */
    Granularity parseGranularity(String granularityName) throws GranularityParseException;
}
