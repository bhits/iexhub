package org.iexhub.services;

import ca.uhn.fhir.jaxrs.server.AbstractJaxRsPageProvider;
import ca.uhn.fhir.rest.server.Constants;
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;
import ca.uhn.fhir.rest.server.IPagingProvider;

import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path(/*"/"*/ JaxRsConsentRestProvider.PATH)
@Stateless
@Produces({ MediaType.APPLICATION_JSON, Constants.CT_FHIR_JSON, Constants.CT_FHIR_XML })
public class JaxRsConsentPageProvider extends AbstractJaxRsPageProvider{

    public static final IPagingProvider PAGE_PROVIDER;

    static
    {
        PAGE_PROVIDER = new FifoMemoryPagingProvider(10);
    }

    @Override
    public IPagingProvider getPagingProvider()
    {
        return PAGE_PROVIDER;
    }
}
