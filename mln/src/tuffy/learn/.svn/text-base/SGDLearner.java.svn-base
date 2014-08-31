package tuffy.learn;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import tuffy.ground.partition.PartitionScheme;
import tuffy.infer.InferPartitioned;
import tuffy.infer.MRF;
import tuffy.main.Infer;
import tuffy.mln.Clause;
import tuffy.mln.Predicate;
import tuffy.parse.CommandOptions;
import tuffy.util.BitSetIntPair;
import tuffy.util.Config;
import tuffy.util.FileMan;
import tuffy.util.Settings;
import tuffy.util.StringMan;
import tuffy.util.Timer;
import tuffy.util.UIMan;

public class SGDLearner extends Infer{

	public void run(CommandOptions opt) throws SQLException{
		
		UIMan.println(">>> Running SGD Learning.");
		
		Config.track_clause_provenance = true;
		Config.learning_mode = true;
		
		setUp(opt);
		ground();

		if(options.maxFlips == 0){
			options.maxFlips = 100 * grounding.getNumAtoms();
		}
		if(options.maxTries == 0){
			options.maxTries = 3;
		}

		MRF mrf = null;
		
		String mfout = options.fout;
		if(opt.dual) mfout += ".mle";
		
		if(mrf == null){
			mrf = new MRF(mln);
			dmover.loadMrfFromDb(mrf, mln.relAtoms, mln.relClauses);
		}
		
		
		HashMap<String, Double> weights = new HashMap<String, Double>();

		String sql = "SELECT DISTINCT weight, ffcid FROM " + "mln" + mln.getID() + "_cbuffer" + ";";
		ResultSet rs = db.query(sql);
		while(rs.next()){
			String ffcid = rs.getString("ffcid");
			Double wght = rs.getDouble("weight");
			String newCID = ffcid;
			if(newCID.charAt(0) == '-'){
				newCID = newCID.substring(1, newCID.length());
				wght = -wght;
			}
			weights.put(newCID, wght);
		}
		rs.close();
		
		System.out.println(weights);
		
		// prepare for the learning
		mrf.compile();
		
		for(int i=0;i<options.nDIteration;i++){
			weights = mrf.MLE_getGradientUpdate(weights, 0, 0.000000000000001);
			if(i%1000 == 0){
				System.out.println(i + ", " + weights);
			}
		}
		System.out.println(weights);
		
		//double sumCost = mrf.MLE_naiveSampler(options.mcsatSamples);
		Object[] keySet1 = weights.keySet().toArray();
		java.util.Arrays.sort(keySet1);
		
		Timer.runStat.markInferDone();
		UIMan.println(">>> Writing answer to file: " + options.fout);
		this.dumpAnswers(weights, options.fout);
				
		cleanUp();
	}
	
	public void dumpAnswers(HashMap<String, Double> currentWeight, String fout){
		ArrayList<String> lines = new ArrayList<String>();
		DecimalFormat twoDForm = new DecimalFormat("#.####");
		
		HashSet<Predicate> allp = mln.getAllPred();
		for(Predicate p : allp){
			String s = "";
			if(p.isClosedWorld()){
				//System.out.print("*");
				s += "*";
			}
			//System.out.print(p.getName() + "(");
			s += p.getName() + "(";
			for(int i=0;i<p.arity();i++){
				//System.out.print(p.getTypeAt(i).name());
				s += p.getTypeAt(i).name();
				if(i!=p.arity()-1){
					//System.out.print(",");
					s += ",";
				}
			}
			//System.out.println(")");
			s += ")";
			lines.add(s);
		}
		lines.add("\n");
		//System.out.println();
		
		lines.add("//////////////AVERAGE WEIGHT OF ALL THE ITERATIONS//////////////");
		Object[] keySet = currentWeight.keySet().toArray();
		java.util.Arrays.sort(keySet);

		
		lines.add("\n");
		
		lines.add("//////////////WEIGHT OF LAST ITERATION//////////////");
		keySet = currentWeight.keySet().toArray();
		java.util.Arrays.sort(keySet);
		for(Object ss : keySet){
			String s = (String) ss;
			//System.out.println(s + "\t" + Learner.currentWeight.get(s) + ":" + 
			//		this.trainingSatisification.get(s) + "/" + this.trainingViolation.get(s) + 
			//		"\t" + Clause.mappingFromID2Desc.get(s));
			lines.add("" + twoDForm.format(currentWeight.get(s)) + " " + Clause.mappingFromID2Desc.get(s) + " //" + s);
		}
		
		FileMan.writeToFile(fout, StringMan.join("\n", lines));
	}
	
}

