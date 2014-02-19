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

import org.kobjects.htmlview.HtmlView;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class HtmlDemo extends Activity {

  HtmlView htmlView;
  LinearLayout linearLayout;
  ScrollView scrollView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    scrollView = new ScrollView(this);
    linearLayout = new LinearLayout(this);
    linearLayout.setOrientation(LinearLayout.VERTICAL);
    htmlView = new HtmlView(this);
    linearLayout.addView(htmlView);
    scrollView.addView(linearLayout);
    setContentView(scrollView);
    loadDemo();
  }

  @Override
  public void onBackPressed() {
    if (htmlView.getBaseUrl().equals(HtmlView.ASSET_BASE_URL.resolve("index.html"))) {
      super.onBackPressed();
    } else {
      loadDemo();
    }
  }
  
  void loadDemo() {
    htmlView.loadUrl("index.html");
    while(linearLayout.getChildCount() > 1) {
    	linearLayout.removeViewAt(linearLayout.getChildCount() - 1);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add("Reset");
    menu.add("Add 10 WebViews");
    menu.add("Add 10 HtmlViews");
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    String title = item.getTitle().toString();
    if (title.equals("Reset")) {
      loadDemo();
      return true;
    }
    TextView label = new TextView(this);
    label.setText("View count: " + (linearLayout.getChildCount() + 10));
    if (title.equals("Add 10 HtmlViews")) {
      for (int i = 0; i < 10; i++) {
        HtmlView htmlView = new HtmlView(this);
        linearLayout.addView(htmlView);
        htmlView.loadUrl("index.html");
      }
      linearLayout.addView(label);
      return true;
    }
    if (title.equals("Add 10 WebViews")) {
      for (int i = 0; i < 10; i++) {
        WebView webView = new WebView(this);
        linearLayout.addView(webView);
        webView.loadUrl("file:///android_asset/index.html");
      }
      linearLayout.addView(label);
      return true;
    }
   	return super.onContextItemSelected(item);
  }
}
