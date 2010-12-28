package com.lego.minddroid;

import android.content.Context;
import android.view.View;

public class TouchGameConnector implements GameConnector {
	Context context;
	MINDdroid uiActivity;
	TouchGameView mView;
	
	public TouchGameConnector(Context context, MINDdroid uiActivity) {
		this.context = context;
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
