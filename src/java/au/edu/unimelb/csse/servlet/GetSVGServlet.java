/*******************************************************************************
 * Copyright 2011 The fangorn project
 * 
 *        Author: Sumukh Ghodke
 * 
 *        Licensed to the Apache Software Foundation (ASF) under one
 *        or more contributor license agreements.  See the NOTICE file
 *        distributed with this work for additional information
 *        regarding copyright ownership.  The ASF licenses this file
 *        to you under the Apache License, Version 2.0 (the
 *        "License"); you may not use this file except in compliance
 *        with the License.  You may obtain a copy of the License at
 * 
 *          http://www.apache.org/licenses/LICENSE-2.0
 * 
 *        Unless required by applicable law or agreed to in writing,
 *        software distributed under the License is distributed on an
 *        "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *        KIND, either express or implied.  See the License for the
 *        specific language governing permissions and limitations
 *        under the License.
 ******************************************************************************/
package au.edu.unimelb.csse.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetSVGServlet extends HttpServlet {
	private static final int NORM_CHAR_WIDTH = 7;
	private static final int CAP_CHAR_WIDTH = 9;
	private static final long serialVersionUID = 1L;
	private static final String JSON_PARAM_NAME = "json";
	private static final String HEIGHT = "h";
	private static final String WIDTH = "w";
	private static final int Y_PADDING = 4;
	private static final int POL_WIDTH = 5;
	private static final int POL_HEIGHT = 18;
	private static final int TEXT_HEIGHT = 10;
	private final Pattern name = Pattern.compile("\\{\"n\":\"");
	private final Pattern doubleQuote = Pattern.compile("\"");
	private final Pattern x = Pattern.compile("\"x\":\"");
	private final Pattern y = Pattern.compile("\"y\":\"");
	private final Pattern tri = Pattern.compile("\"tri\":\"true\"");
	private final Pattern c = Pattern.compile("\"c\":\\[");
	private final Pattern close = Pattern.compile("\\}");
	private final Pattern open = Pattern.compile("\\{");
	private final Pattern closeArray = Pattern.compile("\\]");
	private Matcher nameMatcher;
	private Matcher doubleQuoteMatcher;
	private Matcher xMatcher;
	private Matcher yMatcher;
	private Matcher triMatcher;
	private Matcher cMatcher;
	private Matcher closeMatcher;
	private Matcher openMatcher;
	private Matcher closeArrayMatcher;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		final String json = req.getParameter(JSON_PARAM_NAME);
		final PrintWriter writer = resp.getWriter();
		resp.setContentType("image/svg+xml");
		writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		writer
				.write("<?xml-stylesheet rel=\"shortcut icon\" href=\"favicon.ico\" type=\"image/x-icon\"?>\n");
		writer
				.write("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.0//EN\" \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\">\n");

		writer
				.write("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" height=\""
						+ req.getParameter(HEIGHT)
						+ "\" width=\""
						+ req.getParameter(WIDTH) + "\">");
		writer
				.write("<style type=\"text/css\" >"
						+ "<![CDATA["
						+ "text{"
						+ "font-family: \"Lucida Grande\", Lucida, Verdana, sans-serif;"
						+ "font-size: 10pt;" + "}" + "line {"
						+ "stroke: rgb(150,150,150);" + "stroke-width: 1;"
						+ "}" + "polygon {" + "stroke: rgb(150,150,150);"
						+ "stroke-width: 2;" + "fill: white;" + "}" + "]]>"
						+ "</style>");
		writeSVGToStream(json, writer);
		writer.write("</svg>");
		writer.close();
	}

	void writeSVGToStream(String json, PrintWriter writer) {
		this.nameMatcher = name.matcher(json);
		this.doubleQuoteMatcher = doubleQuote.matcher(json);
		this.xMatcher = x.matcher(json);
		this.yMatcher = y.matcher(json);
		this.triMatcher = tri.matcher(json);
		this.cMatcher = c.matcher(json);
		this.closeMatcher = close.matcher(json);
		this.openMatcher = open.matcher(json);
		this.closeArrayMatcher = closeArray.matcher(json);
		writeNodeToStream(json, 0, writer, null, null, null);
	}

	int writeNodeToStream(String json, int position, PrintWriter writer,
			String parentXpx, String parentYpx, String parentName) {
		final boolean nameFound = nameMatcher.find(position);
		if (!nameFound) {
			return -1;
		}
		boolean doubleQuoteFound = doubleQuoteMatcher.find(nameMatcher.end());
		if (!doubleQuoteFound) {
			return -1;
		}
		String name = json.substring(nameMatcher.end(), doubleQuoteMatcher
				.start());
		final boolean xFound = xMatcher.find(doubleQuoteMatcher.end());
		if (!xFound || xMatcher.start() != doubleQuoteMatcher.end() + 1) {
			return -1;
		}
		doubleQuoteFound = doubleQuoteMatcher.find(xMatcher.end());
		if (!doubleQuoteFound) {
			return -1;
		}
		String xPx = json.substring(xMatcher.end(), doubleQuoteMatcher.start());
		final boolean yFound = yMatcher.find(doubleQuoteMatcher.end());
		if (!yFound || yMatcher.start() != doubleQuoteMatcher.end() + 1) {
			return -1;
		}
		doubleQuoteFound = doubleQuoteMatcher.find(yMatcher.end());
		if (!doubleQuoteFound) {
			return -1;
		}
		String yPx = json.substring(yMatcher.end(), doubleQuoteMatcher.start());
		final boolean triFound = triMatcher.find(doubleQuoteMatcher.end());
		boolean closeFound;
		if (triFound && triMatcher.start() == doubleQuoteMatcher.end() + 1) {
			closeFound = closeMatcher.find(triMatcher.end());
			if (!closeFound) {
				return -1;
			}
			writer.write("<text x=\"" + xPx + "px\" y=\"" + yPx + "px\">"
					+ name + "</text>");
			writeLine(writer, name, xPx, yPx, parentXpx, parentYpx, parentName);
			writePolygon(writer, name, xPx, yPx);
			return closeMatcher.end();
		}
		final boolean cFound = cMatcher.find(doubleQuoteMatcher.end());
		if (!cFound
				|| (cFound && cMatcher.start() != doubleQuoteMatcher.end() + 1)) {
			closeFound = closeMatcher.find(doubleQuoteMatcher.end());
			if (closeFound && closeMatcher.start() == doubleQuoteMatcher.end()) {
				writer.write("<text x=\"" + xPx + "px\" y=\"" + yPx + "px\">"
						+ name + "</text>");
				writeLine(writer, name, xPx, yPx, parentXpx, parentYpx,
						parentName);
				return closeMatcher.end();
			}
			return -1;
		}
		boolean openFound = openMatcher.find(cMatcher.end());
		boolean closeArrayFound = closeArrayMatcher.find(cMatcher.end());
		if (!openFound || !closeArrayFound) {
			return -1;
		}
		if (closeArrayMatcher.start() < openMatcher.start()) {
			if (closeArrayMatcher.start() == cMatcher.end()) {
				// no children
				closeFound = closeMatcher.find(closeArrayMatcher.end());
				if (!closeFound) {
					return -1;
				}
				writer.write("<text x=\"" + xPx + "\" y=\"" + yPx + "\">"
						+ name + "</text>");
				writeLine(writer, name, xPx, yPx, parentXpx, parentYpx,
						parentName);
				return closeMatcher.end();
			}
			return -1;
		}
		int pos = cMatcher.end();
		while (pos != -1 && pos < json.length()) {
			pos = writeNodeToStream(json, pos, writer, xPx, yPx, name);
			if (pos != -1) {
				openFound = openMatcher.find(pos);
				closeArrayFound = closeArrayMatcher.find(pos);
				if (!openFound && !closeArrayFound) {
					return -1;
				}
				if ((closeArrayFound && closeArrayMatcher.start() == pos)
						|| (closeArrayMatcher.start() < openMatcher.start())) {
					pos = closeArrayMatcher.end();
					break;
				}
			}
		}
		closeFound = closeMatcher.find(pos);
		if (!closeFound) {
			return -1;
		}
		writer.write("<text x=\"" + xPx + "px\" y=\"" + yPx + "px\">" + name
				+ "</text>");
		writeLine(writer, name, xPx, yPx, parentXpx, parentYpx, parentName);
		return closeMatcher.end();
	}

	private void writeLine(PrintWriter writer, String str, String xPx,
			String yPx, String parentXpx, String parentYpx, String parentName) {
		if (parentXpx == null || parentYpx == null || parentName == null) {
			return;
		}
		String startXPos = getString(getMidXPos(parentName, parentXpx));
		String endXPos = getString(getMidXPos(str, xPx));
		//adjusting for some float division errors
		if (Math.abs(Float.valueOf(endXPos) - Float.valueOf(startXPos)) < 1) {
			endXPos = startXPos;
		}
		String startYpos = getString(Float.parseFloat(parentYpx) + Y_PADDING);
		String endYPos = getString(Float.parseFloat(yPx) - TEXT_HEIGHT
				- Y_PADDING);
		writer.write("<line x1=\"" + startXPos + "\" y1=\"" + startYpos
				+ "\" x2=\"" + endXPos + "\" y2=\"" + endYPos + "\" />");
	}

	private void writePolygon(PrintWriter writer, String str, String xPx,
			String yPx) {
		float polygonXStart = getMidXPos(str, xPx);
		float polygonYStart = Float.parseFloat(yPx) + Y_PADDING;

		writer
				.write("<polygon points=\""
						+ getString(polygonXStart)
						+ ","
						+ getString(polygonYStart)
						+ " "
						+ getString(polygonXStart - POL_WIDTH)
						+ ","
						+ getString(polygonYStart + POL_HEIGHT)
						+ " "
						+ getString(polygonXStart + POL_WIDTH)
						+ ","
						+ getString(polygonYStart + POL_HEIGHT)
						+ "\" />");
	}

	float getMidXPos(String str, String xPx) {
		float polygonXStart = Float.parseFloat(xPx);
		if (shouldUseNormalWidth(str)) {
			polygonXStart += (str.length() * NORM_CHAR_WIDTH) / 2;
		} else {
			polygonXStart += (str.length() * CAP_CHAR_WIDTH) / 2;
		}
		return polygonXStart;
	}

	boolean shouldUseNormalWidth(String str) {
		char[] asChars = new char[str.length()];
		str.getChars(0, str.length(), asChars, 0);
		int numberOfSmall = 0;
		for (char c : asChars) {
			if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) numberOfSmall++;
		}
		return numberOfSmall >= str.length() / 2;
	}

	private String getString(float num) {
		if ((num - ((int) num)) == 0) {
			return Integer.toString((int) num);
		}
		return Float.toString(num);
	}
}
