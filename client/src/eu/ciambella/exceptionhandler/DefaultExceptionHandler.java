package eu.ciambella.exceptionhandler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.util.Log;

public class DefaultExceptionHandler implements UncaughtExceptionHandler {

	static final String TAG = DefaultExceptionHandler.class.getSimpleName();
	
	private UncaughtExceptionHandler defaultExceptionHandler;

	public DefaultExceptionHandler(UncaughtExceptionHandler pDefaultExceptionHandler) {
		defaultExceptionHandler = pDefaultExceptionHandler;
	}

	@Override
	public void uncaughtException(final Thread t, final Throwable e) {
		// Debug
		Log.w(TAG, "Thread crashed: "+ t.hashCode());
		Log.w(TAG, "Uncaught exception receive", e);
		
		// Submit StackTraces
		new Thread() {
            @Override
            public void run() {
            	// Extract StackTraces
            	final Writer result = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(result);
                e.printStackTrace(printWriter);
                
                // Build parameter
                final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("package_name", ExceptionHandler.sAppPackage));
				nvps.add(new BasicNameValuePair("package_version", ExceptionHandler.sAppVersionName));
				nvps.add(new BasicNameValuePair("phone_model", ExceptionHandler.sDeviceModel));
				nvps.add(new BasicNameValuePair("android_version", ExceptionHandler.sAndroidVersion));
				nvps.add(new BasicNameValuePair("stacktrace", result.toString()));
                
            	// Send to server
				Log.d(TAG, "Try to send to server");
				try {
					final DefaultHttpClient httpClient = new DefaultHttpClient();
					final HttpPost httpPost = new HttpPost(ExceptionHandler.sServerUrl);
					httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
					final HttpResponse response = httpClient.execute(httpPost);
					if (response.getStatusLine().getStatusCode() == 200) {
						Log.i(TAG, "Send successfull");
						return;
					}
				} catch (Exception e) {
					Log.e(TAG, "Impossible to transmit exception");
				}
				
				// Store exception
				Log.d(TAG, "Try to store in local");
	            try {
	            	final Random generator = new Random();
	                final int random = generator.nextInt(99999);      
	                final String filename = ExceptionHandler.sAppVersionName +"-"+ Integer.toString(random);
	                final String path = ExceptionHandler.sStoragePath +"/"+ filename +".stacktrace";
	                
	                Log.d(TAG, "Writing unhandled exception to: "+ path);
	                 
	                BufferedWriter bos = new BufferedWriter(new FileWriter(path));
	                bos.write(ExceptionHandler.sAppPackage + "\n");
	                bos.write(ExceptionHandler.sAppVersionName + "\n");
	                bos.write(ExceptionHandler.sDeviceModel + "\n");
	                bos.write(ExceptionHandler.sAndroidVersion + "\n");
	                bos.write(result.toString());
	                bos.flush();
	                // Close up everything
	                bos.close();
	            } catch (Exception e) {
	            	Log.w(TAG, "Impossible to store this crash", e);
	            }
            }
		}.start();
		
		// Call original handler
		defaultExceptionHandler.uncaughtException(t, e);
	}
	
}