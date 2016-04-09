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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.kobjects.css.CssUtils;
import org.kobjects.css.Style;
import org.kobjects.css.StyleSheet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;



/**
 * Stylable XML/HTML Element representation. Also used to represent an anonymous
 * parent element of the document root while parsing, in order to simplify the
 * process and to avoid an additional document class. 
 * 
 * @author Stefan Haustein
 */
public class Element implements org.kobjects.css.StylableElement {

  /**
   * Type constant for child elements.
   */
  public static final int ELEMENT = 1;

  /**
   * Type constant for text content.
   */
  public static final int TEXT = 2;

  private static final int FLAG_BLOCK = 1;
  private static final int FLAG_EMPTY = 2;
  private static final int FLAG_LIST_ITEM = 4;
  private static final int FLAG_TABLE_CELL = 8;
  private static final int FLAG_TABLE_ROW = 16;
  private static final int FLAG_PARAGRAPH = 32;
  private static final int FLAG_IGNORE_CONTENT = 64;
  private static final int FLAG_ADD_COMMENTS = 128;

  private static final int TAG_SCRIPT = 0x0010000;
  private static final int TAG_STYLE = 0x0020000;
  private static final int TAG_META = 0x0030000;
  private static final int TAG_LINK = 0x0040000;
  private static final int TAG_TITLE = 0x0050000;
  private static final int TAG_BASE = 0x0060000;

  /**
   * Hash table mapping tag names to flags and tag name constants. 
   * Used for automatically closing unbalanced elements.
   */
  private static HashMap<String, Integer> flagMap = new HashMap<String,Integer>();

  static final String[] NON_TEXT_INPUT_TYPES = {"checkbox", "cancel", "submit", "hidden", "radio", "image"};
  
  static {
    setFlags("area", FLAG_EMPTY);
    setFlags("base", FLAG_EMPTY | TAG_BASE);
    setFlags("basefont", FLAG_EMPTY);
    setFlags("br", FLAG_EMPTY);
    setFlags("body", FLAG_BLOCK);
    setFlags("center", FLAG_BLOCK|FLAG_PARAGRAPH);
    setFlags("col", FLAG_EMPTY);
    setFlags("dd", FLAG_BLOCK | FLAG_LIST_ITEM);
    setFlags("dir", FLAG_BLOCK);
    setFlags("div", FLAG_BLOCK);
    setFlags("dl", FLAG_BLOCK);
    setFlags("dt", FLAG_BLOCK | FLAG_LIST_ITEM);
    setFlags("frame", FLAG_EMPTY);
    setFlags("h1", FLAG_BLOCK | FLAG_PARAGRAPH);
    setFlags("h2", FLAG_BLOCK | FLAG_PARAGRAPH);
    setFlags("h3", FLAG_BLOCK | FLAG_PARAGRAPH);
    setFlags("h4", FLAG_BLOCK | FLAG_PARAGRAPH);
    setFlags("h5", FLAG_BLOCK | FLAG_PARAGRAPH);
    setFlags("h6", FLAG_BLOCK | FLAG_PARAGRAPH);
    setFlags("hr", FLAG_BLOCK | FLAG_EMPTY);
    setFlags("img", FLAG_EMPTY);
    setFlags("input", FLAG_EMPTY);
    setFlags("isindex", FLAG_EMPTY);
    setFlags("li", FLAG_BLOCK | FLAG_LIST_ITEM);
    setFlags("link", FLAG_EMPTY | TAG_LINK);
    setFlags("marquee", FLAG_BLOCK);
    setFlags("menu", FLAG_BLOCK);
    setFlags("meta", FLAG_EMPTY | TAG_META);
    setFlags("ol", FLAG_BLOCK);
    setFlags("option", FLAG_BLOCK);
    setFlags("p", FLAG_BLOCK  | FLAG_PARAGRAPH);
    setFlags("param", FLAG_EMPTY);
    setFlags("pre", FLAG_BLOCK);
    setFlags("script", TAG_SCRIPT);
    setFlags("style", FLAG_ADD_COMMENTS | TAG_STYLE);
    setFlags("table", FLAG_BLOCK);
    setFlags("title", TAG_TITLE);
    setFlags("td", FLAG_BLOCK | FLAG_TABLE_CELL);
    setFlags("th", FLAG_BLOCK | FLAG_TABLE_CELL);
    setFlags("tr", FLAG_BLOCK | FLAG_TABLE_ROW);
    setFlags("ul", FLAG_BLOCK);
    setFlags("xmp", FLAG_BLOCK);
  }

  private static void setFlags(String name, int i) {
    flagMap.put(name, Integer.valueOf(i));
  }

  HtmlView htmlView;
  private Element parent;
  private String name;
  private HashMap<String,String> attributes;
  private ArrayList<Object> content;  
  private Style computedStyle;
  private Paint fontCache;
  private Paint backgroundCache;
  View nativeView;

  /**
   * Create a new element with the given name. 
   * 
   * @param htmlView the htmlView this element belongs to
   * @param name the name of this element
   */
  public Element(HtmlView htmlView, String name) {
    this.htmlView = htmlView;
    this.name = name;
  }

  /**
   * Add the given child element at the end of this element's content.
   * 
   * @param element child element to be added.
   */
  public void addElement(Element element) {
    if (content == null) {
      content = new ArrayList<Object>();
    }
    content.add(element);
    element.parent = this;
  }

  /**
   * Add the given text at the end of this element's content.
   * 
   * @param text the text to add.
   */
  public void addText(String text) {
    if (content == null) {
      content = new ArrayList<Object>();
    }
    content.add(text);
  }

  /**
   * Set the given attribute to the given value.
   * 
   * @param attrName attribute name
   * @param value attribute value
   */
  public void setAttribute(String attrName, String value) {
    if (attributes == null) {
      attributes = new HashMap<String, String>();
    }
    if (value == null) {
      attributes.remove(attrName);
    } else {
      attributes.put(attrName, value);
    }
  }

  /**
   * Returns the type (ELEMENT or TEXT) of the child node with the given index.
   * 
   * @param index node index
   * @return ELEMENT or TEXT
   */
  public int getChildType(int index) {
    return content.get(index) instanceof Element ? ELEMENT : TEXT;
  }

  /**
   * Returns the number of child nodes.
   * 
   * @return number of child nodes.
   */
  public int getChildCount() {
    return content == null ? 0 : content.size();
  }

  /**
   * Returns the child element with the given index. Note that the child node at
   * the given index needs to be an element, otherwise a class cast exception
   * will be thrown. Use getChildType() to ensure the correct type.
   */
  public Element getElement(int index) {
    return (Element) content.get(index);
  }

  /**
   * Returns the first child element with the given name, or null if there is no
   * such element.
   */
  public Element getElement(String childName) {
    if (content != null) {
      for (int i = 0; i < getChildCount(); i++) {
        if (getChildType(i) == ELEMENT
            && getElement(i).getName().equals(childName)) {
          return getElement(i);
        }
      }
    }
    return null;
  }

  /**
   * Returns the name of this element.
   */
  public String getName() {
    return name;
  }
  
  /** 
   * Returns the "native" view associated with this (input) element, e.g. the
   * EditText view for an input element.
   */
  public View getNativeView() {
    return nativeView;
  }

  /**
   * Returns the text node with the given index. Use getChildType() to ensure
   * the correct child node type. In the case of a mismatch, a class cast
   * exception will be thrown.
   */
  public String getText(int index) {
    return (String) content.get(index);
  }
  
  /**
   * Returns the first child which has a matching id attribute.
   * Searches recursively through all children.
   * 
   * @param id
   * @return First child with matching id attribute
   */
  public Element getChildById(String id) {
    int childCount = this.getChildCount();
    Element currChild = null;
    for (int i=0; i < childCount; i++) {
      if (this.getChildType(i) == Element.ELEMENT) {
        currChild = this.getElement(i);
        if (currChild != null && 
            id.equals(currChild.getAttributeValue("id"))
            ) {
          break;
        } else if (currChild.getChildCount() > 0) {
          currChild.getChildById(id);
        }
      }
    }
    return currChild;
  }


  /**
   * Returns the attribute value interpreted as a boolean.
   * If the attribute is not set or the value is "false"
   * or "0", false is returned. Otherwise, true is returned.
   */
  public boolean getAttributeBoolean(String attrName) {
    String v = getAttributeValue(attrName);
    if (v == null) {
      return false;
    }
    v = CssUtils.identifierToLowerCase(v.trim());
    return !"false".equals(v) && !"0".equals(v);
  }

  /**
   * Returns the value of the given attribute.
   * 
   * @param attrName name of the attribute
   * @return attribute value or null
   */
  public String getAttributeValue(String attrName) {
    return attributes == null ? null : (String) attributes.get(attrName);
  }

  /**
   * Returns the integer value of the given attribute. If there is no such
   * attribute, or the value is not parseable to an integer, 0 is returned.
   * 
   * @param attrName name of the attribute
   * @return attribute integer value or 0 if not available
   */  
  public int getAttributeInt(String attrName, int dflt) {
    String v = getAttributeValue(attrName);
    if (v == null) {
      return dflt;
    }
    try {
      return Integer.parseInt(v);
    }
    catch (NumberFormatException e) {
      return dflt;
    }
  }

  /**
   * Returns <a href="http://www.w3.org/TR/CSS21/cascade.html#computed-value">
   * computed css values</a>, taking the cascade and inheritance into 
   * account, but not actual layout or rendering.  If a style was not yet set 
   * (i.e. SyleSheet.apply() was not called), null is returned.
   */
  public Style getComputedStyle() {
    return computedStyle;
  }

  /**
   * Sets the focused flag for this element.
   */
  public void setFocused() {
    htmlView.focusedElement = this;
  }

  /**
   * Returns the distance to an ancestor with the given name. If there is no
   * such ancestor, -1 is returned. Used to decide what to do about unbalanced tags.
   */
  public int getAncestorDistance(String aName) {
    Element p = parent;
    int dist = 1;
    while (p != null) {
      if (p.getName().equals(aName)) {
        return dist;
      }
      dist++;
      p = p.parent;
    }
    return -1;
  }

  /**
   * Returns true if this element is currently focused. Used by widgets to make 
   * sure the focus highlight spans all elements involved, not just the primary 
   * widget that is actually traversed. 
   */
  public boolean isFocused() {
    return htmlView.focusedElement == this;
  }

  /**
   * Returns true if the element is a link
   */
  public boolean isLink() {
    return getName().equals("a") && getAttributeValue("href") != null;
  }

  /**
   * Returns the parent element, or null if this is the root element.
   */
  public Element getParent() {
    return parent;
  }

  /** 
   * Returns all text content in a single string, skipping all tags (but including
   * the text content of child elements).
   */
  public String getText() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < getChildCount(); i++) {
      if (getChildType(i) == TEXT) {
        sb.append(getText(i));
      } else if (getChildType(i) == ELEMENT) {
        sb.append(getElement(i).getText());
      }

    }
    return sb.toString();
  }

  /**
   * Parses the contents of an element (the part between the start and the end
   * tag, not including the attributes).
   * 
   * @param parser the parser
   * @throws XmlPullParserException should never be thrown in relaxed mode
   * @throws IOException for underlying IO exceptions
   */
  void parseContent(XmlPullParser parser) throws XmlPullParserException, IOException {
    int autoClose = 0;
    int flags = 0;
    Integer flagObject = (Integer) flagMap.get(name);
    if (flagObject != null) {
      flags = flagObject.intValue();
      if ((flags & FLAG_TABLE_ROW) != 0) {
        autoClose = FLAG_TABLE_ROW;
      } else if ((flags & FLAG_LIST_ITEM) != 0) {
        autoClose = FLAG_LIST_ITEM;
      } else if ((flags & FLAG_TABLE_CELL) != 0) {
        autoClose = FLAG_TABLE_CELL | FLAG_TABLE_ROW;
      } else if ((flags & FLAG_PARAGRAPH) != 0) {
        autoClose = FLAG_BLOCK;
      }
    }

    int position = parser.getLineNumber();

    if ((flags & FLAG_EMPTY) == 0) {
      loop : while (true) {
        switch (parser.getEventType()) {
          case XmlPullParser.START_TAG:
            String childName = CssUtils.identifierToLowerCase(parser.getName());
            Element child;
            Integer childFlagObject = (Integer) flagMap.get(childName);
            int childFlags = childFlagObject == null ? 0 : childFlagObject.intValue();

            if ((autoClose & childFlags) != 0) {
              // closing <name> implied by <childName>
              break loop;
            } else {
              if ((childFlags & FLAG_TABLE_CELL) != 0 && (flags & FLAG_TABLE_ROW) == 0) {
                child = new Element(htmlView, "tr");
                addElement(child);
              } else {
                child = new Element(htmlView, childName);
                addElement(child);
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                  child.setAttribute(
                      CssUtils.identifierToLowerCase(parser.getAttributeName(i)),
                      parser.getAttributeValue(i));
                }
                parser.nextToken();
              }
              child.parseContent(parser);
            }
            break;

          case XmlPullParser.CDSECT:
          case XmlPullParser.ENTITY_REF:
          case XmlPullParser.TEXT:
            if ((flags & FLAG_IGNORE_CONTENT) == 0) {
              addText(parser.getText());
            }
            parser.nextToken();
            break;

          case XmlPullParser.END_TAG:
            String endName = CssUtils.identifierToLowerCase(parser.getName());
            if (endName.equals(name)) {
              // direct match -> advance and leave loop
              parser.nextToken();
              break loop;
            } else {
              Integer endFlags = (Integer) flagMap.get(endName);
              if (endFlags == null || (endFlags.intValue() & FLAG_EMPTY) == 0) {
                int delta = getAncestorDistance(endName);
                if (delta <= 2 && delta != -1) {
                  // parent match -> don't advance, but leave loop
                  break loop;
                }
              }
            }
            // ignore unmatched end tags
            parser.nextToken();
            break;

          case XmlPullParser.END_DOCUMENT:
            break loop;

          case XmlPullParser.COMMENT:
            // add comments as text for script and style elements, otherwise
            if ((flags & FLAG_ADD_COMMENTS) != 0) {
              addText(parser.getText());
            }
            parser.nextToken();
            break;

          default:
            // ignore other content (DTD, comments, PIs etc.)
            parser.nextToken();
        }
      }
    }

    // perform action on element 
    switch (flags & 0xffff0000) {
      case TAG_STYLE:
        if (StyleSheet.matchesMediaType(getAttributeValue("media"), HtmlView.MEDIA_TYPES)) {
          htmlView.updateStyle(htmlView.baseUrl, getText(), new int[] {position});
        }
        break;
      case TAG_TITLE:
        htmlView.title = getText();
        break;
      case TAG_LINK:
        String rel = getAttributeValue("rel");
        if (!"stylesheet alternate".equals(rel) && 
            ("stylesheet".equals(rel) || 
                "text/css".equals(getAttributeValue("type")))
                && StyleSheet.matchesMediaType(getAttributeValue("media"), HtmlView.MEDIA_TYPES)) {
          htmlView.requestStyleSheet(htmlView.getAbsoluteUrl(getAttributeValue("href")), new int[] {position});
        } 
        break;
      case TAG_BASE:
        String href = getAttributeValue("href");
        if (href != null) {
          try {
            htmlView.baseUrl = new URI(href);
          } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
    }
  }
  
  public Iterator<Element> getChildElementIterator() {
    return new Iterator<Element>() {
      Element next;
      int index;
      
      @Override
      public boolean hasNext() {
        while (next == null && index < getChildCount()) {
          if (getChildType(index) == ELEMENT) {
            next = getElement(index);
          }
          index++;
        }
        return next != null;
      }

      @Override
      public Element next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        Element result = next;
        next = null;
        return result;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /** 
   * Called from StyleSheet.apply with the computed style.
   * Return false to signal that style calculation should be aborted.
   */
  public boolean setComputedStyle(Style style) {
    fontCache = null;
    backgroundCache = null;
    Style previousStyle = computedStyle;
    
    // handle legacy stuff 

    applyHtmlAttributes(style);
    computedStyle = style;

    if (!htmlView.needsBuild) {
      htmlView.needsBuild = previousStyle == null || 
      previousStyle.getEnum(Style.DISPLAY) != 
        computedStyle.getEnum(Style.DISPLAY) || 
        previousStyle.isBlock(false) != computedStyle.isBlock(false);
    }
    return true;
  }
  
  
  /**
   * Apply HTML attributes that influence the style (align, color, valign etc.).
   */
  private void applyHtmlAttributes(Style style) {
    String s = getAttributeValue("align");
    if (s == null) {
      s = getAttributeValue("halign");
    }
    if (s != null) {
      s = CssUtils.identifierToLowerCase(s.trim());
      if ("left".equals(s)) {
        style.set(Style.TEXT_ALIGN, Style.LEFT, Style.ENUM);
      } else if ("right".equals(s)) {
        style.set(Style.TEXT_ALIGN, Style.RIGHT, Style.ENUM);
      } else if ("center".equals(s)) {
        style.set(Style.TEXT_ALIGN, Style.CENTER, Style.ENUM);
      }
    }
    s = getAttributeValue("width");
    if (s != null) {
      try {
        if (s.endsWith("%")) {
          style.set(Style.WIDTH, 1000 * Integer.parseInt(
              s.substring(0, s.length() - 1)), Style.PERCENT);
        } else {
          if (s.endsWith("px")) {
            s = s.substring(0, s.length() - 2);
          }
          style.set(Style.WIDTH, 1000 * Integer.parseInt(s), Style.PX);
        }
      } catch (NumberFormatException e) {
        // do nothing for unparseable width attributes
      }
    }
    s = getAttributeValue("height");
    if (s != null) {
      try {
        if (s.endsWith("%")) {
          style.set(Style.HEIGHT, 1000 * Integer.parseInt(
              s.substring(0, s.length() - 1)), Style.PERCENT);
        } else {
          if (s.endsWith("px")) {
            s = s.substring(0, s.length() - 2);
          }
          style.set(Style.HEIGHT, 1000 * Integer.parseInt(s), Style.PX);
        }
      } catch (NumberFormatException e) {
        // do nothing for unparseable height attributes
      }
    }
    s = getAttributeValue("bgcolor");
    if (s != null) {
      style.setColor(Style.BACKGROUND_COLOR, s, 0);
    }

    boolean table = "table".equals(name);
    if (table || "td".equals(name) || "th".equals(name)) {
      s = getTableAttributeValue("valign");
      if (s != null) {
        s = CssUtils.identifierToLowerCase(s.trim());
        if ("top".equals(s)) {
          style.set(Style.VERTICAL_ALIGN, Style.TOP, Style.ENUM);
        } else if ("bottom".equals(s)) {
          style.set(Style.VERTICAL_ALIGN, Style.BOTTOM, Style.ENUM);
        } else if ("center".equals(s)) {
          style.set(Style.VERTICAL_ALIGN, Style.MIDDLE, Style.ENUM);
        }
      } 
      s = getTableAttributeValue("border");
      if (s != null) {
        int border = 1;
        try {
          border = Integer.parseInt(s);
        } catch (NumberFormatException e) {
          // ignore
        }
        if (!table) {
          border = Math.min(border, 1);
        }

        style.set(Style.BORDER_TOP_STYLE | Style.MULTIVALUE_TRBL, Style.SOLID, Style.ENUM, 0);
        style.set(Style.BORDER_TOP_COLOR | Style.MULTIVALUE_TRBL, 0xffcccccc, Style.ARGB, 0);
        style.set(Style.BORDER_TOP_WIDTH | Style.MULTIVALUE_TRBL, 1000 * border, Style.PX, 0);
      }
      if (!table) {
        s = getTableAttributeValue("cellpadding");
        if (s != null) {
          int padding = 0;
          try {
            padding = Integer.parseInt(s);
          } catch (NumberFormatException e) {
            // ignore
          }
          style.set(Style.PADDING_TOP | Style.MULTIVALUE_TRBL, 1000 * padding, Style.PX, 0);
        }
        s = getTableAttributeValue("cellspacing");
        if (s != null) {
          int spacing = 0;
          try {
            spacing = Integer.parseInt(s);
          } catch (NumberFormatException e) {
            // ignore
          }
          style.set(Style.MARGIN_TOP | Style.MULTIVALUE_TRBL, 500 * spacing, Style.PX, 0);
        }
      }
    } 
    if ("font".equals(name)) {
      String color = getAttributeValue("color");
      if (color != null) {
        style.setColor(Style.COLOR, color, 0);
      } 
    } else if ("img".equals(name)) {
      int i = getAttributeInt("vspace", -1);
      if (i >= 0) {
        style.set(Style.PADDING_TOP, i * 1000, Style.PX);
        style.set(Style.PADDING_BOTTOM, i * 1000, Style.PX);
      }
      i = getAttributeInt("hspace", -1);
      if (i >= 0) {
        style.set(Style.PADDING_LEFT, i * 1000, Style.PX);
        style.set(Style.PADDING_RIGHT, i * 1000, Style.PX);
      }
    } else if ("input".equals(name)) {
      if (!style.isSet(Style.WIDTH) && CssUtils.indexOfIgnoreCase(NON_TEXT_INPUT_TYPES, getAttributeValue("type")) == -1) {
        int size = getAttributeInt("size", 20);
        // Adding 2 chars for decoration
        style.set(Style.WIDTH, style.getPx(Style.FONT_SIZE) * (size + 2) * 500, Style.PX);
      }
    } else if ("textarea".equals(name)) {
      if (!style.isSet(Style.WIDTH)) {
        int cols = getAttributeInt("cols", 20);
        style.set(Style.WIDTH, style.getPx(Style.FONT_SIZE) * (cols + 2) * 500, Style.PX);
      }
/*      if (!style.isSet(Style.HEIGHT)) {
        int rows = getAttributeInt("rows", 2);
        style.set(Style.HEIGHT, style.getPx(Style.FONT_SIZE) * (rows + 1) * 1000, Style.PX);
      } */
    }
  }

  /**
   * Looks up table attributes in parent elements of the same table (tr, table)
   * 
   * @param attribute name
   * @return attribute value or null if not present
   */
  private String getTableAttributeValue(String attr) {
    String s = getAttributeValue(attr);
    if (s == null && parent != null && ("table".equals(parent.name) || 
        "tr".equals(parent.name))) {
      s = parent.getTableAttributeValue(attr);
    }
    return s;
  }

  /**
   * Remove the child node with the given index.
   * 
   * @param i index of the child node to be removed
   */
  public void remove(int i) {
    content.remove(i);
  }

  /**
   * Sets the parent element. Currently used to detach the html root element 
   * from the document pseudo element after parsing. This is necessary to make
   * CSS behave correctly. 
   * 
   * @param parent the new parent element
   */
  public void setParent(Element parent) {
    this.parent = parent;    
  }
  
  public HtmlView gethtmlView() {
       return htmlView;
  }
  
  public int getScaledPx(int id) {
    return Math.round(computedStyle.getPx(id) * htmlView.pixelScale);
  }

  public Paint getFont() {
    if (this.fontCache == null) {
      this.fontCache = new Paint(Paint.ANTI_ALIAS_FLAG);
      this.fontCache.setTextSize(getScaledPx(Style.FONT_SIZE));
      int style = computedStyle.getRaw(Style.FONT_WEIGHT) >= Style.BOLD ? Typeface.BOLD : Typeface.NORMAL;
      switch (computedStyle.getEnum(Style.FONT_STYLE)) {
      case Style.ITALIC: 
        style |= Typeface.ITALIC;
        break;
      case Style.OBLIQUE:
        fontCache.setTextSkewX(-0.25f);
        break;
      }
      Typeface typeface = Typeface.DEFAULT;
      String fontFamily = computedStyle.getString(Style.FONT_FAMILY);
      if (fontFamily != null) {
        String s = CssUtils.identifierToLowerCase(fontFamily);
        if (s.indexOf("monospace") != -1) {
          typeface = Typeface.MONOSPACE;
        } else if (s.indexOf("serif") != -1) {
          typeface = s.indexOf("sans") != -1 ? Typeface.SANS_SERIF : Typeface.SERIF;
        }
      }
      this.fontCache.setTypeface(style == 0 ? typeface : Typeface.create(typeface, style));
      this.fontCache.setColor(computedStyle.getColor(Style.COLOR));
    }
    return this.fontCache;
  }

  public int getScaledPx(int id, int percentOf) {
    return Math.round(computedStyle.getPx(id, Math.round(percentOf / htmlView.pixelScale)) * htmlView.pixelScale);
  }

  
  public Paint getBackgroundPaint() {
    if (backgroundCache == null) {
      int color = computedStyle.getColor(Style.BACKGROUND_COLOR);
      if ((color & 0x0ff000000) != 0) {
        backgroundCache = new Paint();
        backgroundCache.setStyle(android.graphics.Paint.Style.FILL);
        backgroundCache.setColor(color);
      }
    } 
    return backgroundCache;
  }

  public boolean click() {
    if (isLink()) {
      String href = getAttributeValue("href");
      if (href.startsWith("#")) {
        htmlView.gotoLabel(href.substring(1));
      } else {
        htmlView.requestHandler.openLink(htmlView, this,
            htmlView.getAbsoluteUrl(getAttributeValue("href")));
      }
      return true;
    }
    return false;
  }

  public int indexOf(Element element) {
    if (content != null) {
      int len = content.size();
      int index = 0;
      for (int i = 0; i < len; i++) {
        Object o = content.get(i);
        if (o == element) {
          return index;
        }
        if (o instanceof Element) {
          index++;
        }
      }
    }
    return -1;
  }
  
  /**
   * returns the stringified start tag including attributes. Does not do any escaping --
   * this is not safe for any use other than debugging.
   */
  public String toString() {
    StringBuilder sb = new StringBuilder("<");
    sb.append(name);
    if (attributes != null) {
      for (Map.Entry<String,String> e: attributes.entrySet()) {
        sb.append(' ');
        sb.append(e.getKey());
        sb.append('=');
        sb.append(e.getValue()); 
      }
    }
    sb.append('>');
    return sb.toString();
  }
}
