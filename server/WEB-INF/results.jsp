<?xml version="1.0" encoding="UTF-8"?>
<%
	response.setContentType("application/xhtml+xml");
%>
<!--
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
-->
<%@ page import="au.edu.unimelb.csse.search.complete.Result,au.edu.unimelb.csse.Corpora"%>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

<link rel="shortcut icon" href="favicon.ico" type="image/x-icon" />
<link rel="stylesheet" href="style.css" type="text/css" />
<title>Results</title>
<noscript>
<div class="error">
Your browser either doesn't support Javascript or you have
turned it off. This page requires Javascript to display results.
</div>
</noscript>
<script type="text/ecmascript" src="tree.js"></script>
<script type="text/ecmascript" src="query.js"></script>
<script type="text/ecmascript" src="jquery-1.7.2.min.js"></script>
<script type="text/ecmascript">
<![CDATA[
	var newwindow;
	var queryTree;
<%int totalhits = (Integer) request.getAttribute("totalhits");
			String[] results = (String[]) request.getAttribute("results");
			Result[] resultMeta = (Result[]) request.getAttribute("metadata");
			int pageNum = (Integer) request.getAttribute("pagenum");
			int totalPages = totalhits / 10 + 1;
			for (int i = 0; i < 10 && i < results.length; i++) {%>
		var row_<%=i%>_match = 0;
		var row_<%=i%>_match_total = <%=resultMeta[i].numberOfMatches()%>;
		var t<%=i%> = <%=results[i]%>;
		var matches<%=i%> = <%=resultMeta[i].matchesAsJSONString()%>;
		var tree<%=i%> = new Tree(t<%=i%>, "tree<%=i%>", "<%=i%>", matches<%=i%>);
		
<%}%>


function render() {
	if (navigator.userAgent.indexOf("Firefox") == -1)
        	alert("To see this page as it is meant to appear please use a Mozilla Firefox browser.");
    var dec_qry_str = decodeSymbols(document.forms["stateinf"].p.value);
	document.getElementById("query_fld_1").setAttribute("value",dec_qry_str);
	var qryFld2 = document.getElementById("query_fld_2");
	if (qryFld2 != null) qryFld2.setAttribute("value",dec_qry_str);
	selectInList("corpus_select_1", '<%=request.getAttribute("corpus")%>');
	selectInList("corpus_select_2", '<%=request.getAttribute("corpus")%>');
<%for (int i = 0; i < 10 && i < results.length; i++) {%>
		tree<%=i%>.draw("row<%=i%>", 0);
		var leaves = tree<%=i%>.leaves();
		sentence_row = document.getElementById("<%=i%>_sentence");
		for (j = 0; j < leaves.length; j++) {
			sp = document.createElement("span");
			sp.setAttribute("id", "<%=i%>_" + j);
			sp.appendChild(document.createTextNode(' ' + leaves[j]  + ' '));
			sentence_row.appendChild(sp);
		}
		document.getElementById("row_<%=i%>_match_prev").className='greynav';
		document.getElementById("row_<%=i%>_match_next").className= row_<%=i%>_match_total == 1 ? 'greynav' : 'normalnav';
<%}%>
	enableDisablePageNavigation(<%=pageNum%>, <%=totalPages%>);    
}

function decodeSymbols(queryStr) {
	return queryStr.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/\\\\/g, '\\').replace(/&quot;/g, '"').replace(/&amp;/g, '&');
}

function enableDisablePageNavigation(pageNum, totalPages) {
	document.getElementById("page_prev").className= pageNum == 1 ? 'greynav' : 'normalnav';
	document.getElementById("page_next").className= pageNum == totalPages ? 'greynav' : 'normalnav';
	var pagePrev = document.getElementById("page_scroll_prev");
	if (pagePrev != null) { pagePrev.className= pageNum < 6 ? 'greynav' : 'normalnav'; }
	var pageNext = document.getElementById("page_scroll_next"); 
	if (pageNext != null) { pageNext.className= pageNum >= totalPages - 5 ? 'greynav' : 'normalnav'; }
}

function selectInList(listid, item) {
	var l = document.getElementById(listid);
	if (l != null) {
		for (i = 0; i < l.options.length; i++) {
			if (l.options[i].value == item) {
				l.options[i].selected = true;
				break;
			}
		}
	}
}

function prev(id) { // id is of the form row_i_match where i is the row number
	if (eval(id) < 1) {
		document.getElementById(id + "_prev").className='greynav';
	} else {
		var p = eval(id) - 1;
		document.getElementById(id).innerHTML = eval(p + 1);
		if (p < eval(id + "_total") - 1) {
			document.getElementById(id + "_next").className = 'normalnav';
		}
		if (p <= 0) {
			document.getElementById(id + "_prev").className='greynav';
		}
		var rownum = id.split("_", 2);
		var num = rownum[1];
		var t = eval("tree" + num);
		t.redraw(rownum.join(""), p);
		eval(id + "=" + p);
	}
}

function next(id) { // id is of the form row_i_match where i is the row number
	if (eval(id) == eval(id + "_total") - 1) {
		document.getElementById(id + "_next").className='greynav';
	} else {
		var n = eval(id) + 1;
		document.getElementById(id).innerHTML = eval(n + 1);
		if (n > 0) {
			document.getElementById(id + "_prev").className = 'normalnav';
		}
		if (n >= eval(id + "_total") - 1) {
			document.getElementById(id + "_next").className='greynav';
		}
		var rownum = id.split("_", 2);
		var num = rownum[1];
		var t = eval("tree" + num);
		t.redraw(rownum.join(""), n);
		eval(id + "=" + n);
	}
}

function openPennTreebankSentence(num) {
	if (newwindow && !newwindow.closed) { 
		newwindow.focus(); newwindow.document.clear();
	} else { 
		var tr = eval("tree" + num);
		var s = tr.pennTreebankSentence();
		newwindow=window.open('','Sentence ' + num,'width=690,height=300,resizable=1');
		newwindow.document.writeln('<html> <head> <link rel="shortcut icon" href="favicon.ico" type="image\/x-icon" \/><link rel="stylesheet" href="style.css" type="text\/css" \/> <title>Sentence ' + num + '<\/title> <\/head> <body>' + s + '<\/body> <\/html>');
		newwindow.document.close();
	}
}

function openSVGSentence(num) {
	var tr = eval("tree" + num);
	var str = tr.getBasicDataString();
	var frm = document.forms["sent" + num];
	var rowobj = document.getElementById("row" + num);
	frm.h.value = rowobj.getAttribute("height");
	frm.w.value = rowobj.getAttribute("width");
	frm.json.value = str;
	frm.submit();
}

function buildQuery(num) {
	document.getElementById('overlay').className = 'overlayshow';
	var rowObj = document.getElementById("row" + num);
	var modalWidth = rowObj.width;
	var queryWindow = document.getElementById('querywindow');
	queryWindow.className = 'querywindowshow';
	$("#querywindow").css({
		left:Math.max($(window).width() - modalWidth, 0) / 2 + $(window).scrollLeft()
	});
	var resultTree = eval("tree" + num);
	var matches = eval("matches" + num);
	var match = parseInt(eval("row_" + num + "_match"));
	var qry_str = decodeSymbols(document.forms["stateinf"].p.value);
	var queryTree = new QueryTree(clone(resultTree.tree), 'queryTree', 11, 'querywindowtext', 'querywindowtree', clone(matches["ms"][match]["m"]), qry_str); //assigned an id 11 as the first 10 are already displayed
	if (queryTree.matches.hasTreeNodesMatchingMultipleQueryTerms()) {
		queryTree.displayLoopError();
	} else {
		queryTree.draw();
	}
}

function clone(obj) {
    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) return obj;

    // Handle Array
    if (obj instanceof Array) {
        var copy = [];
        var len = obj.length;
        for (var i = 0; i < len; ++i) {
            copy[i] = clone(obj[i]);
        }
        return copy;
    }

    // Handle Object
    if (obj instanceof Object) {
        var copy = {};
        for (var attr in obj) {
            if (obj.hasOwnProperty(attr)) copy[attr] = clone(obj[attr]);
        }
        return copy;
    }

    throw new Error("Unable to copy obj! Its type isn't supported.");
}


function setModalPosition(modalWin, height, width) {
	modalWin.style.left = Math.max(window.innerWidth - parseInt(width), 0) / 2;
}


function closeQueryWindow(num) {
	document.getElementById('overlay').className = 'overlayhide'
	document.getElementById('querywindow').className = 'querywindowhide';
	
	var divNode = document.getElementById('querywindowtree');
	removeAllChildren(divNode);
	
	divNode = document.getElementById('querywindowtext');
	removeAllChildren(divNode);
}

function removeAllChildren(myNode) {
	while (myNode.firstChild) {
		removeAllChildren(myNode.firstChild);
    	myNode.removeChild(myNode.firstChild);
	}
}

function page(num) {
	var frm = document.forms["stateinf"];
	frm.j.value = num;
	frm.submit();
}

function expandAll(num) {
	eval("tree" + num).displayAllExpanded("row" + num);
}

function collapseAll(num) {
	eval("tree" + num).displayCollapsed("row" + num);
}

function defaultDisplay(num) {
	eval("tree" + num).displayDefault("row" + num);
}
]]>
</script>
</head>
<body onload="render();">
<div id="top">
<h3 id="pagetitle"><a href="index" title="Click to reach Home page">Fangorn</a></h3>
</div>
<a name="top"></a>
<hr />
<br />
<center>
<form method="post" action="search">
<table cellpadding="5">
	<tr>
		<td>Corpus: <select id="corpus_select_1" name="corpus">
			<%
				for (int i = 0; i < Corpora.INSTANCE.corporaDirs().size(); i++) {
			%>
			<option value="<%=Corpora.INSTANCE.getDir(i)%>"><%=Corpora.INSTANCE.getName(i)%></option>
			<%
				}
			%>
		</select></td>
		<td>Query: <input id="query_fld_1" type="text" size="50"
			name="query" /></td>
		<td><input type="submit" value="Search" /></td>
	</tr>
</table>
</form>
</center>
<br />
<hr />
<table id="resultstat">
	<tr>
		<%
			if (totalhits == 0) {
		%>
		<td align="left" id="numrofresults">0 sentences match your query</td>
		<%
			} else {
				int sentStart = (pageNum - 1) * 10 + 1;
				int sentEnd = sentStart + 9 <= totalhits ? sentStart + 9
						: totalhits;
		%>
		<td align="left" id="numrofresults">Results <%=sentStart%> to <%=sentEnd%>
		of <%=totalhits%></td>
		<%
			}
		%>
		<td align="right" id="searchtime">Search time: <%=request.getAttribute("searchtime")%></td>
	</tr>
</table>
<div id="resultdiv">
<table width="100%">
	<tr>
		<td>
		<table width="100%">
			<%
				for (int i = 0; i < 10 && i < results.length; i++) {
			%>
			<tr class='<%=i % 2 == 0 ? "oddrow" : "evenrow"%>'>
				<td>
				<div
					style="overflow-x: auto; overflow-y: auto; text-align: center; margin: auto; width: 100%;">

				<div>
				<table width="99.99%">
					<tr>
						<td>
						<center id="<%=i%>_sentence"></center>
						</td>
					</tr>
					<tr>
						<td>
						<table width="99.99%">
							<tr>
								<td align="left" class="navbarleft">
								<button name="ExpandAll" type="button" class="navbarbtn" onclick='expandAll(<%=i%>)'>Expand all<br /> nodes</button>
								<button name="CollapseTree" type="button" class="navbarbtn" onclick='collapseAll(<%=i%>)'>Collapse<br /> tree</button>
								<button name="DefaultDisplay" type="button" class="navbarbtn" onclick='defaultDisplay(<%=i%>)'>Default<br /> display</button>
								</td>
								<td align="center" valign="middle" class="navbarmiddle"><span>
								<span id='<%="row_" + i + "_match_prev"%>' title='Previous match in tree' onclick='prev("<%="row_" + i + "_match"%>");'>&lt;</span> Match
								<span id='<%="row_" + i + "_match"%>'>1</span> of <%=resultMeta[i].numberOfMatches()%>
								<span id='<%="row_" + i + "_match_next"%>' title='Next match in tree' onclick='next("<%="row_" + i + "_match"%>");'>&gt;</span> 
								</span></td>
								<td align="right" class="navbarright">
								<table>
									<tr>
										<td>
										    <button name="BuildQuery" type="button" class="navbarbtn"
											   onclick='buildQuery(<%=i%>)' align='right'>Build query<br /> from tree</button>
										</td>
									
										<td>
										<button name="PennTreebankSentence" type="button" class="navbarbtn"
											onclick='openPennTreebankSentence(<%=i%>)' align='right'>View tree in<br />bracketed form</button>
										</td>
										<td>
										<form id="sent<%=i%>" style="margin:0px;" action="sentence.svg" method="post" target="_blank">
										    <input type="hidden" name="json" /> 
										    <input type="hidden" name="h" />
										    <input type="hidden" name="w" />
										    <button name="SVGSentence" type="submit" value="submit" class="navbarbtn"
											   onclick='openSVGSentence(<%=i%>)' align='right'>View tree as<br /> SVG image</button>
										</form>
										</td>
									</tr>
								</table>
								</td>
							</tr>
						</table>
						</td>
					</tr>
				</table>
				</div>
				<div style="margin-top:-20px;"><object id="row<%=i%>" name="svg" type="image/svg+xml"></object>
				</div>
				</div>
				</td>
			</tr>
			<%
				}
			%>
		</table>
		</td>
	</tr>
</table>
</div>
<hr />
<%
	if (totalhits > 0) {
%>
<table width="100%">
	<tr>
		<td></td>
	</tr>
	<tr>
		<td>
		<center><span> <span id='<%="page_prev"%>'
			onclick='page(<%=pageNum - 1%>);'>&lt;</span> Page <%=pageNum%> of <%=totalPages%>
		<span id='<%="page_next"%>' onclick='page(<%=pageNum + 1%>);'>&gt;</span>
		</span></center>
		</td>
	</tr>
	<tr>
		<td>
			<form id="stateinf" action="paging" method="post">
			<input type="hidden" name="j" /> 
			<input type="hidden" name="h" value="<%=request.getAttribute("hash")%>" /> 
			<input type="hidden" name="p" value="<%=request.getAttribute("query-view")%>" /> 
			<input type="hidden" name="c" value="<%=request.getAttribute("corpus")%>" />
			<input type="hidden" name="d" value="<%=request.getAttribute("docnums")%>" /> 
			<input type="hidden" name="t" value="<%=totalhits%>" />
			</form>
		<%
			int startPage = pageNum <= 5 ? 1 : pageNum - 5;
			int endPage = startPage + 9 <= totalPages ? startPage + 9 : totalPages;
			if (startPage != endPage) {
		%>
			<center><span> 
				<span id='<%="page_scroll_prev"%>' title='Previous page' onclick='page(<%=startPage - 1%>);'>&lt;&lt;</span>
		<%
			for (int i = startPage; i <= endPage; i++) {
				if (i == pageNum) {
		%> 			<span class='greynav'><%=i%> </span> <%
 				} else {
 		%> 			<span class='normalnav' onclick='page(<%=i%>);' title='Go to page <%=i%>'><u><%=i%></u> </span> 
		<%		 }
 			}
 		%> 	<span id='<%="page_scroll_next"%>' title='Next page' onclick='page(<%=endPage + 1%>);'>&gt;&gt;</span>
			</span></center>
		<%
			}
		%>
		</td>
	</tr>
	<tr>
		<td></td>
	</tr>
</table>
<hr />
<table width="100%">
	<tr>
		<td></td>
	</tr>
	<tr>
		<td>
		<center><a href="index.html" title="Click to reach Home page"><b><u>Home</u></b></a>
		<a href="#top" title="Back to top"><b><u>Top</u></b></a></center>
		</td>
	</tr>
	<tr>
		<td><br />
		<center>
		<form method="post" action="search">
		<table cellpadding="5">
			<tr>
				<td>Corpus: <select id="corpus_select_2" name="corpus">
					<%
						for (int i = 0; i < Corpora.INSTANCE.corporaDirs().size(); i++) {
					%>
					<option value="<%=Corpora.INSTANCE.getDir(i)%>"><%=Corpora.INSTANCE.getName(i)%></option>
					<%
						}
					%>
				</select></td>
				<td>Query: <input id="query_fld_2" type="text" size="50"
					name="query" /></td>
				<td><input type="submit" value="Search" /></td>
			</tr>
		</table>
		</form>
		</center>
		<br />
		</td>
	</tr>
</table>
<%
	} else {
%>
<table width="100%">
	<tr>
		<td><form id="stateinf" action="paging" method="post">
			<input type="hidden" name="p" value="<%=request.getAttribute("query-view")%>" /> 
			</form></td>
	</tr>
	<tr>
		<td>
		<center><a href="index.html" title="Click to reach Home page"><b><u>Home</u></b></a>
		</center>
		</td>
	</tr>
	<tr>
		<td></td>
	</tr>
</table>
<%
	}
%>
<hr />
<table width="100%">
	<tr>
		<td>
		<center><b>Browser Requirements</b><br />
		<p>Result trees are rendered correctly only in Mozilla Firefox
		with Javascript turned on. A desktop resolution of 1024x768 or higher
		is recommended.</p>
		</center>
		</td>
	</tr>
</table>
<div id="overlay" class="overlayhide"></div>
<div id="querywindow" class="querywindowhide">
	<img src="close.png" class="close" onclick="closeQueryWindow()"></img>
	<table id="querytexttable"><tr>
	<td><div id="querywindowtext"> </div></td>
	<td><button name="QuerySearch" type="button" class="navbarbtn" onclick='searchQuery()' align='right'>Search</button></td>
	<td><button name="QuerySearch" type="button" class="navbarbtn" onclick='copyToSearchBar()' align='right'>Copy to <br/> search bar</button></td>
	</tr></table>
	<hr/>
	<table id="querytreetable"><tr width="99%">
	<td><div id="querywindowtree"> </div></td>
	</tr></table>
</div>
</body>
</html>
