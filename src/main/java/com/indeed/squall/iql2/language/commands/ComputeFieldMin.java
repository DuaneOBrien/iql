package com.indeed.squall.iql2.language.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.google.common.collect.ImmutableMap;
import com.indeed.squall.iql2.language.Validator;
import com.indeed.squall.iql2.language.util.ValidationHelper;
import com.indeed.squall.iql2.language.util.ValidationUtil;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public class ComputeFieldMin implements Command, JsonSerializable {
    private final String field;
    private final Set<String> scope;

    public ComputeFieldMin(Set<String> scope, String field) {
        this.scope = scope;
        this.field = field;
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeObject(ImmutableMap.of("command", "computeFieldMin", "scope", scope, "field", field));
    }

    @Override
    public void serializeWithType(JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        this.serialize(gen, serializers);
    }

    @Override
    public void validate(ValidationHelper validationHelper, Validator validator) {
        ValidationUtil.validateField(scope, field, validationHelper, validator, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComputeFieldMin that = (ComputeFieldMin) o;
        return Objects.equals(field, that.field) &&
                Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, scope);
    }

    @Override
    public String toString() {
        return "ComputeFieldMin{" +
                "field='" + field + '\'' +
                ", scope=" + scope +
                '}';
    }
}