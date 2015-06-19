package tuffy.parse;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;


import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;

import tuffy.mln.MarkovLogicNetwork;
import tuffy.util.ExceptionMan;

public class InputParser {
	MarkovLogicNetwork mln;
	
	public InputParser(MarkovLogicNetwork amln){
		mln = amln;
	}
	
	public void parseProgramFile(String fprog){
		MLNParser parser = new MLNParser(getTokens(fprog));
		parser.ml = this.mln;
		try {
			parser.definitions();
		} catch (Exception e) {
			mln.closeFiles();
			ExceptionMan.handle(e);
		}
	}

	public void parseEvidenceFile(String fevid){
		MLNParser parser = new MLNParser(getTokens(fevid));
		parser.ml = this.mln;
		try {
			parser.evidenceList();
		} catch (Exception e) {
			mln.closeFiles();
			ExceptionMan.handle(e);
		}
	}


	public void parseEvidenceString(String chunk, long lineOffset){
		ANTLRStringStream input = new ANTLRStringStream(chunk);
		MLNLexer lexer = new MLNLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		MLNParser parser = new MLNParser(tokens);
		parser.lineOffset = lineOffset;
		parser.ml = this.mln;
		try {
			parser.evidenceList();
			parser.reset();
			tokens.reset();
			lexer.reset();
			input.reset();
			parser.ml = null;
			parser = null;
		} catch (Exception e) {
			mln.closeFiles();
			ExceptionMan.handle(e);
		}
	}

	
	public void parseQueryFile(String fquery){
		MLNParser parser = new MLNParser(getTokens(fquery));
		parser.ml = this.mln;
		try {
			parser.queryList();
		} catch (Exception e) {
			mln.closeFiles();
			ExceptionMan.handle(e);
		}
	}
	
	public void parseQueryCommaList(String queryAtoms){
		CharStream input = new ANTLRStringStream(queryAtoms);
		MLNLexer lexer = new MLNLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		MLNParser parser = new MLNParser(tokens);
		parser.ml = this.mln;
		try {
			parser.queryCommaList();
		} catch (Exception e) {
			mln.closeFiles();
			ExceptionMan.handle(e);
		}
	}
	
	private CommonTokenStream getTokens(String fname){
		try {
			InputStream is;
			FileInputStream fis = new FileInputStream(fname);
			if(fname.toLowerCase().endsWith(".gz")){
				is = new GZIPInputStream(fis);
			}else{
				is = fis;
			}
			ANTLRInputStream input = new ANTLRInputStream(is);
			MLNLexer lexer = new MLNLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			is.close();
			return tokens;
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}
		return null;
	}

}
