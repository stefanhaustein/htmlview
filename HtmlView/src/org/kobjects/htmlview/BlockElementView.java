package org.kobjects.htmlview;

import java.util.ArrayList;

import org.kobjects.css.Style;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.view.View;

@SuppressLint("ViewConstructor")
class BlockElementView extends AbstractElementView {

  /** Contains the child elements in document order (!= display order). */
  ArrayList<View> children = new ArrayList<View>();

  BlockElementView(Context context, Element element, boolean traversable) {
    super(context, element, traversable);
  }
  
  /**
   * Construct a new BlockWidget for the given element with the given flags.
   * 
   * @param element the HTML element this widget belongs to
   * @param flags {@see addChildren()}
   */
  BlockElementView(Context context, Element element, BuildContext buildContext) {
    super(context, element, buildContext.unassignedTraversal);
    buildContext.unassignedTraversal = false;
    addChildren(element, buildContext);
  }

  /**
   * Add widgets for the child elements of the given element to this block 
   * widget. The buildContext parameter is used as in and out parameter. The flag
   * with the index FLAG_PRESERVE_LEADING_SPACE controls whether leading
   * space needs to be preserved or will be trimmed. The flag with the index
   * FLAG_UNASSIGNED_TRAVERSAL is set if a link element was visited, but 
   * no visible element to carry the link was encountered so far.
   * <p>
   * This method normalizes white space as follows (if not instructed by the
   * style to preserve all whitespace):
   * <ol>
   * <li>sequences of CR, LF, TAB and SPACE will be normalized to a single space
   * <li>whitespace before or after a block element will be removed
   * <li>whitespace at the beginning or end of a block element will be removed
   * <li>whitespace at the beginning of a new inline element will be removed if 
   *     there already was whitespace before the the element.
   * <li>whitespace immediately after the end of an inline element will be 
   *     removed if there already was whitespace inside the element at the end.
   * </ol>
   * 
   * @param element element containing the child elements to be added
   * @param flags flags controlling traversability and leading white space, 
   *        see above. 
   */
  void addChildren(Element e, BuildContext buildContext) {
    Style style = e.getComputedStyle();
    StringBuilder buf = new StringBuilder();
    boolean isBlock = style.isBlock(true);
    boolean preserveLeadingSpace = buildContext.preserveLeadingSpace && !isBlock;
    boolean preserveAllSpace = style.getEnum(Style.WHITE_SPACE) == Style.PRE;  

    for (int i = 0; i < e.getChildCount(); i++) {
      switch (e.getChildType(i)) {
        case Element.ELEMENT:
          Element child = e.getElement(i);
          if ("br".equals(child.getName())) {
            HtmlUtils.rTrim(buf);
            buf.append("\n");
            preserveLeadingSpace = false;
          } else {           
            boolean childIsBlock = child.getComputedStyle().isBlock(true);
            // before block elements, always remove all whitespace
            if (!preserveAllSpace && childIsBlock) {
              HtmlUtils.rTrim(buf); // does not trim /n!
            }
            // insert text aggregated in case Element.TEXT if any
            if (buf.length() > 0) {
              TextFragmentView fragment = new TextFragmentView(getContext(), e,
                  buf.toString(), buildContext.unassignedTraversal);
              buildContext.unassignedTraversal = false;
              children.add(fragment);
              preserveLeadingSpace = buf.charAt(buf.length() - 1) > ' ';
              buf.setLength(0);
            }
            // tell the recursive call whether there is whitespace 
            // in front of the next element
            buildContext.preserveLeadingSpace = preserveLeadingSpace;
            addElement(e.getElement(i), buildContext);
            // preserver leading space only if the call did not signal 
            // whitespace at the end and the sub-elment was not a block
            preserveLeadingSpace = buildContext.preserveLeadingSpace && !childIsBlock;
          }
          break;
        case Element.TEXT:
          // Collect text nodes until we encounter an element or the end tag;
          // make sure duplicated spaces are removed.
          if (preserveAllSpace) {
            buf.append (e.getText(i));
          } else {
            HtmlUtils.appendTrimmed(buf, e.getText(i), preserveLeadingSpace);
            if (buf.length() > 0) {
              preserveLeadingSpace = buf.charAt(buf.length() - 1) > ' ';
            }
          }
          break;
      }
    }
    // for block elements, remove whitespace at the end
    if (isBlock) {
      HtmlUtils.rTrim(buf);
    }
    // append all remaining text collected in case Element.TEXT above
    if (buf.length() > 0) {
      TextFragmentView fragment = new TextFragmentView(getContext(), e,
          buf.toString(), buildContext.unassignedTraversal);
      children.add(fragment);
      buildContext.unassignedTraversal = false;
      preserveLeadingSpace = !isBlock && buf.charAt(buf.length() - 1) > ' ';
    }
    // signal trailing white space to the caller
    buildContext.preserveLeadingSpace = preserveLeadingSpace;
  } 
  
  /**
   * Add widgets for the given child element to this block widget. 
   * 
   * @param element the tree to be added recursively
   * @param flags {@see addChildren}
   */
  void addElement(Element child, BuildContext buildContext) {
    boolean parentUnassignedTraversal = buildContext.unassignedTraversal;
    boolean focusable = child.isLink();
    buildContext.unassignedTraversal |= focusable;

    Style style = child.getComputedStyle();
    String name = child.getName();

    int labelIndex = children.size();
    int display = style.getEnum(Style.DISPLAY);

    if (display == Style.NONE) {
      // ignore
    } else if (display == Style.TABLE || display == Style.INLINE_TABLE) { 
      children.add(new TableElementView(getContext(), child));
    } else if ("img".equals(name)) {
      buildContext.preserveLeadingSpace = true;
      children.add(NativeElementView.createImg(getContext(), child));
    } else if ("include".equals(name)){
      children.add(NativeElementView.createInclude(getContext(), child));
    } else if ("input".equals(name)) {
      children.add(NativeElementView.createInput(getContext(), child));
    } else if ("textarea".equals(name)) {
      children.add(NativeElementView.createTextArea(getContext(), child));
    } else if ("select".equals(name)) {
      children.add(NativeElementView.createSelect(getContext(), child));
    } else {
      if (style.isBlock(false)) {
        children.add(new BlockElementView(getContext(), child, buildContext));
      } else {
        // Element does not get its own block widget, just add the element's 
        // children to this block
        addChildren(child, buildContext);
      }
    }

    String label = child.getAttributeValue("id");
    if (label == null) {
      label = child.getAttributeValue("name");
    }
    if (label != null) {
      child.htmlView.labels.put(label, children.size() == 0 ? this
          : children.get(Math.min(children.size() - 1, labelIndex)));
    }
    String accesskey = child.getAttributeValue("accesskey");
    if (accesskey != null && accesskey.length() == 1) {
      child.htmlView.accesskeys.put(accesskey.charAt(0), child);
    }

    buildContext.unassignedTraversal = focusable && parentUnassignedTraversal;
  }

  /**
   * Adjust the horizontal align for child widgets on the same line.
   * 
   * @param startIndex first child widget on the line
   * @param endIndex last child widget on the line + 1
   * @param layoutContext Layout context object holding all relevant layout
   *    information (line height, available width, space distribution factors)
   */
  void adjustLine(int startIndex, int endIndex, LayoutContext layoutContext) {
    int lineHeight = layoutContext.getLineHeight();
    int remainingWidth = layoutContext.getHorizontalSpace(lineHeight);    
    int indent = layoutContext.getAdjustmentX(remainingWidth);

    for (int i = startIndex; i < endIndex; i++) {
      View w = getChildAt(i);
      if (w instanceof TextFragmentView
          && ((TextFragmentView) w).getLineCount() > 1) {
        HtmlUtils.assertEquals(i, startIndex);
        TextFragmentView tfw = (TextFragmentView) w;
        tfw.adjustLastLine(indent, lineHeight, layoutContext);
      } else {
        HasMeasuredPosition c = (HasMeasuredPosition) w;
        int dy = layoutContext.getAdjustmentY(lineHeight - w.getMeasuredHeight());
        c.setMeasuredPosition(c.getMeasuredX() + indent, c.getMeasuredY() + dy);
      }
    }
  }

  /**
   * Adjust dimensions to include all child widgets and adjust 
   * boxX, boxY, boxW, and boxH to reflect the original position and dimensions
   */
  void adjustDimensions() {  
    int minX = 0;
    int minY = 0;
    int maxX = boxWidth;
    int maxY = boxHeight;
    Style style = element.getComputedStyle();

    if (!(this instanceof HtmlView) && 
        style.getEnum(Style.OVERFLOW) != Style.HIDDEN) {

      int cnt = getChildCount();
      for (int i = 0; i < cnt; i++) {
        View child = getChildAt(i);
        HasMeasuredPosition c = (HasMeasuredPosition) child;
        int cx = c.getMeasuredX();
        int cy = c.getMeasuredY();
        minX = Math.min(minX, cx);
        minY = Math.min(minY, cy);
        maxX = Math.max(maxX, cx + child.getMeasuredWidth());
        maxY = Math.max(maxY, cy + child.getMeasuredHeight());
      }

      if (style.getEnum(Style.DISPLAY) == Style.LIST_ITEM) {
        minX = Math.min(minX, -((AbstractElementView) getParent()).element.getScaledPx(Style.MARGIN_LEFT));
      }
      
      boxX = -minX;
      boxY = -minY;

      if (minX < 0 || minY < 0) {
        for (int i = 0; i < cnt; i++) {
          HasMeasuredPosition child = (HasMeasuredPosition) getChildAt(i);
          child.setMeasuredPosition(child.getMeasuredX() - minX, child.getMeasuredY() - minY);
        }
      }
    }
    setMeasuredDimension(this.getMeasuredX() - boxX, this.getMeasuredY() - boxY, maxX - minX, maxY - minY);
  //  Dim.setDimensions(this, Dim.getX(this) - boxX, Dim.getY(this) - boxY, maxX - minX, maxY - minY);
  }

  /**
   * Calculates the minimum and maximum widths of a block and stores them
   * in minimumWidth and maximumWidth. Do not call this method or use
   * the values directly, use getMinimumWidth() or getMaximumWidht() instead.
   * 
   * @param containerWidth Width of the container.
   */
  protected void calculateWidth(int containerWidth) {
    Style style = element.getComputedStyle();
    int border = 
        element.getScaledPx(Style.MARGIN_LEFT) +  element.getScaledPx(Style.MARGIN_RIGHT) + 
        element.getScaledPx(Style.BORDER_LEFT_WIDTH) + 
        element.getScaledPx(Style.BORDER_RIGHT_WIDTH) + 
        element.getScaledPx(Style.PADDING_LEFT) + element.getScaledPx(Style.PADDING_RIGHT);

    int display = style.getEnum(Style.DISPLAY);

    int minW;
    int maxW;
    int currentLineWidth = 0;

    if (display != Style.TABLE_CELL && style.isLengthFixedOrPercent(Style.WIDTH)) {
      minW = maxW = element.getScaledPx(Style.WIDTH, containerWidth);
    } else {
      maxW = minW = element.getScaledPx(Style.WIDTH);
      int childContainerWidth = display == Style.TABLE_CELL 
          ? element.getScaledPx(Style.WIDTH, containerWidth) + border
              : containerWidth - border;

      for (int i = 0; i < children.size(); i++) {
        View child = children.get(i);
        if (child instanceof TextFragmentView) {
          TextFragmentView fragment = (TextFragmentView) child;
          Paint font = fragment.getFont();
          String text = fragment.text;

          char c = 160;
          int wordWidth = 0;
          int wordStart = 0;
          for (int j = 0; j < text.length(); j++) {
            char d = text.charAt(j);
            if (HtmlUtils.canBreak(c, d)) {
              wordWidth = HtmlUtils.measureText(font, text, wordStart, j + 1);
              minW = Math.max(minW, wordWidth);
              currentLineWidth += wordWidth;
              if (c == '\n') {
                maxW = Math.max(maxW, currentLineWidth);
                currentLineWidth = 0;
              }
              wordWidth = 0;
              wordStart = j;
            }
            c = d;
          }
          if (c == '\n') {
            maxW = Math.max(maxW, currentLineWidth);
            currentLineWidth = 0;
          }

          minW = Math.max(minW, wordWidth);
          currentLineWidth += wordWidth;
        } else {
          AbstractElementView block = (AbstractElementView) child;
          Style childStyle = block.element.getComputedStyle();

          int childDisplay = childStyle.getEnum(Style.DISPLAY);
          if (childStyle.getEnum(Style.FLOAT) == Style.NONE && 
              (childDisplay == Style.BLOCK || childDisplay == Style.LIST_ITEM)) {
            maxW = Math.max(maxW, currentLineWidth);
            maxW = Math.max(maxW, block.getMaximumWidth(childContainerWidth));
            currentLineWidth = 0;
          } else {
            currentLineWidth += block.getMaximumWidth(childContainerWidth);
          }

          minW = Math.max(minW, block.getMinimumWidth(childContainerWidth));
        } 
      }
    }

    maxW = Math.max(maxW, currentLineWidth);

    minimumWidth = minW + border;
    maximumWidth = maxW + border;

    widthValid = true;
  }


  @Override
  void measureBlock(int outerMaxWidth, final int viewportWidth, 
      LayoutContext parentLayoutContext, boolean shrinkWrap) {

    // If the width did not change and none of the children changed and the
    // layout is not influenced by flow objects of the parent, we do not
    // need to re-layout. The HtmlView instanceof check is just extra safety against
    // infinite loops (because we overwrite onMeasure there).
    if (!isLayoutRequested() && parentLayoutContext == null 
        && containingWidth == outerMaxWidth && !(this instanceof HtmlView)) {
      measure(getMeasuredWidth(), getMeasuredHeight());
    }

    containingWidth = outerMaxWidth;
    AbstractElementView previousBlock = null;

    removeAllViews();

    Style style = element.getComputedStyle();
    int display = style.getEnum(Style.DISPLAY);

    // Left and right margins are stored in members to avoid the auto margin
    // calculation in other places. 
    marginLeft = element.getScaledPx(Style.MARGIN_LEFT, containingWidth);
    marginRight = element.getScaledPx(Style.MARGIN_RIGHT, containingWidth);

    int marginTop = element.getScaledPx(Style.MARGIN_TOP, containingWidth);
    int marginBottom = element.getScaledPx(Style.MARGIN_BOTTOM, containingWidth);

    int borderLeft = element.getScaledPx(Style.BORDER_LEFT_WIDTH, containingWidth);
    int borderTop =  element.getScaledPx(Style.BORDER_TOP_WIDTH, containingWidth);
    int borderBottom = element.getScaledPx(Style.BORDER_BOTTOM_WIDTH, containingWidth);
    int borderRight = element.getScaledPx(Style.BORDER_RIGHT_WIDTH, containingWidth);

    int paddingLeft = element.getScaledPx(Style.PADDING_LEFT, containingWidth);
    int paddingTop = element.getScaledPx(Style.PADDING_TOP, containingWidth);
    int paddingBottom = element.getScaledPx(Style.PADDING_BOTTOM, containingWidth);
    int paddingRight = element.getScaledPx(Style.PADDING_RIGHT, containingWidth);

    int left = marginLeft + borderLeft + paddingLeft;
    int right = marginRight + borderRight + paddingRight;
    int top = marginTop + borderTop + paddingTop;
    int bottom = marginBottom + borderBottom + paddingBottom;

    // ShrinkWrap means we need to calculate the width based on the contents
    // for floats, table entries etc. without a fixed width
    if (shrinkWrap) {
      outerMaxWidth = style.isLengthFixedOrPercent(Style.WIDTH)
          ? element.getScaledPx(Style.WIDTH, outerMaxWidth) + left + right
          : Math.min(outerMaxWidth, getMaximumWidth(containingWidth));
      // Otherwise, if this is not a table cell and the width is fixed, we need 
      // to calculate the value for auto margins here (This is typically used 
      // to center the contents).
    } else if (display != Style.TABLE_CELL && style.isLengthFixedOrPercent(Style.WIDTH)) {
      int remaining = containingWidth - element.getScaledPx(Style.WIDTH, containingWidth) - 
            left - right;

      if (style.getEnum(Style.MARGIN_LEFT) == Style.AUTO) {
        if (style.getEnum(Style.MARGIN_RIGHT) == Style.AUTO) {
          marginLeft = marginRight = remaining / 2;
          left += marginLeft;
          right += marginRight;
        } else {
          marginLeft = remaining;
          left += marginLeft;
        } 
      } else {
        right += remaining;
        marginRight += remaining;
      }
    }

    boxWidth = outerMaxWidth;

    boolean fixedHeight = isHeightFixed();
    if (fixedHeight && getParent() instanceof AbstractElementView) {
      boxHeight = top + getFixedInnerHeight() + bottom;
    }

    // calculate the maximum inner width (outerMaxWidth minus borders, padding,
    // margin) -- the maximum width available for child layout
    int innerMaxWidth = outerMaxWidth - left - right;

    // Keeps track of y-position and borders for the regular layout set by
    // floating elements. The viewport width is taken into account here.
    LayoutContext layoutContext = new LayoutContext(
        Math.min(innerMaxWidth, viewportWidth), style, parentLayoutContext, 
        left, top);

    // line break positions determined when laying out TextFragmentWidget
    // are carried over from one TextFragmentWidget to another.
    int breakPosition = -1;

    // Index of first widget on a single line. Used when adjusting position
    // for top/bottom/left/right/center align
    int lineStartIndex = 0;

    // This is the position where regular in-flow widgets are inserted. 
    // Positioned widget are inserted at the end so they are always on top of 
    // regular widgets.
    int childInsertionIndex = 0;

    // iterate all child widgets
    for (int childIndex = 0; childIndex < children.size(); childIndex++) {
      View child = children.get(childIndex);
      Style childStyle;
      Element childElement;
      int childPosition;
      int childWidth;

      if (child instanceof TextFragmentView) {
        TextFragmentView fragment = (TextFragmentView) child;
        addView(fragment, childInsertionIndex);
        childElement = fragment.element;
        childStyle = fragment.element.getComputedStyle();
        childPosition = childStyle.getEnum(Style.POSITION);

        fragment.setMeasuredPosition(left, top + layoutContext.getCurrentY());

        // line-break and size the fragment
        breakPosition = fragment.doLayout(childIndex, layoutContext, 
            breakPosition, lineStartIndex, childInsertionIndex);

        // update position and status accordingly
        if (fragment.getLineCount() > 1) {       
          lineStartIndex = childInsertionIndex;
        }
        childInsertionIndex++;
        childWidth = fragment.getWidth();
      } else {
        // break positions are valid only for sequences of TextFragmentWidget
        breakPosition = -1;
        AbstractElementView block = (AbstractElementView) child;
        childElement = block.element;
        childStyle = block.element.getComputedStyle();
        int childDisplay = childStyle.getEnum(Style.DISPLAY);
        childPosition = childStyle.getEnum(Style.POSITION);
        int floating = childStyle.getEnum(Style.FLOAT);

        if (childPosition == Style.ABSOLUTE || childPosition == Style.FIXED){
          // absolute or fixed position: move block to its position; leave
          // anything else unaffected (in particular the layout context).
          addView(block);

          block.measureBlock(innerMaxWidth, viewportWidth, null, true);
          int left1 = marginLeft + borderLeft;
          int right1 = marginRight + borderRight;
          int top1 = marginTop + borderTop;
          int bottom1 = marginBottom + borderBottom;
          int iw = boxWidth - left1 - right1;

          int mx;
          int my;
          if (childStyle.getEnum(Style.RIGHT) != Style.AUTO) {
            mx = boxWidth - block.boxX - right1 - block.boxWidth - 
                block.element.getScaledPx(Style.RIGHT, iw);
          } else if (childStyle.getEnum(Style.LEFT) != Style.AUTO) {
            mx = left1 + block.element.getScaledPx(Style.LEFT, iw) - block.boxX;
          } else {
            mx = left1 - block.boxX;
          }
          if (childStyle.getEnum(Style.TOP) != Style.AUTO) {
            my = top1 - block.boxY +
                block.element.getScaledPx(Style.TOP, getMeasuredHeight() - top1 - bottom1);
          } else if (childStyle.getEnum(Style.BOTTOM) != Style.AUTO) {
            my = top1 - block.boxY + boxHeight -
                block.element.getScaledPx(Style.TOP, getMeasuredHeight() - top1 - bottom1);
          } else {
            my = top + layoutContext.getCurrentY() - block.boxY;
          }
          block.setMeasuredPosition(mx, my);
        } else if (floating == Style.LEFT || floating == Style.RIGHT){
          // float: easy. just call layout for the block and place it.
          // the block is added to the layout context, but the current 
          // y-position remains unchanged (advance() is not called)

          addView(block);
          block.measureBlock(innerMaxWidth, viewportWidth, null, true);
          layoutContext.placeBox(block.boxWidth, block.boxHeight, 
              floating, childStyle.getEnum(Style.CLEAR));
          block.setMeasuredPosition(left + layoutContext.getBoxX() - block.boxX, 
              top + layoutContext.getBoxY() - block.boxY);
        } else if (childDisplay == Style.BLOCK || 
            childDisplay == Style.LIST_ITEM) {  
          // Blocks and list items always start a new paragraph (implying a new
          // line.

          // if there is a pending line, adjust the alignement for it
          if (layoutContext.getLineHeight() > 0) {
            if (lineStartIndex != childInsertionIndex) {
              adjustLine(lineStartIndex, childInsertionIndex, layoutContext);
            }
            layoutContext.advance(layoutContext.getLineHeight());
            previousBlock = null;
          }            

          // if the position is relative, the widget is inserted on top of 
          // others. Other adjustments for relative layout are made at the end
          if (childPosition == Style.RELATIVE) {
            addView(block);
          } else {
            addView(block, childInsertionIndex++);
          }

          if (layoutContext.clear(childStyle.getEnum(Style.CLEAR))) {
            previousBlock = null;
          }

          // check whether we can collapse margins with the previous block
          if (previousBlock != null) {
            int m1 = previousBlock.element.getScaledPx(
                Style.MARGIN_BOTTOM, outerMaxWidth);
            int m2 = block.element.getScaledPx(Style.MARGIN_TOP, outerMaxWidth);
            // m1 has been applied already, the difference between m1 and m2
            // still needs to be applied
            int delta;
            if (m1 < 0) {
              if (m2 < 0) {
                delta = -(m1 + m2);
              } else {
                delta = -m1;
              }
            } else if (m2 < 0) {
              delta = -m2;
            } else {
              delta = -Math.min(m1, m2);
            }
            layoutContext.advance(delta);
          }

          int saveY = layoutContext.getCurrentY();
          block.measureBlock(innerMaxWidth, viewportWidth, layoutContext, false);
          block.setMeasuredPosition(left - block.boxX, top + saveY - block.boxY);
          lineStartIndex = childInsertionIndex;
          previousBlock = block;
        } else { 
          // inline-block, needs to be inserted in the regular text flow 
          // similar to text fragments.
          addView(block, childInsertionIndex);
          previousBlock = null;
          block.measureBlock(innerMaxWidth, viewportWidth, null, childDisplay != Style.TABLE);
          int avail = layoutContext.getHorizontalSpace(block.boxHeight);
          if (avail >= block.boxWidth) {
            layoutContext.placeBox(
                block.boxWidth, block.boxHeight, Style.NONE, 0);
          } else {
            // line break necessary
            adjustLine(lineStartIndex, childInsertionIndex, layoutContext);
            lineStartIndex = childInsertionIndex;
            layoutContext.advance(layoutContext.getLineHeight());
            layoutContext.placeBox(
                block.boxWidth, block.boxHeight, Style.NONE, 0);    
            layoutContext.advance(layoutContext.getBoxY() - 
                layoutContext.getCurrentY());
            layoutContext.setLineHeight(block.boxHeight);
          }
          block.setMeasuredPosition(left + layoutContext.getBoxX() - block.boxX, 
                                    top + layoutContext.getCurrentY() - block.boxY);
          childInsertionIndex++;
        }
        childWidth = block.boxWidth;
      }

      // Make adjustments for relative positioning
      if (childPosition == Style.RELATIVE) {
        HasMeasuredPosition c = (HasMeasuredPosition) child;
        int mx;
        if (childStyle.isSet(Style.RIGHT)) {
          mx = c.getMeasuredX() + boxWidth - childWidth - 
              childElement.getScaledPx(Style.RIGHT, getWidth());
          
        } else {
          mx = c.getMeasuredX() + childElement.getScaledPx(Style.LEFT, getMeasuredWidth());
        }
        c.setMeasuredPosition(mx, c.getMeasuredY() + childElement.getScaledPx(Style.TOP, getMeasuredHeight()));
      }
    }

    // Still need to adjust alignment if there is a pending line.
    if (lineStartIndex != childInsertionIndex && 
        layoutContext.getLineHeight() != 0) {
      adjustLine(lineStartIndex, childInsertionIndex, layoutContext);
    }

    // make sure currentY() reflects the full contents of the layout context
    layoutContext.advance(layoutContext.getLineHeight());
    if (parentLayoutContext == null) {
      layoutContext.clear(Style.BOTH);
    }

    // if the height is not fixed, set it to the actual height here
    if (!fixedHeight) {
      boxHeight = (top + layoutContext.getCurrentY() + bottom);
    }

    // Adjust the parent's layout context to the new y positon
    if (parentLayoutContext != null) {
      parentLayoutContext.adjustCurrentY(layoutContext.getCurrentY());
      parentLayoutContext.advance(boxHeight - layoutContext.getCurrentY() - top);
    }

    // adjust dimensions and box coordinates for the case where children are 
    // partially outside the dimensions
    adjustDimensions();
  }

}
