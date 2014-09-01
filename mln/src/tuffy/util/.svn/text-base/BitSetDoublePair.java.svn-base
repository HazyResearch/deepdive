package tuffy.util;

import java.util.BitSet;

public class BitSetDoublePair implements Comparable{

		public BitSet bitset;
		public Double doub;
		
		public BitSetDoublePair(BitSet _bitset, Double _double){
			bitset = _bitset;
			doub = _double;
		}

		@Override
		public int compareTo(Object o) {
			if(this.doub - ((BitSetDoublePair) o).doub > 0){
				return 1;
			}else{
				return -1;
			}
		}
		
		public String toString(){
			return doub + ": " + bitset;
		}
		
	}
