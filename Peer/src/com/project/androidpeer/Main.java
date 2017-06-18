package com.project.androidpeer;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import com.project.androidpeer.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {

	EditText editTextAddress;
	TextView textPort;
	
	public static Activity mActivity = null; 

	static final int SocketServerPORT = 8085; //This is the server socket port set to 8080
	ServerSocketThread serverSocketThread;	//creating a server thread
	ServerSocket serverSocket;	//creating a server socket

	private String IP;   //this will be used as the ip address for the server
	
	private static String mServerIP;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        
		mActivity = (Activity)this;
		
		editTextAddress = (EditText) findViewById(R.id.address);	
		textPort = (TextView) findViewById(R.id.port);
		textPort.setText("port: " + SocketServerPORT);	//changing the address as a port

		IP = getIpAddress();							//getting the IP address of the device using the function 

		startService(new Intent(getBaseContext(), ServerSocketThread.class));		//calling the startService function 
	}

	private String getIpAddress() {
		String ip = "";
		try {
			Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (enumNetworkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();			 	 	 	
				Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();		
				while (enumInetAddress.hasMoreElements()) {
					InetAddress inetAddress = enumInetAddress.nextElement();

					if (inetAddress.isSiteLocalAddress()) { 
						ip += inetAddress.getHostAddress() + "\n";		//concatinating the ip address
						ip.trim();
					}

				}

			}

		} catch (SocketException e) {
			e.printStackTrace();
			ip += "Peer Something Wrong! " + e.toString() + "\n";	
		}

		return ip;			//return the ip address
	}

	public void mySFiles(View view) {						//on click of the button on screen called my files it will display the list of files on that peer
		Intent i = new Intent(this, SharedActivity.class); 	 	 
		startActivity(i);									
	}

	public void Trackersend(View view) {
		mServerIP = editTextAddress.getText().toString();
		ClientRxThread clientRxThread = new ClientRxThread(mServerIP, SocketServerPORT); //creating a new thread with its ip address and socket port number

		clientRxThread.start();		//startng the client thread
	}
//----------------------------------------------------------------------------------------------------
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		
		Toast.makeText(mActivity, "TrackerList closed", Toast.LENGTH_LONG).show();
		ClientRxExitThread clientRxExitThread = new ClientRxExitThread(mServerIP, SocketServerPORT); //creating a new thread with its ip address and socket port number

		clientRxExitThread.start();
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private class ClientRxExitThread extends Thread {
		String dstAddress;										
		int dstPort;
		ArrayList<String> fromTrackerList;

		ClientRxExitThread(String address, int port) {
			dstAddress = address;				//setting the destination ip address and port number
			dstPort = port;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			Socket socket = null;

			try {
				socket = new Socket(dstAddress, dstPort);			//creating a new socket 

				// Sending the file to the tracker.
				try {
					ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream()); //crating an outputstream
					try {
						String exitMsg = "Disconnected: "+IP;
						objectOutput.writeObject(exitMsg);
						
						
						Main.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(Main.this, "Exit message sent.", Toast.LENGTH_LONG).show();		//The error message if not connected to tracker properly
							}
						});
						
						
					} catch (Exception e) {
						Log.e("Peer", e.getMessage());
					}
				} catch (IOException e) {
					System.out.println("The socket for reading the object has problem");
					e.printStackTrace();
				}

				socket.close();

			} catch (IOException e) {

				e.printStackTrace();

				final String eMsg = "Something wrong: " + e.getMessage();
				Main.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(Main.this, eMsg, Toast.LENGTH_LONG).show();		//The error message if not connected to tracker properly
					}
				});

			} finally {
				if (socket != null) {
					try {
						socket.close(); //closing the socket 
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	
	private class ClientRxThread extends Thread {
		String dstAddress;										
		int dstPort;
		ArrayList<String> fromTrackerList;

		ClientRxThread(String address, int port) {
			dstAddress = address;				//setting the destination ip address and port number
			dstPort = port;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			Socket socket = null;

			try {
				socket = new Socket(dstAddress, dstPort);			//creating a new socket 

				String path = Environment.getExternalStorageDirectory().toString() + "/Test";	//get access to the path of test folder in the other phone

				File f = new File(path);							
				final File[] fileList = f.listFiles();

				final ArrayList<String> list = new ArrayList<String>();
				for (int i = 0; i < fileList.length; i++) {
					String fileName = fileList[i].getName();
					list.add(IP + " : " + fileName);				//adding the list of files available on the server using ip and file name
				}

				// Sending the file to the tracker.
				try {
					ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream()); //crating an outputstream
					try {
						objectOutput.writeObject(list);
					} catch (Exception e) {
						Log.e("Peer", e.getMessage());
					}
				} catch (IOException e) {
					System.out.println("The socket for reading the object has problem");
					e.printStackTrace();
				}

				try {
					ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());
					try {
						Object object = objectInput.readObject();
						fromTrackerList = (ArrayList<String>) object;  //reading the list of files from the tracker
					} catch (Exception e) {
						Log.e("Peer", e.getMessage());
					}
				} catch (IOException e) {
					System.out.println("PEER The socket for reading the object has problem");
					e.printStackTrace();
				}

				socket.close();

				Main.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(Main.this, "Going to TrackerFiles", Toast.LENGTH_LONG).show(); //Displaying where the ui will lead us to

						Intent i = new Intent(Main.this, TrackerFiles.class);
						Log.d("Test", "fromTrackerList: " + fromTrackerList);
						i.putStringArrayListExtra("fromTrackerList", fromTrackerList);
						startActivityForResult(i, 1);
					}
				});

			} catch (IOException e) {

				e.printStackTrace();

				final String eMsg = "Something wrong: " + e.getMessage();
				Main.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(Main.this, eMsg, Toast.LENGTH_LONG).show();		//The error message if not connected to tracker properly
					}
				});

			} finally {
				if (socket != null) {
					try {
						socket.close(); //closing the socket 
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
