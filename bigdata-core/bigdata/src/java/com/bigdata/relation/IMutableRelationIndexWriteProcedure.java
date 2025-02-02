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
 * Created on Apr 9, 2009
 */

package com.bigdata.relation;

import com.bigdata.btree.IIndex;
import com.bigdata.btree.proc.AbstractKeyArrayIndexProcedure;
import com.bigdata.btree.proc.IIndexProcedure;
import com.bigdata.btree.proc.IParallelizableIndexProcedure;

/**
 * Marker interface for procedures responsible for writing on an {@link IIndex}
 * in order to satisfy an {@link IMutableRelation} write. Many relations
 * maintain multiple index orders. There is generally one such procedure for
 * each index order. These procedures generally accept <code>byte[][]</code>
 * keys and values and therefore should extend
 * {@link AbstractKeyArrayIndexProcedure}. In order to use scattered writes, the
 * procedures should also implement {@link IParallelizableIndexProcedure}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IMutableRelationIndexWriteProcedure<T> extends IIndexProcedure<T> {

}
