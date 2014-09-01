package eu.ciambella.exceptionhandler.test;

import eu.ciambella.exceptionhandler.ExceptionHandler;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class TestActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ExceptionHandler.register(this, "http://stacktrace.file.link/stacktrace.php");
		
		setContentView(R.layout.test);
		
		findViewById(R.id.nullpointer).setOnClickListener(new OnClickListener() {
			@SuppressWarnings("null")
			@Override
			public void onClick(View v) {
				Bundle bundle = null;
				bundle.get("AH");
			}
		});
		
		findViewById(R.id.byzero).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				@SuppressWarnings("unused")
				final int nbr = 42 / 0;
			}
		});
		
		findViewById(R.id.intent).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				startActivity(intent);
			}
		});
		
		findViewById(R.id.outofbound).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				byte by[] = new byte[42];
				by[84] = 42;
			}
		});
		
		findViewById(R.id.throwex).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				throw new RuntimeException("BOOOOHHH !");
			}
		});
	}
	
}
