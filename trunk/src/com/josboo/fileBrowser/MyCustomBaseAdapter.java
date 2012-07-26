package com.josboo.fileBrowser;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MyCustomBaseAdapter extends BaseAdapter {
	private static ArrayList<MyFile> fileArrayList;
	
	private LayoutInflater mInflater;

	public MyCustomBaseAdapter(Context context, ArrayList<MyFile> results) {
		fileArrayList = results;
		mInflater = LayoutInflater.from(context);
	}

	public int getCount() {
		return fileArrayList.size();
	}

	public Object getItem(int position) {
		return fileArrayList.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		convertView = mInflater.inflate(R.layout.folder_row_view, null);
		if (fileArrayList.get(position).isDirectory()){
			holder = new ViewHolder();
			holder.fileName = (TextView) convertView.findViewById(R.id.folderName);
			holder.fileIcon = (ImageView) convertView.findViewById(R.id.fileIcon);
			holder.fileDateModified = (TextView) convertView.findViewById(R.id.fileDateModified);

			convertView.setTag(holder);
			holder.fileName.setText(fileArrayList.get(position).getFileName());
			holder.fileIcon.setImageResource(fileArrayList.get(position).getFileIcon());
			holder.fileDateModified.setText(fileArrayList.get(position).getFileDateModified());
		} else {
			convertView = mInflater.inflate(R.layout.file_row_view, null);
			holder = new ViewHolder();
			holder.fileIcon = (ImageView) convertView.findViewById(R.id.fileIcon);
			holder.fileName = (TextView) convertView.findViewById(R.id.fileName);
			holder.fileSize = (TextView) convertView.findViewById(R.id.fileSize);
			holder.filePermissions = (TextView) convertView.findViewById(R.id.filePermissions);
			holder.fileDateModified = (TextView) convertView.findViewById(R.id.fileDateModified);

			convertView.setTag(holder);
		
			holder.fileIcon.setImageResource(fileArrayList.get(position).getFileIcon());
			holder.fileName.setText(fileArrayList.get(position).getFileName());
			holder.fileSize.setText(fileArrayList.get(position).getFileSize());
			holder.filePermissions.setText(fileArrayList.get(position).getFilePermission());
			holder.fileDateModified.setText(fileArrayList.get(position).getFileDateModified());
		}
		return convertView;
	}

	static class ViewHolder {
		ImageView fileIcon;
		TextView fileName;
		TextView fileSize;
		TextView filePermissions;
		TextView fileDateModified;
	}
}
