package com.kineticdata.bridgehub.adapter.rest;

import com.kineticdata.bridgehub.adapter.QualificationParser;

public class RestTemplateQualificationParser extends QualificationParser {
    public String encodeParameter(String name, String value) {
        return value;
    }
}
