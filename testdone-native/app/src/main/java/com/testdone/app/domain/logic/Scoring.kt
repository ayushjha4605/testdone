package com.testdone.app.domain.logic

import com.testdone.app.domain.model.TestAttempt
import java.util.Calendar

/** Scoring utilities — ported 1:1 from the web app so numbers stay consistent. */

/** Honest percentile derived from score% (higher score → higher percentile). */
fun estimatePercentile(scorePct: Double): Double {
    if (scorePct.isNaN() || scorePct.isInfinite()) return 0.0
    val clamped = scorePct.coerceIn(0.0, 100.0)
    return (maxOf(1.0, minOf(99.9, clamped)) * 10).toInt() / 10.0
}

/** Deterministic estimated AIR from percentile & the exam's real applicant base. */
fun estimateAir(candidateBase: Long, percentile: Double): Long {
    val base = if (candidateBase > 0) candidateBase else 100_000L
    return maxOf(1L, Math.round(((100.0 - percentile) / 100.0) * base))
}

/** Consecutive-day streak ending today or yesterday, computed from attempt timestamps. */
fun computeStreak(timestamps: List<Long>): Int {
    if (timestamps.isEmpty()) return 0
    fun dayKey(ts: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }
    fun dayKeyOffset(offset: Int): Int {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }
    val days = timestamps.map(::dayKey).toSet()
    val start = when {
        days.contains(dayKeyOffset(0)) -> 0
        days.contains(dayKeyOffset(-1)) -> -1
        else -> return 0
    }
    var streak = 0
    var off = start
    while (days.contains(dayKeyOffset(off))) {
        streak++
        off--
    }
    return streak
}

/** Human-friendly relative time ("2h ago"). */
fun timeAgo(ts: Long, now: Long = System.currentTimeMillis()): String {
    val s = ((now - ts) / 1000L).coerceAtLeast(0)
    return when {
        s < 60 -> "just now"
        s < 3600 -> "${s / 60}m ago"
        s < 86400 -> "${s / 3600}h ago"
        else -> "${s / 86400}d ago"
    }
}

/** Format seconds as m:ss or h:mm:ss. */
fun formatDuration(totalSec: Int): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun greeting(nowHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): Int {
    return when {
        nowHour < 12 -> 0 // morning
        nowHour < 17 -> 1 // afternoon
        else -> 2         // evening
    }
}

/** Per-subject aggregate across all attempts (for analytics). */
fun subjectPerformance(attempts: List<TestAttempt>): List<Pair<String, Int>> {
    val map = LinkedHashMap<String, Pair<Int, Int>>() // name -> (correct, total)
    for (a in attempts) {
        for (sec in a.sectionResults) {
            val cur = map.getOrPut(sec.subjectName) { 0 to 0 }
            map[sec.subjectName] = (cur.first + sec.correct) to (cur.second + sec.total)
        }
    }
    return map.map { (name, c) ->
        val short = if (name.length > 12) name.take(12) + "…" else name
        short to if (c.second > 0) (c.first * 100 / c.second) else 0
    }
}
