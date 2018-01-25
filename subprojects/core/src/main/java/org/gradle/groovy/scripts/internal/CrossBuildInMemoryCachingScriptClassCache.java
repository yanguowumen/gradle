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
package org.gradle.groovy.scripts.internal;

import groovy.lang.Script;
import org.codehaus.groovy.ast.ClassNode;
import org.gradle.api.Action;
import org.gradle.api.internal.initialization.loadercache.ClassLoaderId;
import org.gradle.cache.internal.CacheKeyBuilder;
import org.gradle.cache.internal.CrossBuildInMemoryCache;
import org.gradle.cache.internal.CrossBuildInMemoryCacheFactory;
import org.gradle.groovy.scripts.ScriptSource;
import org.gradle.internal.Cast;
import org.gradle.internal.hash.HashCode;

public class CrossBuildInMemoryCachingScriptClassCache {
    private final CrossBuildInMemoryCache<String, CachedCompiledScript> cachedCompiledScripts;
    private final ScriptSourceHasher scriptSourceHasher;
    private final CacheKeyBuilder cacheKeyBuilder;

    public CrossBuildInMemoryCachingScriptClassCache(ScriptSourceHasher scriptSourceHasher, CacheKeyBuilder cacheKeyBuilder, CrossBuildInMemoryCacheFactory cacheFactory) {
        this.scriptSourceHasher = scriptSourceHasher;
        this.cacheKeyBuilder = cacheKeyBuilder;
        cachedCompiledScripts = cacheFactory.newCache();
    }

    public <T extends Script, M> CompiledScript<T, M> getOrCompile(ScriptSource source, ClassLoader classLoader,
                                                                   ClassLoaderId classLoaderId,
                                                                   CompileOperation<M> operation,
                                                                   Class<T> scriptBaseClass,
                                                                   Action<? super ClassNode> verifier,
                                                                   ScriptClassCompiler delegate) {
        CacheKeyBuilder.CacheKeySpec cacheKeySpec = CacheKeyBuilder.CacheKeySpec
            .withPrefix("cross-build-in-memory-script-classes")
            .plus(source.getClassName())
            .plus(classLoader)
            .plus(operation.getId());
        String cacheKey = cacheKeyBuilder.build(cacheKeySpec);
        CachedCompiledScript cached = cachedCompiledScripts.get(cacheKey);
        HashCode hash = scriptSourceHasher.hash(source);
        if (cached != null) {
            if (hash.equals(cached.hash)) {
                return Cast.uncheckedCast(cached.compiledScript);
            }
        }
        CompiledScript<T, M> compiledScript = delegate.compile(source, classLoader, classLoaderId, operation, scriptBaseClass, verifier);
        cachedCompiledScripts.put(cacheKey, new CachedCompiledScript(hash, compiledScript));
        return compiledScript;
    }

    private static class CachedCompiledScript {
        private final HashCode hash;
        private final CompiledScript<?, ?> compiledScript;

        private CachedCompiledScript(HashCode hash, CompiledScript<?, ?> compiledScript) {
            this.hash = hash;
            this.compiledScript = compiledScript;
        }
    }

}
