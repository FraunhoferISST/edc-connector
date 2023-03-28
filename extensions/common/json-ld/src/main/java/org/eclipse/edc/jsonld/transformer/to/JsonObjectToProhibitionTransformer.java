/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.transformer.JsonLdNavigator.visitProperties;
import static org.eclipse.edc.jsonld.transformer.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.TransformerUtil.transformArrayOrObject;
import static org.eclipse.edc.jsonld.transformer.TransformerUtil.transformObject;

public class JsonObjectToProhibitionTransformer extends AbstractJsonLdTransformer<JsonObject, Prohibition> {
    
    private static final String ODRL_ACTION_PROPERTY = ODRL_SCHEMA + "action";
    private static final String ODRL_CONSTRAINT_PROPERTY = ODRL_SCHEMA + "constraint";
    
    public JsonObjectToProhibitionTransformer() {
        super(JsonObject.class, Prohibition.class);
    }
    
    @Override
    public @Nullable Prohibition transform(@Nullable JsonObject object, @NotNull TransformerContext context) {
        if (object == null) {
            return null;
        }
        
        var builder = Prohibition.Builder.newInstance();
        visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));
        return builder.build();
    }
    
    private void transformProperties(String key, JsonValue value, Prohibition.Builder builder, TransformerContext context) {
        if (ODRL_ACTION_PROPERTY.equals(key)) {
            transformObject(value, Action.class, builder::action, context);
        } else if (ODRL_CONSTRAINT_PROPERTY.equals(key)) {
            transformArrayOrObject(value, Constraint.class, builder::constraint, builder::constraints, context);
        }
    }
}