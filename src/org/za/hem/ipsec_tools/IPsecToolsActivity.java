package org.za.hem.ipsec_tools;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.app.Activity;
import android.content.ServiceConnection;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Context;
import android.os.IBinder;
import android.os.Binder;
import android.content.ComponentName;;


/*
 * Register
 * android.telephony.TelephonyManager.DATA_CONNECTED
 * android.telephony.TelephonyManager.DATA_DISCONNECTED
 * 
 * Context.getSystemService(Context.CONNECTIVITY_SERVICE).
 * CONNECTIVITY_ACTION
 */

/**
 * 
 * @author mikael
 *
 */

public class IPsecToolsActivity extends Activity {
	private File mBinDir;
	private TextView outputView;
	private Handler handler = new Handler();
	private boolean mIsBound;
	private NativeService mBoundService;
	
	/*
	public String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e(LOG_TAG, ex.toString());
	    }
	    return null;
*/
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.ipsec_tools_activity);
            mBinDir = getDir("bin", MODE_PRIVATE);
            putBinary("setkey");
            putBinary("setkey.sh");
            outputView = (TextView)findViewById(R.id.output);
            Button prefBtn = (Button) findViewById(R.id.pref_button);
            prefBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                            Intent settingsActivity = new Intent(getBaseContext(),
                                            Preferences.class);
                            startActivity(settingsActivity);
                    }
            });
            Button startBtn = (Button) findViewById(R.id.start_button);
            startBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                    	// TODO start service
                    	output("Starting VPN...");
                    	doBindService();
                    }
            });
            Button stopBtn = (Button) findViewById(R.id.stop_button);
            stopBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                    	// TODO start service
                    	output("Stopping VPN...");
                    	doUnbindService();
                    }
            });
    		output(ls(new String[]{mBinDir.getAbsolutePath()}));
    }
    
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
	        mBoundService = ((NativeService.NativeBinder)service).getService();
	        output("Connected");
	        // Tell the user about this for our demo.
//	        Toast.makeText(Binding.this, R.string.native_service_connected,
	//                Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        mBoundService = null;
	        output("Disconnected");
	  //      Toast.makeText(Binding.this, R.string.native_service_disconnected,
	    //            Toast.LENGTH_SHORT).show();
	    }
	};
	
	void doBindService() {
		
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
	    bindService(new Intent(IPsecToolsActivity.this, 
	            NativeService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}
	
	void doUnbindService() {
	    if (mIsBound) {
	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	    }
	}
	
	private String ls(String[] parameters) {
    	return system("/system/bin/ls", parameters);
    }
    
    /**
     * Copy binary file from assets into bin directory.
     */
    private void putBinary(String fileName) {
    	try {
    		File file = new File(mBinDir, fileName);
    		InputStream input = getAssets().open(fileName);
    		int read;
    		byte[] buffer = new byte[4096];
    		OutputStream output = new FileOutputStream(file);
    		
    		while ((read = input.read(buffer)) > 0) {
    			output.write(buffer, 0, read);
    		}
    		input.close();
    		output.close();
    		chmod(file, 711);
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * Set file mode
     * @param file File to modify
     * @param mode New file mode
     */
    private void chmod(File file, int mode) {
		system("/system/bin/chmod " + mode + " " + file.getAbsolutePath());
    }


    /**
     * Run system command wait for and return result
     * @param cmd System command to run
     * @return stdout
     */
    private String system(String cmd) {
    	try {
    		// Executes the command.
    		Process process = Runtime.getRuntime().exec(cmd);
        
    		// Reads stdout.
    		// NOTE: You can write to stdin of the command using
    		//       process.getOutputStream().
    		BufferedReader reader = new BufferedReader(
    				new InputStreamReader(process.getInputStream()));
    		int read;
    		char[] buffer = new char[4096];
    		StringBuffer output = new StringBuffer();
    		while ((read = reader.read(buffer)) > 0) {
    			output.append(buffer, 0, read);
    		}
    		reader.close();
        
    		// Waits for the command to finish.
    		process.waitFor();
        
    		return output.toString();
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	} catch (InterruptedException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    private String system(String prog, String[] parameters)
    {
    	StringBuffer buf = new StringBuffer(4096);
    	
    	buf.append(prog);
    	for ( String str : parameters ) {
    		buf.append(' ');
    		buf.append(str);
    	}
    	
    	return system(buf.toString());
    }
    
    private void output(final String str) {
    	Runnable proc = new Runnable() {
			public void run() {
				outputView.setText(str);
			}
    	};
    	handler.post(proc);
    }
}
