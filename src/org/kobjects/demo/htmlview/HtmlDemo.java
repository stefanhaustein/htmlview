package org.kobjects.demo.htmlview;

//Copyright 2013 Google Inc.
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

import java.net.URI;
import java.net.URISyntaxException;

import org.kobjects.htmlview.HtmlView;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;

public class HtmlDemo extends Activity {

  HtmlView htmlView;
  ScrollView scrollView;
  URI startUri;

  static final String [] LINKS = {
    "http://www.uow.edu.au/~nabg/WebServer/HTMLnJavascript/HTML2.html",
    "http://www.uow.edu.au/~nabg/WebServer/HTMLnJavascript/Forms.html",
  };

  static final String DEMO_HTML_PREFIX = "<html><body>" +
      "<p style='border:5px solid green;background-color:yellow;padding:5px;float:right'>" +
      "float:right with<br>green border</p>" +
      "<h1>HtmlView Demo</h1>" +
      "<p>The links below are annotated with target='_htmlview' to stay in the html view. " +
      "Other links will trigger a web intent.</p>" +
      "<ul>";

  static final String DEMO_HTML_SUFFIX = "</ul>" +
      "<p>Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor " +
      "incidunt ut labore et dolore magna aliqua. " +
      "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut " +
      "aliquid ex ea commodi consequat.</p>" + 
      "<hr>" +
      "<center>The End.</center>" +
      "</body></html>";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    scrollView = new ScrollView(this);
    htmlView = new HtmlView(this);
    scrollView.addView(htmlView);
    setContentView(scrollView);
    try {
      startUri = new URI("");
    } catch (URISyntaxException e) {
    }
    loadDemo();
  }

  @Override
  public void onBackPressed() {
    if (htmlView.getDocumentUrl().equals(startUri)) {
      super.onBackPressed();
    } else {
      loadDemo();
    }
  }
  
  void loadDemo() {
    htmlView.setDocumentUrl(startUri);
    StringBuilder sb = new StringBuilder(DEMO_HTML_PREFIX);
    for (String s: LINKS) {
      sb.append("<li><a href='" + s + "' target='_htmlview'>" + s + "</a></li>");
    }
    sb.append(DEMO_HTML_SUFFIX);
    htmlView.loadHtml(sb.toString());
  }
  

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add("Reset");
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    String title = item.getTitle().toString();
    if (title.equals("Reset")) {
      loadDemo();
      return true;
    }
    return super.onContextItemSelected(item);
  }
}
