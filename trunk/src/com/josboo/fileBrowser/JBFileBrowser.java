package com.josboo.fileBrowser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class JBFileBrowser extends Activity {
	private String root = "/";
	private String currentDirectory = root;
	private String searchString;
	private String searchDirectory;
	private int MAX_DIRECTORY_TEXT_LENGTH;
	private int listViewSavedPosition = -1;
	private int listViewSavedListTop;
	private boolean fileToCopy = false;
	private boolean cutFile = false;
	private ArrayList<MyFile> results = new ArrayList<MyFile>();
	private ArrayList<File> searchResultFiles = new ArrayList<File>();
	private MyFile contextMenuFile;
	private File copyFile;
	
	private ProgressDialog mProgressDialog;
	private TextView currentDirectoryTextView;
	private ListView fileListView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setupActivity();
    }
    
    public void setupActivity(){
    	if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
    		MAX_DIRECTORY_TEXT_LENGTH = 35;
    	} else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
    		MAX_DIRECTORY_TEXT_LENGTH = 75;
    	}
    	
        currentDirectoryTextView = (TextView) findViewById(R.id.currentLocationTextView);
        currentDirectoryTextView.setMaxEms(MAX_DIRECTORY_TEXT_LENGTH/2);
        currentDirectoryTextView.setMinEms(MAX_DIRECTORY_TEXT_LENGTH/2);
	    registerForContextMenu(findViewById(R.id.fileListView));
        getDirectory(null, currentDirectory);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	setContentView(R.layout.main);
    	
    	setupActivity();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	super.onSaveInstanceState(savedInstanceState);
    	
    	listViewSavedPosition = fileListView.getFirstVisiblePosition();
        View firstVisibleView = fileListView.getChildAt(0);
        listViewSavedListTop = (firstVisibleView == null) ? 0 : firstVisibleView.getTop();

    	savedInstanceState.putInt("listViewSavedPosition", listViewSavedPosition);
    	savedInstanceState.putInt("listViewSavedListTop", listViewSavedListTop);
    	savedInstanceState.putString("currentDir", currentDirectory);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	listViewSavedPosition = savedInstanceState.getInt("listViewSavedPosition");
    	listViewSavedListTop = savedInstanceState.getInt("listViewSavedListTop");
    	currentDirectory = savedInstanceState.getString("currentDir");
  		setupActivity();               
        fileListView.setSelectionFromTop(listViewSavedPosition, listViewSavedListTop);
    }
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        if (fileToCopy == false)
            menu.getItem(2).setEnabled(false);
        else
            menu.getItem(2).setEnabled(true);
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.browser_menu, menu);
        return true;
    }
    
    @Override  
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) { 
    	super.onCreateContextMenu(menu, v, menuInfo);
    	
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        
        contextMenuFile = results.get(info.position);

        menu.setHeaderTitle(contextMenuFile.getFileName());  
        
        if (!contextMenuFile.isDirectory()){
	        menu.add(0, v.getId(), 0, "Open with...");
        }
        
        menu.add(0, v.getId(), 1, "Copy");
        menu.add(0, v.getId(), 2, "Cut");

        if (fileToCopy){
	        menu.add(0, v.getId(), 3, "Paste");
        }
        
        menu.add(0, v.getId(), 4, "Rename");
        menu.add(0, v.getId(), 5, "Delete");
    }
    
    @Override  
    public boolean onContextItemSelected(MenuItem item) {  
    	switch (item.getOrder()){
    	case 0:
            openFileWith();
        	break;
    	case 1:
            copyFile();
        	break;
    	case 2:
            cutFile();
        	break;
    	case 3:
        	pasteFile();
        	break;
    	case 4:
            renameFile();
        	break;
    	case 5:
            deleteFile();
        	break;
        default:
        	return false;
    	}
        return true;  
    }
    
    boolean recursiveDelete(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File file : fileOrDirectory.listFiles())
            	recursiveDelete(file);

        return fileOrDirectory.delete();
    }

	private void deleteFile() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		
		alert.setTitle("Confirm Delete");
		alert.setMessage("Do you want to delete " + contextMenuFile.getFileName() + "?");
		alert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            	File deleteFile = new File(contextMenuFile.getFileAbsolutePath());
            	if (recursiveDelete(deleteFile)){
                	CharSequence text = contextMenuFile.getFileName() + " has been deleted";
                	int duration = Toast.LENGTH_SHORT;
        	    	Toast toast = Toast.makeText(getApplicationContext(), text, duration);
        	    	toast.show();
        	    	contextMenuFile = null;
        	    	getDirectory(null, currentDirectory);
            	} else {
                	CharSequence text = contextMenuFile.getFileName() + " could not be deleted";
                	int duration = Toast.LENGTH_SHORT;
        	    	Toast toast = Toast.makeText(getApplicationContext(), text, duration);
        	    	toast.show();
            	}
            }
        });

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});
		
		alert.show();
	}

	private void renameFile() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Rename");
		alert.setMessage("Name:");
		
		LayoutInflater inflater = getLayoutInflater();
		View searchView = inflater.inflate(R.layout.input_text, null);
		alert.setView(searchView);
		
		final EditText input = (EditText)searchView.findViewById(R.id.inputText);
		input.setText(contextMenuFile.getFileName());

		alert.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			String newName = input.getText().toString();

			File oldFile = new File(contextMenuFile.getFileAbsolutePath());
	    	File newFile = new File(currentDirectory + "/" + newName);
	
	    	CharSequence text = "";
	    	int duration = Toast.LENGTH_SHORT;
	    	
	    	if (newFile.exists()){
			    text = "Error: " + newFile.getName() + " already exists";
	    	} else {
		    	if (oldFile.renameTo(newFile)){
				    text = "Successfully renamed " + oldFile.getName() + " to " + newFile.getName();
					refreshDirectory();
			    } else {
				    text = "Error: " + oldFile.getName() + " was not renamed";
			    }
	    	}
		    	Toast toast = Toast.makeText(getApplicationContext(), text, duration);
		    	toast.show();
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});

		alert.show();
	}

	private void cutFile() {
		fileToCopy = true;
		setCutFile(true);
		
		copyFile = new File(contextMenuFile.getFileAbsolutePath());
	}

	private void copyFile() {
		fileToCopy = true;
		setCutFile(false);
		
		copyFile = new File(contextMenuFile.getFileAbsolutePath());
	}

	private void openFileWith() {
    	File file = new File(contextMenuFile.getFileAbsolutePath());

		if (file.isDirectory()) {
			if (file.canRead()) {
				getDirectory(null, contextMenuFile.getFileAbsolutePath());
			}
		} else {
			String mimeType = getFileMimeType(file);
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setDataAndType(Uri.fromFile(file), mimeType);
			String title = "Open with";
			Intent chooser = Intent.createChooser(intent, title);
			startActivity(chooser);
		}
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuAddFolder:
                createFolder();
                return true;
            case R.id.menuRefresh:
            	refreshDirectory();
                return true;
            case R.id.menuPaste:
            	pasteFile();
            	return true;
            case R.id.menuSearch:
            	search();
                return true;
            case R.id.menuAbout:
            	showAbout();
                return true;
            case R.id.menuExitBrowser:
            	finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void pasteFile() {
    	File newFile = new File(currentDirectory + "/" + copyFile.getName());
    	
    	PasteFile pasteFile = new PasteFile();
    	pasteFile.execute(copyFile, newFile);
	}

	private void createFolder() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Create Folder");
		alert.setMessage("Folder Name:");

		LayoutInflater inflater = getLayoutInflater();
		View searchView = inflater.inflate(R.layout.input_text, null);
		alert.setView(searchView);
		
		final EditText input = (EditText)searchView.findViewById(R.id.inputText);

		alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			String folderName = input.getText().toString();

	    	File newDirectory = new File(currentDirectory + "/" + folderName);
	
	    	CharSequence text = "";
	    	int duration = Toast.LENGTH_SHORT;
	    	
	    	if (!newDirectory.isDirectory()){
		    	boolean result = newDirectory.mkdir();
		    	if(result == true){
			    	text = folderName + " was successfully created";
		    	} else {
			    	text = "Error: " + folderName + " was not created";
		    	}
		    	
		    	Toast toast = Toast.makeText(getApplicationContext(), text, duration);
		    	toast.show();
		    	getDirectory(null, currentDirectory);
	    	} else {
		    	text = "Folder already exists!";
		    	Toast toast = Toast.makeText(getApplicationContext(), text, duration);
		    	toast.show();
	    	}
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});

		alert.show();
	}

	private void refreshDirectory() {
        getDirectory(null, currentDirectory);
	}
	
	private void search() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Search");
		alert.setMessage("File Name:");
		
		LayoutInflater inflater = getLayoutInflater();
		View searchView = inflater.inflate(R.layout.search_view, null);
		alert.setView(searchView);
		
		final EditText input = (EditText) searchView.findViewById(R.id.searchInputText);
		final CheckBox checkCurrentDir = (CheckBox) searchView.findViewById(R.id.checkCurrentDir);
		
		alert.setPositiveButton("Search", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				searchString = input.getText().toString();
				
				if (checkCurrentDir.isChecked()){
					searchDirectory = currentDirectory;
				} else {
					searchDirectory = root;
				}
				
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

				SearchFiles fileSearch = new SearchFiles();
				fileSearch.execute(searchDirectory, searchString);
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});

		alert.show();
	}
    
    private void showAbout() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("About Jb File Browser");
		
		LayoutInflater inflater = getLayoutInflater();
		View searchView = inflater.inflate(R.layout.about_view, null);
		alert.setView(searchView);
				
		alert.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		  }
		});

		alert.show();		
	}

    public class SortIgnoreCase implements Comparator<Object> {
        public int compare(Object o1, Object o2) {
        	File file1 = (File) o1;
        	File file2 = (File) o2;
            String s1 = file1.getName();
            String s2 = file2.getName();
            return s1.toLowerCase().compareTo(s2.toLowerCase());
        }
    }
    
    @Override  
    public boolean onKeyDown(int keyCode, KeyEvent event)  
    {  
        if(keyCode==KeyEvent.KEYCODE_BACK)  
        {
        	try {
        		if (currentDirectoryTextView.getText().charAt(0) != '/' && currentDirectoryTextView.getText().charAt(0) !=  '.'){
        			searchResultFiles.clear();
        			getDirectory(null, new File(searchDirectory).getPath());
        		} else {
        			if (!searchResultFiles.isEmpty()){
        				getDirectory(searchResultFiles, searchString);
        			} else {
        				getDirectory(null, new File(currentDirectory).getParent());
        			}
        		}
            	if(listViewSavedPosition != -1){
            		fileListView.setSelectionFromTop(listViewSavedPosition, listViewSavedListTop);
            		listViewSavedPosition = -1;
            	}
        	} catch (Exception e) {
            	finish();
        	}
        } else if (keyCode==KeyEvent.KEYCODE_MENU){
            openOptionsMenu();
        } else if (keyCode==KeyEvent.KEYCODE_SEARCH){
            search();
        }  
        return true;  
    }

    public void showMenu(View v){
        openOptionsMenu();
    }
    
    public void showSearch(View v){
        search();
    }
    
	private void getDirectory(ArrayList<File> showFiles, String directory){
		results = new ArrayList<MyFile>();
    	ArrayList<MyFile> folderResults = new ArrayList<MyFile>();
    	ArrayList<MyFile> fileResults = new ArrayList<MyFile>();

		File[] files;
    	File f = new File(directory);
		
    	if (showFiles != null){
    		files = showFiles.toArray(new File[showFiles.size()]);
    		currentDirectoryTextView.setText(Integer.toString(showFiles.size()) + " results found containing '" + directory + "'");
			MyFile tempFile = new MyFile();
			tempFile.setFileName(root);
			tempFile.setFileAbsolutePath("/");
			tempFile.setDirectory(true);
			tempFile.setFileIcon(R.drawable.icon_home);
			results.add(tempFile);

			tempFile = new MyFile();
			tempFile.setFileName("../");
			tempFile.setFileAbsolutePath(f.getParent());
			tempFile.setDirectory(true);
			tempFile.setFileIcon(R.drawable.icon_back);
			results.add(tempFile);
    	} else {
    		currentDirectory = directory;
    		if (!directory.equals(root)) {
    			MyFile tempFile = new MyFile();
    			tempFile.setFileName(root);
    			tempFile.setFileAbsolutePath("/");
    			tempFile.setDirectory(true);
    			tempFile.setFileIcon(R.drawable.icon_home);
    			results.add(tempFile);

    			tempFile = new MyFile();
    			tempFile.setFileName("../");
    			tempFile.setFileAbsolutePath(f.getParent());
    			tempFile.setDirectory(true);
    			tempFile.setFileIcon(R.drawable.icon_back);
    			results.add(tempFile);
    		} 
    		files = f.listFiles();
    		currentDirectoryTextView.setText(shrinkDirectoryText(currentDirectory));
    	}
				
		Arrays.sort(files, new SortIgnoreCase());

		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			
			SimpleDateFormat dateFormat= new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
			String lastModDate = dateFormat.format(new Date(file.lastModified()));
									
			String filePermission = "";
			
			if (file.canRead()){
				filePermission = "r";
			} else {
				filePermission = "-";
			}
			
			if (file.canWrite()){
				filePermission += "w";
			} else {
				filePermission += "-";
			}
			
			if (file.canExecute()){
				filePermission += "x";
			} else {
				filePermission += "-";
			}
			
			if (file.isDirectory()) {
				MyFile tempFile = new MyFile();
				tempFile.setFileName(file.getName());
				tempFile.setFileAbsolutePath(file.getAbsolutePath());
				tempFile.setDirectory(true);
				tempFile.setFileIcon(R.drawable.icon_folder);
				tempFile.setFileDateModified(lastModDate);
				folderResults.add(tempFile);
			} else {				
				MyFile tempFile = new MyFile();
				tempFile.setFileName(file.getName());
				tempFile.setFileSize(file.length());
				tempFile.setFilePermission(filePermission);
				tempFile.setFileAbsolutePath(file.getAbsolutePath());
				tempFile.setFileDateModified(lastModDate);
				tempFile.setDirectory(false);

				String mimeType = getFileMimeType(file);
				
				if (mimeType != null){
					if (mimeType.contains("audio")){
						tempFile.setFileIcon(R.drawable.icon_audio);
				    } else if (mimeType.contains("video")){
						tempFile.setFileIcon(R.drawable.icon_video);
				    } else if (mimeType.contains("image")){
						tempFile.setFileIcon(R.drawable.icon_image);	
				    } else if (mimeType.contains("text")){
						tempFile.setFileIcon(R.drawable.icon_text);
				    } else if (mimeType.contains("zip")){
						tempFile.setFileIcon(R.drawable.icon_archive);
				    } else {
						tempFile.setFileIcon(R.drawable.icon_other);
				    }
			    } else {
					tempFile.setFileIcon(R.drawable.icon_other);
			    }
				
				fileResults.add(tempFile);
			}
		}

		results.addAll(folderResults);
		results.addAll(fileResults);

	    fileListView = (ListView) findViewById(R.id.fileListView);
	    
        fileListView.setAdapter(new MyCustomBaseAdapter(this, results));
        
        fileListView.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> a, View v, int position, long id) { 
        		Object o = fileListView.getItemAtPosition(position);
            	MyFile selectedFile = (MyFile) o;
            
            	File file = new File(selectedFile.getFileAbsolutePath());

        		if (file.isDirectory()) {
        			if (file.canRead()) {
        		    	listViewSavedPosition = fileListView.getFirstVisiblePosition();
        		        View firstVisibleView = fileListView.getChildAt(0);
        		        listViewSavedListTop = (firstVisibleView == null) ? 0 : firstVisibleView.getTop();
        				getDirectory(null, selectedFile.getFileAbsolutePath());
        			}
        		} else {
    				String mimeType = getFileMimeType(file);
        		      
        			Intent intent = new Intent();
        			intent.setAction(android.content.Intent.ACTION_VIEW);
        			
        			if (mimeType != null){
        			    if (mimeType.contains("audio")){
                			intent.setDataAndType(Uri.fromFile(file), "audio/*");
    				    } else if (mimeType.contains("video")){
    	        			intent.setDataAndType(Uri.fromFile(file), "video/*");
    				    } else if (mimeType.contains("image")){
    	        			intent.setDataAndType(Uri.fromFile(file), "image/*");	
    				    } else if (mimeType.contains("text")){
    	        			intent.setDataAndType(Uri.fromFile(file), "text/*");			    	
    				    } else {
    	        			intent.setDataAndType(Uri.fromFile(file), "application/*");
    				    }
    			    } else {
            			intent.setDataAndType(Uri.fromFile(file), "audio/*");
    			    }
        			
        			startActivity(intent);
        		}
        	}        	
        });        
    }

	private String getFileMimeType(File file) {
        String filename = file.getName();
        String filenameArray[] = filename.split("\\.");
        String extension = filenameArray[filenameArray.length-1];
		
	    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
	}

	private String shrinkDirectoryText(String directory) {
		String shrunkDirectory = directory;
		if (directory.length() > MAX_DIRECTORY_TEXT_LENGTH){
			
			String[] directorySplit = directory.split("/") ;
			int i = 1;
			shrunkDirectory = directorySplit[(directorySplit.length-i)] + "/";
			while (shrunkDirectory.length() + directorySplit[(directorySplit.length-i)].length() + 5 < MAX_DIRECTORY_TEXT_LENGTH ){
				i++;
				shrunkDirectory = directorySplit[(directorySplit.length-i)] + "/" + shrunkDirectory;
			}
			shrunkDirectory =  ".../" + shrunkDirectory;
		}
		return shrunkDirectory;
	}

	public boolean isCutFile() {
		return cutFile;
	}

	public void setCutFile(boolean cutFile) {
		this.cutFile = cutFile;
	}
	
	private class PasteFile extends AsyncTask<File, Integer, Boolean> {
		private long totalFileSize;
		private File newFile;
		
		public PasteFile(){
	        mProgressDialog = new ProgressDialog(JBFileBrowser.this);    
	    	if (cutFile == true){
	    		mProgressDialog.setMessage("Moving file(s)...");
	    	} else {
	    		mProgressDialog.setMessage("Copying file(s)...");
	    	}
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(100);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setCancelable(true);
			mProgressDialog.setCanceledOnTouchOutside(false);

			mProgressDialog.setOnCancelListener(new OnCancelListener(){
				public void onCancel(DialogInterface dialog) {
					cancel(true);

			        CharSequence text = "";
			    	if (cutFile == true){
			    		text = "Moving cancelled";
			    	} else {
			    		text = "Copying cancelled";
			    	}
			    	recursiveDelete(newFile);
					refreshDirectory();
			    	
			    	int duration = Toast.LENGTH_SHORT;
					
			    	Toast toast = Toast.makeText(getApplicationContext(), text, duration);
			    	toast.show();
				}
			});
		}
		
	    @Override
	    protected Boolean doInBackground(File... files) {
	    	try {
	    		newFile = files[1];
	    		getDirectorySize(files[0]);
				copyDirectory(files[0], files[1]);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    	return true;
	    }
	    
	    private void getDirectorySize(File sourceLocation) throws IOException {
	        if (sourceLocation.isDirectory()) {
	            String[] children = sourceLocation.list();
	            for (int i = 0; i < children.length; i++) {
	            	getDirectorySize(new File(sourceLocation, children[i]));
	            }
	        } else {
	        	totalFileSize += sourceLocation.length();
	        }
		}

		@Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        mProgressDialog.show();
	    }

		protected void onPostExecute(Boolean result) {
	        mProgressDialog.dismiss();

	        CharSequence text = "";
	    	int duration = Toast.LENGTH_SHORT;
	        if (new File(currentDirectory + "/" + copyFile.getName()).exists()){	        
		    	if (cutFile == true){
				    text = "Successfully moved " + copyFile.getName();
			    	fileToCopy = false;
			    	recursiveDelete(copyFile);
			    } else {
				    text = "Successfully copied " + copyFile.getName();
			    }
			} else {    
		    	if (cutFile == true){
		    		text = "Error: Failed to move file";
		    	} else {
		    		text = "Error: Failed to copy file";
		    	}
			}
			
	    	Toast toast = Toast.makeText(getApplicationContext(), text, duration);
	    	toast.show();
			refreshDirectory();
		}

	    @Override
	    protected void onProgressUpdate(Integer... progress) {
	        super.onProgressUpdate(progress);
	        mProgressDialog.setProgress(progress[0]);
	    }
	    
	    public void copyDirectory(File sourceLocation , File targetLocation) throws IOException {

	        if (sourceLocation.isDirectory()) {
	            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
	                throw new IOException("Cannot create directory: " + targetLocation.getAbsolutePath());
	            }

	            String[] children = sourceLocation.list();
	            for (int i = 0; i < children.length; i++) {
	                copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
	            }
	        } else {	        	
	            File directory = targetLocation.getParentFile();
	            if (directory != null && !directory.exists() && !directory.mkdirs()) {
	                throw new IOException("Cannot create directory: " + directory.getAbsolutePath());
	            }

	            FileInputStream in = new FileInputStream(sourceLocation);
	            FileOutputStream out = new FileOutputStream(targetLocation);
	            
	            byte[] buf = new byte[1024];
	            long total = 0;
	            int len;
	            while ((len = in.read(buf)) > 0) {
	                out.write(buf, 0, len);
	                total += len;
	                publishProgress((int) (total * 100 / totalFileSize));
	            }
	            in.close();
	            out.close();
	        }
	    }
	}

	private class SearchFiles extends AsyncTask<String, String, Boolean> {
		String searchString;
		
		public SearchFiles(){				
	        mProgressDialog = new ProgressDialog(JBFileBrowser.this);
	    	mProgressDialog.setMessage("Searching files...");
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(100);
			mProgressDialog.setCancelable(true);
			mProgressDialog.setCanceledOnTouchOutside(false);

			mProgressDialog.setOnCancelListener(new OnCancelListener(){
				public void onCancel(DialogInterface dialog) {
					getDirectory(searchResultFiles, searchString);
					cancel(true);
					
					CharSequence text = "Search cancelled";
			    	int duration = Toast.LENGTH_SHORT;
					
			    	Toast toast = Toast.makeText(getApplicationContext(), text, duration);
			    	toast.show();
				}
			});
		}
		
	    @Override
	    protected Boolean doInBackground(String... stringArgs) {
	    	searchString = stringArgs[1];
			File searchDir = new File(stringArgs[0]);
			recursiveSearch(searchDir, searchString);
	    	return true;
	    }

		public void recursiveSearch(File searchDirectory, String searchString){
			if (searchDirectory.isDirectory()){
				publishProgress(searchDirectory.getName());
				File[] files = searchDirectory.listFiles();
				if (files != null){
					for (int i = 0; i < files.length; i++) {
						if (files[i].getName().contains(searchString)){
							searchResultFiles.add(files[i]);
						}
						if (files[i].isDirectory()){
							recursiveSearch(files[i], searchString);
						} 
					}
				}
			} else {
				if (searchDirectory.getName().contains(searchString)){
					searchResultFiles.add(searchDirectory);
				}
			}
		}
	    
		@Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        mProgressDialog.show();
	    }

		protected void onPostExecute(Boolean result) {
	        mProgressDialog.dismiss();
			getDirectory(searchResultFiles, searchString);
		}

	    @Override
	    protected void onProgressUpdate(String... currentDir) {
	        super.onProgressUpdate(currentDir);
	        mProgressDialog.setMessage("Searching: " + currentDir[0]);
	    }
	}
}