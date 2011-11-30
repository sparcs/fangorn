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
var sent;
var position;

function sentAsDom(sentence) {
	sent = sentence.slice(0, sentence.length - 1);
	position = 0;
	var svgElement = document.createElementNS('http://www.w3.org/2000/svg',
			'svg:svg');
	svgElement.appendChild(asDom());
	return svgElement;
}

function asDom() {
	var data = null;
	if (sent.charAt(position) != '(') {
		return 'Sorry! cannot parse this sentence';
	}
	data = document.createElement('g');
	position += 1;
	var text = '';
	var currentG = data;
	while (position < sent.length && sent.charAt(position) != ')') {
		if (sent.charAt(position) == '(') {
			if (text != '') {
				var texte = document.createElementNS(
						'http://www.w3.org/2000/svg', 'svg:text');
				var textn = document.createTextNode(text);
				texte.appendChild(textn);
				var line = document.createElementNS(
						'http://www.w3.org/2000/svg', 'svg:line');
				currentG.appendChild(texte);
				currentG.appendChild(line);
			}
			var dom = asDom();
			if (dom != null) {
				data.appendChild(dom);
			}
			text = '';
		} else if (sent.charAt(position) == ' ') {
			position += 1;
			var texte = document.createElementNS('http://www.w3.org/2000/svg',
					'svg:text');
			var textn = document.createTextNode(text);
			texte.appendChild(textn);
			var line = document.createElementNS('http://www.w3.org/2000/svg',
					'svg:line');
			currentG.appendChild(texte);
			currentG.appendChild(line);

			var newG = document.createElement('g');
			data.appendChild(newG);
			currentG = newG;
			text = '';
		} else {
			text = text + sent.charAt(position);
			position += 1;
		}
	}
	if (sent.charAt(position) == ')') {
		position += 1;
		if (text != '') {
			var texte = document.createElementNS('http://www.w3.org/2000/svg',
					'svg:text');
			var textn = document.createTextNode(text);
			texte.appendChild(textn);
			var line = document.createElementNS('http://www.w3.org/2000/svg',
					'svg:line');
			currentG.appendChild(texte);
			currentG.appendChild(line);
		}
	}
	return data;
}

function removeGs(svgElement) {
	var uwElements = svgElement.getElementsByTagName('g');
	while (uwElements.length > 0) {
		var toremove = uwElements.item(0);
		var par = toremove.parentNode;
		while (toremove.childNodes.length > 0) {
			par.appendChild(toremove.firstChild);
		}
		par.removeChild(toremove);
		uwElements = svgElement.getElementsByTagName('g');
	}
}

function removeEmptyLines(svgElement) {
	var lineElements = svgElement.getElementsByTagName('svg:line');
	for ( var i = 0; i < lineElements.length; i++) {
		le = lineElements.item(i);
		if (le.attributes.length == 0) {
			le.parentNode.removeChild(le);
		}
	}
}

function maxXY(domE) {
	var maxX = 0;
	var maxY = 0;
	var thisX,thisY,itm;
	var texts = domE.getElementsByTagName('svg:text');
	for (var i = 0; i < texts.length; i++) {
		itm = texts.item(i);
		thisX = parseFloat(itm.getAttribute('x').replace(/px/, ''));
		thisX = thisX + itm.firstChild.nodeValue.length
		if (thisX > maxX) {	
			maxX = thisX;
		}
		thisY = parseFloat(itm.getAttribute('y').replace(/px/, ''));
		if (thisY > maxY) {
			maxY = thisY;
		}
	}
	return {x:maxX, y:maxY};
}

function rec_maxXY(dom) {
	
}
