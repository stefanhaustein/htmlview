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
import java.io.UnsupportedEncodingException;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;


/**
 * Widget class representing a HTML document (or snippet). First builds up a
 * "logical" DOM consisting of Element objects for the (X)HTML code using KXML,
 * then constructs a physical View representation, consisting of BlockWidget,
 * InputWidget, TableWidget and TextFragmentWidget.
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
  public static URI ASSET_BASE_URL;

  {
    try {
      ASSET_BASE_URL = new URI("file:///android_asset/");
    } catch (URISyntaxException e) {
      // Should be impossible -- the URI syntax is correct.
      throw new RuntimeException(e);
    }
  }
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
  protected RequestHandler requestHandler = new DefaultRequestHandler(true);

  /** Base URL for this document */
  URI baseUrl = ASSET_BASE_URL;

  /** Maps labels to view */
  HashMap<String, View> labels = new HashMap<String, View>();

  /** Map for access keys */
  HashMap<Character, Element> accesskeys = new HashMap<Character, Element>();

  /** The title of the document. Initialized to "" to avoid null pointers. */
  String title = "";

  /** 
   * We need to keep a reference to the focused element since we may need to rebuild
   * the widgets.
   */
  Element focusedElement;

  /** True if the widget tree needs to be rebuilt. */
  boolean needsBuild = true;
  
  float pixelScale = 1;
  
  /**
   * Creates a HTML document widget.
   * 
   * @param requestHandler the object used for requesting resources (embedded images, links)
   * @param documentUrl document URL, used as base URL for resolving relative links
   */
  public HtmlView(Context context) {
    super(context, null, false);
    // android.R.layout.simple_spinner_dropdown_item
    // android.R.layout.simple_list_item_1
    TextView tv = (TextView) LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_dropdown_item, null,false); 
    
    pixelScale = tv.getTextSize() / 16f;
    Log.d("HtmlView", "TextView text size: " + tv.getTextSize() + " paint: " + tv.getPaint().getTypeface());
  }
  
  public void loadUrl(String url) {
    loadAsync(ASSET_BASE_URL.resolve(url), null, Onload.SHOW_HTML);
  }

  /**
   * This method expects that the URI is absolute.
   */
  public void loadAsync(final URI uri, final byte[] post, final Onload onload) {
    new AsyncTask<Void, Integer, Exception>() {
      String encoding;
      byte[] rawData;
      Bitmap image;      
      @Override
      protected Exception doInBackground(Void... params) {
        try {
          int contentLength = -1;
          InputStream is;
          String uriStr = HtmlUtils.toString(uri);
          if (uriStr.startsWith("file:///android_asset/")) {
            int cut = uriStr.indexOf("t/") + 2;
            String assetName = uriStr.substring(cut);
            is = getContext().getAssets().open(assetName);
            encoding = null;
          } else {
            publishProgress(RequestHandler.ProgressType.CONNECTING.ordinal(), 0);
            URLConnection con = uri.toURL().openConnection();
            con.setRequestProperty("UserAgent", "AndroidHtmlView/1.0 (Mobile)");
            if (post != null) {
              con.setDoOutput(true);
              con.getOutputStream().write(post);
            }
            is = con.getInputStream();
            encoding = "ISO-8859-1";  // As per HTTP spec.
            String contentType = con.getContentType();
            if (contentType != null) {
              int cut = contentType.indexOf("charset=");
              if (cut != -1) {
                encoding = contentType.substring(cut + 8).trim();
              }
            }
            contentLength = con.getContentLength();
          }
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          byte[] buf = new byte[8096];
          while (true) {
            if (contentLength <= 0) {
              publishProgress(RequestHandler.ProgressType.LOADING_BYTES.ordinal(), 
                  baos.size());
            } else {
              publishProgress(RequestHandler.ProgressType.LOADING_PERCENT.ordinal(), 
                  baos.size() * 100 / contentLength);
            }
            int count = is.read(buf);
            if (count <= 0) {
              break;
            }
            baos.write(buf, 0, count);
          }
          is.close();
          publishProgress(RequestHandler.ProgressType.DONE.ordinal(), 0);
          rawData = baos.toByteArray();
          baos = null;
          if (onload == Onload.ADD_IMAGE) {
            image = BitmapFactory.decodeByteArray(rawData, 0, rawData.length);
            rawData = null;
          }
          return null;
        } catch (Exception e) {
          encoding = null;
          return e;
        }
      }
      @Override
      protected void onPostExecute(Exception e) {
        if (e != null) {
          if (onload == Onload.SHOW_HTML) {
            requestHandler.error(HtmlView.this, e);
          }
        } else {
          switch(onload) {
          case SHOW_HTML:
            loadData(rawData, encoding, uri);
            break;
          case ADD_IMAGE:
            addImage(uri, image);
            break;
          case ADD_STYLE_SHEET:
            if (encoding == null) {
              encoding = HtmlUtils.UTF8;
            }
            try {
              addStyleSheet(uri, new String(rawData, encoding));
            } catch (UnsupportedEncodingException uee) {
              Log.e("HtmlView", "Unsupported Encoding: " + encoding, uee);
            }
            break;
          }
        }
      }
      @Override 
      protected void onProgressUpdate(Integer... progress) {
        if (onload == Onload.SHOW_HTML) {
          requestHandler.progress(HtmlView.this, 
              RequestHandler.ProgressType.values()[progress[0]], progress[1]);
        }
      }
    }.execute(null, null);
  }


  public void onMeasure(int widthSpec, int heightSpec) {
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
    loadData(HtmlUtils.getUtf8Bytes(html), null, null);
  }
  
  /**
   * Loads an HTML document from the given stream. 
   * 
   * @param is The stream to read the document from
   * @param encoding The stream encoding. Defaults to UTF8 if null or empty.
   * @param url The base URL for resolving local image URLs, links etc. If null,
   *    the asset base URL will be used. If relative, this will be resolved relative
   *    to the asset base URL.
   */
  public void loadData(byte[] data, String encoding, URI url) {
    this.baseUrl = url == null ? ASSET_BASE_URL : ASSET_BASE_URL.resolve(url);
    
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

    // Remove reference to the dummy element that was created to simplify parsing
    htmlElement.setParent(null);

    // Apply the default style sheet and style info collected while building
    // the element tree.
    applyStyle();
    
    // Defer scrolling to the fragment to when we know where it is...
    post(new Runnable() {
      public void run() {
        gotoLabel(baseUrl.getFragment());
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
    return baseUrl.resolve(relativeUrl);
  }

  /**
   * Applies the style sheet to the document. 
   */
  void applyStyle() {
    if (element == null) {
      return;
    }
    styleSheet.apply(element, baseUrl);
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
   * Returns the base URL of this document as handed in. Will be the document URL 
   * if the document was loaded via loadUrl. Used to resolve relative URLs.
   */
  public URI getBaseUrl() {
    return baseUrl;
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
