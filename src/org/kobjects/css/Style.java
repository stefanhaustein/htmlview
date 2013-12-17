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
import java.util.HashMap;

/**
 * Class for representing a set of CSS properties (e.g. a CSS style attribute).
 * 
 * @author Stefan Haustein
 */
public class Style {

  /**
   * CSS DPI constant.
   */
  public static final int DPI = 96;

  // TODO: Turn (at least) the unit into an enum? Property names may be tricky because
  // of the multi-value flags. But perhaps they do not need to be user-visible, and we
  // can use ord in the map / for array access. 
  // Not sure about turning the value array into a hashtable:
  // http://stackoverflow.com/questions/11565554/calculating-hashmap-overhead-in-java
  public static final int BACKGROUND_COLOR = 0;  // not inherited
  public static final int BORDER_COLLAPSE = 1;
  public static final int BORDER_SPACING = 2;
  public static final int CAPTION_SIDE = 3;
  public static final int COLOR = 4;
  public static final int DISPLAY = 5;          // not inherited
  public static final int EMPTY_CELLS = 6; 
  public static final int FONT_FAMILY = 7;
  public static final int FONT_SIZE = 8;
  public static final int FONT_STYLE = 9;
  public static final int FONT_VARIANT = 10;
  public static final int FONT_WEIGHT = 11;
  public static final int LINE_HEIGHT = 12;
  public static final int LIST_STYLE_POSITION = 13;
  public static final int LIST_STYLE_TYPE = 14;
  public static final int TEXT_ALIGN = 15;
  public static final int TEXT_DECORATION = 16;
  public static final int TEXT_INDENT = 17;
  public static final int TEXT_TRANSFORM = 18;
  public static final int VISIBILITY = 19;
  public static final int WHITE_SPACE = 20;

  // Note: TOP/RIGHT/BOTTOM/LEFT must be adjacent constants in ascending order
  public static final int BACKGROUND_IMAGE = 21;
  public static final int BACKGROUND_POSITION_X = 22; 
  public static final int BACKGROUND_POSITION_Y = 23;
  public static final int BACKGROUND_REPEAT = 24;
  
  public static final int BORDER_TOP_COLOR = 25;
  public static final int BORDER_RIGHT_COLOR = 26;
  public static final int BORDER_BOTTOM_COLOR = 27;
  public static final int BORDER_LEFT_COLOR = 28;
  public static final int BORDER_TOP_WIDTH = 29;
  public static final int BORDER_RIGHT_WIDTH = 30;
  public static final int BORDER_BOTTOM_WIDTH = 31;
  public static final int BORDER_LEFT_WIDTH = 32;
  public static final int BORDER_TOP_STYLE = 33;
  public static final int BORDER_RIGHT_STYLE = 34;
  public static final int BORDER_BOTTOM_STYLE = 35;
  public static final int BORDER_LEFT_STYLE = 36;
  public static final int BOTTOM = 37;
  public static final int CLEAR = 38;
  public static final int CLIP = 39;
  public static final int FLOAT = 40;
  public static final int HEIGHT = 41;
  public static final int LEFT = 42;
  public static final int MARGIN_TOP = 43;
  public static final int MARGIN_RIGHT = 44;
  public static final int MARGIN_BOTTOM = 45;
  public static final int MARGIN_LEFT = 46;
  public static final int OVERFLOW = 47;
  public static final int PADDING_TOP = 48;
  public static final int PADDING_RIGHT = 49;
  public static final int PADDING_BOTTOM = 50;
  public static final int PADDING_LEFT = 51;
  public static final int POSITION = 52;
  public static final int RIGHT = 53;
  public static final int TABLE_LAYOUT = 54;
  public static final int TOP = 55;
  public static final int VERTICAL_ALIGN = 56;
  public static final int WIDTH = 57;
  public static final int Z_INDEX = 58;
  
  public static final int BORDER_TOP_SPACING = 59;
  public static final int BORDER_RIGHT_SPACING = 60;
  public static final int BORDER_BOTTOM_SPACING = 61;
  public static final int BORDER_LEFT_SPACING = 62;

  // all inherited properties and the background color
  private static final int TEXT_PROPERTY_COUNT = 21;
  private static final int PROPERTY_COUNT = 63;

  /**
   * Flag for -top/-right/-bottom/-left abbreviation multiple value property
   * handling
   */
  public static final int MULTIVALUE_TRBL = 0x10000;
  private static final int MULTIVALUE_BORDER = 0x20000;

  private static final int MULTIVALUE_BACKGROUND = 0x1000;
  private static final int MULTIVALUE_BACKGROUND_POSITION = 0x1001;
  private static final int MULTIVALUE_FONT = 0x1002;
  private static final int MULTIVALUE_LIST_STYLE = 0x1003;

  static final int UNRECOGNIZED_PROPERTY_ID = 0x1234;
  static final Integer UNRECOGNIZED_PROPERTY = UNRECOGNIZED_PROPERTY_ID;

  // general values
  public static final int NONE = 0;
  public static final int AUTO = 1001;
  public static final int INHERIT = 1002;
  public static final int HIDDEN = 1003;
  public static final int INVALID = 1004;

  // font-weights
  public static final int NORMAL = 500000;
  public static final int BOLD = 700000;

  // various
  // note: top/left/right/bottom already defined as property names
  public static final int ABSOLUTE = 1100;
  public static final int BASELINE = 1101;
  public static final int BOTH = 1102;
  public static final int CENTER = 1103;
  public static final int FIXED = 1105;
  public static final int HIDE = 1106;
  public static final int INSIDE = 1107;
  public static final int JUSTIFY = 1108;
  public static final int MEDIUM = 1109;
  public static final int MIDDLE = 1110;
  public static final int NO_REPEAT = 1111;
  public static final int NOWRAP = 1112;
  public static final int OUTSIDE = 1113;
  public static final int PRE = 1114;
  public static final int PRE_WRAP = 1115;
  public static final int PRE_LINE = 1116;
  public static final int RELATIVE = 1117;
  public static final int REPEAT = 1118;
  public static final int REPEAT_X = 1119;
  public static final int REPEAT_Y = 1120;
  public static final int SCROLL = 1121;
  public static final int SHOW = 1124; 
  public static final int STATIC = 1125;
  public static final int TEXT_TOP = 1126;
  public static final int TEXT_BOTTOM = 1127;
  public static final int THICK = 1128;
  public static final int THIN = 1129;

  // display values
  public static final int INLINE = 1201;
  public static final int BLOCK = 1202;
  public static final int COLLAPSE = 1203;
  public static final int INLINE_BLOCK = 1204;
  public static final int LIST_ITEM = 1205;
  public static final int TABLE = 1206;
  public static final int INLINE_TABLE = 1207;
  public static final int TABLE_ROW = 1208;
  public static final int TABLE_ROW_GROUP = 1209;
  public static final int TABLE_HEADER_GROUP = 1210;
  public static final int TABLE_FOOTER_GROUP = 1211;
  public static final int TABLE_COLUMN = 1212;
  public static final int TABLE_COLUMN_GROUP = 1213;
  public static final int TABLE_CELL = 1214;
  public static final int TABLE_CAPTION = 1215;

  // list styles
  public static final int SQUARE = 1301;
  public static final int CIRCLE = 1302;
  public static final int DISC = 1303;
  public static final int DECIMAL = 1304;

  // border styles
  public static final int DOTTED = 1401;
  public static final int DASHED = 1402;
  public static final int SOLID = 1403;
  public static final int DOUBLE = 1404;
  public static final int GROOVE = 1405;
  public static final int RIDGE = 1406;
  public static final int INSET = 1407;
  public static final int OUTSET = 1408;

  // font stuff
  public static final int CAPITALIZE = 1500;
  public static final int ITALIC = 1501;
  public static final int OBLIQUE = 1502;
  public static final int SUB = 1503;
  public static final int SUPER = 1504;
  public static final int UNDERLINE = 1505;
  public static final int UPPERCASE = 1506;
  
  /**
   * Unit constant for numbers without unit.
   */
  public static final byte NUMBER = 0;

  /**
   * Unit constant for percent values.
   */
  public static final byte PERCENT = 1;

  /**
   * Unit constant for centimeters (cm).
   */
  public static final byte CM = 2;

  /**
   * Unit constant for the m-width typographic unit.
   */
  public static final byte EM = 3;

  /**
   * Unit constant for the lowercase letter height typographic unit (ex).
   */
  public static final byte EX = 4;

  /**
   * Unit constant for inches (in).
   */
  public static final byte IN = 5;

  /**
   * Unit constant for millimeters (cm).
   */
  public static final byte MM = 6;

  /**
   * Unit constant for pica (pc, 12pt).
   */
  public static final byte PC = 7;

  /**
   * Unit constant for point (pt, 1/72 in).
   */
  public static final byte PT = 8;

  /**
   * Unit constant for pixels.
   */
  public static final byte PX = 9;

  /**
   * Unit constant to mark enumerated values (e.g. LEFT/RIGHT/CENTER/JUSTIFY
   * etc.).
   */
  public static final byte ENUM = 16;

  /**
   * Unit constant for color values (Note: The COLOR constant denotes the CSS
   * property name "color").
   */
  public static final byte ARGB = 17;

  /** Unit constant for font-family and background-image */ 
  public static final byte STRING = 18;
  
  private static final Style EMPTY_STYLE = new Style();

  /**
   * Names of the units, the order must correspond to the unit constants
   * (NUMBER, PERCENT, CM, ...)
   */
  private static final String[] UNIT_NAMES = {
    "", "%", "cm", "em", "ex", "in", "mm", "pc", "pt", "px"};

  private static final HashMap<String, Integer> NAME_TO_ID_MAP;
  private static final HashMap<Integer, String> ID_TO_NAME_MAP;
  private static final HashMap<String, Long> VALUE_TO_ID_MAP;
  private static final HashMap<Integer, String> ID_TO_VALUE_MAP;

  /**
   * Bit field, keeping track of which fields have explicitly been set for this
   * Style object.
   */
  private long valuesSet;
  private int[] values = new int[TEXT_PROPERTY_COUNT];
  private byte[] units = new byte[TEXT_PROPERTY_COUNT];

  static final int[] TOP_LEVEL = new int[0];

  String backgroundImage;
  String fontFamily;

  /**
   * Specificity of this style, set by the corresponding selector in the
   * stylesheet parse method.
   */
  int specificity;

  /** 
   * Position of this style declaration inside the style sheet
   */
  int position;

  /**
   * Nested import positions
   */
  int[] nesting = TOP_LEVEL;


  private int firstSet = PROPERTY_COUNT; 
  private int lastSet;

  private static final String[] TRBL = {
    "-top", "-right", "-bottom", "-left"
  };

  static {
    NAME_TO_ID_MAP = new HashMap<String, Integer>();
    ID_TO_NAME_MAP = new HashMap<Integer, String>();
    VALUE_TO_ID_MAP = new HashMap<String, Long>();
    ID_TO_VALUE_MAP = new HashMap<Integer, String>();

    addName("background", MULTIVALUE_BACKGROUND);
    addName("background-color", BACKGROUND_COLOR);
    addName("background-position", MULTIVALUE_BACKGROUND_POSITION);
    addName("background-repeat", BACKGROUND_REPEAT);
    addName("border", MULTIVALUE_BORDER | MULTIVALUE_TRBL);
    addName("border-collapse", BORDER_COLLAPSE);
    addName("border-color", MULTIVALUE_TRBL | BORDER_TOP_COLOR);
    addName("border-style", MULTIVALUE_TRBL | BORDER_TOP_STYLE);
    addName("border-width", MULTIVALUE_TRBL | BORDER_TOP_WIDTH);
    addName("border-spacing", MULTIVALUE_TRBL | BORDER_TOP_SPACING);
    addName("bottom", BOTTOM);
    addName("caption-side", CAPTION_SIDE);
    addName("clear", CLEAR);
    addName("clip", CLIP);
    addName("color", COLOR);
    addName("display", DISPLAY);
    addName("empty-cells", EMPTY_CELLS);
    addName("float", FLOAT);
    addName("font", MULTIVALUE_FONT);
    addName("font-weight", FONT_WEIGHT);
    addName("font-size", FONT_SIZE);
    addName("font-style", FONT_STYLE);
    addName("font-variant", FONT_VARIANT);
    
    addName("height", HEIGHT);
    addName("left", LEFT);
    addName("line-height", LINE_HEIGHT);
    addName("list-style", MULTIVALUE_LIST_STYLE);
    addName("list-style-postion", LIST_STYLE_POSITION);
    addName("list-style-type", LIST_STYLE_TYPE);
    addName("margin", MULTIVALUE_TRBL | MARGIN_TOP);
    addName("overflow", OVERFLOW);
    addName("padding", MULTIVALUE_TRBL | PADDING_TOP);
    addName("position", POSITION);
    addName("right", RIGHT);
    addName("table-layout", TABLE_LAYOUT);
    addName("text-align", TEXT_ALIGN);
    addName("text-decoration", TEXT_DECORATION);
    addName("text-indent", TEXT_INDENT);
    addName("text-transform", TEXT_TRANSFORM);
    addName("top", TOP);
    addName("vertical-align", VERTICAL_ALIGN);
    addName("visibility", VISIBILITY);
    addName("white-space", WHITE_SPACE);
    addName("width", WIDTH);
    addName("z-index", Z_INDEX);

    // special-cased 
    addName("background-image", BACKGROUND_IMAGE);
    addName("font-family", FONT_FAMILY);

    for (int i = 0; i < 4; i++) {
      addName("border" + TRBL[i] + "-color", BORDER_TOP_COLOR + i);
      addName("border" + TRBL[i] + "-style", BORDER_TOP_STYLE + i);
      addName("border" + TRBL[i] + "-width", BORDER_TOP_WIDTH + i);
      addName("border" + TRBL[i] + "-spacing", BORDER_TOP_SPACING + i);
      addName("border" + TRBL[i], MULTIVALUE_BORDER | i);
      addName("margin" + TRBL[i], MARGIN_TOP + i);
      addName("padding" + TRBL[i], PADDING_TOP + i);
    }
    
    addValue("auto", AUTO, ENUM);
    addValue("none", NONE, ENUM);
    addValue("hidden", HIDDEN, ENUM);
    addValue("inherit", INHERIT, ENUM);

    addValue("lighter", NORMAL, NUMBER);
    addValue("normal", NORMAL, NUMBER);
    addValue("bold", BOLD, NUMBER);
    addValue("bolder", BOLD, NUMBER);

    addValue("inline", INLINE, ENUM);
    addValue("inline-block", INLINE_BLOCK, ENUM);
    addValue("italic", ITALIC, ENUM);
    addValue("block", BLOCK, ENUM);
    addValue("list-item", LIST_ITEM, ENUM);
    addValue("table", TABLE, ENUM);
    addValue("inline-table", INLINE_TABLE, ENUM);
    addValue("table-row", TABLE_ROW, ENUM);
    addValue("table-tow-group", TABLE_ROW_GROUP, ENUM);
    addValue("table-header-group", TABLE_HEADER_GROUP, ENUM);
    addValue("table-footer-group", TABLE_FOOTER_GROUP, ENUM);
    addValue("table-column", TABLE_COLUMN, ENUM);
    addValue("table-column-group", TABLE_COLUMN_GROUP, ENUM);
    addValue("table-cell", TABLE_CELL, ENUM);
    addValue("table-caption", TABLE_CAPTION, ENUM);
    addValue("oblique", OBLIQUE, ENUM);

    // various
    // note: top/left/right/bottom already defined as property names
    addValue("absolute", ABSOLUTE, ENUM);
    addValue("baseline", BASELINE, ENUM);
    addValue("both", BOTH, ENUM);
    addValue("bottom", BOTTOM, ENUM);
    addValue("center", CENTER, ENUM);
    addValue("capitalize", CAPITALIZE, ENUM);
    addValue("fixed", FIXED, ENUM);
    addValue("hide", HIDE, ENUM);
    addValue("inside", INSIDE, ENUM);
    addValue("justify", JUSTIFY, ENUM);
    addValue("medium", MEDIUM, ENUM);
    addValue("middle", MIDDLE, ENUM);
    addValue("no-repeat", NO_REPEAT, ENUM);
    addValue("nowrap", NOWRAP, ENUM);
    addValue("outside", OUTSIDE, ENUM);
    addValue("pre", PRE, ENUM);
    addValue("pre-wrap", PRE_WRAP, ENUM);
    addValue("pre-line", PRE_LINE, ENUM);
    addValue("relative", RELATIVE, ENUM);
    addValue("scroll", SCROLL, ENUM);
    addValue("sub", SUB, ENUM);
    addValue("super", SUPER, ENUM);
    addValue("show", SHOW, ENUM);
    addValue("static", STATIC, ENUM);
    addValue("top", TOP, ENUM);
    addValue("text-top", TEXT_TOP, ENUM);
    addValue("text-bottom", TEXT_BOTTOM, ENUM);
    addValue("thick", THICK, ENUM);
    addValue("thin", THIN, ENUM);
    addValue("underline", UNDERLINE, ENUM);
    addValue("uppercase", UPPERCASE, ENUM);
    addValue("repeat", REPEAT, ENUM);
    addValue("repeat-x", REPEAT_X, ENUM);
    addValue("repeat-y", REPEAT_Y, ENUM);

    addValue("square", SQUARE, ENUM);
    addValue("circle", CIRCLE, ENUM);
    addValue("disc", DISC, ENUM);
    addValue("decimal", DECIMAL, ENUM);

    addValue("dotted", DOTTED, ENUM);
    addValue("dashed", DASHED, ENUM);
    addValue("solid", SOLID, ENUM);
    addValue("double", DOUBLE, ENUM);
    addValue("groove", GROOVE, ENUM);
    addValue("rdige", RIDGE, ENUM);
    addValue("inset", INSET, ENUM);
    addValue("outset", OUTSET, ENUM);

    addValue("left", LEFT, ENUM);
    addValue("right", RIGHT, ENUM);

    addValue("transparent", 0, ARGB);    

    addValue("aqua", 0xff00ffff, ARGB); 
    addValue("black", 0xff000000, ARGB);
    addValue("blue", 0xff0000ff, ARGB); 
    addValue("fuchsia", 0xffff00ff, ARGB); 
    addValue("gray", 0xff808080, ARGB);
    addValue("green", 0xff008000, ARGB);
    addValue("lime", 0xff00ff00, ARGB);
    addValue("maroon",  0xff800000, ARGB); 
    addValue("navy", 0xff000080, ARGB);
    addValue("olive", 0xff808000, ARGB);
    addValue("orange", 0xffffA500, ARGB);
    addValue("purple", 0xff800080, ARGB);
    addValue("red", 0x0ffff0000, ARGB);
    addValue("silver", 0xffc0c0c0, ARGB);
    addValue("white", 0xffffffff, ARGB);
    addValue("yellow", 0xffffff00, ARGB);
    addValue("teal", 0xff008080, ARGB);
    
    // Unofficial 
    addValue("pink", 0x0ffffc0cb, ARGB);
    addValue("grey", 0xff808080, ARGB);
  }

  private static void addName(String name, int id) {
    NAME_TO_ID_MAP.put(name, id);
    ID_TO_NAME_MAP.put(id, name);
  }

  private static void addValue(String value, int id, byte unit) {
    VALUE_TO_ID_MAP.put(value, (((long) unit) << 32) | (id & 0x0ffffffffL));
    if (unit == ENUM) {
      ID_TO_VALUE_MAP.put(id, value);
    }
  }

  /**
   * Creates a style object with the color propery black, normal font weight and
   * display type "inline"; all other values are 0.
   */
  public Style() {
  }

  /**
   * Inherit values from the given style. Needs to be called before a style can
   * be used in order to get rid of INHERIT values and percentages for font-size and
   * line-height.
   * 
   * @param from the style to inherit from
   */
  public void inherit(Style from) {
    if (from == null) {
      from = EMPTY_STYLE;
    }

    int max = Math.max(lastSet, from.lastSet);

    for (int id = Math.min(firstSet, from.firstSet); id <= max; id++) {
      if (isSet(id)) {  // Explicit inherit
        if (units[id] == ENUM) {
          if (values[id] == INHERIT) {
            if (id == BACKGROUND_IMAGE) {
              backgroundImage = from.backgroundImage;
            } else if (id == FONT_FAMILY) {
              fontFamily = from.fontFamily;
            }
            set(id, from.values[id], from.units[id]);
          } else if (values[id] == PERCENT) {
            if (id == FONT_SIZE) {
              set(id, from.values[id] * values[id] / 100000, from.units[id]);
            } else if (id == LINE_HEIGHT) {
              set(id, values[FONT_SIZE] * values[id] / 100000, units[FONT_SIZE]);
            }
          }
        }
      } else if (id < TEXT_PROPERTY_COUNT && id != BACKGROUND_COLOR && 
          id != DISPLAY && from.isSet(id)) {
        if (id == FONT_FAMILY) {
          fontFamily = from.fontFamily;
        }
        set(id, from.values[id], from.units[id]);
      }
    }
  }

  /**
   * Copies all style information set in from to this
   * 
   * @param from the style to copy from
   */
  public void set(Style from) {
    if (from == null) {
      return;
    }
    for (int id = from.firstSet; id <= from.lastSet; id++) {
      if (from.isSet(id)) {
        set(id, from.values[id], from.units[id]);
      }
    }
    if (from.backgroundImage != null) {
      backgroundImage = from.backgroundImage;
    }
    if (from.fontFamily != null) {
      fontFamily = from.fontFamily;
    }
  }

  /**
   * Returns true if the given property is set to a value.
   */
  public boolean isSet(int id) {
    return (valuesSet & (1L << id)) != 0;
  }

  /**
   * Set the given property to the given value and unit.
   * 
   * @param id CSS property id constant
   * @param value the value to set (multiply lengths with 1000)
   * @param unit the unit (NUMBER, PERCENT, CM, EM, EX, IN, MM, PC, PT, PX,
   *            ARGB)
   * @return this style
   */
  public Style set(int id, int value, byte unit) {
    if (id != UNRECOGNIZED_PROPERTY_ID) {
      if (id >= values.length) {
        int[] newValues = new int[PROPERTY_COUNT];
        byte[] newUnits = new byte[PROPERTY_COUNT];
        System.arraycopy(values, 0, newValues, 0, values.length);
        System.arraycopy(units, 0, newUnits, 0, units.length);
        values = newValues;
        units = newUnits;
      }

      values[id] = value;
      units[id] = unit;
      valuesSet |= 1L << id;
      firstSet = Math.min(firstSet, id);
      lastSet = Math.max(lastSet, id);
    }
    return this;
  }

  /**
   * Internal version of set that takes the value position into account
   * for multi-value properties (border, font, margin etc.)
   * 
   * @param id property id
   * @param value property value
   * @param unit unit constant
   * @param pos position of this value
   * @return this
   */
  public Style set(int id, int value, byte unit, int pos) {
    if ((id & MULTIVALUE_BORDER) != 0) {
      id -= MULTIVALUE_BORDER;
      switch (unit) {
        case ARGB:
          id += BORDER_TOP_COLOR;
          break;

        case ENUM:
          switch(value) {
            case MEDIUM:
            case THIN:
            case THICK:
              id += BORDER_TOP_WIDTH;
              break;
            default:
              id += BORDER_TOP_STYLE;
          }
          break;

        default:
          id += BORDER_TOP_WIDTH;
      }
      set(id, value, unit, 0);
    } else if ((id & MULTIVALUE_TRBL) != 0) {
      id -= MULTIVALUE_TRBL;
      switch (pos) {
        case 0:
          set(id, value, unit);
          set(id + 1, value, unit);
          set(id + 2, value, unit);
          set(id + 3, value, unit);
          break;
        case 1:
          set(id + 1, value, unit);
          set(id + 3, value, unit);
          break;
        case 2:
          set(id + 2, value, unit);
          break;
        case 3:
          set(id + 3, value, unit);
          break;
      } 
    } else {
      switch (id) {
        case MULTIVALUE_FONT: 
          if (unit == NUMBER) {
            set(FONT_WEIGHT, value, unit);
          }
          break;
        case MULTIVALUE_BACKGROUND:
          if (unit == ENUM && value == INHERIT && pos == 0) {
            set(BACKGROUND_COLOR, INHERIT, ENUM);
            set(BACKGROUND_REPEAT, INHERIT, ENUM);
            set(BACKGROUND_POSITION_X, INHERIT, ENUM);
            set(BACKGROUND_POSITION_Y, INHERIT, ENUM);
          } else if (unit == ARGB) {
            set(BACKGROUND_COLOR, value, ARGB);
          } else if (unit == ENUM && (value == NO_REPEAT || value == REPEAT ||
              value == REPEAT_X || value == REPEAT_Y)) {
            set(BACKGROUND_REPEAT, value, ENUM);
          } else if (unit == ENUM && (value == SCROLL || value == FIXED)){
            // ignore attachment
          } else if (!isSet(BACKGROUND_POSITION_X)) {
            set(BACKGROUND_POSITION_X, value, unit);
            set(BACKGROUND_POSITION_Y, value, unit);
          } else {
            set(BACKGROUND_POSITION_Y, value, unit);
          }        
          break;
        case MULTIVALUE_BACKGROUND_POSITION: 
          if (pos == 0) {
            set(BACKGROUND_POSITION_X, value, unit);
          } 
          if (pos == 0 || pos == 1) {
            set(BACKGROUND_POSITION_Y, value, unit);
          }
          break;
        case MULTIVALUE_LIST_STYLE:
          if (pos == 0 && unit == ENUM && value == INHERIT) {
            set(LIST_STYLE_POSITION, INHERIT, ENUM);
            set(LIST_STYLE_TYPE, INHERIT, ENUM);
          } else if (unit == ENUM && (value == INSIDE || value == OUTSIDE)) {
            set(LIST_STYLE_POSITION, value, ENUM);
          } else {
            set(LIST_STYLE_TYPE, value, unit);
          }
          break;

        default:
          set(id, value, unit);
      }
    }
    return this;
  }

  /**
   * Determines whether a length has a fixed value.
   */
  public boolean isLengthFixed(int id) {
    switch(getUnit(id)) {
      case NUMBER:
      case CM:
      case EM:
      case EX:
      case IN:
      case MM:
      case PC:
      case PT:
      case PX:
        return true;
    }
    return false;
  }

  public boolean isLengthFixedOrPercent(int id) {
    return getUnit(id) == PERCENT || isLengthFixed(id);
  }
  
  /**
   * Returns the pixel value as a rounded integer, i.e. not in the internal
   * fixpoint format. Other units are converted to pixel automatically. Percent
   * values are converted to 0. For properties permitting percent values, please
   * use getPx(int id, int base).
   */
  public int getPx(int id) {
    if (id >= BORDER_TOP_WIDTH && id <= BORDER_LEFT_WIDTH) {
      if (getRaw(id - BORDER_TOP_WIDTH + BORDER_TOP_STYLE) == 0) {
        return 0;
      }

      if (getUnit(id) == ENUM){
        switch(getRaw(id)) {
          case THIN: 
            return 1;
          case THICK:
            return 3;
          default:
            return 2;
        }
      }
    }

    int v = getRaw(id);
    // none, 0


    if (v < 0) {
      switch(id) {
        case BORDER_TOP_WIDTH:
        case BORDER_RIGHT_WIDTH:
        case BORDER_BOTTOM_WIDTH:
        case BORDER_LEFT_WIDTH:
          return 0;
      }
    }

    if (v == 0) {
      return 0;
    }

    switch (getUnit(id)) {
     // case PX:
    //return v / 1000;
      case EM:
        // TODO(haustein) Increase precision?
        return v * getPx(FONT_SIZE) / 1000;
      case EX:
        return v * getPx(FONT_SIZE) / 2000;
      case IN:
        return (v * DPI) / 1000;
      case CM:
        return (v * DPI * 100 / 254) / 1000;
      case MM:
        return (v * DPI * 10 / 254) / 1000;
      case PT:
        return (v * DPI / 72) / 1000;
      case PC:
        return (v * DPI / 6) / 1000;
      default: // PX, NONE
        return  v / 1000;
    }
  }

  /**
   * Returns the value of the property if it is an ENUM, INVALID otherwise.
   */
  public int getEnum(int id) {
    return getUnit(id) == ENUM ? getRaw(id) : INVALID;
  }

  /**
   * Return the ARGB color value if the property contains a color value; 0 otherwise.
   */
  public int getColor(int id) {
    return getUnit(id) == ARGB ? getRaw(id) : 0;
  }

  public int getBackgroundReferencePoint(int id, int containerLength, int imageLength) {
    int percent = 0;
    switch(getUnit(id)) {
      case ENUM:
        switch(getRaw(id)) {
          case TOP:
          case LEFT:
            return 0;
          case CENTER:
            percent = 50;
            break;
          case RIGHT:
          case BOTTOM:
            percent = 100;
            break;
          default:
            return 0;
        }
        break;
      case PERCENT:
        percent = getRaw(id) / 1000;
        break;
      default:
        return getPx(id);
    }
    return (containerLength - imageLength) * percent / 100;    
  }

  /**
   * Returns the pixel value as a rounded integer, i.e. not in the internal
   * fixpoint format. Other units are converted to pixel automatically. Percent
   * values are multiplied with the base value. Note: Automatic margins  
   * are implemented by shifting the child component to the right in order to
   * keep the implementation of getPx() and BlockWidget.preDraw() simple.
   */
  public int getPx(int id, int base) {
    return isSet(id) && units[id] == PERCENT ? base * values[id] / 100000 : getPx(id);
  }

  /**
   * Returns the raw integer value for the given property value. For lengths,
   * please use getPx() to obtain a pixel integer value. Lengths are stored
   * internally multiplied by 1000. This method does all the default handling
   * (that is not handled in the default style sheet).
   * 
   * @param id property id
   * @return raw propery value
   */
  public int getRaw(int id) {
    //    if (id >= units.length || (units[id] == PERCENT && id == HEIGHT)) {
    //      return id == WIDTH || id == HEIGHT ? AUTO : 0;
    //    }
    if (isSet(id)) { 
      if (id == HEIGHT && units[id] == PERCENT) {
        return AUTO;
      }
      return values[id];
    }
    switch(id) {
      case BORDER_TOP_WIDTH:
      case BORDER_BOTTOM_WIDTH:
      case BORDER_LEFT_WIDTH:
      case BORDER_RIGHT_WIDTH:
        return MEDIUM;
      case BOTTOM:
      case HEIGHT:
      case LEFT:
      case RIGHT:
      case TABLE_LAYOUT:
      case TOP:
      case WIDTH: 
        return AUTO;
      case COLOR:
        return 0x0ff000000;
      case DISPLAY:
        return INLINE;
      case FONT_SIZE:
        return 12000;  // 12pt
      case FONT_WEIGHT:
        return 400000;
      case LINE_HEIGHT: 
        return 100000;
      case LIST_STYLE_TYPE:
        return DISC;
      case POSITION:
        return STATIC;
      case BACKGROUND_REPEAT: 
        return REPEAT;
    }
    return 0;
  }

  public String getString(int id) {
    if (!isSet(id)) {
      return null;
    }
    if (id == BACKGROUND_IMAGE) {
      return backgroundImage;
    } else if (id == FONT_FAMILY) {
      return fontFamily;
    }
    int v = values[id];
    byte unit = units[id];
    switch (unit) {
    case ARGB:
      return '#' + Integer.toString((v & 0x0ffffff) | 0x01000000, 16).substring(1);
    case ENUM:
      return ID_TO_VALUE_MAP.get(v);
    default:
      StringBuilder buf = new StringBuilder();
      if (v % 1000 == 0) {
        buf.append(v / 1000);
      } else {
        buf.append(v);
        while (buf.length() < 4) {
          buf.insert(0, '0');
        }
        buf.insert(buf.length() - 3, '.');
      }
      if (unit >= 0 && unit < UNIT_NAMES.length) {
        buf.append(UNIT_NAMES[unit]);
      }
      return buf.toString();
    }
  }

  /**
   * Returns the unit for the given property.
   * 
   * @param id
   * @return One of the lenght unit constants (PX etc), PERCENT, NUMBER or ENUM.
   */
  public int getUnit(int id) {
    if (isSet(id)) {
      return units[id];
    } 

    switch(id) {
      case BACKGROUND_POSITION_X:
      case BACKGROUND_POSITION_Y:
      case LINE_HEIGHT:
        return PERCENT;
      case BACKGROUND_COLOR:
      case COLOR:
        return ARGB;
      case FONT_WEIGHT:
        return NUMBER;
      case FONT_SIZE:
        return PT;
    }

    // Note: ENUM is fine for number types since 0 is returned for getPx()
    return ENUM;
  }

  /**
   * Reads a style declaration from a string.
   */
  public void read(URI url, String def) {
    CssTokenizer ct = new CssTokenizer(url, def);
    read(ct);
  }

  /**
   * Reads a style declaration from a CSS tokenizer.
   */
  void read(CssTokenizer tokenizer) {
    while (tokenizer.ttype != CssTokenizer.TT_EOF && tokenizer.ttype != '}') {
      if (tokenizer.ttype == CssTokenizer.TT_IDENT) {
        String name = tokenizer.sval;
        Integer idObj = (Integer) NAME_TO_ID_MAP.get(name);
        int id;
        if (idObj == null) {
          tokenizer.debug("unrecognized property");
          id = UNRECOGNIZED_PROPERTY_ID;
        } else {
          id = idObj.intValue();
        }
        tokenizer.nextToken(false);
        if (tokenizer.ttype != ':') {
          continue;
        }
        tokenizer.nextToken(false);

        int pos = 0;
        loop : while (true) {
          switch (tokenizer.ttype) {
          case CssTokenizer.TT_HASH:
            setColor(id, '#' + tokenizer.sval, pos);
            break;

          case CssTokenizer.TT_DIMENSION:
            set(id, tokenizer.nval, 
                (byte) CssUtils.indexOfIgnoreCase(UNIT_NAMES, tokenizer.sval), pos);
            break;

          case CssTokenizer.TT_NUMBER:
            set(id, tokenizer.nval, NUMBER, pos);
            break;

          case CssTokenizer.TT_PERCENTAGE:
            set(id, tokenizer.nval, PERCENT, pos);
            break;

          case CssTokenizer.TT_IDENT:
            Long v = (Long) VALUE_TO_ID_MAP.get(tokenizer.sval);
            if (v != null) {
              set(id, (int) v.longValue(),
                  (byte) (v.longValue() >>> 32), pos);
            } else if (id == MULTIVALUE_FONT || id == FONT_FAMILY) {
              fontFamily = (fontFamily == null ? "" : fontFamily) + tokenizer.sval;
              set(id, 0, STRING);
            } else {
              tokenizer.debug("Unrecognized value '" + v + "' for property " + name);
            }
            break;

          case CssTokenizer.TT_URI:
            if (id == MULTIVALUE_BACKGROUND || id == BACKGROUND_IMAGE) {
              backgroundImage = tokenizer.sval;
              set(id, 0, STRING);
            }
            break;

          case ',':
          case CssTokenizer.TT_STRING:
            if (id == MULTIVALUE_FONT || id == FONT_FAMILY) {
              fontFamily = (fontFamily == null ? "" : fontFamily) + tokenizer.sval;
              set(id, 0, STRING);
            }
            break;

          default:
            break loop;
          }
          pos++;
          tokenizer.nextToken(false);
        }
      }

      // handle !important
      if (tokenizer.ttype == '!') {
        tokenizer.nextToken(false);
        if (tokenizer.ttype == CssTokenizer.TT_IDENT && 
            "important".equals(tokenizer.sval)) {
          specificity = StyleSheet.SPECIFICITY_IMPORTANT;
          tokenizer.nextToken(false);
        }
      }

      // skip trailing trash
      while (tokenizer.ttype != CssTokenizer.TT_EOF 
          && tokenizer.ttype != ';' && tokenizer.ttype != '}') {
        tokenizer.debug("skipping");
        tokenizer.nextToken(false);
      }
      while (tokenizer.ttype == ';') {
        tokenizer.nextToken(false);
      }
    }
  }

  /**
   * Set a CSS color value.
   * 
   * @param id field id (COLOR, ...)
   * @param color the color in the form #RGB or #RRGGBB or one of the 16 CSS
   *              color identifiers
   * @param pos index of the border-color value (0..3); 0 otherwise
   */
  public void setColor(int id, String color, int pos) {
    if (color.length() > 0 && color.charAt(0) == '#') {
      try {
        // #RGB or #RRGGBB hexadecimal color value
        int value = Integer.parseInt(color.substring(1), 16);
        if (color.length() == 4) {
          value = (value & 0x00f) | ((value & 0x0ff) << 4)
          | ((value & 0xff0) << 8) | ((value & 0xf00) << 12);
        }
        // set with transparency opaque
        set(id, 0x0ff000000 | value, ARGB, pos);
      } catch (NumberFormatException e) {
        // ignore invalid colors
      }
    } else {
      Long v = (Long) VALUE_TO_ID_MAP.get(CssUtils.identifierToLowerCase(color));
      if (v != null) {
        long l = v.longValue();
        if ((l >>> 32) == ARGB) {
          set(id, (int) l, ARGB, pos);
        }
      }
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    toString("", sb);
    return sb.toString();
  }

  public void toString(String indent, StringBuilder sb) {
    for (int id = 0; id < PROPERTY_COUNT; id++) {
      if (isSet(id)) {
        sb.append(indent).append(ID_TO_NAME_MAP.get(id)).append(": ");
        sb.append(getString(id));
        sb.append(";\n");
      }
    }
    sb.append("/* specifity: " + specificity + " */\n");
  }

  /**
   * Compares the specificity of this style to s2 and returns the difference.
   */
  public int compareSpecificity(Style s2) {
    if (specificity > s2.specificity) {
      return 1;
    } else if (specificity < s2.specificity) {
      return -1;
    } else {
      int min = Math.min(nesting.length, s2.nesting.length); 
      for (int i = 0; i < min; i++) {
        if (nesting[i] > s2.nesting[i]) {
          return 1;
        } else if (nesting[i] < s2.nesting[i]) {
          return -1;
        }
      }
      int n1 = min + 1 < nesting.length ? nesting[min + 1] : position;
      int n2 = min + 1 < s2.nesting.length ? s2.nesting[min + 1] : s2.position;
      return n1 - n2;
    }
  }

  /**
   * Returns true if this is a (inline)block style. 
   * 
   * @param full if false, true is returned for inline blocks, too.
   */
  public boolean isBlock(boolean full) {
    int display = getEnum(DISPLAY);
    if (display == BLOCK || display == TABLE || display == LIST_ITEM || getEnum(POSITION) == ABSOLUTE) {
      return true;
    }
    if (full) {
      return false;
    }
    return getEnum(DISPLAY) != INLINE;
        /*
        backgroundImage != null || getEnum(DISPLAY) != INLINE  ||
        getPx(BORDER_BOTTOM_WIDTH) != 0 || getPx(BORDER_LEFT_WIDTH) != 0 ||
        getPx(BORDER_RIGHT_WIDTH) != 0 || getPx(BORDER_TOP_WIDTH) != 0 ||
        getEnum(POSITION) == ABSOLUTE ||
        lengthIsFixed(Style.HEIGHT, true) || lengthIsFixed(Style.WIDTH, true); */
  }
}
