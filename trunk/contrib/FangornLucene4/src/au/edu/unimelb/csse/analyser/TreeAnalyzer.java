package au.edu.unimelb.csse.analyser;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;

public class TreeAnalyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName,
			Reader reader) {
		final TreeTokenizer tknzr = new TreeTokenizer(reader);
		return new TokenStreamComponents(tknzr, tknzr) {
			@Override
			protected void setReader(final Reader reader) throws IOException {
				super.setReader(reader);
			}
		};

	}

}
