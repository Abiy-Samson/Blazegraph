/* Generated By:JJTree: Do not edit this line. ASTNumericLiteral.java */

package com.bigdata.rdf.sail.sparql.ast;

import org.openrdf.model.URI;
import com.bigdata.rdf.sail.sparql.ast.ASTRDFValue;
import com.bigdata.rdf.sail.sparql.ast.SyntaxTreeBuilder;
import com.bigdata.rdf.sail.sparql.ast.SyntaxTreeBuilderVisitor;
import com.bigdata.rdf.sail.sparql.ast.VisitorException;

public class ASTNumericLiteral extends ASTRDFValue {

	private String value;

	private URI datatype;

	public ASTNumericLiteral(int id) {
		super(id);
	}

	public ASTNumericLiteral(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public URI getDatatype() {
		return datatype;
	}

	public void setDatatype(URI datatype) {
		this.datatype = datatype;
	}

	@Override
	public String toString()
	{
		return super.toString() + " (value=" + value + ", datatype=" + datatype + ")";
	}
}
