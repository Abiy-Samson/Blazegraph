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
package com.bigdata.bop.joinGraph.rto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.bigdata.bop.BOp;
import com.bigdata.bop.BOpUtility;
import com.bigdata.bop.IConstraint;
import com.bigdata.bop.IPredicate;
import com.bigdata.bop.engine.QueryEngine;
import com.bigdata.rdf.sparql.ast.eval.AST2BOpRTO;

/**
 * A join path is an ordered sequence of N {@link Vertex vertices} and
 * represents an ordered series of N-1 joins.
 * <p>
 * During exploration, the {@link Path} is used to develop an estimate of the
 * cost of different join paths which explore the {@link Vertex vertices} in a
 * {@link JGraph join graph}, possibly under some set of {@link IConstraint}s.
 * The estimated cost of the join path is developed from a sample of the initial
 * {@link Vertex} followed by the cutoff sample of each join in the join path.
 * Join paths may be re-sampled in successive rounds at a greater sample size in
 * order to improve the accuracy and robustness of the estimated cost for the
 * join path.
 * <p>
 * Each join path reflects a specific history. The cutoff sample for the initial
 * vertex can be shared across join paths since there is no prior history. This
 * is true even when we re-sample the vertex at the start of each round. The
 * cutoff sample for each join reflects the history of joins. It can only be
 * shared with join paths having the same history up to that vertex. For
 * example, the following join paths can share estimates of the vertices A, B,
 * and C but not D or E.
 * 
 * <pre>
 * p1: {A, B, C, E, D}
 * p2: {A, B, C, D, E}
 * </pre>
 * 
 * This is because their histories diverge after the (B,C) join.
 * <p>
 * In each successive round of exploration, each join path is replaced by one or
 * more one-step extensions of that path. The extensions are generated by
 * considering the {@link Vertex vertices} in the join graph which are not yet
 * in use within the join path. The join paths which spanning the same unordered
 * set of vertices in a given round of exploration compete based on their
 * estimated cost. The winner is the join path with the lowest estimated cost.
 * The losers are dropped from further consideration in order to prune the
 * search space. See {@link JGraph} which manages the expansion and competition
 * among join paths.
 * <p>
 * When considering {@link Vertex vertices} which can extend the join path, we
 * first select constrained joins. Only if there are no remaining constrained
 * joins will a join path be extended by an unconstrained join. A constrained
 * join is one which shares a variable with the existing join path. The variable
 * may either be shared directly via the {@link IPredicate}s or indirectly via
 * an {@link IConstraint} which can be evaluated for the {@link Vertex} under
 * consideration given the set of variables which are already known to be bound
 * for the join path. An unconstrained join is one where there are no shared
 * variables and always results in a full cross-product. Unconstrained joins are
 * not chosen unless there are no available constrained joins.
 */
public class Path {

//    private static final transient Logger log = Logger.getLogger(Path.class);

    /**
     * An ordered list of the vertices in the {@link Path}.
     */
    final Vertex[] vertices;

    /**
     * An ordered list of the {@link IPredicate}s in the {@link #vertices}. This
     * is computed by the constructor and cached as it is used repeatedly.
     */
    private final IPredicate<?>[] preds;

	/**
	 * The sample obtained by the step-wise cutoff evaluation of the ordered
	 * edges of the path.
	 * <p>
	 * Note: This sample is generated one edge at a time rather than by
	 * attempting the cutoff evaluation of the entire join path (the latter
	 * approach does allow us to limit the amount of work to be done to satisfy
	 * the cutoff).
	 * <p>
	 * Note: This is updated when we resample the path prior to expanding the
	 * path with another vertex.
	 */
    EdgeSample edgeSample;// TODO rename pathSample?

	/**
	 * Examine the path. If there is a cardinality underflow, then boost the
	 * sampling limit. Otherwise, increase the sample by the caller's value.
	 * 
	 * @param limitIn
	 *            The default increment for the sample limit.
	 * 
	 * @return The limit to use when resampling this path.
	 */
    public int getNewLimit(final int limitIn) {
    	
		if (edgeSample.estimateEnum == EstimateEnum.Underflow) {

			return edgeSample.limit * 2;
			
		}
		
		return edgeSample.limit + limitIn;
    	
    }
    
    /**
     * The cumulative estimated cardinality of the path. This is zero for an
     * empty path. For a path consisting of a single edge, this is the estimated
     * cardinality of that edge. When creating a new path by adding an edge to
     * an existing path, the cumulative cardinality of the new path is the
     * cumulative cardinality of the existing path plus the estimated
     * cardinality of the cutoff join of the new edge given the input sample of
     * the existing path.
     * 
     * @todo Track this per vertex as well as the total for more interesting
     *       traces in showPath(Path). In fact, that is just the VertexSample
     *       for the initial vertex and the EdgeSample for each subsequent
     *       vertex in path order. The EdgeSamples are maintained in a map
     *       managed by JGraph during optimization.
     */
    final public long sumEstCard;

	/**
	 * The cumulative estimated #of tuples that would be read for this path if
	 * it were to be fully executed (sum(tuplesRead*f) for each step in the
	 * path).
	 */
    final public long sumEstRead;

	/**
	 * The expected cost of this join path if it were to be fully executed. This
	 * is a function of {@link #sumEstCard} and {@link #sumEstRead}. The
	 * former reflects the #of intermediate solutions generated. The latter
	 * reflects the #of tuples read from the disk. These two measures are
	 * tracked separately and then combined into the {@link #sumEstCost}.
	 */
    final public long sumEstCost;

    /**
     * Combine the cumulative expected cardinality and the cumulative expected
     * tuples read to produce an overall measure of the expected cost of the
     * join path if it were to be fully executed.
     * 
     * @return The cumulative estimated cost of the join path.
     * 
     *         TODO Compute this incrementally as estCost using estRead and
     *         estCard and then take the running sum as sumEstCost and update
     *         the JGraph trace appropriately. [Refactor into an IPathCost
     *         interface. It should have visibility into the full path and also
     *         allow visibility into the vertex cost for generality.]
     * 
     *         TODO Add a cost function API, e.g., IPathCost. This gets passed
     *         into Path to compute a score. We also compute a score for a
     *         vertex. Add query hints for both so we can control the behavior.
     *         The default should be estCard, but estRead or a weighted
     *         combination of estCard and estRead are also possible cost
     *         functions.
     */
    private static long getCost(final long sumEstRead, final long sumEstCard) {

		final long total;
//		total = sumEstCard + sumEstRead; // intermediate results + IO.
//		total = sumEstRead; // just IO
		total = sumEstCard; // just intermediate results.

        return total;
        
    }

	@Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Path{[");
        boolean first = true;
        for (Vertex v : vertices) {
            if (!first)
                sb.append(",");
            sb.append(v.pred.getId());
            first = false;
        }
//        for (Edge e : edges) {
//            if (!first)
//                sb.append(",");
//            sb.append(e.getLabel());
//            first = false;
//        }
		sb.append("]");
		sb.append(",sumEstRead=" + sumEstRead);
		sb.append(",sumEstCard=" + sumEstCard);
		sb.append(",sumEstCost=" + sumEstCost);
		sb.append(",sample=" + edgeSample);
		sb.append("}");
        return sb.toString();
    }

//    /**
//     * Create an empty path.
//     */
//    public Path() {
////        this.edges = Collections.emptyList();
//        this.vertices = new Vertex[0];
//        this.preds = new IPredicate[0];
//        this.cumulativeEstimatedCardinality = 0;
//        this.sample = null;
//    }

    /**
     * Create a path from a single edge.
     * 
     * @param v0
     *            The initial vertex in the path.
     * @param v1
     *            The 2nd vertex in the path.
     * @param edgeSample
     *            The sample obtained from the cutoff join of (v0,v1).
     */
    public Path(final Vertex v0, final Vertex v1, final EdgeSample edgeSample) {

        if (v0 == null)
            throw new IllegalArgumentException();

        if (v1 == null)
            throw new IllegalArgumentException();

        if (v0.sample == null)
            throw new IllegalArgumentException();

        if (edgeSample == null)
            throw new IllegalArgumentException();

        if (edgeSample.getSample() == null)
            throw new IllegalArgumentException();

        this.vertices = new Vertex[]{v0,v1};

        this.preds = getPredicates(vertices);
        
        this.edgeSample = edgeSample;

		/*
		 * The expected #of tuples read for the full join of (v0,v1). This is
		 * everything which could be visited for [v0] plus the #of tuples read
		 * from [v1] during the cutoff join times the (adjusted) join hit ratio.
		 */
		this.sumEstRead = v0.sample.estCard + edgeSample.estRead;

		/*
		 * The estimated cardinality of the cutoff join of (v0,v1).
		 */
		this.sumEstCard = edgeSample.estCard;

		this.sumEstCost = getCost(this.sumEstRead, this.sumEstCard);
		
    }

	/**
	 * Private constructor used when we extend a path.
	 * 
	 * @param vertices
	 *            The ordered array of vertices in the new path. The last entry
	 *            in this array is the vertex which was used to extend the path.
	 * @param preds
	 *            The ordered array of predicates in the new path (correlated
	 *            with the vertices and passed in since it is already computed
	 *            by the caller).
	 * @param edgeSample
	 *            The sample from the cutoff join of the last vertex added to
	 *            this path.
	 * @param sumEstCard
	 *            The cumulative estimated cardinality of the new path.
	 * @param sumEstRead
	 *            The cumulative estimated tuples read of the new path.
	 */
    private Path(//
            final Vertex[] vertices,//
            final IPredicate<?>[] preds,//
            final EdgeSample edgeSample,//
            final long sumEstCard,//
            final long sumEstRead//
            ) {

        if (vertices == null)
            throw new IllegalArgumentException();

        if (preds == null)
            throw new IllegalArgumentException();

        if (vertices.length != preds.length)
            throw new IllegalArgumentException();

        if (sumEstCard < 0)
            throw new IllegalArgumentException();

        if (edgeSample == null)
            throw new IllegalArgumentException();

        if (edgeSample.getSample() == null)
            throw new IllegalArgumentException();

		this.vertices = vertices;

		this.preds = preds;

		this.edgeSample = edgeSample;

		this.sumEstCard = sumEstCard;

		this.sumEstRead = sumEstRead;

		this.sumEstCost = getCost(this.sumEstRead, this.sumEstCard);
        
    }

    /**
     * Return the #of vertices in this join path.
     */
    public int getVertexCount() {
        
        return vertices.length;
        
    }

    /**
     * Return <code>true</code> iff the {@link Path} contains that
     * {@link Vertex}.
     * 
     * @param v
     *            The vertex
     * 
     * @return true if the vertex is already part of the path.
     */
    public boolean contains(final Vertex v) {

        if (v == null)
            throw new IllegalArgumentException();

        for (Vertex x : vertices) {
         
            if (v == x)
                return true;
            
        }
//        for (Edge e : edges) {
//
//            if (e.v1 == v || e.v2 == v)
//                return true;
//
//        }

        return false;
    }

    /**
     * Return <code>true</code> if this path is an unordered variant of the
     * given path (same vertices in any order).
     * 
     * @param p
     *            Another path.
     * 
     * @return <code>true</code> if this path is an unordered variant of the
     *         given path.
     */
    public boolean isUnorderedVariant(final Path p) {

        if (p == null)
            throw new IllegalArgumentException();

        if (vertices.length != p.vertices.length) {
            /*
             * Fast rejection. This assumes that each edge after the first
             * adds one distinct vertex to the path. That assumption is
             * enforced by #addEdge().
             */
            return false;
        }

        final Vertex[] v1 = this.vertices;
        final Vertex[] v2 = p.vertices;

        if (v1.length != v2.length) {

            // Reject (this case is also covered by the test above).
            return false;
            
        }

        /*
         * Scan the vertices of the caller's path. If any of those vertices
         * are NOT found in this path the paths are not unordered variations
         * of one another.
         */
        for (int i = 0; i < v2.length; i++) {

            final Vertex tmp = v2[i];

            boolean found = false;
            for (int j = 0; j < v1.length; j++) {

                if (v1[j] == tmp) {
                    found = true;
                    break;
                }

            }

            if (!found) {
                return false;
            }

        }

        return true;

    }

    /**
     * Return the vertices in this path (in path order). For the first edge,
     * the minimum cardinality vertex is always reported first (this is
     * critical for producing the correct join plan). For the remaining
     * edges in the path, the unvisited is reported.
     * 
     * @return The vertices (in path order).
     */
    public List<Vertex> getVertices() {

        return Collections.unmodifiableList(Arrays.asList(vertices));

    }

    /**
     * Return the {@link IPredicate}s associated with the vertices of the
     * join path in path order.
     * 
     * @see #getVertices()
     */
    public IPredicate<?>[] getPredicates() {

        return preds;

    }

    /**
     * Return the {@link BOp} identifiers of the predicates associated with
     * each vertex in path order.
     */
    public int[] getVertexIds() {
        
//        return getVertexIds(edges);
        
        return BOpUtility.getPredIds(preds);
        
    }
    
    /**
     * Return the predicates associated with the vertices.
     * 
     * @param vertices
     *            The vertices in the selected evaluation order.
     * 
     * @return The predicates associated with those vertices in the same order.
     */
    static private IPredicate<?>[] getPredicates(final Vertex[] vertices) {

        // The predicates in the same order as the vertices.
        final IPredicate<?>[] preds = new IPredicate[vertices.length];

        for (int i = 0; i < vertices.length; i++) {

            preds[i] = vertices[i].pred;

        }

        return preds;

    }

    /**
     * Return <code>true</code> if this path begins with the given path.
     * 
     * @param p
     *            The given path.
     * 
     * @return <code>true</code> if this path begins with the given path.
     * 
     * @todo unit tests.
     */
    public boolean beginsWith(final Path p) {

        if (p == null)
            throw new IllegalArgumentException();

        if (vertices.length < p.vertices.length) {

            // Proven false since the caller's path is longer.
            return false;
            
        }

        for (int i = 0; i < p.vertices.length; i++) {

            final Vertex vSelf = vertices[i];
            
            final Vertex vOther = p.vertices[i];
            
            if (vSelf != vOther) {
            
                return false;
                
            }
            
        }

        return true;
    }

    /**
     * Return <code>true</code> if this path begins with the given path.
     * 
     * @param p
     *            The given path.
     * 
     * @return <code>true</code> if this path begins with the given path.
     * 
     * @todo unit tests.
     */
    public boolean beginsWith(final int[] ids) {

        if (ids == null)
            throw new IllegalArgumentException();

        if (vertices.length < ids.length) {
            // Proven false since the caller's path is longer.
            return false;
        }

        for (int i = 0; i < ids.length; i++) {

            final int idSelf = vertices[i].pred.getId();
            
            final int idOther = ids[i];
            
            if (idSelf != idOther) {
            
                return false;
                
            }
            
        }

        return true;
    }

    /**
     * Return the first N {@link IPredicate}s in this {@link Path}.
     * 
     * @param length
     *            The length of the path segment.
     * 
     * @return The path segment.
     */
    public IPredicate<?>[] getPathSegment(final int length) {

        if (length > preds.length)
            throw new IllegalArgumentException();

        final IPredicate<?>[] preds2 = new IPredicate[length];
        
        System.arraycopy(preds/* src */, 0/* srcPos */, preds2/* dest */,
                0/* destPos */, length);
        
        return preds2;

    }

    /**
     * Add an edge to a path, computing the estimated cardinality of the new
     * path, and returning the new path. The cutoff join is performed using the
     * {@link #edgeSample} of <i>this</i> join path and the actual access path
     * for the target vertex.
     * 
     * @param queryEngine
     * @param limit
     * @param vnew
     *            The new vertex.
     * @param constraints
     *            The join graph constraints (if any).
	 * @param pathIsComplete
	 *            <code>true</code> iff all vertices in the join graph are
	 *            incorporated into this path.
     * 
     * @return The new path. The materialized sample for the new path is the
     *         sample obtained by the cutoff join for the edge added to the
     *         path.
     * 
     * @throws Exception
     */
    public Path addEdge(final QueryEngine queryEngine,
            final JoinGraph joinGraph, final int limit, final Vertex vnew,
            final IConstraint[] constraints, final boolean pathIsComplete)
            throws Exception {

        if (vnew == null)
            throw new IllegalArgumentException();

        if(contains(vnew))
            throw new IllegalArgumentException(
                "Vertex already present in path: vnew=" + vnew + ", path="
                        + this);

        if (this.edgeSample == null)
            throw new IllegalStateException();

        // The new vertex.
        final Vertex targetVertex = vnew;

		/*
		 * Chain sample the edge.
		 * 
		 * Note: ROX uses the intermediate result I(p) for the existing path as
		 * the input when sampling the edge. The corresponding concept for us is
		 * the sample for this Path, which will have all variable bindings
		 * produced so far. In order to estimate the cardinality of the new join
		 * path we have to do a one step cutoff evaluation of the new Edge,
		 * given the sample available on the current Path.
		 * 
		 * Note: It is possible for the resulting edge sample to be empty (no
		 * solutions). Unless the sample also happens to be exact, this is an
		 * indication that the estimated cardinality has underflowed. We track
		 * the estimated cumulative cardinality, so this does not make the join
		 * path an immediate winner, but it does mean that we can not probe
		 * further on that join path as we lack any intermediate solutions to
		 * feed into the downstream joins. To resolve that, we have to increase
		 * the sample limit (unless the path is the winner, in which case we can
		 * fully execute the join path segment and materialize the results and
		 * use those to probe further).
		 */

        // Ordered array of all predicates including the target vertex.
        final IPredicate<?>[] preds2;
        final Vertex[] vertices2;
        {
            preds2 = new IPredicate[preds.length + 1];

            vertices2 = new Vertex[preds.length + 1];

            System.arraycopy(preds/* src */, 0/* srcPos */, preds2/* dest */,
                    0/* destPos */, preds.length);
            
            System.arraycopy(vertices/* src */, 0/* srcPos */, vertices2/* dest */,
                    0/* destPos */, preds.length);

            preds2[preds.length] = targetVertex.pred;
            
            vertices2[preds.length] = targetVertex;
            
        }

        final EdgeSample edgeSample2 = AST2BOpRTO.cutoffJoin(//
                queryEngine,//
                joinGraph,//
                limit, //
                preds2,//
                constraints,//
                pathIsComplete,//
                this.edgeSample // the source sample.
                );


		// Extend the path.
		final Path tmp = new Path(//
				vertices2,//
				preds2,//
				edgeSample2,//
				this.sumEstCard + edgeSample2.estCard,// sumEstCard
				this.sumEstRead + edgeSample2.estRead // sumEstRead
		);

		return tmp;

    }

    /**
     * Cutoff join of the last vertex in the join path.
     * <p>
     * <strong>The caller is responsible for protecting against needless
     * re-sampling.</strong> This includes cases where a sample already exists
     * at the desired sample limit and cases where the sample is already exact.
     * 
     * @param queryEngine
     *            The query engine.
     * @param joinGraph
     *            The pipeline operator that is executing the RTO. This defines
     *            the join graph (vertices, edges, and constraints) and also
     *            provides access to the AST and related metadata required to
     *            execute the join graph.
     * @param limit
     *            The limit for the cutoff join.
     * @param path
     *            The path segment, which must include the target vertex as the
     *            last component of the path segment.
     * @param constraints
     *            The constraints declared for the join graph (if any). The
     *            appropriate constraints will be applied based on the variables
     *            which are known to be bound as of the cutoff join for the last
     *            vertex in the path segment.
     * @param pathIsComplete
     *            <code>true</code> iff all vertices in the join graph are
     *            incorporated into this path.
     * @param sourceSample
     *            The input sample for the cutoff join. When this is a one-step
     *            estimation of the cardinality of the edge, then this sample is
     *            taken from the {@link VertexSample}. When the edge (vSource,
     *            vTarget) extends some {@link Path}, then this is taken from
     *            the {@link EdgeSample} for that {@link Path}.
     * 
     * @return The result of sampling that edge.
     * 
     * @throws Exception
     */
    static public EdgeSample cutoffJoin(//
            final QueryEngine queryEngine,//
            final JoinGraph joinGraph,//
            final int limit,//
            final IPredicate<?>[] path,//
            final IConstraint[] constraints,//
            final boolean pathIsComplete,//
            final SampleBase sourceSample//
    ) throws Exception {

        // Note: Delegated to the AST/RTO integration class.
        return AST2BOpRTO.cutoffJoin(queryEngine, joinGraph, limit, path,
                constraints, pathIsComplete, sourceSample);

    }

}
