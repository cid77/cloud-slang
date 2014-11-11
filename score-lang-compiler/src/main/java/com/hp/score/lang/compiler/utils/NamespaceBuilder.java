package com.hp.score.lang.compiler.utils;
/*
 * Licensed to Hewlett-Packard Development Company, L.P. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/

/*
 * Created by orius123 on 05/11/14.
 */

import ch.lambdaj.Lambda;
import ch.lambdaj.function.matcher.Predicate;
import com.hp.score.lang.compiler.domain.SlangFile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

@Component
public class NamespaceBuilder {

    @Autowired
    private YamlParser yamlParser;

    public TreeMap<String, List<SlangFile>> buildNamespace(List<File> classpath, SlangFile slangFile) {
        Validate.notEmpty(classpath, "Can't build namespaces without a classpath");
        Validate.notEmpty(slangFile.getImports(), "cannot filter dependencies when there are no imports");

//        first we build a map of all the relevant files we got in the classpath sorted by their namespace
        List<File> filteredClassPath = filterNonSlangFiles(classpath);
        TreeMap<String, List<SlangFile>> namespaces = sortByNameSpace(filteredClassPath);

//        then we filter the files that their namespace was not imported
        return filterNonImportedFiles(namespaces, slangFile.getImports());
    }

    private List<File> filterNonSlangFiles(List<File> classpath) {
        String[] extensions = System.getProperty("classpath.extensions", "yaml,yml,py").split(",");
        List<File> filteredClassPath = new ArrayList<>();
        for (File file : classpath) {
            if (file.isDirectory()) {
                filteredClassPath.addAll(FileUtils.listFiles(file, extensions, true));
            } else {
                filteredClassPath.add(file);
            }
        }
        return filteredClassPath;
    }

    private TreeMap<String, List<SlangFile>> sortByNameSpace(List<File> classpath) {
        TreeMap<String, List<SlangFile>> namespaces = new TreeMap<>();
        final List<String> yamlExtensions = Arrays.asList("yaml", "yml");
        Predicate<File> isYaml = new Predicate<File>() {
            @Override
            public boolean apply(File file) {
                return yamlExtensions.contains(FilenameUtils.getExtension(file.getAbsolutePath()));
            }
        };
        Iterable<File> yamlFiles = Lambda.filter(isYaml, classpath);
//        Iterable<File> otherFiles = Lambda.filter(not(isYaml), classpath); //todo this is indented for supporting python files
        for (File yamlFile : yamlFiles) {
            SlangFile slangFile = yamlParser.loadSlangFile(yamlFile);
            List<SlangFile> existingSlangFiles = namespaces.get(slangFile.getNamespace());

            List<SlangFile> slangFilesToAdd = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(existingSlangFiles)) {
                slangFilesToAdd.addAll(existingSlangFiles);
            }

            slangFilesToAdd.add(slangFile);
            namespaces.put(slangFile.getNamespace(), slangFilesToAdd);
        }
//        for (File file : otherFiles) {
//            namespaces.put(FilenameUtils.getBaseName(file.getAbsolutePath()), file); //todo this is indented for supporting python files
//        }

        return namespaces;
    }

    private TreeMap<String, List<SlangFile>> filterNonImportedFiles(TreeMap<String, List<SlangFile>> namespaces, Map<String, String> imports) {
        Validate.notEmpty(namespaces, "File that was requested to compile has imports but no classpath was given");

        TreeMap<String, List<SlangFile>> importsFiles = new TreeMap<>();
        for (Map.Entry<String, String> anImport : imports.entrySet()) {
            importsFiles.put(anImport.getKey(), namespaces.get(anImport.getValue()));
        }

        return importsFiles;
    }


}