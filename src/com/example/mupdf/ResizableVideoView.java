package com.example.mupdf;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.VideoView;

public class ResizableVideoView extends VideoView {
    Rect coord;
    boolean fullScreen;
    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    DisplayMode screenMode = DisplayMode.FULL_SCREEN;
    Rect tempCoord;
    RelativeLayout.LayoutParams tempLayout;

    public ResizableVideoView(Context paramContext) {
        super(paramContext);
    }

    public ResizableVideoView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
    }

    private void setOptimumVideoSize(ViewGroup.LayoutParams paramLayoutParams, int paramInt1, int paramInt2, int paramInt3, int paramInt4) {
        if (paramInt1 < paramInt2) {
            paramLayoutParams.width = paramInt1;
            paramLayoutParams.height = ((int) (mVideoHeight / mVideoWidth * paramInt1));
            return;
        }
        paramLayoutParams.width = ((int) (paramInt1 / paramInt2 * paramInt2));
        paramLayoutParams.height = paramInt2;
    }

    public void changeVideoSize(Rect paramRect1, Rect paramRect2) {
        if (fullScreen)
            return;
        mVideoWidth = paramRect1.width();
        mVideoHeight = paramRect1.height();
        coord = paramRect1;
        RelativeLayout localRelativeLayout = (RelativeLayout) getParent();
        RelativeLayout.LayoutParams localLayoutParams = (RelativeLayout.LayoutParams) localRelativeLayout.getLayoutParams();
        localLayoutParams.width = mVideoWidth;
        localLayoutParams.height = mVideoHeight;
        localLayoutParams.leftMargin = paramRect1.left;
        localLayoutParams.topMargin = paramRect1.top;
        localLayoutParams.rightMargin = (paramRect2.right - paramRect1.right);
        localLayoutParams.bottomMargin = (paramRect2.bottom - paramRect1.bottom);
        process();
        forceLayout();
        postInvalidate();
        localRelativeLayout.invalidate();
    }

    public void exitFullScreen() {
        fullScreen = false;
        mVideoWidth = tempCoord.width();
        mVideoHeight = tempCoord.height();
        coord = new Rect(tempCoord);
        tempCoord = null;
        RelativeLayout localRelativeLayout = (RelativeLayout) getParent();
        localRelativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(tempLayout));
        localRelativeLayout.setPadding(0, 0, 0, 0);
        process();
        localRelativeLayout.invalidate();
    }

    public void fullScreen() {
        fullScreen = true;
        RelativeLayout localRelativeLayout = (RelativeLayout) getParent();
        tempLayout = ((RelativeLayout.LayoutParams) localRelativeLayout.getLayoutParams());
        tempCoord = coord;
        WindowManager localWindowManager = (WindowManager) getContext().getSystemService("window");
        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        localWindowManager.getDefaultDisplay().getMetrics(localDisplayMetrics);
        int newWidth = localDisplayMetrics.widthPixels;
        int newHeight = localDisplayMetrics.heightPixels;
        RelativeLayout.LayoutParams newLayoutParams = new RelativeLayout.LayoutParams(newWidth, newHeight);
        localRelativeLayout.setLayoutParams(newLayoutParams);
        RelativeLayout.LayoutParams localLayoutParams2 = new RelativeLayout.LayoutParams(0, 0);
        setOptimumVideoSize(localLayoutParams2, newWidth, newHeight, mVideoWidth, mVideoHeight);
        int k = (int) (0.5D * (newLayoutParams.height - localLayoutParams2.height));
        int m = (int) (0.5D * (newLayoutParams.width - localLayoutParams2.width));
        localRelativeLayout.setPadding(m, k, m, k);
        coord = new Rect(0, 0, newWidth, newHeight);
        mVideoWidth = localLayoutParams2.width;
        mVideoHeight = localLayoutParams2.height;
        process();
        localRelativeLayout.invalidate();
    }

    public Rect getCoord() {
        return coord;
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public void onDraw(Canvas paramCanvas) {
        super.onDraw(paramCanvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int newWidth = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int newHeight = getDefaultSize(mVideoHeight, heightMeasureSpec);

        if (screenMode == DisplayMode.ORIGINAL) {
            if (mVideoWidth > 0 && mVideoHeight > 0) {
                if (mVideoWidth * newHeight > newWidth * mVideoHeight) {
                    // video height exceeds screen, shrink it
                    newHeight = newWidth * mVideoHeight / mVideoWidth;
                } else if (mVideoWidth * newHeight < newWidth * mVideoHeight) {
                    // video width exceeds screen, shrink it
                    newWidth = newHeight * mVideoWidth / mVideoHeight;
                } else {
                    // aspect ratio is correct
                }
            }
        } else if (screenMode == DisplayMode.FULL_SCREEN) {
            // just use the default screen width and screen height
        } else if (screenMode == DisplayMode.ZOOM) {
            // zoom video
            if (mVideoWidth > 0 && mVideoHeight > 0 && mVideoWidth < newWidth) {
                newHeight = mVideoHeight * newWidth / mVideoWidth;
            }
        }

        // must set this at the end
        setMeasuredDimension(newWidth, newHeight);
    }

    public void pause() {
        super.pause();
    }

    void process() {
        setMinimumHeight(mVideoHeight);
        setMinimumWidth(mVideoWidth);
        ((RelativeLayout) getParent()).measure(MeasureSpec.makeMeasureSpec(mVideoWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mVideoHeight, MeasureSpec.EXACTLY));
        getHolder().setFixedSize(mVideoWidth, mVideoHeight);
    }

    void process2() {
        getHolder().setFixedSize(mVideoWidth, mVideoHeight);
        measure(MeasureSpec.makeMeasureSpec(mVideoWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mVideoHeight, MeasureSpec.EXACTLY));
        setMinimumHeight(mVideoHeight);
        setMinimumWidth(mVideoWidth);
        forceLayout();
    }

    public void setCoord(Rect paramRect) {
        coord = paramRect;
    }

    public void setFullScreen(boolean paramBoolean) {
        fullScreen = paramBoolean;
    }

    public void start() {
        super.start();
    }

    public enum DisplayMode {
        ORIGINAL, FULL_SCREEN, ZOOM;
    }
}