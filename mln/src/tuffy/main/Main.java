package tuffy.main;

import java.sql.SQLException;

import tuffy.parse.CommandOptions;
import tuffy.util.Config;
import tuffy.util.UIMan;
/**
 * The Main.
 */
public class Main {
	public static void main(String[] args) throws SQLException {
		
		CommandOptions options = UIMan.parseCommand(args);
		
		UIMan.println("*** Welcome to " + Config.product_name + "!");
		if(options == null){
			return;
		}

		new NonPartInfer().run(options);
	}
	
}
