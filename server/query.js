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

function QueryTree(data, varname, sentnum, matchedPairs, queryString) {
	this.lastXOffset = 0;
	this.sentnum = sentnum;
	this.tree = data; // the actual tree
	this.varname = varname; // used to fire callbacks from dom elements
	this.matches = new QueryTree.prototype.Matches(matchedPairs);
	this.highlightPaths = new Array();

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

	QueryTree.prototype.AXIS_OPRS = [];
	QueryTree.prototype.AXIS_OPRS.push({'name':'descendant', 'sym':'//'});
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

	this.root = this.buildQueryTree(queryString);
	this.mapMatchesToQueryTree();
}

String.prototype.trim = function() {
	return this.toString().replace(/^ +/, '').replace(/ +$/, '');
}

QueryTree.prototype.draw = function(objid) {
	this.addDepth(this.tree, 1);
	this.addDOMID(this.tree, this.sentnum + "_0");
	//this.setDefaults(this.tree); retain the on-screen structure 
	//this.selectMatch(matchnum);
	this.calcPositionAndDrawSVG(objid);
}

QueryTree.prototype.redraw = function(objid, matchnum) {
	//this.setDefaults(this.tree);
	//this.highlightPaths = new Array();
	//this.selectMatch(matchnum);
	this.calcPositionAndDrawSVG(objid);		
}

QueryTree.prototype.calcPositionAndDrawSVG = function(objid) {
	var obj = document.getElementById(objid);
	this.addXAndYPositions();
	var svg = this.toSVG(objid);
	if (obj.childNodes.length > 0) {
		while (obj.childNodes.length > 0) {
			obj.removeChild(obj.firstChild);
		}
	}
	// var xy = this.maxXY(svg);
	var xy = {
		x :0,
		y :0
	};
	xy = this.maxXY(this.tree, xy);
	obj.setAttribute("width", xy.x + 2 * QueryTree.prototype.SVG_PADDING);
	obj.setAttribute("height", xy.y + QueryTree.prototype.SVG_PADDING);
	obj.appendChild(svg);
};

QueryTree.prototype.addXAndYPositions = function() {
	this.lastXOffset = 0;
	this.recAddXAndYPositions(this.tree);
}

QueryTree.prototype.addDOMID = function(t, domid) {
	t["domid"] = domid;
	for ( var i = 0; i < t["c"].length; i++) {
		this.addDOMID(t["c"][i], domid + "_" + i);
	}
}

QueryTree.prototype.setDefaults = function(t) {
	// expand flag
	t["e"] = (t["c"].length > 1) ? false : true;
	// disable collapse flag
	t["disc"] = false;
	t["h"] = false;
	for ( var i = 0; i < t["c"].length; i++) {
		this.setDefaults(t["c"][i]);
	}		
}	

QueryTree.prototype.setDefaultExpandParam = function(t) {
	t["e"] = (t["disc"] == true || t["c"].length < 2) ? true : false;
	for (var i = 0; i < t["c"].length; i++) {
		this.setDefaultExpandParam(t["c"][i]);
	}
}
		
QueryTree.prototype.setExpandAllExpandParam = function(t) {
	if (t["e"] == false) {
		t["e"] = true;
	}
	for (var i = 0; i < t["c"].length; i++) {
		this.setExpandAllExpandParam(t["c"][i]);
	}
}
	
QueryTree.prototype.setCollapseAllExpandParam = function() {
	this.tree["e"] = false;
}

QueryTree.prototype.selectMatch = function(index) {
	// this.matches["ms"] -> matches list; ["ms"][index] -> match set; 
	// ["ms"][index]["m"] -> list of match sets of type ["s":"start_id", "e":"end_id", "o":"operator_id"]
	var m = this.matches["ms"][index]["m"];
	for ( var p = 0; p < m.length; p++) {
		// the e here has a different meaning
		var target = m[p]["e"];
		var tarsp = target.split("_");
		var tarpi = new Array();
		for ( var j = 0; j < tarsp.length; j++) {
			tarpi.push(parseInt(tarsp[j]));
		}
		var e = this.findAndHighlight(this.tree, target, tarpi);
		if (m[p]["s"] != "") {
			target = m[p]["s"]
			tarsp = target.split("_");
			tarpi = new Array();
			for ( var j = 0; j < tarsp.length; j++) {
				tarpi.push(parseInt(tarsp[j]));
			}
			var s = this.findNode(this.tree, target, tarpi);
			this.highlightPaths.push([s, e, m[p]["o"]])
		}
	}
}

QueryTree.prototype.findNode = function(t, target, targetsplit) {
	if (t["i"] == target) {
		return t;
	} else if (t["c"].length > 0) {
		var i = 0;
		while (i < t["c"].length) {
			var cp = t["c"][i]["i"].split("_");
			if (targetsplit[1] >= parseInt(cp[1])
					&& targetsplit[0] <= parseInt(cp[0])) {
				return this.findNode(t["c"][i], target, targetsplit);
			}
			i++;
		}
	}
	return null;
}

QueryTree.prototype.findAndHighlight = function(t, target, targetsplit) {
	if (t["i"] == target) {
		t["disc"] = true;
		t["e"] = true;
		t["h"] = true;
		return t;
	} else if (t["c"].length > 0) {
		if (t["c"].length == 1) {
			t["disc"] = true;
			t["e"] = true;
			return this.findAndHighlight(t["c"][0], target, targetsplit);
		} else {
			var i = 0;
			while (i < t["c"].length) {
				var cp = t["c"][i]["i"].split("_");
				if (targetsplit[1] >= parseInt(cp[1]) && targetsplit[0] <= parseInt(cp[0])) {
					t["disc"] = true;
					t["e"] = true;
					return this.findAndHighlight(t["c"][i], target, targetsplit);
				}
				i++;
			}
		}
	}
}

QueryTree.prototype.addDepth = function(t, start) {
	t["depth"] = start;
	for ( var i = 0; i < t["c"].length; i++) {
		this.addDepth(t["c"][i], start + 1);
	}
};

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
	var matches = name.match(QueryTree.prototype.narrowRegex);
	if (matches == null) return false;
	if (matches.length >= name.length / 2) return true;
	return false;
}

QueryTree.prototype.toSVG = function(objid) {
	var svg = document.createElementNS('http://www.w3.org/2000/svg',
			'svg:svg');
	this.recToSVG(svg, objid, this.tree);
	//this.drawHighlightPaths(svg)
	var m = this.maxXY(this.tree, {
		x :0,
		y :0
	});
	svg.setAttribute("width", m.x + 2 * QueryTree.prototype.SVG_PADDING);
	svg.setAttribute("height", m.y + QueryTree.prototype.SVG_PADDING);
	return svg;
}

QueryTree.prototype.drawHighlightPaths = function(svgElement) {
	for ( var i = 0; i < this.highlightPaths.length; i++) {
		var p = this.highlightPaths[i];
		/**
		 * The numerical comparisons are based on TreeAxis's constants
		 */
		var singleLine = (p[2] > 3 && p[2] < 8) ? false : true;
		var dashedLine = (p[2] == 0 || p[2] == 1 || p[2] == 4 || p[2] == 5 || p[2] == 8
				|| [ 2 ] == 9) ? true : false;
		var colour = (p[2] < 4) ? QueryTree.prototype.BLUE_LINE_COLOUR : QueryTree.prototype.RED_LINE_COLOUR;
		var lineStyle = "stroke:" + colour + ";stroke-width:2;" + ((dashedLine) ? "stroke-dasharray:9,5;" : "");
		//All the preceding axis codes are odd numbers 
		var b = (p[2] % 2 == 1) ? p[1] : p[0];
		var a = (p[2] % 2 == 1) ? p[0] : p[1];
		var width = (b["s"]) ? QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR;
		if (p[2] < 4) { // vertical lines
			var x1 = b["x"] + (b["n"].length * width) / 2;
			var y1 = b["y"] + 2 * QueryTree.prototype.YPADDING
			width = (a["s"]) ? QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR;
			var x2 = a["x"] + (a["n"].length * width) / 2;
			var y2 = a["y"] - QueryTree.prototype.TEXT_HEIGHT - QueryTree.prototype.YPADDING;
			svgElement.appendChild(this.getSVGLine(x1, y1, x2, y2, lineStyle));
		} else { // horizontal lines
			var x1 = b["x"] + (b["n"].length * width) + 5;
			var y1 = b["y"] - QueryTree.prototype.TEXT_HEIGHT / 2 + 1;
			var x2 = a["x"] - 5;
			var y2 = a["y"] - QueryTree.prototype.TEXT_HEIGHT / 2 + 1;
			if (singleLine) {
				svgElement.appendChild(this.getSVGLine(x1, y1, x2, y2, lineStyle));
			} else {
				svgElement.appendChild(this.getSVGLine(x1, y1 - 2, x2, y2 - 2, lineStyle));
				svgElement.appendChild(this.getSVGLine(x1, y1 + 2, x2, y2 + 2, lineStyle));
			}
		}
	}
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

QueryTree.prototype.recToSVG = function(svgElement, objid, t) {
	//this function is called for each displayed node
	var textElem = document.createElementNS('http://www.w3.org/2000/svg', 'svg:text');
	textElem.appendChild(document.createTextNode(t["n"]));
	textElem.setAttribute("x", t["x"] + "px");
	textElem.setAttribute("y", t["y"] + "px");
	width = (t["s"]) ? QueryTree.prototype.WIDTH_OF_NARROW_CHAR : QueryTree.prototype.WIDTH_OF_CHAR;
	var x1 = t["x"] + (t["n"].length * width) / 2;
	var y1 = t["y"] + QueryTree.prototype.YPADDING;
	textElem.setAttribute("id", t["domid"]);
	var mouseoverFunc = this.varname + ".mouseoverNode('" + t["domid"] + "', " + ((t["disc"]) ? "false" : "true") + ");";
	var mouseoutFunc = this.varname + ".mouseoutNode('" + t["domid"] + "', " + ((t["disc"]) ? "false" : "true") + ");";
	textElem.setAttribute("onmouseover", mouseoverFunc);
	textElem.setAttribute("onmouseout", mouseoutFunc);			
	if (!t["disc"]) {
		textElem.setAttribute("onclick", this.varname + ".clickNode('"	+ objid + "', '" + t["domid"] + "');");
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
		pg.setAttribute("onmouseover", this.varname + ".mouseoverNode('" + t["domid"] + "', true);");
		pg.setAttribute("onmouseout", this.varname + ".mouseoutNode('" + t["domid"] + "', true);");
		pg.setAttribute("onclick", this.varname + ".clickNode('" + objid + "', '" + t["domid"] + "');");
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
			if (maxXYc.x > maxx) {
				maxx = maxXYc.x;
			}
			if (maxXYc.y > maxy) {
				maxy = maxXYc.y;
			}
		}
	} else if (!nod["e"] && nod["c"].length > 0) { //adding triangle height
		//shouldn't it be added only when maxy is already at max at this level?
		maxy = maxy + QueryTree.prototype.YPADDING	+ QueryTree.prototype.TRIANGLE_OFFSET + QueryTree.prototype.TRIANGLE_HEIGHT;
	}
	return {x :maxx, y :maxy};
}

QueryTree.prototype.doOnWordRange = function(chosenElem, action) {
	var s = document.getElementById(this.sentnum + '_sentence');
	if (s != null) {
		var cp = chosenElem["i"].split('_');
		left = parseInt(cp[1]);
		right = parseInt(cp[0]);
		
		for (k = 0; k < s.childNodes.length; k++) {
			sid = s.childNodes[k].getAttribute("id");
			if (sid != null) {
				wnum = sid.split('_');//sentNum_wordNum
				if (wnum != null && wnum.length > 1) {
					num = parseInt(wnum[1]);
					if (num >= left && num < right) {
						action(s.childNodes[k]);
					}
					if (!(num < right)) break;
				}
			}
		}
	}
}	
	
QueryTree.prototype.highlightSentence = function(obj) {
	obj.setAttribute("class", "highlight")
}

QueryTree.prototype.removeSentenceHighlight = function(obj) {
	obj.removeAttribute("class");
}

QueryTree.prototype.mouseoverNode = function(id, clickable) {
	var n = document.getElementById(id);
	var te = this.getTreeNode(n);
	if (clickable) {
		if (!te["disc"] && te["c"].length > 0) {
			n.setAttribute('font-weight', 'bold');
			n.setAttribute('cursor', 'pointer');
		}
	}
	this.doOnWordRange(te, QueryTree.prototype.highlightSentence);
}

QueryTree.prototype.mouseoutNode = function(id, clickable) {
	var n = document.getElementById(id);
	var te = this.getTreeNode(n);
	if (clickable) {
		if (!te["disc"] && te["c"].length > 0) {
			n.removeAttribute('font-weight');
			n.removeAttribute('cursor');
		}
	}
	this.doOnWordRange(te, QueryTree.prototype.removeSentenceHighlight);
}

QueryTree.prototype.clickNode = function(objid, id) {
	var n = document.getElementById(id);
	var te = this.getTreeNode(n);
	if (!te["disc"] && te["c"].length > 0) {
		te["e"] = te["e"] ^ true;
		this.calcPositionAndDrawSVG(objid);
	}
	this.doOnWordRange(te, QueryTree.prototype.removeSentenceHighlight);
}

QueryTree.prototype.getTreeNode = function(element) {
	var domid = element.getAttribute("id");
	var elements = domid.split("_");
	var tt = this.tree;
	for ( var i = 2; i < elements.length; i++) {
		tt = tt["c"][elements[i]];
	}
	return tt;
}	

function Node(type) {
	this.type = type;
	this.children = [];
}

Node.prototype.addChild = function(node) {
	this.children.push(node);
}

var LabelNode = function(opr, label) {
	this.opr = opr;
	this.label = label;
}
LabelNode.prototype.__proto__ = new Node("label");

var ExprNode = function(isRoot, isNot) {
	this.hasBraces = !isRoot;
	this.isNot = isNot;
} 
ExprNode.prototype.__proto__ = new Node("expr");

var AndOrNode = function(logOpr) {
	this.logOpr = logOpr;
}
AndOrNode.prototype.__proto__ = new Node("andor");

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
	var node = root;
	var nodeStack = [];
	var prev = node; // this is the prev node where control rests in the tree
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
				var node = new LabelNode(opr, text);
				prev.addChild(node);
				prev = node;
			} 
		} else if (prevToken['type'] == 'text') {
			// do nothing for 'axis_opr'
			if (token['type'] == 'log_opr') {
				var text = queryString.substring(token['start'], token['end']).trim();
				if (text == 'NOT' || text == 'not' || text == '!') {
					// the prev node has to be of the expression type
					prev.isNot = true;
				} else {
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

QueryTree.prototype.addMatchIdsToTree = function(root, matches) {
	var matched = matches.byStart[""];
	root.ids = matched[0]['e'].split("_");
} 

QueryTree.prototype.Matches = function(pairs) {
	this.pairs = pairs,
	this.root = null,
	this.byStart = {},
	this.byEnd = {},
	this.init = function() {
		var len = this.pairs.length;
		for (var i = 0 ; i < len; i++) {
			var start = this.pairs[i]["s"];
			var end = this.pairs[i]["e"];
			this.byStart[start] = (start in this.byStart) ? this.byStart[start] : [];
			this.byStart[start].push(i);
			this.byEnd[end] = (end in this.byEnd) ? this.byEnd[end] : [];
			this.byEnd[end].push(i);
		}
		var rn = this.pairs[this.byStart[""]]
		this.root = {'id':rn["e"]["i"], 'label':rn["e"]["n"], 'opr':rn["o"], 'children':[], 'parent':null};
		node = this.root;
	};
	this.init();
}

QueryTree.prototype.matchesToTree = function(byStart, node) {
//	var children = this.byStart[this.node['id']]
//	for (var i = 0; i < children.length; i++ ) {
//		var child = children[i];
//		var childNode = {'id':child["e"]["i"], 'label':child["e"]["n"], 'opr':child["o"], 'children':[], 'parent':node}; 
//		node.children.push(childNode);
//		this.matchesToTree(byStart, childNode);
//	} 
	
}

QueryTree.prototype.mapMatchesToQueryTree = function() {
	
}

