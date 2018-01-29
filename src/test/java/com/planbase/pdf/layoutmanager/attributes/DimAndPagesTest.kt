package com.planbase.pdf.layoutmanager.attributes

import com.planbase.pdf.layoutmanager.attributes.DimAndPages.Companion.INVALID_PAGE_RANGE
import com.planbase.pdf.layoutmanager.attributes.DimAndPages.Companion.maxExtents
import org.junit.Assert.*
import org.junit.Test

class DimAndPagesTest {
    @Test fun testMaxExtentsStaticFun() {
        val ir0to1 = IntRange(0, 1)
        assertEquals(ir0to1, maxExtents(INVALID_PAGE_RANGE, ir0to1))
        assertEquals(ir0to1, maxExtents(ir0to1, ir0to1))
        assertEquals(INVALID_PAGE_RANGE, maxExtents(INVALID_PAGE_RANGE, INVALID_PAGE_RANGE))

        assertEquals(IntRange(0, 2), maxExtents(ir0to1, IntRange(1, 2)))

        assertEquals(IntRange(-1, 1), maxExtents(ir0to1, IntRange(-1, 0)))

        assertEquals(IntRange(-1, 2), maxExtents(ir0to1, IntRange(-1, 2)))
        assertEquals(IntRange(-1, 2), maxExtents(IntRange(-1, 2), ir0to1))
    }
}