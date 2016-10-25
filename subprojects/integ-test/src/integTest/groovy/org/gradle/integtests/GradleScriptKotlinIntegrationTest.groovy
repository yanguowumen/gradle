/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.integtests

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.util.Requires
import spock.lang.Issue

import static org.gradle.util.TestPrecondition.FIX_TO_WORK_ON_JAVA9
import static org.gradle.util.TestPrecondition.NOT_JDK_IBM

@Issue("https://github.com/gradle/gradle-script-kotlin/issues/154")
@Requires([FIX_TO_WORK_ON_JAVA9, NOT_JDK_IBM])
class GradleScriptKotlinIntegrationTest extends AbstractIntegrationSpec {

    @Override
    protected String getDefaultBuildFileName() {
        'build.gradle.kts'
    }

    def setup() {
        settingsFile << "rootProject.buildFileName = '$defaultBuildFileName'"
    }

    def 'can run a simple task'() {
        given:
        buildFile << """
            import org.gradle.api.*
            import org.gradle.api.tasks.*
            
            open class SimpleTask : DefaultTask() {
                @TaskAction fun run() = println("it works!")
            }

            task<SimpleTask>("build")
        """

        when:
        run 'build'

        then:
        result.output.contains('it works!')
    }
}
