package com.project.androidtracker;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	TextView infoIp, infoPort;

	static final int SocketServerPORT = 8085;		//set server port to 8085
	ServerSocket serverSocket;					//create a socket
	
	public static boolean isPeerDisconnected = false;
	
	public static Socket socket = null;
	
	public static HashSet<String> mUniquePeersList = new HashSet<String>();

	ServerSocketThread serverSocketThread;
	
	Context mContext;

	ArrayList<String> peerFileList;					
	static ArrayList<String> viewFileList = new ArrayList<String>();;
	ListView listViewPeerFiles;			

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_main);					//setting view from activity_main.xml
		infoIp = (TextView) findViewById(R.id.infoip);				//storing ip of tracker
		infoPort = (TextView) findViewById(R.id.infoport);			//storing port number of tracker

		infoIp.setText(getIpAddress());				//showing the ip address

		listViewPeerFiles = (ListView) findViewById(R.id.listViewPeerFiles); //list the shared files available

		serverSocketThread = new ServerSocketThread();
		serverSocketThread.start(); //starting the thread
	}

	@Override
	protected void onDestroy() {  //closing socket once app is hut down
		super.onDestroy();

		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		
		int id = item.getItemId();
		
		switch (id) {
		case R.id.peers:
//			if (socket == null) {
//				return false;
//			}
//			
//			mUniquePeersList.clear();
//			
//			FileTxThread fileTxThread = new FileTxThread(socket);
//			fileTxThread.start();
//			try {
//				fileTxThread.join();
//			} catch(Exception ex) {
//				
//			}
			
			MainActivity.this.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), "Peer connected: " + mUniquePeersList.size(), Toast.LENGTH_LONG).show();
				}
			});
			break;

		default:
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	private String getIpAddress() {  //getting ip address, same as peer 
		String ip = "";
		try {
			Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (enumNetworkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
				Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
				while (enumInetAddress.hasMoreElements()) {
					InetAddress inetAddress = enumInetAddress.nextElement();

					if (inetAddress.isSiteLocalAddress()) {
						ip += "SiteLocalAddress: " + inetAddress.getHostAddress() + "\n";
					}

				}

			}

		} catch (SocketException e) {
			e.printStackTrace();
			ip += "Something Wrong! " + e.toString() + "\n";
		}

		return ip;
	}

	public class ServerSocketThread extends Thread {

		@Override
		public void run() {
			

			try {
				serverSocket = new ServerSocket(SocketServerPORT);
				MainActivity.this.runOnUiThread(new Runnable() {	

					@Override
					public void run() {
						infoPort.setText("Connect with me on: " + serverSocket.getLocalPort());  //displaying ip address of server
					}
				});

				while (true) {
					socket = serverSocket.accept();							//accept connection and start a file thread
					
					FileTxThread fileTxThread = new FileTxThread(socket);
					fileTxThread.start();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	private class StableArrayAdapter extends ArrayAdapter<String> {  //displaying the files available

		HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

		public StableArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
			super(context, textViewResourceId, objects);
			for (int i = 0; i < objects.size(); ++i) {
				mIdMap.put(objects.get(i), i);
			}
		}

		@Override
		public long getItemId(int position) {
			String item = getItem(position);
			return mIdMap.get(item);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}
	}

	public class FileTxThread extends Thread {
		Socket socket;

		FileTxThread(Socket socket) {
			this.socket = socket;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
																// Accept file list from peer
			try {
				ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());	
				try {
					final Object object = objectInput.readObject();
					
					try {
						peerFileList = new ArrayList<String>();
						peerFileList = (ArrayList<String>) object;
					} catch (Exception ex) {
						Log.d("Test", "Exception: " + ex.toString());
						MainActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								String msg = (String)object;
								mUniquePeersList.remove(msg.split(":")[1].trim());
								Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
								isPeerDisconnected = true;
							}
						});
					}
					
					if (isPeerDisconnected) {
						isPeerDisconnected = false;
						socket.close();
						return;
					}
					
					Iterator<String> i = peerFileList.iterator();
					
					while (i.hasNext()) {
						String file = i.next();
						mUniquePeersList.add(file.split(":")[0].trim());
						Log.d("Test", "File: " + file);
						viewFileList.add(file);
					}
					Log.i("Tracker", viewFileList.size() + " ");
					

					MainActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), "New Peer connected", Toast.LENGTH_LONG).show();
							final StableArrayAdapter adapter = new StableArrayAdapter(MainActivity.this,
									android.R.layout.simple_list_item_1, viewFileList);
							listViewPeerFiles.setAdapter(adapter);
						}
					});

				} catch (ClassNotFoundException e) {
					Log.d("Test", "ObjectInputStream - ClassNotFoundException: " + e.toString());
					System.out.println("The title list has not come from the server");
					e.printStackTrace();
				} catch (Exception e) {
					Log.d("Test", "ObjectInputStream - Exception: " + e.toString());
					Log.e("Client", e.getMessage());
				}
			} catch (IOException e) {
				Log.d("Test", "ObjectInputStream - IOException: " + e.toString());
				System.out.println("The socket for reading the object has problem");
				e.printStackTrace();
			}

			// Send file list to peer
			try {
				ArrayList<String> my = new ArrayList<String>();
				for (int i = 0; i < viewFileList.size(); i++) {
					my.add(viewFileList.get(i));
				}
				
				try {
					ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
					objectOutput.writeObject(my);
				} catch (IOException e) {
					e.printStackTrace();
				}
				socket.close();

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					socket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	}
}
