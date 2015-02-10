package com.boomgaarden_corney.android.orientation;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class OrientationMainActivity extends Activity implements
		SensorEventListener {

	private final String DEBUG_TAG = "DEBUG_ORIENTATION";
	private final String SERVER_URL = "http://54.86.68.241/orientation/test.php";

	private TextView txtResults;
	private SensorManager sensorManager;

	private String errorMsg;

	private float orientationAccuracy;
	private float orientationValue0;
	private float orientationValue1;
	private float orientationValue2;
	private float orientationMaxRange = 0;
	private float orientationPower = 0;
	private float orientationResolution = 0;
	private int  orientationSensorType;
	private int numOrientationChanges = 0;
	private int orientationVersion = 0;
	private int orientationHashCode = 0;
	private Sensor mOrientation;
	private long orientationTimeStamp;
	private String orientationVendor;

	private List<NameValuePair> paramsDevice = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsOrientation = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsSensor = new ArrayList<NameValuePair>();


	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_orientation_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);

		// Setup Orientation Manager and Provider
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mOrientation = sensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		

		setDeviceData();
		showDeviceData();
		sendDeviceData();

		if (mOrientation == null){
			setErrorMsg("No Orientation Detected");
			showErrorMsg();
			sendErrorMsg();
		} else{
			setSensorData();
			showSensorData();
			sendSensorData();
		}
		

	}

	/* Request Orientation updates at startup */
	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);

	}

	/* Remove the Orientationlistener updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (mOrientation != null) {
			if ((event.sensor.getType() == mOrientation.getType()) && numOrientationChanges < 10) {
				
				numOrientationChanges++;
				setOrientationData(event);
				showOrientationData();
				sendOrientationData();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.orientation_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("DEVICE")) {
			writer.write(buildPostRequest(paramsDevice));
		} else if (postParameters.equals("ORIENTATION")) {
			writer.write(buildPostRequest(paramsOrientation));
			paramsOrientation = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		}  else if (postParameters.equals("SENSOR")) {
			writer.write(buildPostRequest(paramsSensor));
			paramsSensor = new ArrayList<NameValuePair>();
		}

		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();
		
		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Orientation
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}
	}

	private void setDeviceData() {
		paramsDevice.add(new BasicNameValuePair("Device", Build.DEVICE));
		paramsDevice.add(new BasicNameValuePair("Brand", Build.BRAND));
		paramsDevice.add(new BasicNameValuePair("Manufacturer",
				Build.MANUFACTURER));
		paramsDevice.add(new BasicNameValuePair("Model", Build.MODEL));
		paramsDevice.add(new BasicNameValuePair("Product", Build.PRODUCT));
		paramsDevice.add(new BasicNameValuePair("Board", Build.BOARD));
		paramsDevice.add(new BasicNameValuePair("Android API", String
				.valueOf(Build.VERSION.SDK_INT)));
	}

	private void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}

	private void setOrientationData(SensorEvent orientation) {
		orientationAccuracy = orientation.accuracy;
		orientationSensorType = orientation.sensor.getType();
		orientationTimeStamp = orientation.timestamp;
		orientationValue0 = orientation.values[0];
		orientationValue1 = orientation.values[1];
		orientationValue2 = orientation.values[2];
		orientationHashCode = orientation.hashCode();

		paramsOrientation.add(new BasicNameValuePair("Orientation Update Count",
				String.valueOf(numOrientationChanges)));
		paramsOrientation.add(new BasicNameValuePair("Accuracy", String
				.valueOf(orientationAccuracy)));
		paramsOrientation.add(new BasicNameValuePair("Sensor Type", String
				.valueOf(orientationSensorType)));
		paramsOrientation.add(new BasicNameValuePair("Time Stamp", String
				.valueOf(orientationTimeStamp)));
		paramsOrientation.add(new BasicNameValuePair(
				"Value 0 Azimuth angle around the z-axis", String
						.valueOf(orientationValue0)));
		paramsOrientation.add(new BasicNameValuePair(
				"Value 1 Pitch angle around the x-axis", String
						.valueOf(orientationValue1)));
		paramsOrientation.add(new BasicNameValuePair(
				"Value 2 Roll angle around the y-axis", String
						.valueOf(orientationValue2)));
		paramsOrientation.add(new BasicNameValuePair(
				"Hash Code Value", String
						.valueOf(orientationHashCode)));
	}
	
	private void setSensorData() {
		orientationMaxRange = mOrientation.getMaximumRange();
		orientationPower = mOrientation.getPower();
		orientationResolution = mOrientation.getResolution();
		orientationVendor = mOrientation.getVendor();
		orientationVersion = mOrientation.getVersion();		
		
		paramsSensor.add(new BasicNameValuePair("Max Range", String
						.valueOf(orientationMaxRange)));
		paramsSensor.add(new BasicNameValuePair("Power", String
				.valueOf(orientationPower)));
		paramsSensor.add(new BasicNameValuePair("Resolution", String
				.valueOf(orientationResolution)));
		paramsSensor.add(new BasicNameValuePair("Vendor", String
				.valueOf(orientationVendor)));
		paramsSensor.add(new BasicNameValuePair("Version", String
				.valueOf(orientationVersion)));
	}

	private void showDeviceData() {
		// Display and store (for sending via HTTP POST query) device
		// information
		txtResults.append("Device: " + Build.DEVICE + "\n");
		txtResults.append("Brand: " + Build.BRAND + "\n");
		txtResults.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		txtResults.append("Model: " + Build.MODEL + "\n");
		txtResults.append("Product: " + Build.PRODUCT + "\n");
		txtResults.append("Board: " + Build.BOARD + "\n");
		txtResults.append("Android API: "
				+ String.valueOf(Build.VERSION.SDK_INT) + "\n");

		txtResults.append("\n");

	}

	private void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void showOrientationData() {
		StringBuilder results = new StringBuilder();

		results.append("Orientation Update Count: "
				+ String.valueOf(numOrientationChanges) + "\n");
		results.append("Orientation Accuracy: " + String.valueOf(orientationAccuracy) + "\n");
		results.append("Orientation Sensor Type: " + String.valueOf(orientationSensorType) + "\n");
		results.append("Orientation Time Stamp: " + String.valueOf(orientationTimeStamp) + "\n");
		results.append("Orientation Vaule 0 (X axis): " + String.valueOf(orientationValue0) + "\n");
		results.append("Orientation Vaule 1 (Y axis): " + String.valueOf(orientationValue1) + "\n");
		results.append("Orientation Vaule 2 (Z axis): " + String.valueOf(orientationValue2) + "\n");
		results.append("Orientation Hash Code " + String.valueOf(orientationHashCode) + "\n");
		
		txtResults.append(new String(results));
		txtResults.append("\n");
	}
	
	private void showSensorData() {
		StringBuilder results = new StringBuilder();
		
		results.append("Max Range: " + String.valueOf(orientationMaxRange) + "\n");
		results.append("Power: " + String.valueOf(orientationPower) + "\n");
		results.append("Resolution: " + String.valueOf(orientationResolution) + "\n");
		results.append("Vendor: " + String.valueOf(orientationVendor) + "\n");
		results.append("Version: " + String.valueOf(orientationVersion) + "\n");
		
		txtResults.append(new String(results));
		txtResults.append("\n");
	}

	private void sendDeviceData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Orientation info
			new SendHttpRequestTask().execute(SERVER_URL, "DEVICE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Orientation info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendOrientationData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Orientation info
			new SendHttpRequestTask().execute(SERVER_URL, "ORIENTATION");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendSensorData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Orientation info
			new SendHttpRequestTask().execute(SERVER_URL, "SENSOR");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

}
