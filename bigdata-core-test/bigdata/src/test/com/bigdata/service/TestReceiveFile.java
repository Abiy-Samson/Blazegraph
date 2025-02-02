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
 * Created on Jun 18, 2006
 */
package com.bigdata.service;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.bigdata.service.ResourceService.ReadResourceTask;
import com.bigdata.util.DaemonThreadFactory;
import com.bigdata.util.config.NicUtil;

/**
 * Test verifies the ability to transmit a file using the
 * {@link ResourceService}.
 * 
 * @version $Id$
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson
 *         </a>
 */
public class TestReceiveFile extends TestCase3 {

    /**
     * 
     */
    public TestReceiveFile() {
        super();
    }

    public TestReceiveFile(String name) {
        super(name);
    }

    /**
     * Test the ability to receive a file.
     * 
     * @throws Exception
     */
    public void test_receiveFile() throws Exception {

        final UUID allowedUUID = UUID.randomUUID();

        final File allowedFile = new File(
                "bigdata/src/test/com/bigdata/service/testSendFile.seg");

        assertTrue("Could not locate file: " + allowedFile, allowedFile
                .exists());

        final File tmpFile = File.createTempFile(allowedFile.getName(), ".tmp");

        final ResourceService service = new ResourceService(
                new InetSocketAddress(InetAddress
                        .getByName(NicUtil.getIpAddress("default.nic",
                                "default", true/* loopbackOk */)), 0/* port */
                ), 0/* requestServicePoolSize */) {
            
            @Override
            protected File getResource(UUID uuid) {

                if (allowedUUID.equals(uuid)) {

                    // allowed.
                    return allowedFile;

                }

                log.warn("Not allowed: " + uuid);

                // Not allowed.
                return null;

            }

            @Override
            protected ByteBuffer getBuffer(UUID uuid) throws Exception {
                // No such resource.
                return null;
            }

        };

        try {

            service.awaitRunning(100, TimeUnit.MILLISECONDS);

            assertTrue(service.isOpen());

            assertEquals(tmpFile, new ReadResourceTask(service.getAddr(),
                    allowedUUID, tmpFile).call());

            if (log.isInfoEnabled())
                log.info(service.counters.getCounters());

        } finally {

            if (tmpFile.exists()) {

                // delete tmp file.
                tmpFile.delete();

            }

            // shutdown the service.
            service.shutdownNow();

            // verify service is down.
            assertFalse(service.isOpen());

        }

    }

    /**
     * Unit test verifies that concurrent "receive" requests for the same
     * resource do not cause problems with {@link FileLock}.
     * <p>
     * Note: {@link OverlappingFileLockException}s can arise when there are
     * concurrent requests to obtain a shared lock on the same file. Personally,
     * I think that this is a bug since the lock requests are shared and should
     * be processed without deadlock.
     * 
     * @see http://blogs.sun.com/DaveB/entry/new_improved_in_java_se1
     * @see http://forums.sun.com/thread.jspa?threadID=5324314.
     */
    public void test_concurrentReceiveRequests() throws Exception {

        final UUID allowedUUID = UUID.randomUUID();

        final File allowedFile = new File(
                "bigdata/src/test/com/bigdata/service/testSendFile.seg");

        assertTrue("Could not locate file: " + allowedFile, allowedFile
                .exists());

        final ResourceService service = new ResourceService(
                new InetSocketAddress(InetAddress
                        .getByName(NicUtil.getIpAddress("default.nic",
                                "default", true/* loopbackOk */)), 0/* port */
                ), 0/* requestServicePoolSize */) {

            @Override
            protected File getResource(UUID uuid) {

                if (allowedUUID.equals(uuid)) {

                    // allowed.
                    return allowedFile;

                }

                log.warn("Not allowed: " + uuid);

                // Not allowed.
                return null;

            }

            @Override
            protected ByteBuffer getBuffer(UUID uuid) throws Exception {
                // No such resource.
                return null;
            }

        };

        final ExecutorService exService = Executors
                .newCachedThreadPool(DaemonThreadFactory.defaultThreadFactory());

        final List<File> tempFiles = new LinkedList<File>();

        try {

            service.awaitRunning(100, TimeUnit.MILLISECONDS);

            assertTrue(service.isOpen());

            final List<Callable<File>> tasks = new LinkedList<Callable<File>>();
            
            for (int i = 0; i < 20; i++) {

                final File tmpFile = File.createTempFile(allowedFile.getName(),
                        ".tmp");

                tempFiles.add(tmpFile);

                tasks.add(new ReadResourceTask(service.getAddr(), allowedUUID,
                        tmpFile));

            }

            final List<Future<File>> futures = exService.invokeAll(tasks);
            
            // verify no errors.
            for(Future<?> f : futures) {
                
                f.get();
                
            }
            
        } finally {

            exService.shutdownNow();
            
            // shutdown the service.
            service.shutdownNow();

            // verify service is down.
            assertFalse(service.isOpen());

            if (log.isInfoEnabled())
                log.info(service.counters.getCounters());

            for(File tmpFile : tempFiles) {
                
                if (tmpFile.exists()) {

                    // delete tmp file.
                    tmpFile.delete();

                }

            }
            
        }

    }

}
