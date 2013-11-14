package net.cappital.torch.sample;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Clickable RelativeLayout that contains an ImageView and 2 TextViews. Set
 * attributes {@value android:title}, {@value android:text}, {@value
 * android:src} and {@value android:contentDescription} from XML or use the
 * setter methods.
 * 
 * @author Reinier
 * 
 */
public class BlockButton extends RelativeLayout {

	public BlockButton(Context context) {
		super(context);
		init(context, null);
	}

	public BlockButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public BlockButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		/*
		 * set default View characteristics
		 */
		setBackgroundResource(R.drawable.block_background_holo_light);
		setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
		setMinimumHeight(convertToPx(48));
		int padding = getResources().getDimensionPixelSize(R.dimen.blockbutton_padding);
		setPadding(padding, padding, padding, padding);

		// inflate view
		LayoutInflater.from(context).inflate(R.layout.view_blockbutton, this,
				true);

		// skip if attrs is null
		if (attrs == null)
			return;

		TypedArray array = context.obtainStyledAttributes(attrs,
				R.styleable.BlockButton, 0, 0);

		/*
		 * fill child Views with provided info from XML
		 */
		// set title
		String title = array.getString(R.styleable.BlockButton_android_title);
		if (title == null)
			title = "Title";
		setTitle(title);

		// set description
		String text = array.getString(R.styleable.BlockButton_android_text);
		if (text == null)
			text = "Title";
		setText(text);

		// set icon
		Drawable drawable = array
				.getDrawable(R.styleable.BlockButton_android_src);
		String contentDescription = array
				.getString(R.styleable.BlockButton_android_contentDescription);

		setIconDrawable(drawable);
		setContentDescription(contentDescription);

		array.recycle();
	}

	private int convertToPx(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				getResources().getDisplayMetrics());
	}

	/**
	 * Set this View's icon's ContentDescription.
	 * 
	 * @param resourceId The content-description's String resource ID.
	 */
	public void setContentDescription(int resourceId) {
		ImageView icon = (ImageView) findViewById(R.id.icon);
		if (icon != null)
			icon.setContentDescription(getResources().getString(resourceId));
	}

	/**
	 * Set this View's icon's ContentDescription.
	 * 
	 * @param text
	 */
	public void setContentDescription(String text) {
		ImageView icon = (ImageView) findViewById(R.id.icon);
		if (icon != null)
			icon.setContentDescription(text);
	}

	/**
	 * Set this View's icon from a Drawable
	 * 
	 * @param drawable
	 */
	public void setIconDrawable(Drawable drawable) {
		ImageView icon = (ImageView) findViewById(R.id.icon);
		if (icon != null)
			icon.setImageDrawable(drawable);
	}
	
	/**
	 * Set this View's icon from a resource
	 * 
	 * @param resourceId
	 */
	public void setIconResource(int resourceId) {
		ImageView icon = (ImageView) findViewById(R.id.icon);
		if (icon != null)
			icon.setImageResource(resourceId);
	}

	public void setOnIconClickListener(OnClickListener clickListener) {
		ImageView icon = (ImageView) findViewById(R.id.icon);
		if (icon != null) {
			icon.setOnClickListener(clickListener);
		}
	}

	/**
	 * Set this View's content / summary
	 * 
	 * @param resourceId ID of the String resource
	 */
	public void setText(int resourceId) {
		TextView tv = (TextView) findViewById(R.id.text);
		if (tv != null)
			tv.setText(resourceId);
	}

	/**
	 * Set this View's content / summary
	 * 
	 * @param text
	 */
	public void setText(String text) {
		TextView tv = (TextView) findViewById(R.id.text);
		if (tv != null)
			tv.setText(text);
	}

	/**
	 * Set this View's title
	 * 
	 * @param resourceId String resource id
	 */
	public void setTitle(int resourceId) {
		TextView tv = (TextView) findViewById(R.id.title);
		if (tv != null)
			tv.setText(resourceId);
	}

	/**
	 * Set this View's title
	 * 
	 * @param text
	 */
	public void setTitle(String text) {
		TextView tv = (TextView) findViewById(R.id.title);
		if (tv != null)
			tv.setText(text);
	}

}
