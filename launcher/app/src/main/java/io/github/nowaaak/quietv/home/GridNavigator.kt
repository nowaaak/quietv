package io.github.nowaaak.quietv.home

enum class Direction { UP, DOWN, LEFT, RIGHT }

class GridNavigator(private val columns: Int) {

    fun isDock(position: Int): Boolean = position < columns

    fun vertical(position: Int, count: Int, direction: Direction): Int? = when (direction) {
        Direction.UP -> if (isDock(position)) null else position - columns
        Direction.DOWN -> {
            val lastRowStart = ((count - 1) / columns) * columns
            if (position >= lastRowStart) position else minOf(position + columns, count - 1)
        }
        else -> null
    }

    fun moveTarget(position: Int, count: Int, direction: Direction): Int? {
        val target = when (direction) {
            Direction.LEFT -> position - 1
            Direction.RIGHT -> position + 1
            Direction.UP -> position - columns
            Direction.DOWN -> {
                val lastRowStart = ((count - 1) / columns) * columns
                if (position >= lastRowStart) return null
                minOf(position + columns, count - 1)
            }
        }
        return target.takeIf { it in 0 until count && it != position }
    }
}
