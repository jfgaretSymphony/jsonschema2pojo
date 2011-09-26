/**
 * Copyright © 2010-2011 Nokia
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

package com.googlecode.jsonschema2pojo.integration.util;

import static org.apache.commons.io.FileUtils.*;
import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.googlecode.jsonschema2pojo.maven.Jsonschema2PojoMojo;

public class CodeGenerationHelper {

    /**
     * Invokes the jsonschema2pojo plugin to generate Java types from a given
     * schema.
     * 
     * @param schema
     *            a classpath resource to be used as the input JSON Schema
     * @param targetPackage
     *            the default target package for generated classes
     * @param generateBuilders
     *            should builder methods be generated?
     * @param usePrimitives TODO
     * @return the directory containing the generated source code
     */
    public static File generate(String schema, String targetPackage, boolean generateBuilders, boolean usePrimitives) {

        URL schemaResource = CodeGenerationHelper.class.getResource(schema);
        assertThat("Unable to read schema resource from the classpath", schemaResource, is(notNullValue()));

        return generate(schemaResource, targetPackage, generateBuilders, usePrimitives);
    }

    /**
     * Invokes the jsonschema2pojo plugin to generate Java types from a given
     * schema.
     * 
     * @param schema
     *            a URL to be used as the input JSON Schema
     * @param targetPackage
     *            the default target package for generated classes
     * @param generateBuilders
     *            should builder methods be generated?
     * @param usePrimitives TODO
     * @return the directory containing the generated source code
     */
    public static File generate(URL schema, String targetPackage, boolean generateBuilders, boolean usePrimitives) {

        File outputDirectory = createTemporaryOutputFolder();

        try {
            File sourceDirectory = new File(schema.toURI());

            Jsonschema2PojoMojo pluginMojo = new TestableJsonschema2PojoMojo().configure(
                    sourceDirectory, outputDirectory, targetPackage, generateBuilders, usePrimitives, getMockProject());

            pluginMojo.execute();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (MojoExecutionException e) {
            throw new RuntimeException(e);
        } catch (DependencyResolutionRequiredException e) {
            throw new RuntimeException(e);
        }

        return outputDirectory;
    }

    private static MavenProject getMockProject() throws DependencyResolutionRequiredException {

        MavenProject project = createNiceMock(MavenProject.class);
        expect(project.getCompileClasspathElements()).andStubReturn(new ArrayList<String>());
        replay(project);

        return project;
    }

    /**
     * Compiles the source files in a given directory.
     * 
     * @param sourceDirectory
     *            the directory containing Java source to be compiled.
     * @return a classloader which will provide access to any classes that were
     *         generated by the plugin.
     */
    public static ClassLoader compile(File sourceDirectory) {

        new Compiler().compile(sourceDirectory);

        try {
            return URLClassLoader.newInstance(new URL[] { sourceDirectory.toURI().toURL() }, Thread.currentThread().getContextClassLoader());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Invokes the jsonschema2pojo plugin then compiles the resulting source.
     * 
     * @param schema
     *            a classpath resource to be used as the input JSON Schema.
     * @param targetPackage
     *            the default target package for generated classes.
     * @param generateBuilders
     *            should builder methods be generated?
     * @param usePrimitives TODO
     * @return a classloader which will provide access to any classes that were
     *         generated by the plugin.
     */
    public static ClassLoader generateAndCompile(String schema, String targetPackage, boolean generateBuilders, boolean usePrimitives) {

        File outputDirectory = generate(schema, targetPackage, generateBuilders, usePrimitives);

        return compile(outputDirectory);

    }

    private static File createTemporaryOutputFolder() {

        String tempDirectoryName = System.getProperty("java.io.tmpdir");
        String outputDirectoryName = tempDirectoryName + File.separator + UUID.randomUUID().toString();

        final File outputDirectory = new File(outputDirectoryName);

        try {
            outputDirectory.mkdir();
        } finally {
            deleteOnExit(outputDirectory);
        }

        return outputDirectory;
    }

    /**
     * Deletes temporary output files on exit <em>recursively</em> (which is not
     * possible with {@link File#deleteOnExit}).
     * 
     * @param outputDirectory
     *            the directory to be deleted.
     */
    private static void deleteOnExit(final File outputDirectory) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    deleteDirectory(outputDirectory);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }));
    }

}
