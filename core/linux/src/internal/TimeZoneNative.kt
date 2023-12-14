/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal actual fun currentSystemDefaultZone(): RegionTimeZone {
    val zoneId = pathToSystemDefault()?.second?.toString()
        ?: throw IllegalStateException("Failed to get the system timezone")
    return RegionTimeZone(systemTzdb.rulesForId(zoneId), zoneId)
}

internal actual fun defaultTzdbPath(): String? = null