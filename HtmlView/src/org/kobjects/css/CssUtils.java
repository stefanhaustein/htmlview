// Copyright 2013 Google Inc.
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

public class CssUtils {

  public static int indexOfIgnoreCase(String[] array, String s) {
    int len = array.length;
    if (s == null) {
      for (int i = 0; i < len; i++) {
        if (array[i] == null) {
          return i;
        }
      } 
    } else {
      s = identifierToLowerCase(s);
      for (int i = 0; i < len; i++) {
        if (s.equals(array[i])) {
          return i;
        }
      }
    }
    return -1;
  }

  public static String identifierToLowerCase(String s) {
    int len = s.length();
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (c >= 'A' && c <= 'Z') {
        StringBuilder sb = new StringBuilder(s.substring(0, i));
        do {
          c = s.charAt(i++);
          if (c >= 'A' && c <= 'Z') {
            c += (char) ('a' - 'A');
          }
          sb.append(c);
        } while (i < len);
        return sb.toString();
      }
    }
    return s;
  }

  /**
   * Returns a string array created by splitting the given string at the given separator.
   */
  public static String[] split(String target, char separator) {
    int separatorInstances = 0;
    int targetLength = target.length();
    for (int index = target.indexOf(separator, 0);
    index != -1 && index < targetLength;
    index = target.indexOf(separator, index)) {
      separatorInstances++;
      // Skip over separators
      if (index >= 0) {
        index++;
      }
    }
    String[] results = new String[separatorInstances + 1];
    int beginIndex = 0;
    for (int i = 0; i < separatorInstances; i++) {
      int endIndex = target.indexOf(separator, beginIndex);
      results[i] = target.substring(beginIndex, endIndex);
      beginIndex = endIndex + 1;
    }
    // Last piece (or full string if there were no separators).
    results[separatorInstances] = target.substring(beginIndex);
    return results;
  }
  
}
