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

import java.util.ArrayList;

import org.kobjects.css.Style;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;


/**
 * Widget representing HTML tables.
 * 
 * @author Stefan Haustein
 */
@SuppressLint("ViewConstructor")
class TableElementView extends AbstractElementView {

  /** 
   * number of table rows. 
   */
  private int rowCount;
  
  // TODO: Distinguish row and cell children?
  /** Contains the child elements in document order (!= display order). */
  protected ArrayList<View> children = new ArrayList<View>();

  /**
   * Builds the table by calling build to recurse into row and cell elements.
   * 
   * @param element the table element.
   */
  public TableElementView(Context context, Element element) {
    super(context, element, false);
    build(element);
  }
  
  /**
   * Adds cell widgets to a table widget.
   */
  private void build(Element element) {
    for (int i = 0; i < element.getChildCount(); i++) {
      if (element.getChildType(i) != Element.ELEMENT) {
        continue;
      }
      Element child = element.getElement(i);
      int display = child.getComputedStyle().getEnum(Style.DISPLAY);
      if (display == Style.TABLE_CELL) {
        BuildContext buildContext = new BuildContext();
        buildContext.preserveLeadingSpace = true;
        children.add(new BlockElementView(getContext(), child, buildContext));
      } else {
        if (display == Style.TABLE_ROW) {
          children.add(rowCount++, new BlockElementView(getContext(), child, false));
        }
        build(child);
      }
    }
  }

  protected void calculateWidth(int parentWidth) {
    widthValid = true;
    formatTable(parentWidth, parentWidth, false, true);
  }

  public void measureBlock(int containerWidth, int viewportWidth, LayoutContext context, 
      boolean shrinkWrap) {
    if (!isLayoutRequested() && containerWidth == containingWidth && context == null) {
      return;
    }
    this.containingWidth = containerWidth;
    formatTable(containerWidth, viewportWidth, shrinkWrap, false);
    if (context != null) {
      context.advance(boxHeight);
    }
  }
  
  /** 
   * Calculates the table width or performs the table layout.
   * 
   * @param containerWidth inner width of the containing widget
   * @param shrinkWrap if true, use all space necessary 
   *    (for absolute positioning)
   * @param measureOnly true if this method should only set minWidth and 
   *    maxWidth, not performing the actual table layout. Used in 
   *    calculateWidth().
   */
  private void formatTable(int containerWidth, int viewportWidth, boolean shrinkWrap, 
      boolean measureOnly) {
    // This method is rather long. Unfortunately, splitting it up would mean 
    // - handing over a lot of variables, 
    // - adding them as members or
    // - creating a separate class bundling the layout information,
    // none of which seems desirable. 
    // It just performs a sequence of steps, accessing and modifying table layout
    // information
    
    StringBuilder skipRows = new StringBuilder();
    Style style = element.getComputedStyle();
    
    int left = element.getScaledPx(Style.MARGIN_LEFT, containerWidth) + 
        element.getScaledPx(Style.PADDING_LEFT, containerWidth) +
        element.getScaledPx(Style.BORDER_LEFT_WIDTH, containerWidth);
    int right = element.getScaledPx(Style.MARGIN_RIGHT, containerWidth) +
        element.getScaledPx(Style.PADDING_RIGHT, containerWidth) +
        element.getScaledPx(Style.BORDER_RIGHT_WIDTH, containerWidth);
    int top = element.getScaledPx(Style.MARGIN_TOP, containerWidth) +
        element.getScaledPx(Style.PADDING_TOP, containerWidth) +
        element.getScaledPx(Style.BORDER_TOP_WIDTH, containerWidth);
    int bottom = element.getScaledPx(Style.MARGIN_BOTTOM, containerWidth) +
        element.getScaledPx(Style.PADDING_BOTTOM, containerWidth) +
        element.getScaledPx(Style.BORDER_BOTTOM_WIDTH, containerWidth);
    
    int maxInnerWidth;
    int minInnerWidth = 0;

    if (shrinkWrap) {
      maxInnerWidth = getMaximumWidth(containerWidth);
    } else if (style.isLengthFixedOrPercent(Style.WIDTH)){
      maxInnerWidth = element.getScaledPx(Style.WIDTH, containerWidth);
      minInnerWidth = maxInnerWidth;
    } else {
      maxInnerWidth = containerWidth - left - right;
    }
    
    Element currentRowElement = null;
    int column = 0;
    int row = 0;
    
    int cellCount = children.size() - rowCount;
    
    int[] cols = new int[cellCount];
    int[] rows = new int[cellCount];
    int[] colSpans = new int[cellCount];
    int[] rowSpans = new int[cellCount];
    
    // determine column, row, colSpan and rowSpan for each table cell.
    for (int i = 0; i < cellCount; i++) {
      AbstractElementView cell = (AbstractElementView) children.get(rowCount + i);
      // TODO(haustein) move add to end
      Element cellElement = cell.element;
      if (currentRowElement == null) {
        currentRowElement = cellElement.getParent();
      } else if (currentRowElement != cellElement.getParent()) {
        currentRowElement = cellElement.getParent();
        column = 0;
        row++;
        for (int j = 0; j < skipRows.length(); j++) {
          int span = skipRows.charAt(j);
          if (span > 0) {
            skipRows.setCharAt(j, (char) (span - 1));
          }
        }
      }

      while (column < skipRows.length() && skipRows.charAt(column) > 0) {
        column++;
      }
      
      int rowSpan = Math.max(cellElement.getAttributeInt("rowspan", 1), 1);
      int colSpan = Math.max(cellElement.getAttributeInt("colspan", 1), 1);
      
      cols[i] = column;
      rows[i] = row;
      colSpans[i] = colSpan;
      rowSpans[i] = rowSpan;
      
      while (skipRows.length() < column + colSpan){
        skipRows.append(1);
      }
      for (int j = 0; j < colSpan; j++) {
        skipRows.setCharAt(column++, (char) rowSpan);
      }
    }

    // CSS 2.1, 17.5.2.1 - Fixed table layout 
    // -- not yet supported, using auto layout 
    //    (should usually deliver similar results)
    
    // CSS 2.1, 17.5.2.2 - Automatic table layout

    int colCount = skipRows.length();
    
    int[] minWidths = new int[colCount];
    int[] specWidths = new int[colCount];
    int[] maxWidths = new int[colCount];
    boolean[] isFixed = new boolean[colCount];
    
    // 1., 2.: figure out min/max widht for single colspan cells
    for (int i = 0; i < cellCount; i++) {
      if (colSpans[i] == 1) {
        AbstractElementView cell = (AbstractElementView) children.get(rowCount + i);
        column = cols[i];
        minWidths[column] = Math.max(minWidths[column], 
            cell.getMinimumWidth(maxInnerWidth));
        specWidths[column] = Math.max(specWidths[column], 
            cell.getSpecifiedWidth(maxInnerWidth));
        maxWidths[column] = Math.max(maxWidths[column], 
            cell.getMaximumWidth(maxInnerWidth));
        Style cellStyle = cell.element.getComputedStyle();
        isFixed[column] |= cellStyle.isLengthFixed(Style.WIDTH);
       // ||(cellStyle.lengthIsFixed(Style.WIDTH, true) && specWidths[column] 
       //< minWidths[column]); 
      }
    }
    
    // 3. update min/max for cells with colspan > 1
    for (int i = 0; i < cellCount; i++) {
      AbstractElementView cell = (AbstractElementView) children.get(rowCount + i);
      if (colSpans[i] > 1) {
        column = cols[i];
        int span = colSpans[i];
        int min = 0;
        int max = 0;
        int div = 0;
        for (int j = 0; j < span; j++) {
          min += minWidths[column + j];
          max += maxWidths[column + j];
          if (!isFixed[column + j]) {
            div++;
          }
        }
        if (div == 0) {
          div = span;
        }
        min = Math.max(
            (cell.getMinimumWidth(maxInnerWidth) - min + div - 1) / div, 0);
        max = Math.max(
            (cell.getMaximumWidth(maxInnerWidth) - max + div - 1) / div, 0);
        
        for (int j = 0; j < span; j++) {
          if (div == span || !isFixed[column + j]) {
            minWidths[column + j] += min;
            maxWidths[column + j] += max;
          }
        }
      }
    }
    
    // calculate sums, simplify values (ensure min <= spec <= max)
    int minSum = 0;
    int maxSum = 0;
    int specSum = 0;
    int actualWidth = 0;
    int fixedCount = 0;
    
    for (int i = 0; i < colCount; i++) {
      minSum += minWidths[i];
      specWidths[i] = Math.max(minWidths[i], specWidths[i]);
      specSum += specWidths[i];
      maxWidths[i] = isFixed[i] ? specWidths[i] : maxWidths[i];
      maxSum += maxWidths[i];
      if (isFixed[i]) {
        fixedCount++;
      }
    }

    if (measureOnly) {
      // The minimum width of table must be larger than or equal to the size
      // specified in table tag (minInnerWidth).
      this.minimumWidth = Math.max(minSum, minInnerWidth);
      // maxSum is already larger than the size specified in table tag
      // (maxInnerWidth).
      this.maximumWidth = maxSum;
      return;
    }
    
    if (maxSum < maxInnerWidth) {
      System.arraycopy(maxWidths, 0, minWidths, 0, colCount);
      minSum = maxSum;
    } 
    actualWidth = minSum;
    
    // distribute space to cells having a percent width that was not met yet.
    if (maxInnerWidth > minSum && specSum > minSum) {
      int distribute = maxInnerWidth - minSum;
      for (int i = 0; i < colCount; i++) {
        int wants = specWidths[i] - minWidths[i];
        int add = wants * distribute / (specSum - minSum);
        minWidths[i] += add;
        actualWidth += add;
      }
    }
    
    // distribute still remaining space
    if (maxInnerWidth > specSum && maxSum > minSum) {
      int totalWanted = maxSum;
      int available = maxInnerWidth - specSum;
      
      for (int i = 0; i < colCount; i++) {
        if (!isFixed[i]) {
          int add = maxWidths[i] * available / totalWanted;
          minWidths[i] += add;
          actualWidth += add;
        }
      }
    }
    
    // if the table has a fixed width, force column widths wider if necessary.
    if (style.isLengthFixedOrPercent(Style.WIDTH) && maxSum > 0 && 
        maxInnerWidth > actualWidth) {
      if (fixedCount == colCount) {
        fixedCount = 0;
      }
      int add = (maxInnerWidth - actualWidth) / (colCount - fixedCount);
      for (int i = 0; i < colCount; i++) {
        if (fixedCount == 0 || !isFixed[i]) {
          minWidths[i] += add;
          actualWidth += add;
        }
      }
    }
    
    // ok, lets adjust the width and margins now...
    boxWidth = actualWidth + left + right;
    marginLeft = element.getScaledPx(Style.MARGIN_LEFT, containerWidth);
    marginRight = element.getScaledPx(Style.MARGIN_RIGHT, containerWidth);
    
    if (!shrinkWrap) {
      if (style.getEnum(Style.MARGIN_LEFT) == Style.AUTO) {
        if (style.getEnum(Style.MARGIN_RIGHT) == Style.AUTO) {
          marginRight = (containerWidth - left - right - actualWidth) / 2;
          marginLeft = marginRight;
          boxWidth += marginLeft + marginRight;
        }
      }
    }
    
    // iterate the table cells and format them accordingly
    column = 0;
    int currentRow = 0;
    int currentX = marginLeft;
    int currentY = 0;
    AbstractElementView[] cells = new AbstractElementView[colCount];
    int[] accumulatedHeights = new int[colCount];
    for (int i = 0; i < skipRows.length(); i++){
      skipRows.setCharAt(i, (char) 0);
    }

    removeAllViews();
    StringBuilder rowHeights = new StringBuilder();
    
    for (int i = 0; i < cellCount; i++) {
      AbstractElementView cell = (AbstractElementView) children.get(rowCount + i);
      addView(cell);

      if (currentRow != rows[i]) {
        int rh = formatTableRow(cells, skipRows, accumulatedHeights);
        rowHeights.append((char) rh);
        currentY += rh;
        currentX = marginLeft;
        column = 0;
        currentRow = rows[i];
      }
      
      while (cols[i] > column) {
        currentX += minWidths[column++];
      }
      
      cells[column] = cell; 
      accumulatedHeights[column] = 0;
      cell.setMeasuredPosition(left + currentX, top + currentY);
      
      int w = 0;
      for (int j = 0; j < colSpans[i]; j++) {
        w += minWidths[column + j];
        skipRows.setCharAt(column + j, (char) rowSpans[i]);
      }
      
      cell.measureBlock(w, viewportWidth, null, false);
      currentX += w;
      column += colSpans[i];
    }

    // TODO(haustein) test <table><tr><td rowspan="2">Test</td></tr></table>
    rowHeights.append((char) formatTableRow(cells, skipRows, 
        accumulatedHeights));
    
    // finally, care about the <tr> elements
    currentY = 0;
    for (int i = 0; i < rowHeights.length(); i++) {
      int h = rowHeights.charAt(i);
      if (i < rowCount) {
      AbstractElementView rowWidget = (AbstractElementView) children.get(i);
      rowWidget.boxX = 0;
      rowWidget.boxY = 0;
      rowWidget.boxWidth = actualWidth;
      rowWidget.boxHeight = h;

      rowWidget.setMeasuredDimension(0, currentY, actualWidth, h);
    //  Dim.setDimensions(rowWidget, 0, currentY, actualWidth, h);
      
      addView(rowWidget, i); 
      }
      currentY += h;
    }
    boxHeight = top + currentY + bottom;
    
    setMeasuredDimension(boxWidth, boxHeight);
  //  Dim.setDimension(this, boxWidth, boxHeight);
  }

  /**
   * Formats a single table row and returns the row height.
   * 
   * @param cells The cells the row consists of
   * @param rowSpans remaining row spans for the cells, will be decremented
   * @param accumulatedHeights the accumulated heights of previous rows if this
   *                           cell spans multiple rows
   * @return the row height
   */
  private int formatTableRow(AbstractElementView[] cells, StringBuilder rowSpans,
      int[] accumulatedHeights) {
    
    int rowHeight = 0;
    for (int i = 0; i < cells.length; i++) {
      int span = rowSpans.charAt(i);
      AbstractElementView cell = cells[i];
      
      if (span == 1 && cell != null) {
        rowHeight = Math.max(rowHeight, cell.boxHeight - accumulatedHeights[i]);
      } 
    }
    
    for (int i = 0; i < cells.length; i++) {
      int span = rowSpans.charAt(i);
      AbstractElementView cell = cells[i];
      
      if (span == 1 && cell != null) {
        int oldH = cell.boxHeight;
        cell.boxHeight = rowHeight + accumulatedHeights[i];
        cell.adjustVerticalPositions(cell.boxHeight - oldH);
        cell.setMeasuredHeight(cell.getMeasuredHeight() + cell.boxHeight - oldH);
        cells[i] = null;
      }
     
      if (span > 0) {
        rowSpans.setCharAt(i, (char) (span - 1));
        accumulatedHeights[i] += rowHeight;
      }
    }
    return rowHeight;
  }
}
