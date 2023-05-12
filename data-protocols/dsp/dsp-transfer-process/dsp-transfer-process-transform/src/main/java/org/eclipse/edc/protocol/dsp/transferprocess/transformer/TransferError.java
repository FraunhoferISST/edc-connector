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

package org.eclipse.edc.protocol.dsp.transferprocess.transformer;

import java.util.Optional;

public class TransferError {
    private Optional<String> processId;

    private Exception exception;

    public TransferError(Optional<String> processId, Exception exception) {
        this.processId = processId;
        this.exception = exception;
    }

    public Optional<String> getProcessId() {
        return processId;
    }

    public Exception getException() {
        return exception;
    }
}