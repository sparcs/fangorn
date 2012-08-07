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

	QueryTree.prototype.AXIS_OPRS = [{'name':'descendant', 'sym':'//'}];
	QueryTree.prototype.AXIS_OPRS.push({'name':'ancestor', 'sym':'\\\\'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'child', 'sym':'/'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'parent', 'sym':'\\'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'following-sibling', 'sym':'==>'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'preceding-sibling', 'sym':'<=='});
	QueryTree.prototype.AXIS_OPRS.push({'name':'immediate-following-sibling', 'sym':'=>'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'immediate-preceding-sibling', 'sym':'<='}); 
	QueryTree.prototype.AXIS_OPRS.push({'name':'following', 'sym':'-->'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'preceding', 'sym':'<--'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'immediate-following', 'sym':'->'});
	QueryTree.prototype.AXIS_OPRS.push({'name':'immediate-preceding', 'sym':'<-'});

	this.setParentOnTreeNode(this.tree, null);
	//here position indicates right_left_depth_parent
	//this.queryNodeByPosition = {};
	this.root = this.buildQueryTree(queryString);
	this.treeNodeByPosition = {};
	this.indexTreeByPosition(this.tree);
	this.selectedState = this.noneSelected;
}

String.prototype.trim = function() {
	return this.toString().replace(/^ +/, '').replace(/ +$/, '');
}

QueryTree.prototype.indexTreeByPosition = function(node) {
	this.treeNodeByPosition[node["i"]] = node;
	for ( var i = 0; i < node["c"].length; i++) {
		this.indexTreeByPosition(node["c"][i]);
	}
}

QueryTree.prototype.noneSelected = function() {
	this.mouseoverNode = function() {
	},
	this.mouseoutNode = function() {
	};
}

QueryTree.prototype.highlightedSelected = function() {
	this.mouseoverNode = function() {

	},
	this.mouseoutNode = function() {

	};
}

QueryTree.prototype.noMatchesNoneSelected = function() {
	
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
	for ( var i = 0; i < this.matches.pairs.length; i++) {
		this.drawHighlightPath(svgElement, i);
	}
}

QueryTree.prototype.drawHighlightPath = function(svgElement, i) {
	var p = this.matches.pairs[i];
	if (p["s"] == "") { return; }
	/**
	 * The numerical comparisons are based on TreeAxis's constants
	 */
	var opr = p["o"];
	var singleLine = (opr > 3 && opr < 8) ? false : true;
	var dashedLine = (opr == 0 || opr == 1 || opr == 4 || opr == 5 || opr == 8 || opr == 9) ? true : false;
	var colour = (opr < 4) ? QueryTree.prototype.BLUE_LINE_COLOUR : QueryTree.prototype.RED_LINE_COLOUR;
	var lineStyle = "stroke:" + colour + ";stroke-width:2;" + ((dashedLine) ? "stroke-dasharray:9,5;" : "");
	var transStyle = "stroke-width:9;stroke-opacity:0.001;stroke:white;";//used to create a near transparent line to help click on the line
	//All the preceding axis codes are odd numbers 
	var b = this.treeNodeByPosition[(opr % 2 == 1) ? p["e"] : p["s"]];
	var a = this.treeNodeByPosition[(opr % 2 == 1) ? p["s"] : p["e"]];
	var width = (b["s"]) ? QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR;
	var nextOpr = this.getNextOpr(p["s"], p["e"], opr); //other possible operators
	if (opr < 4) { // vertical lines
		var x1 = b["x"] + (b["n"].length * width) / 2;
		var y1 = b["y"] + 2 * QueryTree.prototype.YPADDING
		width = (a["s"]) ? QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR;
		var x2 = a["x"] + (a["n"].length * width) / 2;
		var y2 = a["y"] - QueryTree.prototype.TEXT_HEIGHT - QueryTree.prototype.YPADDING;
		svgElement.appendChild(this.getClickableSVGLine(x1, y1, x2, y2, transStyle, i, 0, nextOpr));
		svgElement.appendChild(this.getClickableSVGLine(x1, y1, x2, y2, lineStyle, i, 1, nextOpr));
	} else { // horizontal lines
		var x1 = b["x"] + (b["n"].length * width) + 5;
		var y1 = b["y"] - QueryTree.prototype.TEXT_HEIGHT / 2 + 1;
		var x2 = a["x"] - 5;
		var y2 = a["y"] - QueryTree.prototype.TEXT_HEIGHT / 2 + 1;
		if (singleLine) {
			svgElement.appendChild(this.getClickableSVGLine(x1, y1, x2, y2, transStyle, i, 0, nextOpr));
			svgElement.appendChild(this.getClickableSVGLine(x1, y1, x2, y2, lineStyle, i, 1, nextOpr));
		} else {
			svgElement.appendChild(this.getClickableSVGLine(x1, y1, x2, y2, transStyle, i, 0, nextOpr));
			svgElement.appendChild(this.getClickableSVGLine(x1, y1 - 2, x2, y2 - 2, lineStyle, i, 1, nextOpr));
			svgElement.appendChild(this.getClickableSVGLine(x1, y1 + 2, x2, y2 + 2, lineStyle, i, 2, nextOpr));
		}
	}
	
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

QueryTree.prototype.getIdSVGLine = function(x1, y1, x2, y2, style, matchPos, lineNum) {
	var line = this.getSVGLine(x1, y1, x2, y2, style);
	line.setAttribute("id", "t_" + matchPos + "_" + lineNum);
	return line;
}
QueryTree.prototype.getClickableSVGLine = function(x1, y1, x2, y2, style, matchPos, lineNum, nextOpr) {
	var line = this.getIdSVGLine(x1, y1, x2, y2, style, matchPos, lineNum);
	line.setAttribute("onmouseover", this.varname + ".mouseoverLine('" + matchPos + "', " + nextOpr + ");");
	line.setAttribute("onmouseout", this.varname + ".mouseoutLine('" + matchPos + "', " + nextOpr + ");");
	line.setAttribute("onclick", this.varname + ".clickLine('" + matchPos + "', " + nextOpr + ");");
	return line;
}

QueryTree.prototype.getNodeDomId = function(position) {
	return this.sentnum + "_" + this.treeNodeByPosition[position]["i"];
}

QueryTree.prototype.recToSVG = function(svgElement, objid, t) {
	//this function is called for each displayed node
	var textElem = document.createElementNS('http://www.w3.org/2000/svg', 'svg:text');
	textElem.appendChild(document.createTextNode(t["n"]));
	textElem.setAttribute("x", t["x"] + "px");
	textElem.setAttribute("y", t["y"] + "px");
	width = (t["s"]) ? QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR;
	var x1 = t["x"] + (t["n"].length * width) / 2;
	var y1 = t["y"] + QueryTree.prototype.YPADDING;
	textElem.setAttribute("id", this.getNodeDomId(t["i"]));
	var mouseoverFunc = this.varname + ".mouseoverNode('" + t["i"] + "', " + ((t["h"]) ? "false" : "true") + ");";
	var mouseoutFunc = this.varname + ".mouseoutNode('" + t["i"] + "', " + ((t["h"]) ? "false" : "true") + ");";
	textElem.setAttribute("onmouseover", mouseoverFunc);
	textElem.setAttribute("onmouseout", mouseoutFunc);			
	if (!t["disc"]) {
		textElem.setAttribute("onclick", this.varname + ".clickNode('" + t["i"] + "');");
	}
	if (t["h"]) {
		textElem.setAttribute("style",
				"stroke:" + QueryTree.prototype.HIGHLIGHT_TEXT_COLOUR + ";fill:" + QueryTree.prototype.HIGHLIGHT_TEXT_COLOUR + ";");
	}
	svgElement.appendChild(textElem);
	if (t["e"] && t["c"].length > 0) { //draw black lines to all child nodes
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
		pg.setAttribute("onmouseover", this.varname + ".mouseoverNode('" + t["i"] + "', true);");
		pg.setAttribute("onmouseout", this.varname + ".mouseoutNode('" + t["i"] + "', true);");
		pg.setAttribute("onclick", this.varname + ".expandNode('" + t["i"] + "');");
		pg.setAttribute("style", "stroke:" + QueryTree.prototype.LINE_COLOUR + ";stroke-width:2;fill:white;cursor:pointer;");
		svgElement.appendChild(pg);
	}
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

QueryTree.prototype.mouseoverNode = function(pos, clickable) {
	var n = document.getElementById(this.getNodeDomId(pos));
	var te = this.treeNodeByPosition[pos];
	n.setAttribute('font-weight', 'bold');
	n.setAttribute('cursor', 'pointer');
	var qn = document.getElementById("q_" + pos);
	if (qn) { qn.setAttribute("class", "highlight"); }
}

QueryTree.prototype.mouseoutNode = function(pos, clickable) {
	var n = document.getElementById(this.getNodeDomId(pos));
	var te = this.treeNodeByPosition[pos];
	n.removeAttribute('font-weight');
	n.removeAttribute('cursor');
	var qn = document.getElementById("q_" + pos);
	if (qn) { qn.removeAttribute("class"); }
}

QueryTree.prototype.clickNode = function(pos) {
	var n = document.getElementById(this.getNodeDomId(pos));
	var te = this.treeNodeByPosition[pos];

}

QueryTree.prototype.expandNode = function(pos) {
	var n = document.getElementById(this.getNodeDomId(pos));
	var te = this.treeNodeByPosition[pos];
	if (!te["disc"] && te["c"].length > 0) {
		te["e"] = te["e"] ^ true;
		this.calcPositionAndDrawSVG();
	}
}

QueryTree.prototype.mouseoverLine = function(matchNum, nextOpr) {
	var l0 = document.getElementById("t_" + matchNum + "_0");
	var l1 = document.getElementById("t_" + matchNum + "_1");
	var l2 = document.getElementById("t_" + matchNum + "_2");
	if (nextOpr != null) {
		l0.setAttribute('cursor', 'pointer');
		l1.setAttribute('cursor', 'pointer');
		if (l2) { l2.setAttribute('cursor', 'pointer');}
	}
	l1.style.strokeWidth = 3;
	if (l2) { l2.style.strokeWidth = 3; }	
	var qn = document.getElementById("q_" + matchNum);
	if (qn) { qn.setAttribute("class", "highlight"); }
}

QueryTree.prototype.mouseoutLine = function(matchNum, nextOpr) {
	var l0 = document.getElementById("t_" + matchNum + "_0");
	var l1 = document.getElementById("t_" + matchNum + "_1");
	var l2 = document.getElementById("t_" + matchNum + "_2");
	if (nextOpr != null) {
		l0.removeAttribute('cursor');
		l1.removeAttribute('cursor');
		if (l2) { l2.removeAttribute('cursor'); }
	}
	l1.style.strokeWidth = 2;
	if (l2) { l2.style.strokeWidth = 2; }
	var qn = document.getElementById("q_" + matchNum);
	if (qn) { qn.removeAttribute("class"); }
}

QueryTree.prototype.clickLine = function(matchNum, nextOpr) {
	var l0 = document.getElementById("t_" + matchNum + "_0");
	var l1 = document.getElementById("t_" + matchNum + "_1");
	var l2 = document.getElementById("t_" + matchNum + "_2");
	if (nextOpr != null) {
		this.matches.updatePair(matchNum, nextOpr);
		var queryTextNode = document.getElementById("q_" + matchNum).firstChild;
		queryTextNode.innerHTML = QueryTree.prototype.AXIS_OPRS[nextOpr]["sym"];
		var parent = l0.parentNode;
		parent.removeChild(l0);
		parent.removeChild(l1);
		if (l2) { parent.removeChild(l2); }
		this.drawHighlightPath(parent, matchNum);		
	}
}

function Node(type) {
	this.type = type;
	this.children = [];
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
		if (text.length != 0) { div.appendChild(document.createTextNode(text)); }
	};
	this.addPostToDom = function(div) { 
		var text = this.getPostString();
		if (text.length != 0) { div.appendChild(document.createTextNode(text)); }
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
}

var LabelNode = function(opr, label, queryTreePos, treePos) {
	this.opr = opr;
	this.label = label;
	this.children = [];
	this.queryTreePos = queryTreePos;
	this.treePos = treePos;

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
	this.hasBraces = !isRoot;
	this.isNot = isNot;
	this.children = [];

	this.getPreString = function() {
		if (this.isRoot) { return ""; }
		return "[" + ( this.isNot ? "NOT" : "");
	};

	this.getPostString = function() {
		if (this.isRoot) { return ""; }
		return "]";
	};
} 
ExprNode.prototype = new Node("expr");

var AndOrNode = function(logOpr) {
	this.logOpr = logOpr;
	this.children = [];
	this.getPreString = function() {
		return  " " + this.logOpr +  " ";
	};
}
AndOrNode.prototype = new Node("andor");	

QueryTree.prototype.getMatches = function(queryString, pattern, type) {
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

	var axisOprToks = this.getMatches(queryString, axisOprPat, 'axis_opr');
	var logOprToks = this.getMatches(queryString, logOprPat, 'log_opr');
	var openExpToks = this.getMatches(queryString, openExpPat, 'open_exp');
	var closeExpToks = this.getMatches(queryString, closeExpPat, 'close_exp');
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
	for (var i = 0; i < tokens.length; i++) {
		var token = tokens[i];
		if (prevToken == null) {
			// do nothing; the prevToken assignment at the end takes care of the next case		
			//the first token has to be a / or // ('axis_opr') since it has been parsed successfully by the server already
		} else if (prevToken['type'] == 'axis_opr') {
			if (token['type'] == 'text') { 
				var opr = queryString.substring(prevToken['start'], prevToken['end']);
				var text = queryString.substring(token['start'], token['end']).trim();
				var mp = this.matches.pairs[termId];
				var node = new LabelNode(opr, text, termId, mp["e"]);
				this.matches.updateQueryNodeByPairNum(termId, node);
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
				// push the prev node to the stack and start a new expr node
				nodeStack.push(prev);
				var expr = new ExprNode(false, false);
				nodeStack[nodeStack.length - 1].addChild(expr);
				prev = expr; 
			} else if (token['type'] == 'close_exp') {
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
				prev = nodeStack.pop();
			} 
		}
		prevToken = token;
	}
	return root;
}

QueryTree.prototype.Matches = function(pairs) {
	this.pairs = pairs,
	this.init = function() {
		this.pairs.sort(function(a, b) { return a["t"] - b["t"]; }); 
		var len = this.pairs.length;
		this.byEnd = {};
		this.queryNodeByPairNum = {};
		this.treeNodesMatchingMultipleQueryTerms = {};
		this.treeNodesToQueryTerms = {};
		for (var i = 0 ; i < len; i++) {
			var end = this.pairs[i]["e"];
			this.byEnd[end] = (end in this.byEnd) ? this.byEnd[end] : [];
			this.byEnd[end].push(i);
			if (end in this.treeNodesToQueryTerms) {
				this.treeNodesToQueryTerms[end].push(this.pairs[i]["t"]);
				this.treeNodesMatchingMultipleQueryTerms[end].push(this.pairs[i]["t"]);
			} else {
				this.treeNodesToQueryTerms[end] = [this.pairs[i]["t"]];
			}
		}
	},
	this.updateQueryNodeByPairNum = function(pairNum, queryNode) {
		this.queryNodeByPairNum[pairNum] = queryNode;
	},
	this.updatePair = function(pairNum, newOpr) {
		this.pairs[pairNum]["o"] = newOpr;
		this.queryNodeByPairNum[pairNum].opr = QueryTree.prototype.AXIS_OPRS[newOpr]["sym"];
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

