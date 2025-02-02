/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

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
/*
 * Created on Oct 20, 2011
 */

package com.bigdata.rdf.sparql.ast;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.bigdata.bop.Constant;
import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.internal.VTE;
import com.bigdata.rdf.internal.impl.TermId;

/**
 * This test suite is built around around BSBM Q5. Each test has an existing
 * join path and a new vertex to be added to the join path. The question is
 * whether or not the vertex <em>can join</em> with the join path using one or
 * more shared variable(s). This tests a method used to incrementally grow a
 * join path when it is dynamically decided that an {@link IJoinNode} may be
 * added to the join path based on shared variables. Static analysis easily
 * reports those joins which are allowed based on the variables directly given
 * with two {@link IJoinNode}s. The purpose of this test suite is to explore
 * when joins (based on shared variables) become permissible through
 * {@link FilterNode}s as the variable(s) used within those constraints become
 * bound.
 * <p>
 * This set of unit tests explores various join paths and verifies that the
 * canJoin() and canJoinUsingConstraints() methods correctly recognize edges by
 * which a join path can be extended corresponding to both static and dynamic
 * analysis of the query.
 * 
 * @see StaticAnalysis#canJoin(IJoinNode, IJoinNode)
 * @see {@link StaticAnalysis#canJoinUsingConstraints(IJoinNode[], IJoinNode, FilterNode[])}
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: TestBOpUtility_canJoinUsingConstraints.java 4211 2011-02-20
 *          21:20:44Z thompsonbry $
 * 
 * @todo These are the full plans generated by the runtime and static
 *       optimizers. One way to test canJoinXXX() is to run out these join plans
 *       and verify that they report "true" in each case. However, the critical
 *       bit to test are join plans where the predicates w/o the shared
 *       variables can be run earlier due to the FILTERs.
 * 
 *       <pre>
 * test_bsbm_q5 : static [0] : : ids=[1, 2, 4, 6, 0, 3, 5]
 * test_bsbm_q5 : runtime[0] : : ids=[1, 2, 0, 4, 6, 3, 5]
 * </pre>
 */
//@SuppressWarnings("unchecked")
public class TestStaticAnalysis_CanJoinUsingConstraints extends
        AbstractASTEvaluationTestCase {

    private static final Logger log = Logger
            .getLogger(TestStaticAnalysis_CanJoinUsingConstraints.class);
    
    /**
     * 
     */
    public TestStaticAnalysis_CanJoinUsingConstraints() {
    }

    /**
     * @param name
     */
    public TestStaticAnalysis_CanJoinUsingConstraints(final String name) {
        super(name);
    }

    /**
     * Return a (Mock) IV.
     */
    @SuppressWarnings("rawtypes")
    private IV mockIV() {
        return TermId.mockIV(VTE.URI);
    }

    /**
     * Unit tests to verify that arguments are validated.
     * 
     * @see StaticAnalysis#canJoinUsingConstraints(IJoinNode[], IJoinNode, FilterNode[])
     */
    public void test_canJoinUsingConstraints_illegalArgument() {

        @SuppressWarnings("rawtypes")
        final ConstantNode p = new ConstantNode(new Constant<IV>(mockIV()));
        @SuppressWarnings("rawtypes")
        final ConstantNode q = new ConstantNode(new Constant<IV>(mockIV()));
        @SuppressWarnings("rawtypes")
        final ConstantNode r = new ConstantNode(new Constant<IV>(mockIV()));

        final VarNode x = new VarNode("x");
        final VarNode y = new VarNode("y");
        
        // Note: no shared variables.
        final StatementPatternNode p1 = new StatementPatternNode(x, q, r);
        final StatementPatternNode p2 = new StatementPatternNode(y, p, q);

        final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT));
        
        // path must not be null.
        try {
            sa.canJoinUsingConstraints(//
                    null, // path
                    p1,// vertex
                    new FilterNode[0]// constraints
                    );
            fail("Expecting: " + IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            if (log.isInfoEnabled())
                log.info("Expecting: " + IllegalArgumentException.class);
        }

        // vertex must not be null.
        try {
            sa.canJoinUsingConstraints(//
                    new IJoinNode[]{p1}, // path
                    null,// vertex
                    new FilterNode[0]// constraints
                    );
            fail("Expecting: " + IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            if (log.isInfoEnabled())
                log.info("Expecting: " + IllegalArgumentException.class);
        }

        // path may not be empty.
        try {
            sa.canJoinUsingConstraints(//
                    new IJoinNode[] {}, // path
                    p1,// vertex
                    new FilterNode[0]// constraints
                    );
            fail("Expecting: " + IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            if (log.isInfoEnabled())
                log.info("Expecting: " + IllegalArgumentException.class);
        }

        // path elements may not be null.
        try {
            sa.canJoinUsingConstraints(//
                    new IJoinNode[] { p2, null }, // path
                    p1,// vertex
                    new FilterNode[0]// constraints
                    );
            fail("Expecting: " + IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            if (log.isInfoEnabled())
                log.info("Expecting: " + IllegalArgumentException.class);
        }

        // vertex must not appear in the path.
        try {
            sa.canJoinUsingConstraints(//
                    new IJoinNode[] { p2, p1 }, // path
                    p1,// vertex
                    new FilterNode[0]// constraints
                    );
            fail("Expecting: " + IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            if (log.isInfoEnabled())
                log.info("Expecting: " + IllegalArgumentException.class);
        }

        // constraint array may not contain null elements.
        try {
            sa.canJoinUsingConstraints(//
                    new IJoinNode[] { p2 }, // path
                    p1,// vertex
                    new FilterNode[] { //
                            new FilterNode(new VarNode("x")),
                            null //
                    }// constraints
                    );
            fail("Expecting: " + IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            if (log.isInfoEnabled())
                log.info("Expecting: " + IllegalArgumentException.class);
        }

    }

    /**
     * Unit test for one-step joins based on the {@link #product} variable.
     */
    public void test_canJoinUsingConstraints_1step_productVar() {

        final BSBMQ5Setup s = new BSBMQ5Setup(store);

        final StaticAnalysis sa = new StaticAnalysis(s.queryRoot);

        // share ?product
        final IJoinNode[] a = new IJoinNode[] { s.p0, s.p2, s.p4, s.p6 };
        for (int i = 0; i < a.length; i++) {
            for (int j = i; j < a.length; j++) {
                final IJoinNode t0 = a[i];
                final IJoinNode t1 = a[j];
                assertTrue(sa.canJoin(t0, t1));
                assertTrue(sa.canJoin(t1, t0));
                if (t0 != t1) {
                    /*
                     * Test join path extension, but not when the vertex used to
                     * extend the path is already present in the join path.
                     */
                    assertTrue(sa.canJoinUsingConstraints(//
                            new IJoinNode[] { t0 }, // path
                            t1,// vertex
                            new FilterNode[0]// constraints
                            ));
                    assertTrue(sa.canJoinUsingConstraints(//
                            new IJoinNode[] { t1 }, // path
                            t0,// vertex
                            new FilterNode[0]// constraints
                            ));
                }
            }
        }

    }

    /**
     * Unit test for multi-step join paths based on the {@link #product}
     * variable.
     */
    public void test_canJoinUsingConstraints_multiStep_productVar() {

        final BSBMQ5Setup s = new BSBMQ5Setup(store);

        final StaticAnalysis sa = new StaticAnalysis(s.queryRoot);

        final Random r = new Random();
        
        // share ?product
        final IJoinNode[] a = new IJoinNode[] { s.p0, s.p2, s.p4, s.p6 };
        
        // existing path length [1:3].
        final int existingPathLength = r.nextInt(3) + 1;
        
        // generated pre-existing path.
        final IJoinNode[] path = new IJoinNode[existingPathLength];
        // vertex which will extend that path
        final IJoinNode vertex;
        {
            // collection of predicates used so far by the path.
            final Set<Integer> used = new LinkedHashSet<Integer>();
            for (int i = 0; i < path.length; i++) {
                // Locate an unused predicate.
                int index;
                while (true) {
                    index = r.nextInt(a.length);
                    if (!used.contains(index)) {
                        used.add(index);
                        break;
                    }
                }
                // add to the path.
                path[i] = a[index];
            }
            // Locate an unused predicate to serve as the extension vertex.
            {
                // Locate an unused predicate.
                int index;
                while (true) {
                    index = r.nextInt(a.length);
                    if (!used.contains(index)) {
                        used.add(index);
                        break;
                    }
                }
                vertex = a[index];
            }
        }

        // Verify all joins in the path are legal.
        for (int i = 0; i < path.length - 1; i++) {
            assertTrue(sa.canJoin(path[i], path[i + 1]));
        }
        
        // Verify the extension of the path is legal.
        assertTrue(sa.canJoinUsingConstraints(//
                path,//
                vertex,//
                new FilterNode[0]// constraints
                ));

    }

    /**
     * Unit test examines the predicates without shared variables and verifies
     * (a) that joins are not permitted when the constraints are not considered;
     * and (b) that joins are permitted when the constraints are considered.
     * <p>
     * This test is identical to {@link #test_canJoinUsingConstraints_p5_p6()()}
     * except that it considers the ({@link #p3} x {@link #p4}) join via the
     * {@link #c1} constraint instead.
     */
    public void test_canJoinUsingConstraints_p3_p4() {

        final BSBMQ5Setup s = new BSBMQ5Setup(store);

        final StaticAnalysis sa = new StaticAnalysis(s.queryRoot);

        /*
         * Verify (p3,p4) join is not permitted when we do not consider the
         * constraints (i.e., the join would be an unconstrained cross product
         * if it were executed).
         */
        assertFalse(sa.canJoin(s.p3, s.p4));
        assertFalse(sa.canJoin(s.p4, s.p3));
        assertFalse(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p3 }, // path
                s.p4,// vertex
                new FilterNode[0]// constraints
                ));
        assertFalse(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p4 }, // path
                s.p3,// vertex
                new FilterNode[0]// constraints
                ));

        /*
         * Verify (p3,p4) join is not permitted if we do not consider the
         * constraint which provides the shared variables.
         */
        assertFalse(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p3 }, // path
                s.p4,// vertex
                new FilterNode[] { s.c2 }// constraints
                ));
        assertFalse(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p4 }, // path
                s.p3,// vertex
                new FilterNode[] { s.c2 }// constraints
                ));

        /*
         * Verify (p3,p4) join is permitted if we consider the constraint which
         * provides the shared variables.
         */
        assertTrue(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p3 }, // path
                s.p4,// vertex
                new FilterNode[] { s.c1 }// constraints
                ));
        assertTrue(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p4 }, // path
                s.p3,// vertex
                new FilterNode[] { s.c1 }// constraints
                ));

    }

    /**
     * Unit test examines the predicates without shared variables and verifies
     * (a) that joins are not permitted when the constraints are not considered;
     * and (b) that joins are permitted when the constraints are considered.
     * <p>
     * This test is identical to {@link #test_canJoinUsingConstraints_p3_p4()}
     * except that it considers the ({@link #p5} x {@link #p6}) join via the
     * {@link #c2} constraint instead.
     */
    public void test_canJoinUsingConstraints_p5_p6() {

        final BSBMQ5Setup s = new BSBMQ5Setup(store);

        final StaticAnalysis sa = new StaticAnalysis(s.queryRoot);

        /*
         * Verify (p5,p6) join is not permitted when we do not consider the
         * constraints (i.e., the join would be an unconstrained cross product
         * if it were executed).
         */
        assertFalse(sa.canJoin(s.p5, s.p6));
        assertFalse(sa.canJoin(s.p6, s.p5));
        assertFalse(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p5 }, // path
                s.p6,// vertex
                new FilterNode[0]// constraints
                ));
        assertFalse(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p6 }, // path
                s.p5,// vertex
                new FilterNode[0]// constraints
                ));

        /*
         * Verify (p5,p6) join is not permitted if we do not consider the
         * constraint which provides the shared variables.
         */
        assertFalse(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p5 }, // path
                s.p6,// vertex
                new FilterNode[] { s.c1 }// constraints
                ));
        assertFalse(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p6 }, // path
                s.p5,// vertex
                new FilterNode[] { s.c1 }// constraints
                ));

        /*
         * Verify (p5,p6) join is permitted if we consider the constraint which
         * provides the shared variables.
         */
        assertTrue(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p5 }, // path
                s.p6,// vertex
                new FilterNode[] { s.c2 }// constraints
                ));
        assertTrue(sa.canJoinUsingConstraints(//
                new IJoinNode[] { s.p6 }, // path
                s.p5,// vertex
                new FilterNode[] { s.c2 }// constraints
                ));

    }

    /*
     * Unit tests for attaching constraints using a specific join path.
     */

    /** <code>path = [1, 2, 4, 6, 0, 3, 5]</code> */
    public void test_attachConstraints_BSBM_Q5_path01() {

        final BSBMQ5Setup s = new BSBMQ5Setup(store);

        final StaticAnalysis sa = new StaticAnalysis(s.queryRoot);

        final IJoinNode[] path = { s.p1, s.p2, s.p4, s.p6, s.p0, s.p3, s.p5 };

        final FilterNode[][] actual = sa
                .getJoinGraphConstraints(path, s.constraints,
                        null/* knownBoundVars */, true/* pathIsComplete */);

        @SuppressWarnings("unchecked")
        final Set<FilterNode>[] expected = new Set[] { //
                s.NA, // p1
                s.C0, // p2
                s.NA, // p4
                s.NA, // p6
                s.NA, // p0
                s.C1, // p3
                s.C2, // p5
        };

        assertSameConstraints(expected, actual);

    }

    /** <code>[5, 3, 1, 0, 2, 4, 6]</code>. */
    public void test_attachConstraints_BSBM_Q5_path02() {

        final BSBMQ5Setup s = new BSBMQ5Setup(store);

        final StaticAnalysis sa = new StaticAnalysis(s.queryRoot);

        final IJoinNode[] path = { s.p5, s.p3, s.p1, s.p0, s.p2, s.p4, s.p6 };

        final FilterNode[][] actual = sa
                .getJoinGraphConstraints(path, s.constraints,
                        null/* knownBoundVars */, true/* pathIsComplete */);

        @SuppressWarnings("unchecked")
        final Set<FilterNode>[] expected = new Set[] { //
                s.NA, // p5
                s.NA, // p3
                s.NA, // p1
                s.C0, // p0
                s.NA, // p2
                s.C1, // p4
                s.C2, // p6
        };

        assertSameConstraints(expected, actual);

    }

    /** <code>[3, 4, 5, 6, 1, 2, 0]</code> (key-range constraint variant). */
    public void test_attachConstraints_BSBM_Q5_path03() {

        final BSBMQ5Setup s = new BSBMQ5Setup(store);

        final StaticAnalysis sa = new StaticAnalysis(s.queryRoot);

        final IJoinNode[] path = { s.p3, s.p4, s.p5, s.p6, s.p1, s.p2, s.p0 };

        final FilterNode[][] actual = sa
                .getJoinGraphConstraints(path, s.constraints,
                        null/* knownBoundVars */, true/* pathIsComplete */);

        @SuppressWarnings("unchecked")
        final Set<FilterNode>[] expected = new Set[] { //
                s.NA, // p3
                asSet(new FilterNode[]{s.c0,s.c1}), // p4
                s.NA, // p5
                s.C2, // p6
                s.NA, // p1
                s.NA, // p2
                s.NA, // p0
        };

        assertSameConstraints(expected, actual);

    }

    /**
     * <code>[5  6  0  2  1  4 3]</code>.
     * 
     * FIXME The above join path produces a false ZERO result for the query and
     * all of the join path segments below produce a false exact ZERO (0E)
     * cardinality estimate. Figure out why. The final path chosen could have
     * been any of the one step extensions of a path with a false 0E cardinality
     * estimate.
     * 
     * <pre>
     * INFO : 3529      main com.bigdata.bop.joinGraph.rto.JGraph.expand(JGraph.java:1116): 
     * ** round=4: paths{in=14,considered=26,out=6}
     * path    srcCard  *          f (      in  sumRgCt tplsRead      out    limit  adjCard) =    estRead    estCard  : sumEstRead sumEstCard sumEstCost  joinPath
     *    0          0E *       0.00 (       0        0        0        0      200        0) =          0          0E :          1          0          0  [ 5  6  0  2  1  4 ]
     *    1          0E *       0.00 (       0        0        0        0      200        0) =          0          0E :          1          0          0  [ 5  6  0  2  4  3 ]
     *    2          0E *       0.00 (       0        0        0        0      200        0) =          0          0E :          1          0          0  [ 5  6  0  4  1  3 ]
     *    3          0E *       0.00 (       0        0        0        0      200        0) =          0          0E :          1          0          0  [ 5  6  2  1  4  3 ]
     *    4        208  *       1.00 (      26       26       26       26      400       26) =         26        208  :      16576       1447       1447  [ 5  3  1  2  4  0 ]
     *    5          0E *       0.00 (       0        0        0        0      200        0) =          0          0E :          2          1          1  [ 5  3  6  0  1  2 ]
     * </pre>
     */
    public void test_attachConstraints_BSBM_Q5_path04() {

        final BSBMQ5Setup s = new BSBMQ5Setup(store);

        final StaticAnalysis sa = new StaticAnalysis(s.queryRoot);

        final IJoinNode[] path = { s.p5, s.p6, s.p0, s.p2, s.p1, s.p4, s.p3 };

        final FilterNode[][] actual = sa
                .getJoinGraphConstraints(path, s.constraints,
                        null/* knownBoundVars */, true/* pathIsComplete */);

        @SuppressWarnings("unchecked")
        final Set<FilterNode>[] expected = new Set[] { //
                s.NA, // p5
                asSet(new FilterNode[] { s.c0, s.c2 }), // p6
                s.NA, // p0
                s.NA, // p2
                s.NA, // p1
                s.NA, // p4
                s.C1, // p3
        };

        assertSameConstraints(expected, actual);

    }

    /**
     * Verifies that the right set of constraints is attached at each of the
     * vertices of a join path. Comparison of {@link FilterNode} instances is by
     * reference.
     * 
     * @param expected
     * @param actual
     */
    static void assertSameConstraints(final Set<FilterNode>[] expected,
            final FilterNode[][] actual) {

        assertEquals("length", expected.length, actual.length);

        for (int i = 0; i < expected.length; i++) {

            final Set<FilterNode> e = expected[i];
            final FilterNode[] a = actual[i];

            if (e.size() != a.length) {
                fail("Differs at expected[" + i + "] : expecting " + e.size()
                        + ", not " + a.length + " elements: "
                        + Arrays.toString(a));
            }

            for (int j = 0; j < a.length; j++) {

                boolean foundRef = false;
                for (FilterNode t : e) {

                    if (t == a[j]) {
                        foundRef = true;
                        break;
                    }

                }

                if (!foundRef) {

                    fail("Differs at expected[" + i + "][" + j + "] : actual="
                            + a[j]);

                }

            }

        }

    }

}
