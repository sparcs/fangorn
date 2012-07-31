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
function QueryTree(data, varname, sentnum, matches) {
	this.lastXOffset = 0;
	this.sentnum = sentnum;
	this.tree = data; // the actual tree
	this.varname = varname; // used to fire callbacks from dom elements
	this.matches = matches;
	this.highlightPaths = new Array();
	this._leaves = new Array();
	this.leavesSet = false;

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

	QueryTree.prototype.draw = function(objid) {
		this.addDepth(this.tree, 1);
		this.addDOMID(this.tree, this.sentnum + "_0");
		//this.setDefaults(this.tree); retain the on-screen structure 
		this.leavesSet = true;
		//this.selectMatch(matchnum);
		this.calcPositionAndDrawSVG(objid);
	};
	
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
	};

	QueryTree.prototype.addDOMID = function(t, domid) {
		t["domid"] = domid;
		for ( var i = 0; i < t["c"].length; i++) {
			this.addDOMID(t["c"][i], domid + "_" + i);
		}
	};

	QueryTree.prototype.setDefaults = function(t) {
		// expand flag
		t["e"] = (t["c"].length > 1) ? false : true;
		// disable collapse flag
		t["disc"] = false;
		t["h"] = false;
		for ( var i = 0; i < t["c"].length; i++) {
			this.setDefaults(t["c"][i]);
		}		
		if (this.leavesSet == false) {
			if (t["c"].length == 0) {
				this._leaves.push(t["n"]);
			}
		}
	};
	
	QueryTree.prototype.leaves = function() {
		return this._leaves;
	};	
	
	QueryTree.prototype.setDefaultExpandParam = function(t) {
		t["e"] = (t["disc"] == true || t["c"].length < 2) ? true : false;
		for (var i = 0; i < t["c"].length; i++) {
			this.setDefaultExpandParam(t["c"][i]);
		}
	};
		
	QueryTree.prototype.setExpandAllExpandParam = function(t) {
		if (t["e"] == false) {
			t["e"] = true;
		}
		for (var i = 0; i < t["c"].length; i++) {
			this.setExpandAllExpandParam(t["c"][i]);
		}
	};
	
	QueryTree.prototype.setCollapseAllExpandParam = function() {
		this.tree["e"] = false;
	};

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
	};

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
	};

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
	};
	
	QueryTree.prototype.recShiftX = function(t, x) {
		t["x"] = t["x"] + x;
		t["x-end"] = t["x-end"] + x;
		if (t["e"] && t["c"].length > 0) {
			for (var i = 0; i < t["c"].length; i++) {
				this.recShiftX(t["c"][i], x);
			}
		}
	};
	
	QueryTree.prototype.mainlyNarrowChars = function(name) {
		var matches = name.match(QueryTree.prototype.narrowRegex);
		if (matches == null) return false;
		if (matches.length >= name.length / 2) return true;
		return false;
	};

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
	};

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
	};

	QueryTree.prototype.getSVGLine = function(x1, y1, x2, y2, style) {
		var lineElem = document.createElementNS('http://www.w3.org/2000/svg', 'svg:line');
		lineElem.setAttribute("x1", x1 + "px");
		lineElem.setAttribute("y1", y1 + "px");
		lineElem.setAttribute("x2", x2 + "px");
		lineElem.setAttribute("y2", y2 + "px");
		lineElem.setAttribute("style", style);
		return lineElem;
	};

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
	};

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
	};

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
	};	
	
	QueryTree.prototype.highlightSentence = function(obj) {
		obj.setAttribute("class", "highlight")
	};
	
	QueryTree.prototype.removeSentenceHighlight = function(obj) {
		obj.removeAttribute("class");
	};

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
	};
	
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
	};

	QueryTree.prototype.clickNode = function(objid, id) {
		var n = document.getElementById(id);
		var te = this.getTreeNode(n);
		if (!te["disc"] && te["c"].length > 0) {
			te["e"] = te["e"] ^ true;
			this.calcPositionAndDrawSVG(objid);
		}
		this.doOnWordRange(te, QueryTree.prototype.removeSentenceHighlight);
	};

	QueryTree.prototype.getTreeNode = function(element) {
		var domid = element.getAttribute("id");
		var elements = domid.split("_");
		var tt = this.tree;
		for ( var i = 2; i < elements.length; i++) {
			tt = tt["c"][elements[i]];
		}
		return tt;
	};
	
	QueryTree.prototype.pennTreebankSentence = function() {
		return '(' + this.recSentence(this.tree) + ')';
	};
	
	QueryTree.prototype.recSentence = function(node) {
		if (node["c"] == 0) {
			return ' ' + node["n"];
		}
		var s = '(' + node["n"];
		for (var i = 0; i < node["c"].length; i++) {
			s = s + this.recSentence(node["c"][i]);
		}
		s = s + ')';
		return s;
	};
	
	QueryTree.prototype.displayDefault = function(objid) {
		if (this.tree["e"] == false) {
			this.tree["e"] = true;
		}
		this.setDefaultExpandParam(this.tree);
		this.calcPositionAndDrawSVG(objid);
	};
	
	QueryTree.prototype.displayAllExpanded = function(objid) {
		this.setExpandAllExpandParam(this.tree);
		this.calcPositionAndDrawSVG(objid);
	};
	
	QueryTree.prototype.displayCollapsed = function(objid) {
		this.setCollapseAllExpandParam();
		this.calcPositionAndDrawSVG(objid);
	};
	
	QueryTree.prototype.getBasicDataString = function() {
		return this.recGetBasicData(this.tree);
	}
	
	QueryTree.prototype.recGetBasicData = function(node) {
		var str = "{\"n\":\"";
		str = str + node["n"];
		str = str + "\",\"x\":\"" + node["x"] + "\",\"y\":\"" + node["y"] + "\"";
		if (node["c"].length > 0) {
			if (!node["e"]) {
				str = str + ",\"tri\":\"true\"";
			} else {
				str = str + ",\"c\":[";
				for (var i = 0; i < node["c"].length; i++) {
					str = str + this.recGetBasicData(node["c"][i]);
					if (i != node["c"].length - 1) {
						str = str + ",";
					}
				}
				str = str + "]";
			}
		}
		str = str + "}";
		return str;
	}
}
