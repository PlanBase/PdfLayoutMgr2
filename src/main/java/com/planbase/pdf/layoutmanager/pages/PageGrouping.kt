// Copyright 2017 PlanBase Inc.
//
// This file is part of PdfLayoutMgr2
//
// PdfLayoutMgr is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// PdfLayoutMgr is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with PdfLayoutMgr.  If not, see <https://www.gnu.org/licenses/agpl-3.0.en.html>.
//
// If you wish to use this code with proprietary software,
// contact PlanBase Inc. <https://planbase.com> to purchase a commercial license.

package com.planbase.pdf.layoutmanager.pages

import com.planbase.pdf.layoutmanager.PdfLayoutMgr
import com.planbase.pdf.layoutmanager.PdfLayoutMgr.Orientation
import com.planbase.pdf.layoutmanager.PdfLayoutMgr.Orientation.PORTRAIT
import com.planbase.pdf.layoutmanager.attributes.CellStyle
import com.planbase.pdf.layoutmanager.attributes.DimAndPages
import com.planbase.pdf.layoutmanager.attributes.DimAndPages.Companion.maxExtents
import com.planbase.pdf.layoutmanager.attributes.LineStyle
import com.planbase.pdf.layoutmanager.attributes.PageArea
import com.planbase.pdf.layoutmanager.attributes.TextStyle
import com.planbase.pdf.layoutmanager.contents.Cell
import com.planbase.pdf.layoutmanager.contents.ScaledImage.WrappedImage
import com.planbase.pdf.layoutmanager.lineWrapping.LineWrappable
import com.planbase.pdf.layoutmanager.lineWrapping.LineWrapped
import com.planbase.pdf.layoutmanager.utils.Coord
import com.planbase.pdf.layoutmanager.utils.Dim
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.color.PDColor
import java.io.IOException
import java.util.TreeSet
import kotlin.math.ceil

/**
 *
 * Maybe better called a "DocumentSection" this represents a group of Renderables that logically
 * belong on the same page, but may spill over multiple subsequent pages as necessary in order to
 * fit.  Headers and footers are tied to this Logical Page / Document Section.
 *
 *
 * Here is a typical page layout:
 * <pre>`
 * +---------------------+ -.
 * | M  Margin Header  M |  |
 * | a +-------------+ a |   > Margin body top
 * | r |    Header   | r |  |
 * | g +-------------+ g | -'
 * |   |             |   |
 * | B |             | B |
 * | o |     Body    | o |
 * | d |             | d |
 * | y |             | y |
 * |   +-------------+   | -.
 * | L |    Footer   | R |  |
 * | e +-------------+ t |   > Margin body bottom
 * | f  Margin Footer    |  |
 * +---------------------+ -'
`</pre> *
 *
 *
 * Here is our model
 * <pre>`
 * +--------------------+
 * |                    |
 * |                    |
 * |   +------------+   | <- yBodyTop()
 * |   |           h|   |
 * |   |           e|   |
 * |   |           i|   |
 * |   |    Body   g|   |
 * |   |           h|   |
 * |   |w i d t h  t|   |
 * |   #------------+   | <- yBodyBottom()
 * |   ^                |
 * | Body               |
 * | Offset             |
 * #--------------------+
 * (0,0)
`</pre> *
 *
 *
 * Put header/footer content wherever you want.  We move the body as a unit as needed.
 *
 * Constructor
 * @param mgr the PdfLayoutMgr you are using.
 * @param orientation page orientation for this logical page grouping.
 * @param body the offset and size of the body area.
 * @return a new PageGrouping with the given settings.
 */
class PageGrouping(private val mgr: PdfLayoutMgr,
                   val orientation: Orientation,
                   override val body: PageArea) : RenderTarget { // AKA Document Section

    // borderItems apply to a logical section
    private val borderItems = TreeSet<SinglePage.PdfItem>()

    val yBodyBottom:Float = body.topLeft.y - body.dim.height

    private var valid = true

    fun invalidate() { valid = false }

    override fun toString(): String =
            "PageGrouping(pageDim=${if (orientation == PORTRAIT) mgr.pageDim else mgr.pageDim.swapWh()}" +
            " $orientation body=$body)"

    // ===================================== Instance Methods =====================================

    // TODO: could be a val instead of a fun.
    /** The Y-value for top of the body section (in document units)  */
    fun yBodyTop(): Float = body.topLeft.y

    /**
     * Width of the entire page (in document units).  This is the short dimension for portrait,
     * the long dimension for landscape.
     */
    fun pageWidth(): Float =
            if (orientation == PORTRAIT)
                mgr.pageDim.width
            else
                mgr.pageDim.height

    //    /**
    //     Height of the entire page (in document units).  This is the long dimension for portrait,
    //     the short dimension for landscape.
    //     */
    //    public float pageHeight() {
    //        return portrait ? mgr.pageDim().height()
    //                        : mgr.pageDim().width();
    //    }

    override fun drawStyledText(baselineLeft: Coord, text: String, textStyle: TextStyle, reallyRender: Boolean): HeightAndPage {
        if (!valid) {
            throw IllegalStateException("Logical page accessed after commit")
        }
        // Text rendering calculation spot 3/3
        val belowBaseline = textStyle.lineHeight - textStyle.ascent
        val pby = appropriatePage(baselineLeft.y - belowBaseline, textStyle.lineHeight, 0f)
        pby.pb.drawStyledText(baselineLeft.y(pby.y + belowBaseline), text, textStyle, reallyRender)
        return HeightAndPage(textStyle.lineHeight + pby.adj, pby.pb.pageNum)
    }

    override fun drawImage(bottomLeft: Coord, wi: WrappedImage, reallyRender: Boolean): HeightAndPage {
        if (!valid) {
            throw IllegalStateException("Logical page accessed after commit")
        }
        // Calculate what page image should start on
        val pby = appropriatePage(bottomLeft.y, wi.dim.height, 0f)
        // draw image based on baseline and decrement y appropriately for image.
        pby.pb.drawImage(bottomLeft.y(pby.y), wi, reallyRender)
        return HeightAndPage(wi.dim.height + pby.adj, pby.pb.pageNum)
    }

    override fun fillRect(bottomLeft: Coord, dim: Dim, c: PDColor, reallyRender: Boolean): Float {
        if (!valid) {
            throw IllegalStateException("Logical page accessed after commit")
        }
        //        System.out.println("putRect(" + outerTopLeft + " " + outerDimensions + " " +
        //                           Utils.toString(c) + ")");
        val left = bottomLeft.x
        val topY = bottomLeft.y + dim.height
        val width = dim.width
        val maxHeight = dim.height
        val bottomY = bottomLeft.y

        if (topY < bottomY) {
            throw IllegalStateException("height must be positive")
        }
        // logger.info("About to put line: (" + x1 + "," + y1 + "), (" + x2 + "," + y2 + ")");
        val pby1 = appropriatePage(topY, 0f, 0f)
        val pby2 = appropriatePage(bottomY, 0f, 0f)
        if (pby1 == pby2) {
            pby1.pb.fillRect(Coord(left, pby1.y), Dim(width, maxHeight), c, reallyRender)
        } else {
            val totalPages = pby2.pb.pageNum - pby1.pb.pageNum + 1

            var currPage = pby1.pb
            // The first x and y are correct for the first page.  The second x and y will need to
            // be adjusted below.
            var ya:Float
            var yb:Float

            for (pageNum in 1..totalPages) {
                // On all except the first page the first y will start at the top of the page.
                // lt or equals, because can never be greater than
                ya = if (pby1.pb.pageNum < currPage.pageNum) {
                    yBodyTop()
                } else {
                    pby1.y
                }

                // the second Y must be adjusted by the height of the pages already printed.
                // On all except the last page, the second-y will end at the bottom of the page.
                yb = if (pageNum == totalPages) {
                    pby2.y
                } else {
                    yBodyBottom
                }

                currPage.fillRect(Coord(left, yb), Dim(width, ya - yb), c, reallyRender)

                // pageNum is one-based while get is zero-based, so passing get the current
                // pageNum actually gets the next page.  Don't get another one after we already
                // processed the last page!
                if (pageNum < totalPages) {
                    currPage = mgr.page(currPage.pageNum)
                }
            }
        }

        return maxHeight + pby2.adj
    }

    // TODO: this should be the have all the gory details.  drawLine should inherit from the default implementation
    override fun drawLineStrip(points: List<Coord>, lineStyle: LineStyle, reallyRender: Boolean): IntRange {
        if (!valid) {
            throw IllegalStateException("Logical page accessed after commit")
        }

        // TODO: Find min and max Y.  If they are on the same page, just pass params to SinglePage.drawLineStrip
        var start: Coord = points[0]
        var pageNums:IntRange = DimAndPages.INVALID_PAGE_RANGE
        for (i in 1..points.lastIndex) {
            val end = points[i]
            val currRange:IntRange = drawLine(start, end, lineStyle, reallyRender)
            pageNums = maxExtents(pageNums, currRange)
            start = end
        }
        return pageNums
    }

    override fun drawLine(start: Coord, end: Coord, lineStyle: LineStyle, reallyRender: Boolean): IntRange {
        if (!valid) {
            throw IllegalStateException("Logical page accessed after commit")
        }

//        println("About to put line: start=$start end=$end")
        val flip:Boolean = end.y > start.y

        val pby1 = appropriatePage(if (flip) { end.y } else { start.y }, 0f, 0f)
        val pby2 = appropriatePage(if (flip) { start.y } else { end.y }, 0f, 0f)
//        println("pby1=$pby1, pby2=$pby2")
        if (pby1 == pby2) {
            if (flip) {
                pby1.pb.drawLine(end.y(pby1.y), start.y(pby2.y), lineStyle, reallyRender)
            } else {
                pby1.pb.drawLine(start.y(pby1.y), end.y(pby2.y), lineStyle, reallyRender)
            }
        } else {
            val totalPages = pby2.pb.pageNum - pby1.pb.pageNum + 1
            val xDiff = end.x - start.x
            val yDiff = start.y - end.y
//            println("xDiff=$xDiff")
//            println("yDiff=$yDiff")

            var currPage = pby1.pb
            // The first x and y are correct for the first page.  The second x and y will need to
            // be adjusted below.
            var xa = if (flip) { end.x } else { start.x }
            var xb = 0f // left of page.

            for (pageNum in 1..totalPages) {
                if (pageNum > 1) {
                    // The x-value at the start of the new page will be the same as
                    // it was on the bottom of the previous page.
                    xa = xb
                }

                val ya = if (pby1.pb.pageNum < currPage.pageNum) {
                    // On all except the first page the first y will start at the top of the page.
                    yBodyTop()
                } else { // equals, because can never be greater than
                    pby1.y
                }

                val yb:Float
                if (pageNum == totalPages) {
                    xb = if (flip) { start.x } else { end.x }
                    // the second Y must be adjusted by the height of the pages already printed.
                    yb = pby2.y
                } else {
                    // On all except the last page, the second-y will end at the bottom of the page.
                    yb = yBodyBottom

                    // This represents the x-value of the line at the bottom of one page and later
                    // becomes the x-value for the top of the next page.  It should work whether
                    // slope is negative or positive, because the sign of xDiff will reflect the
                    // slope.
                    //
                    // x1 is the starting point.
                    // xDiff is the total deltaX over all pages so it needs to be scaled by:
                    // (ya - yb) / yDiff is the proportion of the line shown on this page.
                    xb = xa + xDiff * ((ya - yb) / yDiff)
                }

//                println("(xa=$xa, ya=$ya), (xb=$xb, yb=$yb)")

                // This may look silly, but if we're doing mitering, the direction of lines is important.
                // In that case, the last endpoint of the previous line must equal the starting point of this line.
                // So if we detected that we had to flip the line to break it across pages, flip it back here!
                if (flip) {
                    currPage.drawLine(Coord(xb, yb), Coord(xa, ya), lineStyle, reallyRender)
                } else {
                    currPage.drawLine(Coord(xa, ya), Coord(xb, yb), lineStyle, reallyRender)
                }

                // pageNum is one-based while get is zero-based, so passing get the current
                // pageNum actually gets the next page.  Don't get another one after we already
                // processed the last page!
                if (pageNum < totalPages) {
                    currPage = mgr.page(currPage.pageNum)
                }
            }
        }

        return IntRange(pby1.pb.pageNum, pby2.pb.pageNum)
    }

    /**
     * Add LineWrapped items directly to the page grouping at the specified coordinate.  This is a little more
     * work than adding an entire chapter to a cell and calling Cell.render(), but it allows each top level item
     * to return a page range.  These pages can later be used to create a table of contents or an index.
     *
     * @param topLeft the coordinate to add the item at.  Might want to make a convenience version of this method
     * that internally updates a cursor so you never have to specify this.
     * @param block the LineWrapped item to display
     */
    fun add(topLeft: Coord, block: LineWrapped): DimAndPages {
        // TODO: Why is the return value ignored here?
        this.pageBreakingTopMargin(topLeft.y - body.dim.height, body.dim.height, 0f)
        val dap:DimAndPages = block.render(this, topLeft)
        cursorY = topLeft.y - dap.dim.height
        return dap
    }

    var cursorY:Float = body.topLeft.y

    /**
     * Moves cursor to the bottom of the body of the current page so that whatever you draw will get popped to the
     * next page.  The new page is not created until it is written to.
     */
    fun cursorToNewPage() {
        val prevCursorY = cursorY
        cursorY -= this.pageBreakingTopMargin(cursorY, body.dim.height, 0f)
        if (cursorY == prevCursorY) {
            cursorY -= body.dim.height
//            cursorY = cursorY.nextDown()
        }

        // Is this a better way?  I mean, if it worked?
//        cursorY = appropriatePage(cursorY, body.dim.height, 0f).y
    }

    /** Returns the vertical distance from the cursor to the bottom of the body of this page. */
    fun roomBelowCursor():Float {
        return this.pageBreakingTopMargin(cursorY, body.dim.height, 0f)
    }

    /**
     * Add LineWrapped items directly to the page grouping at the current cursor.
     * The cursor is always at the left-hand side of the body at the bottom of the last item put on the page.
     *
     * @param block the LineWrapped item to display
     */
    fun append(block: LineWrapped): DimAndPages =
            // TODO: Should have x=0 only if there is a pageReactor???
            add(Coord(0f, cursorY), block)

    /**
     * Cell goes at x=0 and cursorY.  Cell width is bodyDim.width.
     *
     * @param cellStyle the style for the cell to make
     * @param contents the contents of the cell
     */
    fun appendCell(cellStyle: CellStyle, contents:List<LineWrappable>): DimAndPages =
            // TODO: Should have x=0 only if there is a pageReactor???
            add(Coord(0f, cursorY), Cell(cellStyle, body.dim.width, contents).wrap())

    override fun pageBreakingTopMargin(bottomY:Float, height:Float, requiredSpaceBelow:Float):Float =
            appropriatePage(bottomY, height, requiredSpaceBelow).adj

    /**
     * Returns the correct page for an item with the given height and bottom y-value.
     * The user may use any Y value and we continue extending their canvas downward (negative) by adding extra pages.
     * @param bottomY the un-adjusted (bottom) y value of the item we're considering.
     * @param height the height of the item we're considering
     * @param requiredSpaceBelow if there isn't this much space left at the bottom of the page, move to the next page.
     * @return the proper page and adjusted y value for that page.
     */
    internal fun appropriatePage(bottomY: Float, height: Float, requiredSpaceBelow:Float): PageBufferAndY {
//        println("appropriatePage(bottomY=$bottomY, height=$height, requiredSpaceBelow=$requiredSpaceBelow)")

        // If the requiredSpaceBelow makes it too big to fit on any page, then ignore that param.
        // Used to throw exception, but this is a valid situation.
        val spaceBelow: Float = if ( (height + requiredSpaceBelow) > body.dim.height ) {
            0f
        } else {
            requiredSpaceBelow
        }

        if (!mgr.hasAnyPages()) {
            throw IllegalStateException("Cannot work with the any pages until one has been" +
                                        " created by calling mgr.ensurePageIdx(1).")
        }
        var y = bottomY
        var pageDiff = 0

        // Several pages in this page-grouping could be queued up before getting to this point.
        // The following advances to the first possible page our item could start on.
        // This used to be done in a loop, but repeated addition exaggerates floating point errors
        // So now it's done with math instead.
        if ( (y - spaceBelow) < yBodyBottom ) {
//            println("  y=$y yBodyBottom=$yBodyBottom")

            // How many pages behind are we?
            pageDiff = ceil((yBodyBottom - (y - spaceBelow)) / body.dim.height).toInt()

            // But repeated addition ruins floating point accuracy, so instead, we'll multiply each time.
            y = bottomY + (body.dim.height * pageDiff)
        }

        val newIdx = mgr.unCommittedPageIdx() + pageDiff

        var adj = 0f
        if (y + height > yBodyTop()) {
//            println("  y=$y yBodyTop()=${yBodyTop()}")
            val oldY = y
            y = yBodyTop() - height
            adj = oldY - y
        }
//        println("  y=$y, adj=$adj")

        mgr.ensurePageIdx(newIdx, body)
        return PageBufferAndY(mgr.page(newIdx), y, adj)
    }

    @Throws(IOException::class)
    fun commitBorderItems(stream: PDPageContentStream) {
        if (!valid) {
            throw IllegalStateException("Logical page accessed after commit")
        }
        // Since items are z-ordered, then sub-ordered by entry-order, we will draw
        // everything in the correct order.
        for (item in borderItems) {
            item.commit(stream)
        }
    }

//    /**
//     * Adds items to every page in page grouping.  You should not need to use this directly.  It only
//     * has package scope so that Text can access it for one thing.  It may become private in the
//     * future.
//     */
//    internal fun borderStyledText(bottomLeft: Coord, text: String, s: TextStyle) {
//        if (!valid) {
//            throw IllegalStateException("Logical page accessed after commit")
//        }
//        borderItems.add(SinglePage.Text(bottomLeft, text, s, borderOrd++.toLong(),
//                                        PdfItem.DEFAULT_Z_INDEX))
//    }

    /**
    @param pb specific page item will be put on
    @param y the y-value on that page
    @param adj the height of the adjustment used to keep the line on one page.
     */
    internal data class PageBufferAndY(val pb: SinglePage,
                                       val y: Float,
                                       val adj: Float)
}
