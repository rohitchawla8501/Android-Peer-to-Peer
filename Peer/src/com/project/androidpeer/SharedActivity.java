package com.project.androidpeer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.project.androidpeer.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class SharedActivity extends Activity {

	ListView listViewMyShareFiles;							

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_shared_files); 	//setting the view from the activity_my_shared_files.xml

		listViewMyShareFiles = (ListView) findViewById(R.id.listViewMyShareFiles); 		//showing available contents as a list 

		String path = Environment.getExternalStorageDirectory().toString() 
				+ "/Test";		//setting path as the test folder of tghe other peer
		Toast.makeText(getApplicationContext(), path, Toast.LENGTH_LONG).show();   

		File f = new File(path);
		final File[] fileList = f.listFiles();

		final ArrayList<String> list = new ArrayList<String>();	//adding file lists to ui from the arraylist of files
		for (int i = 0; i < fileList.length; i++) {					
			String fileName = fileList[i].getName();				
			System.out
					.println("inside for loop of converting values of i to string and splitting"); 
			System.out.println("value of i" + fileName);
			list.add(fileName);		//add file name
			Log.i("SharedActivity - ", "Added data to list " + fileName);
		}

		final StableArrayAdapter adapter = new StableArrayAdapter(this,		
				android.R.layout.simple_list_item_1, list);   //call the stable array adapter  function to create a hash map of objects
		listViewMyShareFiles.setAdapter(adapter);    
	}

	private class StableArrayAdapter extends ArrayAdapter<String> {

		HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

		public StableArrayAdapter(Context context, int textViewResourceId,
				List<String> objects) {
			super(context, textViewResourceId, objects);
			Log.i("SharedFiles - ",
					"Inside StableArrayAdapter constructor");
			for (int i = 0; i < objects.size(); ++i) {
				mIdMap.put(objects.get(i), i);
			}
		}

		@Override
		public long getItemId(int position) {
			String item = getItem(position);		//getting item id from position
			return mIdMap.get(item);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

	}
}
