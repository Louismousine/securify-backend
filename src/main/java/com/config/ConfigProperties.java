package com.config;

import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

//helper class that retrieves properties from application.properties
@Component
@PropertySource("classpath:application.properties")
public class ConfigProperties {

    @Autowired
    private Environment env;

    public String getConfigValue(String configKey){
        return env.getProperty(configKey);
    }
}
