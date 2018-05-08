package com.planbase.pdf.layoutmanager.contents

import TestManualllyPdfLayoutMgr.Companion.RGB_BLUE_GREEN
import TestManualllyPdfLayoutMgr.Companion.RGB_LIGHT_GREEN
import TestManualllyPdfLayoutMgr.Companion.RGB_YELLOW_BRIGHT
import TestManualllyPdfLayoutMgr.Companion.letterLandscapeBody
import com.planbase.pdf.layoutmanager.PdfLayoutMgr
import com.planbase.pdf.layoutmanager.PdfLayoutMgr.Orientation.*
import com.planbase.pdf.layoutmanager.attributes.Align
import com.planbase.pdf.layoutmanager.attributes.BorderStyle
import com.planbase.pdf.layoutmanager.attributes.BoxStyle
import com.planbase.pdf.layoutmanager.attributes.CellStyle
import com.planbase.pdf.layoutmanager.attributes.DimAndPageNums
import com.planbase.pdf.layoutmanager.attributes.Padding
import com.planbase.pdf.layoutmanager.attributes.TextStyle
import com.planbase.pdf.layoutmanager.lineWrapping.MultiLineWrapped
import com.planbase.pdf.layoutmanager.utils.Coord
import com.planbase.pdf.layoutmanager.utils.Dim
import com.planbase.pdf.layoutmanager.utils.RGB_BLACK
import junit.framework.TestCase
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import org.junit.Test
import kotlin.test.assertEquals

class WrappedCellTest {

    @Test fun testBasics() {
        val pageMgr = PdfLayoutMgr(PDDeviceRGB.INSTANCE, Dim(PDRectangle.LETTER))
        val lp = pageMgr.startPageGrouping(LANDSCAPE, letterLandscapeBody)
        val cellWidth = 200.0
        val hello = Text(textStyle, "Hello")
        val cell = Cell(CellStyle(Align.BOTTOM_CENTER, boxStyle),
                        cellWidth, listOf(hello), null)
//        println(cell)
        val wrappedCell: WrappedCell = cell.wrap()
//        println(wrappedCell)

        kotlin.test.assertEquals(textStyle.lineHeight + cell.cellStyle.boxStyle.topBottomInteriorSp(),
                                 wrappedCell.dim.height)

        kotlin.test.assertEquals(cellWidth,
                                 wrappedCell.dim.width)

        val upperLeft = Coord(100.0, 500.0)

        kotlin.test.assertEquals(cellWidth,
                                 wrappedCell.dim.width)

        val dimAndPageNums: DimAndPageNums = wrappedCell.render(lp, upperLeft)
        Dim.assertEquals(wrappedCell.dim, dimAndPageNums.dim, 0.00002)

        pageMgr.commit()
    }

    @Test fun testMultiLine() {
        val pageMgr = PdfLayoutMgr(PDDeviceRGB.INSTANCE, Dim(PDRectangle.LETTER))
        val lp = pageMgr.startPageGrouping(LANDSCAPE, letterLandscapeBody)
        val cellWidth = 300.0
        val hello = Text(textStyle, "Hello\nThere\nWorld!")
        val cell = Cell(CellStyle(Align.BOTTOM_CENTER, boxStyle),
                        cellWidth, listOf(hello), null)
//        println(cell)
        val wrappedCell: WrappedCell = cell.wrap()
//        println(wrappedCell)

        kotlin.test.assertEquals((textStyle.lineHeight * 3) + cell.cellStyle.boxStyle.topBottomInteriorSp(),
                                 wrappedCell.dim.height)

        kotlin.test.assertEquals(cellWidth,
                                 wrappedCell.dim.width)

        val upperLeft = Coord(100.0, 500.0)

        kotlin.test.assertEquals(cellWidth,
                                 wrappedCell.dim.width)

        wrappedCell.render(lp, upperLeft)
        pageMgr.commit()

//        val os = FileOutputStream("test4.pdf")
//        pageMgr.save(os)
    }

    @Test fun testRightAlign() {
        val pageMgr = PdfLayoutMgr(PDDeviceRGB.INSTANCE, Dim(PDRectangle.LETTER))
        val lp = pageMgr.startPageGrouping(LANDSCAPE, letterLandscapeBody)

        val cellWidth = 200.0
        val hello = Text(textStyle, "Hello")
        val cell = Cell(CellStyle(Align.TOP_RIGHT, boxStyle),
                        cellWidth, listOf(hello), null)

        val wrappedCell =
                WrappedCell(Dim(cellWidth,
                                  textStyle.lineHeight + boxStyle.topBottomInteriorSp()),
                            CellStyle(Align.TOP_RIGHT, boxStyle),
                            listOf({
                                       val mlw = MultiLineWrapped()
                                       mlw.width = hello.maxWidth()
                                       mlw.ascent = textStyle.ascent
                                       mlw.lineHeight = textStyle.lineHeight
                                       mlw.append(
                                               WrappedText(
                                                       textStyle,
                                                       hello.text,
                                                       hello.maxWidth()
                                               )
                                       )
                                       mlw
                                   }.invoke()), 0.0)
//        val wrappedCell = cell.wrap()
//        println("cell.wrap()=${cell.wrap()}")

        kotlin.test.assertEquals(textStyle.lineHeight + cell.cellStyle.boxStyle.topBottomInteriorSp(),
                                 wrappedCell.dim.height)

        kotlin.test.assertEquals(cellWidth,
                                 wrappedCell.dim.width)

        val upperLeft = Coord(100.0, 500.0)

        kotlin.test.assertEquals(cellWidth,
                                 wrappedCell.dim.width)

        val dimAndPageNums: DimAndPageNums = wrappedCell.render(lp, upperLeft)
//        println("upperLeft=" + upperLeft)
//        println("xyOff=" + xyOff)

        assertEquals(cellWidth, dimAndPageNums.dim.width)

        Dim.assertEquals(wrappedCell.dim, dimAndPageNums.dim, 0.00002)

        pageMgr.commit()

//        // We're just going to write to a file.
//        // Commit it to the output stream!
//        val os = FileOutputStream("wrappedCellRight.pdf")
//        pageMgr.save(os)
    }

    // There was only one significant line changed when I added this test without any comments.
    // Looks like I had assumed pageBreakingTopMargin wanted the text baseline which was above the curent y-value,
    // but it actually needs the bottom of the text area which is below the current y-value.
    //
    // This comment is based on:
    // git diff 13d097b86807ff458191a01633e1d507abcf3fc3 e2958def12f99beb699fc7546f5f7f0024b22df7
    // In class WrappedCell:
    // - val adjY = lp.pageBreakingTopMargin(y + line.descentAndLeading, line.lineHeight) + line.lineHeight
    // + val adjY = lp.pageBreakingTopMargin(y - line.lineHeight, line.lineHeight) + line.lineHeight
    @Test fun testCellHeightBug() {
        val pageMgr = PdfLayoutMgr(PDDeviceRGB.INSTANCE, Dim(PDRectangle.LETTER))
        val lp = pageMgr.startPageGrouping(LANDSCAPE, letterLandscapeBody)
        val textStyle = TextStyle(PDType1Font.COURIER_BOLD_OBLIQUE, 12.0, RGB_YELLOW_BRIGHT)
        val cellStyle = CellStyle(Align.BOTTOM_CENTER, BoxStyle(Padding(2.0), RGB_BLUE_GREEN, BorderStyle(RGB_BLACK)))

        val tB = Table()
                .addCellWidths(listOf(120.0))
                .textStyle(textStyle)
                .partBuilder()
                .cellStyle(cellStyle)
                .rowBuilder().addTextCells("First").buildRow()
                .buildPart()
        val wrappedTable = tB.wrap()

        TestCase.assertEquals(textStyle.lineHeight + cellStyle.boxStyle.topBottomInteriorSp(),
                              wrappedTable.dim.height)

        TestCase.assertEquals(120.0, wrappedTable.dim.width)

        val dimAndPageNums: DimAndPageNums = wrappedTable.render(lp, lp.body.topLeft)

        Dim.assertEquals(wrappedTable.dim, dimAndPageNums.dim, 0.00003)

        pageMgr.commit()
//        val os = FileOutputStream("test3.pdf")
//        pageMgr.save(os)
    }

    companion object {
        val boxStyle = BoxStyle(Padding(2.0), RGB_LIGHT_GREEN, BorderStyle(RGB_BLACK))
        private val textStyle = TextStyle(PDType1Font.HELVETICA, 9.5, RGB_BLACK)
    }
}