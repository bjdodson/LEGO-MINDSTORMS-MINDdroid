package com.lego.minddroid.controller;

import com.lego.minddroid.MINDdroid;

import android.content.Context;
import android.view.View;

public class TouchGameConnector extends GameConnector {
	Context context;
	MINDdroid uiActivity;
	TouchGameView mView;
	
	public TouchGameConnector(MINDdroid uiActivity) {
		this.context = uiActivity.getApplicationContext();
		this.uiActivity = uiActivity;
		mView = new TouchGameView(context, uiActivity);
	}

	@Override
	public View getView() {
		return mView;
	}
	
	@Override
	public GameThread getThread() {
		return mView.getThread();
	}
	
	@Override
	public void registerListener() {
		mView.registerListener();
	}
	
	@Override
	public void unregisterListener() {
		mView.unregisterListener();
	}
}
