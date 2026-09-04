package com.seoulection.admin.ingredient.infrastructure.repository;

public record PropertyDefinitionView(String propertyKey, String displayNameKo, String valueType,
                                     String valueUnit, String description) { }
