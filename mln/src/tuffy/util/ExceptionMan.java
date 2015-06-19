package tuffy.util;

import java.io.File;

import tuffy.db.RDB;


/**
 * Container of exception related utilities.
 */
public class ExceptionMan {
	public static void handle(Exception e) {
		if(Config.exiting_mode) return;
		e.printStackTrace(System.err);
		die(e.getMessage());
	}
	
	public static void die(String msg){
		if(Config.exiting_mode) return;		
		Config.exiting_mode = true;
		UIMan.error(msg);
		RDB db = RDB.getRDBbyConfig();
		
		if(Config.keep_db_data == false){
			UIMan.print("removing database schema '" + Config.db_schema + "'...");
			UIMan.println(db.dropSchema(Config.db_schema)?"OK" : "FAILED");
		}
		
		UIMan.print("removing temporary dir '" + Config.getWorkingDir() + "'...");
		UIMan.println(FileMan.removeDirectory(new File(Config.getWorkingDir()))?"OK" : "FAILED");
		if(Config.throw_exception_when_dying){
			throw new TuffyThrownError(msg);
		}else{
			System.exit(2);
		}
	}
}
