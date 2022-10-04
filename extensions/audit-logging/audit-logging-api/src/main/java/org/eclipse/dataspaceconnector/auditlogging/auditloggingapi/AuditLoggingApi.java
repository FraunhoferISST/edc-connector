/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.dataspaceconnector.auditlogging.auditloggingapi;

import jakarta.validation.Valid;
import jakarta.ws.rs.container.AsyncResponse;
import org.eclipse.dataspaceconnector.auditlogging.auditloggingapi.model.LoggingBody;

public interface AuditLoggingApi {
    void getLogs(AsyncResponse response);

    String createNewLog(@Valid LoggingBody body);
}
