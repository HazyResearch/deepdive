package tuffy.main;

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;

import tuffy.db.RDB;
import tuffy.mln.Clause;
import tuffy.mln.MarkovLogicNetwork;
import tuffy.mln.Predicate;
import tuffy.parse.CommandOptions;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;
import tuffy.util.FileMan;
import tuffy.util.UIMan;
import tuffy.util.StringMan;
/**
 * Common routines to inference procedures.
 */
public class DeepDiveTuffy {
	
	/**
	 * The DB.
	 */
	protected RDB db = null;
		
	/**
	 * The MLN.
	 */
	public MarkovLogicNetwork mln = null;
	
	/**
	 * Command line options.
	 */
	protected CommandOptions options = null;

	/**
	 * Set up database and generate Deepdive input file, including the following steps:
	 * 
	 * 1) loadMLN {@link Infer#loadMLN};
	 * 2) store symbols and evidence {@link MarkovLogicNetwork#materializeTables};
	 * 3) generate Deepdive input file
	 * 
	 * @param opt command line options.
	 */
	public void run(CommandOptions opt){
		options = opt;

		Clause.mappingFromID2Const = new HashMap<Integer, String>();
		Clause.mappingFromID2Desc = new HashMap<String, String>();
		
		UIMan.println(">>> Connecting to RDBMS at " + Config.db_url);
		db = RDB.getRDBbyConfig();
		
		db.resetSchema(Config.db_schema);

		mln = new MarkovLogicNetwork();
		loadMLN(mln, db, options);

		mln.materializeTables();
		
		// generate the Deepdive input file.

		ArrayList<String> template = FileMan.getLines(Config.dir_out + "/application.conf");
		ArrayList<String> applicationConf = new ArrayList<String>();
		for (String line : template) {
			applicationConf.add(line);
			if (line.indexOf("schema.variables") >= 0) {
				// generate schema.variables
				for (Predicate p : mln.getAllPred()) {
					applicationConf.add(p.getRelName() + ".truth : Boolean");
				}
			} else if (line.indexOf("inference.factors") >= 0) {
				// generate normal rules
				for (Clause c : mln.listClauses) {
					applicationConf.addAll(c.generateDeepdiveInferenceRules());
				}
				// generate rules for soft evidence
				for (Predicate p : mln.getAllPred()) {
					applicationConf.addAll(p.generateDeepdiveSoftEvidenceInferenceRules());
				}
			} else if (line.indexOf("holdout_fraction") >= 0) {
				// generate observation only evidence
				ArrayList<String> attrs = new ArrayList<String>();
				for (Predicate p : mln.getAllPred()) {
					attrs.add(p.generateDeepdiveObservationOnlyEvidence());
				}
				applicationConf.add("observation_query: \"" + StringMan.join(" ", attrs) + "\"");
			}
		}
		FileMan.writeLines(opt.fout, applicationConf);
	}
	
	/**
	 * Load the rules and data of the MLN program.
	 * 
	 * 1) {@link MarkovLogicNetwork#loadPrograms(String[])};
	 * 2) {@link MarkovLogicNetwork#loadQueries(String[])};
	 * 3) {@link MarkovLogicNetwork#parseQueryCommaList(String)};
	 * 4) Mark closed-world predicate specified by {@link CommandOptions#cwaPreds};
	 * 5) {@link MarkovLogicNetwork#prepareDB(RDB)};
	 * 6) {@link MarkovLogicNetwork#loadEvidences(String[])}.
	 * 
	 * @param mln the target MLN
	 * @param adb database object used for this MLN
	 * @param opt command line options
	 */
	protected void loadMLN(MarkovLogicNetwork mln, RDB adb, CommandOptions opt){
		
		String[] progFiles = opt.fprog.split(",");
		mln.loadPrograms(progFiles);

		if(opt.fquery != null){
			String[] queryFiles = opt.fquery.split(",");
			mln.loadQueries(queryFiles);
		}
		
		if(opt.queryAtoms != null){
			UIMan.verbose(2, ">>> Parsing query atoms in command line");
			mln.parseQueryCommaList(opt.queryAtoms);
		}

		if(opt.cwaPreds != null){
			String[] preds = opt.cwaPreds.split(",");
			for(String ps : preds){
				Predicate p = mln.getPredByName(ps);
				if(p == null){
					mln.closeFiles();
					ExceptionMan.die("COMMAND LINE: Unknown predicate name -- " + ps);
				}else{
					p.setClosedWorld(true);
				}
			}
		}
		
		mln.prepareDB(adb);
		
		if(opt.fevid != null){
			String[] evidFiles = opt.fevid.split(",");
			mln.loadEvidences(evidFiles);
		}
		
	}

}
