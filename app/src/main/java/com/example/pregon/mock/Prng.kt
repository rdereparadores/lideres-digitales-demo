package com.example.pregon.mock

object Prng {
    private const val SEED: Long = 2026L

    fun hashString(input: String): Long {
        var hash = 0x811c9dc5L xor SEED
        for (ch in input) {
            hash = hash xor ch.code.toLong()
            hash = (hash * 0x01000193L) and 0xFFFFFFFFL
        }
        return hash
    }

    fun boundedInt(key: String, min: Int, max: Int): Int {
        if (min >= max) return min
        val h = hashString(key)
        val range = (max - min + 1)
        val value = ((h % range + range) % range).toInt()
        return min + value
    }

    fun boundedDouble(key: String, min: Double, max: Double): Double {
        val h = hashString(key)
        val norm = (h % 10000).toDouble() / 10000.0
        return min + norm * (max - min)
    }
}
