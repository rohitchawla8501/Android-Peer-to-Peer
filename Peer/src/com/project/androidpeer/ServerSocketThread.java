package com.project.androidpeer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class ServerSocketThread extends Service {
	ServerThread serverThread;						//declaring the socket and server thread
	ServerSocket serverSocket;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Toast.makeText(this, "P2P Applciation Started", Toast.LENGTH_LONG).show(); //when the application is created, we display that server has started
		serverThread = new ServerThread();
		serverThread.start();   //starting the p2p sever 
	}

	private class ServerThread extends Thread {

		@Override
		public void run() {
			Socket socket = null;

			try {
				serverSocket = new ServerSocket(9090);			//creating a new server thread om port 9090

				Log.i("PEER SST", "Waiting on " + 9090);		//displaying the port number on the server UI

				while (true) {
					socket = serverSocket.accept();
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

	private class FileTxThread extends Thread {
		Socket socket;

		FileTxThread(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			String fileName = " ";
			
																//Getting the file name from peer
			try {
				ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());
				try {
					fileName = (String) objectInput.readObject();			//typecasting the filename to string
					Log.i("PEER SST", fileName);

				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				System.out.println("The socket for reading the ObjectInputStream has problem");
				e.printStackTrace();
			}

															//checking if socket is closed 
			if (socket.isClosed()) {
				Log.i("PEER SST", "socket state " + socket.isClosed());
			}
			
				//sending the requested file to the other peer
			try {
				ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
				Log.i("Peer SST", "read OOS");
				try {
					String filePath = (Environment.getExternalStorageDirectory() + "/Test/").trim()  //setting file path to external/internal storage and pointing to the test directory
							+ fileName.trim();
					Log.i("Peer SST", "actual filename with path " + filePath);

					File file = new File(filePath);
					if (file.exists()) {										
						Log.i("Peer SST", "file exists");							
						FileInputStream fis = new FileInputStream(file);			//creating an input stream
						Log.i("Peer SST", "got FIS");
						ByteArrayOutputStream bos = new ByteArrayOutputStream();	//creating and Byte array output stream to write to buffer
						Log.i("Peer SST", "got BOS");
						byte[] buf = new byte[1024];
						try {
							for (int readNum; (readNum = fis.read(buf)) != -1;) {		//writing bytes to the buffer
								bos.write(buf, 0, readNum);
								System.out.println("read " + readNum + " bytes,");	
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						byte[] content = bos.toByteArray();							//concatinating the content of the array
						objectOutput.writeObject(content);				//writing object on the output stream
						Log.i("Peer SST", "wrote content");
					}
					objectOutput.close();
				} catch (Exception e) {
					Log.e("Peer", e.getMessage());
				}
			} catch (IOException e) {
				System.out.println("The socket for reading the ObjectOutputStream has problem");
				e.printStackTrace();
			}

			try {
				socket.close();				//closing the socket
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
