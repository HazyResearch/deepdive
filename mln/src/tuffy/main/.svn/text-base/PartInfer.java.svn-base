package tuffy.main;

import tuffy.ground.partition.PartitionScheme;
import tuffy.infer.InferPartitioned;
import tuffy.infer.MRF;
import tuffy.parse.CommandOptions;
import tuffy.util.Config;
import tuffy.util.Settings;
import tuffy.util.UIMan;

/**
 * Partitioning-aware inference.
 */
public class PartInfer extends Infer{

	public void run(CommandOptions opt){
		UIMan.println(">>> Running partition-aware inference.");
		setUp(opt);

		ground();
		
		InferPartitioned ip = new InferPartitioned(grounding, dmover);
		
		if(options.maxFlips == 0){
			options.maxFlips = 100 * grounding.getNumAtoms();
		}
		if(options.maxTries == 0){
			options.maxTries = 3;
		}
		
		PartitionScheme pmap = ip.getPartitionScheme();
		int ncomp = pmap.numComponents();
		int nbuck = ip.getNumBuckets();
		String sdata = UIMan.comma(ncomp) + (ncomp > 1 ? " components" : " component");
		sdata += " (grouped into ";
		sdata += UIMan.comma(nbuck) + (nbuck > 1 ? " buckets" : " bucket)");
		
		
		Settings settings = new Settings(); 
		Double fpa = ((double)options.maxFlips)/grounding.getNumAtoms();
		
		if(!opt.mle && (!opt.marginal || opt.dual)){
			UIMan.println(">>> Running MAP inference on " + sdata);
			String mapfout = options.fout;
			if(opt.dual) mapfout += ".map";
			
			settings.put("task", "MAP");
			settings.put("ntries", new Integer(options.maxTries));
			settings.put("flipsPerAtom", fpa);
			double lowCost = ip.infer(settings);
			
			UIMan.println("### Best answer has cost " + UIMan.decimalRound(2,lowCost));
			UIMan.println(">>> Writing answer to file: " + mapfout);
			dmover.dumpTruthToFile(mln.relAtoms, mapfout);
			
		}
		
		if(opt.mle){
			UIMan.println(">>> Running MLE inference...");
			String mfout = options.fout;
			if(opt.dual) mfout += ".mle";
			
			settings.put("task", "MLE");
			settings.put("nsamples", new Integer(options.mcsatSamples));
			settings.put("flipsPerAtom", fpa);
			double lowCost = ip.infer(settings);
	
			UIMan.println("### Prob = " + lowCost);
			
			UIMan.println(">>> Writing answer to file: " + mfout);
			dmover.dumpTruthToFile(mln.relAtoms, mfout);
			
			//dmover.dumpMLETruthToFile(mfout);
			
			UIMan.println(">>> Writing answer to file: " + mfout + ".prob");
			dmover.dumpProbsToFile(mln.relAtoms, mfout + ".prob");
			
		}
		
		if(opt.marginal || opt.dual){
			UIMan.println(">>> Running marginal inference on " + sdata);
			String mfout = options.fout;
			if(opt.dual) mfout += ".marginal";
			
			settings.put("task", "MARGINAL");
			settings.put("nsamples", new Integer(options.mcsatSamples));
			settings.put("flipsPerAtom", fpa);
			double aveCost = ip.infer(settings);
			
			UIMan.println("### Average Cost " + UIMan.decimalRound(2,aveCost));
			UIMan.println(">>> Writing answer to file: " + mfout);
			dmover.dumpProbsToFile(mln.relAtoms, mfout);
		}

		
		if(Config.sampleLog != null){
			Config.sampleLog.close();
			
			String mfout = options.fout;
			dmover.dumpSampleLog(mfout + ".log");
			
			
			
		}
		
		
		
		
		cleanUp();
	}

}
