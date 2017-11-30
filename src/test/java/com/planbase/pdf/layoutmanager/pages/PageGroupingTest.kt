package com.planbase.pdf.layoutmanager.pages

import com.planbase.pdf.layoutmanager.PdfLayoutMgr
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import org.junit.Test

import java.io.ByteArrayOutputStream
import java.io.IOException

import com.planbase.pdf.layoutmanager.PdfLayoutMgr.Orientation.LANDSCAPE
import com.planbase.pdf.layoutmanager.PdfLayoutMgr.Orientation.PORTRAIT
import com.planbase.pdf.layoutmanager.attributes.LineStyle
import com.planbase.pdf.layoutmanager.attributes.TextStyle
import com.planbase.pdf.layoutmanager.contents.ScaledImage
import com.planbase.pdf.layoutmanager.contents.Text
import com.planbase.pdf.layoutmanager.utils.Utils
import com.planbase.pdf.layoutmanager.utils.Dimensions
import com.planbase.pdf.layoutmanager.utils.Point2d
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.common.PDRectangle.*
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertEquals
import java.io.File
import java.io.FileOutputStream
import java.lang.Float.max
import java.util.Collections
import javax.imageio.ImageIO

class PageGroupingTest {
    @Test
    @Throws(IOException::class)
    fun testBasics() {
        var pageMgr = PdfLayoutMgr(PDDeviceRGB.INSTANCE, Dimensions(LETTER))
        var lp = pageMgr.logicalPageStart()

        // Just testing some default values before potentially merging changes that could make
        // these variable.
        assertEquals((LETTER.width - PdfLayoutMgr.DEFAULT_MARGIN).toDouble(), lp.yBodyTop().toDouble(), 0.000000001)
        assertEquals(PdfLayoutMgr.DEFAULT_MARGIN.toDouble(), lp.yBodyBottom().toDouble(), 0.000000001)
        assertEquals(LETTER.height.toDouble(), lp.pageWidth().toDouble(), 0.000000001)
        assertEquals((LETTER.width - PdfLayoutMgr.DEFAULT_MARGIN * 2).toDouble(), lp.bodyHeight().toDouble(), 0.000000001)

        lp = pageMgr.logicalPageStart(PORTRAIT)

        assertEquals((LETTER.height - PdfLayoutMgr.DEFAULT_MARGIN).toDouble(), lp.yBodyTop().toDouble(), 0.000000001)
        assertEquals(PdfLayoutMgr.DEFAULT_MARGIN.toDouble(), lp.yBodyBottom().toDouble(), 0.000000001)
        assertEquals(LETTER.width.toDouble(), lp.pageWidth().toDouble(), 0.000000001)
        assertEquals((LETTER.height - PdfLayoutMgr.DEFAULT_MARGIN * 2).toDouble(), lp.bodyHeight().toDouble(), 0.000000001)

        // Write to nothing to suppress the "stream not committed" warning
        lp.commit()
        pageMgr.save(ByteArrayOutputStream())

        // Make a new manager for a new test.
        pageMgr = PdfLayoutMgr(PDDeviceCMYK.INSTANCE, Dimensions(A1))
        lp = pageMgr.logicalPageStart(PORTRAIT)

        assertEquals((A1.height - PdfLayoutMgr.DEFAULT_MARGIN).toDouble(), lp.yBodyTop().toDouble(), 0.000000001)
        assertEquals(PdfLayoutMgr.DEFAULT_MARGIN.toDouble(), lp.yBodyBottom().toDouble(), 0.000000001)
        assertEquals(A1.width.toDouble(), lp.pageWidth().toDouble(), 0.000000001)
        assertEquals((A1.height - PdfLayoutMgr.DEFAULT_MARGIN * 2).toDouble(), lp.bodyHeight().toDouble(), 0.000000001)

        lp = pageMgr.logicalPageStart()

        assertEquals((A1.width - PdfLayoutMgr.DEFAULT_MARGIN).toDouble(), lp.yBodyTop().toDouble(), 0.000000001)
        assertEquals(PdfLayoutMgr.DEFAULT_MARGIN.toDouble(), lp.yBodyBottom().toDouble(), 0.000000001)
        assertEquals(A1.height.toDouble(), lp.pageWidth().toDouble(), 0.000000001)
        assertEquals((A1.width - PdfLayoutMgr.DEFAULT_MARGIN * 2).toDouble(), lp.bodyHeight().toDouble(), 0.000000001)

        // Write to nothing to suppress the "stream not committed" warning
        lp.commit()
        pageMgr.save(ByteArrayOutputStream())

        val topM = 20f
        val bottomM = 60f
        // Make a new manager for a new test.
        pageMgr = PdfLayoutMgr(PDDeviceGray.INSTANCE, Dimensions(A6))
        lp = PageGrouping(pageMgr, PORTRAIT, Point2d(PdfLayoutMgr.DEFAULT_MARGIN, bottomM),
                          pageMgr.pageDim.minus(Dimensions(PdfLayoutMgr.DEFAULT_MARGIN * 2, topM + bottomM)))

        assertEquals((A6.height - topM).toDouble(), lp.yBodyTop().toDouble(), 0.000000001)
        assertEquals(bottomM.toDouble(), lp.yBodyBottom().toDouble(), 0.000000001)
        assertEquals(A6.width.toDouble(), lp.pageWidth().toDouble(), 0.000000001)
        assertEquals((A6.height - (topM + bottomM)).toDouble(), lp.bodyHeight().toDouble(), 0.000000001)

        // Write to nothing to suppress the "stream not committed" warning
        lp.commit()
        pageMgr.save(ByteArrayOutputStream())

        // Make a new manager for a new test.
        pageMgr = PdfLayoutMgr(PDDeviceGray.INSTANCE, Dimensions(A6))
        lp = PageGrouping(pageMgr, LANDSCAPE, Point2d(PdfLayoutMgr.DEFAULT_MARGIN, bottomM),
                          pageMgr.pageDim.swapWh()
                                                                       .minus(Dimensions(PdfLayoutMgr.DEFAULT_MARGIN * 2, topM + bottomM)))

        assertEquals((A6.width - topM).toDouble(), lp.yBodyTop().toDouble(), 0.000000001)
        assertEquals(bottomM.toDouble(), lp.yBodyBottom().toDouble(), 0.000000001)
        assertEquals(A6.height.toDouble(), lp.pageWidth().toDouble(), 0.000000001)
        assertEquals((A6.width - (topM + bottomM)).toDouble(), lp.bodyHeight().toDouble(), 0.000000001)

        // Write to nothing to suppress the "stream not committed" warning
        lp.commit()
        pageMgr.save(ByteArrayOutputStream())
    }

    @Test fun testBasics2() {
        val pageMgr = PdfLayoutMgr(PDDeviceRGB.INSTANCE, Dimensions(PDRectangle.LETTER))
        val lp = pageMgr.logicalPageStart()
        val f = File("target/test-classes/melon.jpg")
        val melonPic = ImageIO.read(f)
        val melonHeight = 100f
        val melonWidth = 170f
        val bigMelon = ScaledImage(melonPic, Dimensions(melonWidth, melonHeight)).wrap()
        val text = Text(TextStyle(PDType1Font.HELVETICA_BOLD, 11f, Utils.RGB_BLACK), "Hello")

        val squareSide = 70f
        val squareDim = Dimensions(squareSide, squareSide)

        // TODO: The topLeft parameters on RenderTarget are actually LOWER-left.

        val melonX = lp.bodyTopLeft().x
        val textX = melonX + melonWidth + 10
        val squareX = textX + text.maxWidth() + 10
        val lineX1 = squareX + squareSide + 10
        val lineX2 = lineX1 + 100
        var y = lp.yBodyTop() - melonHeight

        while(y >= lp.yBodyBottom()) {
            val imgY = lp.drawImage(Point2d(melonX, y), bigMelon)
            assertEquals(melonHeight, imgY)

            val txtY = lp.drawStyledText(Point2d(textX, y), text.text, text.textStyle)
            assertEquals(text.textStyle.lineHeight(), txtY)

            val rectY = lp.fillRect(Point2d(squareX, y), squareDim, Utils.RGB_BLACK)
            assertEquals(squareSide, rectY)

            lp.drawLine(Point2d(lineX1, y), Point2d(lineX2, y), LineStyle(Utils.RGB_BLACK, 1f))

            y -= melonHeight
        }

        while(y >= lp.yBodyBottom() - 300) {
            val imgY:Float = lp.drawImage(Point2d(melonX, y), bigMelon)
//            assertEquals(melonHeight, imgY)

            val txtY:Float = lp.drawStyledText(Point2d(textX, y), text.text, text.textStyle)
//            assertEquals(text.textStyle.lineHeight(), txtY)

            val rectY:Float = lp.fillRect(Point2d(squareX, y), squareDim, Utils.RGB_BLACK)
//            assertEquals(squareSide, rectY)

            lp.drawLine(Point2d(lineX1, y), Point2d(lineX2, y), LineStyle(Utils.RGB_BLACK, 1f))

            println("imgY=$imgY, txtY=$txtY, rectY=$rectY")

            y -= listOf(imgY, txtY, rectY).max() as Float
        }

        lp.commit()
        val os = FileOutputStream("pageGrouping.pdf")
        pageMgr.save(os)
    }

}