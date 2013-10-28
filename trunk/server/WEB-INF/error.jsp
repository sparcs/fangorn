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
<%@ page import="au.edu.unimelb.csse.Corpora"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

<link rel="shortcut icon" href="favicon.ico" type="image/x-icon" />
<link rel="stylesheet" href="style.css" type="text/css" />
<title>Error</title>
<script type="text/javascript">
function render() {
	selectInList("corpus_select_1", '<%=request.getAttribute("corpus")%>' );
}

function selectInList(listid, item) {
	var l = document.getElementById(listid);
	for (i = 0; i < l.options.length; i++) {
		if (l.options[i].value == item) {
			l.options[i].selected = true;
			break;
		}
	}
}
</script>
</head>
<body onload="render();">
<%@include file="title.jspf" %>
<hr />
<br />
<center>
<form method="post" action="search">
<table cellpadding="5">
	<tr>
		<td>
			Corpus: <select id="corpus_select_1" name="corpus">
			<% for (int i =0 ; i < Corpora.INSTANCE.corporaDirs().size(); i++) {%>
				<option value="<%=Corpora.INSTANCE.getDir(i)%>"><%=Corpora.INSTANCE.getName(i)%></option>
			<%} %>
			</select>
		</td>
		<td><input type="text" size="50" name="query"
			value="<%=request.getParameter("query")%>" /></td>
		<td><input type="submit" value="Search" /></td>
	</tr>
</table>
</form>
</center>
<br />
<hr />
<div id="resultdiv"><%=request.getAttribute("error")%></div>
<hr />
<table width="100%">
	<tr>
		<td></td>
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
<hr />
<table width="100%">
	<tr>
		<td>
		<center><b>Browser Requirements</b><br />
		<p>Result trees require JavaScript to render correctly. 
        A desktop resolution of 1024x768 or higher is recommended.</p>
		</center>
		</td>
	</tr>
</table>
</body>
</html>
