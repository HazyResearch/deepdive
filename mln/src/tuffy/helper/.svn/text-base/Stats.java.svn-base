package tuffy.helper;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import tuffy.db.RDB;
import tuffy.infer.MRF;
import tuffy.infer.ds.GAtom;
import tuffy.infer.ds.GClause;
import tuffy.mln.Clause;
import tuffy.mln.MarkovLogicNetwork;
import tuffy.mln.Predicate;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;
import tuffy.util.FileMan;
import tuffy.util.StringMan;
import tuffy.util.UIMan;
/**
 * Stats collected from inference. Mainly for debugging and MLN developping purposes.
 * Current implementation/features are very rough.
 */
public class Stats {
	
	public static class ClauseCostComparator implements Comparator<Clause>
	{
	    @Override
	    public int compare(Clause x, Clause y)
	    {
	        double d = x.cost - y.cost;
	        if(d > 0){
	        	return -1;
	        }
	        if(d < 0){
	        	return 1;
	        }
	        return 0;
	    }
	}
	
	public static void dumpStats(MRF mrf){
		StringBuilder sb = new StringBuilder();
		sb.append(reportMostViolatedClauses(mrf, 100));
		UIMan.println(">>> Writing stats to " + Config.file_stats + "...");
		FileMan.writeToFile(Config.file_stats, sb.toString());
	}
	
	public static void pullAtomReps(MRF mrf){
		MarkovLogicNetwork mln = mrf.getMLN();
		HashMap<Integer, GAtom> atoms = mrf.atoms;
		RDB db = mln.getRDB();
		HashMap<Long,String> cmap = db.loadIdSymbolMapFromTable();
		for(Predicate p : mln.getAllPred()) {
			if(p.isImmutable()) continue;
			String sql = "SELECT * FROM " + p.getRelName() +
				" WHERE id >= 0";
			ResultSet rs = db.query(sql);
			try {
				while(rs.next()) {
					int aid = rs.getInt("id");
					GAtom a = atoms.get(aid);
					if(a == null) continue;
					StringBuilder line = new StringBuilder();
					line.append(p.getName() + "(");
					ArrayList<String> cs = new ArrayList<String>();
					for(String arg : p.getArgs()) {
						long c = rs.getLong(arg);
						cs.add(cmap.get(c));
					}
					line.append(StringMan.commaList(cs) + ")");
					a.rep = line.toString();
				}
				rs.close();
			} catch (SQLException e) {
				ExceptionMan.handle(e);
			}
		}
	}
	
	public static String reportMostViolatedClauses(MRF mrf, int k){
		if(mrf.getCost() <= 0) return "/* There were no clauses violated */\n";
		mrf.auditClauseViolations();
		MarkovLogicNetwork mln = mrf.getMLN();
		pullAtomReps(mrf);
		ClauseCostComparator comp = new ClauseCostComparator();
		PriorityQueue<Clause> pq = new PriorityQueue<Clause>(k, comp);
		pq.addAll(mln.getAllNormalizedClauses());
		StringBuilder sbh = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		sbh.append("/******************\nTOP " + k + " VIOLATED CLAUSES");
		double tc = 0;
		for(int i=0; i<k; i++){
			Clause c = pq.poll();
			if(c == null || c.cost == 0) break;
			tc += c.cost;
			sb.append("------Group #" + (i+1) + "------\n");
			sb.append(c.toString() + "\n[cost(estimate) = " + UIMan.comma(c.cost) + 
					"]\n[#violations(amortized estimate, grouped by unknown-atom tuples) = " + UIMan.comma(c.violations) + "]\n");
			for(GClause gc : c.violatedGClauses){
				sb.append(gc.toLongString(mrf.atoms));
			}
			sb.append("\n\n");
		}
		sb.append("********************/\n\n");
		sbh.append("\n(contributed " + UIMan.comma(tc) + " [" +
				UIMan.comma(100*tc/mrf.getCost()) + "%] of the " +
				UIMan.comma(mrf.getCost()) + " total cost)\n\n");
		return sbh.toString() + sb.toString();
	}
}
