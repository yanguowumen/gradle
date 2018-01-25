/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.testing.worker

import org.gradle.api.Action
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.classpath.Module
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory
import org.gradle.internal.classpath.ClassPath
import org.gradle.internal.remote.ObjectConnection
import org.gradle.internal.work.WorkerLeaseRegistry
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.JavaExecHandleBuilder
import org.gradle.process.internal.worker.WorkerProcess
import org.gradle.process.internal.worker.WorkerProcessBuilder
import org.gradle.process.internal.worker.WorkerProcessFactory
import spock.lang.Specification
import spock.lang.Subject

class ForkingTestClassProcessorTest extends Specification {
    WorkerLeaseRegistry.WorkerLease workerLease = Mock(WorkerLeaseRegistry.WorkerLease)
    WorkerProcessFactory workerProcessFactory = Mock(WorkerProcessFactory)
    WorkerProcessBuilder workerProcessBuilder = Mock(WorkerProcessBuilder)
    WorkerProcess workerProcess = Mock(WorkerProcess)
    ModuleRegistry moduleRegistry = Mock(ModuleRegistry)
    JavaForkOptions options = Mock(JavaForkOptions)
    DocumentationRegistry documentationRegistry = Mock(DocumentationRegistry)

    @Subject
        processor = Spy(ForkingTestClassProcessor, constructorArgs: [workerLease, workerProcessFactory, Mock(WorkerTestClassProcessorFactory), options, [new File("classpath.jar")], Mock(Action), moduleRegistry, documentationRegistry])

    def "acquires worker lease and starts worker process on first test"() {
        def test1 = Mock(TestClassRunInfo)
        def test2 = Mock(TestClassRunInfo)

        def remoteProcessor = Mock(RemoteTestClassProcessor)

        when:
        processor.processTestClass(test1)
        processor.processTestClass(test2)

        then:
        1 * workerLease.startChild()
        1 * options.getSystemProperties() >> [:]
        1 * processor.forkProcess() >> remoteProcessor
        1 * remoteProcessor.processTestClass(test1)
        1 * remoteProcessor.processTestClass(test2)
        0 * remoteProcessor._
    }

    def "starts process with a limited implementation classpath"() {
        setup:
        1 * workerProcessFactory.create(_) >> workerProcessBuilder
        1 * workerProcessBuilder.build() >> workerProcess
        _ * workerProcessBuilder.getJavaCommand() >> Stub (JavaExecHandleBuilder)
        1 * workerProcess.getConnection() >> Stub(ObjectConnection) { addOutgoing(_) >> Stub(RemoteTestClassProcessor) }

        when:
        processor.forkProcess()

        then:
        11 * moduleRegistry.getModule(_) >> { module(it[0]) }
        7 * moduleRegistry.getExternalModule(_) >> { module(it[0]) }
        1 * workerProcessBuilder.setImplementationClasspath(_) >> { assert it[0].size() == 18 }
    }

    def module(String module) {
        return Stub(Module) {
            _ * getImplementationClasspath() >> {
                Stub(ClassPath) {
                    _ * getAsURLs() >> { [new URL("file://${module}.jar")] }
                }
            }
        }
    }
}
