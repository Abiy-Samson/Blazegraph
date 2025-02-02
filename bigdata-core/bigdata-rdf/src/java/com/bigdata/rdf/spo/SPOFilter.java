/**
   Copyright (C) SYSTAP, LLC 2006-2012.  All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.bigdata.rdf.spo;

import com.bigdata.relation.accesspath.IElementFilter;

public abstract class SPOFilter<E extends ISPO> implements IElementFilter<E> {
        
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public boolean canAccept(final Object o) {
        
        return o instanceof ISPO;
        
    }
    
}
