package com.lego.minddroid;

import android.view.View;

/**
 * Interface between the main MINDdroid activity and controller UI.
 *
 */
public interface GameConnector {
	public static final String TILT = "tilt";
	public static final String TOUCH = "touch";
	
	public View getView();
	public GameThread getThread();
	public void registerListener();
	public void unregisterListener();
	
	
	public abstract class GameThread extends Thread {
		public boolean mActionPressed;
	}
}
