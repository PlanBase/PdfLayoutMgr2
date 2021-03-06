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

package com.planbase.pdf.lm2.lineWrapping

/**
 Represents a part of a line (of text).
 Terminal means this item ends with a line break.
 Continuing means it does not.
 None means that nothing is left or nothing will fit in the given remaining width on this line.
 */
sealed class ConTermNone
/**
 * The wrapped line fragment does not end with a hard line break.
 * @param hasMore whether there is more text to go on the next line or not.
 */
data class Continuing(override val item: LineWrapped, val hasMore: Boolean): ConTermNone(), ConTerm {
    override fun toString() = "Cont($item)"
}
/* The wrapped line fragment ends with a hard line break "\n" */
data class Terminal(override val item: LineWrapped): ConTermNone(), ConTerm {
    override fun toString() = "Term($item)"
}
/* None means that nothing more is available (or nothing will fit in the given space). */
object None: ConTermNone() {
    override fun toString() = "None"
}

/**
Represents only a continuing or terminal LineWrapped where Continuing means there could be more on this
line (no hard line break) and Terminal means a hard-coded line-break was encountered.  There is no "None" in
this interface.
 */
interface ConTerm {
    val item: LineWrapped
}