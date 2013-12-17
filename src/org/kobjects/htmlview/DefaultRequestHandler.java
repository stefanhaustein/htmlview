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

package org.kobjects.htmlview;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.kobjects.htmlview.HtmlView.Onload;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/**
 * A default implementation of RequestHandler. 
 * Delegates most calls to htmlView.loadAsync().
 */
public class DefaultRequestHandler implements RequestHandler {
  
  public static final String LOG_TAG = "HtmlViewReq";
  final boolean log;
  Toast toast;
  int toastTextLength;
  
  public DefaultRequestHandler() {
    this(false);
  }

  /**
   * If log is true, all requests are logged at level "DEBGUG" with the tag "HtmlViewReq".
   */
  public DefaultRequestHandler(boolean log) {
    this.log = log;
  }

  @Override
  public void openLink(HtmlView htmlView, Element a, URI href) {
    if (log) {
      Log.d(LOG_TAG, "onOpenLink  " + a + " " + HtmlUtils.toString(href));
    }
    if ("_htmlview".equals(a.getAttributeValue("target"))) {
      htmlView.loadAsync(href, HtmlView.Onload.SHOW_HTML);
    } else {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse(HtmlUtils.toString(href)));
      htmlView.getContext().startActivity(intent);
    }
  }

  @Override
  public void submitForm(HtmlView htmlView, Element form, URI uri, boolean post, 
      List<Map.Entry<String, String>> formData) {
    if (log) {
      Log.d(LOG_TAG, "onSubmitForm " + form + " " + HtmlUtils.toString(uri) + " " + formData);
    }
  }

  @Override
  public void requestImage(HtmlView htmlView, URI uri) {
    if (log) {
      Log.d(LOG_TAG, "onRequestImage " + HtmlUtils.toString(uri));
    }
    htmlView.loadAsync(uri, HtmlView.Onload.ADD_IMAGE);
  }

  @Override
  public void requestStyleSheet(HtmlView htmlView, URI uri) {
    if (log) {
      Log.d(LOG_TAG, "requestStyleSheet " + HtmlUtils.toString(uri));
    }
    htmlView.loadAsync(uri, HtmlView.Onload.ADD_STYLE_SHEET);
  }

  @Override
  public void click(HtmlView htmlView, Element e, String onClick) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void error(HtmlView htmlView, Exception e) {
    htmlView.loadHtml("<html><head>title>Error</title></head><body>" +
        "<h1>Loading HTML failed</h1><p>Error message:</p>" +
        "<p>" + e.getMessage() + "</p></body></html>");
  }

  @Override
  public void progress(HtmlView htmlView, ProgressType type, int progress) {
    String message = null;
    switch(type) {
    case CONNECTING:
      message = "Connecting";
      break;
    case LOADING_BYTES:
      message = "Loaded " + progress + " bytes";
      break;
    case LOADING_PERCENT:
      message = "Loaded " + progress + "%";
    }
    if (message == null) {
      if (toast != null) {
        toast.cancel();
        toast = null; 
      }
    } else {
      if (toast != null) {
        // Prevent jumping around... :-/
        if (toastTextLength != message.length()) {
          toast.cancel();
          toast = null;
        } else {
          toast.setText(message);
        }
      }
      if (toast == null) {
        toast = Toast.makeText(htmlView.getContext(), message, Toast.LENGTH_LONG);
      }
      toast.show();
    }
  }

}
