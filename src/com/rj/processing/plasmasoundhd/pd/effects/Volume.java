package com.rj.processing.plasmasoundhd.pd.effects;

import java.util.HashMap;

import android.view.MotionEvent;

import com.rj.processing.mt.Cursor;
import com.rj.processing.plasmasoundhd.pd.instruments.PSND;
import com.rj.processing.plasmasoundhd.pd.instruments.Parameter;

public  class Volume extends Effect {	
	
	public Parameter amp; 
	public Parameter ampglobal;
	public Parameter on;
	public Parameter off;
	
	public Volume() {
		this.name = "volume";

		params = new HashMap<String, Parameter>();
		ampglobal = new Parameter(PSND.AMP_GLOBAL, true);
		ampglobal.setMinMax(0f, 1f);
		ampglobal.setDefault(0.9f);
		//params.put(AMP_GLOBAL, ampglobal );
		amp = new Parameter(PSND.AMP, false);
		amp.setMinMax(0f, 1f);
		amp.setDefault(0.9f);
		params.put(PSND.AMP, amp );
		
		on = new Parameter(PSND.AMP_ON, false);
		on.setMinMax(0, 1);
		on.setDefault(0);
		
		off = new Parameter(PSND.AMP_OFF, false);
		off.setMinMax(0, 1);
		off.setDefault(0);

		
	    
		this.yenabledlist = new String[] {
				PSND.AMP,
		};
		this.yenabled = false;

	}

	
	public void setVolume(final float val) {
		ampglobal.pushValue(val);
	}


	@Override
	public void touchDown(final MotionEvent me, final int index, final float x, final float y, final Cursor c) {
		super.touchDown(me, index, x, y, c);
		on.pushValue(1,index);
		setVolume(1);
	}
 	
	@Override
	public void touchUp(final MotionEvent me, final int index, final float x, final float y, final Cursor c) {
		super.touchUp(me, index, x, y, c);
		off.pushValue(1, index);
		//amp.pushValue(0, index);
	}
	
	@Override
	public void allUp() {
		super.allUp();
		for (int index=1; index<=MAX_INDEX; index++) {
			off.pushValue(1, index);
		}
	}


}
