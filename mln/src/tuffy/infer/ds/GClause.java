package tuffy.infer.ds;

import java.sql.Array;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

import tuffy.util.Config;
import tuffy.util.ExceptionMan;
import tuffy.util.ProbMan;
/**
 * A ground clause.
 *
 */
public class GClause {
	
	/**
	 * ID of this GClause.
	 */
	public int id;
	
	/**
	 * Weight of this GClause.
	 */
	public double weight = 0;
	
	/**
	 * List of literals in this grounded clause. This
	 * is from $lits$ attribute in {@link Config#relClauses}
	 * table.
	 */
	public int[] lits = null;
	
	/**
	 * List of original clauses used to generate this 
	 * grounded clause. This is from $fcid$ attribute
	 * in the clause table when
	 * {@link Config#track_clause_provenance} set to true.
	 */
	public int[] fcid = null; // list of id to fo clauses collectivley generating this gclause
	
	/**
	 * Finer-grained clause origin.
	 * Similar to {@link GClause#fcid}, corresponding to
	 * attribute $ffcid$ in {@link Config#relClauses} table.
	 */
	public String[] ffcid = null;
	
	/**
	 * Number of satisfied literals in this GClause. This
	 * GClause is true iff. nsat > 0.
	 */
	public int nsat = 0;
	
	/**
	 * Whether this clause should be ignored while SampleSAT.
	 * Used by MC-SAT.
	 */
	public boolean dead = false;
	
	
	/**
	 * The largest fcid seen when parsing the database for all GClause.
	 */
	public static int maxFCID = -1;

	/**
	 * Return whether this clause is a positive clause. Here by
	 * positive it means this clause has a positive weight.
	 */
	public boolean isPositiveClause(){
		return weight >= 0;
	}
	
	/**
	 * Return whether this clause is a hard clause. Here by hard
	 * it means this clause has a weight larger than {@link Config#hard_weight},
	 * which means this clause must be satisfied in reasoning result.
	 */
	public boolean isHardClause(){
		return Math.abs(weight) >= Config.hard_weight;
	}

	/**
	 * 
	 * Return true if this clause is not set to ``dead''. This is used
	 * in MCSAT.
	 */
	public boolean selectMCSAT(){
		if(cost() > 0) return false;
		double r = ProbMan.nextDouble();
		return r < (1 - Math.exp(-Math.abs((weight)) * Config.mcsat_sample_para));
	}
	
	/**
	 * Return the cost for violating this GClause. For positive
	 * clause, if it is violated, cost equals to weight.
	 * For negative clause, if it is satisfied, cost equals to
	 * -weight. Otherwise, return 0.
	 */
	public double cost(){
		if(weight > 0 && nsat == 0){
			return weight;
		}
		if(weight < 0 && nsat > 0){
			return -weight;
		}
		return 0;
	}
	
	/**
	 * Returns +/-1 if this GClause contains this atom; 0 if not. 
	 * If -1, then the atom in this clause is with negative sense.
	 * @param atom
	 */
	public int linkType(int atom){
		for(int l : lits){
			if(l==atom) return 1;
			if(l==-atom) return -1;
		}
		return 0;
	}

	/**
	 * Replaces the ID of a particular atom, assuming that
	 * no twins exist.
	 * @param oldID
	 * @param newID
	 * @return 1 if oldID=>newID, -1 if -oldID=>-newID, 0 if no replacement
	 */
	public int replaceAtomID(int oldID, int newID){
		for(int k=0; k<lits.length; k++){
			if(lits[k] == oldID){
				lits[k] = newID;
				return 1;
			}else if(lits[k] == -oldID){
				lits[k] = -newID;
				return -1;
			}
		}
		return 0;
	}

	/**
	 * Initialize GClause from results of SQL. This involves set
	 * $cid$ to {@link GClause#id}, $weight$ {@link GClause#weight},
	 * $lits$ to {@link GClause#lits}, $fcid$ to {@link GClause#fcid}.
	 * @param rs the ResultSet for SQL. This sql is a sequential
	 * scan on table {@link Config#relClauses}.
	 * 
	 */
	public void parse(ResultSet rs){
		try {
			id = rs.getInt("cid");
			weight = rs.getDouble("weight");
						
			Array a = rs.getArray("lits");
			Integer[] ilits = (Integer[]) a.getArray();
			lits = new int[ilits.length];
			for(int i=0; i<lits.length; i++){
				lits[i] = ilits[i];
			}
			if(Config.track_clause_provenance){
				Array fc = rs.getArray("fcid");
				Integer[] ifc = (Integer[]) fc.getArray();
				ArrayList<Integer> lfcid = new ArrayList<Integer>();
				for(int i=0; i< ifc.length; i++){
					// ignoring soft evidence unit clauses
					if(ifc[i] != null && ifc[i] != 0){
						lfcid.add(ifc[i]);
						// ADDED BY CE ON NOV. 29
						if(Math.abs(ifc[i]) > GClause.maxFCID){
							GClause.maxFCID = Math.abs(ifc[i]);
						}
					}
				}
				fcid = new int[lfcid.size()];
				for(int i=0; i<fcid.length; i++){
					fcid[i] = lfcid.get(i);
				}
				
				fc = rs.getArray("ffcid");
				String[] sfc = (String[]) fc.getArray();
				ArrayList<String> lsfc = new ArrayList<String>();
				for(int i=0; i< sfc.length; i++){
					// ignoring soft evidence unit clauses
					if(sfc[i] != null && sfc[i] != "0"){
						lsfc.add(sfc[i]);
					}
				}
				ffcid = new String[lsfc.size()];
				for(int i=0; i<ffcid.length; i++){
					ffcid[i] = lsfc.get(i);
				}
			}
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}
	}
	
	/**
	 * Returns the string form of this GClause, which is,
	 * 
	 * { <lit_1>, <lit_2>, ..., <lit_n> } | weight
	 * 
	 * where lit_i is the literal ID in {@link GClause#lits}.
	 */
	public String toPGString(){
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for(int i=0; i<lits.length; i++){
			sb.append(lits[i]);
			if(i < lits.length-1) sb.append(",");
		}
		sb.append("} | " + weight);
		return sb.toString();
	}
	
	/**
	 * Returns string form of this GClause. Compared
	 * with {@link GClause#toString()}, this function also only shows
	 * literals violating this clause.
	 * 
	 * @param atoms Map from Literal ID to GAtom object. This is used
	 * for obtaining the truth value of GAtom, which is inevitable
	 * for determining violation.
	 */
	public String toLongString(HashMap<Integer, GAtom> atoms){
		StringBuilder sb = new StringBuilder();
		sb.append("ViolatedGroundClause" + id + " [weight=" + weight +
				", satisfied=" + (nsat > 0) + "]\n");
		for(int l : lits){
			GAtom a = atoms.get(Math.abs(l));
			boolean vio = false;
			if((weight<0) == ((l > 0) == (a.truth))){
				vio = true;
			}
			if(vio){
				sb.append("\t");
				sb.append(a.truth ? " " : "!");
				sb.append(a.rep + "\n");
			}
		}
		return sb.toString();
	}
	
	/**
	 * Returns its human-friendly representation.
	 */
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(weight + ": [");
		for(int l : lits){
			sb.append(l + ",");
		}
		sb.append("]");
		
		if(this.ffcid != null){
			sb.append("{");
			for(String s : this.ffcid){
				sb.append(" " + s);
			}
			sb.append("}");
		}
		
		return sb.toString();
	}

}
