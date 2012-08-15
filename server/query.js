/**
#-------------------------------------------------------------------------------
# Copyright 2011 The fangorn project
# 
#        Author: Sumukh Ghodke
# 
#        Licensed to the Apache Software Foundation (ASF) under one
#        or more contributor license agreements.  See the NOTICE file
#        distributed with this work for additional information
#        regarding copyright ownership.  The ASF licenses this file
#        to you under the Apache License, Version 2.0 (the
#        "License"); you may not use this file except in compliance
#        with the License.  You may obtain a copy of the License at
# 
#          http://www.apache.org/licenses/LICENSE-2.0
# 
#        Unless required by applicable law or agreed to in writing,
#        software distributed under the License is distributed on an
#        "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#        KIND, either express or implied.  See the License for the
#        specific language governing permissions and limitations
#        under the License.
#-------------------------------------------------------------------------------
**
 * This QueryTree class is capable of rendering json objects as
 * expandable-collapsible SVG trees
 * @author Sumukh Ghodke
 * 
 * @param data
 *            json object
 * @param objid
 *            id of the object wrapper
 * @param varname
 *            name of the javascript variable which holds the reference to a tree instance; used to generate callbacks in html
 * @return
 */

function QueryTree(data, varname, sentnum, queryDomId, treeDomId, matchedPairs, queryString) {
	this.lastXOffset = 0;
	this.tree = data; // the actual tree
	this.varname = varname; // used to fire callbacks from dom elements
	this.sentnum = sentnum;
	this.matches = new QueryTree.prototype.Matches(matchedPairs);
	this.queryDomId = queryDomId;
	this.treeDomId = treeDomId;

	QueryTree.prototype.XSPACE = 15;
	QueryTree.prototype.YSPACE = 45;
	QueryTree.prototype.TEXT_HEIGHT = 12;
	QueryTree.prototype.WIDTH_OF_CHAR = 9;
	QueryTree.prototype.WIDTH_OF_NARROW_CHAR = 7;
	QueryTree.prototype.YPADDING = 2;
	QueryTree.prototype.TRIANGLE_OFFSET = 2;
	QueryTree.prototype.TRIANGLE_HEIGHT = 18;
	QueryTree.prototype.TRIANGLE_WIDTH_2 = 5;

	QueryTree.prototype.SVG_PADDING = 12;
	
	QueryTree.prototype.LINE_COLOUR = "rgb(150,150,150)";
	QueryTree.prototype.HIGHLIGHT_TEXT_COLOUR = "rgb(34,139,34)";
	QueryTree.prototype.RED_LINE_COLOUR = "rgb(200,0,0)";
	QueryTree.prototype.BLUE_LINE_COLOUR = "rgb(0,0,200)";
	QueryTree.prototype.narrowRegex = /[a-z0-9]/g;

	QueryTree.prototype.AXIS_OPRS = [{'name':'Descendant', 'sym':'//'}];
	QueryTree.prototype.AXIS_OPRS.push({'name':'Ancestor', 'sym':'\\\\'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'Child', 'sym':'/'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'Parent', 'sym':'\\'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'Following-sibling', 'sym':'==>'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'Preceding-sibling', 'sym':'<=='});
	QueryTree.prototype.AXIS_OPRS.push({'name':'Immediate-following-sibling', 'sym':'=>'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'Immediate-preceding-sibling', 'sym':'<='}); 
	QueryTree.prototype.AXIS_OPRS.push({'name':'Following', 'sym':'-->'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'Preceding', 'sym':'<--'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'Immediate-following', 'sym':'->'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'Immediate-preceding', 'sym':'<-'});

	QueryTree.prototype.START_MESG = "Click on highlighted nodes and lines to edit query.";
	QueryTree.prototype.NODE_SEL_MESG = "Edit node or create path by selecting a new node.";
	QueryTree.prototype.OPR_SEL_EDIT_MESG = " operator selected. Click line to switch operator.";
	QueryTree.prototype.OPR_SEL_UNEDIT_MESG = " is the only possible operator between the nodes.";
	QueryTree.prototype.CHR_NOTALLOWED_MESG = "Node labels cannot contain these characters: <whitespace> [ ] { } ( ) < > & ^ = \\ / !";
	QueryTree.prototype.NODE_EDITED_MESG = " node changed to "; 

	this.NODE_SELECTED_STATE = new QueryTree.prototype.NodeSelected();
	this.UNSELECTED_STATE = new QueryTree.prototype.Unselected();
	this.LINE_SELECTED_STATE = new QueryTree.prototype.LineSelected(this);
	this.setParentOnTreeNode(this.tree, null);
	//termId is a unique number given to terms in the query; deleted term's Ids are not reused; identified by tree["t"]
	this.queryNodeByTermId = {};
	//here pos indicates right_left_depth_parent; identified by tree["i"]
	this.queryNodeByTermPos = {};
	this.treeNodeByTermPos = {};
	this.indexTreeByTermPos(this.tree);
	this.currentState = this.UNSELECTED_STATE;
	this.selectedNodeTreeNode = null;
	this.selectedNodeDom = null;
	this.selectedLineTermId = null;
	this.root = this.buildQueryTree(queryString);
}

String.prototype.trim = function() {
	return this.toString().replace(/^ +/, '').replace(/ +$/, '');
}

QueryTree.prototype.indexTreeByTermPos = function(node) {
	this.treeNodeByTermPos[node["i"]] = node;
	for ( var i = 0; i < node["c"].length; i++) {
		this.indexTreeByTermPos(node["c"][i]);
	}
}

QueryTree.prototype.getQuery = function() {
	return this.root.toString();
}

QueryTree.prototype.LineSelected = function(qt) {
	this.mouseoverNode = qt.UNSELECTED_STATE.mouseoverNode,
	this.mouseoutNode = qt.UNSELECTED_STATE.mouseoutNode,
	this.clickNode = function(n, te, qt) {
		if (te["h"]) { 
			qt.selectNode(n, te);
			qt.currentState = qt.NODE_SELECTED_STATE;
			qt.removeLineSelection();
		}
	},
	this.clickLine = function(termId, matchPos, nextOpr, qt){
		if (qt.selectedLineTermId != termId) {
			qt.selectLine(termId, qt.matches.pairs[matchPos]["o"], nextOpr);
		} else {
			var l0 = document.getElementById("t_" + termId + "_0");
			var l1 = document.getElementById("t_" + termId + "_1");
			var l2 = document.getElementById("t_" + termId + "_2");
			if (nextOpr != null) { // change operator, redraw highlight path and selection
				qt.matches.updatePairOpr(matchPos, nextOpr);
				qt.queryNodeByTermId[termId].opr = QueryTree.prototype.AXIS_OPRS[nextOpr]["sym"];
				var queryNodeSpan = document.getElementById("q_" + termId);
				var oprDom = document.createElement('span');
				oprDom.appendChild(document.createTextNode(QueryTree.prototype.AXIS_OPRS[nextOpr]["sym"]));
				oprDom.setAttribute("id", "q_" + termId);
				queryNodeSpan.parentNode.insertBefore(oprDom, queryNodeSpan);
				queryNodeSpan.parentNode.removeChild(queryNodeSpan);
				var parent = l0.parentNode;
				parent.removeChild(l0);
				parent.removeChild(l1);
				if (l2) { parent.removeChild(l2); }
				qt.drawHighlightPath(parent, matchPos);
				qt.selectLine(termId, qt.matches.pairs[matchPos]["o"], nextOpr);	
			}
		}
	};
}

QueryTree.prototype.Unselected = function() {
	this.mouseoverNode = function(n, te) {
		if (te["h"]) { 
			n.setAttribute('font-weight', 'bold');
			n.setAttribute('cursor', 'pointer'); 
		}
	},
	this.mouseoutNode = function(n ,te) {
		if (te["h"]) { 
			n.removeAttribute('font-weight');
			n.removeAttribute('cursor'); 
		}
	},
	this.clickNode = function(n, te, qt) {
		if (te["h"]) { 
			qt.selectNode(n, te);
			qt.currentState = qt.NODE_SELECTED_STATE;
		}
	},
	this.clickLine = function(termId, matchPos, nextOpr, qt){
		qt.currentState = qt.LINE_SELECTED_STATE;
		qt.selectLine(termId, qt.matches.pairs[matchPos]["o"], nextOpr);
	};
}

QueryTree.prototype.NodeSelected = function() {
	this.mouseoverNode = function(n, te) {
		n.setAttribute('font-weight', 'bold');
		n.setAttribute('cursor', 'pointer');
	},
	this.mouseoutNode = function(n, te) {
		n.removeAttribute('font-weight');
		n.removeAttribute('cursor');
	},
	this.clickNode = function(n, te, qt) {
		if (te["h"]) {
			if (te != this.selectedNodeTreeNode) {
				qt.removePrevSelectedNode();
				qt.selectNode(n, te);
			}
		} else { //
			var startNode = qt.selectedNodeTreeNode;
			var svgElement = qt.selectedNodeDom.parentNode;
			qt.removePrevSelectedNode();
			te["h"] = true;
			n.setAttribute("style", "stroke:" + QueryTree.prototype.HIGHLIGHT_TEXT_COLOUR + ";fill:" + QueryTree.prototype.HIGHLIGHT_TEXT_COLOUR + ";"); //change colour of new node text to green
			var newTermId = qt.matches.nextTermId++;
			var opr = qt.getOpr(startNode["i"], te["i"]);
			var newMatchPos = qt.matches.addPair({"s": startNode["i"], "e": te["i"], "o": opr, "t": newTermId});
			qt.updateQueryTree(startNode, opr, te["n"], newTermId, te["i"]);
			qt.selectNode(n, te);//draw box around new node and set edit and info box
			qt.drawHighlightPath(svgElement, newMatchPos); //draw the line from old node to new node
			qt.drawQuery(); // redraw the entire query because it is complicated to modify in-place
		}
	},
	this.clickLine = function(termId, matchPos, nextOpr, qt){
		qt.removePrevSelectedNode();
		qt.currentState = qt.LINE_SELECTED_STATE;
		qt.selectLine(termId, qt.matches.pairs[matchPos]["o"], nextOpr);
	};
}

QueryTree.prototype.setParentOnTreeNode = function(node, parent) {
	node["p"] = parent;
	for ( var i = 0; i < node["c"].length; i++) {
		this.setParentOnTreeNode(node["c"][i], node);
	}
}

QueryTree.prototype.draw = function() {
	this.drawQuery();
	this.calcPositionAndDrawSVG();
	this.updateInfoDiv(QueryTree.prototype.START_MESG);
}

QueryTree.prototype.updateInfoDiv = function(message) {
	var infoDiv = document.getElementById('infodiv')
	while (infoDiv.firstChild) {
		infoDiv.removeChild(infoDiv.firstChild);
	}
	infoDiv.appendChild(document.createTextNode(message));
}

QueryTree.prototype.drawQuery = function() {
	var obj = document.getElementById(this.queryDomId);
	this.clearDom(obj);
	this.root.addToDom(obj);
}

QueryTree.prototype.calcPositionAndDrawSVG = function() {
	var obj = document.getElementById(this.treeDomId);
	this.addXAndYPositions();
	var svg = this.toSVG(this.treeDomId);
	this.clearDom(obj);
	var xy = {x:0, y:0};
	xy = this.maxXY(this.tree, xy);
	obj.setAttribute("width", xy.x + 2 * QueryTree.prototype.SVG_PADDING);
	obj.setAttribute("height", xy.y + QueryTree.prototype.SVG_PADDING);
	obj.appendChild(svg);
};

QueryTree.prototype.clearDom = function(obj) {
	if (obj.childNodes.length > 0) {
		while (obj.childNodes.length > 0) {
			obj.removeChild(obj.firstChild);
		}
	}
}

QueryTree.prototype.addXAndYPositions = function() {
	this.lastXOffset = 0;
	this.recAddXAndYPositions(this.tree);
}

QueryTree.prototype.recAddXAndYPositions = function(t) {
	var c = t["c"];
	var narrow = this.mainlyNarrowChars(t["n"]);
	var lengthOfTLabel = t["n"].length * ((narrow) ? QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR);
	t["s"] = (narrow) ? true : false; 
	if (t["e"] && c.length > 0) {
		for ( var i = 0; i < c.length; i++) {
			this.recAddXAndYPositions(c[i]);
		}
		mid = (c[0]["x"] + c[0]["x-end"] + c[c.length - 1]["x"] + c[c.length - 1]["x-end"]) / 4;
		t["x"] = mid - (lengthOfTLabel / 2);
		if (t["x"] < c[0]["x"] || t["x"] < 0) {
			var dist = (t["x"] < 0) ? 0 - t["x"] : c[0]["x"] - t["x"];
			for (var i = 0; i < c.length; i++) {
				this.recShiftX(c[i], dist);
			}
			t["x"] = t["x"] + dist;
		}
	} else {
		t["x"] = this.lastXOffset + QueryTree.prototype.XSPACE;
	}
	var newXOffset = t["x"] + lengthOfTLabel;
	if (newXOffset > this.lastXOffset) {
		this.lastXOffset = newXOffset;
	}
	t["x-end"] = t["x"] + lengthOfTLabel;
	t["y"] = t["depth"] * QueryTree.prototype.YSPACE;
}

QueryTree.prototype.recShiftX = function(t, x) {
	t["x"] = t["x"] + x;
	t["x-end"] = t["x-end"] + x;
	if (t["e"] && t["c"].length > 0) {
		for (var i = 0; i < t["c"].length; i++) {
			this.recShiftX(t["c"][i], x);
		}
	}
}

QueryTree.prototype.mainlyNarrowChars = function(name) {
	var narrowChars = name.match(QueryTree.prototype.narrowRegex);
	if (narrowChars == null) return false;
	if (narrowChars.length >= name.length / 2) return true;
	return false;
}

QueryTree.prototype.toSVG = function(objid) {
	var svg = document.createElementNS('http://www.w3.org/2000/svg',
			'svg:svg');
	this.recToSVG(svg, objid, this.tree);
	this.drawHighlightPaths(svg)
	var m = this.maxXY(this.tree, {x:0, y:0});
	svg.setAttribute("width", m.x + 2 * QueryTree.prototype.SVG_PADDING);
	svg.setAttribute("height", m.y + QueryTree.prototype.SVG_PADDING);
	return svg;
}

QueryTree.prototype.drawHighlightPaths = function(svgElement) {
	for (var i in this.matches.pairs) { // warning: don't just iterate, some positions will have undefined values because we may have deleted them
		this.drawHighlightPath(svgElement, i);
	}
}

QueryTree.prototype.drawHighlightPath = function(svgElement, matchPos) {
	var p = this.matches.pairs[matchPos];
	if (p["s"] == "") { return; }
	/**
	 * The numerical comparisons are based on TreeAxis's constants
	 */
	var opr = p["o"];
	var singleLine = (opr > 3 && opr < 8) ? false : true;
	var dashedLine = (opr == 0 || opr == 1 || opr == 4 || opr == 5 || opr == 8 || opr == 9) ? true : false;
	var colour = (opr < 4) ? QueryTree.prototype.BLUE_LINE_COLOUR : QueryTree.prototype.RED_LINE_COLOUR;
	var lineStyle = "stroke:" + colour + ";stroke-width:2;" + ((dashedLine) ? "stroke-dasharray:9,5;" : "");
	var transStyle = "stroke:black;stroke-width:0;fill:white;fill-opacity:0.001";//used to create a polygon to help click on the line and also highlight the line
	//All the preceding axis codes are odd numbers 
	var b = this.treeNodeByTermPos[(opr % 2 == 1) ? p["e"] : p["s"]];
	var a = this.treeNodeByTermPos[(opr % 2 == 1) ? p["s"] : p["e"]];
	var width = (b["s"]) ? QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR;
	var nextOpr = this.getNextOpr(p["s"], p["e"], opr); //other possible operators
	var termId = p["t"];
	var x1, y1, x2, y2;
	if (opr < 4) { // vertical line calculations
		x1 = b["x"] + (b["n"].length * width) / 2;
		y1 = b["y"] + 2 * QueryTree.prototype.YPADDING
		width = (a["s"]) ? QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR;
		x2 = a["x"] + (a["n"].length * width) / 2;
		y2 = a["y"] - QueryTree.prototype.TEXT_HEIGHT - QueryTree.prototype.YPADDING;
	} else { // horizontal line calculations
		x1 = b["x"] + (b["n"].length * width) + 5;
		y1 = b["y"] - QueryTree.prototype.TEXT_HEIGHT / 2 + 1;
		x2 = a["x"] - 5;
		y2 = a["y"] - QueryTree.prototype.TEXT_HEIGHT / 2 + 1;
	}
	var corners = this.getCorners(x1, y1, x2, y2, singleLine);
	svgElement.appendChild(this.getClickableSVGPolygon(corners, transStyle, termId, matchPos, 0, nextOpr));
	if (singleLine) {
		svgElement.appendChild(this.getClickableSVGLine(x1, y1, x2, y2, lineStyle, termId, matchPos, 1, nextOpr));
	} else { // double line is only possible in horizontal lines. therefore we adjust the y-coord values
		svgElement.appendChild(this.getClickableSVGLine(x1, y1 - 2, x2, y2 - 2, lineStyle, termId, matchPos, 1, nextOpr));
		svgElement.appendChild(this.getClickableSVGLine(x1, y1 + 2, x2, y2 + 2, lineStyle, termId, matchPos, 2, nextOpr));
	}
}

QueryTree.prototype.getClickableSVGPolygon = function(corners, style, termId, matchPos, lineNum, nextOpr) {
	var pg = document.createElementNS('http://www.w3.org/2000/svg', 'svg:polygon');
	var points_str = corners.x1 + "," + corners.y1 + " " + corners.x2 + "," + corners.y2 + " " + corners.x3 + "," + corners.y3 + " " + corners.x4 + "," + corners.y4;
	pg.setAttribute("points", points_str);
	pg.setAttribute("id", "t_" + termId + "_" + lineNum);
	pg.setAttribute("style", style);
	pg.setAttribute("onmouseover", this.varname + ".mouseoverLine('" + termId + "', " + nextOpr + ");");
	pg.setAttribute("onmouseout", this.varname + ".mouseoutLine('" + termId + "', " + nextOpr + ");");
	pg.setAttribute("onclick", this.varname + ".clickLine('" + termId + "', " + matchPos + ", " + nextOpr + ");");
	return pg;
}

QueryTree.prototype.getCorners = function(x1, y1, x2, y2, single) {
	var gap = single ? 4 : 6;
	var corners = new Object();
	if (x1 - x2 == 0) { // line is vertical
		corners.x1 = x1 - gap;
		corners.y1 = y1;
		corners.x2 = x1 + gap;
		corners.y2 = y1;
		corners.x3 = x2 + gap;
		corners.y3 = y2;
		corners.x4 = x2 - gap;
		corners.y4 = y2;
	} else if (y1 - y2 == 0) { // line is horizontal
		corners.x1 = x1;
		corners.y1 = y1 - gap;
		corners.x2 = x1;
		corners.y2 = y1 + gap;
		corners.x3 = x2;
		corners.y3 = y2 + gap;
		corners.x4 = x2;
		corners.y4 = y2 - gap;		
	} else { // calculate using slope
		var m = (y1 - y2) / (x1 - x2);
		var points = this.circleLineIntercept(-1/m, x1, y1, gap);		
		corners.x1 = points.x1;
		corners.y1 = points.y1;
		corners.x2 = points.x2;
		corners.y2 = points.y2;
		points = this.circleLineIntercept(-1/m, x2, y2, gap);		
		corners.x4 = points.x1;
		corners.y4 = points.y1;
		corners.x3 = points.x2;
		corners.y3 = points.y2;
	}	
	return corners;
}

QueryTree.prototype.circleLineIntercept = function(m, x1, y1, rad) {
	var points = new Object();
	var c = y1 - m * x1;
	var x2cf = m * m + 1;
	var xcf = -2 * x1 + 2 * m * c - 2 * m * y1;
	var con = c * c - 2 * y1 * c + x1 * x1 + y1 * y1 - rad * rad;
	var twoa = 2 * x2cf;
	var mbBy2a = -1 * xcf / twoa;
	var dis = xcf * xcf - 4 * x2cf * con; // this has to be greater than 0 because the line passes through the center of the circle
	var disSqBy2a = Math.sqrt(dis) / twoa;
	points.x1 = mbBy2a + disSqBy2a;
	points.x2 = mbBy2a - disSqBy2a;
	points.y1 = m * points.x1 + c;
	points.y2 = m * points.x2 + c;
	return points;
}

QueryTree.prototype.getOpr = function(start, end) {
	var sPos = start.split('_');
	sPos = [parseInt(sPos[0]), parseInt(sPos[1]), parseInt(sPos[2]), parseInt(sPos[3])]; 
	var ePos = end.split('_');
	ePos = [parseInt(ePos[0]), parseInt(ePos[1]), parseInt(ePos[2]), parseInt(ePos[3])];
	if (sPos[0] >= ePos[0] && sPos[1] <= ePos[1] && sPos[2] < ePos[2]) {
		if (sPos[2] + 1 == ePos[2]) { return 2;}
		return 0;
	} else if (sPos[0] <= ePos[0] && sPos[1] >= ePos[1]) {
		if (sPos[2] == ePos[2] + 1) { return 3;}
		return 1;
	} else if (sPos[0] <= ePos[1]) { //following
		if (sPos[0] == ePos[1]	&& sPos[3] == ePos[3]) { return 6;}
		else if (sPos[3] == ePos[3]) { return 4;}
		else if (sPos[0] == ePos[1]) { return 10; }
		return 8;
	} //preceding 
	if (ePos[0] == sPos[1]	&& sPos[3] == ePos[3]) { return 7;}
	else if (sPos[3] == ePos[3]) { return 5;}
	else if (ePos[0] == sPos[1]) { return 11; }
	return 9;
}

QueryTree.prototype.getNextOpr = function(start, end, opr) {
	var sPos = start.split('_');
	var ePos = end.split('_');
	if (opr < 4) {
		if (opr == 2) {// child
			return 0;
		} else if (opr == 3) { // parent
			return 1;
		} else if (opr == 0) { // descendant
			if (parseInt(sPos[2]) + 1 == parseInt(ePos[2])) { return 2; }
			return null;
		}  // ancestor opr == 1
		if (parseInt(sPos[2]) == parseInt(ePos[2]) + 1) { return 3; }
		return null;
	} else if (opr % 2 == 0) { // following
		if (opr == 6) { // immediate following sibling
			return 4;
		} else if (opr == 4) { // following sibling
			if (ePos[1] == sPos[0]) { return 10; }
			return 8;
		} else if (opr == 10) { // immediate following
			return 8;
		} // following opr == 8
		if (sPos[3] == ePos[3]) { // siblings
			if (ePos[1] == sPos[0]) { return 6; }
			return 4;
		} else if (ePos[1] == sPos[0]) { return 10; }
		return null;
	} // preceding
	if (opr == 7) { // immediate preceding sibling
		return 5;
	} else if (opr == 5) { // preceding sibling
		if (ePos[0] == sPos[1]) { return 11; }
		return 9;
	} else if (opr == 11) { // immediate preceding
		return 9;
	} // preceding opr == 9
	if (sPos[3] == ePos[3]) { // siblings
		if (ePos[0] == sPos[1]) { return 7; }
		return 5;
	} else if (ePos[0] == sPos[1]) { return 11; }
	return null;
}

QueryTree.prototype.getSVGLine = function(x1, y1, x2, y2, style) {
	var lineElem = document.createElementNS('http://www.w3.org/2000/svg', 'svg:line');
	lineElem.setAttribute("x1", x1 + "px");
	lineElem.setAttribute("y1", y1 + "px");
	lineElem.setAttribute("x2", x2 + "px");
	lineElem.setAttribute("y2", y2 + "px");
	lineElem.setAttribute("style", style);
	return lineElem;
}

QueryTree.prototype.getClickableSVGLine = function(x1, y1, x2, y2, style, termId, matchPos, lineNum, nextOpr) {
	var line = this.getSVGLine(x1, y1, x2, y2, style);
	line.setAttribute("id", "t_" + termId + "_" + lineNum);
	line.setAttribute("onmouseover", this.varname + ".mouseoverLine('" + termId + "', " + nextOpr + ");");
	line.setAttribute("onmouseout", this.varname + ".mouseoutLine('" + termId + "', " + nextOpr + ");");
	line.setAttribute("onclick", this.varname + ".clickLine('" + termId + "', " + matchPos + ", " + nextOpr + ");");
	return line;
}

QueryTree.prototype.getNodeDomId = function(position) {
	return this.sentnum + "_" + this.treeNodeByTermPos[position]["i"];
}

QueryTree.prototype.recToSVG = function(svgElement, objid, t) {
	//this function is called for each displayed node
	var textElem = document.createElementNS('http://www.w3.org/2000/svg', 'svg:text');
	textElem.appendChild(document.createTextNode(t["n"]));
	textElem.setAttribute("x", t["x"] + "px");
	textElem.setAttribute("y", t["y"] + "px");
	textElem.setAttribute("id", this.getNodeDomId(t["i"]));
	var mouseoverFunc = this.varname + ".mouseoverNode('" + t["i"] + "');";
	var mouseoutFunc = this.varname + ".mouseoutNode('" + t["i"] + "');";
	textElem.setAttribute("onmouseover", mouseoverFunc);
	textElem.setAttribute("onmouseout", mouseoutFunc);			
	textElem.setAttribute("onclick", this.varname + ".clickNode('" + t["i"] + "');");
	if (t["h"]) {
		textElem.setAttribute("style", "stroke:" + QueryTree.prototype.HIGHLIGHT_TEXT_COLOUR + ";fill:" + QueryTree.prototype.HIGHLIGHT_TEXT_COLOUR + ";");
	}
	svgElement.appendChild(textElem);
	var width = (t["s"]) ? QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR;
	var x1 = t["x"] + (t["n"].length * width) / 2;
	var y1 = t["y"] + QueryTree.prototype.YPADDING;
	if (t["e"] && t["c"].length > 0) { //draw blackish lines to all child nodes
		var lineStyle = "stroke:" + QueryTree.prototype.LINE_COLOUR + ";stroke-width:1";
		for ( var i = 0; i < t["c"].length; i++) {
			width = (t["c"][i]["s"]) ?  QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR;
			var x2 = t["c"][i]["x"] + (t["c"][i]["n"].length * width) / 2;
			var y2 = t["c"][i]["y"] - QueryTree.prototype.TEXT_HEIGHT - QueryTree.prototype.YPADDING;
			svgElement.appendChild(this.getSVGLine(x1, y1 + QueryTree.prototype.YPADDING, x2, y2, lineStyle));
			this.recToSVG(svgElement, objid, t["c"][i]);
		}
	}
	if (!t["e"] && t["c"].length > 0) { //draw a triangle below this node
		var x11 = x1 - QueryTree.prototype.TRIANGLE_OFFSET;
		var x2 = x1 - QueryTree.prototype.TRIANGLE_WIDTH_2;
		var x3 = x1 + QueryTree.prototype.TRIANGLE_WIDTH_2;
		var y2 = y1 + QueryTree.prototype.TRIANGLE_OFFSET	+ QueryTree.prototype.TRIANGLE_HEIGHT;
		var pg = document.createElementNS('http://www.w3.org/2000/svg', 'svg:polygon');
		var points_str = (x1) + "," + (y1 + 2) + " " + (x2) + "," + (y2) + " " + (x3) + "," + (y2);
		pg.setAttribute("points", points_str);
		pg.setAttribute("id", "coll_" + t["i"]);
		pg.setAttribute("onmouseover", this.varname + ".mouseoverCollapsed('" + t["i"] + "');");
		pg.setAttribute("onmouseout", this.varname + ".mouseoutCollapsed('" + t["i"] + "');");
		pg.setAttribute("onclick", this.varname + ".expandNode('" + t["i"] + "');");
		pg.setAttribute("style", "stroke:" + QueryTree.prototype.LINE_COLOUR + ";stroke-width:2;fill:white;cursor:pointer;");
		svgElement.appendChild(pg);
	}
}

QueryTree.prototype.getHighlightSVGBox = function(x, y, width, height) {
	var box = document.createElementNS('http://www.w3.org/2000/svg', 'svg:rect');
	box.setAttribute("x", x);
	box.setAttribute("y", y);
	box.setAttribute("width", width);
	box.setAttribute("height", height);
	box.setAttribute("rx", 2);
	box.setAttribute("ry", 2);
	box.setAttribute("style", "stroke:black; stroke-width:1; fill:none;");
	return box;
}

QueryTree.prototype.maxXY = function(nod, xyv) {
	var maxx = xyv.x;
	if (nod["x-end"] > maxx) { maxx = nod["x-end"]; }
	var maxy = xyv.y;
	if (nod["y"] > maxy) { maxy = nod["y"]; }
	if (nod["e"] && nod["c"].length > 0) { //find max XY based on children's max
		for ( var i = 0; i < nod["c"].length; i++) {
			maxXYc = this.maxXY(nod["c"][i], {x:maxx, y:maxy});
			if (maxXYc.x > maxx) { maxx = maxXYc.x; }
			if (maxXYc.y > maxy) { maxy = maxXYc.y; }
		}
	} else if (!nod["e"] && nod["c"].length > 0) { //adding triangle height
		//shouldn't it be added only when maxy is already at max at this level?
		maxy = maxy + QueryTree.prototype.YPADDING	+ QueryTree.prototype.TRIANGLE_OFFSET + QueryTree.prototype.TRIANGLE_HEIGHT;
	}
	return {x:maxx, y:maxy};
}

QueryTree.prototype.mouseoverNode = function(pos) {
	var n = document.getElementById(this.getNodeDomId(pos));
	var te = this.treeNodeByTermPos[pos];
	var qn = document.getElementById("q_" + pos);
	if (qn) { qn.setAttribute("class", "highlight"); }
	this.currentState.mouseoverNode(n, te);
}

QueryTree.prototype.mouseoutNode = function(pos) {
	var n = document.getElementById(this.getNodeDomId(pos));
	var te = this.treeNodeByTermPos[pos];
	var qn = document.getElementById("q_" + pos);
	if (qn) { qn.removeAttribute("class"); }
	this.currentState.mouseoutNode(n, te);
}

QueryTree.prototype.clickNode = function(pos) {
	var n = document.getElementById(this.getNodeDomId(pos));
	var te = this.treeNodeByTermPos[pos];
	this.currentState.clickNode(n, te, this);
}

//this method requires a newly inserted node to be present in the query tree.
QueryTree.prototype.selectNode = function(n, te) {
	var width = ((te["s"]) ? QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR) * te["n"].length;
	var x = n.getAttribute("x");
	x = x.substring(0, x.length - 2) - 5;
	var y = n.getAttribute("y");
	y = y.substring(0, y.length - 2) - 15;
	var box = this.getHighlightSVGBox(x, y, width + 10, 20);
	this.selectedNodeDom = box;
	this.selectedNodeTreeNode = te;
	n.parentNode.appendChild(box);
	var modifyNodeDiv = document.getElementById('modifynode');
	modifyNodeDiv.removeAttribute('class');
	var editNodeText = document.getElementById('editnodelabel');
	editNodeText.value = te["n"];
	this.updateInfoDiv(QueryTree.prototype.NODE_SEL_MESG);
	if (te["i"] in this.queryNodeByTermPos && !this.queryNodeByTermPos[te["i"]].isDeleteable()) {
		document.getElementById('deletebutton').setAttribute("disabled", "disabled");
	} else {
		document.getElementById('deletebutton').removeAttribute("disabled");
	}
}

QueryTree.prototype.removePrevSelectedNode = function() {
	if (this.selectedNodeDom != null) { 
		this.selectedNodeDom.parentNode.removeChild(this.selectedNodeDom);
		this.selectedNodeDom = null;
		this.selectedNodeTreeNode = null;
		var modifyNodeDiv = document.getElementById('modifynode');
		modifyNodeDiv.setAttribute('class', 'hide');
	}
}

QueryTree.prototype.expandNode = function(pos) {
	var n = document.getElementById(this.getNodeDomId(pos));
	var te = this.treeNodeByTermPos[pos];
	if (!te["disc"] && te["c"].length > 0) {
		te["e"] = te["e"] ^ true;
		this.calcPositionAndDrawSVG();
	}
	if (this.selectedNodeDom != null) {
		var domNode = document.getElementById(this.getNodeDomId(pos));
		this.selectNode(domNode, this.selectedNodeTreeNode);
	}
}

QueryTree.prototype.mouseoverCollapsed = function(pos) {
	var n = document.getElementById("coll_" + pos);
	n.style.strokeWidth = 3;
	n.style.stroke = "black";
	n.setAttribute('cursor', 'pointer');
}

QueryTree.prototype.mouseoutCollapsed = function(pos) {
	var n = document.getElementById("coll_" + pos);
	n.style.strokeWidth = 2;
	n.style.stroke = QueryTree.prototype.LINE_COLOUR;
	n.removeAttribute('cursor');
}

QueryTree.prototype.selectLine = function(termId, currOpr, nextOpr) {
	if (this.selectedLineTermId != null && this.selectedLineTermId != termId) { this.removeLineSelection(); }
	this.selectedLineTermId = termId;
	var l0 = document.getElementById("t_" + termId + "_0");
	l0.style.strokeWidth = 1;
	var modifyOprDiv = document.getElementById('modifyopr');
	modifyOprDiv.removeAttribute('class');
	var msg = QueryTree.prototype.AXIS_OPRS[currOpr]['name'] + (nextOpr == null ? QueryTree.prototype.OPR_SEL_UNEDIT_MESG : QueryTree.prototype.OPR_SEL_EDIT_MESG);
	this.updateInfoDiv(msg);
}

QueryTree.prototype.removeLineSelection = function() {
	if (this.selectedLineTermId != null) {
		var l0 = document.getElementById("t_" + this.selectedLineTermId + "_0");
		l0.style.strokeWidth = 0;
		var modifyOprDiv = document.getElementById('modifyopr');
		modifyOprDiv.setAttribute('class', 'hide');
		this.selectedLineTermId = null;
	}
}

QueryTree.prototype.mouseoverLine = function(termId, nextOpr) {
	var l0 = document.getElementById("t_" + termId + "_0");
	var l1 = document.getElementById("t_" + termId + "_1");
	var l2 = document.getElementById("t_" + termId + "_2");
	if (nextOpr != null) {
		l0.setAttribute('cursor', 'pointer');
		l1.setAttribute('cursor', 'pointer');
		if (l2) { l2.setAttribute('cursor', 'pointer');}
	}
	l1.style.strokeWidth = 3;
	if (l2) { l2.style.strokeWidth = 3; }	
	var qn = document.getElementById("q_" + termId);
	if (qn) { qn.setAttribute("class", "highlight"); }
}

QueryTree.prototype.mouseoutLine = function(termId, nextOpr) {
	var l0 = document.getElementById("t_" + termId + "_0");
	var l1 = document.getElementById("t_" + termId + "_1");
	var l2 = document.getElementById("t_" + termId + "_2");
	if (nextOpr != null) {
		l0.removeAttribute('cursor');
		l1.removeAttribute('cursor');
		if (l2) { l2.removeAttribute('cursor'); }
	}
	l1.style.strokeWidth = 2;
	if (l2) { l2.style.strokeWidth = 2; }
	var qn = document.getElementById("q_" + termId);
	if (qn) { qn.removeAttribute("class"); }
}

QueryTree.prototype.clickLine = function(termId, matchPos, nextOpr) {
	this.currentState.clickLine(termId, matchPos, nextOpr, this);
}

QueryTree.prototype.updateNodeLabel = function() {
	var newValue = document.getElementById('editnodelabel').value;
	if (this.selectedNodeTreeNode != null && this.selectedNodeDom != null && this.selectedNodeTreeNode["n"] != newValue && newValue.length > 0) {
		var oldValue = this.selectedNodeTreeNode["n"];
		var termPos = this.selectedNodeTreeNode["i"];
		var queryNode = this.queryNodeByTermPos[termPos];
		var regex = new RegExp("\\s|\\t|\\[|\\]|\\{|\\}|\\(|\\)|\\<|\\>|\\&|\\^|=|\\\\|\\/", "g");
		if (newValue.search(regex) != -1) {
			this.updateInfoDiv(QueryTree.prototype.CHR_NOTALLOWED_MESG);
			return;
		} 
		this.selectedNodeTreeNode["n"] = newValue;
		queryNode.label = newValue;
		this.drawQuery();
		this.calcPositionAndDrawSVG();
		this.removePrevSelectedNode();
		var newDomNode = document.getElementById(this.getNodeDomId(termPos));
		this.selectNode(newDomNode, this.treeNodeByTermPos[termPos]);
		this.updateInfoDiv(oldValue + QueryTree.prototype.NODE_EDITED_MESG + newValue + ".");
	}
}

QueryTree.prototype.deleteNode = function() {
	if (this.selectedNodeTreeNode == null) { return; }
	var treePos = this.selectedNodeTreeNode["i"];
	var deletedNodeLabel = this.selectedNodeTreeNode["n"];
	var queryNode = this.queryNodeByTermPos[treePos];
	if (queryNode == null) { return; }
	if (!queryNode.isDeleteable()) { return; }
	this.selectedNodeTreeNode["h"] = false;
	this.removePrevSelectedNode();
	var deletedPos = queryNode.queryTreePos;
	var updatedRoot = false;
	if (queryNode.parent == this.root) {
		// if root has no children or more than one child it can't be deleted
		if (queryNode.children[0].type == "filter" && !queryNode.children[0].children[0].isNot) { // filter should have only one child expr check if is a NOT expression
			this.root = queryNode.children[0].children[0];
			this.root.parent = null; 
		} else { // queryNode has a child node
			this.root.children[0] = this.root.children[0].children[0];
			this.root.children[0].parent = this.root;
		}
		this.root.children[0].opr = QueryTree.prototype.AXIS_OPRS[0].sym //change new root's opr to descendant
		updatedRoot = true;
	} else {
		if (queryNode.parent.type == "label") { //simple leaf
			//if filter + child -> this node is the last node, only child -> last node; pop will always work
			var qp = queryNode.parent;
			qp.children.pop(); // removes the node, now check if the query can be simplified
			if (qp.children.length > 0 && qp.children[0].children.length == 1 && !qp.children[0].children[0].isNot) { //has filter and only one not NOT expr in it
				qp.children[0] = qp.children[0].children[0].children[0]; //remove filter and make the label node of the expr as the child
				qp.children[0].parent = qp;
			}
		} else { // parent is an expr
			if (queryNode.parent.parent.children.length == 1) {//filter has this one node, remove filter
				queryNode.parent.parent.parent.children.splice(0, 1); // queryNode.parent(expr).parent(filter).parent(labelnode with the filter)
			} else if (queryNode.parent.parent.children.length > 1) { //expression is one amongst many remove the associated and/or operator 
				var exprToFind = queryNode.parent;
				var nodeWithFilter = queryNode.parent.parent.parent;
				var pos = -1;
				for (var i = 0; i < nodeWithFilter.children[0].children.length; i++) { // nodeWithFilter.children[0] is the filter
					if (nodeWithFilter.children[0].children[i] == exprToFind) { 
						pos = i; 
						break;
					}
				}
				// (if first one -> remove the next AND/OR opr; others -> remove previous opr AND/OR) in addition to expr itself
				nodeWithFilter.children[0].children.splice((pos == 0 ? 0 : pos - 1), 2);
				// if removing one filter expression leaves out only one not NOT expression and there are no other children of nodeWithFilter then remove filter
				if (nodeWithFilter.children.length == 1 && nodeWithFilter.children[0].children.length == 1 && !nodeWithFilter.children[0].children[0].isNot) {
					nodeWithFilter.addChild(nodeWithFilter.children[0].children[0].children[0]); // add the label node in nodeWithFitler/filter/expr to nodeWithFilter 
					nodeWithFilter.children.splice(0, 1); //remove the filter expression
				}
			}

		} 
	}
	this.matches.removePair(deletedPos);
	if (updatedRoot) { this.matches.updateStartOpr(this.root.children[0].queryTreePos, "", 0); }
	this.drawQuery();
	this.calcPositionAndDrawSVG();
	this.updateInfoDiv("Deleted node " + deletedNodeLabel + ".");
}

Node.prototype.isDeleteable = function() { // this is called only on LabelNodes
	if (this.parent.parent == null) { //is root expr
		if (this.children.length > 1) { return false; }
		if (this.children.length == 1 && this.children[0].type == "filter") {
			if (this.children[0].children.length > 1) { return false; } // more than one expression in the filter
			if (this.children[0].children[0].isNot) { return false; } // there is only one expression (from prev check); if that exp is a not exp return false
			return true;
		} else if (this.children.length == 1 && this.children[0].type == "label") {
			return true;
		}
		return false;
	}
	if (this.children.length > 0) { return false; }
	return true;
}

function Node(type) {
	this.type = type;
	this.children = [];
	this.parent = null;
	this.getPreString = function() {return ""};
	this.getPostString = function() {return ""};
	this.toString = function() {
		var str = this.getPreString();
		for (var i = 0; i < this.children.length; i++) {
			str += this.children[i].toString();
		}
		str += this.getPostString();
		return str;
	};
	this.addPreToDom = function(div) { 		
		var text = this.getPreString();
		if (text.length > 0) { div.appendChild(document.createTextNode(text)); }
	};
	this.addPostToDom = function(div) { 
		var text = this.getPostString();
		if (text.length > 0) { div.appendChild(document.createTextNode(text)); }
	};
	this.addToDom = function(div) {
		this.addPreToDom(div);
		for (var i = 0; i < this.children.length; i++) {
			this.children[i].addToDom(div);
		}
		this.addPostToDom(div);
	};	
}

Node.prototype.addChild = function(node) {
	this.children.push(node);
	node.parent = this;
}

var LabelNode = function(opr, label, queryTreePos, treePos) {
	this.opr = opr;
	this.label = label;
	this.children = [];
	this.parent = null;
	this.queryTreePos = queryTreePos;
	this.treePos = treePos; //this can be null because parts of the query may not be in the matches list eg. NOT expressions and ORed expressions

	this.getPreString = function() {
		return this.opr + this.label;
	};

	this.addPreToDom = function(div) {
		var oprDom = document.createElement('span');
		oprDom.appendChild(document.createTextNode(this.opr));
		oprDom.setAttribute("id", "q_" + this.queryTreePos);
		div.appendChild(oprDom);
		var labelDom = document.createElement('span');
		labelDom.appendChild(document.createTextNode(this.label));
		labelDom.setAttribute("id", "q_" + this.treePos);
		div.appendChild(labelDom);
	};
}
LabelNode.prototype = new Node("label");

var ExprNode = function(isRoot, isNot) {
	this.isNot = isNot;
	this.children = [];
	this.parent = null;

	this.getPreString = function() {
		return this.isNot ? "NOT" : "";
	};
} 
ExprNode.prototype = new Node("expr");

var AndOrNode = function(logOpr) {
	this.logOpr = logOpr;
	this.children = [];
	this.parent = null;
	this.getPreString = function() {
		return  " " + this.logOpr +  " ";
	};
}
AndOrNode.prototype = new Node("andor");	

var FilterNode = function() {
	this.children = [];
	this.parent = null;
	this.getPreString = function() {
		return "[";
	};
	this.getPostString = function() {
		return "]";
	};
}
FilterNode.prototype = new Node("filter");

QueryTree.prototype.tokenize = function(queryString, pattern, type) {
	var tokens = [];
	var r = RegExp(pattern);
	var res = r.exec(queryString);
	while (res != null) {
		tokens.push({'start':res.index, 'end':r.lastIndex, 'type':type});
		res = r.exec(queryString);
	}
	return tokens;
}

QueryTree.prototype.getTokens = function(queryString) { 
	var axisOprPat = /\/\/|\/|\\\\|\\|-->|->|<--|<-|==>|=>|<==|<=/g;
	var logOprPat = /&|\||!|\bAND\b|\band\b|\bOR\b|\bor\b|\bNOT\b|\bnot\b/g;
	var openExpPat = /\[/g;
	var closeExpPat = /\]/g;

	var axisOprToks = this.tokenize(queryString, axisOprPat, 'axis_opr');
	var logOprToks = this.tokenize(queryString, logOprPat, 'log_opr');
	var openExpToks = this.tokenize(queryString, openExpPat, 'open_exp');
	var closeExpToks = this.tokenize(queryString, closeExpPat, 'close_exp');
	var i = 0, j = 0, m = 0, n = 0;
	var tokens = [];
	var prevEnd = 0;
	var last = {'start':queryString.length, 'end':queryString.length, 'type':''}
	//merge the tokens with the text tokens so that all tokens are ordered by 'start' numbers
	while (i < axisOprToks.length || j < logOprToks.length || m < openExpToks.length || n < closeExpToks.length) {
		var axisOpr = (i < axisOprToks.length) ? axisOprToks[i] : last;
		var logOpr = (j < logOprToks.length) ? logOprToks[j] : last;
		var openSym = (m < openExpToks.length) ? openExpToks[m] : last;
		var closeSym = (n < closeExpToks.length) ? closeExpToks[n] : last;
		var minToken = axisOpr;
		if (logOpr['start'] < minToken['start']) {
			minToken = logOpr;
		}
		if (openSym['start'] < minToken['start']) {
			minToken = openSym;
		}
		if (closeSym['start'] < minToken['start']) {
			minToken = closeSym;
		}
		if (minToken['start'] > prevEnd) {
			tokens.push({'start':prevEnd, 'end':minToken['start'], 'type':'text'});
			prevEnd = minToken['start'];
		} else {
			tokens.push(minToken);
			prevEnd = minToken['end'];
			var minType = minToken['type'];
			if (minType == 'axis_opr') {
				i += 1;
			} else if (minType == 'log_opr') {
				j += 1;
			} else if (minType == 'open_exp') {
				m += 1;
			} else if (minType == 'close_exp') {
				n += 1;
			}
		}
	}
	if (prevEnd < queryString.length) {
		tokens.push({'start':prevEnd, 'end':queryString.length, 'type':'text'});
	}
	//correct mistaken logOpr tokens
	for (var i = 0; i < tokens.length; i++ ) {
		var token = tokens[i];	
		if (token['type'] == 'log_opr' && i > 0 && tokens[i - 1]['type'] == 'axis_opr') {
			//mistaken token
			token['type'] = 'text';
		}
	}
	return tokens;
}

QueryTree.prototype.buildQueryTree = function(queryString) {
	queryString = queryString.replace(/ +/, ' ');
	queryString = queryString.trim();
	var root = new ExprNode(true, false);
	var termId = 0;
	var nodeStack = [];
	var prev = root; // this is the prev node where control rests in the tree
	var prevToken = null; // this is the prev token in the string query
	var tokens = this.getTokens(queryString);
	var endPosByPairNum = this.matches.getEndPosByPairNum();
	for (var i = 0; i < tokens.length; i++) {
		var token = tokens[i];
		if (prevToken == null) {
			// do nothing; the prevToken assignment at the end takes care of the next case		
			//the first token has to be a / or // ('axis_opr') since it has been parsed successfully by the server already
		} else if (prevToken['type'] == 'axis_opr') {
			if (token['type'] == 'text') { 
				var opr = queryString.substring(prevToken['start'], prevToken['end']);
				var text = queryString.substring(token['start'], token['end']).trim();
				var endPos = null;
				if (termId in endPosByPairNum) { endPos = endPosByPairNum[termId]; }
				var node = new LabelNode(opr, text, termId, endPos);
				if (endPos != null) { 
					this.queryNodeByTermId[termId] = node; 
					this.queryNodeByTermPos[endPos] = node;
				}
				termId++; //we have successfully processed one term in the query so increment it to point to the next one
				prev.addChild(node);
				prev = node;
			} 
		} else if (prevToken['type'] == 'text') {
			// do nothing for 'axis_opr'
			if (token['type'] == 'log_opr') {
				var text = queryString.substring(token['start'], token['end']).trim();
				if (!(text == 'NOT' || text == 'not' || text == '!')) { // a NOT expr cannot follow a text token
					//in this case an expression has just ended and another one is about to begin next
					//top of nodeStack has the node who will have another child expression
					//not performing any validation here since we know that nodeStack should have at least one entry for a correct query
					nodeStack[nodeStack.length - 1].addChild(new AndOrNode(text));
					var expr = new ExprNode(false, false);
					nodeStack[nodeStack.length - 1].addChild(expr);
					prev = expr;
				}
			} else if (token['type'] == 'open_exp') {
				// push the prev node to the stack; create a filter node and push to stack; start a new expr node
				nodeStack.push(prev);
				var filter = new FilterNode();
				nodeStack[nodeStack.length - 1].addChild(filter);
				nodeStack.push(filter);
				var expr = new ExprNode(false, false);
				nodeStack[nodeStack.length - 1].addChild(expr);
				prev = expr; 
			} else if (token['type'] == 'close_exp') {
				prev = nodeStack.pop(); //Repeated twice intentionally to pop label node and filter node
				prev = nodeStack.pop();
			}
		} else if (prevToken['type'] == 'log_opr') {
			// do nothing for 'axis_opr'
			if (token['type'] == 'log_opr') {
				//has to be a not operator since the previous token was also a log_opr
				var text = queryString.substring(token['start'], token['end']).trim();
				if (text == 'NOT' || text == 'not' || text == '!') {
					prev.isNot = true;
				}
			} 	
		} else if (prevToken['type'] == 'open_exp') {
			// do nothing for 'axis_opr'
			if (token['type'] == 'log_opr') {
				// has to be a not operator
				var text = queryString.substring(token['start'], token['end']).trim();
				if (text == 'NOT' || text == 'not' || text == '!') {
					prev.isNot = true;
				}
			}		
		} else if (prevToken['type'] == 'close_exp') {
			// do nothing for 'axis_opr'
			if (token['type'] == 'log_opr') {
				// has to be of AND or OR logical expression
				var text = queryString.substring(token['start'], token['end']).trim();
				if (!(text == 'NOT' || text == 'not' || text == '!')) {
					//in this case an expression with filter just ended and another one is about to begin next
					//top of nodeStack has the node who will have another child expression
					//not performing any validation here since we know that nodeStack should have at least one entry for a correct query
					nodeStack[nodeStack.length - 1].addChild(new AndOrNode(text));
					var expr = new ExprNode(false, false);
					nodeStack[nodeStack.length - 1].addChild(expr);
					prev = expr;
				} 
			} else if (token['type'] == 'close_exp') {
				prev = nodeStack.pop();//Repeated twice intentionally 
				prev = nodeStack.pop();
			} 
		}
		prevToken = token;
	}
	return root;
}

QueryTree.prototype.updateQueryTree = function(startTreeNode, oprId, label, queryTreePos, treePos) {
	var opr = QueryTree.prototype.AXIS_OPRS[oprId]["sym"];
	var labelNode = new LabelNode(opr, label, queryTreePos, treePos);
	var startQueryNode = this.queryNodeByTermPos[startTreeNode["i"]];
	if (startQueryNode.children.length == 0 || (startQueryNode.children.length == 1 && startQueryNode.children[0].type == "filter")) {
		startQueryNode.addChild(labelNode);
	} else if (startQueryNode.children.length == 2) { // the first child is the filter
		var filter = startQueryNode.children[0];
		filter.addChild(new AndOrNode("AND"));
		var expr = new ExprNode(false, false);
		expr.addChild(labelNode);
		filter.addChild(expr);
	} else if (startQueryNode.children.length == 1 && startQueryNode.children[0].type == "label") {
		var currentChild = startQueryNode.children.pop();
		var filter = new FilterNode();
		var expr = new ExprNode(false, false);
		expr.addChild(labelNode);
		filter.addChild(expr);
		startQueryNode.addChild(filter);
		startQueryNode.addChild(currentChild);
	}
	this.queryNodeByTermPos[treePos] = labelNode;
	this.queryNodeByTermId[queryTreePos] = labelNode;
}

QueryTree.prototype.Matches = function(pairs) {
	this.pairs = pairs,
	this.nextTermId = 0,
	this.init = function() {
		var treeNodesToQueryTerms = {};
		this.pairs.sort(function(a, b) { return a["t"] - b["t"]; }); 
		this.treeNodesMatchingMultipleQueryTerms = {};
		for (var i = 0 ; i < this.pairs.length; i++) {
			var pair = this.pairs[i];
			var end = pair["e"];
			var termId = pair["t"];
			if (end in treeNodesToQueryTerms) {
				treeNodesToQueryTerms[end].push(termId);
				this.treeNodesMatchingMultipleQueryTerms[end].push(termId);
			} else {
				treeNodesToQueryTerms[end] = [termId];
			}
		}
		this.nextTermId = this.pairs.length;
	},
	this.getEndPosByPairNum = function() {
		var byPairNum = {};
		for (var i = 0; i < this.pairs.length; i++) {
			var pair = this.pairs[i];
			byPairNum[pair["t"]] = pair["e"];
		}
		return byPairNum;
	},
	this.addPair = function(pair) {
		this.pairs.push(pair);
		return this.pairs.length - 1; //returns matchPos
	},
	this.updatePairOpr = function(matchPos, newOpr) {
		this.pairs[matchPos]["o"] = newOpr ;
	},
	this.updateStartOpr = function(matchPos, start, opr) {
		this.pairs[matchPos]["s"] = start;
		this.pairs[matchPos]["o"] = opr;
	},
	this.removePair = function(matchPos) {
		var end = this.pairs[matchPos]["e"]
		delete this.pairs[matchPos];
	},
	this.hasTreeNodesMatchingMultipleQueryTerms = function() {
		for (key in this.treeNodesMatchingMultipleQueryTerms) {
			return true;
		}
		return false;
	};
	this.init();
}

QueryTree.prototype.displayLoopError = function() {
	var obj = document.getElementById(this.treeDomId);
	obj.innerHTML  = "One or more nodes in the query tree match a single node in the result tree. Please select another result tree to build a query." 
}

