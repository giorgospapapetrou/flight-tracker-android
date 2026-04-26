package com.giorgospapapetrou.flightfinder.domain.model

import java.time.Instant

data class FlightPosition(
    val timestamp: Instant,
    val lat: Double?,
    val lon: Double?,
    val altitudeFt: Int?,
    val groundSpeedKt: Int?,
    val headingDeg: Int?,
    val verticalRateFpm: Int?,
) {
    val hasPosition: Boolean get() = lat != null && lon != null
}

data class FlightDetail(
    val summary: FlightSummary,
    val positions: List<FlightPosition>,
) {
    /**
     * Positions filtered to only those with valid coordinates.
     * This is what we use for path drawing and replay.
     */
    val drawablePositions: List<FlightPosition> by lazy {
        positions.filter { it.hasPosition }
    }

    val durationMillis: Long
        get() {
            if (drawablePositions.size < 2) return 0L
            return drawablePositions.last().timestamp.toEpochMilli() -
                    drawablePositions.first().timestamp.toEpochMilli()
        }

    /**
     * Interpolate position state at a fractional time (0.0 = start, 1.0 = end).
     * Returns null if there are not enough drawable positions.
     */
    fun interpolateAt(fraction: Double): FlightPosition? {
        val points = drawablePositions
        if (points.size < 2) return points.firstOrNull()

        val clamped = fraction.coerceIn(0.0, 1.0)
        val startMs = points.first().timestamp.toEpochMilli()
        val endMs = points.last().timestamp.toEpochMilli()
        val targetMs = startMs + ((endMs - startMs) * clamped).toLong()

        // Binary search for the two surrounding points
        var lo = 0
        var hi = points.size - 1
        while (lo < hi - 1) {
            val mid = (lo + hi) / 2
            if (points[mid].timestamp.toEpochMilli() <= targetMs) lo = mid else hi = mid
        }

        val before = points[lo]
        val after = points[hi]
        val beforeMs = before.timestamp.toEpochMilli()
        val afterMs = after.timestamp.toEpochMilli()

        if (afterMs == beforeMs) return before
        val t = (targetMs - beforeMs).toDouble() / (afterMs - beforeMs).toDouble()

        return FlightPosition(
            timestamp = Instant.ofEpochMilli(targetMs),
            lat = lerp(before.lat, after.lat, t),
            lon = lerp(before.lon, after.lon, t),
            altitudeFt = lerp(before.altitudeFt, after.altitudeFt, t),
            groundSpeedKt = lerp(before.groundSpeedKt, after.groundSpeedKt, t),
            headingDeg = lerpHeading(before.headingDeg, after.headingDeg, t),
            verticalRateFpm = lerp(before.verticalRateFpm, after.verticalRateFpm, t),
        )
    }

    private fun lerp(a: Double?, b: Double?, t: Double): Double? {
        if (a == null) return b
        if (b == null) return a
        return a + (b - a) * t
    }

    private fun lerp(a: Int?, b: Int?, t: Double): Int? {
        if (a == null) return b
        if (b == null) return a
        return (a + (b - a) * t).toInt()
    }

    /**
     * Heading interpolation across the 0/360 boundary.
     * E.g., heading 350° -> 10° should go through 0°, not backwards through 180°.
     */
    private fun lerpHeading(a: Int?, b: Int?, t: Double): Int? {
        if (a == null) return b
        if (b == null) return a
        var diff = (b - a) % 360
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360
        return ((a + diff * t).toInt() % 360 + 360) % 360
    }
}