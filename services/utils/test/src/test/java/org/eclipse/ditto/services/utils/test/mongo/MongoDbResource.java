/*
 * Copyright (c) 2017 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/index.php
 *
 * Contributors:
 *    Bosch Software Innovations GmbH - initial contribution
 */
package org.eclipse.ditto.services.utils.test.mongo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Assume;
import org.junit.rules.ExternalResource;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.store.HttpProxyFactory;
import de.flapdoodle.embed.process.config.store.IProxyFactory;
import de.flapdoodle.embed.process.config.store.NoProxyFactory;
import de.flapdoodle.embed.process.io.progress.StandardConsoleProgressListener;

/**
 * External Mongo DB resource for utilization within tests.
 */
public final class MongoDbResource extends ExternalResource {

    /**
     * Default port number of the MongoDB process.
     */
    private static final int MONGO_DB_PORT_DEFAULT = 27030;
    private static final String HTTP_PROXY_ENV_KEY = "HTTP_PROXY";

    /**
     * The MongoDB executable.
     */
    private MongodExecutable mongodExecutable;

    /**
     * The MongoDB process.
     */
    private MongodProcess mongodProcess;

    /**
     * Constructs a new {@code MongoDbResource} object.
     */
    public MongoDbResource() {
        mongodExecutable = null;
        mongodProcess = null;
    }

    public void start() {
        before();
    }

    public void stop() {
        after();
    }

    @Override
    protected void before() {
        final Optional<String> proxyUppercase = Optional.ofNullable(System.getenv(HTTP_PROXY_ENV_KEY));
        final Optional<String> proxyLowercase = Optional.ofNullable(System.getenv(HTTP_PROXY_ENV_KEY.toLowerCase()));
        final Optional<String> httpProxy = proxyUppercase.isPresent() ? proxyUppercase : proxyLowercase;
        final IProxyFactory proxyFactory = httpProxy
                .map(URI::create)
                .map(proxyURI -> ((IProxyFactory) new HttpProxyFactory(
                        proxyURI.getHost(), proxyURI.getPort())))
                .orElse(new NoProxyFactory());

        int mongoDbPort = MONGO_DB_PORT_DEFAULT;
        if (System.getProperty("ACTIVE_RANDOM_PORT") != null) {
            mongoDbPort = findFreePort();
        }
        mongodExecutable = tryToConfigureMongoDb(mongoDbPort, proxyFactory);
        mongodProcess = tryToStartMongoDb(mongodExecutable);
        Assume.assumeTrue("MongoDBResource failed to start.", isHealthy());
    }

    @Override
    protected void after() {
        if (mongodProcess != null) {
            mongodProcess.stop();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
    }

    /**
     * @return whether mongodb started successfully.
     */
    public boolean isHealthy() {
        return mongodProcess != null && mongodExecutable != null;
    }

    /**
     * @return the port on which the db listens.
     */
    public int getPort() {
        return mongodProcess.getConfig().net().getPort();
    }

    /**
     * This method will return a free port number.
     *
     * @return the port number.
     * @throws IllegalStateException if no free port available.
     */
    private static int findFreePort() {
        final Supplier<Integer> freePortFinder = new FreePortFinder();
        return freePortFinder.get();
    }

    private MongodExecutable tryToConfigureMongoDb(final int mongoDbPort,
            final IProxyFactory proxyFactory) {
        try {
            return configureMongoDb(mongoDbPort, proxyFactory);
        } catch (final Throwable e) {
            return null;
        }
    }

    private static MongodExecutable configureMongoDb(final int mongoDbPort,
            final IProxyFactory proxyFactory) throws IOException {
        final Command command = Command.MongoD;

        final MongodStarter mongodStarter = MongodStarter.getInstance(new RuntimeConfigBuilder()
                .defaults(command)
                .artifactStore(new ArtifactStoreBuilder()
                        .defaults(command)
                        .download(new DownloadConfigBuilder()
                                .defaultsForCommand(command)
                                .proxyFactory(proxyFactory)
                                .progressListener(new StandardConsoleProgressListener())
                        )
                )
                .build());

        return mongodStarter.prepare(new MongodConfigBuilder()
                .net(new Net(mongoDbPort, false))
                .version(Version.Main.V3_2)
                .cmdOptions(new MongoCmdOptionsBuilder()
                        .useStorageEngine("wiredTiger")
                        .useNoJournal(false)
                        .build())
                .build());
    }

    public static MongodProcess tryToStartMongoDb(final MongodExecutable mongodExecutable) {
        if (mongodExecutable == null) {
            return null;
        } else {
            try {
                return mongodExecutable.start();
            } catch (final IOException e) {
                throw new IllegalStateException("Failed to start MongoDB!", e);
            }
        }
    }

    private static final class FreePortFinder implements Supplier<Integer> {

        @Override
        public Integer get() {
            try (ServerSocket socket = tryToCreateServerSocket()) {
                tryToSetReuseAddress(socket);
                return socket.getLocalPort();
            } catch (final IOException e) {
                throw new IllegalStateException("Failed to close server socket!", e);
            }
        }

        private static ServerSocket tryToCreateServerSocket() {
            try {
                return new ServerSocket(0);
            } catch (final IOException e) {
                throw new IllegalStateException("Failed to create a ServerSocket object!", e);
            }
        }

        private static void tryToSetReuseAddress(final ServerSocket serverSocket) {
            try {
                serverSocket.setReuseAddress(true);
            } catch (final SocketException e) {
                throw new IllegalStateException("Failed to set reuse address to server socket!", e);
            }
        }

    }

}
