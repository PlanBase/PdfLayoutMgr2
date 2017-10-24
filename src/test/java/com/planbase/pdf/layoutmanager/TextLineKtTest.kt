package com.planbase.pdf.layoutmanager

import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertEquals
import org.junit.Test
//import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextLineKtTest {
    private val floatCloseEnough = 0.000002f

    @Test fun testLine() {
        val tStyle1 = TextStyle(PDType1Font.HELVETICA, 9f, Utils.CMYK_BLACK)
        val txt1 = Text(tStyle1, "Hello ")
        val tStyle2 = TextStyle(PDType1Font.HELVETICA_BOLD, 13f, Utils.CMYK_BLACK)
        val txt2 = Text(tStyle2, "there ")
        val txt3 = Text(tStyle1, "world!")
        val line = TextLine()
//        println("txt1.style().lineHeight(): " + txt1.style().lineHeight())
        line.append(txt1.lineWrapper().getSomething(999f).item)
        assertEquals(tStyle1.lineHeight(), line.height(), floatCloseEnough)

        line.append(txt2.lineWrapper().getSomething(999f).item)
        assertEquals(tStyle2.lineHeight(), line.height(), floatCloseEnough)

        line.append(txt3.lineWrapper().getSomething(999f).item)
        assertEquals(tStyle2.lineHeight(), line.height(), floatCloseEnough)
    }

//    @Ignore

    fun verifyLine(line:TextLine, lineHeight:Float, maxWidth:Float, text:String) {
//        println("line: " + line)
        assertEquals(lineHeight, line.height(), floatCloseEnough)
        assertTrue(line.width < maxWidth)
        assertEquals(text,
                     line.items
                             .fold(StringBuilder(),
                                   {acc, item -> acc.append((item as Text.WrappedRow).string)})
                             .toString())
    }

    @Test fun testRenderablesToLines() {
        val tStyle1 = TextStyle(PDType1Font.HELVETICA, 9f, Utils.CMYK_BLACK)
        val txt1 = Text(tStyle1, "Hello ")
        val tStyle2 = TextStyle(PDType1Font.HELVETICA_BOLD, 13f, Utils.CMYK_BLACK)
        val txt2 = Text(tStyle2, "there ")
        val txt3 = Text(tStyle1, "world! This is great stuff.")
        val maxWidth = 60f

        val textLines: List<TextLine> = renderablesToTextLines(listOf(txt1, txt2, txt3), maxWidth)
//        println(textLines)

        assertEquals(3, textLines.size)

        verifyLine(textLines[0], tStyle2.lineHeight(), maxWidth, "Hello there")

        verifyLine(textLines[1], tStyle1.lineHeight(), maxWidth, "world! This is")

        verifyLine(textLines[2], tStyle1.lineHeight(), maxWidth, "great stuff.")
    }

    @Test fun testRenderablesToLines2() {
        val tStyle1 = TextStyle(PDType1Font.HELVETICA, 9f, Utils.CMYK_BLACK)
        val txt1 = Text(tStyle1, "Hello ")
        val tStyle2 = TextStyle(PDType1Font.HELVETICA_BOLD, 13f, Utils.CMYK_BLACK)
        val txt2 = Text(tStyle2, "there ")
        val txt3 = Text(tStyle1, "world! This is great stuff.")
        val maxWidth = 90f

        val textLines: List<TextLine> = renderablesToTextLines(listOf(txt1, txt2, txt3), maxWidth)
//        println(textLines)

        assertEquals(2, textLines.size)

        verifyLine(textLines[0], tStyle2.lineHeight(), maxWidth, "Hello there world!")

        verifyLine(textLines[1], tStyle1.lineHeight(), maxWidth, "This is great stuff.")
    }

    @Test fun testRenderablesToLines3() {
        val tStyle1 = TextStyle(PDType1Font.HELVETICA, 9f, Utils.CMYK_BLACK)
        val txt1 = Text(tStyle1, "Hello there world! This is great stuff.")
        val maxWidth = 300f

        val textLines: List<TextLine> = renderablesToTextLines(listOf(txt1), maxWidth)
//        println(textLines)

        assertEquals(1, textLines.size)

        verifyLine(textLines[0], tStyle1.lineHeight(), maxWidth, "Hello there world! This is great stuff.")
    }


//    @Test fun testRenderablesToLinesTerminal() {
//        val tStyle1 = TextStyle(PDType1Font.HELVETICA, 9f, Utils.CMYK_BLACK)
//        val txt1 = Text(tStyle1, "Hello\nthere world! This is great stuff.")
//        val maxWidth = 300f
//
//        val textLines: List<TextLine> = renderablesToTextLines(listOf(txt1), maxWidth)
//        println(textLines)
//
//        assertEquals(2, textLines.size)
//
//        verifyLine(textLines[0], tStyle1.lineHeight(), maxWidth, "Hello")
//        verifyLine(textLines[1], tStyle1.lineHeight(), maxWidth, "there world! This is great stuff.")
//    }

}