/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.random.*
import kotlin.test.*
import kotlin.time.*

class LocalDateTimeTest {



    @Test
    fun localDateTimeParsing() {
        fun checkParsedComponents(value: String, year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, nanosecond: Int, dayOfWeek: Int, dayOfYear: Int) {
            checkComponents(value.toLocalDateTime(), year, month, day, hour, minute, second, nanosecond, dayOfWeek, dayOfYear)
        }
        checkParsedComponents("2019-10-01T18:43:15.100500", 2019, 10, 1, 18, 43, 15, 100500000, 2, 274)
        checkParsedComponents("2019-10-01T18:43:15", 2019, 10, 1, 18, 43, 15, 0, 2, 274)
        checkParsedComponents("2019-10-01T18:12", 2019, 10, 1, 18, 12, 0, 0, 2, 274)
    }

    @UseExperimental(ExperimentalTime::class)
    @Test
    fun localDtToInstantConversion() {
        val ldt1 = "2019-10-01T18:43:15.100500".toLocalDateTime()
        val ldt2 = "2019-10-01T19:50:00.500600".toLocalDateTime()

        val diff = with(TimeZone.UTC) { ldt2.toInstant() - ldt1.toInstant() }
        assertEquals(1.hours + 7.minutes - 15.seconds + 400100.microseconds, diff)
    }

    @Test
    fun instantToLocalConversion() {
        val instant = Instant.parse("2019-10-01T18:43:15.100500Z")
        val datetime = instant.toLocalDateTime(TimeZone.UTC)
        checkComponents(datetime, 2019, 10, 1, 18, 43, 15, 100500000)
    }

    @Test
    fun getCurrentHMS() {
        with(Instant.now().toLocalDateTime(TimeZone.SYSTEM)) {
            println("${hour}h ${minute}m")
        }
    }


    @UseExperimental(ExperimentalTime::class)
    @Test
    fun tomorrow() {
        val localFixed = LocalDateTime(2019, 1, 30, 0, 0, 0, 0)

//        println(localFixed + 1.calendarMonths + 1.calendarDays)
//        println(localFixed + (1.calendarMonths + 1.calendarDays))
//        println(localFixed + period { 1.days + 1.months })
//        println(localFixed + Period(days = -2, months = 1))
//
//        println(localFixed + 1.days.toCalendarPeriod())

        println(localFixed.dayOfWeek)
    }

}

fun checkComponents(value: LocalDateTime, year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0, dayOfWeek: Int? = null, dayOfYear: Int? = null) {
    assertEquals(year, value.year)
    assertEquals(month, value.monthNumber)
    assertEquals(Month(month), value.month)
    assertEquals(day, value.dayOfMonth)
    assertEquals(hour, value.hour)
    assertEquals(minute, value.minute)
    assertEquals(second, value.second)
    assertEquals(nanosecond, value.nanosecond)
    if (dayOfWeek != null) assertEquals(dayOfWeek, value.dayOfWeek.number)
    if (dayOfYear != null) assertEquals(dayOfYear, value.dayOfYear)

    val fromComponents = LocalDateTime(year, month, day, hour, minute, second, nanosecond)
    checkEquals(fromComponents, value)
}

fun checkEquals(expected: LocalDateTime, actual: LocalDateTime) {
    assertEquals(expected, actual)
    assertEquals(expected.hashCode(), actual.hashCode())
    assertEquals(expected.toString(), actual.toString())
}

