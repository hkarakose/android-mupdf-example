package com.librelio.activity;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import com.artifex.mupdf.MediaHolder;
import com.example.mupdf.R;
import com.librelio.view.ImageLayout;

/**
 * @author Mike Osipov
 */
public class SlideShowActivity extends FragmentActivity {
	private static final String TAG = "SlideShowActivity";
	
	private ImageLayout imageLayout;
	private Handler autoPlayHandler;
	
	private String fullPath;
	private int autoPlayDelay;
	private int bgColor;
	private int initialSlidePosition = 0;
	private boolean transition = true;
	private boolean autoPlay;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.sideshow_activity_layout);
		LinearLayout frame = (LinearLayout)findViewById(R.id.slide_show_full);

        Bundle extras = getIntent().getExtras();
        autoPlayDelay = extras.getInt(MediaHolder.PLAY_DELAY_KEY);
		transition = extras.getBoolean(MediaHolder.TRANSITION_KEY);
		autoPlay = extras.getBoolean(MediaHolder.AUTO_PLAY_KEY);
		bgColor = extras.getInt(MediaHolder.BG_COLOR_KEY);
		fullPath = extras.getString(MediaHolder.FULL_PATH_KEY);
		initialSlidePosition = extras.getInt(MediaHolder.INITIAL_SLIDE_POSITION);

		imageLayout = new ImageLayout(this, fullPath, transition);
		LayoutParams lp = new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.CENTER;
		imageLayout.setLayoutParams(lp);

		imageLayout.post(new Runnable() {
			@Override
			public void run() {
				imageLayout.setCurrentPosition(initialSlidePosition, false);
			}
		});
		
		if(autoPlay) {
			autoPlayHandler = new Handler();
			autoPlayHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG, "autoPlayHandler start");
					imageLayout.setCurrentPosition(imageLayout.getCurrentPosition() + 1, transition);
					autoPlayHandler.postDelayed(this, autoPlayDelay);
				}}, autoPlayDelay);
		}
		
		imageLayout.setBackgroundColor(bgColor);
		
		frame.addView(imageLayout);
	}
}
