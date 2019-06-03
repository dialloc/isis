/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.core.runtime.headless;

import java.io.PrintStream;

import org.apache.isis.applib.clock.Clock;
import org.apache.isis.applib.services.xactn.TransactionService;
import org.apache.isis.config.IsisConfiguration;
import org.apache.isis.core.plugins.environment.IsisSystemEnvironment;
import org.apache.isis.core.runtime.headless.logging.LeveledLogger;
import org.apache.isis.core.runtime.headless.logging.LogConfig;
import org.apache.isis.core.runtime.headless.logging.LogStream;
import org.apache.isis.core.runtime.logging.IsisLoggingConfigurer;
import org.apache.logging.log4j.Level;

import lombok.extern.log4j.Log4j2;

/**
 * Provides headless access to the system, first bootstrapping the system if required.
 *
 * <p>
 *     This acts as a common superclass from which framework-provided adapter classes for integration tests
 *     (<tt>IntegrationTestAbstract3</tt>) and for BDD spec glue (<tt>CukeGlueIntegrationScopeAbstract</tt> inherit,
 *     to bootstrap the system with a given module for headless access.
 * </p>
 */
@Log4j2
public abstract class HeadlessWithBootstrappingAbstract extends HeadlessAbstract {

    private final LogConfig logConfig;
    protected static PrintStream logPrintStream(Level level) {
        return LogStream.logPrintStream(log, level);
    }

    private final LeveledLogger logger;

    private final static ThreadLocal<Boolean> setupLogging = new ThreadLocal<Boolean>() {{
        set(false);
    }};


    private final IsisSystemBootstrapper isisSystemBootstrapper;

    protected HeadlessWithBootstrappingAbstract(final LogConfig logConfig, IsisConfiguration config) {

        IsisSystemEnvironment.setUnitTesting(true);
         
        this.logConfig = logConfig;
        this.logger = new LeveledLogger(log, logConfig.getTestLoggingLevel());

        final boolean firstTime = !setupLogging.get();
        if(firstTime) {
            IsisLoggingConfigurer loggingConfigurer = new IsisLoggingConfigurer(org.apache.log4j.Level.INFO);
            loggingConfigurer.configureLoggingWithFile(logConfig.getLoggingPropertyFile(), new String[0]);
            setupLogging.set(true);
        }

        final String integTestModuleFqcn = System.getProperty("isis.integTest.module");
        log.info("isis.integTest.module = {}", integTestModuleFqcn);
        String moduleFqcn = integTestModuleFqcn;

        if(moduleFqcn == null) {
            final String headlessModuleFqcn = System.getProperty("isis.headless.module");
            log.info("isis.headless.module = {}", headlessModuleFqcn);
            moduleFqcn = headlessModuleFqcn;
        }

        this.isisSystemBootstrapper =
                IsisSystemBootstrapper.of(logConfig, config);
    }


    private org.joda.time.LocalDate timeBeforeTest;

    protected void bootstrapAndSetupIfRequired() {

        System.setProperty("isis.headless", "true");
        System.setProperty("isis.integTest", "true");
        System.setProperty("isis.bddSpec", "true");

        isisSystemBootstrapper.bootstrapIfRequired();
        isisSystemBootstrapper.injectServicesInto(this);
        fixtureScripts.setFixtureTracing(logConfig.getFixtureTracing());

        beginTransaction();

        isisSystemBootstrapper.setupModuleRefData();

        timeBeforeTest = Clock.getTimeAsLocalDate();
    }

    private void beginTransaction() {
        IsisSystem.get().getService(HeadlessTransactionSupport.class).beginTransaction();
    }

    protected void tearDownAllModules() {

        final boolean testHealthy = transactionService != null;
        if(!testHealthy) {
            // avoid throwing an NPE here if something unexpected has occurred...
            return;
        }

        transactionService.nextTransaction(TransactionService.Policy.ALWAYS);

        isisSystemBootstrapper.tearDownAllModules();

        // reinstate clock
        setFixtureClockDate(timeBeforeTest);
    }

    protected void log(final String message) {
        logger.log(message);
    }
}