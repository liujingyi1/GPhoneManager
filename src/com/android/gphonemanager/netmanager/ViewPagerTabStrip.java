package com.android.gphonemanager.netmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.text.TextPaint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.gphonemanager.R;

@SuppressLint("DrawAllocation")
public class ViewPagerTabStrip extends LinearLayout {
    private int mSelectedUnderlineThickness;
    private final Paint mSelectedUnderlinePaint;

    private int mUnderlinebBottom;
    private int mUnderlinebOver;
    boolean  mIsText;
    
    private int mIndexForSelection;
    private float mSelectionOffset;

    public ViewPagerTabStrip(Context context) {
        this(context, null);
    }

    public ViewPagerTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources res = context.getResources();

        mSelectedUnderlineThickness =
                res.getDimensionPixelSize(R.dimen.tab_selected_underline_height);
        mUnderlinebBottom = 
        		res.getDimensionPixelSize(R.dimen.tab_selected_underline_height);
        mUnderlinebOver =
                res.getDimensionPixelSize(R.dimen.tab_selected_underline_over);
        
        int underlineColor = res.getColor(R.color.tab_selected_underline_color);
        int backgroundColor = res.getColor(R.color.actionbar_background_color);
        

        mSelectedUnderlinePaint = new Paint();
        mSelectedUnderlinePaint.setColor(underlineColor);

        setBackgroundColor(backgroundColor);
        setWillNotDraw(false);
    }

    /**
     * Notifies this view that view pager has been scrolled. We save the tab index
     * and selection offset for interpolating the position and width of selection
     * underline.
     */
    void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mIndexForSelection = position;
        mSelectionOffset = positionOffset;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int childCount = getChildCount();

        // Thick colored underline below the current selection
        if (childCount > 0) {
            View selectedTitle = getChildAt(mIndexForSelection);
            if (selectedTitle == null) {
                return;
            }
            
            int selectedLeft;
            int selectedRight;
            
			int left = selectedTitle.getLeft();
			int right = selectedTitle.getRight();

			TextView tView = (TextView)selectedTitle;
			String text = (String)tView.getText();
			Rect bounds = new Rect();
			TextPaint tPaint = tView.getPaint();
			tPaint.getTextBounds(text, 0, text.length(), bounds);
			selectedLeft = (right+left)/2-bounds.width()/2-mUnderlinebOver;
			selectedRight = (right+left)/2+bounds.width()/2+mUnderlinebOver;

            final boolean hasNextTab = (mIndexForSelection < (getChildCount() - 1));
            if ((mSelectionOffset > 0.0f) && hasNextTab) {
                View nextTitle = getChildAt(mIndexForSelection + (/*isRtl ? -1 :*/ 1));
                
                int nextLeft;
                int nextRight;
                
				int nextleft = nextTitle.getLeft();
				int nextright = nextTitle.getRight();
				TextView tnextView = (TextView)nextTitle;
				String nexttext = (String)tnextView.getText();
				Rect nextbounds = new Rect();
				TextPaint tnextPaint = tnextView.getPaint();
				tnextPaint.getTextBounds(nexttext, 0, nexttext.length(), nextbounds);
				nextLeft = (nextright+nextleft)/2-nextbounds.width()/2-mUnderlinebOver;
				nextRight = (nextright+nextleft)/2+nextbounds.width()/2+mUnderlinebOver;

                selectedLeft = (int) (mSelectionOffset * nextLeft +
                        (1.0f - mSelectionOffset) * selectedLeft);
                selectedRight = (int) (mSelectionOffset * nextRight +
                        (1.0f - mSelectionOffset) * selectedRight);
            }

            int height = getHeight();
            canvas.drawRect(selectedLeft, height - mSelectedUnderlineThickness - mUnderlinebBottom,
                    selectedRight, height - mUnderlinebBottom, mSelectedUnderlinePaint);
        }
    }

    /*private boolean isRtl() {
        return getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }*/
}
