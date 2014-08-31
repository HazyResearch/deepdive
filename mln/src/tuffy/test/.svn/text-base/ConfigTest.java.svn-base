package tuffy.test;

import static org.junit.Assert.*;

import org.junit.Test;

import tuffy.util.Config;
import tuffy.util.FileMan;
import tuffy.util.UIMan;

/**
 * Testing class for loading configuration file.
 *
 */
public class ConfigTest {

	/**
	 * Test the function of parsing configuration file.
	 */
	@Test
	public final void testParseConfigFile() {
		String conf = "# JDBC connection string; must be PostgreSQL\r\n" + 
				"db_url = jdbc:postgresql://localhost:5432/lmn\r\n" + 
				"\r\n" + 
				"# Database username; must be a superuser\r\n" + 
				"db_username = tuffer\r\n" + 
				"\r\n" + 
				"# The password for db_username\r\n" + 
				"db_password = strongPasswoRd\r\n" + 
				"\r\n" + 
				"# The working directory; Tuffy may write sizable temporary data here\r\n" + 
				"dir_working = /tmp/tuffy-workspace";

		FileMan.ensureExistence(Config.dir_tests);
		String fprog = Config.dir_tests + "/conf";
		FileMan.writeToFile(fprog, conf);
		UIMan.parseConfigFile(fprog);
		FileMan.removeFile(fprog);
		assertEquals("tuffer", Config.db_username);
		assertEquals("pass", Config.db_password);
	}

}
