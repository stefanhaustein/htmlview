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

package org.kobjects.css;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class represents a CSS style sheet. It is also used to represent parts of a style sheet in 
 * a tree structure, where the depth of the tree equals the length of the longest selector.
 * 
 * @author Stefan Haustein
 */
public class StyleSheet {
  private static final char SELECT_ATTRIBUTE_NAME = 7;
  private static final char SELECT_ATTRIBUTE_VALUE = 8;
  private static final char SELECT_ATTRIBUTE_INCLUDES = 9;
  private static final char SELECT_ATTRIBUTE_DASHMATCH = 10;

  /**
   * Specificity weight for element name and pseudoclass selectors.
   */
  private static final int SPECIFICITY_D = 1;

  /**
   * Specificity weight for element name selectors.
   */
  private static final int SPECIFICITY_C = 100 * SPECIFICITY_D;

  /**
   * Specificity weight for id selectors.
   */                                        
  private static final int SPECIFICITY_B = 100 * SPECIFICITY_C;

  /**
   * Specificity weight for !important selectors
   */
  static final int SPECIFICITY_IMPORTANT = 100 * SPECIFICITY_B;

  /**
   * A table mapping element names to sub-style sheets for the corresponding
   * selection path.
   */
  public HashMap<String, StyleSheet> selectElementName;

  /**
   * A table mapping pseudoclass names to sub-style sheets for the corresponding
   * selection path.
   */
  private HashMap<String, StyleSheet> selectPseudoclass;

  /**
   * A list of attribute names for selectors. Forms attribute selectors together
   * with selectAttributeOperation and selectAttributeValue.
   */
  private ArrayList<String> selectAttributeName;

  /**
   * A list of attribute operations for selectors (one of the
   * SELECT_ATTRIBUTE_XX constants). Forms attribute selectors together with
   * selectAttributeName and selectAttributeValue.
   */
  private StringBuilder selectAttributeOperation;

  /**
   * A list of Hashtables, mapping attribute values to sub-style sheets for the
   * corresponding selection path. Forms attribute selectors together with
   * selectAttributeName and selectAttributeOperation.
   */
  private ArrayList<HashMap<String, StyleSheet>> selectAttributeValue;

  /**
   * Reference to child selector selector sub-style sheet.
   */
  private StyleSheet selectChild;

  /**
   * Reference to descendant selector sub-style sheet.
   */
  private StyleSheet selectDescendants;

  /**
   * Properties for * rules 
   */
  private ArrayList<Style> properties;

  /**
   * Creates a new style sheet with default rules for HTML.
   * 
   * <table>
   * <tr><td>a </td><td>color: #0000ff; decoration: underline</td></tr>
   * <tr><td>b </td><td>font-weight: 700; </td></tr>
   * <tr><td>body </td><td>display: block; padding: 5px; </td></tr>
   * <tr><td>dd </td><td>display: block; </td></tr>
   * <tr><td>dir </td><td>display: block; margin-top: 2px; 
   *  margin-bottom: 2px; margin-left: 10px; </td></tr>
   * <tr><td>div </td><td>display: block; </td></tr>
   * <tr><td>dl </td><td>display: block; </td></tr>
   * <tr><td>dt </td><td>display: block; </td></tr>
   * <tr><td>h1 .. h6</td><td>display: block; font-weight: 700; 
   *  margin-top: 2px; margin-bottom: 2px; </td></tr>
   * <tr><td>hr </td><td>border-top-color: #888888; border-top-width: 1px; 
   *  border-top-style: solid; display: block; 
   *  margin-top: 2px; margin-bottom: 2px; </td></tr>
   * <tr><td>li </td><td>display: list-item; margin-top: 2px; 
   *  margin-bottom: 2px; </td></tr></td></tr>
   * <tr><td>ol </td><td>display: block; list-style-type: decimal; 
   *  margin-left: 10px; </td></tr>
   * <tr><td>p </td><td>display: block; margin-top: 2px; 
   *  margin-bottom: 2px; </td></tr>
   * <tr><td>th </td><td>display: table-cell; font-weight: 700;  
   *  padding: 1px;</td></tr>
   * <tr><td>tr </td><td>display: table-row;</td></tr>
   * <tr><td>td </td><td>display: table-cell; padding: 1px; </td></tr>
   * <tr><td>ul </td><td>display: block; margin-left: 10px; </td></tr>
   * <tr><td>img </td><td>display: inline-block; </td></tr>
   * </table>
   */
  public static StyleSheet createDefault() {
    StyleSheet s = new StyleSheet();
    // Set default indent with to sufficient space for ordered lists with
    // two digits and the default paragraph spacing to 50% of the font height
    // (so top and bottom spacing adds up to a full line)
    int defaultIndent = 50000; // ((int) font.measureText("88. ")) * 1000;
    int defaultParagraphSpace = 12000; // Math.max(1, ((int) font.getFontMetrics(null)) / 2) * 1000;
    
    s.put(":link", new Style().set(Style.COLOR, 0x0ff0000ff, Style.ARGB).
        set(Style.TEXT_DECORATION, Style.UNDERLINE, Style.ENUM));
    s.put("address", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM));
    s.put("b", new Style().set(Style.FONT_WEIGHT, 700000, Style.NUMBER));
    Style tt = new Style();
    tt.fontFamily = "monospace";
    s.put("tt", tt);
    s.put("blockquote", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
        set(Style.MARGIN_TOP, defaultParagraphSpace, Style.PX).
        set(Style.MARGIN_RIGHT, defaultIndent, Style.PX).
        set(Style.MARGIN_BOTTOM, defaultParagraphSpace, Style.PX).
        set(Style.MARGIN_LEFT, defaultIndent, Style.PX));
    s.put("body", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
        set(Style.PADDING_TOP | Style.MULTIVALUE_TRBL, 
            defaultParagraphSpace / 2, Style.PX, 0));
    s.put("button", new Style().
        set(Style.DISPLAY, Style.INLINE_BLOCK, Style.ENUM).
        set(Style.PADDING_TOP | Style.MULTIVALUE_TRBL, 3000, Style.PX, 0));
    s.put("center", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
        set(Style.MARGIN_TOP, defaultParagraphSpace, Style.PX).
        set(Style.MARGIN_BOTTOM, defaultParagraphSpace, Style.PX).
        set(Style.TEXT_ALIGN, Style.CENTER, Style.ENUM));
    s.put("dd", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
        set(Style.MARGIN_LEFT, defaultIndent, Style.PX)
    );
    s.put("dir", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
        set(Style.MARGIN_TOP, defaultParagraphSpace, Style.PX).
        set(Style.MARGIN_BOTTOM, defaultParagraphSpace, Style.PX).
        set(Style.MARGIN_LEFT, defaultIndent, Style.PX).
        set(Style.LIST_STYLE_TYPE, Style.SQUARE, Style.ENUM));
    s.put("div", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM));
    s.put("dl", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM));
    s.put("dt", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM));
    s.put("form", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM));
    for (int i = 1; i <= 6; i++) {
      s.put("h" + i, new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
          set(Style.FONT_WEIGHT, 700000, Style.NUMBER).
          set(Style.MARGIN_TOP, defaultParagraphSpace, Style.PX).
          set(Style.MARGIN_BOTTOM, defaultParagraphSpace, Style.PX).
          set(Style.FONT_SIZE, 20000 - 2000 * i, Style.PT));
    }
    s.put("hr", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
        set(Style.BORDER_TOP_WIDTH, 1000, Style.PX).
        set(Style.BORDER_TOP_STYLE, Style.SOLID, Style.ENUM).
        set(Style.BORDER_TOP_COLOR, 0x0ff888888, Style.ARGB).
        set(Style.MARGIN_TOP, defaultParagraphSpace, Style.PX).
        set(Style.MARGIN_BOTTOM, defaultParagraphSpace, Style.PX));
    Style italic = new Style().set(Style.FONT_STYLE, Style.ITALIC, Style.ENUM);
    s.put("i", italic);
    s.put("em", italic);
    s.put("img", new Style().set(Style.DISPLAY, Style.INLINE_BLOCK, Style.ENUM));
    s.put("input", new Style().
        set(Style.DISPLAY, Style.INLINE_BLOCK, Style.ENUM));    
    s.put("li", new Style().set(Style.DISPLAY, Style.LIST_ITEM, Style.ENUM).
        set(Style.MARGIN_TOP, defaultParagraphSpace, Style.PX).
        set(Style.MARGIN_BOTTOM, defaultParagraphSpace, Style.PX));
    s.put("marquee", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM));
    s.put("menu", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
        set(Style.MARGIN_TOP, defaultParagraphSpace, Style.PX).
        set(Style.MARGIN_BOTTOM, defaultParagraphSpace, Style.PX).
        set(Style.MARGIN_LEFT, defaultIndent, Style.PX).
        set(Style.LIST_STYLE_TYPE, Style.SQUARE, Style.ENUM));
    s.put("ol", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
        set(Style.MARGIN_LEFT, defaultIndent, Style.PX).
        set(Style.LIST_STYLE_TYPE, Style.DECIMAL, Style.ENUM));
    s.put("p", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
        set(Style.MARGIN_TOP, defaultParagraphSpace, Style.PX).
        set(Style.MARGIN_BOTTOM, defaultParagraphSpace, Style.PX));
    Style pre = new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
        set(Style.WHITE_SPACE, Style.PRE, Style.ENUM).
        set(Style.MARGIN_TOP, defaultParagraphSpace, Style.PX).
        set(Style.MARGIN_BOTTOM, defaultParagraphSpace, Style.PX);
    pre.fontFamily = "monospace";
    s.put("pre", pre);
    s.put("script", new Style().set(Style.DISPLAY, Style.NONE, Style.ENUM));
    s.put("strong", new Style().set(Style.FONT_WEIGHT, Style.BOLD, Style.ENUM));
    s.put("style", new Style().set(Style.DISPLAY, Style.NONE, Style.ENUM));

    s.put("sup", new Style().set(Style.VERTICAL_ALIGN, Style.SUPER, Style.ENUM));
    s.put("sub", new Style().set(Style.VERTICAL_ALIGN, Style.SUB, Style.ENUM));

    s.put("table", new Style().set(Style.DISPLAY, Style.TABLE, Style.ENUM).
        set(Style.CLEAR, Style.BOTH, Style.ENUM));
    s.put("td", new Style().set(Style.DISPLAY, Style.TABLE_CELL, Style.ENUM).
        set(Style.PADDING_TOP | Style.MULTIVALUE_TRBL, 1000, Style.PX, 0).
        set(Style.TEXT_ALIGN, Style.LEFT, Style.ENUM));
    s.put("th", new Style().set(Style.DISPLAY, Style.TABLE_CELL, Style.ENUM).
        set(Style.FONT_WEIGHT, 700000, Style.NUMBER).
        set(Style.PADDING_TOP | Style.MULTIVALUE_TRBL, 1000, Style.PX, 0).
        set(Style.TEXT_ALIGN, Style.CENTER, Style.ENUM));
    s.put("tr", new Style().set(Style.DISPLAY, Style.TABLE_ROW, Style.ENUM));
    s.put("ul", new Style().set(Style.DISPLAY, Style.BLOCK, Style.ENUM).
        set(Style.MARGIN_LEFT, defaultIndent, Style.PX).
        set(Style.LIST_STYLE_TYPE, Style.SQUARE, Style.ENUM));
    s.put("ul ul", new Style().
        set(Style.LIST_STYLE_TYPE, Style.CIRCLE, Style.ENUM));
    s.put("ul ul ul", new Style().
        set(Style.LIST_STYLE_TYPE, Style.DISC, Style.ENUM));
    return s;
  }

  /**
   * Returns true if s matches any type in the given media type array. null, the empty string
   * and all are always matched. s is converted to lower case for the match.
   */
  public static boolean matchesMediaType(String s, String[] mediaTypes) {
    if (s == null) {
      return true;
    }
    s = s.trim().toLowerCase(Locale.US);
    return s.length() == 0 || s.equals("all") || CssUtils.indexOfIgnoreCase(mediaTypes, s) != -1;
  }
  
  
  /**
   * Reads a style sheet from the given css string and merges it into this style sheet.
   * @param css the CSS string to load the style sheet from
   * @param url URL of this style sheet (or the containing document)
   * @param nesting The nesting of this style sheet in other style sheets.
   * 
   * @return this
   * @throws IOException if there is an underlying stream exception
   */
  public StyleSheet read(String css, URI url, int[] nesting, String[] mediaTypes, List<Dependency> dependencies) {
    CssTokenizer ct = new CssTokenizer(url, css);
    int position = 0;
    boolean inMedia = false;
    while (ct.ttype != CssTokenizer.TT_EOF) {
      if (ct.ttype == CssTokenizer.TT_ATKEYWORD) {
        if ("media".equals(ct.sval)) {
          ct.nextToken(false);
          inMedia = false;
          do {
            if(ct.ttype != ',') {
              inMedia |= matchesMediaType(ct.sval, mediaTypes);
            }
            ct.nextToken(false);
          } while (ct.ttype != '{' && ct.ttype != CssTokenizer.TT_EOF);
          ct.nextToken(false);
          if (!inMedia) {
            int level = 1;
            do {
              switch (ct.ttype) {
              case '}': 
                level--;
                break;
              case '{':
                level++;
                break;
              case CssTokenizer.TT_EOF:
                return this;
              }
              ct.nextToken(false);
            } while (level > 0);
          }
        } else if ("import".equals(ct.sval)){
          ct.nextToken(false);
          String importUrl = ct.sval;
          ct.nextToken(false);
          StringBuilder media = new StringBuilder();
          while (ct.ttype != ';' && ct.ttype != CssTokenizer.TT_EOF) {
            media.append(ct.sval);
            ct.nextToken(false);
          }
          if (matchesMediaType(media.toString(), mediaTypes)) {
            int [] dependencyNesting = new int[nesting.length + 1];
            System.arraycopy(nesting, 0, dependencyNesting, 0, nesting.length);
            dependencyNesting[nesting.length] = position;
            dependencies.add(new Dependency(url.resolve(importUrl), dependencyNesting));
          }
          ct.nextToken(false);
          position++;
        } else {
          ct.debug("unsupported @" + ct.sval);
          ct.nextToken(false);
        }
      } else if (ct.ttype == '}') {
        if (!inMedia) {
          ct.debug("unexpected }");
        }
        inMedia = false;
        ct.nextToken(false);
      } else {
        // no @keyword or } -> regular selector
        ArrayList<Style> targets = new ArrayList<Style>();
        targets.add(parseSelector(ct));
        while (ct.ttype == ',') {
          ct.nextToken(false);
          targets.add(parseSelector(ct));
        }

        Style style = new Style();
        if (ct.ttype == '{') {
          ct.nextToken(false);
          style.read(ct);
          ct.assertTokenType('}');
        } else {
          ct.debug("{ expected");
        }

        for (int i = 0; i < targets.size(); i++) {
          Style target = targets.get(i);
          if (target == null) {
            continue;
          }
          target.position = position;
          target.nesting = nesting;
          target.set(style);
        }
        ct.nextToken(false);
        position++;
      }
    }
    return this;
  }

  /**
   * Parse a selector. The tokenizer must be at the first token of the selector.
   * When returning, the current token will be ',' or '{'.
   * <p>
   * This method brings selector paths into the tree form described in the class
   * documentation.
   * 
   * @param ct the css tokenizer
   * @return the node at the end of the tree path denoted by this selector,
   *         where the corresponding CSS properties will be stored
   * @throws IOException
   */
  private Style parseSelector(CssTokenizer ct) {

    boolean error = false;
    
    int specificity = 0;
    StyleSheet result = this;

    loop : while (true) {
      switch (ct.ttype) {
        case CssTokenizer.TT_IDENT: {
          if (result.selectElementName == null) {
            result.selectElementName = new HashMap<String, StyleSheet>();
          }
          result = descend(result.selectElementName, 
              CssUtils.identifierToLowerCase(ct.sval));
          specificity += SPECIFICITY_D;
          ct.nextToken(true);
          break;
        }
        case '*': {
          // no need to do anything...
          ct.nextToken(true);
          continue;
        }
        case '[': {
          ct.nextToken(false);
          String name = CssUtils.identifierToLowerCase(ct.sval);
          ct.nextToken(false);
          char type;
          String value = "";
          
          if (ct.ttype == ']') {
            type = SELECT_ATTRIBUTE_NAME;
          } else {
            switch (ct.ttype) {
              case CssTokenizer.TT_INCLUDES:
                type = SELECT_ATTRIBUTE_INCLUDES;
                break;
              case '=':
                type = SELECT_ATTRIBUTE_VALUE;
                break;
              case CssTokenizer.TT_DASHMATCH:
                type = SELECT_ATTRIBUTE_DASHMATCH;
                break;
              default:
                error = true;
                break loop;
            }
            ct.nextToken(false);
            if (ct.ttype != CssTokenizer.TT_STRING) {
              error = true;
              break loop;
            }
            value = ct.sval;
            ct.nextToken(false);
            ct.assertTokenType(']');
            specificity += SPECIFICITY_C;
          }
          result = result.createAttributeSelector(type, name, value);
          ct.nextToken(true);
          break;
        }
        case '.':
          ct.nextToken(false);
          error = ct.ttype != CssTokenizer.TT_IDENT;
          result = result.createAttributeSelector(SELECT_ATTRIBUTE_INCLUDES, "class", ct.sval);
          specificity += SPECIFICITY_C;
          ct.nextToken(true);
          break;

        case CssTokenizer.TT_HASH:
          result = result.createAttributeSelector(SELECT_ATTRIBUTE_VALUE, "id", ct.sval);
          specificity += SPECIFICITY_B;
          ct.nextToken(true);
          break;

        case ':': 
          ct.nextToken(false);
          error = ct.ttype != CssTokenizer.TT_IDENT;
          if (result.selectPseudoclass == null) {
            result.selectPseudoclass = new HashMap<String, StyleSheet>();
          }
          result = descend(result.selectPseudoclass, ct.sval);
          specificity += SPECIFICITY_C;
          ct.nextToken(true);
          break;

        case CssTokenizer.TT_S:
          ct.nextToken(false);
          if (ct.ttype == '{' || ct.ttype == ',' || ct.ttype == -1) {
            break loop;
          }
          if (ct.ttype == '>') {
            if (result.selectChild == null) {
              result.selectChild = new StyleSheet();
            }
            result = result.selectChild;
            ct.nextToken(false);
          } else {
            if (result.selectDescendants == null) {
              result.selectDescendants = new StyleSheet();
            }
            result = result.selectDescendants;
          }
          break;

        case '>':
          if (result.selectChild == null) {
            result.selectChild = new StyleSheet();
          }
          result = result.selectChild;
          ct.nextToken(false);
          break;

        default: // unknown
          break loop;
      }
    }

    // state: behind all recognized tokens -- check for unexpected stuff
    if (error || (ct.ttype != ',' && ct.ttype != '{')) {
      ct.debug("Unrecognized selector");
      // parse to '{', ',' or TT_EOF to get to a well-defined state
      while (ct.ttype != ',' && ct.ttype != CssTokenizer.TT_EOF
          && ct.ttype != '{') {
        ct.nextToken(false);
      }
      return null;
    }

    Style style = new Style();
    style.specificity = specificity;
    if (result.properties == null) {
      result.properties = new ArrayList<Style>();
    }
    result.properties.add(style);
    
    return style;
  }

  private StyleSheet createAttributeSelector(char type, String name, String value) {
    int index = -1;
    if (selectAttributeOperation == null) {
      selectAttributeOperation = new StringBuilder();
      selectAttributeName = new ArrayList<String>();
      selectAttributeValue = new ArrayList<HashMap<String, StyleSheet>>();
    } else {
      for (int j = 0; j < selectAttributeOperation.length(); j++) {
        if (selectAttributeOperation.charAt(j) == type
            && selectAttributeName.get(j).equals(name)) {
          index = j;
        }
      }
    }

   if (index == -1) {
      index = selectAttributeOperation.length();
      selectAttributeOperation.append(type);
      selectAttributeName.add(name);
      selectAttributeValue.add(new HashMap<String, StyleSheet>());
    }
    return descend(selectAttributeValue.get(index), value);
  }

  /**
   * Returns the style sheet denoted by the given key from the hashtable. If not
   * yet existing, a corresponding entry is created.
   */
  private static StyleSheet descend(Map<String, StyleSheet> h, String key) {
    StyleSheet s = h.get(key);
    if (s == null) {
      s = new StyleSheet();
      h.put(key, s);
    }
    return s;
  }

  /**
   * Helper method for collectStyles(). Determines whether the given key is 
   * in the given map. If so, the style search continues in the corresponding 
   * style sheet.
   * 
   * @param element the element under consideration (may be the target element
   *            or any parent)
   * @param map corresponding sub style sheet map
   * @param key element name or attribute value
   * @param queue queue of matching rules to be processed further
   */
  private static void collectStyles(StylableElement element, Map<String, StyleSheet> map, String key,
      List<Style> queue, List<StyleSheet> children, List<StyleSheet> descendants) {
    if (key == null || map == null) {
      return;
    }
    StyleSheet sh = map.get(key);
    if (sh != null) {
      sh.collectStyles(element, queue, children, descendants);
    }
  }

  /**
   * Performs a depth first search of all matching selectors and enqueues the
   * corresponding style information.
   * 
   * @param element The element currently under consideration
   * @param s the style to be modified
   * @param queue the queue
   */
  public void collectStyles(StylableElement element, List<Style> queue, List<StyleSheet> children, 
      List<StyleSheet> descendants) {
    
    if (properties != null) {
      // enqueue the style at the current node according to its specificity

      for (int i = 0; i < properties.size(); i++) {
        Style p = properties.get(i);
        int index = queue.size();
        while (index > 0) {
          Style s = queue.get(index - 1);
          if (s.compareSpecificity(p) < 0) {
            break;
          }
          if (s == p) {
            index = -1;
            break;
          }
          index--;
        }
        if (index != -1) {
          queue.add(index, p);
        }
      }
    }
      
    if (selectAttributeOperation != null) {
      for (int i = 0; i < selectAttributeOperation.length(); i++) {
        int type = selectAttributeOperation.charAt(i);
        String name = (String) selectAttributeName.get(i);
        String value = element.getAttributeValue(name);
        if (value == null) {
          continue;
        }
        HashMap<String, StyleSheet> valueMap = selectAttributeValue.get(i);
        if (type == SELECT_ATTRIBUTE_NAME) {
          collectStyles(element, valueMap, "", queue, children, descendants);
        } else if (type == SELECT_ATTRIBUTE_VALUE) {
          collectStyles(element, valueMap, value, queue, children, descendants);
        } else {
          String[] values = CssUtils.split(value,
              type == SELECT_ATTRIBUTE_INCLUDES ? ' ' : ',');
          for (int j = 0; j < values.length; j++) {
            collectStyles(element, valueMap, values[j], queue, children, descendants);
          }
        }
      }
    }

    if (selectElementName != null) {
      collectStyles(element, selectElementName, element.getName(), queue, children, descendants);
    }

    if (selectChild != null) {
      children.add(selectChild);
    }

    if (selectPseudoclass != null && element.isLink()) {
      collectStyles(element, selectPseudoclass, "link", queue, children, descendants);
    }
    
    if (selectDescendants != null) {
      descendants.add(selectDescendants);
    }
  }

  /**
   * Internal method used to simplify building the default style sheet.
   * 
   * @param selector element name
   * @param style default style for the element
   */
  private void put(String selector, Style style) {
    if (selectElementName == null) {
      selectElementName = new HashMap<String, StyleSheet>();
    }
    
    boolean simple = true;
    for (int i = 0; i < selector.length(); i++){
      char c = selector.charAt(i);
      if (c < 'a' || c > 'z') {
        simple = false;
        break;
      }
    }

    if (simple) {
      StyleSheet s = new StyleSheet();
      s.properties = new ArrayList<Style>();
      s.properties.add(style);
      style.specificity = SPECIFICITY_D - SPECIFICITY_IMPORTANT;
      selectElementName.put(selector, s);
    } else {
      CssTokenizer ct = new CssTokenizer(null, selector + "{");
      Style target = parseSelector(ct);
      target.set(style);
      // copy important
      target.specificity += style.specificity - SPECIFICITY_IMPORTANT; 
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    toString("", sb);
    return sb.toString();
  }

  public void toString(String current, StringBuilder sb) {
    if (properties != null) {
      sb.append(current.length() == 0 ? "*" : current);
      sb.append(" {");
      for (int i = 0; i < properties.size(); i++) {
        properties.get(i).toString("", sb);
      }
      sb.append("}\n");
    }

    if (selectElementName != null) {
      for (Map.Entry<String, StyleSheet> entry: selectElementName.entrySet()) {
        entry.getValue().toString(entry.getKey() + current, sb);
      }
    }

    if (selectAttributeOperation != null) {
      for (int i = 0; i < selectAttributeOperation.length(); i++) {
        int type = selectAttributeOperation.charAt(i);
        StringBuilder p = new StringBuilder(current);
        p.append('[');
        p.append((String) selectAttributeName.get(i));

        if (type == SELECT_ATTRIBUTE_NAME) {
          p.append(']');
          selectAttributeValue.get(i).get("").toString(p.toString(), sb);
        } else {
          switch (type) {
            case SELECT_ATTRIBUTE_VALUE:
              p.append('=');
              break;
            case SELECT_ATTRIBUTE_INCLUDES:
              p.append("~=");
              break;
            case SELECT_ATTRIBUTE_DASHMATCH:
              p.append("|=");
              break;
          }
          HashMap<String, StyleSheet> valueMap = selectAttributeValue.get(i);
          for (Map.Entry<String, StyleSheet> e : valueMap.entrySet()) {
            e.getValue().toString(p.toString() + '"' + e.getKey() + "\"]", sb);
          }
        }
      }
    }

    if (selectDescendants != null) {
      selectDescendants.toString(current + " ", sb);
    }

    if (selectChild != null) {
      selectChild.toString(current + " > ", sb);
    }
  }

  public void apply(StylableElement element, URI baseUri) {
    ArrayList<StyleSheet> applyAnywhere = new ArrayList<StyleSheet>();
    applyAnywhere.add(this);
    StyleSheet.apply(element, baseUri, null, new ArrayList<StyleSheet>(), applyAnywhere);
    
  }

  /**
   * Applies the given style sheet to this element and recursively to all child
   * elements, setting the computedStyle field to the computed CSS values.
   * <p>
   * Technically, it builds a queue of applicable styles and then applies them 
   * in the order of ascending specificity. After the style sheet has been 
   * applied, the inheritance rules and finally the style attribute are taken 
   * into account.
   */
  private static void apply(StylableElement element, URI baseUri, Style inherit, 
      List<StyleSheet> applyHere, List<StyleSheet> applyAnywhere) {
    Style style = new Style();
  
    ArrayList<Style> queue = new ArrayList<Style>();
    ArrayList<StyleSheet> childStyles = new ArrayList<StyleSheet>();
    ArrayList<StyleSheet> descendantStyles = new ArrayList<StyleSheet>();
  
    int size = applyHere.size();
    for (int i = 0; i < size; i++) {
      StyleSheet styleSheet = applyHere.get(i);
      styleSheet.collectStyles(element, queue, childStyles, descendantStyles);
    }
    size = applyAnywhere.size();
    for (int i = 0; i < size; i++) {
      StyleSheet styleSheet = applyAnywhere.get(i);
      descendantStyles.add(styleSheet);
      styleSheet.collectStyles(element, queue, childStyles, descendantStyles);
    }
  
    for (int i = 0; i < queue.size(); i++) {
      style.set((queue.get(i)));
    }
  
    String styleAttr = element.getAttributeValue("style");
    if (styleAttr != null) {
      style.read(baseUri, styleAttr);
    }
  
    if (inherit != null) {
      style.inherit(inherit);
    }
  
    if (element.setComputedStyle(style)) {
      // recurse....
      Iterator<? extends StylableElement> iterator = element.getChildElementIterator();
      while(iterator.hasNext()) {
        apply(iterator.next(), baseUri, style, childStyles, descendantStyles);
      }
    }
  }

  /**
   * Helper class to keep track of and resolve imports.
   */
  public static class Dependency {
    private final URI url;
    private final int[] nesting;

    Dependency(URI url, int[] nesting) {
      this.url = url;
      this.nesting = nesting;
    }

    /** 
     * Returns the URL of the nested style sheet to load.
     */
    public URI getUrl() {
      return url;
    }

    /**
     * Returns the nesting positions of the style sheet to load.
     * Used for specificity calculation.
     */
    public int[] getNestingPositions() {
      return nesting;
    }
  }
}
