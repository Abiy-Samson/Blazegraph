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
 * Created on Sep 18, 2009
 */

package com.bigdata.samples;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;

import com.bigdata.journal.Journal;
import com.bigdata.journal.RWStrategy;
import com.bigdata.rdf.load.RDFFilenameFilter;
import com.bigdata.rdf.sail.BigdataSail;
import com.bigdata.rdf.sail.BigdataSailRepository;
import com.bigdata.rdf.store.DataLoader;

/**
 * Sample code for loading RDF data using the {@link BigdataSail} and the
 * openrdf API.
 * 
 * @see DataLoader
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class LoadNamedGraphs extends SampleCode {

    /**
     * Load all data from some directory.
     * 
     * @param dir
     * 
     * @throws Exception
     */
    public void loadAll(final Properties properties, final File file) throws Exception {

		/*
		 * We are going to use the "quads" mode. Right now, the quads mode does
		 * not do inference AT ALL.
		 */
//		final File propertyFile = new File(
//				"c:/bigdata-data-sets/LoadNamedGraphs.properties");
    	
//        // create a backing file
//        final File journalFile = new File("c:/bigdata.jnl");
////        final File journalFile = File.createTempFile("bigdata", ".jnl");
////        journalFile.deleteOnExit();
//        properties.setProperty(BigdataSail.Options.FILE, journalFile
//                .getAbsolutePath());

        // You can do the overrides in the property file.
//        /*
//         * Override the write retention queue (default is 500).
//         * 
//         * This makes a BIG difference in the journal size and throughput if you
//         * are bulk loading data and have enough RAM.
//         */
//        properties.setProperty(
//                IndexMetadata.Options.WRITE_RETENTION_QUEUE_CAPACITY, "8000");
//
//        properties.setProperty(IndexMetadata.Options.BTREE_BRANCHING_FACTOR,
//                "64");

        // instantiate a sail
        final BigdataSail sail = new BigdataSail(properties);
        try {
        final Repository repo = new BigdataSailRepository(sail);
        repo.initialize();

        final RepositoryConnection cxn = repo.getConnection();
        cxn.setAutoCommit(false);
        try {
            final long stmtsBefore =
                // fast range count!
                sail.getDatabase().getStatementCount();
//                cxn.size();
            final long start = System.currentTimeMillis();

            if (file.getName().endsWith(".zip")
					|| file.getName().endsWith(".ZIP")) {

				// then process the sample data files one at a time
				final InputStream is = new FileInputStream(file);
				try {
				final ZipInputStream zis = new ZipInputStream(
						new BufferedInputStream(is));
				try {
				ZipEntry ze = null;
				while ((ze = zis.getNextEntry()) != null) {
					if (ze.isDirectory()) {
						continue;
					}
					final String name = ze.getName();
					if (log.isInfoEnabled())
						log.info(name);
					final ByteArrayOutputStream baos = new ByteArrayOutputStream();
					final byte[] bytes = new byte[4096];
					int count;
					while ((count = zis.read(bytes, 0, 4096)) != -1) {
						baos.write(bytes, 0, count);
					}
					baos.close();
					final Reader reader = new InputStreamReader(
							new ByteArrayInputStream(baos.toByteArray()));
					final String baseIRI = file.toURI() + "/" + name;

					cxn.add(reader, baseIRI, RDFFormat.forFileName(name));

					// note: due to buffering, this reports stmts flush to the
					// db
					// not stmts added to the cxn.
					final long elapsed = System.currentTimeMillis() - start;
	                // fast range count!
	                final long stmtsAfter = sail.getDatabase().getStatementCount();
//					long stmtsAfter = cxn.size();
					final long stmtsAdded = stmtsAfter - stmtsBefore;
					final int throughput = (int) ((double) stmtsAdded
							/ (double) elapsed * 1000d);
					System.err.println("loaded: " + name + " : " + stmtsAdded
							+ " stmts in " + elapsed + " millis: " + throughput
							+ " stmts/sec");
					logCounters(sail);
				}
				} finally {
				zis.close();
				}
				} finally {
					is.close();
				}
				
			} else if(file.isDirectory()) {
				
				final File[] files = file.listFiles(new RDFFilenameFilter());
				
				if (files != null) {

				    int nloaded = 0;
				    
					for (File f : files) {

//						System.err.println("Reading: " + f);
						
						final Reader reader = new InputStreamReader(
								(f.getName().endsWith(".gz")
										|| f.getName().endsWith(".GZ") ? new GZIPInputStream(
										new FileInputStream(f))
										: new FileInputStream(f)));

						try {
						
						final String baseIRI = file.toURI().toString();

						cxn.add(reader, baseIRI, RDFFormat.forFileName(f
								.getName()));

                        /*
                         * Note: due to buffering, this reports stmts flushed to
                         * the db not stmts added to the cxn.
                         * 
                         * Note: cxn.size() will do a FULL SCAN of the statement
                         * index for many cases in order to report an exact
                         * range count.  This is an issue with the Sesame API
                         * semantics (exact range count reporting) and with
                         * delete markers in the bigdata indices.  Fast range
                         * counts are available with two key probes but do not
                         * satisfy the Sesame semantics.  You can get the fast
                         * range count from the bigdata APIs.
                         */
						final long elapsed = System.currentTimeMillis() - start;
		                // fast range count!
						final long stmtsAfter = sail.getDatabase().getStatementCount();
//						long stmtsAfter = cxn.size();
						final long stmtsAdded = stmtsAfter - stmtsBefore;
						final int throughput = (int) ((double) stmtsAdded
								/ (double) elapsed * 1000d);

                        nloaded++;

						System.err.println("loaded: " + f 
						        + " : " + stmtsAdded + " stmts"
						        +" in " + elapsed + " millis" + 
						        " : "+ throughput + " stmts/sec"+
								", nloaded="+nloaded);
						logCounters(sail);

						} finally {
						reader.close();
						}
						
					}
					
				}
				
			} else if(file.isFile()) {
				
				final Reader reader = new InputStreamReader(
						new FileInputStream(file));
				try {
				final String baseIRI = file.toURI().toString();

				cxn.add(reader, baseIRI, RDFFormat.forFileName(file
						.getName()));

				// note: due to buffering, this reports stmts flush to the
				// db not stmts added to the cxn.
				final long elapsed = System.currentTimeMillis() - start;
//				long stmtsAfter = cxn.size();
				final long stmtsAfter = sail.getDatabase().getStatementCount();
				final long stmtsAdded = stmtsAfter - stmtsBefore;
				final int throughput = (int) ((double) stmtsAdded
						/ (double) elapsed * 1000d);

				System.err.println("loaded: " + file + " : " + stmtsAdded
						+ " stmts in " + elapsed + " millis: " + throughput
						+ " stmts/sec");
	            logCounters(sail);
				} finally {
				reader.close();
				}
				
			} else {
				
				System.err.println("Can not load: "+file);
				
			}
            
            // autocommit is false, we need to commit our SAIL "transaction"
            cxn.commit();

            // gather statistics
            final long elapsed = System.currentTimeMillis() - start;
//            long stmtsAfter = cxn.size();
            final long stmtsAfter = sail.getDatabase().getStatementCount();
            final long stmtsAdded = stmtsAfter - stmtsBefore;
            final int throughput = (int) ((double) stmtsAdded / (double) elapsed * 1000d);
            System.err.println("statements after: " + stmtsAfter);
            System.err.println("loaded: " + stmtsAdded + " in " + elapsed + " millis: "
                    + throughput + " stmts/sec");
            logCounters(sail);

        } catch (Exception ex) {
            cxn.rollback();
            throw ex;
        } finally {
            // close the repository connection
            cxn.close();
        }
        } finally {
            sail.shutDown();
        }

    }

    private void logCounters(final BigdataSail sail) {

    	if (!(sail.getDatabase().getIndexManager() instanceof Journal))
			return;
		
    	if (!(((Journal) sail.getDatabase().getIndexManager())
				.getBufferStrategy() instanceof RWStrategy))
			return;
		
    	final StringBuilder sb = new StringBuilder();
    	
    	((RWStrategy) ((Journal) sail.getDatabase()
				.getIndexManager()).getBufferStrategy()).getStore()
				.showAllocators(sb);
    	
    	System.err.println(sb.toString());

	}
    
	/**
	 * Loads a bunch of data from a file, zip file, or directory
	 * (non-recursive). You can use <code>quad.properties</code> as the
	 * properties file or anything else that you like.
	 * 
	 * @param args
	 *            The name of the property and the name of the file or directory
	 *            to load.
	 * 
	 * @throws Exception
	 */
    public static void main(final String[] args) {

        if (args.length < 2 ) {

            System.out.println("usage: properties fileOrDirectoryOrZip");

            System.exit(1);

        }

		final Properties properties;
		try {

			final File propertyFile = new File(args[0]);

			if (!propertyFile.exists()) {

				throw new FileNotFoundException(propertyFile.toString());
				
			}

			properties = new Properties();
			
			final InputStream is = new BufferedInputStream(new FileInputStream(
					propertyFile));
			
			try {
		
				properties.load(is);
				
			} finally {
				
				is.close();
				
			}

		} catch(IOException ex) {
			
			throw new RuntimeException(ex);
			
		}
    	
        try {
       
        	final File dataFileOrDirectory = new File(args[1]);
        	
        	if (!dataFileOrDirectory.exists())
				throw new FileNotFoundException(dataFileOrDirectory.toString());
        	
            new LoadNamedGraphs().loadAll(properties, dataFileOrDirectory);
            
        } catch (Exception ex) {
            
            ex.printStackTrace(System.err);
            
        }

    }

}
