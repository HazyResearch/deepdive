package tuffy.util;

import java.util.ArrayList;

public class PlotMan {

	public static class TCPair{
		public double time, cost;
		public TCPair(double t, double c){
			time = t;
			cost = c;
		}
		
		public String toString(){
			return time + "\t" + cost;
		}
		
		public static TCPair parse(String line){
			String[] parts = line.trim().split("\t");
			if(parts.length != 2) return null;
			double t = Double.parseDouble(parts[0]);
			double c = Double.parseDouble(parts[1]);
			return new TCPair(t,c);
		}
	}
	
	public ArrayList<TCPair> input(String fin){
		System.out.println("reading...");
		ArrayList<String> lines = FileMan.getLines(fin);
		ArrayList<TCPair> out = new ArrayList<TCPair>();
		for(String line : lines){
			TCPair p = TCPair.parse(line);
			if(p != null) out.add(p);
		}
		System.out.println("#points = " + out.size());
		return out;
	}
	
	public void output(ArrayList<TCPair> list, String fout){
		System.out.println("writing...");
		StringBuilder sb = new StringBuilder();
		for(TCPair p : list){
			sb.append(p + "\n");
		}
		FileMan.writeToFile(fout, sb.toString());
	}
	
	public ArrayList<TCPair> filter(ArrayList<TCPair> list){
		System.out.println("filtering...");
		double granu = 100;
		if(list.size() < 20) return list;
		ArrayList<TCPair> out = new ArrayList<TCPair>();
		TCPair first = list.get(0);
		TCPair last = list.get(list.size()-1);
		double mint = (last.time - first.time)/granu;
		double minc = (first.cost - last.cost)/granu;
		
		out.add(first);
		TCPair prev = first;
		for(int i=1; i<list.size()-1; i++){
			TCPair cur = list.get(i);
			if(cur.time - prev.time >= mint ||
					prev.cost - cur.cost >= minc){
				out.add(cur);
				prev = cur;
			}
		}
		out.add(last);
		System.out.println("#points = " + out.size());
		
		return out;
	}
	
	public void spit(int n, String fout){
		StringBuilder sb = new StringBuilder();
		for(int i=1; i<=n; i++){
			sb.append("pub(P" + i + ")\n");
		}
		FileMan.writeToFile(fout, sb.toString());
	}
	
	public static void main(String[] args) {
		String loc = "/sandbox/exp/";
		String fin = loc + "trace.txt";
		String fout = loc + "points.txt";
		PlotMan man = new PlotMan();
		man.spit(1000, "/sandbox/bench/wam/evidence.db");
		man.output(man.filter(man.input(fin)), fout);
	}

}
