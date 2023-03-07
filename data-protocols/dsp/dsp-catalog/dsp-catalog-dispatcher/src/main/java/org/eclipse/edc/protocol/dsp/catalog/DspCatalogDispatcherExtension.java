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

package org.eclipse.edc.protocol.dsp.catalog;

import org.eclipse.edc.protocol.dsp.catalog.delegate.CatalogRequestDelegate;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspRemoteMessageDispatcher;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

@Extension(value = DspCatalogDispatcherExtension.NAME)
public class DspCatalogDispatcherExtension implements ServiceExtension {
    
    public static final String NAME = "Dataspace Protocol Catalog Dispatcher Extension";
    
    @Inject
    private DspRemoteMessageDispatcher messageDispatcher;
    @Inject
    private TypeManager typeManager;
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        messageDispatcher.registerDelegate(new CatalogRequestDelegate(typeManager.getMapper("json-ld")));
    }
    
}