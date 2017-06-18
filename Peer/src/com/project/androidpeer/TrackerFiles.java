package com.project.androidpeer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.project.androidpeer.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class TrackerFiles extends Activity implements
		OnItemClickListener {
	ArrayList<String> trackerFileList;
	ListView listViewTrackerFiles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {		//once connected set view to activity_tracker_file_list.xml
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tracker_file_list);	
		listViewTrackerFiles = (ListView) findViewById(R.id.listViewTrackerFiles);

		startService(new Intent(getBaseContext(), ServerSocketThread.class)); 

		trackerFileList = getIntent()
				.getStringArrayListExtra("fromTrackerList");

		final StableArrayAdapter adapter = new StableArrayAdapter(
				TrackerFiles.this,
				android.R.layout.simple_list_item_1, trackerFileList);
		listViewTrackerFiles.setAdapter(adapter);			
		listViewTrackerFiles.setOnItemClickListener(this);				
	}
	
	@Override
	protected void onDestroy() {
		
		super.onDestroy();
	}

	private class StableArrayAdapter extends ArrayAdapter<String> {

		HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

		public StableArrayAdapter(Context context, int textViewResourceId,
				List<String> objects) {
			super(context, textViewResourceId, objects);
			Log.i("SharedActivity - ",
					"Inside StableArrayAdapter constructor");
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

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,		
			long id) {
		String itemName = parent.getItemAtPosition(position).toString();
		String peerIP = itemName.split(":")[0];
		String fileName = itemName.split(":")[1];
		ClientRxThread clientRxThread = new ClientRxThread(fileName, peerIP,
				9090);				//when we click on the file name to be downloaded a new thread is created
									//and the file name, server port and ip address is passed to it.

		clientRxThread.start();    //starting a client thread
	}

	private class ClientRxThread extends Thread {
		String dstFileName;
		String dstAddress;
		int dstPort;

		ClientRxThread(String fileName, String address, int port) {
			dstFileName = fileName;				//putting the destination file name, server port and address in variables to use
			dstAddress = address;
			dstPort = port;
		}

		@Override
		public void run() {
			Socket socket = null;

			try {
				Log.i("PEER TFLA", dstFileName + ":" + dstAddress + ":"
						+ dstPort);
				socket = new Socket(dstAddress, dstPort);			//creating a new socket for destination

				String filePath = (Environment.getExternalStorageDirectory() + "/Test/")		//setting path to the test folder in destination peer
						.trim() + dstFileName.trim();

																//Send the file name to the other peer
				try {
					ObjectOutputStream objectOutput = new ObjectOutputStream(
							socket.getOutputStream());
					try {
						objectOutput.writeObject(dstFileName);
					} catch (Exception e) {
						Log.e("Peer", e.getMessage());
					}
				} catch (IOException e) {
					System.out
							.println("The socket for reading the ObjectOutputStream has problem");
					e.printStackTrace();
				}

														//checks if socket is closed
				final boolean state = socket.isClosed();
				TrackerFiles.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {

//						Toast.makeText(getApplicationContext(),
//								"socket state: " + "state " + state,
//								Toast.LENGTH_LONG).show();
					}
				});

				
				try {
					ObjectInputStream objectInput = new ObjectInputStream(
							socket.getInputStream());		//create input stream for accepting the file
					Log.i("TFLA", "got OIS");
					try {
						Object object = objectInput.readObject();
						Log.i("TFLA", "read object");
						byte[] buf = (byte[]) object;		//putting it on buffer
						Log.i("TFLA", "object to buf");
						File file = new File(filePath);
						if (!file.exists()) {
							FileOutputStream fos = new FileOutputStream(file);
							Log.i("TFLA", "got FOS");
							fos.write(buf);			//write buffer to output stream
							Log.i("TFLA", "wrote buf to FOS");
							fos.close();			//close file stream
							Log.i("TFLA", "close FOS");
						}
						objectInput.close();
						Log.i("TFLA", "close OI");
					} catch (Exception e) {
						Log.e("Peer", e.getMessage());
					}
				} catch (IOException e) {
					System.out
							.println("PEER The socket for reading the ObjectInputStream has problem");
					e.printStackTrace();
				}

				socket.close();

				TrackerFiles.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {				//if success then we declare file transfer is finished
						Toast.makeText(TrackerFiles.this,
								"Downloaded", Toast.LENGTH_LONG).show();
					}
				});

			} catch (FileNotFoundException e) {

				e.printStackTrace();

				final String eMsg = "Peer TrackerFiles File wrong: " + e.getMessage();	//error message if problem with file
				TrackerFiles.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(TrackerFiles.this, eMsg,
								Toast.LENGTH_LONG).show();
					}
				});
			} catch (IOException e) {

				e.printStackTrace();

				final String eMsg = "Peer TrackerFiles Something wrong: "	//Error message if the tracker has a problem
						+ e.getMessage();
				TrackerFiles.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(TrackerFiles.this, eMsg,
								Toast.LENGTH_LONG).show();
					}
				});

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
}
