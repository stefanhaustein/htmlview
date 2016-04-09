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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;


/**
 * Widget representing a (HTML) text fragment.
 * 
 * @author Stefan Haustein
 */
@SuppressLint("ViewConstructor")
class TextFragmentView extends View implements HasMeasuredPosition {
  
  /** Element this widget corresponds to */
  Element element;
  String text;
  short[] indices;
  private int firstLineYOffset;
  private int mainYOffset;
  private int lastLineYOffset;
  private int measuredX;
  private int measuredY;

  /**
   * Creates a new TextFragmentWidget
   */
  public TextFragmentView(Context context, Element element, String text, boolean focusable) {
    super(context);
    this.element = element;
    this.text = text;
    setFocusable(focusable);
    setClickable(focusable);
    if (focusable && isFocused()) {
      element.setFocused();
    }
  }

  /**
   * Returns the number of lines for the current layout state. If called before
   * the first call of doLayout(), 1 is returned. 
   */
  public int getLineCount() {
    return indices == null ? 1 : indices.length / 3;
  }

  /**
   * Returns the width of the nth line in pixels.
   */
  public int getLineWidth(int n) {
    return (int) (indices == null ? getFont().measureText(text) : 
      HtmlUtils.measureText(getFont(), text, indices[n * 3], indices[n * 3] + indices[n * 3 + 1]));
  }

  /**
   * Returns the GoogleFont associated with the computed style for the element
   * owning this text fragment.
   */
  public Paint getFont() {
    return element.getFont();
  }

  /**
   * Layouts this text fragment.
   * 
   * Precondition: x, y, w are set correctly.
   * 
   * @param index index of this widget in the parent widget
   * @param x0 start position for the first character, relative to getX()
   * @param breakPos first line break position. 
   * @param lineHeight height of the current line
   */
  public int doLayout(int myIndex, LayoutContext borders, int breakPos, 
      int lineStartIndex, int insertionIndex) {

    firstLineYOffset = 0;
    mainYOffset = 0;
    lastLineYOffset = 0;
    
    BlockElementView parent = (BlockElementView) getParent();

    Paint font = getFont();
    int maxWidth = borders.getMaxWidth();
    int fontHeight = font.getFontMetricsInt(null);

    int availableWidth = borders.getHorizontalSpace(fontHeight);

    int vAlign = element.getComputedStyle().getEnum(Style.VERTICAL_ALIGN);
    if (vAlign == Style.SUB || vAlign == Style.SUPER) {
      font = new Paint(font);
      font.setTextSize(font.getTextSize() * 3 / 4);
    }

    // breakpos invalid?
    if (breakPos == -1) {
      breakPos = Math.max(0, 
          findBreakPosition(parent, myIndex, 0, availableWidth, 
              availableWidth == maxWidth));
    }
    
    int len = text.length();
    
    if (breakPos > len) {
      int w = Math.min((int) font.measureText(text), maxWidth);
      borders.placeBox(w, fontHeight, Style.NONE, 0);
      
      indices = null;
      setMeasuredX(this.getMeasuredX() + borders.getBoxX());
      setMeasuredDimension(w, fontHeight);
      return breakPos - len;
    }

    StringBuilder buf = new StringBuilder();
    int lastBreak = 0;

    int h = Math.max(borders.getLineHeight(), fontHeight);
    borders.setLineHeight(h);
    firstLineYOffset = borders.getAdjustmentY(h - fontHeight);
    mainYOffset = h - fontHeight - firstLineYOffset;
    
    do {
      int end = breakPos;
      if (end > lastBreak && text.charAt(end - 1) <= ' ') {
        end--;
      }

      buf.append((char) lastBreak);
      buf.append((char) (end - lastBreak));
      
      int w = Math.min(HtmlUtils.measureText(font, text, lastBreak, end),  maxWidth);
      
      if (lineStartIndex != insertionIndex) {
        ((BlockElementView) getParent()).adjustLine(lineStartIndex, insertionIndex, borders);
        lineStartIndex = insertionIndex;
      }
      
      borders.placeBox(w, fontHeight, borders.getLineHeight(), 0);
      buf.append((char) (borders.getBoxX() + 
          borders.getAdjustmentX(availableWidth - w)));
      borders.advance(borders.getLineHeight());
      
      lastBreak = breakPos;
      
      availableWidth = borders.getHorizontalSpace(fontHeight);
      breakPos = Math.max(lastBreak, findBreakPosition(parent, myIndex, lastBreak, availableWidth, 
          availableWidth == maxWidth));
      
      h += fontHeight;
    } while (breakPos <= len);

    buf.append((char) lastBreak);
    buf.append((char) (text.length() - lastBreak));
    int w = Math.min(HtmlUtils.measureText(font, text, lastBreak, 
        text.length()), borders.getHorizontalSpace(fontHeight));
    
    borders.placeBox(w, fontHeight, Style.NONE, 0);
    buf.append((char) borders.getBoxX());
    
    indices = new short[buf.length()];
    for (int i = 0; i < indices.length; i++) {
      indices[i] = (short) buf.charAt(i);
    }

    setMeasuredDimension(maxWidth, h);
    return breakPos - len;
  }

  /**
   * Draws the text.
   */
  public void onDraw(Canvas g) {//, int dx, int dy) {
    // System.out.println("drawing tfw: "+text);
    int dx = 0;
    int dy = 0;

    Paint font = getFont();
    
    if (isFocused() && !element.isFocused()) {
      element.setFocused();
    }
    boolean focus = element.isFocused();
    int ascent = -font.getFontMetricsInt().ascent;

    int vAlign = element.getComputedStyle().getEnum(Style.VERTICAL_ALIGN);
    if (vAlign == Style.SUB || vAlign == Style.SUPER) {
      font = new Paint(font);
      float size = font.getTextSize();
      font.setTextSize(size * 3 / 4);
      dy += vAlign == Style.SUB ? size/4 : -size/4;
    }


    if (indices == null) {
      // TODO: Draw focus!
   /*   if (focus) {
    	Skin.get().fillFocusRect(g, dx, dy, 
            font.stringWidth(text), font.getHeight());
      } */

      // TODO Remove allocation
      g.drawText(text, dx, dy + ascent, font);
    } else {
      int y = dy + firstLineYOffset;
      // TODO: Remove allocation 
      Rect r = new Rect();
      g.getClipBounds(r);
      int clipY = r.top;
      int clipH = r.height();
      int fh = font.getFontMetricsInt(null);

      for (int i = 0; i < indices.length; i += 3) {
        if (clipY < y + fh &&  y < clipY + clipH) {
          int start = indices[i];
          int len = indices[i + 1];

          /*
          if (focus) {
            Skin.get().fillFocusRect(g, dx + indices[i + 2], y, 
                font.substringWidth(text, start, len), font.getHeight());
          }*/
          g.drawText(text, start, start + len, dx + indices[i + 2], y + ascent, font);
        }
        y += fh;
        if (i == 0) {
          y += mainYOffset;
        } 
        if (i == indices.length - 6) {
          y += lastLineYOffset;
        } 
      }
    }

    /*
    if (htmlView.debug == this) {
      g.setColor(0x00ff00);
      g.drawRect(dx, dy, getWidth() - 1, getHeight() - 1);
      if (getLineCount() > 1) {
       
        int fh = font.getHeight();
        int y = dy;
        g.setColor(0x0ff0000);
        g.drawLine(dx, y, dx, y + firstLineYOffset);
        y += firstLineYOffset;
        
        for (int i = 0; i < indices.length; i += 3) {
          g.setColor(0x0000ff);
          g.drawRect(dx + indices[i + 2], y, 
              font.substringWidth(text, indices[i], indices[i + 1]), fh - 1);
          y += fh;
          if (i == 0) {
            g.setColor(0x0ff0000);
            g.drawLine(dx + 2, y, dx + 2, y + mainYOffset);
            y += mainYOffset;
          } 
          if (i == indices.length - 6) {
            g.setColor(0x0ff0000);
            g.drawLine(dx + 4, y, dx + 4, y + lastLineYOffset);
            y += lastLineYOffset;
          }
        }
      }
    }*/
  }

  /**
   * Finds a suitable line break position in a run of TextFragmentWidgets,
   * starting at character index startCharIndex (relative to the start of
   * the first child). Assumes no kerning for performance reasons,
   * which may result in breaking the line slightly too early.
   * 
   * @param myIndex the child index of this text fragment in the parent widget
   * @param startCharIndex start character index
   * @param maxWidth maximum line width
   * @param force if true, the next break position is returned, even if it does
   *        not fit within maxWidth
   * @return character index for break (first character on new line); May be 
   *         outside of this TextFragmentWidget;
   *         Integer.MAX_VALUE if no break is necessary;
   *         Integer.MIN_VALUE if force is false and no suitable break position
   *         could be found
   */
  final int findBreakPosition(BlockElementView parent,
      int myIndex, int startCharIndex, int maxWidth, boolean force) {    
    int len = text.length();
    
    if (startCharIndex >= len) {
      TextFragmentView next = getNextSibling(parent, myIndex);
      if (next == null) {
        return Integer.MAX_VALUE;
      }
      int result = next.findBreakPosition(parent,
          myIndex + 1, startCharIndex - len, maxWidth, force);
      
      return result > Integer.MIN_VALUE && result < Integer.MAX_VALUE 
          ? result + len : result;
    }
    char startChar = text.charAt(startCharIndex);
    return findBreakPosition(parent, myIndex, startChar, startCharIndex + 1, 
        HtmlUtils.measureText(getFont(), text, startCharIndex, startCharIndex + 1),  
        force ? Integer.MAX_VALUE : Integer.MIN_VALUE, maxWidth);
  }
  
  /** 
   * Internal helper method for the package visible method with the same name.
   * Delegates break position search to next sibling if necessary.
   */
  private final int findBreakPosition(BlockElementView parent, int myIndex, 
      char currentChar, int nextCharIndex, int w, int bestPos, int maxWidth) {
    Paint font = getFont();
    int len = text.length();
    
    if (currentChar == '\n') {
      return nextCharIndex;
    }
    int startPos = nextCharIndex;
    while (true) {
      if (nextCharIndex >= len) {
        break;
      }
      char nextChar = text.charAt(nextCharIndex);
      if (currentChar == '\n' || HtmlUtils.canBreak(currentChar, nextChar)) {
        w += HtmlUtils.measureText(font, text, startPos, nextCharIndex);
        if (w > maxWidth) {
          return bestPos;
        }
        if (currentChar == '\n') {
          return nextCharIndex;
        }
        startPos = nextCharIndex;
        bestPos = nextCharIndex;
      }
      nextCharIndex++;
      currentChar = nextChar;
    }
    w += HtmlUtils.measureText(font, text, startPos, nextCharIndex);
    if (w > maxWidth && bestPos != Integer.MAX_VALUE) {
      return bestPos;
    }
    /*
    while (true) {
      if (currentChar == '\n') {
        return nextCharIndex;
      }
      if (w > maxWidth && bestPos != Integer.MAX_VALUE) {
        return bestPos;
      }
      if (nextCharIndex >= len) {
        break;
      }
      char nextChar = text.charAt(nextCharIndex);
      if (HtmlUtils.canBreak(currentChar, nextChar)) {
        bestPos = nextCharIndex;
      }
      nextCharIndex++;
      currentChar = nextChar;
      // we do not consider kerning for performance reasons. This may result in 
      // breaking the line slightly too early, which should be fine.
      // TODO(haustein) consider summing up word lengths instead
      w += font.measureText(text, nextCharIndex-1, nextCharIndex);
    }*/

    TextFragmentView next = getNextSibling(parent, myIndex);
    if (next == null) {
      return w <= maxWidth ? Integer.MAX_VALUE : bestPos;
    }
    int result = next.findBreakPosition(parent,
        myIndex + 1, currentChar, nextCharIndex - len, w, 
        bestPos == Integer.MIN_VALUE || bestPos == Integer.MAX_VALUE 
        ? bestPos : bestPos - len, maxWidth);
    
    return Integer.MIN_VALUE < result && result < Integer.MAX_VALUE 
        ? result + len : result;
  }
  
  /** 
   * Returns the next TextFragmentWidget sibling or null, if the next widget
   * is not an instance of TextFragmentWidget or this widget is the last child.
   *  
   * @param myIndex the index of this TextFragmentWidget in the parent widget
   * @return the next TextFragmentWidget if available; otherwise null
   */
  TextFragmentView getNextSibling(BlockElementView parent, int myIndex) {
   
    HtmlUtils.assertTrue(parent.children.get(myIndex) == this);
    if (myIndex + 1 >= parent.children.size()) {
      return null;
    }
    Object next = parent.children.get(myIndex + 1);
    return next instanceof TextFragmentView  
        ? (TextFragmentView) next : null;
  }
  
  
  protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
    if (isFocusable()) {
      if (gainFocus) {
        element.setFocused();
      }
      invalidate();
    }
  }
  
  /**
   * Returns the element owning the text fragment.
   */
  public Element getElement() {
    return element;
  }

  /**
   * Adjusts the vertical and horizontal alignment for the last line.
   * 
   * @param indent horizontal indent for the last line
   * @param lineH line height of the last line
   * @param context current LayoutContext
   */
  public void adjustLastLine(int indent, int lineH, LayoutContext context) {
    lastLineYOffset = context.getAdjustmentY(lineH - getFont().getFontMetricsInt(null));
 //   Dim.setHeight(this, this.getMeasuredHeight() + lastLineYOffset);
    setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() + lastLineYOffset);
    indices[indices.length - 1] += indent;
  }

  @Override
  public int getMeasuredX() {
    return measuredX;
  }


  @Override
  public int getMeasuredY() {
    return measuredY;
  }


  @Override
  public void setMeasuredPosition(int x, int y) {
    measuredX = x;
    measuredY = y;
  }


  @Override
  public void setMeasuredX(int x) {
    measuredX = x;
  }


  @Override
  public void setMeasuredY(int y) {
    measuredY = y;
  }
  
  @Override 
  public boolean performClick() {
    super.performClick();
    return element.click();
  }
  
  /**
   * Dummy implementation for cases where we know we are unchanged.
 
  protected void onMeasure(int widthSpec, int heightSpec) {
    setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
  }  */
}
