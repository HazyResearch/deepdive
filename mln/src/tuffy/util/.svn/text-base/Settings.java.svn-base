package tuffy.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Settings {
	private HashMap<String, Object> map = new HashMap<String, Object>();
	
	public Settings(){
		
	}
	
	public String toString(){
		ArrayList<String> lines = new ArrayList<String>();
		for(String k : map.keySet()){
			lines.add("    " + k + ": " + map.get(k).toString());
		}
		return StringMan.join("\n", lines);
	}
	
	public Settings(Map<String, Object> map){
		this.map.putAll(map);
	}
	
	public void put(String k, Object v){
		map.put(k, v);
	}
	
	public boolean hasKey(String k){
		return map.containsKey(k);
	}
	
	public Object get(String k){
		return map.get(k);
	}
	
	public Integer getInt(String k){
		return (Integer)(map.get(k));
	}

	public Double getDouble(String k){
		return (Double)(map.get(k));
	}

	public String getString(String k){
		return (String)(map.get(k));
	}

	public Boolean getBool(String k){
		return (Boolean)(map.get(k));
	}
}
