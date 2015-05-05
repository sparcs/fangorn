# Fangorn Query Language #

Fangorn specifies tree patterns using a path query language. A path query expression is a navigation from one node to another, and from that node to yet another, and so on, thus forming a path from the first node specified to the last node in the sequence. An operator specifies the structural relationship between each pair of node labels. The first operator in the path should always be either a child or a descendant operator, signifying that the path starts at the root or anywhere in the tree, respectively. Paths may also branch out to two or more nodes and the branches are combined using logical operators.

We suggest you read through this query language manual in order. The description of the syntax is first described in BNF language. Even if you prefer an explanation in plain English to BNF, we suggest you read the Syntax section as it shows the symbols used for structural navigation in a tree and also provides examples for each operator. Later sections provide guides on constructing tree queries. The examples in this manual assume some familiarity with the Penn Treebank Annotation format. Check out this [webpage](http://bulba.sdsu.edu/jeanette/thesis/PennTags.html) if you are unsure of the meanings of some labels.

## Syntax ##

For those of you who like reading BNF the query language can be summarised as follows:

```
<expr> ::= <term> [<term>]*
<term> ::= <axis-operator><node-label> [<filter-expr>]
<filter-expr> ::= "[" <filter-element> [(AND|OR) <filter-element>]* "]"
<filter-element> ::= [NOT] <expr>
<node-label> ::= annotation_label | word | punctuation
```

Open and close square brackets (`[` and `]`) signify optional elements except when shown within double quotes, as in the definition of `<filter-expr>`. There it signifies an actual open and close bracket in the query syntax. Other symbols have their regular BNF language meaning. The AND, OR and NOT logical operators can be replaced by other alternatives shown in the table later in this section.

The structural operators (or axis operators) supported by Fangorn are represented using the following symbols:

| **Operator** | **Symbol** | **Example(s)** |
|:-------------|:-----------|:---------------|
| Descendant | // | ![http://fangorn.googlecode.com/svn/wiki/images/descendant.png](http://fangorn.googlecode.com/svn/wiki/images/descendant.png) |
| Child | / | ![http://fangorn.googlecode.com/svn/wiki/images/child.png](http://fangorn.googlecode.com/svn/wiki/images/child.png) |
| Ancestor | \\ | ![http://fangorn.googlecode.com/svn/wiki/images/ancestor.png](http://fangorn.googlecode.com/svn/wiki/images/ancestor.png) |
| Parent | \ | ![http://fangorn.googlecode.com/svn/wiki/images/parent.png](http://fangorn.googlecode.com/svn/wiki/images/parent.png) |
| Following Sibling | ==> | ![http://fangorn.googlecode.com/svn/wiki/images/followingsibling.png](http://fangorn.googlecode.com/svn/wiki/images/followingsibling.png) |
| Immediately Following Sibling | => | ![http://fangorn.googlecode.com/svn/wiki/images/immediatefollowingsibling.png](http://fangorn.googlecode.com/svn/wiki/images/immediatefollowingsibling.png) |
| Preceding Sibling | <== | ![http://fangorn.googlecode.com/svn/wiki/images/precedingsibling.png](http://fangorn.googlecode.com/svn/wiki/images/precedingsibling.png) |
| Immediately Preceding Sibling | <= | ![http://fangorn.googlecode.com/svn/wiki/images/immediateprecedingsibling.png](http://fangorn.googlecode.com/svn/wiki/images/immediateprecedingsibling.png) |
| Following  | --> | ![http://fangorn.googlecode.com/svn/wiki/images/following.png](http://fangorn.googlecode.com/svn/wiki/images/following.png) |
| Immediately Following | -> | ![http://fangorn.googlecode.com/svn/wiki/images/immediatefollowing.png](http://fangorn.googlecode.com/svn/wiki/images/immediatefollowing.png) |
| Preceding | <-- | ![http://fangorn.googlecode.com/svn/wiki/images/preceding.png](http://fangorn.googlecode.com/svn/wiki/images/preceding.png) |
| Immediately Preceding | <- | ![http://fangorn.googlecode.com/svn/wiki/images/immediatepreceding.png](http://fangorn.googlecode.com/svn/wiki/images/immediatepreceding.png) |

The following logical operator symbols are supported at nodes with branches:

| **Logical Operator** | **Symbols** |
|:---------------------|:------------|
| Conjunction | AND, and, & |
| Disjunction | OR, or, | |
| Negation | NOT, not, ! |

#### Matching node label text ####

At present all tree node labels, irrespective of whether they are phrase labels, part-of-speech labels, or words, are treated the same. Methods of identifying the type of a node will be made available in a future release. In all discussions here a query term could represent any type of node label. Additionally, query terms are **matched verbatim** with tree node labels. That is, a node label of NP-SBJ-1 will not match a query term NP-SBJ or NP. NP-SBJ-1 is a noun phrase subject with an additional suffix, -1, that marks a feature relating it to another node in the same tree. Therefore, a search for NP-SBJ should ideally match a node label NP-SBJ-N, where N is any number. At the same time, searching for a noun phrase should match all noun phrases, even those that play the role of a subject in a sentence. Hence, a search for NP should also match NP-SBJ node labels. However, incorporating such features requires an understanding of the Corpus' annotation methodology. In a future version of Fangorn we plan to allow the addition of the annotation methodology as an input.

## A 2 minute guide to simple path queries ##

This section is a quick introduction to path queries that do not have branches (we have taken the liberty of calling them simple path queries here). A path is a sequence of operator and node label pairs. If the first node label in the path should match the root of the tree the first operator should be a Child operator (/). Otherwise, the first operator should be a Descendant operator (//). The other operators in the path can be one of the operators specified in the operator table in the Syntax section.

A query to find trees where the root node is a verb-phrase VP is written as:
```
/VP
```
Notice the child operator at the start of the query.

A query to find trees where a noun phrase subject NP-SBJ appears anywhere in the tree would be:
```
//NP-SBJ
```
Words are the leaf nodes in a tree, therefore a similar query is used to find sentences containing a word.

The above two examples that search for a single node label are trivial and are seldom used in practice. A more common scenario is a path that combines several operator and node-label pairs. Consider searching for a tree where a verb-phrase immediately dominates (parent of) a noun-phrase and the noun phrase has two prepositional phrases as its immediately following siblings. The query would be written as:
```
//VP/NP=>PP=>PP
```

Observe that each node label in the query has exactly one structural relationship with its neighbouring node labels. These paths cannot have logical operators. To include logical operators, a filter expression needs to be inserted even if there is only one outgoing node relationship. We will explain this along with branching path expressions.

## A 5 minute guide to queries with filter expressions ##

Filter expressions are required when a path branches out into two or more paths at a node, or when a query specifies that certain paths should not occur (Negation operation).

When a path branches at a node, the node has more than two neighbours. Consider a simple branched query where we find a gerund VBG that is immediately preceded by the word `is', and is immediately following by the sibling sentence fragment S, and is immediately followed by a personal pronoun PRP. Here VBG shares a structural relationship with three terms in the query. The query can be written as:
```
//VBG[<-is AND =>S AND ->PRP]
```

The square brackets that appear after the VBG term are called filter expressions. Two or more expressions can be combined using logical AND or OR operators within the filter expression. In the above query, each expression in the filter consists of a single term, but in general they could be complete expression with multiple terms and even other nested filter expressions. For example, the above query could be refined to find only those sentence fragments that contain a TO part-of-speech label immediately following the the personal pronoun. The query would then be written as:
```
//VBG[<-is AND =>S AND ->PRP->TO]
```

Now since all the logical operators separating the nodes were AND operators, the query could be re-written in other forms. For example, any one of the expressions in the filter could have been removed outside the filter expression as a continuation of the main path. A few ways in which the query could have been rewritten are shown below:
```
//VBG[<-is AND =>S]->PRP
//VBG[<-is AND ->PRP]=>S
//VBG[=>S AND ->PRP]<-is
```

In the previous example the order of the expressions within the filter did not matter because all the operators were AND operators. However, when the filter contains OR and AND operators, the AND has higher precedence than OR operators. If the above query was
```
//VBG[<-is OR =>S AND ->PRP]
```
then it would have matched trees containing either of the following patterns:
```
//VBG<-is
```
or
```
//VBG[=>S AND ->PRP]
```

The logical NOT operator has higher precedence over both AND and OR operator in the ordered execution of queries. The NOT operator optionally appears before an expression within a filter. If the first query discussed in this section should not have a sentence fragment as the immediately following sibling of the gerund then the query can be rewritten as:
```
//VBG[<-is AND NOT =>S AND ->PRP]
```
The same follows even where there is only one NOT term in the filter expression. The query to search for all occurrences of gerunds not preceded by the word `is' is written as:
```
//VBG[NOT <-is]
```

As mentioned earlier, filter expressions can also be nested. A query to search for the gerund with the first set of conditions and an additional requirement that the sentence fragment should not dominate a prepositional phrase PP is written as:
```
//VBG[<-is AND =>S[NOT //PP] AND ->PRP]
```