// Copyright 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.kobjects.htmlview;


import org.kobjects.css.Style;

/**
 * Keeps track of context information (the current left and right borders, 
 * y-position, horizontal and vertical alignment) while elements are laid out.
 * Note that for flow elements, the borders may become quite complex.
 * 
 * @author Stefan Haustein
 */
class LayoutContext {

  /**
   * The border structure resulting from adding flow elements, stored as 
   * sequence of character triples representing the left border (x0), the
   * right border (x1) and the height for which the current border configuration
   * is valid, starting at currentY. At the end of the sequence,
   * the left border is 0 and the right border is maxWidth
   */
  private StringBuilder borders = new StringBuilder();

  /** The maximum width available for layout. */
  private int maxWidth;

  /** x-coordinate of the last box placed via placeBox */
  private int boxX;

  /** y-coordinate of the last box placed via placeBox */
  private int boxY;

  /** Current y-position for layout */
  private int currentY;

  /** The height of the current line */
  private int lineHeight;

  /** The style of the element this layout context is created for. */
  private Style style;

  /**
   * position of the left border, relative to the border of the parent element 
   * if the border data structure was inherited.
   */
  int leftBorderX;

  /**
   * position of the right border, relative to the border of the parent element 
   * if the border data structure was inherited (= tunnelX0 + maxWidth).
   */
  int rightBorderX;

  /**
   * Create a new layout context.
   * 
   * @param maxWidth the maximum width available
   * @param style the style of the element, used to determine adjustments for
   *        the horizontal and vertical alignment
   * @param parent the parent layout context, may be null if n/a
   * @param x0 left offset relative to the parent
   * @param top top margin, i.e. vertical space that is implicitly skipped in 
   *        the parent layout context
   */
  public LayoutContext(int maxWidth, Style style, LayoutContext parent, int x0, 
      int top) {
    this.maxWidth = maxWidth;
    this.style = style;

    if (parent == null) {
      borders = new StringBuilder();
    } else {
      parent.advance(top);
      borders = parent.borders;
      leftBorderX = parent.leftBorderX + x0;
    }
    rightBorderX = leftBorderX + maxWidth;
  }

  /** 
   * Returns the horizontal space available for the given maximum width and 
   * required height. If the required height is smaller than the current line 
   * height, the line height is assumed as the required height.
   */ 
  int getHorizontalSpace(int requiredHeight) {

    if (requiredHeight < lineHeight) {
      requiredHeight = lineHeight;
    }

    if (borders.length() == 0) {
      return maxWidth;
    }

    int x0 = Math.max(leftBorderX, borders.charAt(0));
    int x1 = Math.min(rightBorderX, borders.charAt(1));
    int h = borders.charAt(2);
    int i = 3;
    while (h < requiredHeight && i < borders.length()) {
      x0 = Math.max(x0, borders.charAt(i));
      x1 = Math.min(x1, borders.charAt(i + 1));
      h += borders.charAt(i + 2);
      i += 3;
    }
    return x1 - x0;
  }

  /**
   * Moves the current y position down by the given value and resets the line
   * height to 0.
   * 
   * @param h the number of pixels to advance.
   */
  void advance(int h) {
    currentY += h;
    lineHeight = 0;

    int i = 0;
    while (i < borders.length() && h >= borders.charAt(i + 2)) {
      h -= borders.charAt(i + 2);
      i += 3;
    }
    if (i > 0) {
      borders.delete(0, i);
    }

    if (borders.length() > 0) {
      borders.setCharAt(2, (char) (borders.charAt(2) - h));
    }
    //    Assert.assertEquals(0, borders.length() % 3);
  }

  int getMaxWidth() {
    return maxWidth;
  }
  
  /** 
   * Places a box with the given width and height. If the box is not a floating
   * element, the lineHeight becomes the maximum of the line height and 
   * box height. The clear value determines whether the box is pushed down until
   * the left or right border are clear. The box position determined can be
   * red from the boxX and boxY member variables.
   * 
   * @param boxW widht of the box to be placed
   * @param boxH height of the box to be placed
   * @param floatTo determines if the box is a floating element; one of 
   *        Style.LEFT, Style.RIGHT or Style.NONE
   * @param clear determines whether the box is pushed down to an area where the
   *        left, right or both borders are clear (free of floating elements);
   *        one of Syle.LEFT, Style.RIGHT, Style.BOTH or Style.NONE.       
   */
  void placeBox(int boxW, int boxH, int floatTo, int clear) {
    // for non-floating boxes, set both the box height and line height to the
    // maximum of the box and line height 
    if (floatTo != Style.LEFT && floatTo != Style.RIGHT) {
      lineHeight = Math.max(lineHeight, boxH);
      boxH = lineHeight;
    }
    boolean right = floatTo == Style.RIGHT;

    // current index in border segments
    int i = 0;

    // y-position relative to currentY
    int y = 0;

    int x0 = 0;
    int x1 = maxWidth;

    boolean clearLeft = clear == Style.LEFT || clear == Style.BOTH;
    boolean clearRight = clear == Style.RIGHT || clear == Style.BOTH;

    // find box y position with sufficient width and height 
    while (i < borders.length()) {
      x0 = Math.max(leftBorderX, borders.charAt(i));
      x1 = Math.min(rightBorderX, borders.charAt(i + 1));
      int h = borders.charAt(i + 2);
      int j = i + 3;
      // accumulate sufficient height
      while (h < boxH && j < borders.length()) {
        x0 = Math.max(x0, borders.charAt(j));
        x1 = Math.min(x1, borders.charAt(j + 1));
        h += borders.charAt(j + 2);
        j += 3;
      }
      // check that width and clear constraints are met
      if (x1 - x0 >= boxW && (!clearLeft || x0 == 0) && 
          (!clearRight || x1 == maxWidth)) {
        break;
      }
      y += borders.charAt(i + 2);
      i += 3;
    }

    // box position found
    int remainingH = boxH;
    if (i >= borders.length()) {
      x0 = leftBorderX;
      x1 = rightBorderX;
    }

    boxX = (right ? x1 - boxW : x0) - leftBorderX;
    boxY = currentY + y;

    // adjust fully covered segments, reduce remainingH accordingly
    while (i < borders.length() && remainingH >= borders.charAt(i + 2)) {
      remainingH -= borders.charAt(i + 2);
      if (right) {
        borders.setCharAt(i + 1, (char) (x1 - boxW));
      } else {
        borders.setCharAt(i, (char) (x0 + boxW));
      }
      i += 3;
    }

    // adjust partially covered segments for remaining h, if any
    // otherwise create new segment at the end
    if (remainingH > 0) {
      if (i < borders.length()) {
        int ox0 = borders.charAt(i);
        int ox1 = borders.charAt(i + 1);
        int blockH = borders.charAt(i + 2);
        borders.setCharAt(i + 2, (char) (blockH - remainingH));

        if (right) {
          borders.insert(i, (char) ox0);
          borders.insert(i + 1, (char) (x1 - boxW));
        } else {
          borders.insert(i, (char) (x0 + boxW));
          borders.insert(i + 1, (char) ox1);
        }
        borders.insert(i + 2, (char) remainingH);

      } else {
        if (right) {
          borders.append((char) leftBorderX);
          borders.append((char) (x1 - boxW));
        } else {
          borders.append((char) (x0 + boxW));
          borders.append((char) rightBorderX);
        }
        borders.append((char) remainingH);
      }
    }
    //    Assert.assertEquals(0, borders.length() % 3);
  }

  /**
   * Moves the current y position below all floating elements on the left, 
   * right or both sides, depending on the clear value. If clear is Style.NONE,
   * the current y position remains unchanged.
   * 
   * @param clear one of Style.LEFT. Style.RIGHT, Style.BOTH or Style.NONE
   * @return true if the current y-position was changed.
   */
  boolean clear(int clear) {
    if (clear != Style.LEFT && clear != Style.RIGHT && clear != Style.BOTH) {
      return false;
    }
    placeBox(0, 0, Style.NONE, clear);
    if (boxY == currentY) {
      return false;
    }
    advance(boxY - currentY);
    return true;
  }  

  /**
   * Dumps internal data structures for debugging purposes.
   */
  public String toString() {
    StringBuilder sb = new StringBuilder(
        "LyaoutContext currentY: " + currentY + " maxWidth: " + maxWidth);
    for (int i = 0; i < borders.length(); i += 3) {
      sb.append(" " + i / 3 + "; x1: " + (int) borders.charAt(i) + 
          " x2: " + (int) borders.charAt(i + 1) + " h: " + 
          (int) borders.charAt(i + 2));
    }
    return sb.toString();
  }

  /**
   * Returns the x-coordinate of the last box placed via placeBox();
   */
  int getBoxX() {
    return boxX;
  }

  /**
   * Returns the y-coordinate of the last box placed via placeBox();
   */
  int getBoxY() {
    return boxY;
  }

  /**
   * Returns the current line height.
   */
  int getLineHeight() {
    return lineHeight;
  }

  /** 
   * Returns the current y-position. 
   */
  int getCurrentY() {
    return currentY;
  }

  /**
   * Returns the borders data structure for testing purposes.
   */
  StringBuilder getBordersForTest() {
    return borders;
  }

  /**
   * Sets the current line height.
   */
  void setLineHeight(int lineHeight) {
    this.lineHeight = lineHeight;
  }

  /**
   * Add the given value to the current y-position without changing the borders.
   * Used to correct the y-position after handing the borders over to the 
   * layout context of a child element.
   */
  void adjustCurrentY(int delta) {
    this.currentY += delta;
  }

  /**
   * Returns the horizontal adjustment to the x-position needed to respect
   * the horizontal alignment for the style of this layout context. This is
   * 0 for left align, space/2 for center align and space for right align.
   */
  int getAdjustmentX(int space) {
    switch (style.getEnum(Style.TEXT_ALIGN)) {
      case Style.CENTER:
        return space / 2;
      case Style.RIGHT:
        return space;
      default:
        return 0;
    }
  }

  /**
   * Returns the vertical adjustment to the y-position needed to respect
   * the vertical alignment for the style of this layout context. This is
   * 0 for top align, space/2 for center align and space for bottom align.
   */
  int getAdjustmentY(int space) {
    switch (style.getEnum(Style.VERTICAL_ALIGN)) {
      case Style.TOP:
        return 0;
      case Style.BOTTOM:
        return space;
      case Style.MIDDLE:
        return space / 2;
      default: // HACK
        return space * 7 / 8;
    }
  }
}
