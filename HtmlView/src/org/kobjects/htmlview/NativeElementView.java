package org.kobjects.htmlview;

import java.net.URI;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.kobjects.css.Style;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

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


  public static View createInclude(Context context, Element element) {
    String layoutName = element.getAttributeValue("layout");
    int lid = context.getResources().getIdentifier(layoutName, "layout", context.getPackageName());
    LayoutInflater li = LayoutInflater.from(context);
    View view = li.inflate(lid, null);
    return new NativeElementView(context, element, false, view);
  }


  static NativeElementView createInput(final Context context, final Element element) {
    View content = null; // null should not be necessary 
    int textSize = element.getScaledPx(Style.FONT_SIZE);
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
    NativeElementView result = new NativeElementView(context, element, false, content);
    if (content instanceof Button) {
      content.setOnClickListener(result);
    }
    result.reset();
    return result;
  }
  
  

  static NativeElementView createSelect(final Context context, final Element element) {
    boolean multiple = element.getAttributeBoolean("multiple");
    ArrayList<Element> options = new ArrayList<Element>();
    for (int i = 0; i < element.getChildCount(); i++) {
      if (element.getChildType(i) == Element.ELEMENT) {
        Element child = element.getElement(i);
        if (child.getName().equals("option")) {
          options.add(child);
        }
      }
    }
    SelectAdapter adapter = new SelectAdapter(context, element, multiple, options);
    adapter.reset(); // needed here: performs measurement
    adapter.view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
     
    if (!element.getComputedStyle().isSet(Style.WIDTH)) {
      element.getComputedStyle().set(Style.WIDTH, Math.round(
          (adapter.width + adapter.view.getMeasuredWidth()) * 1000 / element.htmlView.pixelScale), Style.PX);
    } if (!element.getComputedStyle().isSet(Style.HEIGHT)) {
      element.getComputedStyle().set(Style.HEIGHT, Math.round(element.getFont().getFontMetricsInt(null) * 
          (1 + element.getAttributeInt("size", 1) * 2000 / element.htmlView.pixelScale)), Style.PX);
    }
    return new NativeElementView(context, element, false, adapter.view);
  }
  
  
  static NativeElementView createTextArea(final Context context, final Element element) {
    int textSize = element.getScaledPx(Style.FONT_SIZE);
    EditText editText = new EditText(context);
    editText.setSingleLine(false);
    editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    editText.setGravity(Gravity.TOP);
    // TODO: Calculate lines based on height if fixed.
    editText.setLines(element.getAttributeInt("rows", 2));
    editText.setVerticalScrollBarEnabled(true);
    LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
    editText.setLayoutParams(params);
    NativeElementView result = new NativeElementView(context, element, false, editText);
    result.reset();
    return result;
  }

  
  static void reset(Element element) {
    if (element.nativeView != null) {
      ((NativeElementView) element.nativeView.getParent()).reset();
    }
    for (int i = 0; i < element.getChildCount(); i++) {
      if (element.getChildType(i) == Element.ELEMENT) {
        reset(element.getElement(i));
      }
    }
  }
  
  static void resetRadioGroup(Element element, String name) {
    if (element.nativeView instanceof RadioButton && 
        name.equals(element.getAttributeValue("name"))) {
        ((RadioButton) element.nativeView).setChecked(false);
    }
    for (int i = 0; i < element.getChildCount(); i++) {
      if (element.getChildType(i) == Element.ELEMENT) {
        resetRadioGroup(element.getElement(i), name);
      }
    }
  }
  
  static void readValues(Element element, List<Map.Entry<String, String>> result) {
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
  void reset() {
    if (nativeView instanceof Checkable) {
      ((Checkable) nativeView).setChecked(element.getAttributeValue("checked") != null);
    } else if (nativeView instanceof EditText) {
      EditText editText = (EditText) nativeView;
      if (element.getName().equals("textarea")) {
        editText.setText(element.getText());
      } else {
        editText.setText(element.getAttributeValue("value"));
      }
    } else if (nativeView instanceof AdapterView<?>) {
      Object adapter = ((AdapterView<?>) nativeView).getAdapter();
      if (adapter instanceof SelectAdapter) {
        SelectAdapter select = (SelectAdapter) adapter;
        select.reset();
      }
    }
    invalidate();
  }

  /**
   * If this view has an input value, set it in the given map.
   */
  void readValue(List<Map.Entry<String,String>> result) {
    String name = element.getAttributeValue("name");
    if (name != null) {
      if (nativeView instanceof Checkable && ((Checkable) nativeView).isChecked()) {
        String value = element.getAttributeValue("value");
        addEntry(result, name, value == null ? "" : value);
      } else if (nativeView instanceof EditText) {
        addEntry(result, name, ((EditText) nativeView).getText().toString());
      } else if (nativeView instanceof AdapterView<?>) {
        Object adapter = ((AdapterView<?>) nativeView).getAdapter();
        if (adapter instanceof SelectAdapter) {
          ((SelectAdapter) adapter).readValue(result);
        }
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
          reset(form);
        } else if ("submit".equals(type)) {
          List<Map.Entry<String,String>> formData = new ArrayList<Map.Entry<String,String>>();
          String name = element.getAttributeValue("name");
          String value = element.getAttributeValue("value");
          if (name != null && value != null) {
            addEntry(formData, name, value);
          }
          readValues(form, formData);
          URI uri = element.htmlView.getBaseUrl();
          String action = element.getAttributeValue("action");
          if (action != null) {
            uri = uri.resolve(action);
          } 
          String method = element.getAttributeValue("method");
          if (method == null) {
            method = "get";
          }
          element.htmlView.requestHandler.submitForm(element.htmlView, element, uri, "post".equalsIgnoreCase(method), formData);
        } else if ("radio".equals(type)) {
          String name = element.getAttributeValue("name");
          if (name != null) {
            resetRadioGroup(form, name);
          }
          ((Checkable) nativeView).setChecked(true);
        }
      }
    }
  }

  static class SelectAdapter extends ArrayAdapter<Element> {
    BitSet selected;
    boolean multiple;
    ListView listView;
    Spinner spinner;
    Element select;
    private int width;
    // Why is AdapterView parameterized in the first place?
    @SuppressWarnings("rawtypes")
    AdapterView view;
    // Needed because notifyDataSetChanged does not work as expected, see below.
    List<View> knownViews = new ArrayList<View>();
    
    SelectAdapter(Context context, Element select, boolean multiple, ArrayList<Element> options) {
      super(context, android.R.layout.simple_list_item_1, options);
      this.multiple = multiple;
      if (multiple) {
        this.listView = new ListView(context);
        this.listView.setAdapter(this);
        this.view = listView;
        selected = new BitSet();
      } else {
        this.spinner = new Spinner(context);
        this.spinner.setAdapter(this);
        this.view = spinner;
      }
      this.select = select;
    }

    void readValue(List<Map.Entry<String, String>> result) {
      if (multiple) {
        for (int i = 0; i < getCount(); i++) {
          if (selected.get(i)) {
            readOptionValue(getItem(i), select.getAttributeValue("name"), result);
          }
        }
      } else {
        if (spinner.getSelectedItemPosition() >= 0) {
          readOptionValue(getItem(spinner.getSelectedItemPosition()), 
              select.getAttributeValue("name"),result);
        }
      }
    }

    private void readOptionValue(Element option, String name, List<Map.Entry<String, String>> result) {
      String value = option.getAttributeValue("value");
      addEntry(result, name, value == null ? option.getText() : value);
    }

    @Override
    public View getDropDownView(final int position, View reuse, final ViewGroup parent) {
      // TODO: Why is this nonsense needed :((
      TextView tv = reuse instanceof TextView ? (TextView) reuse : new TextView(getContext()) {
        public boolean onTouchEvent (MotionEvent event) {
          spinner.setSelection(position, true);
          return super.onTouchEvent(event);
        }
      };
      tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, select.getScaledPx(Style.FONT_SIZE));
      tv.setText(getItem(position).getText());
      int ts = (int) tv.getTextSize();
      tv.setPadding(ts / 4, ts / 2, ts / 4, ts / 2);
      return tv;
    }

    @Override
    public View getView(final int position, View reuse, final ViewGroup group) {
      TextView tv = reuse instanceof TextView ? (TextView) reuse : new TextView(getContext());
      tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, select.getScaledPx(Style.FONT_SIZE));
      tv.setText(getItem(position).getText());
      if (multiple) {
        while (knownViews.size() <= position) {
          knownViews.add(null);
        }
        knownViews.set(position, tv);
        int ts = (int) tv.getTextSize();
        tv.setPadding(ts / 4, ts / 2, ts / 4, ts / 2);
        tv.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
            selected.set(position, !selected.get(position));
            applySelection(v, position);
          }
        }); 
      }
      return tv;
    }

    // This should not be needed, but notifyDataSetChanged does not really work as
    // expected...
    public void applySelection(View v, int position) {
      v.setBackgroundColor(selected.get(position) ? 0xff8888ff : 0);
      v.invalidate();
    }

    public void reset() {
      if (multiple) {
        selected.clear();
      } else {
        spinner.setSelection(0, true);
      }
      width = 0;
      for (int i = 0; i < getCount(); i++) {
        Element option = getItem(i);
        String text = option.getText();
        int wi = HtmlUtils.measureText(select.getFont(), text, 0, text.length());
        if (wi > width) {
          width = wi;
        }
        
        if (option.getAttributeBoolean("selected")) {
          if (multiple) {
            selected.set(getCount());
          } else {
            spinner.setSelection(i, true);
          }
        }
      }
      notifyDataSetChanged();
      // Sad... :(
      // view.setAdapter (recommended on stackOverflow) hides the list.
      if (multiple) {
        for (int i = 0; i < knownViews.size(); i++) {
          View v = knownViews.get(i);
          if (v != null) {
            applySelection(v, i);
          }
        }
      }
    }
  }
  
  static void addEntry(List<Map.Entry<String,String>> result, final String key, final String value) {
    result.add(new Map.Entry<String,String>() {
      @Override
      public String getKey() {
        return key;
      }

      @Override
      public String getValue() {
        return value;
      }

      @Override 
      public String toString() {
        return key + '=' + value;
      }
      @Override
      public String setValue(String object) {
        throw new UnsupportedOperationException();
      }});
  }

}
