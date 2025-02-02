/*

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
 * Created on Apr 15, 2009
 */

package com.bigdata.service.ndx;

import com.bigdata.counters.CounterSet;
import com.bigdata.service.AbstractFederation;
import com.bigdata.service.ndx.pipeline.IndexAsyncWriteStats;

/**
 * Counters pertaining to a scale-out index. The {@link IScaleOutClientIndex}
 * has two APIs. One is synchronous RPC. The other is an asynchronous stream
 * oriented API for writes. Both sets of performance counters are available from
 * this class.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class ScaleOutIndexCounters {

    /**
     * These counters are used only for the asynchronous write pipeline.
     */
    @SuppressWarnings("unchecked")
    final public IndexAsyncWriteStats asynchronousStats;

    /**
     * These counters are used for the synchronous RPC calls (reads and writes).
     */
    final public IndexSyncRPCCounters synchronousCounters;

    public ScaleOutIndexCounters(AbstractFederation fed) {

        asynchronousStats = new IndexAsyncWriteStats(fed);

        synchronousCounters = new IndexSyncRPCCounters();
        
    }

    /**
     * Return a new {@link CounterSet} reporting both the
     * {@link IndexSyncRPCCounters} and the {@link IndexAsyncWriteStats} for
     * this index.
     * 
     * @todo While this reports several averages, most of them are not moving
     *       averages but averages over the life of the client. They should be
     *       sampled and converted into moving averages.
     */
    public CounterSet getCounters() {

        final CounterSet counterSet = new CounterSet();

        // attach the statistics for asynchronous (pipeline) writes.
        counterSet.makePath("synchronous").attach(
                synchronousCounters.getCounters());

        // attach the statistics for synchronous RPC (reads and writes).
        counterSet.makePath("asynchronous").attach(
                asynchronousStats.getCounterSet());

        return counterSet;
        
    }

    public String toString() {
        
        return getClass().getName() + "{asynchronous=" + asynchronousStats
                + ", synchronous=" + synchronousCounters + "}";
        
    }
    
}
