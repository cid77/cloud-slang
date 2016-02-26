/**
 * ****************************************************************************
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 which accompany this distribution.
 * <p/>
 * The Apache License is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * *****************************************************************************
 */
package io.cloudslang.lang.cli;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.shell.Bootstrap;

import java.io.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Bonczidai Levente
 * @since 12/16/2015
 */
public class SlangBootstrap {

    private static final String USER_CONFIG_DIR = "configuration";
    private static final String USER_CONFIG_FILENAME = "cslang.properties";
    private static final String USER_CONFIG_FILEPATH = USER_CONFIG_DIR + File.separator + USER_CONFIG_FILENAME;
    private static final String SUBSTITUTION_REGEX = "\\$\\{([^${}]+)\\}"; // ${VAR}
    private static final Pattern SUBSTITUTION_PATTERN = Pattern.compile(SUBSTITUTION_REGEX);
    private static final String LOG4J_CONFIGURATION = "log4j.configuration";

    public static void main(String[] args) throws IOException {
        Properties userProperties = loadUserProperties();
        configureLogging(userProperties);
        System.out.println("Loading..");
        Bootstrap.main(args);
    }

    private static Properties loadUserProperties() throws IOException {
        String appHome = System.getProperty("app.home");
        String propertyFilePath = appHome + File.separator + USER_CONFIG_FILEPATH;
        File propertyFile = new File(propertyFilePath);
        Properties rawProperties = new Properties();
        if (propertyFile.exists()) {
            try (InputStream propertiesStream = new FileInputStream(propertyFilePath)) {
                rawProperties.load(propertiesStream);
            }
        }
        Properties processedProperties = new Properties();
        Enumeration<?> e = rawProperties.propertyNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = rawProperties.getProperty(key);
            value = substitutePropertyReferences(value);
            processedProperties.setProperty(key, value);
        }
        return processedProperties;
    }

    private static String substitutePropertyReferences(String value) {
        Matcher mather = SUBSTITUTION_PATTERN.matcher(value);
        Set<String> variableNames = new HashSet<>();
        while (mather.find()) {
            variableNames.add(mather.group(1));
        }
        for (String variableName : variableNames) {
            String variableValue = System.getProperty(variableName);
            if (StringUtils.isNotEmpty(variableValue)) {
                value = value.replace("${" + variableName + "}", variableValue);
            }
        }
        return value;
    }

    private static void configureLogging(Properties userProperties) {
        String filePath = userProperties.getProperty(LOG4J_CONFIGURATION);
        if (StringUtils.isNotEmpty(filePath)) {
            // load log4j properties from file
            PropertyConfigurator.configure(filePath);
            // log4j will not search for config files in classpath resources
            System.setProperty("log4j.configuration", filePath);
        }
    }

}
