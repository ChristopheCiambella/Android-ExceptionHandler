package eu.ciambella.exceptionhandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class ExceptionHandler {

	static final String TAG = ExceptionHandler.class.getSimpleName();
	static final String UNKNOWN = "unknown";

	static String sServerUrl = null;
	static String sStoragePath = null;
	static String sAppVersionName = UNKNOWN;
	static String sAppVersionCode = UNKNOWN;
	static String sAppPackage = UNKNOWN;
	static String sDeviceModel = UNKNOWN;
	static String sAndroidVersion = UNKNOWN;

	/**
	 * Register handler for unhandled exceptions.
	 * 
	 * @param context
	 * @param Url
	 */
	public static void register(Context context, String url) {
		sServerUrl = url;

		// Get information about the Package
		final PackageManager pm = context.getPackageManager();
		try {
			// Get package informations
			final PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);

			// Get storage informations
			sStoragePath = context.getFilesDir().getAbsolutePath();

			// Get application informations
			sAppVersionName = pi.versionName;
			sAppVersionCode = String.valueOf(pi.versionCode);
			sAppPackage = pi.packageName;

			// Get device informations
			sDeviceModel = android.os.Build.MODEL;
			sAndroidVersion = android.os.Build.VERSION.RELEASE;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Impossible to grab application informations", e);
		}

		// Manage Uncaught Exception Handler
		final UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
		if (currentHandler != null) {
			Log.d(TAG, "current handler class=" + currentHandler.getClass().getName());
		}
		if (!(currentHandler instanceof DefaultExceptionHandler)) {
			Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler(currentHandler));
		}

		// Send stored exception
		sendStoredStackTraces();
	}

	public static void sendStoredStackTraces() {
		new Thread() {
			@Override
			public void run() {
				try {
					// Open stored path
					Log.d(TAG, "Looking for exceptions in: " + sStoragePath);
					final File dir = new File(sStoragePath + "/");
					dir.mkdir();

					// Search files
					final FilenameFilter filter = new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.endsWith(".stacktrace");
						}
					};
					final String[] exceptionList = dir.list(filter);
					Log.d(TAG, "Found " + exceptionList.length + " stacktrace(s)");

					// Send
					for (int i = 0; i < exceptionList.length; i++) {
						final String filePath = sStoragePath + "/" + exceptionList[i];
						final String version = exceptionList[i].split("-")[0];
						Log.d(TAG, "Stacktrace in file '" + filePath + "' belongs to version " + version);
						final StringBuilder contents = new StringBuilder();
						final BufferedReader input = new BufferedReader(new FileReader(filePath));
						String line = null;
						String androidVersion = null;
						String phoneModel = null;
						while ((line = input.readLine()) != null) {
							if (androidVersion == null) {
								androidVersion = line;
								continue;
							} else if (phoneModel == null) {
								phoneModel = line;
								continue;
							}
							contents.append(line);
							contents.append(System.getProperty("line.separator"));
						}
						input.close();
						String stacktrace;
						stacktrace = contents.toString();
						Log.d(TAG, "Transmitting stack trace: " + stacktrace);
						// Transmit stack trace with POST request
						DefaultHttpClient httpClient = new DefaultHttpClient();
						HttpPost httpPost = new HttpPost(sServerUrl);
						List<NameValuePair> nvps = new ArrayList<NameValuePair>();
						nvps.add(new BasicNameValuePair("package_name", sAppPackage));
						nvps.add(new BasicNameValuePair("package_version", version));
						nvps.add(new BasicNameValuePair("phone_model", phoneModel));
						nvps.add(new BasicNameValuePair("android_version", androidVersion));
						nvps.add(new BasicNameValuePair("stacktrace", stacktrace));
						httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
						// We don't care about the response, so we just hope it
						// went well and on with it
						HttpResponse response = httpClient.execute(httpPost);
						if (response.getStatusLine().getStatusCode() == 200) {
							Log.i(TAG, "Send successfull");

							File file = new File(filePath);
							file.delete();
						} else {
							Log.e(TAG, "Send fail");
						}
					}

				} catch (Exception e) {
					Log.w(TAG, "Impossible to send stored exception");
				}
			}
		}.start();
	}

}