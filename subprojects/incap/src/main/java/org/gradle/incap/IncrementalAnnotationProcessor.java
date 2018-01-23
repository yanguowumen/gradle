/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.incap;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public abstract class IncrementalAnnotationProcessor implements Processor {

    private static final String MAPPING_FILE = "org.gradle.incap.mappingFile";
    private final Processor delegate;

    public IncrementalAnnotationProcessor(String delegateName) {
        try {
            delegate = (Processor) Class.forName(delegateName).newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Could not instantiate " + delegateName, e);
        }
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<String>(delegate.getSupportedOptions());
        options.add(MAPPING_FILE);
        return options;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return delegate.getSupportedAnnotationTypes();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return delegate.getSupportedSourceVersion();
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        delegate.init(new TrackingProcessingEnvironment(processingEnv, getMappingFile(processingEnv)));
    }

    private File getMappingFile(ProcessingEnvironment processingEnv) {
        String mappingFileName = processingEnv.getOptions().get(MAPPING_FILE);
        if (mappingFileName == null) {
            throw new IllegalStateException(MAPPING_FILE + " must be set for incremental annotation processing");
        }
        File mappingFile = new File(mappingFileName);
        File parentFolder = mappingFile.getParentFile();
        parentFolder.mkdirs();
        return mappingFile;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return delegate.process(annotations, roundEnv);
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return delegate.getCompletions(element, annotation, member, userText);
    }
}
