/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.util;

import io.jans.configapi.core.service.ConfService;
import io.jans.model.SearchRequest;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.AttributesList;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class Util {

    private static final Class<?>[] LDAP_ENTRY_PROPERTY_ANNOTATIONS = { AttributeName.class, AttributesList.class };

    @Inject
    Logger log;

    @Inject
    ConfService confService;

    public static String escapeLog(Object param) {
        if (param == null)
            return "";
        return param.toString().replaceAll("[\n\r\t]", "_");
    }

    public List<String> getTokens(String str, String tokenizer) {
        log.info(" String to get tokens - str:{}, tokenizer:{}", str, tokenizer);

        ArrayList<String> list = new ArrayList<>();
        if (StringUtils.isBlank(str)) {
            list.add("");
            return list;
        }

        log.trace("str.contains(tokenizer):{}", str.contains(tokenizer));
        if (!str.contains(tokenizer)) {

            list.add(str);
            log.trace(" Not tokenized string - list:{}", list);
            return list;
        }

        log.info("final tokenized list:{}", Collections.list(new StringTokenizer(str, tokenizer)).stream()
                .map(token -> (String) token).collect(Collectors.toList()));
        
        return Collections.list(new StringTokenizer(str, tokenizer)).stream().map(token -> (String) token)
                .collect(Collectors.toList());
    }

    public Map<String, String> getFieldValueMap(SearchRequest searchRequest, String str, String tokenizer,
            String fieldValueSeparator) {
        log.info(" Field Value to get map - searchRequest:{}, str:{], tokenizer:{} fieldValueSeparator:{}",
                searchRequest, str, tokenizer, fieldValueSeparator);

        Map<String, String> fieldValueMap = new HashMap<>();
        log.debug(" StringUtils.isBlank(str):{}, str.contains(tokenizer):{} , str.contains(fieldValueSeparator):{}",
                StringUtils.isBlank(str), str.contains(tokenizer), str.contains(fieldValueSeparator));
        if (StringUtils.isBlank(str) || !str.contains(fieldValueSeparator)) {
            return fieldValueMap;
        }

        log.trace("getTokens(str, tokenizer):{}", getTokens(str, tokenizer));

        List<String> fieldValueList = getTokens(str, tokenizer);
        log.debug("fieldValueList:{}", fieldValueList);
        
        if (fieldValueList == null || fieldValueList.isEmpty()) {
            return fieldValueMap;
        }

        for (String data : fieldValueList) {
            StringTokenizer st = new StringTokenizer(str, fieldValueSeparator);

            if (StringUtils.isNotBlank(data) && st.hasMoreTokens()) {
                String[] keyValue = data.split("=");
                log.debug("fieldValueMap:{},keyValue:{}, keyValue[0]:{}, keyValue[1]):{}", fieldValueMap, keyValue,
                        keyValue[0], keyValue[1]);
                fieldValueMap.put(keyValue[0], keyValue[1]);
            }
        }

        log.info("fieldValueMap:{}", fieldValueMap);
        searchRequest.setFieldValueMap(fieldValueMap);
        
        // Replace filedValue with the DB field name
        getAttributeData(searchRequest);
        return fieldValueMap;
    }

    public SearchRequest getAttributeData(SearchRequest searchRequest) {
        log.info("AttributeData details to be fetched with searchRequest:{} ", searchRequest);

        if (searchRequest == null
                || (searchRequest.getFieldValueMap() == null || searchRequest.getFieldValueMap().isEmpty())) {
            return searchRequest;
        }

        Class<?> theClass = searchRequest.getEntityClass();

        Map<String, List<Annotation>> propertiesAnnotations = confService.getPropertiesAnnotations(theClass,
                LDAP_ENTRY_PROPERTY_ANNOTATIONS);
        log.debug("Properties annotations fetched for theClass:{} are propertiesAnnotations:{}", theClass,
                propertiesAnnotations);

        if (propertiesAnnotations == null || propertiesAnnotations.isEmpty()) {
            return searchRequest;
        }

        Map<String, String> updatedFieldValueMap = new HashMap<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {

            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                log.debug("entry.getKey():{}, entry.getValue():{}", entry.getKey(), entry.getValue());
                String dbFieldName = getFieldDBName(entry.getKey(), propertiesAnnotations.get(entry.getKey()));
                updatedFieldValueMap.put(dbFieldName, entry.getValue());
            }
            searchRequest.setFieldValueMap(updatedFieldValueMap);
        }

        return searchRequest;
    }

    private String getFieldDBName(String fieldName, List<Annotation> annotations) {
        log.info("DB field to be fetched for fieldName:{} are annotations:{}", fieldName, annotations);
        if (StringUtils.isBlank(fieldName) || (annotations == null || annotations.isEmpty())) {
            return fieldName;
        }

        for (Annotation annotation : annotations) {
            try {
                AttributeName attributeName = (AttributeName) annotations;
                if (attributeName != null && StringUtils.isNotBlank(attributeName.name())) {
                    fieldName = attributeName.name();
                }
            } catch (ClassCastException cex) {
                log.error("ClassCastException while fetching DB fieldName for fieldName:{} is :{}", fieldName, cex);
                try {
                    AttributesList attributesList = (AttributesList) annotation;
                    if (attributesList != null && StringUtils.isNotBlank(attributesList.name())) {
                        fieldName = attributesList.name();
                    }
                } catch (Exception ex) {
                    log.error("Error while fetching AttributesList DB fieldName for fieldName:{} is :{}", fieldName,
                            ex);
                }
            } catch (Exception ex) {
                log.error("Error while fetching DB fieldName for fieldName:{} is :{}", fieldName, ex);
            }
        }
        
        log.info("Final DB field fieldName:{} ", fieldName);
        return fieldName;
    }
}
