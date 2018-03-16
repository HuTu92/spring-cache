package com.github.fnpac.config.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Created by 刘春龙 on 2017/5/26.
 */
public class JacksonObjectMapper extends ObjectMapper {
    public JacksonObjectMapper() {
        this.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }
}
