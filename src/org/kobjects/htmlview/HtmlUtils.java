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
import java.util.Arrays;
import java.util.HashMap;

import android.graphics.Paint;
import android.util.Log;
import android.util.SparseIntArray;

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

  private static class FastShortArray {
    short[] data = new short[128];
    FastShortArray() {
      Arrays.fill(data, (short)-1);
    }
    
    private int get(int i) {
      return i < data.length ? data [i] : -1;
    }
    
    private void put(int i, int v) {
      if (i >= data.length) {
        int newLen = data.length * 2;
        while (newLen <= i) {
          newLen *= 2;
        }
        short[] newData = new short[newLen];
        Arrays.fill(newData, data.length, newLen, (short) -1);
        System.arraycopy(data, 0, newData, 0, data.length);
        data = newData;
        Log.d("HtmlView", "allocated " + newData.length * 2);
      }
      data[i] = (short) v;
    }
  }
  
  private static FastShortArray widthCache = new FastShortArray();
  private static HashMap<Paint,FastShortArray> widthCacheMap = new HashMap<Paint,FastShortArray>();
  private static Paint lastFont;
  
  static int measureText(Paint font, String text, int start, int end) {
    if (font != lastFont) {
      widthCache = widthCacheMap.get(font);
      if (widthCache == null) {
        widthCache = new FastShortArray();
        widthCacheMap.put(font, widthCache);
      }
      lastFont = font;
    }
    
    int w = 0;
    for (int i = start; i < end; i++) {
      char c = text.charAt(i);
      int cw = widthCache.get(c);
      if (cw == -1) {
        cw = (int) font.measureText(text, i, i + 1);
        widthCache.put(c, cw);
      }
      w += cw;
    }
    return w;
  }
  
  
}
