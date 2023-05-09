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

package org.eclipse.edc.protocol.dsp.util;

import jakarta.json.Json;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.Map;
import java.util.Optional;

import static jakarta.ws.rs.core.Response.Status.BAD_GATEWAY;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NOT_IMPLEMENTED;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;


/**
 * Creates CatalogError, ContractNegotiation and TransferProcessError Responses for the Dataspace Protocol Apis
 * {DspNegotiationController}, {CatalogController} and {DspTransferProcessApiController}
 *
 */
public class ErrorUtil {

    /** Creating ErrorResponse
     *
     * @param type dspace:ContractNegotiationError/dspace:TransferProcessError/dspace:CatalogError depending on executing Controller
     * @param processId referenced processId Error is occurring
     * @param throwable describing ErrorMessage
     * @return {@link Response} ErrorResponse
     */
    public static Response createErrorResponse(String type, Optional<String> processId, Throwable throwable) {
        var builder = Json.createObjectBuilder();

        builder.add(JsonLdKeywords.TYPE, type);
        if (processId.isPresent()) {
            builder.add(DSPACE_SCHEMA + "processId", processId.get());
        }

        var code = errorCodeMapping(throwable);

        builder.add(DSPACE_SCHEMA + "code", String.valueOf(code));

        if (throwable.getMessage() != null) {
            builder.add(DSPACE_SCHEMA + "reason", Json.createArrayBuilder().add(throwable.getMessage()));
        }

        return Response.status(code).type(MediaType.APPLICATION_JSON).entity(builder.build()).build();
    }

    private static int errorCodeMapping(Throwable throwable) {
        var exceptionMap = Map.of(
                AuthenticationFailedException.class, UNAUTHORIZED,
                NotAuthorizedException.class, FORBIDDEN,
                InvalidRequestException.class, BAD_REQUEST,
                ObjectNotFoundException.class, NOT_FOUND,
                ObjectConflictException.class, CONFLICT,
                BadGatewayException.class, BAD_GATEWAY,
                UnsupportedOperationException.class, NOT_IMPLEMENTED
        );

        var status = exceptionMap.getOrDefault(throwable.getClass(), INTERNAL_SERVER_ERROR);

        return status.getStatusCode();
    }
}
