/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2011.
 *
 * Licensed under the Aduna BSD-style license.
 */
package com.bigdata.rdf.sail.sparql.ast;

import com.bigdata.rdf.sail.sparql.ast.SimpleNode;
import com.bigdata.rdf.sail.sparql.ast.SyntaxTreeBuilder;


/**
 *
 * @author Jeen
 */
public abstract class ASTAggregate extends SimpleNode {

	private boolean distinct;
	
	/**
	 * @param id
	 */
	public ASTAggregate(int id) {
		super(id);
	}
	
	public ASTAggregate(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}
	
	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

}
