package org.cap.gold.util

import kotlin.math.abs
import kotlin.math.round

fun formatAmount(amount: Double): String {
    val negative = amount < 0
    val value = abs(amount)
    val whole = value.toLong()
    val fraction = round((value - whole) * 100).toInt()

    val digits = whole.toString()
    val sb = StringBuilder()
    var group = 0
    for (i in digits.length - 1 downTo 0) {
        sb.append(digits[i])
        group++
        if (group == 3 && i != 0) {
            sb.append(',')
            group = 0
        }
    }
    val grouped = sb.reverse().toString()
    val fracStr = fraction.toString().padStart(2, '0')
    val result = "$grouped.$fracStr"
    return if (negative) "-$result" else result
}
