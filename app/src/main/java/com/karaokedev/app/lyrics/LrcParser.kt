package com.karaokedev.app.lyrics

object LrcParser {
    data class LyricLine(val timeMs: Long, val text: String)

    private val tagRegex = Regex("""\[(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    fun parse(content: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        content.lines().forEach { raw ->
            val matches = tagRegex.findAll(raw).toList()
            if (matches.isEmpty()) return@forEach
            val text = raw.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty()) return@forEach
            matches.forEach { match ->
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val fraction = match.groupValues[3]
                val ms = when (fraction.length) {
                    0 -> 0L
                    1 -> fraction.toLong() * 100
                    2 -> fraction.toLong() * 10
                    else -> fraction.take(3).toLong()
                }
                lines.add(LyricLine(minutes * 60000 + seconds * 1000 + ms, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    fun currentLine(lines: List<LyricLine>, positionMs: Long): String? {
        var current: String? = null
        for (line in lines) {
            if (line.timeMs <= positionMs) current = line.text else break
        }
        return current
    }
}
