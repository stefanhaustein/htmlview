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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;

import android.graphics.Paint;

/**
 * A set of simple static utility methods for CLDC.
 *  
 * @author Peter Baldwin
 * @author Stefan Haustein
 */
class HtmlUtils {
  static final String UTF8 = "UTF8";
 
  /**
   * Throws a runtime exception if i1 != i2.
   */
  static void assertEquals(int i1, int i2) {
    if(i1 != i2) {
      throw new RuntimeException("assert " + i1 + " == " + i2);
    }
  }

  /**
   * Returns true if a line break is valid between the characters c and d.
   */
  static boolean canBreak(char c, char d) {
    if (c <= ' ' || c == ')') {
      return true;
    } 
    if (d > ' ') {
      return "-.,/+(!?;".indexOf(c) != -1;
    }
    return false;
  }

  /**
   * Throws a runtime exception if b is false.
   */
  static void assertTrue(boolean b) {
    if(!b) {
      throw new RuntimeException("assertTrue: " + b);
    }
  }


  public static byte[] getUtf8Bytes(final String s) {
    try {
      return s.getBytes(UTF8);
    } catch(UnsupportedEncodingException e) {
      // UTF8 support is a requirement, this should be impossible.
      throw new RuntimeException(e);
    }
  }

  /**
   * Appends the given text to the given string buffer, normalizing whitespace
   * and trimming the string as specified.
   * 
   * @param pending the buffer to append to
   * @param text text string to be appended
   * @param ltrim remove whitespace before the first character
   */
  static void appendTrimmed(StringBuilder pending, String text, 
      boolean preserveLeadingSpace) {
  
    if (text == null) {
      return;
    }
  
    boolean wasSpace = !preserveLeadingSpace;
  
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
  
      if (c == '\r') {
        continue;
      }
  
      if (c <= ' ') {
        if (!wasSpace) {
          pending.append(' ');
          wasSpace = true;
        }
      } else {
        pending.append(c);
        wasSpace = false;
      }
    }
  }

  /**
   * Removes white space at the end of the given string buffer.
   */
  static void rTrim(StringBuilder buf) {
    if (buf.length() > 0 && buf.charAt(buf.length() - 1) == ' ') {
      buf.setLength(buf.length() - 1);
    }
  }

  /** 
   * We use a string builder as short array here. The cache is needed because measureText
   * is extremely slow on some devices.
   */
  private static StringBuilder widthCache;
  private static HashMap<Paint,StringBuilder> widthCacheMap = 
      new HashMap<Paint,StringBuilder>();
  private static Paint lastFont;
  
  static int measureText(Paint font, String text, int start, int end) {
    if (font != lastFont) {
      widthCache = widthCacheMap.get(font);
      if (widthCache == null) {
        widthCache = new StringBuilder();
        widthCacheMap.put(font, widthCache);
      }
      lastFont = font;
    }
    
    int w = 0;
    for (int i = start; i < end; i++) {
      char c = text.charAt(i);
      if (c >= widthCache.length()) {
        widthCache.setLength(c + 1);
      }
      int cw = widthCache.charAt(c) - 1;
      if (cw == -1) {
        cw = (int) font.measureText(text, i, i + 1);
        widthCache.setCharAt(c, (char) (cw + 1));
      }
      w += cw;
    }
    return w;
  }
  
  /** 
   * Calls uri.toString and normalizes file: URLs to start with three slashes if 
   * there is exactly one.
   */
  public static String toString(URI uri) {
    if (uri == null) {
      return null;
    }
    String s = uri.toString();
    return s.startsWith("file:/") && !s.startsWith("file://") && !s.startsWith("file:///") 
      ? "file:///" + s.substring(6) : s;
  }
}
