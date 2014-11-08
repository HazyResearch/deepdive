package tuffy.util;

import java.util.BitSet;


public class BitSetIntPair implements Comparable{
	
	public BitSet bitset;
	public Integer integer;
	
	public BitSetIntPair(BitSet _bitset, Integer _integer){
		bitset = _bitset;
		integer = _integer;
	}

	@Override
	public int compareTo(Object o) {
		return this.integer - ((BitSetIntPair) o).integer;
	}
	
	public String toString(){
		return integer + ": " + bitset;
	}
	
}
