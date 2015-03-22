//MainActivity.java
//Manages your favorite Flickr searches
//access and display in web browser
package com.power.flickrsearch;

import java.util.ArrayList;
import java.util.Collections;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ListActivity {
	
	//name of SharedPreferences XML file that stores saved searches
	private static final String SEARCHES = "searches";
	
	//edit text where user enters query
	private EditText queryEditText;
	//edit text where user enters tag
	private EditText tagEditText;
	//user's favorite searches
	private SharedPreferences savedSearches;
	//list of tags for saved searches
	private ArrayList<String> tags;
	//bings tags to listView
	private ArrayAdapter<String> adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//get references to text edits
		queryEditText = (EditText) findViewById(R.id.queryEditText);
		tagEditText = (EditText) findViewById(R.id.tagEditText);
		
		//get the sharedpreferences containing user's saved searches
		savedSearches = getSharedPreferences(SEARCHES, MODE_PRIVATE);
		
		//store saved tags in an array list, then sort them
		tags = new ArrayList<String>(savedSearches.getAll().keySet());
		Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);
		
		//create arrayadapter and use it to bind tags to the listview
		adapter = new ArrayAdapter<String>(this, R.layout.list_item, tags);
		setListAdapter(adapter);
		
		//register listener to save a new or edited search
		ImageButton saveButton = (ImageButton) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(saveButtonListener);
		
		//register listener that searches Flickr when user touches a tag
		getListView().setOnItemClickListener(itemClickListener);
		
		//register listener that allows user to edit/dete a search
		getListView().setOnItemLongClickListener(itemLongClickListener);
		
	}//end onCreate

	
	//saveButtonListener saves a tag-query pair into SharedPreferences
	public OnClickListener saveButtonListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			//create tag if neither queryEditText or tagEditText is empty
			if(queryEditText.getText().length() > 0 
					&& tagEditText.getText().length() > 0)
			{
				addTaggedSearch(queryEditText.getText().toString(), tagEditText.getText().toString());
				//clear queryEditText and tagEditText
				queryEditText.setText("");
				tagEditText.setText("");
				
				((InputMethodManager) getSystemService(
						Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
								tagEditText.getWindowToken(), 0);
				
			} 
			else //prompt user to provide query and tag
			{
				//create a new AlertDialog builder
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				
				//set dialog's title and message to display
				builder.setMessage(R.string.missingMessage);
				
				//provide an Ok button to dismiss dialog
				builder.setPositiveButton(R.string.OK, null);
				
				//create AlertDialog from AlertDialog builder
				AlertDialog errorDialog = builder.create();
				errorDialog.show();
				
			}//end if/else
		}//end onClick
		
	};//end onClickListener 
	
	//add new search to teh save file, then refresh all the buttons
	private void addTaggedSearch(String query, String tag)
	{
		//get a sharedpreference.editor to store new tag/query pair
		SharedPreferences.Editor preferencesEditor = savedSearches.edit();
		//store current search
		preferencesEditor.putString(tag, query);
		//store updated preferences
		preferencesEditor.apply();
		
		//if tag is new, add to and sort tags, then display updated list
		if(!tags.contains(tag))
		{
			tags.add(tag);
			Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);
			//rebind tags to ListView
			adapter.notifyDataSetChanged();
		}
	}//end addTaggedSearch
	
	//itemClickListener launches web browser to display search results
	OnItemClickListener itemClickListener = new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			//get query string and create a url representing search
			String tag = ((TextView) view).getText().toString();
			String urlString = getString(R.string.searchURL) + 
					Uri.encode(savedSearches.getString(tag, ""), "UTF-8");
			
			//create an intent to launch web browser
			Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
			
			//launch web browser
			startActivity(webIntent);
		}
	};//end onItemClick
	
	//itemLongClickListener displays dialog allowing user to delete or edit saved search
	OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener()
	{
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
		{
			//get tag that user long touched
			final String tag = ((TextView) view).getText().toString();
			
			//create a new AlertDialog
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			
			//set AlertDialog's title
			builder.setTitle(getString(R.string.shareEditDeleteTitle, tag));
			
			//set list of items to display in dialog
			builder.setItems(R.array.dialog_items, new DialogInterface.OnClickListener()
			{
				//responds to user touch by sharing, editing, deleting saved search
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					switch(which)
					{
					case 0: //share
						shareSearch(tag);
						break;
					case 1: //edit
						//set EditTexts to match chose tag and query
						tagEditText.setText(tag);
						queryEditText.setText(savedSearches.getString(tag, ""));
						break;
					case 2: //delete
						deleteSearch(tag);
						break;
					}//end switch
				}//end onClick
			}//
			);//end builder setItems
			
			//set AlertDialog's negative button
			builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
			{
				//called when cancel button is clicked
				public void onClick(DialogInterface dialog, int id)
				{
					//dismiss alertdialog
					dialog.cancel();
				}
			}
			); //end set negative button
			
			builder.create().show();
			return true;
			
		}//end onitemlongclick
	};//end onitemlongclicklistener 
	
	//allows user to share saved search
	private void shareSearch(String tag)
	{
		//create url representing search
		String urlString = getString(R.string.searchURL) +
				Uri.encode(savedSearches.getString(tag, ""), "UTF-8");
		
		//create intent to share urlString
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shareSubject));
		shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.shareMessage, urlString));
		shareIntent.setType("text/plain");
		
		//display apps that can share
		startActivity(Intent.createChooser(shareIntent, getString(R.string.shareSearch)));
	}
	
	//deletes a search after user confirms delete operation
	private void deleteSearch(final String tag)
	{
		//create a new AlertDialog
		AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
		
		//set alertDialog's message
		confirmBuilder.setMessage(getString(R.string.confirmMessage, tag));
		
		//set the AlertDialog's negative button
		confirmBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
		{
			//called when cancel button is click
			public void onClick(DialogInterface dialog, int id)
			{
				//dismisses dialog
				dialog.cancel();
			}
		}		
		);//end call to setNegativeButton
		
		//set alertdialog's positive button
		confirmBuilder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener()
		{
			//called when user clicks ok
			public void onClick(DialogInterface dialog, int id)
			{
				//removes tag from tags
				tags.remove(tag);
				
				//get sharedpreferences editor to remove saved search
				SharedPreferences.Editor preferencesEditor = savedSearches.edit();
				//remove search
				preferencesEditor.remove(tag);
				//save changes
				preferencesEditor.apply();
				
				//rebind tags ArrayList to ListView to show updated list
				adapter.notifyDataSetChanged();
			}
		}//end onClickListener
		);//end setPositiveButton
	
		confirmBuilder.create().show();
		
	
	}//end deleteSearch
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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
}
