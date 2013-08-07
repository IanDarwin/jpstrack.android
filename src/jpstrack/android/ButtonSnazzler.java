package jpstrack.android;

import java.lang.reflect.Method;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

public class ButtonSnazzler implements OnTouchListener {
	
	static final String TAG = Main.TAG;

	static final int API_LEVEL = Integer.parseInt(android.os.Build.VERSION.SDK);

	static final DecelerateInterpolator sShrinker = new DecelerateInterpolator();

	static final OvershootInterpolator sExpander = new OvershootInterpolator();

	/**
	 * Make a button shrink a bit when pressed, and pop back to
	 * bigger-than-normal then normal when released. From a demo by Chet Haase,
	 * DevBytes: Anticipation and Overshoot - Part 1 at
	 * https://developer.android.com/develop/index.html
	 */

	@Override
	public boolean onTouch(View v, MotionEvent me) {
		Log.d(TAG, "ButtonSnazzler.onTouch(): API Level = " + API_LEVEL);
		if (API_LEVEL < 11) { // Honeycomb
			return false;
		}
		Log.d(TAG, "ButtonSnazzler.onTouch(): Version OK");
		
		if (!(v instanceof Button)) {
			return false;
		}

		// v.animate().setDuration(200); // faster than default

		if (me.getAction() == MotionEvent.ACTION_DOWN) {
			// v.animate().setInterpolator(sExpander).scaleX(.7f).scaleY(.7f);
			animate(v, sShrinker, 0.7f, 150);
		} else if (me.getAction() == MotionEvent.ACTION_UP) {
			// v.animate().setInterpolator(sExpander).scaleX(1.scaleY(1);
			animate(v, sExpander, 1f, 200);
		}

		return false;
	}

	private void animate(View v, Interpolator inter, float scale, int duration) {
		Log.d(TAG, "ButtonSnazzler.animate()");
		try {
			Method animate = View.class.getMethod("animate", (Class[])null);
			if (animate == null) {
				return;
			}

			Object viewPropertyAnimator = animate.invoke(v, (Object[])null);
			Class<? extends Object> viewPropertyAnimatorClazz = viewPropertyAnimator.getClass();

			Method setInterpolator = viewPropertyAnimatorClazz.getMethod(
					"setInterpolator", new Class[] { Interpolator.class });
			setInterpolator.invoke(viewPropertyAnimator, new Object[] { inter });

			@SuppressWarnings("unchecked")
			Class<? extends Object>[] floatArray = new Class[] { float.class };
			float[] scaleArray = new float[] { scale };
			Method scaleX = viewPropertyAnimatorClazz.getMethod("scaleX", floatArray);
			scaleX.invoke(viewPropertyAnimator, scaleArray);

			Method scaleY = viewPropertyAnimatorClazz.getMethod("scaleY", floatArray);
			scaleY.invoke(viewPropertyAnimator, scaleArray);

			Method setDuration = viewPropertyAnimatorClazz.getMethod(
					"setDuration", new Class[] { int.class });
			setDuration.invoke(viewPropertyAnimator, new int[] { duration });

		} catch (Exception e) {
			Log.e(TAG, "animate: caught exception " + e, e);
		}
	}
}
