package com.rj.processing.plasmasoundhd.pd.instruments;

import java.util.ArrayList;

import org.json.JSONObject;
import org.puredata.core.PdBase;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.MotionEvent;

import com.rj.processing.mt.Cursor;
import com.rj.processing.plasmasoundhd.Launcher;
import com.rj.processing.plasmasoundhd.pd.PDManager;
import com.rj.processing.plasmasoundhd.pd.effects.ASDR;
import com.rj.processing.plasmasoundhd.pd.effects.Delay;
import com.rj.processing.plasmasoundhd.pd.effects.Effect;
import com.rj.processing.plasmasoundhd.pd.effects.Filter;
import com.rj.processing.plasmasoundhd.pd.effects.Reverb;
import com.rj.processing.plasmasoundhd.pd.effects.SequencerStuff;
import com.rj.processing.plasmasoundhd.pd.effects.Tremolo;
import com.rj.processing.plasmasoundhd.pd.effects.Vibrato;
import com.rj.processing.plasmasoundhd.pd.effects.Volume;

public class Instrument {
	final PDManager p;
	
	
	
	public static int MAX_INDEX;
	
	
	final private ArrayList<Effect> effects = new ArrayList<Effect>();
	final private Volume volume;
	final public SequencerStuff sequencer;
	
	
	private int patch;
	private String patchName;
	
	public float midiMin = 0;
	public int visualQuality;
	public float midiMax = 127;
	public float waveform = 1;
	public static int NCONTINUOUS = 0;
	public static int NQUANTIZE = 1;
	public static int NSLIDE = 2;
	public int quantize = NCONTINUOUS;
	public String quantval;

	public boolean ready = false;
	TouchAbstraction touchabs;
	
	public Instrument(final PDManager p) {
		MAX_INDEX = Launcher.getUIType() == Launcher.PHONE ? 4 : 8; //phones support 4 touches, tablets support 8;
		touchabs = new TouchAbstraction(MAX_INDEX);
		visualQuality  = Launcher.getUIType() == Launcher.PHONE ? 1 : 2;
		this.p = p;
		volume = new Volume();
		sequencer = new SequencerStuff();
		effects.add(new ASDR());
		//effects.add(sequencer);
		effects.add(new Vibrato());
		effects.add(new Tremolo());
		effects.add(new Delay());
		effects.add(new Reverb());
		effects.add(new Filter());
		effects.add(volume);
	}
	
	public void setPatch(final String patch) {
		patchName = patch;
//		new Thread(new Runnable() { public void run() {
			initInstrument();
			ready = true;
//		}}).start();
	}
	
	public void initInstrument() {
		patch = p.openPatch(patchName);
	}
	
	public void touchUp(final MotionEvent me, int index, float x, final float width, float y, final float height, final Cursor c) {
		//Log.d("Instrument", "TOUCH UP!!!!!! : "+c.curId+" index:"+index);
		x=x/width;
		y=y/height;
		//index ++;
		if (c != null)
			index = touchabs.remove(c);
		//Log.d("Instrument", "TOUCH UP!!!!!! : new index:"+index);
		if (ready && index <= MAX_INDEX) {
			for (final Effect e : effects) {
				//e.touchUp(me, index, x, 0, c); //the only reason I did this was to have the volume ramp down properly which I think is fixed anyway
				e.touchUp(me, index, x, y, c); 
			}
		}
	}
	public void touchMove(final MotionEvent me, int index, float x, final float width, float y, final float height, final Cursor c) {
		//Log.d("Instrument", "TOUCH MOVE!!!!!!: "+c.curId+" index:"+index);
		x=x/width;
		y=y/height;
		//index ++;
		if (c != null)
			index = touchabs.move(c);
		//Log.d("Instrument", "TOUCH MOVE!!!!!!: new index:"+index);
		if (ready && index <= MAX_INDEX) {
			setPitch(x, index, c, width);
			for (final Effect e : effects) {
				e.touchMove(me, index, x, y, c);
			}
		}
	}
	public void touchDown(final MotionEvent me, int index, float x, final float width, float y, final float height, final Cursor c) {
		//Log.d("Instrument", "TOUCH DOWN!!!!!!: "+c.curId+" index:"+index);
		x=x/width;
		y=y/height;
		//index ++;
		if (c != null)
			index = touchabs.add(c);
		//Log.d("Instrument", "TOUCH DOWN!!!!!!: new index:"+index);
		if (ready && index <= MAX_INDEX) {
			setVolume(1);
			setPitch(x, index, c, width);
			for (final Effect e : effects) {
				e.touchDown(me, index, x, y, c);
			}
		}
	}
	public void allUp() {
		if (ready) {
			//setVolume(0);
			//no.
			touchabs.allUp();
			for (int index=1; index<=MAX_INDEX; index++) {
				for (final Effect e : effects) {
					//e.touchUp(null, index, 0, 0, null);
					e.allUp(); //don't think the abolve is necessary anymore.
				}
			}
		}
	}
	
	
	public void setMidiMin(final float val) {
		this.midiMin = val;
	}
	public void setMidiMax(final float val) {
		this.midiMax = val;
	}
	
	public void setVisualQuality(final int val) {
		this.visualQuality = val;
	}
	
	private void sendMessage(final String s,final  float val) {
		PdBase.sendFloat(s, val);
	}
	private void sendMessage(final String s,final  float val,final  int index) {
		PdBase.sendFloat(s+index, val);
	}
	
	public void setPitch(final float val) {
		float pitch = midiMin + ((val+(1/(2*midiMax-2*midiMin))) * (midiMax-midiMin));
		if (quantize != NCONTINUOUS)
			pitch = (float)Math.floor(pitch);
		sendMessage("pitch", pitch);
	}
	public void setPitch(final float val,final int index,final Cursor c, final float width) {
		float pitch = midiMin + (val * (midiMax-midiMin));
		if (quantize != NCONTINUOUS) {
			if (quantize == NQUANTIZE || isCursorSnapped(c, width)) {
				pitch = (float)Math.round(pitch); //too close! round!
			} 
		}
		sendMessage("pitch", pitch, index);
	}
	public boolean isCursorSnapped(final Cursor c, final float width) {
		if (c == null) return false;
		final float spacing = (midiMax-midiMin)/width;
		final int firstClosestX = Math.round((c.firstPoint.x) * spacing);
		final int lastClosestX = Math.round((c.currentPoint.x) * spacing);
		if (firstClosestX == lastClosestX) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public void setVolume(final float amp) {
		volume.setVolume(amp);
	}
	
	
	public void setWaveform(final float waveform) {
		this.waveform = waveform;
		sendMessage("inssel", waveform);
	}
	
	
	public void updateSettings(Context context, final SharedPreferences prefs) {
		updateSettings(context, prefs, "");
	}
	public void updateSettings(Context context, final SharedPreferences prefs, final String preset) {
		try {
			final float prefMidiMin = prefs.getInt(preset+PSND.MIDI_MIN, 70);
			final float prefMidiMax = prefs.getInt(preset+PSND.MIDI_MAX, 86);
			setMidiMin(prefMidiMin);
			setMidiMax(prefMidiMax);
			
			String defaultQuality = "1";//Launcher.getPhoneCPUPower(context) > Launcher.PRETTY_CRAP ? "1" : "0";
			defaultQuality = Launcher.getUIType() == Launcher.PHONE ? defaultQuality : "2";
//			Log.d("VisualQual", "presets have visual quality: "+prefs.contains(preset+PSND.VISUAL_QUALITY));
//			Log.d("VisualQual", "presets for visual quality: "+prefs.getString(preset+PSND.VISUAL_QUALITY, defaultQuality));
			String qual = defaultQuality;
			if (prefs.contains(preset+PSND.VISUAL_QUALITY)) {
				qual = prefs.getString(preset+PSND.VISUAL_QUALITY, defaultQuality);
			} else {
				Editor edit = prefs.edit();
				edit.putString(preset+PSND.VISUAL_QUALITY, qual);
				edit.commit(); //so the preference box sees our change.
			}
			final int prefsQual = Integer.parseInt(qual);
//			Log.d("VisualQual", "presets for visual quality (as int): "+prefsQual);
			setVisualQuality(prefsQual);

			
			final String s_waveform = prefs.getString(preset+PSND.WAVEFORM, "1.0");
			final Float waveform = Float.parseFloat(s_waveform);
			setWaveform(waveform);
			
			quantval = prefs.getString(preset+PSND.QUANTIZE, PSND.QUAT_CONTINUOUS);
			if (quantval.equalsIgnoreCase(PSND.QUAT_QUANTIZE)) {
				quantize = NQUANTIZE;
			} else if (quantval.equalsIgnoreCase(PSND.QUAT_SLIDE)) {
				quantize = NSLIDE;
			} else {
				quantize = NCONTINUOUS;
			}
			
			for (final Effect e : effects) {
				e.updateSettings(prefs, preset);
			}
			sequencer.updateSettings(prefs, preset);
		
		} catch (final Exception e) { e.printStackTrace(); }
	}
	public void updateSettingsFromJSON(JSONObject prefs) {
		updateSettingsFromJSON(prefs, false, null);
	}
	
	public void updateSettingsFromJSON(JSONObject prefs, boolean savetoshared, SharedPreferences sprefs) {
		try {
			Log.d("INSTRUMENT", "Settings changed!!!!!!!!!!!");
			Editor edit = sprefs.edit();
			final float prefMidiMin = prefs.has(PSND.MIDI_MIN) ? prefs.getInt(PSND.MIDI_MIN) : 70;
			if (savetoshared) edit.putInt(PSND.MIDI_MIN, (int)prefMidiMin);
			final float prefMidiMax = prefs.has(PSND.MIDI_MAX) ? prefs.getInt(PSND.MIDI_MAX) : 86;
			if (savetoshared) edit.putInt(PSND.MIDI_MAX, (int)prefMidiMax);
			setMidiMin(prefMidiMin);
			setMidiMax(prefMidiMax);
			
			
			final String s_waveform = prefs.has(PSND.WAVEFORM) ? prefs.getString(PSND.WAVEFORM) : "1.0";
			final Float waveform = Float.parseFloat(s_waveform);
			if (savetoshared) edit.putString(PSND.WAVEFORM, s_waveform);
			setWaveform(waveform);
			
			quantval = prefs.has(PSND.QUANTIZE) ?  prefs.getString(PSND.QUANTIZE) : PSND.QUAT_CONTINUOUS;
			if (quantval.equalsIgnoreCase(PSND.QUAT_QUANTIZE)) {
				quantize = NQUANTIZE;
			} else if (quantval.equalsIgnoreCase(PSND.QUAT_SLIDE)) {
				quantize = NSLIDE;
			} else {
				quantize = NCONTINUOUS;
			}
			if (savetoshared) edit.putString(PSND.QUANTIZE, quantval);

			
			
			for (final Effect e : effects) {
				e.updateSettingsFromJSON(prefs, savetoshared, edit);
			}
			
			if (savetoshared) edit.commit();
		
		} catch (final Exception e) { e.printStackTrace(); }
	}
	
	public JSONObject saveSettingsToJSON(JSONObject prefs) {
		try {
			prefs.put(PSND.MIDI_MIN, this.midiMin);
			prefs.put(PSND.MIDI_MAX, this.midiMax);
	
			prefs.put(PSND.WAVEFORM, this.waveform);

			prefs.put(PSND.QUANTIZE, this.quantval);
						
			
			for (final Effect e : effects) {
				prefs = e.saveSettingsToJSON(prefs);
			}	
			
			return prefs;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	
	
	
	public void cleanup() {
//		PdUtils.closePatch(patch);
		PdBase.closePatch(patch);
	}


}
