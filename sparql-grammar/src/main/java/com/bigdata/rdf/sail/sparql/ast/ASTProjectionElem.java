/* Generated By:JJTree: Do not edit this line. ASTProjectionElem.java Version 4.1 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.bigdata.rdf.sail.sparql.ast;

import com.bigdata.rdf.sail.sparql.ast.ASTString;
import com.bigdata.rdf.sail.sparql.ast.ASTVar;
import com.bigdata.rdf.sail.sparql.ast.Node;
import com.bigdata.rdf.sail.sparql.ast.SimpleNode;
import com.bigdata.rdf.sail.sparql.ast.SyntaxTreeBuilder;
import com.bigdata.rdf.sail.sparql.ast.SyntaxTreeBuilderVisitor;
import com.bigdata.rdf.sail.sparql.ast.VisitorException;

public class ASTProjectionElem extends SimpleNode {

	public ASTProjectionElem(int id) {
		super(id);
	}

	public ASTProjectionElem(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	/** Accept the visitor. **/
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	public boolean hasAlias() {
		return getAlias() != null;
	}

	public String getAlias() {
		if (children.size() >= 2) {
		    // @see http://www.openrdf.org/issues/browse/SES-818
			Node aliasNode = children.get(children.size()-1);

			if (aliasNode instanceof ASTString) {
				return ((ASTString)aliasNode).getValue();
			}
			else if (aliasNode instanceof ASTVar) {
				return ((ASTVar)aliasNode).getName();
			}
		}

		return null;
	}
}
/* JavaCC - OriginalChecksum=ed67c3c7a74ebd8df6304268b81f702d (do not edit this line) */
