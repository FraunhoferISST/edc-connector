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

package org.eclipse.edc.protocol.dsp.transferprocess.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.util.ErrorUtil;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.DspTransferProcessTypeNames.DSPACE_TRANSFER_PROCESS_ERROR;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_COMPLETION;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_INITIAL_REQUEST;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_START;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_SUSPENSION;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_TERMINATION;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFERPROCESS_REQUEST_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFER_COMPLETION_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFER_START_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFER_TERMINATION_TYPE;
import static org.eclipse.edc.protocol.dsp.transform.util.TypeUtil.isOfExpectedType;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

/**
 * Provides the endpoints for receiving messages regarding transfers, like initiating, completing
 * and terminating a transfer process.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path(BASE_PATH)
public class DspTransferProcessApiController {

    private final Monitor monitor;
    private final TypeTransformerRegistry registry;
    private final TransferProcessProtocolService protocolService;
    private final ObjectMapper mapper;
    private final IdentityService identityService;
    private final String dspCallbackAddress;
    private final JsonLd jsonLdService;

    public DspTransferProcessApiController(Monitor monitor, ObjectMapper mapper, TypeTransformerRegistry registry,
                                           TransferProcessProtocolService protocolService, IdentityService identityService, String dspCallbackAddress, JsonLd jsonLdService) {
        this.monitor = monitor;
        this.protocolService = protocolService;
        this.registry = registry;
        this.mapper = mapper;
        this.identityService = identityService;
        this.dspCallbackAddress = dspCallbackAddress;
        this.jsonLdService = jsonLdService;
    }

    /**
     * Retrieves an existing transfer process. This functionality is not yet supported.
     *
     * @param id the ID of the process
     * @return the transfer process in JSON-LD expanded form or TransferError
     */
    @GET
    @Path("/{id}")
    public Response getTransferProcess(@PathParam("id") String id) {
        monitor.debug(() -> format("DSP: Incoming request for transfer process with id %s", id));

        try {
            throw new UnsupportedOperationException("Getting a transfer process not yet supported.");
        } catch (Throwable throwable) {
            return ErrorUtil.createErrorResponse(DSPACE_TRANSFER_PROCESS_ERROR, Optional.of(id), throwable);
        }
    }

    /**
     * Initiates a new transfer process that has been requested by the counter-party.
     *
     * @param jsonObject the {@link TransferRequestMessage} in JSON-LD expanded form
     * @param token      the authorization header
     * @return the created transfer process  in JSON-LD expanded form or TransferProcess
     */
    @POST
    @Path(TRANSFER_INITIAL_REQUEST)
    public Response initiateTransferProcess(JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        try {
            var transferProcess = handleMessage(jsonObject, Optional.empty(), token, DSPACE_TRANSFERPROCESS_REQUEST_TYPE, TransferRequestMessage.class, protocolService::notifyRequested);

            var transferProcessJson = registry.transform(transferProcess, JsonObject.class)
                    .orElseThrow(failure -> new EdcException(format("Response could not be created: %s", failure.getFailureDetail())));

            var compacted = jsonLdService.compact(transferProcessJson);

            var result = compacted.map(jo -> mapper.convertValue(jo, Map.class))
                    .orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));

            return Response.status(Response.Status.OK).entity(result).build();
        } catch (Throwable throwable) {
            return ErrorUtil.createErrorResponse(DSPACE_TRANSFER_PROCESS_ERROR, Optional.empty(), throwable);
        }

    }

    /**
     * Notifies the connector that a transfer process has been started by the counter-part.
     *
     * @param id         the ID of the process
     * @param jsonObject the {@link TransferStartMessage} in JSON-LD expanded form
     * @param token      the authorization header
     * @return {@link Response} Empty Success Response or ErrorResponse
     */
    @POST
    @Path("{id}" + TRANSFER_START)
    public Response transferProcessStart(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        try {
            handleMessage(jsonObject, Optional.of(id), token, DSPACE_TRANSFER_START_TYPE, TransferStartMessage.class, protocolService::notifyStarted);

            return Response.status(Response.Status.OK).build();
        } catch (Throwable throwable) {
            return ErrorUtil.createErrorResponse(DSPACE_TRANSFER_PROCESS_ERROR, Optional.of(id), throwable);
        }
    }

    /**
     * Notifies the connector that a transfer process has been completed by the counter-part.
     *
     * @param id         the ID of the process
     * @param jsonObject the {@link TransferCompletionMessage} in JSON-LD expanded form
     * @param token      the authorization header
     * @return {@link Response} Empty Success Response or ErrorResponse
     */
    @POST
    @Path("{id}" + TRANSFER_COMPLETION)
    public Response transferProcessCompletion(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        try {
            handleMessage(jsonObject, Optional.of(id), token, DSPACE_TRANSFER_COMPLETION_TYPE, TransferCompletionMessage.class, protocolService::notifyCompleted);

            return Response.status(Response.Status.OK).build();
        } catch (Throwable throwable) {
            return ErrorUtil.createErrorResponse(DSPACE_TRANSFER_PROCESS_ERROR, Optional.of(id), throwable);
        }
    }

    /**
     * Notifies the connector that a transfer process has been terminated by the counter-part.
     *
     * @param id         the ID of the process
     * @param jsonObject the {@link TransferTerminationMessage} in JSON-LD expanded form
     * @param token      the authorization header
     * @return {@link Response} Empty Success Response or ErrorResponse
     */
    @POST
    @Path("{id}" + TRANSFER_TERMINATION)
    public Response transferProcessTermination(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        try {
            handleMessage(jsonObject, Optional.of(id), token, DSPACE_TRANSFER_TERMINATION_TYPE, TransferTerminationMessage.class, protocolService::notifyTerminated);

            return Response.status(Response.Status.OK).build();
        } catch (Throwable throwable) {
            return ErrorUtil.createErrorResponse(DSPACE_TRANSFER_PROCESS_ERROR, Optional.of(id), throwable);
        }
    }

    /**
     * Notifies the connector that a transfer process has been suspended by the counter-part.
     * This functionality is not yet supported.
     *
     * @param id the ID of the process
     * @return {@link Response} ErrorResponse
     */
    @POST
    @Path("{id}" + TRANSFER_SUSPENSION)
    public Response transferProcessSuspension(@PathParam("id") String id) {
        monitor.debug(() -> format("DSP: Incoming TransferSuspensionMessage for transfer process %s", id));
        try {
            throw new UnsupportedOperationException("Suspension not yet supported.");
        } catch (Throwable throwable) {
            return ErrorUtil.createErrorResponse(DSPACE_TRANSFER_PROCESS_ERROR, Optional.of(id), throwable);
        }
    }

    /**
     * Handles an incoming message. Validates the identity token. Then verifies that the JSON-LD
     * message has the expected type, before transforming it to a respective message class instance.
     * If the process ID was part of the request's path, verifies that the ID in the message matches
     * the one from the path. Then calls the service method with message and claim token. Will throw
     * an exception if any of the operations fail.
     *
     * @param request      the incoming request body
     * @param processId    the process ID path parameter, if the ID is part of the request path
     * @param token        the token from the authorization header
     * @param expectedType the expected @type property of the message
     * @param messageClass the message class to transform the request to
     * @param serviceCall  the service call to execute
     * @return the transfer process returned by the service call
     */
    private <M extends TransferRemoteMessage> TransferProcess handleMessage(JsonObject request, Optional<String> processId, String token, String expectedType,
                                                                            Class<M> messageClass, BiFunction<M, ClaimToken, ServiceResult<TransferProcess>> serviceCall) {
        processId.ifPresentOrElse(id -> monitor.debug(() -> format("DSP: Incoming %s for transfer process %s", messageClass.getSimpleName(), id)),
                () -> monitor.debug(() -> format("DSP: Incoming %s for initiating a transfer process", messageClass.getSimpleName())));

        var claimToken = checkAuthToken(token);

        var expandedJson = jsonLdService.expand(request);
        var expanded = expandedJson.map(ej -> ej).orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));
        if (!isOfExpectedType(expanded, expectedType)) {
            throw new InvalidRequestException(format("Request body was not of expected type: %s", expectedType));
        }
        var message = registry.transform(expanded, messageClass)
                .orElseThrow(failure -> new InvalidRequestException(format("Failed to read request body: %s", failure.getFailureDetail())));

        processId.ifPresent(id -> validateProcessId(message.getProcessId(), id));

        return serviceCall.apply(message, claimToken)
                .orElseThrow(exceptionMapper(TransferProcess.class));
    }

    private ClaimToken checkAuthToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();

        var result = identityService.verifyJwtToken(tokenRepresentation, dspCallbackAddress);
        if (result.failed()) {
            throw new AuthenticationFailedException();
        }

        return result.getContent();
    }

    private void validateProcessId(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new InvalidRequestException(format("Invalid process ID. Expected: %s, actual: %s", expected, actual));
        }
    }

}
