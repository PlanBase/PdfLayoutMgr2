package com.planbase.pdf.layoutmanager.lineWrapping

//import kotlin.test.assertEquals
import com.planbase.pdf.layoutmanager.PdfLayoutMgr
import com.planbase.pdf.layoutmanager.attributes.LineStyle
import com.planbase.pdf.layoutmanager.attributes.TextStyle
import com.planbase.pdf.layoutmanager.contents.Text
import com.planbase.pdf.layoutmanager.utils.CMYK_BLACK
import com.planbase.pdf.layoutmanager.utils.RGB_BLACK
import com.planbase.pdf.layoutmanager.utils.Dim
import com.planbase.pdf.layoutmanager.utils.Coord
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.FileOutputStream
import kotlin.test.assertTrue

class MultiLineWrappedTest {
    private val floatCloseEnough = 0.000004f

    @Test fun testLine() {
        val tStyle1 = TextStyle(PDType1Font.TIMES_ROMAN, 60f, CMYK_BLACK)
        val txt1 = Text(tStyle1, "Hello ")
        val tStyle2 = TextStyle(PDType1Font.TIMES_BOLD, 100f, CMYK_BLACK)
        val txt2 = Text(tStyle2, "there ")
        val txt3 = Text(tStyle1, "world!")
        val line = MultiLineWrapped()
//        println("txt1.style().lineHeight(): " + txt1.style().lineHeight())
        line.append(txt1.lineWrapper().getSomething(999f).item)
        assertEquals(tStyle1.lineHeight(), line.dim.height, floatCloseEnough)

        line.append(txt2.lineWrapper().getSomething(999f).item)
        assertEquals(tStyle2.lineHeight(), line.dim.height, floatCloseEnough)

        line.append(txt3.lineWrapper().getSomething(999f).item)
        assertEquals(tStyle2.lineHeight(), line.dim.height, floatCloseEnough)

        // This is for the baseline!
        val pageMgr = PdfLayoutMgr(PDDeviceRGB.INSTANCE, Dim(PDRectangle.LETTER))
        val lp = pageMgr.startPageGrouping()
        val yTop = lp.yBodyTop() - 10f
        val yBottom = yTop - tStyle2.lineHeight()
        val upperLeft = Coord(100f, yTop)
        lp.drawLine(Coord(0f, yTop), Coord(lp.pageWidth(), yTop), LineStyle(RGB_BLACK, 0.125f), true)
        lp.drawLine(Coord(0f, yBottom), Coord(lp.pageWidth(), yBottom), LineStyle(RGB_BLACK, 0.125f), true)
        val dim = line.render(lp, upperLeft)
        assertEquals(line.dim, dim)

        lp.commit()
        val os = FileOutputStream("multiLineBaseline.pdf")
        pageMgr.save(os)
    }

//    @Ignore

    fun verifyLine(line: MultiLineWrapped, lineHeight:Float, maxWidth:Float, text:String) {
//        println("line: " + line)
        assertEquals(lineHeight, line.dim.height, floatCloseEnough)
        assertTrue(line.width < maxWidth)
        assertEquals(text,
                     line.items
                             .fold(StringBuilder(),
                                   {acc, item -> acc.append((item as Text.WrappedText).string)})
                             .toString())
    }

    @Test fun testRenderablesToLines() {
        val tStyle1 = TextStyle(PDType1Font.HELVETICA, 9f, CMYK_BLACK)
        val txt1 = Text(tStyle1, "Hello ")
        val tStyle2 = TextStyle(PDType1Font.HELVETICA_BOLD, 13f, CMYK_BLACK)
        val txt2 = Text(tStyle2, "there ")
        val txt3 = Text(tStyle1, "world! This is great stuff.")
        val maxWidth = 60f

        val MultiLineWrappeds: List<MultiLineWrapped> = lineWrappablesToMultiLineWrappeds(listOf(txt1, txt2, txt3), maxWidth)
//        println(MultiLineWrappeds)

        assertEquals(3, MultiLineWrappeds.size)

        verifyLine(MultiLineWrappeds[0], tStyle2.lineHeight(), maxWidth, "Hello there")

        verifyLine(MultiLineWrappeds[1], tStyle1.lineHeight(), maxWidth, "world! This is")

        verifyLine(MultiLineWrappeds[2], tStyle1.lineHeight(), maxWidth, "great stuff.")
    }

    @Test fun testRenderablesToLines2() {
        val tStyle1 = TextStyle(PDType1Font.HELVETICA, 9f, CMYK_BLACK)
        val txt1 = Text(tStyle1, "Hello ")
        val tStyle2 = TextStyle(PDType1Font.HELVETICA_BOLD, 13f, CMYK_BLACK)
        val txt2 = Text(tStyle2, "there ")
        val txt3 = Text(tStyle1, "world! This is great stuff.")
        val maxWidth = 90f

        val MultiLineWrappeds: List<MultiLineWrapped> = lineWrappablesToMultiLineWrappeds(listOf(txt1, txt2, txt3), maxWidth)
//        println(MultiLineWrappeds)

        assertEquals(2, MultiLineWrappeds.size)

        verifyLine(MultiLineWrappeds[0], tStyle2.lineHeight(), maxWidth, "Hello there world!")

        verifyLine(MultiLineWrappeds[1], tStyle1.lineHeight(), maxWidth, "This is great stuff.")
    }

    @Test fun testRenderablesToLines3() {
        val tStyle1 = TextStyle(PDType1Font.HELVETICA, 9f, CMYK_BLACK)
        val txt1 = Text(tStyle1, "Hello there world! This is great stuff.")
        val maxWidth = 300f

        val MultiLineWrappeds: List<MultiLineWrapped> = lineWrappablesToMultiLineWrappeds(listOf(txt1), maxWidth)
//        println(MultiLineWrappeds)

        assertEquals(1, MultiLineWrappeds.size)

        verifyLine(MultiLineWrappeds[0], tStyle1.lineHeight(), maxWidth, "Hello there world! This is great stuff.")
    }


    @Test fun testRenderablesToLinesTerminal() {
        val tStyle1 = TextStyle(PDType1Font.HELVETICA, 9f, CMYK_BLACK)
        val txt1 = Text(tStyle1, "Hello\nthere world! This\nis great stuff.")
        // This is 300 just like previous test, showing this can fit on one line
        // So we know the line breaks are due to the \n characters.
        val maxWidth = 300f

        val MultiLineWrappeds: List<MultiLineWrapped> = lineWrappablesToMultiLineWrappeds(listOf(txt1), maxWidth)
//        println(MultiLineWrappeds)

        assertEquals(3, MultiLineWrappeds.size)

        verifyLine(MultiLineWrappeds[0], tStyle1.lineHeight(), maxWidth, "Hello")
        verifyLine(MultiLineWrappeds[1], tStyle1.lineHeight(), maxWidth, "there world! This")
        verifyLine(MultiLineWrappeds[2], tStyle1.lineHeight(), maxWidth, "is great stuff.")
    }

    @Test fun testRenderablesToLinesTerminal2() {
        val tStyle1 = TextStyle(PDType1Font.HELVETICA, 9f, CMYK_BLACK)
        val txt1 = Text(tStyle1, "Hello\nthere world! This\nis great stuff.\n")
        // This is 300 just like previous test, showing this can fit on one line
        // So we know the line breaks are due to the \n characters.
        val maxWidth = 300f

        val MultiLineWrappeds: List<MultiLineWrapped> = lineWrappablesToMultiLineWrappeds(listOf(txt1), maxWidth)
//        println(MultiLineWrappeds)

        assertEquals(4, MultiLineWrappeds.size)

//        println("line3: " + MultiLineWrappeds[3])

        verifyLine(MultiLineWrappeds[0], tStyle1.lineHeight(), maxWidth, "Hello")
        verifyLine(MultiLineWrappeds[1], tStyle1.lineHeight(), maxWidth, "there world! This")
        verifyLine(MultiLineWrappeds[2], tStyle1.lineHeight(), maxWidth, "is great stuff.")
        // Additional blank line has same height as previous one.
        verifyLine(MultiLineWrappeds[3], tStyle1.lineHeight(), maxWidth, "")
    }

    @Test fun testRenderablesToLinesMultiReturn() {
        val tStyle1 = TextStyle(PDType1Font.HELVETICA, 9f, CMYK_BLACK)
        val txt1 = Text(tStyle1, " Hello    \n\n\n  there world! This\n\nis great stuff.     \n\n")
        // This is 300 just like previous test, showing this can fit on one line
        // So we know the line breaks are due to the \n characters.
        val maxWidth = 300f

        val MultiLineWrappeds: List<MultiLineWrapped> = lineWrappablesToMultiLineWrappeds(listOf(txt1), maxWidth)
//        println(MultiLineWrappeds)

        assertEquals(8, MultiLineWrappeds.size)
//        println("line3: " + MultiLineWrappeds[3])

        verifyLine(MultiLineWrappeds[0], tStyle1.lineHeight(), maxWidth, " Hello")
        verifyLine(MultiLineWrappeds[1], tStyle1.lineHeight(), maxWidth, "")
        verifyLine(MultiLineWrappeds[2], tStyle1.lineHeight(), maxWidth, "")
        verifyLine(MultiLineWrappeds[3], tStyle1.lineHeight(), maxWidth, "  there world! This")
        verifyLine(MultiLineWrappeds[4], tStyle1.lineHeight(), maxWidth, "")
        verifyLine(MultiLineWrappeds[5], tStyle1.lineHeight(), maxWidth, "is great stuff.")
        verifyLine(MultiLineWrappeds[6], tStyle1.lineHeight(), maxWidth, "")
        verifyLine(MultiLineWrappeds[7], tStyle1.lineHeight(), maxWidth, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRenderablesToLinesEx() {
        lineWrappablesToMultiLineWrappeds(listOf(), -1f)
    }
}