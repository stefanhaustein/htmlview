package org.kobjects.htmlview;

import org.kobjects.css.Style;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;

/**
 * Wraps a "regular" android view with HTML margins, padding and borders. 
 */
@SuppressLint("ViewConstructor")
class WrapperView extends AbstractElementView {

  private View content;

  static WrapperView createImg(Context context, Element child) {
    // TODO: Focus / click handling for buttons in a link?
    ImageView imageView = new ImageView(context);
    WrapperView wrapper = new WrapperView(context, child, false, imageView);
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


  static WrapperView createInput(Context context, Element element) {
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
        CheckBox checkBox = new CheckBox(context);
        checkBox.setChecked(element.getAttributeValue("checked") != null);
        content = checkBox;
      } else if ("radio".equals(type)) {
        RadioButton radioButton = new RadioButton(context);
        radioButton.setChecked(element.getAttributeValue("checked") != null);
        content = radioButton;
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
    return new WrapperView(context, element, false, content);
  }

  WrapperView(Context context, Element element, boolean traversable, View content) {
    super(context, element, traversable);
    this.content = content;
    addView(content);
  }

  
  @Override
  void measureBlock(int outerMaxWidth, int viewportWidth,
      LayoutContext parentLayoutContext, boolean shrinkWrap) {
    Style style = element.getComputedStyle();
    boolean fixedWidth = style.isLengthFixedOrPercent(Style.WIDTH);
    boolean fixedHeight = isHeightFixed();
    if (!(fixedHeight && fixedWidth)) {
      content.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
    }
    int innerWidth = fixedWidth ? element.getScaledPx(Style.WIDTH, outerMaxWidth) 
        : content.getMeasuredWidth();
    int innerHeight = fixedHeight ? getFixedInnerHeight() : content.getMeasuredHeight();
    if (fixedHeight || fixedWidth) {
      content.measure(MeasureSpec.EXACTLY | innerWidth, MeasureSpec.EXACTLY | innerHeight);
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
      content.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
      innerWidth = content.getMeasuredWidth();
    }
    minimumWidth = maximumWidth = innerWidth + border;
    widthValid = true;
  }

}
