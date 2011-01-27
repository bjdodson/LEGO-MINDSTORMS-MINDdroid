package com.lego.minddroid.controller;

import com.lego.minddroid.MINDdroid;

import android.content.Context;
import android.content.Intent;
import android.view.View;

/**
 * Interface between the main MINDdroid activity and controller UI.
 *
 */
public abstract class GameConnector {
	public static final String TILT = "tilt";
	public static final String TOUCH = "touch";
	public static final String WEB = "web";
	public static final String EXTRA_GAME_CONTROLLER = "GAME_CONTROLLER";
	
	public abstract View getView();
	public abstract GameThread getThread();
	public abstract void registerListener();
	public abstract void unregisterListener();
	
	public static GameConnector fromIntent(MINDdroid minddroid, Intent intent) {
		String controllerType = intent.getStringExtra(EXTRA_GAME_CONTROLLER);
        if (TOUCH.equals(controllerType)) {
        	return new TouchGameConnector(minddroid);
        } else if (WEB.equals(controllerType)) {
        	return new WebGameConnector(minddroid);
        } else {
        	return new TiltGameConnector(minddroid);
        }
	}
	
	public static String[] getControllerLabels() {
		 return new String[] {"Tilt Controller", "Touch Controller", "Web Controller"};
	}
	
	public static String[] getControllerClasses() {
		 return new String[] {TILT, TOUCH, WEB};
	}
}
