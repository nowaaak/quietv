package io.github.nowaaak.quietv.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GridNavigatorTest {

    private val nav = GridNavigator(columns = 4)

    @Test
    fun upFromTheDockLeavesTheGrid() {
        assertNull(nav.vertical(2, count = 10, direction = Direction.UP))
    }

    @Test
    fun upAndDownMoveExactlyOneRow() {
        assertEquals(1, nav.vertical(5, count = 10, direction = Direction.UP))
        assertEquals(5, nav.vertical(1, count = 10, direction = Direction.DOWN))
    }

    @Test
    fun downIntoAShortLastRowLandsOnItsLastTile() {
        assertEquals(9, nav.vertical(7, count = 10, direction = Direction.DOWN))
    }

    @Test
    fun downOnTheLastRowStays() {
        assertEquals(9, nav.vertical(9, count = 10, direction = Direction.DOWN))
        assertEquals(8, nav.vertical(8, count = 10, direction = Direction.DOWN))
    }

    @Test
    fun moveStaysInsideTheGrid() {
        assertNull(nav.moveTarget(0, count = 10, direction = Direction.LEFT))
        assertNull(nav.moveTarget(9, count = 10, direction = Direction.RIGHT))
        assertNull(nav.moveTarget(2, count = 10, direction = Direction.UP))
        assertEquals(3, nav.moveTarget(2, count = 10, direction = Direction.RIGHT))
        assertEquals(9, nav.moveTarget(7, count = 10, direction = Direction.DOWN))
        assertNull(nav.moveTarget(9, count = 10, direction = Direction.DOWN))
    }
}
