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
package org.gradle.groovy.scripts.internal;

import com.google.common.collect.Maps;
import groovy.lang.Script;
import org.codehaus.groovy.ast.ClassNode;
import org.gradle.api.Action;
import org.gradle.api.internal.initialization.loadercache.ClassLoaderId;
import org.gradle.cache.internal.CacheKeyBuilder;
import org.gradle.groovy.scripts.ScriptSource;
import org.gradle.internal.Cast;

import java.util.Map;

/**
 * This in-memory cache is responsible for caching compiled build scripts during a build session.
 * If the compiled script is not found in this cache, it will try to find it in the global cache,
 * which will use the delegate script class compiler in case of a miss.
 */
public class BuildScopeInMemoryCachingScriptClassCompiler implements ScriptClassCompiler {
    private final CrossBuildInMemoryCachingScriptClassCache cache;
    private final ScriptClassCompiler scriptClassCompiler;
    private final CacheKeyBuilder cacheKeyBuilder;
    private final Map<String, CompiledScript<?, ?>> cachedCompiledScripts = Maps.newHashMap();

    public BuildScopeInMemoryCachingScriptClassCompiler(CrossBuildInMemoryCachingScriptClassCache cache, CacheKeyBuilder cacheKeyBuilder, ScriptClassCompiler scriptClassCompiler) {
        this.cache = cache;
        this.cacheKeyBuilder = cacheKeyBuilder;
        this.scriptClassCompiler = scriptClassCompiler;
    }

    @Override
    public <T extends Script, M> CompiledScript<T, M> compile(ScriptSource source, ClassLoader classLoader, ClassLoaderId classLoaderId, CompileOperation<M> operation, Class<T> scriptBaseClass, Action<? super ClassNode> verifier) {
        CacheKeyBuilder.CacheKeySpec cacheKeySpec = CacheKeyBuilder.CacheKeySpec
            .withPrefix("build-scope-in-memory-script-classes")
            .plus(source.getClassName())
            .plus(classLoader)
            .plus(operation.getId());
        String cacheKey = cacheKeyBuilder.build(cacheKeySpec);
        CompiledScript<T, M> compiledScript = Cast.uncheckedCast(cachedCompiledScripts.get(cacheKey));
        if (compiledScript == null) {
            compiledScript = cache.getOrCompile(source, classLoader, classLoaderId, operation, scriptBaseClass, verifier, scriptClassCompiler);
            cachedCompiledScripts.put(cacheKey, compiledScript);
        }
        return compiledScript;
    }

}
