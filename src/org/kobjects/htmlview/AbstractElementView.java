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

import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.kobjects.css.Style;

/**
 * Widget for rendering HTML block elements. For text content and for inline
 * elements without block content, TextFragmentWidgets are created.
 * 
 * @author Stefan Haustein
 */
abstract class AbstractElementView extends ViewGroup implements HasMeasuredPosition {

  /** The HTML element this widget corresponds to. */
  protected Element element;

  /**
   * If false, a call to calculateWidth() is required before the minWidth and
   * maxWidth values can be used. Used internally in getMinWidth() and 
   * getMaxWidth() only.
   */
  protected boolean widthValid;

  /**
   * Width base value for percentages, set in doLayout
   */
  protected int containingWidth;

  /**
   * Left margin including adjustment for AUTO values
   */
  protected int marginLeft;

  /**
   * Right margin including adjustment for AUTO values
   */
  protected int marginRight;

  /** 
   * X-Coordinate of the CSS box inside this widget. 
   * The widget may be larger than the CSS box to make sure
   * components outside the CSS box are drawn.
   */  
  protected int boxX;

  /** Y-Coordinate of the CSS box inside this widget {@see boxX}. */
  protected int boxY;

  /** Width of the CSS box inside this widget {@see boxX}. */
  protected int boxWidth;

  /** Height of the CSS box inside this widget {@see boxX}. */
  protected int boxHeight;

  /** 
   * Minimal width of this component, as set by calculateWidth(). Use
   * getMinimumWidth to obtain this value.
   */
  protected int minimumWidth;

  /** 
   * Maximal width of this component, as set by calculateWidth(). Use
   * getMaximumWidth to obtain this value.
   */
  protected int maximumWidth;
  
  private int measuredX;
  private int measuredY;

  private Paint borderPaint = new Paint();
  
  /**
   * Constructs a new BlockWidget for the given element. Does not recurse.
   * Used only in constructors for subclasses.
   */
  protected AbstractElementView(Context context, Element element, boolean traversable) {
    super(context);
    setWillNotDraw(false);
    this.element = element;
    setFocusable(traversable);
    setClickable(traversable);
  }



  /**
   * Request a layout for this and all parent views. In addition, set widthValid to false.
   * Regular android layout requests are switched dead 
   */
  public void requestLayout() {
    widthValid = false;
    super.requestLayout();
  }

  /**
   * Determines whether this element has a fixed height, or the height is
   * determined by the content.
   */
  boolean isHeightFixed() {
    if (element.getComputedStyle().isLengthFixed(Style.HEIGHT)) {
      return true;
    } 
    if (!element.getComputedStyle().isLengthFixedOrPercent(Style.HEIGHT) || 
        !(getParent() instanceof AbstractElementView)) {
      return false;
    }  

    return ((AbstractElementView) getParent()).isHeightFixed();
  }

  int getFixedInnerHeight() {
    Style style = element.getComputedStyle();
    if (style.isLengthFixed(Style.HEIGHT)) {
      return element.getScaledPx(Style.HEIGHT);
    }
    return element.getScaledPx(Style.HEIGHT, ((AbstractElementView) getParent()).getFixedInnerHeight());
  }
  
  /**
   * Lays the content out according to the CSS style associated with the 
   * element. 
   * 
   * @param outerMaxWidth the maximum available width for this element including
   *        margins, borders and padding
   * @param layoutContext the layout context of the parent element (for nested
   *        block element in regular flow, null otherwise)
   * @param shrinkWrap true if the element is a floating element or positioned
   *        element where the width is not limited by the with of the display 
   *        but must be determined from the content width.
   */
  abstract void measureBlock(int outerMaxWidth, final int viewportWidth, 
      LayoutContext parentLayoutContext, boolean shrinkWrap);

  /**
   * Dummy implementation for cases where we know we are unchanged.
   */
  protected void onMeasure(int widthSpec, int heightSpec) {
    setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      child.measure(MeasureSpec.EXACTLY | child.getMeasuredWidth(), MeasureSpec.EXACTLY | child.getMeasuredHeight());
    }
  }


  /*
   * Dump the internal state of this object for debugging purposes.
  public void handleDebug() {
    if (DEBUG) {
	  System.out.println("Element path:");
      element.dumpPath();
      System.out.println("Computed Style: ");
      element.dumpStyle();

      System.out.println();
      System.out.println("Width: " + getWidth() + 
        " min: " + getMinimumWidth(containingWidth) + 
        " max: " + getMaximumWidth(containingWidth) + 
        " spec: " + getSpecifiedWidth(containingWidth) +
        " x: " + this.getMeasuredX() + " y: " + this.getMeasuredY() + 
        " marginLeft: " + marginLeft + " marginRight: " + marginRight);
    }
  }
   */


  /**
   * Adjust the vertical child positions according to the vertical alignment
   * if there is excessive space in this block that can be distributed. Called from
   * TableElementLayout for the cells.
   * 
   * @param verticalGap the space to distribute
   */
  void adjustVerticalPositions(int verticalGap) {
    int factorY = 0;
    switch (element.getComputedStyle().getEnum(Style.VERTICAL_ALIGN)) {
      case Style.TOP:
        factorY = 0;
        break;
      case Style.BOTTOM:
        factorY = 2;
        break;
      default:
        factorY = 1;
    }

    int addY = factorY * verticalGap / 2;
    if (addY > 0) {
      for (int childIndex = 0; childIndex < getChildCount(); childIndex++) {
        HasMeasuredPosition child = (HasMeasuredPosition) getChildAt(childIndex);
        child.setMeasuredY(child.getMeasuredY() + addY);
      }
    }
  }

  /**
   * Returns the minimum width of this block including borders.
   * 
   * @param containerWidth the width of the container
   */
  public int getMinimumWidth(final int containerWidth) {
    if (!widthValid) {
      calculateWidth(containerWidth);
    }
    return minimumWidth;
  }

  /**
   * Returns the maximum width of this block including borders.
   * 
   * @param containerWidth the width of the container
   */
  public int getMaximumWidth(final int containerWidth) {
    if (!widthValid) {
      calculateWidth(containerWidth);
    }
    return maximumWidth;    
  }

  /**
   * Returns the specified width of this block including borders.
   * 
   * @param containerWidth the width of the container
   */
  int getSpecifiedWidth(int containerWidth) {
    return element.getScaledPx(Style.WIDTH, containerWidth) +
        element.getScaledPx(Style.BORDER_LEFT_WIDTH) + 
        element.getScaledPx(Style.BORDER_RIGHT_WIDTH) + 
        element.getScaledPx(Style.MARGIN_LEFT) + 
        element.getScaledPx(Style.MARGIN_RIGHT) + 
        element.getScaledPx(Style.PADDING_LEFT) + 
        element.getScaledPx(Style.PADDING_RIGHT);
  }

  /**
   * Calculates the minimum and maximum widths of a block and stores them
   * in minimumWidth and maximumWidth. Do not call this method or use
   * the values directly, use getMinimumWidth() or getMaximumWidht() instead.
   * 
   * @param containerWidth Width of the container.
   */
  abstract void calculateWidth(int containerWidth);

  /**
   * Draws the border and fills the area with the background color if set. 
   */
  @Override
  public void onDraw(Canvas g) { // had dx, dy params
    if (element == null) {
      return;
    }
    
    int dx = boxX;
    int dy = boxY;

    Style style = element.getComputedStyle();

    int marginTop = element.getScaledPx(Style.MARGIN_TOP, containingWidth);
    int marginBottom = element.getScaledPx(Style.MARGIN_BOTTOM, containingWidth);

    int borderTop = element.getScaledPx(Style.BORDER_TOP_WIDTH, containingWidth);
    int borderRight = element.getScaledPx(Style.BORDER_RIGHT_WIDTH, containingWidth);
    int borderBottom = element.getScaledPx(Style.BORDER_BOTTOM_WIDTH, containingWidth);
    int borderLeft = element.getScaledPx(Style.BORDER_LEFT_WIDTH, containingWidth);

    // set to first pixel on border == outside padding area
    int x0 = dx + marginLeft + borderLeft - 1;
    int x1 = dx + boxWidth - marginRight - borderRight;
    int y0 = dy + marginTop + borderTop - 1;
    int y1 = dy + boxHeight - marginBottom - borderBottom;

    Paint bg = element.getBackgroundPaint();
    if (bg != null) {
      g.drawRect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, bg);
    }
   /* 
    Rect clipBounds = new Rect();
    g.getClipBounds(clipBounds);
    g.save();
    g.clipRect(x0 + 1, y0 + 1, x1 - x0 - 1, y1 - y0 - 1);
      if (style.backgroundImage != null && style.backgroundImage[0] != null) {
      Bitmap img = style.backgroundImage[0];
      int cx = clipBounds.left;
      int cy = clipBounds.top;
      int cw = clipBounds.width();
      int ch = clipBounds.height();
      int repeat = style.getEnum(Style.BACKGROUND_REPEAT);

      int bgX = repeat == Style.REPEAT_X || repeat == Style.REPEAT ? 0 : 
        style.getBackgroundReferencePoint(Style.BACKGROUND_POSITION_X, 
            x1 - x0 - 1, img.getWidth());
      int bgY = repeat == Style.REPEAT_Y || repeat == Style.REPEAT ? 0 : 
        style.getBackgroundReferencePoint(Style.BACKGROUND_POSITION_Y, 
            y1 - y0 - 1, img.getHeight());

      g.clipRect(x0 + 1, y0 + 1, x1 - x0 - 1, y1 - y0 - 1);
      if (repeat == Style.REPEAT_Y || repeat == Style.REPEAT) {
        do {
          if (repeat == Style.REPEAT) {
            do {
              g.drawBitmap(img, x0 + 1 + bgX, y0 + 1 + bgY, null);
              bgX += img.getWidth();
            } while (bgX < x1 - x0);
            bgX = 0;
          } else {
            g.drawBitmap(img, x0 + 1 + bgX, y0 + 1 + bgY, null);
          }
          bgY += img.getHeight();
        } while (bgY < y1 - y0);
      } else if (repeat == Style.REPEAT_X) {
        do {
          g.drawBitmap(img, x0 + 1 + bgX, y0 + 1 + bgY, null);
          bgX += img.getWidth();
        } while (bgX < x1 - x0);
      } else {
        g.drawBitmap(img, x0 + 1 + bgX, y0 + 1 + bgY, null);
      }
       g.restore();
    } */
   

    // TODO: Avoid alloc
    if (borderTop > 0) {
      borderPaint.setColor(style.getColor(Style.BORDER_TOP_COLOR));
      int dLeft = (borderLeft << 8) / borderTop;
      int dRight = (borderRight << 8) / borderTop;
      for (int i = 0; i < borderTop; i++) {
        g.drawLine(x0 - ((i * dLeft) >> 8), y0 - i, x1 + ((i * dRight) >> 8), y0 - i, borderPaint);
      }
    }
    if (borderRight > 0) {
      borderPaint.setColor(style.getColor(Style.BORDER_RIGHT_COLOR));
      int dTop = (borderTop << 8) / borderRight;
      int dBottom = (borderBottom << 8) / borderRight;
      for (int i = 0; i < borderRight; i++) {
        g.drawLine(x1 + i, y0 - ((i * dTop) >> 8), x1 + i, y1 + ((i * dBottom) >> 8), borderPaint);
      }
    }
    if (borderBottom > 0) {
      borderPaint.setColor(style.getColor(Style.BORDER_BOTTOM_COLOR));
      int dLeft = (borderLeft << 8) / borderBottom;
      int dRight = (borderRight << 8) / borderBottom;
      for (int i = 0; i < borderBottom; i++) {
        g.drawLine(x0 - ((i * dLeft) >> 8), y1 + i, x1 + ((i * dRight) >> 8), y1 + i, borderPaint);
      }
    }
    if (borderLeft > 0) {
      borderPaint.setColor(style.getColor(Style.BORDER_LEFT_COLOR));
      int dTop = (borderTop << 8) / borderLeft;
      int dBottom = (borderBottom << 8) / borderLeft;
      for (int i = 0; i < borderLeft; i++) {
        g.drawLine(x0 - i, y0 - ((i * dTop) >> 8), x0 - i, y1 + ((i * dBottom) >> 8), borderPaint);
      }
    }

    if (style.getEnum(Style.DISPLAY) == Style.LIST_ITEM) {
      Paint f = element.getFont();
      // round up so in doubt the dot size is a bit bigger and the pos is lower 
      int liy = y0 + 1 + element.getScaledPx(Style.PADDING_TOP, containingWidth);

      String label = "* ";
      switch (style.getEnum(Style.LIST_STYLE_TYPE)) {
        case Style.SQUARE:
          label = "\u25A0 ";
          break;
        case Style.CIRCLE:
          label = "\u25E6 "; // "\u25CB"; // 
          break;
        case Style.DECIMAL:
          if (getParent() instanceof AbstractElementView) {
            label = (((AbstractElementView) getParent()).element.indexOf(element) + 1) + ". ";
          } else {
            label = "# ";
          }
          break;
        case Style.DISC:
          label = "\u2022 ";
          break;
      }
      g.drawText(label, dx - f.measureText(label), liy - f.ascent(), f);
    }
  }

  /*
  public void drawTree(Canvas g, int dx, int dy, int clipX, int clipY, int clipW, int clipH) {
    int overflow = element.getComputedStyle().getEnum(Style.OVERFLOW);

    drawContent(g, dx, dy);
    if (overflow != Style.HIDDEN) {
      super.drawTree(g, dx, dy, clipX, clipY, clipW, clipH);
      drawFocusRect(g, dx, dy);
    } else {
      g.clipRect(dx, dy, getWidth(), getHeight());
      super.drawTree(g, dx, dy, g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
      drawFocusRect(g, dx, dy);
      g.setClip(clipX, clipY, clipW, clipH);
    }
  }
*/

  /**
   * Draws the focus if this widget is directly focused. When
   * HtmlDocumentWidget.DEBUG is set, a red outline of this widget is drawn.
   */
  /*
  protected void drawFocusRect(Graphics g, int dx, int dy) {
    // TODO(haustein) Replace with pseudo-class :focus handling

    if (isFocused() && focusable) {
      Skin.get().drawFocusRect(g, dx + boxX, dy + boxY, boxWidth, boxHeight, false);
    }

    if (DEBUG && htmlView.debug == this) {
      int x0 = dx + boxX;
      int y0 = dy + boxY;
      int x1 = dx + boxWidth - 1;
      int y1 = dy + boxHeight - 1;

      if (boxX != 0 || boxY != 0 || boxWidth != getWidth() || boxHeight != getHeight()) {
        g.setColor(0x0ff0000);
        g.drawRect(dx, dy, getWidth(), getHeight());
      }
      
      Style style = element.getComputedStyle();
      for (int i = 0; i < 3; i++) {
        int id;
        int color;
        // Colors: Yellow: margin; Purple: padding; light blue: block-level element.
        switch (i) {
        case 0:
          color = 0x88ffff00;
          id = Style.MARGIN_TOP;
          break;
        case 1:
          color = 0x88ff0000;
          id = Style.BORDER_TOP_WIDTH;
          break;
        default: 
          color = 0x088ff00ff;
          id = Style.PADDING_TOP;
        }

        int top = style.getPx(id, containingWidth);
        int right = i == 0 ? marginRight : style.getPx(id + 1, containingWidth);
        int bottom = style.getPx(id + 2, containingWidth);
        int left = i == 0 ? marginLeft : style.getPx(id + 3, containingWidth);
        
        GraphicsUtils.fillRectAlpha(g, x0, y0, x1 - x0 + 1, top, color);
        GraphicsUtils.fillRectAlpha(g, x0, y1 - bottom, x1 - x0 + 1, bottom, color);

        GraphicsUtils.fillRectAlpha(g, x0, y0 + top, left, y1 - y0 + 1 - top - bottom, color);
        GraphicsUtils.fillRectAlpha(g, x1 - x0 + 1 - right, y0 + top, right, y1 - y0 + 1 - top - bottom, color);

        y0 += top;
        x0 += left;
        y1 -= bottom;
        x1 -= right;
      }
      GraphicsUtils.fillRectAlpha(g, x0, y0, x1 - x0 + 1, y1 - y0 + 1, 0x440000ff);
    }
  }*/

  /**
   * Notify the element that the focus was received and request a redraw.
   */
  protected void onFocusChanged (boolean gainFocus, int direction, Rect previouslyFocusedRect) {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    if (isFocusable()) {
      if (gainFocus) {
        element.setFocused();
        invalidate();
      }
    }
  }
  

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      int x;
      int y;
      if (child instanceof HasMeasuredPosition) {
        HasMeasuredPosition mp = (HasMeasuredPosition) child;
        x = mp.getMeasuredX();
        y = mp.getMeasuredY();
      } else {
        // WrapperView case.
        x = marginLeft + 
            element.getScaledPx(Style.BORDER_LEFT_WIDTH) + 
            element.getScaledPx(Style.PADDING_LEFT);

        y = element.getScaledPx(Style.MARGIN_TOP) + 
            element.getScaledPx(Style.BORDER_LEFT_WIDTH) + 
            element.getScaledPx(Style.PADDING_LEFT);
      }
      child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }
  }

  void setMeasuredDimension(int x, int y, int w, int h) {
    measuredX = x;
    measuredY = y;
    setMeasuredDimension(w, h);
  }

  void setMeasuredHeight(int h) {
    setMeasuredDimension(getMeasuredWidth(), h);
  }

  @Override
  public int getMeasuredX() {
    return measuredX;
  }

  @Override 
  /** 
   * Disabled because it completely kills performance when the soft keyboard is shown
   * and does not seem to be needed.
   */
  public void forceLayout() {
  }
  
  @Override
  public int getMeasuredY() {
    return measuredY;
  }

  @Override
  public void setMeasuredPosition(int x, int y) {
    this.measuredX = x;
    this.measuredY = y;
  }

  @Override
  public void setMeasuredX(int x) {
    this.measuredX = x;
  }

  @Override
  public void setMeasuredY(int y) {
    this.measuredY = y;
  }
  
  void requestLayoutAll() {
    requestLayout();
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child instanceof AbstractElementView) {
        ((AbstractElementView) child).requestLayoutAll();
      } else {
        requestLayout();
      }
    }
  }
  
  @Override 
  public boolean performClick() {
    super.performClick();
    return element.click();
  }

  static class BuildContext {
    boolean unassignedTraversal;
    boolean preserveLeadingSpace;
  }
}
