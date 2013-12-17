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
import android.widget.ScrollView;

public class HtmlDemo extends Activity {

  HtmlView htmlView;
  ScrollView scrollView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    scrollView = new ScrollView(this);
    htmlView = new HtmlView(this);
    scrollView.addView(htmlView);
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
