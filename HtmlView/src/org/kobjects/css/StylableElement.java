package org.kobjects.css;

import java.util.Iterator;

public interface StylableElement {

  String getAttributeValue(String name);

  String getName();

  // TODO: Remove here?
  boolean isLink();

  Iterator<? extends StylableElement> getChildElementIterator();
  
  boolean setComputedStyle(Style style);
}
