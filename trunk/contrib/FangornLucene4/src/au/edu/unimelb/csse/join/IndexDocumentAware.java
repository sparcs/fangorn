package au.edu.unimelb.csse.join;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

public interface IndexDocumentAware {
	int nextDoc() throws IOException;

	boolean setup(IndexReader r) throws IOException;
	
	Document getDocument(int docId) throws IOException;

	void setupPerDoc() throws IOException;
	
	void setupPerAtomicContext();
}
