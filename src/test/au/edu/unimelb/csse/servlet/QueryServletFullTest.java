package au.edu.unimelb.csse.servlet;

import junit.framework.TestCase;

public class QueryServletFullTest extends TestCase {
	public void testEscapesBackslashInReturnedQueryString() throws Exception {
		QueryServletFull qs = new QueryServletFull();
		String returnedQuery = qs.getReturnQuery("//NP\\VP");
		assertEquals("//NP\\\\VP", returnedQuery);
	}
	
	public void testEscapesDoubleQuoteInReturnedQueryString() throws Exception {
		QueryServletFull qs = new QueryServletFull();
		String returnedQuery = qs.getReturnQuery("//NP\\\"");
		assertEquals("//NP\\\\&quot;", returnedQuery);
	}
	
	public void testEscapesArrowsInReturnedQueryString() throws Exception {
		QueryServletFull qs = new QueryServletFull();
		String returnedQuery = qs.getReturnQuery("//NP<--VP");
		assertEquals("//NP&lt;--VP", returnedQuery);
		returnedQuery = qs.getReturnQuery("//NP-->VP");
		assertEquals("//NP--&gt;VP", returnedQuery);
	}

}
