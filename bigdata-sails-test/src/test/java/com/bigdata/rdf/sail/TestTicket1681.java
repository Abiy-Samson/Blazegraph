/**
Copyright (C) SYSTAP, LLC DBA Blazegraph 2011.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.bigdata.rdf.sail;

import java.io.IOException;
import java.util.Set;

import junit.framework.Assert;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

/**
 * Unit test template for use in submission of bugs.
 * <p>
 * This test case will delegate to an underlying backing store. You can specify
 * this store via a JVM property as follows:
 * <code>-DtestClass=com.bigdata.rdf.sail.TestBigdataSailWithQuads</code>
 * <p>
 * There are three possible configurations for the testClass:
 * <ul>
 * <li>com.bigdata.rdf.sail.TestBigdataSailWithQuads (quads mode)</li>
 * <li>com.bigdata.rdf.sail.TestBigdataSailWithoutSids (triples mode)</li>
 * <li>com.bigdata.rdf.sail.TestBigdataSailWithSids (SIDs mode)</li>
 * </ul>
 * <p>
 * The default for triples and SIDs mode is for inference with truth maintenance
 * to be on. If you would like to turn off inference, make sure to do so in
 * {@link #getProperties()}.
 * 
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 * 
 * @see https://jira.blazegraph.com/browse/BLZG-1681
 */
public class TestTicket1681 extends QuadsTestCase {
	
    public TestTicket1681() {
	}

	public TestTicket1681(String arg0) {
		super(arg0);
	}

	public void testBug() throws Exception {

	    // try with Sesame MemoryStore:
		executeQuery(new SailRepository(new MemoryStore()));

		final BigdataSail sail = getSail();
		try {
			executeQuery(new BigdataSailRepository(sail));
		} finally {
			sail.__tearDownUnitTest();
		}
	}

	private void executeQuery(final SailRepository repo)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException, RDFParseException, IOException {
		try {
			repo.initialize();
			final RepositoryConnection conn = repo.getConnection();
			conn.setAutoCommit(false);
			try {
				conn.add(getClass().getResourceAsStream("TestTicket1681.nt"), "",
						RDFFormat.TURTLE);
				conn.commit();

				final String query = "SELECT * WHERE { ?s <http://p>  ?o . FILTER (?o IN (<http://o2>, <http://o3>) ) }";
				final TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL,
						query);
				final TupleQueryResult tqr = q.evaluate();
				int cnt = 0;
				while (tqr.hasNext()) {
				    final Set<String> bindingNames = tqr.next().getBindingNames();
				    cnt++;
				    if(log.isInfoEnabled())
				        log.info("bindingNames="+bindingNames);
				}
				tqr.close();
				assertEquals(1, cnt);
			} finally {
				conn.close();
			}
		} finally {
			repo.shutDown();
		}
	}
}
