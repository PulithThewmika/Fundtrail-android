package com.fundtrail.domain.model

import java.time.YearMonth
import java.time.ZoneId

/** Months are always cut in Sri Lanka time, whatever the device zone. */
val APP_ZONE: ZoneId = ZoneId.of("Asia/Colombo")

/** Epoch-millis bounds of this month in [zone]: start inclusive, end exclusive. */
fun YearMonth.toMillisRange(zone: ZoneId = APP_ZONE): Pair<Long, Long> =
    atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() to
        plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
