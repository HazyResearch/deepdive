package tuffy.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class HashArray<T> {
	ArrayList<T> list = new ArrayList<T>();
	HashMap<T, Integer> indices = new HashMap<T, Integer>();
	Random rand = new Random();
	
	public int size = 0;
	
	public ArrayList<T> getList(){
		return list;
	}
	
	public T getRandomElement(){
		return list.get(rand.nextInt(list.size()));
	}
	
	public boolean contains(T e){
		return indices.containsKey(e);
	}
	
	public void clear(){
		list.clear();
		indices.clear();
		size = 0;
	}
	
	public boolean isEmpty(){
		return list.isEmpty();
	}
	
	public void add(T e){
		if(indices.containsKey(e)) return;
		list.add(e);
		indices.put(e, list.size() - 1);
		size ++;
	}
	
	public void removeIdx(int i){
		int ss = list.size();
		if(i < 0 || i >= ss) return;
		indices.remove(list.get(i));
		if(i == ss-1){
			list.remove(i);
		}else{
			T last = list.get(ss-1);
			indices.put(last, i);
			list.set(i, last);
			list.remove(ss-1);
		}
		size --;
	}
	
	public void removeObj(T e){
		if(!indices.containsKey(e)) return;
		int i = indices.get(e);
		removeIdx(i);
	}
}
