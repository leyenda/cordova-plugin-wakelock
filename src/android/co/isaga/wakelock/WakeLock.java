package co.isaga.wakelock;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.widget.TextView;
import android.annotation.SuppressLint;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import java.util.ArrayList;

/**
 * Plugin class which does the actual handling
 */
public class WakeLock extends CordovaPlugin {
	// As we only allow one wake-lock, we keep a reference to it here
	private PowerManager.WakeLock wakeLock = null;
	private PowerManager powerManager = null;
	private boolean releaseOnPause = true;

	private ArrayList<AlertDialog> dialogs = new ArrayList<AlertDialog>();
	/**
	 * Fetch a reference to the power-service when the plugin is initialized
	 */
	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		this.cordova = cordova;
		this.powerManager = (PowerManager) cordova.getActivity().getSystemService(Context.POWER_SERVICE);
	}

	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {

		PluginResult result = null;
		Log.d("PowerManagementPlugin", "Plugin execute called - " + this.toString() );
		Log.d("PowerManagementPlugin", "Action is " + action );

		try {
			if( action.equals("acquire") ) {
				if( args.length() > 0 && args.getBoolean(0) ) {
					Log.d("PowerManagementPlugin", "Only dim lock" );
					result = this.acquire( PowerManager.SCREEN_DIM_WAKE_LOCK );
				}
				else {
					result = this.acquire( PowerManager.FULL_WAKE_LOCK );
				}
			} else if( action.equals("release") ) {
				result = this.release();
			} else if( action.equals("setReleaseOnPause") ) {
				try {
					this.releaseOnPause = args.getBoolean(0);
					result = new PluginResult(PluginResult.Status.OK);
				} catch (Exception e) {
					result = new PluginResult(PluginResult.Status.ERROR, "Could not set releaseOnPause");
				}
			}
		}
		catch( JSONException e ) {
			result = new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
		}

		callbackContext.sendPluginResult(result);
		return true;
	}
	public synchronized void alert(final String message, final String title) {
		final CordovaInterface cordova = this.cordova;

		Runnable runnable = new Runnable() {
			public void run() {

				Builder dlg = createDialog(cordova); // new AlertDialog.Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
				dlg.setMessage(message);
				dlg.setTitle(title);
				dlg.setCancelable(true);

				changeTextDirection(dlg);
			};
		};
		this.cordova.getActivity().runOnUiThread(runnable);
	}

	/**
	 * Acquire a wake-lock
	 * @param p_flags Type of wake-lock to acquire
	 * @return PluginResult containing the status of the acquire process
	 */
	private PluginResult acquire( int p_flags ) {
		PluginResult result = null;

		if (this.wakeLock == null) {
			if (Build.MANUFACTURER.equals("Huawei") || Build.MANUFACTURER.equals("HUAWEI")) {
				this.alert(Build.MANUFACTURER, "PowerManagementPlugin");
				Log.d("PowerManagementPlugin", Build.MANUFACTURER);
				String tag = "LocationManagerService";
				this.wakeLock = this.powerManager.newWakeLock(1, tag);
				try {
					this.wakeLock.acquire(180*60*1000L /*180 minutes*/);
					result = new PluginResult(PluginResult.Status.OK);
				}
				catch( Exception e ) {
					this.wakeLock = null;
					result = new PluginResult(PluginResult.Status.ERROR,"Can't acquire wake-lock - check your permissions!");
				}
			}
		}
		else {
			result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION,"WakeLock already active - release first");
		}

		return result;
	}

	/**
	 * Release an active wake-lock
	 * @return PluginResult containing the status of the release process
	 */
	private PluginResult release() {
		PluginResult result = null;

		if( this.wakeLock != null ) {
			try {
				this.wakeLock.release();
				result = new PluginResult(PluginResult.Status.OK, "OK");
			}
			catch (Exception e) {
				result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION, "WakeLock already released");
			}

			this.wakeLock = null;
		}
		else {
			result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION, "No WakeLock active - acquire first");
		}

		return result;
	}

	@SuppressLint("NewApi")
	private Builder createDialog(CordovaInterface cordova) {
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			return new Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
		} else {
			return new Builder(cordova.getActivity());
		}
	}

	@SuppressLint("NewApi")
	private void changeTextDirection(Builder dlg){
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		dlg.create();
		AlertDialog dialog = dlg.show();
		dialogs.add(dialog);
		if (currentapiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			TextView messageview = (TextView)dialog.findViewById(android.R.id.message);
			messageview.setTextDirection(android.view.View.TEXT_DIRECTION_LOCALE);
		}
	}
}