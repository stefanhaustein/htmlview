// Copyright 2008 Google Inc.
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

/**
 * Interface that is used by htmlView to ask for resources, opening links and submitting form data.
 * 
 * @author Stefan Haustein
 */
public interface RequestHandler {

  enum ProgressType {
    CONNECTING, LOADING_PERCENT, LOADING_BYTES, DONE
  }
  
  void click(HtmlView htmlView, Element e, String onClick);
  
  void openLink(HtmlView htmlView, Element a, URI uri);
  
  void submitForm(HtmlView htmlView, Element form, URI uri, boolean post, List<Map.Entry<String,String>> formData);
  
  void requestImage(HtmlView htmlView, URI uri);
  
  void requestStyleSheet(HtmlView htmlView, URI uri);
  
  void error(HtmlView htmlView, Exception e);
  
  void progress(HtmlView htmlView, ProgressType type, int progress);
}
