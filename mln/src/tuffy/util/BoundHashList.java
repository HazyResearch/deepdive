package tuffy.util;

import java.util.HashSet;
import java.util.LinkedList;

public class BoundHashList<T> {
	LinkedList<T> list = new LinkedList<T>();
	HashSet<T> set = new HashSet<T>();
	
	private int bound = Integer.MAX_VALUE;
	
	public boolean contains(T e){
		return set.contains(e);
	}
	
	public BoundHashList(int maxSize){
		bound = maxSize;
	}
	
	public boolean add(T e){
		if(set.contains(e)) return false;
		if(list.size() >= bound){
			set.remove(list.removeFirst());
		}
		list.addLast(e);
		set.add(e);
		return true;
	}
}
