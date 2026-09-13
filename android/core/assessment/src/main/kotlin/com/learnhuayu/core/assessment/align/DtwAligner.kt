package com.learnhuayu.core.assessment.align

data class DtwStep(
    val referenceIndex: Int,
    val attemptIndex: Int,
)

class DtwAlignment(
    val path: List<DtwStep>,
    val normalizedDistance: Float,
) {

    fun attemptRangeFor(referenceFrameRange: IntRange): IntRange? {
        var first = Int.MAX_VALUE
        var last = Int.MIN_VALUE
        for (step in path) {
            if (step.referenceIndex in referenceFrameRange) {
                if (step.attemptIndex < first) first = step.attemptIndex
                if (step.attemptIndex > last) last = step.attemptIndex
            }
        }
        return if (first <= last) first..last else null
    }
}

class DtwAligner(
    private val bandRadius: Int? = null,
) {

    fun align(reference: List<FloatArray>, attempt: List<FloatArray>): DtwAlignment? {
        val referenceSize = reference.size
        val attemptSize = attempt.size
        if (referenceSize == 0 || attemptSize == 0) return null

        val width = attemptSize + 1
        var previous = FloatArray(width) { Float.POSITIVE_INFINITY }
        var current = FloatArray(width) { Float.POSITIVE_INFINITY }
        val backpointers = ByteArray(referenceSize * attemptSize)

        for (referenceIndex in 0 until referenceSize) {
            java.util.Arrays.fill(current, Float.POSITIVE_INFINITY)
            val from = bandRadius?.let { maxOf(0, referenceIndex - it) } ?: 0
            val to = bandRadius?.let { minOf(attemptSize - 1, referenceIndex + it) } ?: attemptSize - 1
            for (attemptIndex in from..to) {
                val cost = squaredDistance(reference[referenceIndex], attempt[attemptIndex])
                val diagonal = when {
                    referenceIndex == 0 && attemptIndex == 0 -> 0f
                    referenceIndex > 0 && attemptIndex > 0 -> previous[attemptIndex - 1]
                    else -> Float.POSITIVE_INFINITY
                }
                val up = if (referenceIndex > 0) previous[attemptIndex] else Float.POSITIVE_INFINITY
                val left = if (attemptIndex > 0) current[attemptIndex - 1] else Float.POSITIVE_INFINITY

                var best = diagonal
                var move = MOVE_DIAGONAL
                if (up < best) {
                    best = up
                    move = MOVE_REFERENCE
                }
                if (left < best) {
                    best = left
                    move = MOVE_ATTEMPT
                }
                current[attemptIndex] = if (best.isFinite()) best + cost else Float.POSITIVE_INFINITY
                backpointers[referenceIndex * attemptSize + attemptIndex] = move
            }
            val swap = previous
            previous = current
            current = swap
        }

        val totalCost = previous[attemptSize - 1]
        if (!totalCost.isFinite()) return null

        val reversedPath = ArrayList<DtwStep>()
        var referenceIndex = referenceSize - 1
        var attemptIndex = attemptSize - 1
        while (true) {
            reversedPath += DtwStep(referenceIndex, attemptIndex)
            if (referenceIndex == 0 && attemptIndex == 0) break
            when (backpointers[referenceIndex * attemptSize + attemptIndex]) {
                MOVE_REFERENCE -> referenceIndex--
                MOVE_ATTEMPT -> attemptIndex--
                else -> {
                    referenceIndex--
                    attemptIndex--
                }
            }
        }

        val path = reversedPath.asReversed().toList()
        return DtwAlignment(path = path, normalizedDistance = totalCost / path.size)
    }

    private fun squaredDistance(first: FloatArray, second: FloatArray): Float {
        val size = minOf(first.size, second.size)
        var sum = 0f
        for (index in 0 until size) {
            val difference = first[index] - second[index]
            sum += difference * difference
        }
        return sum
    }

    private companion object {
        const val MOVE_DIAGONAL: Byte = 0
        const val MOVE_REFERENCE: Byte = 1
        const val MOVE_ATTEMPT: Byte = 2
    }
}
