/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package com.bigdata.rdf.sail.sparql;

import org.openrdf.query.MalformedQueryException;

import com.bigdata.rdf.sail.sparql.ast.ASTOperationContainer;
import com.bigdata.rdf.sail.sparql.ast.ASTString;
import com.bigdata.rdf.sail.sparql.ast.VisitorException;

/**
 * Processes escape sequences in strings, replacing the escape sequence with
 * their actual value. Escape sequences for SPARQL are documented in section <a
 * href="http://www.w3.org/TR/rdf-sparql-query/#grammarEscapes">A.7 Escape
 * sequences in strings</a>.
 * 
 * @author Arjohn Kampman
 * @openrdf
 */
public class StringEscapesProcessor {

	/**
	 * Processes escape sequences in ASTString objects.
	 * 
	 * @param qc
	 *        The query that needs to be processed.
	 * @throws MalformedQueryException
	 *         If an invalid escape sequence was found.
	 */
	public static void process(ASTOperationContainer qc)
		throws MalformedQueryException
	{
		StringProcessor visitor = new StringProcessor();
		try {
			qc.jjtAccept(visitor, null);
		}
		catch (VisitorException e) {
			throw new MalformedQueryException(e);
		}
	}

	private static class StringProcessor extends ASTVisitorBase {

		public StringProcessor() {
		}

		@Override
		public Object visit(ASTString stringNode, Object data)
			throws VisitorException
		{
			String value = stringNode.getValue();
			try {
				value = SPARQLUtil.decodeString(value);
				stringNode.setValue(value);
			}
			catch (IllegalArgumentException e) {
				// Invalid escape sequence
				throw new VisitorException(e.getMessage());
			}

			return super.visit(stringNode, data);
		}
	}
}
