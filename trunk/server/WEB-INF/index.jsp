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
<%@ page import="au.edu.unimelb.csse.Corpora"%>
<html>
<head>
<link rel="shortcut icon" href="favicon.ico" type="image/x-icon" />
<link rel="stylesheet" href="style.css" type="text/css" />
<title>Search</title>
<noscript>It seems like you either have an early version
browser or have Javascript disabled. We regret that the results can only
be displayed using Javascript.</noscript>
</head>
<body>
<div id="top">
<h3 id="pagetitle"><a href="index"
	title="Click to reach Home page">Fangorn</a></h3>
</div>
<hr />
<br />
<center>
<form method="post" action="search">
<table cellpadding="5">
	<tr>
		<td>
			Corpus: <select name="corpus">
			<% for (int i =0 ; i < Corpora.INSTANCE.corporaDirs().size(); i++) {%>
				<option value="<%=Corpora.INSTANCE.getDir(i)%>"><%=Corpora.INSTANCE.getName(i)%></option>
			<%} %>
			</select>
		</td>
		<td>Query: <input type="text" size="50" name="query" /></td>
		<td><input type="submit" value="Search" /></td>
	</tr>
</table>
</form>
</center>
<br />
<hr />
<table border="0" width="100%" cell-spacing="5" cellpadding="15">
	<COLGROUP id="faq" align="center" width="33%" />
	<COLGROUP id="ql" align="center" width="34%" />
	<COLGROUP id="abt" align="center" width="33%" />
	<tbody border="0">
		<tr>
			<td valign="top">
			<center><b>Examples</b></center>
			<p>Q. How to search for a word, say bank, in the corpus?<br />
			A. //bank <br />
			<br />
			Q. How to search for the word bank used as a noun (label: NN)?</br>
			A. //bank\NN <br />
			<br />
			Q. How to find instances of bank not used as a noun (label: NN)?</br>
			A. //bank[NOT\NN] <br />
			<br />
			Q. How to find sentences where a verb phrase (label: VP) is the root?<br />
			A. /VP <br />
			<br />
			Q. How to find sentences with noun phrases that are logical subjects
			(label: NP-LGS)?<br />
			A. //NP-LGS <br />
			<br />
			Q. How to find 3 nested adverb phrases (label: ADVP)?<br/>
			A. //ADVP//ADVP//ADVP <br/>
			<br/>
			Q. How to find sentences where an adjective phrase (label: ADJP) has a noun phrase (label: NP) as its ancestor, the noun phrase has a verb phrase (label: VP) as its immediate following sibling, the verb phrase has a gerund or present participle (label: VBG) child and the child node has a noun phrase following it?<br/>
			A. //ADJP\\NP=>VP/VBG==>NP <br/>
			<br/>
			Q. How to find verb phrases (label: VP) that contain the word gave, immediately followed by a noun phrase (label: NP), which is either immediately followed by the word to and is immediately followed by a plural noun (label:NNS), or is immediately followed by the word for?<br />
			A. //VP//gave->NP[->to->NNS OR ->for] <br />
			<br />
			Q. How to find simple declarative clauses (label: S) that contain the
			words right and turn?<br />
			A. //S[//right AND //turn] <br />
			<br />
			Q. How to find all non-3rd person singular present verbs (label:
			VBP), allow, that are followed by a locative prepositional phrase
			(label: PP-LOC)?<br />
			A. //VBP[/allow AND ==>PP-LOC]</p>
			</td>
			<td valign="top">
			<center><b>Query Language</b></center>
			<p>A query is an expression &lt;expr&gt; as described in the BNF
			below. The axis at the start of the query has to be either a
			descendant or a child axis.</p>
			<table>
				<tr>
					<td>&lt;expr&gt; ::= &lt;term&gt; [&lt;term&gt;]*</td>
				</tr>
				<tr>
					<td>&lt;term&gt; ::= &lt;axis-operator&gt;&lt;label&gt;
					[&lt;filter-expr&gt;]</td>
				</tr>
				<tr>
					<td>&lt;filter-expr&gt; ::= "[" &lt;filter-element&gt; [(AND|OR)
					&lt;filter-element&gt;]* "]" </td>
				</tr>
				<tr>
					<td>&lt;filter-element&gt; ::= [NOT] (&lt;term&gt; |
					&lt;expr&gt;)</td>
				</tr>
				<tr>
					<td>&lt;label&gt; ::= AnnotationLabel | word | punctuation</td>
				</tr>
			</table><br/>
			<b>Axis Operators</b><br />
			<br />
			<table width="100%">
				<COLGROUP align="center" width="30pt" />
				<tr>
					<td>\\</td>
					<td>Ancestor</td>
				</tr>
				<tr>
					<td>//</td>
					<td>Descendant</td>
				</tr>
				<tr>
					<td>\</td>
					<td>Parent</td>
				</tr>
				<tr>
					<td>/</td>
					<td>Child</td>
				</tr>
				<tr>
					<td>--&gt;</td>
					<td>Following</td>
				</tr>
				<tr>
					<td>-&gt;</td>
					<td>Immediate Following</td>
				</tr>
				<tr>
					<td>==&gt;</td>
					<td>Following Sibling</td>
				</tr>
				<tr>
					<td>=&gt;</td>
					<td>Immediate Following Sibling</td>
				</tr>
				<tr>
					<td>&lt;--</td>
					<td>Preceding</td>
				</tr>
				<tr>
					<td>&lt;-</td>
					<td>Immediate Preceding</td>
				</tr>
				<tr>
					<td>&lt;==</td>
					<td>Preceding Sibling</td>
				</tr>
				<tr>
					<td>&lt;=</td>
					<td>Immediate Preceding Sibling</td>
				</tr>
			</table>
			<p>The query language is a simplified version of LPath. For technical information about the complete LPath language, please see: <br /> Catherine Lai and Steven Bird (2005). LPath+: A First-Order Complete Language for Linguistic Tree Query, <i>In proceedings of the 19th Pacific Asia Conference on Language, Information and Computation</i> [ <u><a href="http://www.aclweb.org/anthology/Y05-1001.pdf">pdf</a></u> ] </p> <br/>
			</td>
			<td valign="top">
			<center><b>Corpora</b></center>
			<p>
			<table>
				<tr><td align="center"><b>Corpus</b></td><td align="center"><b>No. of sentences</b></td><td align="center"><b>Created on</b></td></tr>
				<% for (int i =0 ; i < Corpora.INSTANCE.corporaDirs().size(); i++) {%>
				<tr><td align="center"><%=Corpora.INSTANCE.getName(i)%></td><td align="center"><%=Corpora.INSTANCE.getNumberOfSentences(i)%></td><td align="center"><%=Corpora.INSTANCE.getDate(i) %></tr>
				<%} %>			
			</table>
			</p>
<br/>
			<p>The Penn Treebank Example corpus has been taken from <u><a href="http://www.nltk.org/">nltk&apos;s</a></u> demo Penn Treebank corpus.
<br/>
Mitchell P. Marcus, Mary Ann Marcinkiewicz, and Beatrice Santorini. 1993. Building a large annotated
 corpus of English: the Penn Treebank.<i> Comput. Linguist.</i> 19, 2 (June 1993), 313-330. <br/>
</p>
<br/>
<p>                                                                               
For more information on Fangorn, please see: <br/>                        
Sumukh Ghodke and Steven Bird. 2010. Fast Query for Large Treebanks. In <i>Proceedings of the 2010 A
nnual Conference of the North American Chapter of the Association for Comput. Linguist. (NAACL-HLT '
10). </i> Los Angeles, CA, USA, 267-275. [ <u><a href="http://aclweb.org/anthology/N/N10/N10-1034.pd
f">pdf</a></u> ]
</p>
			<br/>
			<br/>
			<center><b>About</b></center>
			<p>You are using a beta version of Fangorn - a treebank search application. For feedback and suggestions please send us an email at:<br />
			<OBJECT data="email.png" type="image/png"> </OBJECT></p>
			<p>A related project: <u><a
				href="http://projects.ldc.upenn.edu/QLDB/">QLDB</a></u></p>
			</td>
		</tr>
	</tbody>
</table>
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
</body>
</html>
