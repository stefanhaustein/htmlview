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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.kobjects.css.StyleSheet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;


/**
 * Widget class representing a HTML document (or snippet). First builds up a
 * "logical" DOM consisting of Element objects for the (X)HTML code using KXML,
 * then constructs a physical View representation, consisting of BlockWidget,
 * InputWidget, TableWidget and TextFragmentWidget.
 * 
 * Support for input elements and CSS can be switched off using ConfigSettings
 * (HTML_DISABLE_INPUT and HTML_DISABLE_CSS) to reduce the memory footprint of
 * this component.
 * 
 * @author Stefan Haustein
 */
public class HtmlView extends BlockElementView  {

  public enum Onload {
    ADD_STYLE_SHEET,
    ADD_IMAGE,
    SHOW_HTML
  }

  /**
   * Interleaved map of character entity reference names to their resolved 
   * string values. 
   * 
   * TODO(haustein) improve this in KXML instead.
   */
  private static final String[] HTML_ENTITY_TABLE = {
    "acute", "\u00B4",
    "apos", "\u0027",
    "Auml", "\u00C4", "auml", "\u00E4", 
    "nbsp", "\u00a0",
    "Ouml", "\u00D6", "ouml", "\u00F6", 
    "szlig", "\u00DF",
    "Uuml", "\u00DC", "uuml", "\u00FC", 
  };

  public static final String[] MEDIA_TYPES = {"all", "screen", "mobile"};

  /** CSS style sheet for this document. */
  StyleSheet styleSheet = StyleSheet.createDefault();

  private HashMap<URI, Bitmap> images = new HashMap<URI, Bitmap>();

  /** Maps URLs to information to the sources of a request. */
  private HashMap<URI, ArrayList<ImageRequest>> pendingImageRequests = 
      new HashMap<URI, ArrayList<ImageRequest>>();

  private HashMap<URI, String> styleSheets = new HashMap<URI, String>();
  private HashMap<URI, int[]> pendingStyleSheetRequests = new HashMap<URI, int[]>();
  private HashSet<URI> appliedStyleSheets = new HashSet<URI>();

  
  /** Request handler used by this widget to request resources. */
  protected RequestHandler requestHandler = new DefaultRequestHandler();

  /** URL of the page represented by this widget. */
  private URI documentUrl;

  /** Base URL for this document */
  URI baseURL;

  /** Maps labels to view */
  HashMap<String, View> labels = new HashMap<String, View>();

  /** Map for access keys */
  HashMap<Character, Element> accesskeys = new HashMap<Character, Element>();

  /** The title of the document. Initialized to "---" to avoid null pointers. */
  String title = "---";

  /** 
   * We need to keep a reference to the focused element since we may need to rebuild
   * the widgets.
   */
  Element focusedElement;

  /** True if the widget tree needs to be rebuilt. */
  boolean needsBuild = true;
  
  /**
   * Creates a HTML document widget.
   * 
   * @param requestHandler the object used for requesting resources (embedded images, links)
   * @param documentUrl document URL, used as base URL for resolving relative links
   */
  public HtmlView(Context context) {
    super(context, null, false);
    try {
      this.baseURL = this.documentUrl = new URI("");
    } catch(Exception e) {
      throw new RuntimeException();
    }
  }
  
  public void loadUrl(String url) {
    try {
      loadAsync(new URI (url), Onload.SHOW_HTML);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  
  public void loadAsync(final URI uri, final Onload onload) {
    new AsyncTask<Void, String, Boolean>() {
      String encoding;
      byte[] rawData;
      Bitmap image;
      Toast toast;
      int toastTextLength;
      
      @Override
      protected Boolean doInBackground(Void... params) {
        try {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          publishProgress("Connecting...");
          URLConnection con = uri.toURL().openConnection();
          con.setRequestProperty("UserAgent", "AndroidHtmlView/1.0 (Mobile)");
          InputStream is = con.getInputStream();
          publishProgress("Starting transfer...");
          byte[] buf = new byte[8096];
          String contentType = con.getContentType();
          encoding = "ISO-8859-1";  // As per HTTP spec.
          if (contentType != null) {
            int cut = contentType.indexOf("charset=");
            if (cut != -1) {
              encoding = contentType.substring(cut + 8).trim();
            }
          }
          Log.d("HtmlView", "encoding: " + encoding);
          int contentLength = con.getContentLength();
          while (true) {
            int count = is.read(buf);
            if (count <= 0) {
              break;
            }
            baos.write(buf, 0, count);
            if (contentLength <= 0) {
              publishProgress("Transferred " + baos.size() + " bytes");
            } else {
              publishProgress("Transferred " + (baos.size() * 100 / contentLength) + "%");
            }
          }
          is.close();
          publishProgress("Transfer complete.");
          rawData = baos.toByteArray();
          baos = null;
          switch(onload) {
          case ADD_IMAGE:
            image = BitmapFactory.decodeByteArray(rawData, 0, rawData.length);
            rawData = null;
            break;
          }
          return true;
        } catch (Exception e) {
          Log.e("HtmlView", "Exception while requesting " + uri, e);
          encoding = null;
          return false;
        }
      }
      @Override
      protected void onPostExecute(Boolean success) {
        if (toast != null) {
          toast.cancel();
        }
        if (success != Boolean.TRUE) {
          return;
        }
        switch(onload) {
        case SHOW_HTML:
          setDocumentUrl(uri);
          loadData(rawData, encoding);
          break;
        case ADD_IMAGE:
          addImage(uri, image);
          break;
        }
      }
      @Override 
      protected void onProgressUpdate(String... s) {
        if (onload == Onload.SHOW_HTML) {
          if (toast != null) {
            // Prevent jumping around... :-/
            if (toastTextLength != s[0].length()) {
              toast.cancel();
              toast = null;
            } else {
              toast.setText(s[0]);
            }
          }
          if (toast == null) {
            toast = Toast.makeText(getContext(), s[0], Toast.LENGTH_LONG);
          }
          toast.show();
        }
      }
    }.execute((Void[]) null);
  }
  
  
  public void setDocumentUrl(URI url) {
    documentUrl = baseURL = url;
  }
  
  
  public void onMeasure(int widthSpec, int heightSpec) {
    Log.d("HtmlView", "onMeasure w:" + View.MeasureSpec.toString(widthSpec) + " h: " + View.MeasureSpec.toString(heightSpec));
    
    int viewportWidth = View.MeasureSpec.getSize(widthSpec);
    
    if (element == null) {
      setMeasuredDimension(viewportWidth, 0);
      return;
    }

    if (isLayoutRequested() || viewportWidth != getMeasuredWidth()) {
   //   int minW = getMinimumWidth(viewportWidth);
     // int w = Math.max(minW, viewportWidth);
    //  Dim.setWidth(this, w);
      // TODO(haustein) We may need to make the viewport width available to calculations...
      measureBlock(viewportWidth, viewportWidth, null, false);
    } else {
      super.onMeasure(widthSpec, heightSpec);
    }
  }

  public void loadHtml(String html) {
    loadData(HtmlUtils.getUtf8Bytes(html), null);
  }
  
  /**
   * Loads an HTML document from the given stream. 
   * 
   * @param is The stream to read the document from
   * @param encoding The stream encoding. Defaults to UTF8 if null or empty.
   */
  public void loadData(byte[] data, String encoding) {

    // Obtain the data, build the DOM
    Element htmlElement;

    try {
      Element dummy = new Element(this, "");
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      XmlPullParser parser = factory.newPullParser();
      if (encoding != null && encoding.trim().length() == 0) {
        encoding = HtmlUtils.UTF8;
      }
      parser.setInput(new ByteArrayInputStream(data), encoding);
      parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true);

      for (int i = 0; i < HTML_ENTITY_TABLE.length; i += 2) {
        parser.defineEntityReplacementText(HTML_ENTITY_TABLE[i], HTML_ENTITY_TABLE[i + 1]);
      }

      dummy.parseContent(parser);

      htmlElement = dummy.getElement("html");

      if (htmlElement == null) {
        htmlElement = new Element(this, "html");
      }

      element = htmlElement.getElement("body");
      if (element == null) {
        element = new Element(this, "body");
        htmlElement.addElement(element);

        for (int i = 0; i < dummy.getChildCount(); i++) {
          switch (dummy.getChildType(i)) {
            case Element.TEXT:
              element.addText(dummy.getText(i));
              break;
            case Element.ELEMENT:
              if (!dummy.getElement(i).getName().equals("html")) {
                element.addElement(dummy.getElement(i));
              }
              break;
          }
        }
      }
    } catch (XmlPullParserException e) {
      // this should not happen since the pull parser must not throw exceptions in relaxed mode.
      throw new RuntimeException(e);
    } catch (IOException e) {
      // this cannot happen since we read from a byte array.
      throw new RuntimeException(e);
    } 

    // Remove reference to thel dummy element that was created to simplify parsing
    htmlElement.setParent(null);

    // Apply the default style sheet and style info collected while building
    // the element tree.
    applyStyle();
    
    // Defer scrolling to the fragment to when we know where it is...
    post(new Runnable() {
      public void run() {
        gotoLabel(documentUrl.getFragment());
      }
    });
  }


  /**
   * Scrolls to the element with the given label (&lt;a name...) and focuses it
   * (if focusable).
   */
  public void gotoLabel(String label) {
    View v = label == null ? this : labels.get(label);
    if (v != null) {
      if (v.isFocusable()) {
        v.requestFocus();
      }
      int y = v.getTop();
      ViewParent parent = v.getParent();
      while (parent instanceof View) {
        if (parent instanceof ScrollView) {
          ((ScrollView) parent).smoothScrollTo(0, y);
          break;
        }
        y += ((View) parent).getTop();
        parent = parent.getParent();
      }
    }
  }


  /**
   * Converts a URL to an absolute URL, using the document base URL. If the URL
   * is already absolute, it is returned unchanged.
   */
  public URI getAbsoluteUrl(String relativeUrl) {
    return baseURL.resolve(relativeUrl);
  }

  /**
   * Returns the URL of this document, as set in setDocumentUrl() (or via loadUrl).
   */
  public URI getDocumentUrl() {
    return documentUrl;
  }

  /**
   * Applies the style sheet to the document. 
   */
  void applyStyle() {
    if (element == null) {
      return;
    }
    styleSheet.apply(element, baseURL);
    if (needsBuild) {
      needsBuild = false;
      children = new ArrayList<View>();
      BuildContext buildContext = new BuildContext();
      buildContext.preserveLeadingSpace = true;
      addChildren(element, buildContext);
    }
    requestLayoutAll();
  }

  /**
   * Use this method to prefill images or to add images requested via the RequestHandler.
   */
  public void addImage(URI uri, Bitmap image) {
    images.put(uri, image);
    ArrayList<ImageRequest> requests = pendingImageRequests.get(uri);
    if (requests != null) {
      pendingImageRequests.remove(uri);
      for (ImageRequest request: requests) {
        ((ImageView) request.widget.getChildAt(0)).setImageBitmap(image);
        request.widget.requestLayout();
      }
    }
  }
  
  /**
   * Use this method to prefill style sheets or to add style sheets requested via the RequestHandler.
   */
  public void addStyleSheet(URI uri, String text) {
    styleSheets.put(uri, text);
    int[] position = pendingStyleSheetRequests.get(uri);
    if (position != null) {
      pendingStyleSheetRequests.remove(uri);
      updateStyle(uri, text, position);
      applyStyle();
    }
  }


  /**
   * Returns the title of this document.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Returns the base URL of this document (used to resolve relative links).
   */
  public URI getBaseURL() {
    return baseURL;
  }
  
  /**
   * Parses the style sheet and requests sub-stylesheets. Does not apply the style sheet to
   * the dom.
   */
  void updateStyle(URI uri, String css, int[] nestingPositions) {
    appliedStyleSheets.add(uri);
    ArrayList<StyleSheet.Dependency> dependencies = new ArrayList<StyleSheet.Dependency>();
    styleSheet.read(css, uri, nestingPositions, MEDIA_TYPES, dependencies);
    for (StyleSheet.Dependency d: dependencies) {
      requestStyleSheet(d.getUrl(), d.getNestingPositions());
    }
  }

  void requestStyleSheet(URI url, int[] nestingPositions) {
    if (appliedStyleSheets.contains(url)) {
      return;
    }
    String css = styleSheets.get(url);
    if (css != null) {
      updateStyle(url, css, nestingPositions);
    } else {
      pendingStyleSheetRequests.put(url, nestingPositions);
      requestHandler.requestStyleSheet(this, url);
    }
  }

  Bitmap requestImage(URI src, AbstractElementView target, ImageRequest.Type type) {
    Bitmap image = images.get(src);
    if (image != null) {
      return image;
    }
    ArrayList<ImageRequest> requests = pendingImageRequests.get(src);
    if (requests == null) {
      requests = new ArrayList<ImageRequest>();
      pendingImageRequests.put(src, requests);
    }
    requests.add(new ImageRequest(target, type));
    requestHandler.requestImage(this, src);
    return null;
  }
  
  
  static class ImageRequest {
    enum Type {
      REGULAR, BACKGROUND, INPUT
    }
    final AbstractElementView widget;
    final Type type;
    
    ImageRequest(AbstractElementView widget, Type type) {
      this.widget = widget;
      this.type = type;
    }
  }

}
