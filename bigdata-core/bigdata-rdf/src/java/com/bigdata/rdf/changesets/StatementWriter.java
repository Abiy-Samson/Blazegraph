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
package com.bigdata.rdf.changesets;

import java.util.Iterator;

import org.apache.log4j.Logger;

import com.bigdata.rdf.spo.ISPO;
import com.bigdata.rdf.spo.ModifiedEnum;
import com.bigdata.rdf.spo.SPO;
import com.bigdata.rdf.store.AbstractTripleStore;
import com.bigdata.relation.accesspath.IElementFilter;
import com.bigdata.striterator.ChunkedArrayIterator;
import com.bigdata.striterator.IChunkedOrderedIterator;

public class StatementWriter {
    
//    private static final Logger log = Logger.getLogger(StatementWriter.class);
    
    public static long addStatements(final AbstractTripleStore database,
            final AbstractTripleStore statementStore, 
            final boolean copyOnly,
            final IElementFilter<ISPO> filter, 
            final IChunkedOrderedIterator<ISPO> itr, 
            final IChangeLog changeLog) {
        
        long n = 0;
        
        if (itr.hasNext()) {
            
//            final BigdataStatementIteratorImpl itr2 = 
//                new BigdataStatementIteratorImpl(database, bnodes, itr)
//                    .start(database.getExecutorService()); 
//            
//            final BigdataStatement[] stmts = 
//                new BigdataStatement[database.getChunkCapacity()];
            final SPO[] stmts = new SPO[database.getChunkCapacity()];
            
            int i = 0;
            while ((i = nextChunk(itr, stmts)) > 0) {
                n += addStatements(database, statementStore, copyOnly, filter, 
                        stmts, i, changeLog);
            }
            
        }
        
        return n;
        
    }
    
    private static long addStatements(final AbstractTripleStore database,
            final AbstractTripleStore statementStore, 
            final boolean copyOnly,
            final IElementFilter<ISPO> filter,
            final ISPO[] stmts, 
            final int numStmts,
            final IChangeLog changeLog) {
        
//        final SPO[] tmp = allocateSPOs(stmts, numStmts);
        
        final long n = database.addStatements(statementStore, copyOnly,
                new ChunkedArrayIterator<ISPO>(numStmts, stmts, 
                        null/* keyOrder */), filter);

        // Copy the state of the isModified() flag and notify changeLog
        for (int i = 0; i < numStmts; i++) {

            if (stmts[i].isModified()) {

//                stmts[i].setModified(true);
                
                if (changeLog != null) {
                    
                    switch(stmts[i].getModified()) {
                    case INSERTED:
                        changeLog.changeEvent(new ChangeRecord(stmts[i], ChangeAction.INSERTED));
                        break;
                    case UPDATED:
                        changeLog.changeEvent(new ChangeRecord(stmts[i], ChangeAction.UPDATED));
                        break;
                    case REMOVED:
                        throw new AssertionError();
                    default:
                        break;
                    }
                    
                }

            }
            
        }
        
        return n;
        
    }
    
    public static long removeStatements(final AbstractTripleStore database,
            final IChunkedOrderedIterator<ISPO> itr,
            final boolean computeClosureForStatementIdentifiers,
            final IChangeLog changeLog) {
        
        long n = 0;
        
        if (itr.hasNext()) {
            
//            final BigdataStatementIteratorImpl itr2 = 
//                new BigdataStatementIteratorImpl(database, bnodes, itr)
//                    .start(database.getExecutorService()); 
//            
//            final BigdataStatement[] stmts = 
//                new BigdataStatement[database.getChunkCapacity()];
            final ISPO[] stmts = new ISPO[database.getChunkCapacity()];
            
            int i = 0;
            while ((i = nextChunk(itr, stmts)) > 0) {
                n += removeStatements(database, stmts, i, 
                        computeClosureForStatementIdentifiers, changeLog);
            }
            
        }
        
        return n;
        
    }
    
    public static long removeStatements(final AbstractTripleStore database,
            final ISPO[] stmts, 
            final int numStmts,
            final boolean computeClosureForStatementIdentifiers,
            final IChangeLog changeLog) {
        
        final long n = database.removeStatements(
                new ChunkedArrayIterator<ISPO>(numStmts, stmts, 
                        null/* keyOrder */), 
                computeClosureForStatementIdentifiers);

        // Copy the state of the isModified() flag and notify changeLog
        for (int i = 0; i < numStmts; i++) {

            if (stmts[i].isModified()) {

                // just to be safe
                stmts[i].setModified(ModifiedEnum.REMOVED);
                
                changeLog.changeEvent(
                        new ChangeRecord(stmts[i], ChangeAction.REMOVED));

            }
            
        }
        
        return n;
        
    }
    
    private static int nextChunk(final Iterator<ISPO> itr, 
            final ISPO[] stmts) {
        
        assert stmts != null && stmts.length > 0;
        
        int i = 0;
        while (itr.hasNext()) {
            stmts[i++] = itr.next();
            if (i == stmts.length) {
                // stmts[] is full
                return i;
            }
        }
        
        /*
         * stmts[] is empty (i = 0) or partially 
         * full (i > 0 && i < stmts.length)
         */ 
        return i;
        
    }
    
//    private static SPO[] allocateSPOs(final BigdataStatement[] stmts, 
//            final int numStmts) {
//        
//        final SPO[] tmp = new SPO[numStmts];
//
//        for (int i = 0; i < tmp.length; i++) {
//
//            final BigdataStatement stmt = stmts[i];
//            
//            final SPO spo = new SPO(stmt);
//
//            if (log.isDebugEnabled())
//                log.debug("writing: " + stmt.toString() + " (" + spo + ")");
//            
//            if(!spo.isFullyBound()) {
//                
//                throw new AssertionError("Not fully bound? : " + spo);
//                
//            }
//            
//            tmp[i] = spo;
//
//        }
//        
//        return tmp;
//        
//        
//    }
    
}
