/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds
import kotlin.test.*

class InstantIsoStringsTest {

    @Test
    fun parseDates() {
        fun Int.zeroPadded(digits: Int): String = when {
            this >= 0 -> "${toString().padStart(digits, '0')}"
            else -> "-${absoluteValue.toString().padStart(digits, '0')}"
        }
        fun localDateToString(year: Int, month: Int, day: Int) =
            "${year.zeroPadded(4)}-${month.zeroPadded(2)}-${day.zeroPadded(2)}"
        // only works for 1-4-digit years
        fun assertMonthBoundariesAreCorrect(year: Int, month: Int, lastDayOfMonth: Int) {
            val validString = "${localDateToString(year, month, lastDayOfMonth)}T23:59:59Z"
            val invalidString = "${localDateToString(year, month, lastDayOfMonth + 1)}T23:59:59Z"
            parseInstant(validString) // shouldn't throw
            assertInvalidFormat(invalidString) { parseInstant(invalidString) }
        }
        val nonLeapYears = listOf(
            1970, 1971, 1973, 1974, 1975, 2021, 2022, 2023, 2100, 1100, 1, 2, 3, 5, -1, -2, -1971, 100, -100
        )
        val leapYears = listOf(
            0, 1972, 1976, 1980, 2000, 1200, 400, -400, -4, -8,
        )
        for ((month, lastDayOfMonth) in arrayOf(
            1 to 31, 3 to 31, 4 to 30, 5 to 31, 6 to 30,
            7 to 31, 8 to 31, 9 to 30, 10 to 31, 11 to 30, 12 to 31,
        )) {
            for (year in nonLeapYears + leapYears) {
                assertMonthBoundariesAreCorrect(year, month, lastDayOfMonth)
            }
        }
        for (leapYear in leapYears) {
            assertMonthBoundariesAreCorrect(leapYear, 2, 29)
        }
        for (nonLeapYear in nonLeapYears) {
            assertMonthBoundariesAreCorrect(nonLeapYear, 2, 28)
        }
    }

    @Test
    fun parseIsoString() {
        for ((str, seconds, nanos) in arrayOf(
            // all components are taken into accout
            Triple("1970-01-01T00:00:00Z", 0, 0),
            Triple("1970-01-01T00:00:00.000000001Z", 0, 1),
            Triple("1970-01-01T00:00:00.100Z", 0, 100000000),
            Triple("1970-01-01T00:00:01Z", 1, 0),
            Triple("1970-01-01T00:01:00Z", 60, 0),
            Triple("1970-01-01T00:01:01Z", 61, 0),
            Triple("1970-01-01T00:01:01.000000001Z", 61, 1),
            Triple("1970-01-01T01:00:00Z", 3600, 0),
            Triple("1970-01-01T01:01:01.000000001Z", 3661, 1),
            Triple("1970-01-02T01:01:01.100Z", 90061, 100000000),
            Triple("1970-02-02T01:01:01.100Z", 31 * 86400 + 90061, 100000000),
            Triple("1971-02-02T01:01:01.100Z", (365 + 31) * 86400 + 90061, 100000000),
            // how many digits get output for various precision of the sub-second portion
            Triple("1970-01-01T00:00:00.100Z", 0, 100_000_000),
            Triple("1970-01-01T00:00:00.010Z", 0, 10_000_000),
            Triple("1970-01-01T00:00:00.001Z", 0, 1_000_000),
            Triple("1970-01-01T00:00:00.000100Z", 0, 100_000),
            Triple("1970-01-01T00:00:00.000010Z", 0, 10_000),
            Triple("1970-01-01T00:00:00.000001Z", 0, 1_000),
            Triple("1970-01-01T00:00:00.000000100Z", 0, 100),
            Triple("1970-01-01T00:00:00.000000010Z", 0, 10),
            Triple("1970-01-01T00:00:00.000000001Z", 0, 1),
            // random data queried from java.time
            Triple("+51861-09-21T11:07:43.782719883Z", 1574430692863, 782719883),
            Triple("+395069-04-30T01:28:37.454777349Z", 12405016603717, 454777349),
            Triple("-551259-03-05T08:01:36.195722269Z", -17458215523104, 195722269),
            Triple("+498403-02-11T17:47:05.156642423Z", 15665915958425, 156642423),
            Triple("+283686-10-14T23:00:25.666521845Z", 8890123158025, 666521845),
            Triple("-910329-04-04T09:27:54.456784744Z", -28789367639526, 456784744),
            Triple("-37222-03-21T18:04:37.006055123Z", -1236773166923, 6055123),
            Triple("-189377-03-30T01:37:14.288808090Z", -6038320515766, 288808090),
            Triple("-67394-03-24T03:19:41.794404047Z", -2188909341619, 794404047),
            Triple("-870649-05-27T13:47:39.925150102Z", -27537183223941, 925150102),
            Triple("+94020-04-10T14:51:21.569206089Z", 2904826114281, 569206089),
            Triple("-945485-07-11T23:28:58.240153828Z", -29898775384262, 240153828),
            Triple("-73722-02-22T11:19:54.364548772Z", -2388604250406, 364548772),
            Triple("-645899-05-17T16:44:21.522135477Z", -20444759104539, 522135477),
            Triple("-702594-10-20T10:13:53.212104714Z", -22233867083167, 212104714),
            Triple("-442579-11-22T01:35:44.591216727Z", -14028583357456, 591216727),
            Triple("-849915-06-25T01:28:27.625015449Z", -26882878833093, 625015449),
            Triple("-481897-08-13T05:44:47.077814711Z", -15269348340913, 77814711),
            Triple("+295919-02-07T15:47:37.850981753Z", 9276137682457, 850981753),
            Triple("+967334-01-15T15:08:10.235167075Z", 30463946694490, 235167075),
            Triple("+774237-04-30T16:00:32.810606451Z", 24370403011232, 810606451),
            Triple("+792959-05-03T08:18:31.616194572Z", 24961212490711, 616194572),
            Triple("-261823-02-16T03:17:35.085815500Z", -8324498983345, 85815500),
            Triple("+931062-03-22T17:04:54.135075640Z", 29319318637494, 135075640),
            Triple("+623320-01-26T03:08:05.121769356Z", 19607914264085, 121769356),
            Triple("+322804-03-06T11:31:24.788006817Z", 10124548774284, 788006817),
            Triple("-784322-04-03T21:25:19.666588404Z", -24812970806081, 666588404),
            Triple("+403293-01-07T05:59:41.601460200Z", 12664531288781, 601460200),
            Triple("-835821-06-01T00:52:15.782852248Z", -26438117296065, 782852248),
            Triple("+222483-07-15T08:29:55.019931345Z", 6958735086595, 19931345),
            Triple("-663595-09-05T04:36:24.110433196Z", -21003181356216, 110433196),
            Triple("+166626-02-15T22:16:34.070665743Z", 5196045449794, 70665743),
            Triple("-517158-01-02T22:52:24.155574933Z", -16382097162456, 155574933),
            Triple("+850155-01-02T10:25:31.349473798Z", 26766133467931, 349473798),
            Triple("-967697-04-25T20:43:33.328060156Z", -30599725115787, 328060156),
            Triple("+437131-04-26T07:32:58.134219875Z", 13732364705578, 134219875),
            Triple("+372920-11-25T13:38:22.852562723Z", 11706079786702, 852562723),
            Triple("+169255-09-07T11:28:18.481625778Z", 5279026303698, 481625778),
            Triple("-980786-08-18T17:05:22.581779094Z", -31012764044078, 581779094),
            Triple("+182945-05-25T20:39:24.545585221Z", 5711031952764, 545585221),
            Triple("+300811-12-15T02:53:38.676752671Z", 9430541175218, 676752671),
            Triple("-807816-01-18T18:04:26.291749218Z", -25554376389334, 291749218),
            Triple("-53033-12-30T22:02:01.398533618Z", -1735695568679, 398533618),
            Triple("-354903-06-14T10:08:46.111648055Z", -11261809864274, 111648055),
            Triple("+842009-03-11T23:58:06.537554993Z", 26509076495886, 537554993),
            Triple("-391976-11-09T04:16:17.862484469Z", -12431707962223, 862484469),
            Triple("-733019-10-28T17:07:13.450343935Z", -23193986539967, 450343935),
            Triple("+595280-03-05T23:36:27.765851400Z", 18723060833787, 765851400),
            Triple("-930296-07-17T03:33:33.094509320Z", -29419456335987, 94509320),
            Triple("+609508-02-29T10:58:02.703241053Z", 19172052557882, 703241053),
            Triple("+996233-06-25T06:01:55.647461964Z", 31375924927315, 647461964),
            Triple("-93200-12-06T21:29:56.140938343Z", -3003245692204, 140938343),
            Triple("+794143-07-02T09:49:35.585085194Z", 24998581100975, 585085194),
            Triple("-783550-12-31T17:10:16.577723428Z", -24788585371784, 577723428),
            Triple("-240168-11-03T17:22:09.108424624Z", -7641110702271, 108424624),
            Triple("+613419-02-15T12:00:07.012460989Z", 19295470641607, 12460989),
            Triple("-521405-03-25T02:03:46.552711998Z", -16516112536574, 552711998),
            Triple("-938829-01-22T16:48:43.582709371Z", -29688747030677, 582709371),
            Triple("+916785-05-16T21:54:45.983221956Z", 28868784818085, 983221956),
            Triple("+482425-06-09T04:24:32.683186155Z", 15161709183872, 683186155),
            Triple("+622585-08-20T05:45:52.555088343Z", 19584737819152, 555088343),
            Triple("-451048-11-02T01:49:29.076392891Z", -14295840847831, 76392891),
            Triple("+721083-09-17T00:31:34.648020241Z", 22693036811494, 648020241),
            Triple("+235979-10-28T12:07:33.706273641Z", 7384636728453, 706273641),
            Triple("+285234-04-12T18:30:25.215363003Z", 8938957285825, 215363003),
            Triple("-917176-03-10T10:03:25.943265324Z", -29005440213395, 943265324),
            Triple("-381932-09-05T02:47:17.004960541Z", -12114755529163, 4960541),
            Triple("-52158-11-11T09:38:45.489915403Z", -1708087530075, 489915403),
            Triple("-584290-11-15T20:15:24.377620606Z", -18500551127076, 377620606),
            Triple("-645616-05-05T17:36:59.941608628Z", -20435829488581, 941608628),
            Triple("+794405-06-22T21:08:20.853641989Z", 25006848239300, 853641989),
            Triple("+986590-08-01T05:15:25.827177433Z", 31071624470125, 827177433),
            Triple("+527158-02-06T12:34:35.088546391Z", 16573335654875, 88546391),
            Triple("-513116-05-01T07:28:44.448204123Z", -16254533665876, 448204123),
            Triple("+397065-10-19T21:59:05.831855226Z", 12468019211945, 831855226),
            Triple("+312769-04-26T11:33:07.802217284Z", 9807879123187, 802217284),
            Triple("+682473-04-14T01:00:38.067076018Z", 21474609498038, 67076018),
            Triple("+731560-02-15T02:15:06.599802467Z", 23023640456106, 599802467),
            Triple("-877354-10-27T22:55:02.723751549Z", -27748759338298, 723751549),
            Triple("-746193-01-02T07:19:56.258497483Z", -23609743807204, 258497483),
            Triple("-822112-07-28T08:55:19.319285417Z", -26005498038281, 319285417),
            Triple("-400365-04-30T00:05:51.210582736Z", -12696455980449, 210582736),
            Triple("+436254-07-11T18:08:06.937065549Z", 13704695921286, 937065549),
            Triple("-340854-01-07T03:17:32.367173472Z", -10818479997748, 367173472),
            Triple("-985221-04-25T22:57:01.511559459Z", -31152729085379, 511559459),
            Triple("+859861-09-01T02:21:20.289341591Z", 27072446149280, 289341591),
            Triple("-0131-07-16T10:47:54.756333457Z", -66284140326, 756333457),
            Triple("-327041-11-18T22:55:21.885337272Z", -10382556503079, 885337272),
            Triple("-268616-05-06T10:27:54.420166505Z", -8538858480726, 420166505),
            Triple("-228012-05-16T15:26:54.680432991Z", -7257519160386, 680432991),
            Triple("+857168-09-12T13:29:36.945689251Z", 26987464272576, 945689251),
            Triple("-974181-04-12T08:47:35.627678735Z", -30804341526745, 627678735),
            Triple("-435700-10-20T22:33:13.897477229Z", -13811505874007, 897477229),
            Triple("-507467-01-19T23:06:05.156792267Z", -16076277276835, 156792267),
            Triple("-382257-11-19T08:00:10.407963305Z", -12125005142390, 407963305),
            Triple("+83082-01-04T20:18:56.409867424Z", 2559647852336, 409867424),
            Triple("-916839-09-12T22:45:39.091941363Z", -28994789466861, 91941363),
            Triple("-147771-05-07T08:31:34.950238979Z", -4725358615706, 950238979),
        )) {
            val instant = parseInstant(str)
            assertEquals(
                seconds.toLong() * 1000 + nanos / 1000000, instant.toEpochMilliseconds(),
                "Parsed $instant from $str, with Unix time = `$seconds + 10^-9 * $nanos`"
            )
            assertEquals(str, formatIso(instant))
        }
        // non-canonical strings are parsed as well, but formatted differently
        for ((str, seconds, nanos) in arrayOf(
            // upper, lower case, trailing zeros
            Triple("2024-07-15T14:06:29.461245000z", 1721052389, 461245000),
            Triple("2024-07-15t14:06:29.4612450z", 1721052389, 461245000),
            // current time
            Triple("2024-07-15T16:06:29.461245691+02:00", 1721052389, 461245691),
        )) {
            val instant = parseInstant(str)
            assertEquals(
                seconds.toLong() * 1000 + nanos / 1000000, instant.toEpochMilliseconds(),
                "Parsed $instant from $str, with Unix time = `$seconds + 10^-9 * $nanos`"
            )
        }
    }

    @Test
    fun nonParseableInstantStrings() {
        for (nonIsoString in listOf(
            // empty string
            "",
            // a non-empty but clearly unsuitable string
            "x",
            // something other than a sign at the beginning
            " 1970-01-01T00:00:00Z",
            // too many digits for the year
            "+1234567890-01-01T00:00:00Z",
            "-1234567890-01-01T00:00:00Z",
            // not enough padding for the year
            "003-01-01T00:00:00Z",
            "-003-01-01T00:00:00Z",
            // a plus sign even though there is only 4 digits
            "+1970-01-01T00:00:00Z",
            // too many digits without padding
            "11970-01-01T00:00:00Z",
            // incorrect separators between the components
            "1970/01-01T00:00:00Z",
            "1970-01/01T00:00:00Z",
            "1970-01-01 00:00:00Z",
            "1970-01-01T00-00:00Z",
            "1970-01-01T00:00-00Z",
            // non-digits where digits are expected
            "1970-X1-01T00:00:00Z",
            "1970-1X-01T00:00:00Z",
            "1970-11-X1T00:00:00Z",
            "1970-11-1XT00:00:00Z",
            "1970-11-10TX0:00:00Z",
            "1970-11-10T0X:00:00Z",
            "1970-11-10T00:X0:00Z",
            "1970-11-10T00:0X:00Z",
            "1970-11-10T00:00:X0Z",
            "1970-11-10T00:00:0XZ",
            // a non-ascii digit
            "1970-11-10T00:00:0٩Z",
            // not enough components
            "1970-11-10T00:00Z",
            // not enough components, even if the length is sufficient
            "1970-11-10T00:00+01:15",
            // a dot without any fraction of the second following it
            "1970-11-10T00:00:00.Z",
            // too many digits in the fraction of the second
            "1970-11-10T00:00:00.1234567890Z",
            // out-of-range values
            "1970-00-10T00:00:00Z",
            "1970-13-10T00:00:00Z",
            "1970-01-32T00:00:00Z",
            "1970-02-29T00:00:00Z",
            "1972-02-30T00:00:00Z",
            "2000-02-30T00:00:00Z",
            "2100-02-29T00:00:00Z",
            "2004-02-30T00:00:00Z",
            "2005-02-29T00:00:00Z",
            "2005-04-31T00:00:00Z",
            "2005-04-01T24:00:00Z",
            "2005-04-01T00:60:00Z",
            "2005-04-01T00:00:60Z",
            // leap second
            "1970-01-01T23:59:60Z",
            // lack of padding
            "1970-1-10T00:00:00+05:00",
            "1970-10-1T00:00:00+05:00",
            "1970-10-10T0:00:00+05:00",
            "1970-10-10T00:0:00+05:00",
            "1970-10-10T00:00:0+05:00",
            // no offset
            "1970-02-03T04:05:06.123456789",
            // some invalid single-character offsets
            "1970-02-03T04:05:06.123456789A",
            "1970-02-03T04:05:06.123456789+",
            "1970-02-03T04:05:06.123456789-",
            // too many components in the offset
            "1970-02-03T04:05:06.123456789+03:02:01:00",
            "1970-02-03T04:05:06.123456789+03:02:01.02",
            // single-digit offset
            "1970-02-03T04:05:06.123456789+3",
            // incorrect sign in the offset
            "1970-02-03T04:05:06.123456789 03",
            // non-digits in the offset
            "1970-02-03T04:05:06.123456789+X3",
            "1970-02-03T04:05:06.123456789+1X",
            "1970-02-03T04:05:06.123456789+X3:12",
            "1970-02-03T04:05:06.123456789+1X:12",
            "1970-02-03T04:05:06.123456789+X3:12",
            "1970-02-03T04:05:06.123456789+13:X2",
            "1970-02-03T04:05:06.123456789+13:1X",
            "1970-02-03T04:05:06.123456789+X3:12:59",
            "1970-02-03T04:05:06.123456789+1X:12:59",
            "1970-02-03T04:05:06.123456789+X3:12:59",
            "1970-02-03T04:05:06.123456789+13:X2:59",
            "1970-02-03T04:05:06.123456789+13:1X:59",
            "1970-02-03T04:05:06.123456789+13:12:X9",
            "1970-02-03T04:05:06.123456789+13:12:5X",
            // incorrect separators in the offset
            "1970-02-03T04:05:06.123456789+13/12",
            "1970-02-03T04:05:06.123456789+13/12:59",
            "1970-02-03T04:05:06.123456789+13:12/59",
            "1970-02-03T04:05:06.123456789+0130",
            "1970-02-03T04:05:06.123456789-0130",
            // incorrect field length
            "1970-02-03T04:05:06.123456789-18:001",
            // out-of-range offsets
            "1970-02-03T04:05:06.123456789+18:12:59",
            "1970-02-03T04:05:06.123456789-18:12:59",
            "1970-02-03T04:05:06.123456789+18:00:01",
            "1970-02-03T04:05:06.123456789-18:00:01",
            "1970-02-03T04:05:06.123456789+18:01",
            "1970-02-03T04:05:06.123456789-18:01",
            "1970-02-03T04:05:06.123456789+19",
            "1970-02-03T04:05:06.123456789-19",
            // out-of-range fields of the offset
            "1970-02-03T04:05:06.123456789+01:12:60",
            "1970-02-03T04:05:06.123456789-01:12:60",
            "1970-02-03T04:05:06.123456789+01:60",
            "1970-02-03T04:05:06.123456789-01:60",
            // lack of padding in the offset
            "1970-02-03T04:05:06.123456789+1:12:50",
            "1970-02-03T04:05:06.123456789+01:2:60",
            "1970-02-03T04:05:06.123456789+01:12:6",
        )) {
            assertInvalidFormat(nonIsoString) { parseInstant(nonIsoString) }
        }
        // this string represents an Instant that is currently larger than Instant.MAX any of the implementations:
        assertInvalidFormat { parseInstant  ("+1000000001-12-31T23:59:59.000000000Z") }
    }

    @Test
    fun parseStringsWithOffsets() {
        val strings = arrayOf(
            Pair("2020-01-01T00:01:01.02+18:00", "2019-12-31T06:01:01.020Z"),
            Pair("2020-01-01T00:01:01.123456789-17:59:59", "2020-01-01T18:01:00.123456789Z"),
            Pair("2020-01-01T00:01:01.010203040+17:59:59", "2019-12-31T06:01:02.010203040Z"),
            Pair("2020-01-01T00:01:01.010203040+17:59", "2019-12-31T06:02:01.010203040Z"),
            Pair("2020-01-01T00:01:01+00", "2020-01-01T00:01:01Z"),
        )
        strings.forEach { (str, strInZ) ->
            val instant = parseInstant(str)
            assertEquals(parseInstant(strInZ), instant, str)
            assertEquals(strInZ, formatIso(instant), str)
        }
        assertInvalidFormat { parseInstant("2020-01-01T00:01:01+18:01") }
        assertInvalidFormat { parseInstant("2020-01-01T00:01:01+1801") }
        assertInvalidFormat { parseInstant("2020-01-01T00:01:01+0") }
        assertInvalidFormat { parseInstant("2020-01-01T00:01:01+") }
        assertInvalidFormat { parseInstant("2020-01-01T00:01:01") }
        assertInvalidFormat { parseInstant("2020-01-01T00:01:01+000000") }

        val instants = listOf(
            Instant.DISTANT_FUTURE,
            Instant.DISTANT_PAST,
            Instant.fromEpochSeconds(0, 0),
            Instant.parse("2020-01-02T03:04:05.6789Z"),
            Instant.MAX,
            Instant.MIN,
        )

        val offsets = listOf(
            0 to "Z",
            3 * 3600 + 12 * 60 + 14 to "+03:12:14",
            - 3 * 3600 - 12 * 60 - 14 to "-03:12:14",
            2 * 3600 + 35 * 60 to "+02:35",
            - 2 * 3600 - 35 * 60 to "-02:35",
            4 * 3600 to "+04",
            - 4 * 3600 to "-04",
        )

        for (instant in instants) {
            for ((offsetSeconds, offsetString) in offsets) {
                if (instant == Instant.MAX && offsetSeconds < 0 ||
                    instant == Instant.MIN && offsetSeconds > 0
                ) continue
                val newInstant = Instant.parse("${instant.toString().dropLast(1)}$offsetString")
                assertEquals(newInstant, instant.minus(offsetSeconds.seconds))
            }
        }
    }

    private fun parseInstant(isoString: String): Instant {
        return parseIso(isoString)
    }

    private fun displayInstant(instant: Instant): String {
        return formatIso(instant)
    }
}


@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
private fun <T> assertInvalidFormat(message: String? = null, f: () -> T) {
    assertFailsWith<IllegalArgumentException>(message) {
        val result = f()
        fail(result.toString())
    }
}