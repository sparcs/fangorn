package au.edu.unimelb.csse.analyser;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;

import au.edu.unimelb.csse.LRDP;

public class TreeAnalyzer extends Analyzer {

	private LRDP nodePositionAware;
	
	public TreeAnalyzer(LRDP nodePositionAware) {
		this.nodePositionAware = nodePositionAware;
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName,
			Reader reader) {
		final TreeTokenizer tknzr = new TreeTokenizer(reader, nodePositionAware);
		return new TokenStreamComponents(tknzr, tknzr) {
			@Override
			protected void setReader(final Reader reader) throws IOException {
				super.setReader(reader);
			}
		};

	}

}
