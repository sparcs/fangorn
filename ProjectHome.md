### Introduction ###
Fangorn is a tool to search for structural patterns in large collections of linguistically annotated trees.

### Features ###
Here are some of the features it currently supports:
  * It indexes syntax trees of the Penn Treebank II format
  * Supports structured path queries similar to XPath
  * Query language supports these relations between nodes: ancestor, descendant, parent, child, following, preceding, immediate-following, immediate-preceding, following-sibling, preceding-sibling, immediate-following-sibling, immediate-preceding-sibling, and logical operators.
  * Implements a fast search algorithm using the Apache Lucene search engine toolkit
  * Runs within an embedded Jetty server to provide browser based access over a network
  * The UI provides interactive trees that can be exported in SVG and plain text format

### WebDemo ###
  * [nltk.ldc.upenn.edu:9090](http://nltk.ldc.upenn.edu:9090/index) contains three corpora
    * The limited Penn Treebank corpus from nltk (packaged with the standard install)
    * 500k Wikipedia sentences annotated with the Charniak-Johnson parser
    * 5 million Wikipedia sentences annotated with the Charniak-Johnson parser

### Citations ###
<a href='Hidden comment: 
Mitchell P. Marcus, Mary Ann Marcinkiewicz, and Beatrice Santorini. 1993. *Building a large annotated corpus of English: the Penn Treebank*. Computational Linguistics 19, 2 (June 1993), 313-330.

Eugene Charniak and Mark Johnson. 2005. *Coarse-to-fine n-best parsing and MaxEnt discriminative reranking*. In Proceedings of the 43rd Annual Meeting on Association for Comput. Linguist. (ACL "05). Stroudsburg, PA, USA, 173-180.

Nordlinger, Rachel. 1998. *A Grammar of Wambaya, Northern Territory (Australia)*. Canberra: Pacific Linguistics.

Emily M. Bender. 2010. *Reweaving a Grammar for Wambaya: A Case Study in Grammar Engineering for Linguistic Hypothesis Testing*. Linguistic Issues in Language Technology 3(3). pp.1-34.
'></a>

Sumukh Ghodke and Steven Bird. 2012. **Fangorn: A system for querying very large treebanks**. In Proceedings of the 24th International Conference on Computational Linguistics (COLING '12). Mumbai, India.

Sumukh Ghodke and Steven Bird. 2010. **Fast Query for Large Treebanks**. In Proceedings of the 2010 Annual Conference of the North American Chapter of the Association for Computational Linguistics (NAACL-HLT '10). Los Angeles, CA, USA, pp.267-275.