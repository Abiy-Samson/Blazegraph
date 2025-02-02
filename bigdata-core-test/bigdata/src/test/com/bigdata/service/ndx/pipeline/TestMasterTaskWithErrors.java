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
 * Created on Apr 16, 2009
 */

package com.bigdata.service.ndx.pipeline;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.bigdata.btree.keys.KVO;
import com.bigdata.relation.accesspath.BlockingBuffer;
import com.bigdata.util.InnerCause;

/**
 * Unit tests for error handling in the control logic used by
 * {@link AbstractMasterTask} and friends.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestMasterTaskWithErrors extends AbstractMasterTestCase {

    public TestMasterTaskWithErrors() {
    }

    public TestMasterTaskWithErrors(String name) {
        super(name);
    }

    /**
     * Unit test verifies correct shutdown and error reporting when a subtask
     * fails.
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void test_startWriteErrorStop() throws InterruptedException,
            ExecutionException {

        final H masterStats = new H();

        final BlockingBuffer<KVO<O>[]> masterBuffer = new BlockingBuffer<KVO<O>[]>(
                masterQueueCapacity);

        /*
         * Note: The master is overridden so that the 1st chunk written onto
         * locator(13) will cause an exception to be thrown.
         */

        final M master = new M(masterStats, masterBuffer, executorService) {
          
            @Override
            protected S newSubtask(L locator, BlockingBuffer<KVO<O>[]> out) {

                if (locator.locator == 13) {

                    return new S(this, locator, out) {

                        @Override
                        protected boolean handleChunk(KVO<O>[] chunk)
                                throws Exception {

                            throw new TestException();
                            
                        }

                    };
                    
                }

                return super.newSubtask(locator, out);
                
            }
            
        };
        
        // Wrap computation as FutureTask.
        final FutureTask<H> ft = new FutureTask<H>(master);
        
        // Set Future on BlockingBuffer.
        masterBuffer.setFuture(ft);
        
        // Start the consumer.
        executorService.submit(ft);
        
        final KVO<O>[] a = new KVO[] {
                new KVO<O>(new byte[]{1},new byte[]{2},null/*val*/),
                new KVO<O>(new byte[]{13},new byte[]{3},null/*val*/)
        };

        masterBuffer.add(a);

        masterBuffer.close();

        try {

            // Note: We expect an exception.
            masterBuffer.getFuture().get();

            fail("Not expecting master to succeed.");
            
        } catch (ExecutionException ex) {
            
            final TestException t = (TestException) InnerCause.getInnerCause(
                    ex, TestException.class);
            
            if (t == null) {
            
                // not the exception we were expecting.
                throw ex;
            
            }
            
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + t);
        
        }

        assertEquals("elementsIn", a.length, masterStats.elementsIn.get());
        assertEquals("chunksIn", 1, masterStats.chunksIn.get());
        assertEquals("partitionCount", 2, masterStats
                .getMaximumPartitionCount());

        /*
         * Note: There is no way to predict whether any chunks will have been
         * written on L(1) since it is drained by its own Thread.
         */
        
    }

}
