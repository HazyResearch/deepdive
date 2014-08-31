package tuffy.test;

import static org.junit.Assert.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.Test;

import tuffy.db.RDB;
import tuffy.mln.Type;
import tuffy.util.Config;


import java.util.Random;

/**
 * Testing class of {@link Type} object.
 *
 */
public class TypeTest {
	
	public static Random rg = new Random(1);

	/**
	 * Test functions of building and querying the constant
	 * doamin of Type.
	 */
	@Test
	public final void test_AddConstant_and_Contains() {

		Type t = new Type("testType1");
		
		int[] isContainingIndex = new int[10000];
		for(int i=0;i<isContainingIndex.length;i++)
			isContainingIndex[i] = 0;
		for(int i=0;i<isContainingIndex.length/2;i++){
			int tmp = rg.nextInt(isContainingIndex.length);
			isContainingIndex[tmp] = 1;
			t.addConstant(tmp);
		}
		
		for(int i=0;i<isContainingIndex.length;i++){
			assertEquals( isContainingIndex[i]==0?false:true,
					t.contains(i));
		}
	}

	/**
	 * Test size-related functions of Type.
	 */
	@Test
	public final void test_size(){
		Type t = new Type("testType1");
		
		for(int i=0;i<100;i++){
			t.addConstant(i);
		}
		
		assertEquals(100, t.size());
	}
	
	/**
	 * Test name-related functions of Type. E.g.,
	 * 1) Type name; 2) Type's RDB table name.
	 */
	@Test
	public final void test_name(){
		Type t = new Type("testType1");

		assertEquals("testType1", t.name());

		t = new Type("testType1");

		assertEquals("type_testType1", t.getRelName());
	}
	
	/**
	 * Test functions of RDB/Type interaction. E.g.,
	 * saving constant list to RDB, etc.
	 */
	@Test
	public final void test_storeConstantList() throws Exception{
		Type t = new Type("testType1");
		
		//Config.test.flushTestConfiguration();
		RDB db = RDB.getRDBbyConfig();
		
		for(int i=0;i<100;i++){
			t.addConstant(i);
		}
		
		t.storeConstantList(db);
		
		PreparedStatement ps = db.getPrepareStatement("SELECT COUNT(DISTINCT constantID) AS CT FROM " + t.getRelName());
		ps.execute();
		ResultSet rss = ps.getResultSet();
		rss.next();
		
		assertEquals(100, rss.getInt("CT"));
		
		db.close();
		
	}

	
}
