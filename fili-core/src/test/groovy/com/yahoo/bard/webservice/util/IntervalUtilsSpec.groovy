// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE.md file distributed with this work for terms.
package com.yahoo.bard.webservice.util

import static com.yahoo.bard.webservice.data.time.DefaultTimeGrain.DAY
import static com.yahoo.bard.webservice.data.time.DefaultTimeGrain.MONTH
import static com.yahoo.bard.webservice.data.time.DefaultTimeGrain.WEEK
import static com.yahoo.bard.webservice.data.time.DefaultTimeGrain.YEAR

import com.yahoo.bard.webservice.data.time.DefaultTimeGrain
import com.yahoo.bard.webservice.data.time.TimeGrain
import com.yahoo.bard.webservice.druid.model.query.AllGranularity
import com.yahoo.bard.webservice.druid.model.query.Granularity

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Interval

import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

class IntervalUtilsSpec extends Specification {

    def setupSpec() {
        DateTimeZone.setDefault(DateTimeZone.UTC)
    }

    Map buildIntervalMap(Collection<String> intervals) {
        int i = 0;
        intervals.collectEntries { [(new Interval(it)): i++] }
    }

    def "Validate #intervalName overlaps with #intervalsToMatch"() {
        given:
        Interval interval = new Interval(intervalName)
        List<Interval> match = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(intervalsToMatch)
        List<Interval> expected = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(expectedMatch)

        expect:
        IntervalUtils.getIntervalOverlaps(interval, match).collect(Collectors.toList()) == expected

        where:
        intervalName | intervalsToMatch                                   || expectedMatch
        "2013/2016"  | ["2014/2015"]                                      || ["2014/2015"]
        "2013/2016"  | ["2014-01-01/2014-02-01", "2014-02-01/2014-03-01"] || ["2014-01-01/2014-02-01", "2014-02-01/2014-03-01"]
        "2013/2014"  | ["2010/2020"]                                      || ["2013/2014"]
        "2013/2015"  | ["2014/2016"]                                      || ["2014/2015"]
        "2013/2015"  | ["2014/2016"]                                      || ["2014/2015"]
        "2013/2014"  | ["2014/2016"]                                      || []
        "2013/2014"  | ["2013-03-01/2014-04-01", "2014/2016"]             || ["2013-03-01/2014-01-01"]
    }

    @Unroll
    def "getOverlappingIntervals returns #expectedMatch with intervals #listOne and #listTwo"() {
        given:
        List<Interval> list1 = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(listOne)
        List<Interval> list2 = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(listTwo)
        Set<Interval> expected = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(expectedMatch) as Set

        expect:
        IntervalUtils.getOverlappingSubintervals(list1, list2) == expected
        IntervalUtils.getOverlappingSubintervals(list2, list1) == expected

        where:
        listOne                    | listTwo                                            || expectedMatch
        ["2013/2016"]              | ["2014/2015"]                                      || ["2014/2015"]
        ["2013/2016"]              | ["2014-01-01/2014-02-01", "2014-02-01/2014-03-01"] || ["2014-01-01/2014-02-01", "2014-02-01/2014-03-01"]
        ["2013/2014"]              | ["2010/2020"]                                      || ["2013/2014"]
        ["2013/2015"]              | ["2014/2016"]                                      || ["2014/2015"]
        ["2013/2015"]              | ["2014/2016"]                                      || ["2014/2015"]
        ["2013/2014"]              | ["2014/2016"]                                      || []
        ["2013/2014"]              | ["2013-03-01/2014-04-01", "2014/2016"]             || ["2013-03-01/2014-01-01"]
        ["2013/2015", "2016/2018"] | ["2014/2016", "2015/2017"]                         || ["2014/2015", "2016/2017"]
        ["2013/2015"]              | ["2013-03-01/2014-04-01", "2014/2016"]             || ["2014/2015", "2013-03-01/2014-04-01"]
    }

    @Unroll
    def "Slicing Intervals #baseIntervals with period #grain returns #expected"(
            Granularity grain,
            List<String> baseIntervals,
            List<String> expected
    ) {
        given:
        List<Interval> baseIntervalList = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(baseIntervals)
        Map<Interval, AtomicInteger> expectedSet = buildIntervalMap(expected)

        expect:
        IntervalUtils.getSlicedIntervals(baseIntervalList, grain).
                collectEntries { k, v -> [(k): v.get()] } == expectedSet

        where:
        [grain, baseIntervals, expected] << sliceExpectedSets()
    }

    static def sliceExpectedSets() {
        List results = []
        results.add(
                [DAY, ["2015-01-14/2015-01-17"], ["2015-01-14/2015-01-15", "2015-01-15/2015-01-16", "2015-01-16/2015-01-17"]]

        )
        results.add(
                [MONTH, ["2015-01/2015-03"], ["2015-01/2015-02","2015-02/2015-03"]]

        )
        results.add(
                [MONTH, ["2015-01/2015-03", "2015-02/2015-03"], ["2015-01/2015-02","2015-02/2015-03"]]
        )

        results.add(
                [new AllGranularity(), ["2015-01-14/2015-02-15", "2015-03-15/2015-03-16", "2015-04-16/2015-05-17"],
                 ["2015-01-14/2015-02-15", "2015-03-15/2015-03-16", "2015-04-16/2015-05-17"]]
        )

        // Single Interval
        results.add(
                [DAY, ["2015-01-14/2015-01-15"], ["2015-01-14/2015-01-15"] ]

        )
        // Empty Interval
        results.add(
                [DAY, [], []]

        )

        // Alignment is not guaranteed by the iterator
        results.add(
                [DAY, ["2015-01-14T10:00:00.000Z/2015-01-16T10:00:00.000Z"],
                 ["2015-01-14T10:00:00.000Z/2015-01-15T10:00:00.000Z", "2015-01-15T10:00:00.000Z/2015-01-16T10:00:00.000Z"]]
        )
        results.add(
                [DAY, ["2015-01-14/2015-01-16T10:00:00.000Z"],
                 ["2015-01-14/2015-01-15",
                  "2015-01-15/2015-01-16",
                  "2015-01-16/2015-01-16T10:00:00.000Z",
                 ]]
        )
        results.add(
                [WEEK, ["2014-06-30/2014-07-21"],
                 ["2014-06-30/2014-07-07",
                  "2014-07-07/2014-07-14",
                  "2014-07-14/2014-07-21"
                 ]]
        )
        // Month wrapping is done correctly even in the ragged end of months
        results.add(
                [MONTH, ["2014-01-30/2014-04-30"],
                 ["2014-01-30/2014-02-28", "2014-02-28/2014-03-30", "2014-03-30/2014-04-30"]]
        )

        return results
    }

    static def complementExpectedSets() {
        List results = []
        results.add( [MONTH, "No source interval",
                      [],
                      ["2015-01-01/2017-01-01"],
                      [] ]
        )
        results.add( [MONTH, "No remove intervals",
                      ["2015-01-01/2017-01-01"],
                      [],
                      ["2015-01-01/2017-01-01"] ]
        )
        results.add( [MONTH, "No overlap",
                      ["2015-01-01/2017-01-01"],
                      ["2010/2011"],
                      ["2015-01-01/2017-01-01"]]
        )
        results.add( [MONTH, "Cut a hole out of the middle",
                      ["2015-01-01/2017-01-01"],
                      ["2016-01-01/2016-02-01"],
                      ["2015-01-01/2016-01-01", "2016-02-01/2017-01-01"] ]
        )
        results.add( [MONTH, "Shave off both ends",
                      ["2015/2017"],
                      ["2014-01-01/2015-02-01", "2016-05-01/2018"],
                      ["2015-02-01/2016-05-01"]
        ]
        )
        results.add( [YEAR, "Normalizes correctly",
                      ["2015/2017", "2016/2020"],
                      ["2016/2018", "2017/2019"],
                      ["2015/2016","2019/2020"]
        ]
        )
        results.add( [YEAR, "A single hole of smaller grain",
                      ["2015/2018"],
                      ["2016-01-01/2016-02-01"],
                      ["2015/2018"]
        ]
        )
        results.add( [YEAR, "A single hole of larger than grain but not aligned",
                      ["2015/2018"],
                      ["2015-12-01/2017-02-01"],
                      ["2015/2016", "2017/2018"]
        ]
        )
        results.add( [YEAR, "A hole larger than the request",
                      ["2015/2018"],
                      ["2014/2020"],
                      [] ]
        )
        return results
    }

    @Unroll
    def "Intersection of #supply yields #expected with fixed request and grain"() {
        setup:
        SimplifiedIntervalList requestedIntervals = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(['2014/2015'])
        Granularity granularity = MONTH
        SimplifiedIntervalList supplyIntervals = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(supply)
        SimplifiedIntervalList expectedIntervals = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(expected)

        expect:
        IntervalUtils.collectBucketedIntervalsIntersectingIntervalList(
                supplyIntervals,
                requestedIntervals,
                granularity
        ) == expectedIntervals

        where:
        supply                  | expected
        ["2013-04/2014-04"]     | ["2014-01/2014-04"]
        ["2014-04/2014-05"]     | ["2014-04/2014-05"]
        ["2014-09/2016-05"]     | ["2014-09/2015"]
        ["2012-09/2016-05"]     | ["2014/2015"]
        ["2012/2013"]           | []
        ["2020/2023"]           | []
    }

    @Unroll
    def "Intersect of #requestedIntervals by #grain yields #expected intersected with fixed supply"() {
        setup:
        SimplifiedIntervalList supply = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(['2012-05-04/2017-02-03'])
        Granularity granularity = grain
        SimplifiedIntervalList requestedIntervals = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(requested)
        SimplifiedIntervalList expectedIntervals = IntervalTestingUtilsSpec.buildSimplifiedIntervalList(expected)

        expect:
        IntervalUtils.collectBucketedIntervalsIntersectingIntervalList(
                supply,
                requestedIntervals,
                granularity
        ) == expectedIntervals

        where:
        grain | requested              | expected
        YEAR  | ["2012/2017"]          | ["2012/2017"]
        MONTH | ["2012-02/2017"]       | ["2012-05/2017"]
        DAY   | ["2012-02-02/2016-05"] | ["2012-05-04/2016-05"]
        YEAR  | ["2013/2018"]          | ["2013/2018"]
        MONTH | ["2013/2017-03"]       | ["2013/2017-03"]
        DAY   | ["2014-02-02/2017-01"] | ["2014-02-02/2017-01"]
    }

    @Unroll
    def "test getTimeGrain from interval - #expectedTimeGrain"() {
        setup:
        String[] dates = stringInterval.split("/")
        DateTime start = new DateTime(dates[0], DateTimeZone.UTC)
        DateTime end = new DateTime(dates[1], DateTimeZone.UTC)
        Interval interval = new Interval(start.toInstant(), end.toInstant())

        when: "we parse the time"
        Optional<TimeGrain> timeGrain = IntervalUtils.getTimeGrain(interval)

        then: "what we expect"
        timeGrain == expectedTimeGrain

        where:
        stringInterval                                      | expectedTimeGrain
        "2015-01-01T00:00:00.000Z/2016-01-01T00:00:00.000Z" | Optional.of(YEAR)
        "2017-01-01T00:00:00.000Z/2017-02-01T00:00:00.000Z" | Optional.of(MONTH)
        "2017-02-27T00:00:00.000Z/2017-03-06T00:00:00.000Z" | Optional.of(WEEK)
        "2015-09-12T00:00:00.000Z/2015-09-13T01:01:01.000Z" | Optional.empty()
        "2015-09-12T00:00:00.000Z/2015-09-13T00:00:00.000Z" | Optional.of(DAY)
        "2015-09-12T00:00:00.000Z/2015-09-12T01:00:00.000Z" | Optional.of(DefaultTimeGrain.HOUR)
        "2015-09-12T00:00:00.000Z/2015-09-12T00:01:00.000Z" | Optional.of(DefaultTimeGrain.MINUTE)
    }

    /**
     * Returns the instant at which this year began.
     *
     * @return  The instant at which this year began.
     */
    def getStartThisYear() {
        new DateTime(DateTime.now().getYear(), 1, 1, 0, 0, 0, 0)
    }
}
