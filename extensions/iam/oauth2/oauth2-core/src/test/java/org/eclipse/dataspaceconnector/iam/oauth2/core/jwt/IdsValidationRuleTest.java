/*
 *  Copyright (c) 2020 - 2022
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.oauth2.core.rule.IdsValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IdsValidationRuleTest {

    private IdsValidationRule rule;
    private JWSHeader         jwsHeader;

    @BeforeEach
    public void setUp() {
        rule = new IdsValidationRule();
        jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).build();
    }

    @Test
    void validation_succeeded() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().build();

        Map<String, Object> additional = new HashMap<>();
        additional.put("IdsValidationRule", true);
        additional.put("validateReferring", false);
        additional.put("issuerConnector", "issuerConnector");

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt, additional);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validation_failed_emptyIssuerConnector() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().build();

        Map<String, Object> additional = new HashMap<>();
        additional.put("IdsValidationRule", true);
        additional.put("validateReferring", false);

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt, additional);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages().contains("Required issuerConnector is missing in message"));
    }
}
