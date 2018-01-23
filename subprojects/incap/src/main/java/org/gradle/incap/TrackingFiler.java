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

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

class TrackingFiler implements Filer {
    private final Filer delegate;
    private final File mappingFile;

    TrackingFiler(Filer delegate, File mappingFile) {
        this.delegate = delegate;
        this.mappingFile = mappingFile;
    }

    @Override
    public JavaFileObject createSourceFile(CharSequence name, Element... originatingElements) throws IOException {
        printMapping(name, originatingElements);
        return delegate.createSourceFile(name, originatingElements);
    }

    @Override
    public JavaFileObject createClassFile(CharSequence name, Element... originatingElements) throws IOException {
        printMapping(name, originatingElements);
        return delegate.createClassFile(name, originatingElements);
    }

    private void printMapping(CharSequence generatedType, Element[] originatingElements) throws IOException {
        FileOutputStream fis = new FileOutputStream(mappingFile, true);
        try {
            PrintStream out = new PrintStream(fis, true, "UTF-8");
            for (Element originatingElement : originatingElements) {
                String originName = getTopLevelType(originatingElement).getQualifiedName().toString();
                out.println(originName + ";" + generatedType);
            }
        } finally {
            fis.close();
        }
    }

    private TypeElement getTopLevelType(Element originatingElement) {
        Element current = originatingElement;
        Element parent;
        while ((parent = current.getEnclosingElement()) != null && !(parent instanceof PackageElement)) {
            current = parent;
        }
        if (!(current instanceof TypeElement)) {
            throw new IllegalArgumentException(originatingElement.getKind() + " can not be processed incrementally");
        }
        return (TypeElement) current;
    }

    @Override
    public FileObject createResource(JavaFileManager.Location location, CharSequence pkg, CharSequence relativeName, Element... originatingElements) throws IOException {
        throw new UnsupportedOperationException("Incremental annotation processors are not allowed to generate resources");
    }

    @Override
    public FileObject getResource(JavaFileManager.Location location, CharSequence pkg, CharSequence relativeName) throws IOException {
        throw new UnsupportedOperationException("Incremental annotation processors are not allowed to query resources");
    }
}
