package com.testdone.app.domain.logic

/**
 * v2.3.16 — dotted-version comparison for the force-update gate.
 *
 * Handles sloppy input safely: "2.3.16", "2.3", "2.3.16-beta" all parse;
 * missing segments count as 0; non-numeric junk never throws.
 */
object Versioning {

    fun parse(raw: String?): IntArray {
        if (raw.isNullOrBlank()) return intArrayOf(0, 0, 0)
        return raw.trim()
            .substringBefore('-')          // ignore -beta / -rc suffixes
            .split('.')
            .take(3)
            .map { segment ->
                segment.filter { it.isDigit() }.take(2).ifEmpty { "0" }.toInt()
            }
            .let { parts ->
                intArrayOf(
                    parts.getOrElse(0) { 0 },
                    parts.getOrElse(1) { 0 },
                    parts.getOrElse(2) { 0 },
                )
            }
    }

    /**
     * True when [candidate] is NEWER than [current] — i.e. the installed
     * build ([current]) is below the minimum required version.
     *
     *   isNewer("2.4.0", "2.3.16")  → true
     *   isNewer("2.3.16", "2.3.16") → false  (same version = no force update)
     *   isNewer("garbage", "2.3.16") → false
     */
    fun isNewer(candidate: String?, current: String?): Boolean {
        val a = parse(candidate)
        val b = parse(current)
        for (i in 0..2) {
            if (a[i] != b[i]) return a[i] > b[i]
        }
        return false
    }
}
