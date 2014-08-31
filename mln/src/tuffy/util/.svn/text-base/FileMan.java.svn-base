package tuffy.util;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Container of file related utilities.
 */
public class FileMan {
	private static long fguid = 0;

	public synchronized static String getUniqueFileName(){
		long tid = Thread.currentThread().getId();
		return String.format("tmp.%5d.%5d", tid, fguid++);
	}

	public synchronized static String getUniqueFileNameAbsolute(){
		long tid = Thread.currentThread().getId();
		return Config.getLoadingDir() + File.pathSeparator + String.format("tmp.%5d.%5d", tid, fguid++);
	}
	
	public static long getFileSize(String filename) {

		File file = new File(filename);

		if (!file.exists() || !file.isFile()) {
			UIMan.println("File doesn't exist");
			return -1;
		}

		return file.length();
	}

	public static BufferedReader getBufferedReaderMaybeGZ(String f){
		try{
			InputStream is;
			FileInputStream fis = new FileInputStream(f);
			if(f.toLowerCase().endsWith(".gz")){
				is = new GZIPInputStream(fis);
			}else{
				is = fis;
			}
			InputStreamReader reader = new InputStreamReader(is, "UTF8");
			BufferedReader lreader = new BufferedReader(reader);
			return lreader;
		}catch(Exception e){
			ExceptionMan.handle(e);
		}
		return null;
	}


	public static BufferedWriter getBufferedWriterMaybeGZ(String f){
		try{
			OutputStream os;
			FileOutputStream fis = new FileOutputStream(f);
			if(f.toLowerCase().endsWith(".gz")){
				os = new GZIPOutputStream(fis);
			}else{
				os = fis;
			}
			OutputStreamWriter reader = new OutputStreamWriter(os, "UTF8");
			BufferedWriter writer = new BufferedWriter(reader);
			return writer;
		}catch(Exception e){
			ExceptionMan.handle(e);
		}
		return null;
	}


	public static String getParentDir(String fname){
		File f = new File(fname);
		String p = f.getParent();
		if(p == null) return ".";
		return p;
	}

	/**
	 * Writes a string to a file, using UTF-8 encoding.
	 */
	public static void writeToFile(String filename, String content) {

		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new OutputStreamWriter
					(new FileOutputStream(filename),"UTF8"));
			bufferedWriter.write(content);
			bufferedWriter.flush();
			bufferedWriter.close();
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Reads lines from a text file.
	 */
	public static ArrayList<String> getLines(String filename){
		try {
			FileReader reader = new FileReader(filename);
			BufferedReader lreader = new BufferedReader(reader);
			String line = lreader.readLine();
			ArrayList<String> lines = new ArrayList<String>();
			while(line != null){
				lines.add(line);
				line = lreader.readLine();
			}
			lreader.close();
			return lines;
		}catch(IOException e) {
			ExceptionMan.handle(e);
		}
		return null;
	}

	/**
	 * Reads content from a text file.
	 */
	public static String getTextContent(String filename){
		return StringMan.join("\n", getLines(filename));
	}

	/**
	 * Removes a directory, even if it's NOT empty!
	 * @return true on success
	 */
	static public boolean removeDirectory(File path) {
		if( path.exists() ) {
			File[] files = path.listFiles();
			for(int i=0; i<files.length; i++) {
				if(files[i].isDirectory()) {
					removeDirectory(files[i]);
				}
				else {
					files[i].delete();
				}
			}
		}
		return(path.delete());
	}

	/**
	 * Creates an empty directory at the given path.
	 * If there already exists such a directory, it will
	 * be cleaned up.
	 */
	public static void createEmptyDirectory(File path){
		removeDirectory(path);
		boolean ok = path.mkdir();
		if(!ok){
			ExceptionMan.die("!!! failed to create dir " + path);
		}
	}

	/**
	 * Creates the directory if it doesn't exist yet.
	 */
	public static void ensureExistence(String dir){
		File path = new File(dir);
		if(!path.exists()){
			path.mkdirs();
		}
	}

	public static String getGZIPVariant(String f){
		if(f.endsWith(".gz")){
			return f;
		}else{
			String g = f + ".gz";
			if(exists(f) && exists(g)){
				File ff = new File(f);
				File fg = new File(g);
				String picked = null;
				if(ff.lastModified() > fg.lastModified()){
					picked = f;
				}else{
					picked = g;
				}
				UIMan.warn("Both regular and gzip'ed versions of this file exist; will use the newer one: " + picked);
				return picked;
			}else if(exists(g)){
				return g;
			}else{
				return f;
			}
		}
	}

	public static boolean exists(String f){
		File path = new File(f);
		return path.exists();
	}

	/**
	 * Removes a file.
	 */
	public static boolean removeFile(String file){
		File f = new File(file);
		return f.delete();
	}

}
