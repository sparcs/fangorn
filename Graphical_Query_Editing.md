## Editing queries graphically ##

Tree queries can be edited graphically by clicking the "Build query from tree" button on the top right of each matched result in the result display screen. A new window then appears over the result window and allows the query annotation to be edited.
This screen does not allow nodes in the tree to be collapsed on a mouse click, unlike in the result display screen. However, collapsed nodes can be expanded by clicking on the triangle below such nodes.

The following operations can be performed in the edit query screen:
  * extend a query starting at a node
  * edit a query term label
  * delete a term from the query
  * edit an operator joining two terms
  * negate an operator

### Extending the query ###
A query is extended by adding a new term to the query expression. In order to do this, first select a highlighted node. A highlighted node is one whose text is green in colour. Once selected, the highlighted node has a box around it.

Then, choose another node in the tree that is not highlighted to create a structural relationship between the highlighted node and the new node. At the end of this operation, the new node is also highlighted and selected. The query in the top bar is updated to reflect the change.

The operator connecting the two nodes is selected based on the structural relationship of the newly added node to the previously selected node. In some cases, more than one structural operator may apply to relate the two nodes. In that case, change the operator by following the instructions given below for editing an operator.

At present new branches at a node are always added using a conjunction operator. To change this, the query would have to be manually edited after pressing "Done" button on the build query screen. Please note the operator precedence of AND and OR operators within the filter expression. For more details read the 5 minute guide to queries with filter expressions section in the Query\_Language wiki. Also note that while adding and deleting nodes from the query, the query expression may be rewritten into different but equivalent forms in the top query bar.

### Edit a query term label ###
Select any highlighted node. The bottom of the screen shows the node label of the selected node in an input box. Change the text in the input box and press enter or click on the "Edit label" button to change the text in the label.

### Delete a query term ###
The button to delete a node is found in the bottom bar when a highlighted node is selected. If it is disabled, then it means that the selected node cannot be deleted without breaking the tree structure of the query. Only the root node (if it does not have branches) or the leaf nodes in the query tree can be deleted because they are connected to only one other node in the tree. On deletion, the structural relationship of the deleted node with the other node in the query is removed completely.

### Edit an operator ###
Highlighted operators (the lines in blue and red) can be edited by first selecting them with a mouse click. A box appears around the line. The bottom bar displays the operator name and a "Negate" check box. If the nodes on either side of the operator do not satisfy any other structural property, then the status bar indicates that the current operator is the only possible one. If that is not indicated in the message, then the operator can be changed. To change, click on the selected line until the required operator is reached.

The default operator between two nodes is always the most restrictive one. For example, if the two nodes are in a parent/child relationship the operator will be the parent/child operator. Clicking on the line once will change it to Ancestor/Descendant. Similarly, if two nodes are in the immediately following sibling relationship, the following operators appear in sequence after each click:
Immediately Following Sibling >  Following Sibling > Immediately Following > Following. Note that if the nodes are not immediate siblings, immediate operators will not appear as options. After each click the operator is less restrictive. A click on the least restrictive operator goes back to the most restrictive operator and the same sequence repeats.

### Negate an operator ###
To negate a highlighted operator select the operator first. The bottom bar has a checkbox that shows if an operator is negated. This checkbox can be checked or unchecked to toggle the negation on the operator in the query tree.

### Finishing Editing ###
Once the editing is complete, press the Done button to copy the new query to the main screen or click on the close button at top right hand corner to cancel and close the screen.