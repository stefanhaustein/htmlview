package org.kobjects.htmlview;

import java.util.HashMap;
import java.util.Map;

import org.kobjects.css.Style;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;

/**
 * Wraps a "regular" android view with HTML margins, padding and borders. 
 */
@SuppressLint("ViewConstructor")
class NativeElementView extends AbstractElementView implements View.OnClickListener {

  private View nativeView;

  static NativeElementView createImg(Context context, Element child) {
    // TODO: Focus / click handling for buttons in a link?
    ImageView imageView = new ImageView(context);
    NativeElementView wrapper = new NativeElementView(context, child, false, imageView);
    String src = child.getAttributeValue("src");
    if (src != null) {
      Bitmap image = child.htmlView.requestImage(
          child.htmlView.getAbsoluteUrl(src), wrapper, 
          HtmlView.ImageRequest.Type.REGULAR);
      if (image != null) {
        imageView.setImageBitmap(image);
      }
    }
    return wrapper;
  }


  static NativeElementView createInput(Context context, Element element) {
    String name = element.getName();
    View content = null; // null should not be necessary 
    int textSize = element.getScaledPx(Style.FONT_SIZE);
    if ("textarea".equals(name)) {
      EditText editText = new EditText(context);
      editText.setSingleLine(false);
      editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
      content = editText;
    } else {//if ("input".equals(name)) {
      String type = element.getAttributeValue("type");
      if ("checkbox".equals(type)) {
        content = new CheckBox(context);
      } else if ("radio".equals(type)) {
        content = new RadioButton(context);
      } else if ("submit".equals(type) || "reset".equals(type)) {
        String value = element.getAttributeValue("value");
        Button button = new Button(context);
        button.setText(value == null ? type : value);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        content = button;
      } else {
        EditText editText = new EditText(context);
        if ("password".equals(type)) {
          editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        content = editText;
      }
    }
    NativeElementView result = new NativeElementView(context, element, false, content);
    if (content instanceof Button) {
      content.setOnClickListener(result);
    }
    result.reset(null);
    return result;
  }
  
  static void reset(Element element, String name) {
    if (element.nativeView != null) {
      ((NativeElementView) element.nativeView.getParent()).reset(name);
    }
    for (int i = 0; i < element.getChildCount(); i++) {
      if (element.getChildType(i) == Element.ELEMENT) {
        reset(element.getElement(i), name);
      }
    }
  }
  
  static void readValues(Element element, Map<String, String> result) {
    if (element.nativeView != null) {
      ((NativeElementView) element.nativeView.getParent()).readValue(result);
    } else if ("input".equals(element.getName())) {
      // Hidden element data.
    }
    for (int i = 0; i < element.getChildCount(); i++) {
      if (element.getChildType(i) == Element.ELEMENT) {
        readValues(element.getElement(i), result);
      }
    }
  }

  NativeElementView(Context context, Element element, boolean traversable, View content) {
    super(context, element, traversable);
    this.nativeView = element.nativeView = content;
    addView(content);
  }

  /**
   * If name is null, reset this view to the state specified in the DOM.
   * Otherwise, if the name matches, reset to unchecked.
   */
  void reset(String name) {
    if (name == null || name.equals(element.getAttributeValue("name"))) {
      if (nativeView instanceof Checkable) {
        ((Checkable) nativeView).setChecked(name == null && 
            element.getAttributeValue("checked") != null);
      } else if (nativeView instanceof EditText) {
        EditText editText = (EditText) nativeView;
        if (element.getName().equals("textarea")) {
          editText.setText(element.getText());
        } else {
          editText.setText(element.getAttributeValue("value"));
        }
      }
      invalidate();
    }
  }

  /**
   * If this view has an input value, set it in the given map.
   */
  void readValue(Map<String,String> result) {
    String name = element.getAttributeValue("name");
    if (name != null) {
      if (nativeView instanceof Checkable && ((Checkable) nativeView).isChecked()) {
        String value = element.getAttributeValue("value");
        result.put(name, value == null ? "" : value);
      } else if (nativeView instanceof EditText) {
        result.put(name, ((EditText) nativeView).getText().toString());
      }
    }
  }

  
  @Override
  void measureBlock(int outerMaxWidth, int viewportWidth,
      LayoutContext parentLayoutContext, boolean shrinkWrap) {
    Style style = element.getComputedStyle();
    boolean fixedWidth = style.isLengthFixedOrPercent(Style.WIDTH);
    boolean fixedHeight = isHeightFixed();
    if (!(fixedHeight && fixedWidth)) {
      nativeView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
    }
    int innerWidth = fixedWidth ? element.getScaledPx(Style.WIDTH, outerMaxWidth) 
        : nativeView.getMeasuredWidth();
    int innerHeight = fixedHeight ? getFixedInnerHeight() : nativeView.getMeasuredHeight();
    if (fixedHeight || fixedWidth) {
      nativeView.measure(MeasureSpec.EXACTLY | innerWidth, MeasureSpec.EXACTLY | innerHeight);
    }
    marginLeft = element.getScaledPx(Style.MARGIN_LEFT, outerMaxWidth);
    marginRight = element.getScaledPx(Style.MARGIN_RIGHT, outerMaxWidth);

    boxHeight = innerHeight + 
        element.getScaledPx(Style.MARGIN_TOP) + element.getScaledPx(Style.MARGIN_BOTTOM) +
        element.getScaledPx(Style.BORDER_TOP_WIDTH) + 
        element.getScaledPx(Style.BORDER_BOTTOM_WIDTH) +
        element.getScaledPx(Style.PADDING_TOP) + element.getScaledPx(Style.PADDING_BOTTOM);

    boxWidth = innerWidth + 
        element.getScaledPx(Style.MARGIN_LEFT) + element.getScaledPx(Style.MARGIN_RIGHT) +
        element.getScaledPx(Style.BORDER_LEFT_WIDTH) + 
        element.getScaledPx(Style.BORDER_RIGHT_WIDTH) +
        element.getScaledPx(Style.PADDING_LEFT) + element.getScaledPx(Style.PADDING_RIGHT);

    setMeasuredDimension(boxWidth, boxHeight);
    if (parentLayoutContext != null) {
      parentLayoutContext.advance(boxHeight);
    }
  }

  @Override
  void calculateWidth(int containerWidth) {
    int border =  element.getScaledPx(Style.BORDER_LEFT_WIDTH) + 
        element.getScaledPx(Style.BORDER_RIGHT_WIDTH) + 
        element.getScaledPx(Style.MARGIN_LEFT) +  element.getScaledPx(Style.MARGIN_RIGHT) + 
        element.getScaledPx(Style.PADDING_LEFT) + element.getScaledPx(Style.PADDING_RIGHT);
    
    Style style = element.getComputedStyle();
    int innerWidth;
    if (style.isLengthFixedOrPercent(Style.WIDTH)) {
      innerWidth = element.getScaledPx(Style.WIDTH, containerWidth);
    } else {
      nativeView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
      innerWidth = nativeView.getMeasuredWidth();
    }
    minimumWidth = maximumWidth = innerWidth + border;
    widthValid = true;
  }

  static Element findForm(Element current) {
    while (current != null && !"form".equals(current.getName())) {
      current = current.getParent();
    }
    return current;
  }

  @Override
  public void onClick(View v) {
    Log.d("HtmlView", "onClick " + element.toString());
    if ("input".equals(element.getName())) {
      String type = element.getAttributeValue("type");
      Element form = findForm(element);
      if (form != null) {
        if ("reset".equals(type)) {
          reset(form, null);
        } else if ("submit".equals(type)) {
          HashMap<String,String> formData = new HashMap<String,String>();
          readValues(form, formData);
          element.htmlView.requestHandler.submitForm(element.htmlView, element, null, false, formData);
        } else if ("radio".equals(type)) {
          String name = element.getAttributeValue("name");
          if (name != null) {
            reset(form, name);
          }
          ((Checkable) nativeView).setChecked(true);
        }
      }
    }
  }

}
