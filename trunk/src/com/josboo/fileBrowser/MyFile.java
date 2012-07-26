package com.josboo.fileBrowser;

public class MyFile {
	private String fileName = "";
	private String fileSize = "";
	private String filePermission = "";
	private String fileDateModified = "";
	private String fileAbsolutePath = "";
	private boolean isDirectory = false;
	private int fileIcon;
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getFileSize() {
		return fileSize;
	}
	
	public void setFileSize(long fileSize) {
		this.fileSize = humanReadableByteCount(fileSize, false);
	}
	
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	public String getFilePermission() {
		return filePermission;
	}
	
	public void setFilePermission(String filePemission) {
		this.filePermission = filePemission + "  ";
	}

	public String getFileAbsolutePath() {
		return fileAbsolutePath;
	}

	public void setFileAbsolutePath(String fileAbsolutePath) {
		this.fileAbsolutePath = fileAbsolutePath;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public int getFileIcon() {
		return fileIcon;
	}

	public void setFileIcon(int fileIcon) {
		this.fileIcon = fileIcon;
	}

	public String getFileDateModified() {
		return fileDateModified;
	}

	public void setFileDateModified(String fileDateCreated) {
		this.fileDateModified = fileDateCreated;
	}
}
