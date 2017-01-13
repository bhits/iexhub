package org.iexhub.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsResourceProvider;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.AddProfileTagEnum;
import ca.uhn.fhir.rest.server.BundleInclusionRule;
import ca.uhn.fhir.rest.server.Constants;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import org.apache.log4j.Logger;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.iexhub.connectors.XdsB;
import org.iexhub.connectors.XdsBRepositoryManager;
import org.iexhub.exceptions.ContractIdParamMissingException;
import org.iexhub.exceptions.DocumentRegistryErrorException;
import org.iexhub.exceptions.InvalidDocumentTypeException;
import org.iexhub.exceptions.MultipleResourcesFoundException;
import org.iexhub.exceptions.UnexpectedServerException;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.AdhocQueryResponse;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ClassificationType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ExternalIdentifierType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.ExtrinsicObjectType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.IdentifiableType;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.RegistryError_type0;
import org.iexhub.services.client.DocumentRegistry_ServiceStub.RegistryObjectListType;
import org.iexhub.services.client.DocumentRepository_ServiceStub;
import org.iexhub.services.client.DocumentRepository_ServiceStub.DocumentResponse_type0;
import org.iexhub.services.client.DocumentRepository_ServiceStub.RetrieveDocumentSetResponse;

import javax.activation.DataHandler;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

@Local
@Path(JaxRsConsentRestProvider.PATH)
@Stateless
@Produces({ MediaType.APPLICATION_JSON, Constants.CT_FHIR_JSON, Constants.CT_FHIR_XML })
public class JaxRsConsentRestProvider extends AbstractJaxRsResourceProvider<Consent>
{
    /** Logger */
    private Logger log = Logger.getLogger(this.getClass());

    private static XdsBRepositoryManager xdsBRepositoryManager = null;
    private static XdsB xdsB = null;

    private static Properties props = null;
    private static boolean testMode = false;
    private static String propertiesFile = "/temp/IExHub.properties";
    private static String cdaToJsonTransformXslt = null;
    private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";
    private static String xdsBRepositoryUniqueId = "1.3.6.1.4.1.21367.13.40.216";
    private static String iExHubAssigningAuthority = "ISO";
    private static String xdsBRegistryEndpointURI = null;
    private static String xdsBRepositoryEndpointURI = null;
    private static FhirContext fhirCtxt = FhirContext.forDstu3();
    private static String privacyConsentClassificationType = "uuid:f0306f51-975f-434e-a61c-c59651d33983";

    /**
     * The HAPI paging provider for this server
     */
    public static final IPagingProvider PAGE_PROVIDER;
    static
    {
        PAGE_PROVIDER = JaxRsConsentPageProvider.PAGE_PROVIDER;
    }

    static final String PATH = "/Consent";

    public JaxRsConsentRestProvider() {
        super(fhirCtxt,JaxRsConsentRestProvider.class);
    }

    private void loadProperties()
    {
        try
        {
            props = new Properties();
            props.load(new FileInputStream(propertiesFile));
            JaxRsConsentRestProvider.testMode = (props.getProperty("TestMode") == null) ? JaxRsConsentRestProvider.testMode
                    : Boolean.parseBoolean(props.getProperty("TestMode"));
            JaxRsConsentRestProvider.cdaToJsonTransformXslt = (props.getProperty("CDAToJSONTransformXSLT") == null) ? JaxRsConsentRestProvider.cdaToJsonTransformXslt
                    : props.getProperty("CDAToJSONTransformXSLT");
            JaxRsConsentRestProvider.iExHubDomainOid = (props.getProperty("IExHubDomainOID") == null) ? JaxRsConsentRestProvider.iExHubDomainOid
                    : props.getProperty("IExHubDomainOID");
            JaxRsConsentRestProvider.iExHubAssigningAuthority = (props.getProperty("IExHubAssigningAuthority") == null) ? JaxRsConsentRestProvider.iExHubAssigningAuthority
                    : props.getProperty("IExHubAssigningAuthority");
            JaxRsConsentRestProvider.xdsBRepositoryEndpointURI = (props.getProperty("XdsBRepositoryEndpointURI") == null) ? JaxRsConsentRestProvider.xdsBRepositoryEndpointURI
                    : props.getProperty("XdsBRepositoryEndpointURI");
            JaxRsConsentRestProvider.xdsBRepositoryUniqueId = (props.getProperty("XdsBRepositoryUniqueId") == null) ? JaxRsConsentRestProvider.xdsBRepositoryUniqueId
                    : props.getProperty("XdsBRepositoryUniqueId");
        }
        catch (IOException e)
        {
            log.error("Error encountered loading properties file, "
                    + propertiesFile
                    + ", "
                    + e.getMessage());
            throw new UnexpectedServerException("Error encountered loading properties file, "
                    + propertiesFile
                    + ", "
                    + e.getMessage());
        }
    }

    /**
     * Consent Create
     * @param consent
     * @param theConditional
     * @return
     * @throws Exception
     */
    @Create
    public MethodOutcome create(@ResourceParam final Consent consent, @ConditionalUrlParam String theConditional) throws Exception
    {
        log.info("Entered FHIR Consent create service");

        if (props == null)
        {
            loadProperties();
        }

        MethodOutcome result = new MethodOutcome().setCreated(false);

        try
        {
            if (xdsBRepositoryManager == null)
            {
                log.info("Instantiating XdsBRepositoryManager connector...");
                xdsBRepositoryManager = new XdsBRepositoryManager(null,
                        null,
                        false);
                log.info("XdsBRepositoryManager connector successfully started");
            }
        }
        catch (Exception e)
        {
            log.error("Error encountered instantiating XdsBRepositoryManager connector, "
                    + e.getMessage());
            throw new UnexpectedServerException("Error - " + e.getMessage());
        }

        try
        {
            // Serialize document to XML...
            IParser xmlParser = fhirCtxt.newXmlParser();
            xmlParser.setPrettyPrint(true);
            String xmlContent = xmlParser.encodeResourceToString(consent);

            // ITI-41 ProvideAndRegisterDocumentSet message...
            XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType response = xdsBRepositoryManager.provideAndRegisterDocumentSet(consent,
                    xmlContent.getBytes(),
                    "text/xml");

            if ((response.getRegistryErrorList() == null) || (response.getRegistryErrorList().getRegistryError().isEmpty()))
            {
                result.setCreated(true);
                result.setId(consent.getIdElement());
                result.setResource(consent);
            }
        }
        catch (Exception e)
        {
            log.error("Error encountered, "
                    + e.getMessage());
            throw new UnexpectedServerException("Error - " + e.getMessage());
        }

        log.info("Exiting FHIR Consent create service");
        return result;
    }

    /**
     *
     * @param identifier
     * @return
     * @throws Exception
     */
    @Search
    public List<Consent> search(@RequiredParam(name = Patient.SP_IDENTIFIER) final IdType identifier) throws Exception
    {
        log.info("Entered FHIR Consent search service");

        if (props == null)
        {
            loadProperties();
        }

        List<Consent> result = null;

        try
        {
            if (xdsB == null)
            {
                log.info("Instantiating XdsB connector...");
                xdsB = new XdsB(null,
                        null,
                        false);
                log.info("XdsB connector successfully started");
            }
        }
        catch (Exception e)
        {
            log.error("Error encountered instantiating XdsB connector, "
                    + e.getMessage());
            throw new UnexpectedServerException("Error - " + e.getMessage());
        }

        try
        {
            // Determine if a complete patient ID (including OID and ISO specification) was provided.  If not, then append IExHubDomainOid and IExAssigningAuthority...
            String referencedId = identifier.getValue();
            if (!referencedId.contains("^^^&"))
            {
                referencedId = "'"
                        + referencedId
                        + "^^^&"
                        + identifier.getId()
                        + "&"
                        + iExHubAssigningAuthority
                        + "'";
            }

            AdhocQueryResponse registryResponse = xdsB.registryStoredQuery(referencedId,
                    null,
                    null
					/*"'57016-8'"*/);

            log.info("Call to XdsB registry successful");

            // Determine if registry server returned any errors...
            if ((registryResponse.getRegistryErrorList() != null) &&
                    (registryResponse.getRegistryErrorList().getRegistryError().length > 0))
            {
                for (RegistryError_type0 error : registryResponse.getRegistryErrorList().getRegistryError())
                {
                    StringBuilder errorText = new StringBuilder();
                    if (error.getErrorCode() != null)
                    {
                        errorText.append("Error code=" + error.getErrorCode() + "\n");
                    }
                    if (error.getCodeContext() != null)
                    {
                        errorText.append("Error code context=" + error.getCodeContext() + "\n");
                    }


                    if (error.getLocation() != null)
                    {
                        errorText.append("Error location=" + error.getLocation());
                    }

                    log.error(errorText.toString());
                    throw new DocumentRegistryErrorException(errorText.toString());
                }
            }

            // Try to retrieve documents...
            boolean documentTypeFound = false;
            RegistryObjectListType registryObjectList = registryResponse.getRegistryObjectList();
            IdentifiableType[] documentObjects = registryObjectList.getIdentifiable();
            if ((documentObjects != null) &&
                    (documentObjects.length > 0))
            {
                log.info("Documents found in the registry, retrieving them from the repository...");

                HashMap<String, String> documentUniqueIds = new HashMap<String, String>();
                HashMap<String, String> documentUuids = new HashMap<String, String>();
                for (IdentifiableType identifiable : documentObjects)
                {
                    if (identifiable.getClass().equals(ExtrinsicObjectType.class))
                    {
                        // Determine if this is the privacy consent document type...
                        ClassificationType[] classifications = ((ExtrinsicObjectType)identifiable).getClassification();
                        for (ClassificationType classification : classifications)
                        {
                            if (classification.getClassificationScheme().getReferenceURI().getPath().compareToIgnoreCase(privacyConsentClassificationType) == 0)
                            {
                                documentTypeFound = true;
                                break;
                            }
                        }

                        if (documentTypeFound)
                        {
                            // Determine if the "home" attribute (homeCommunityId in XCA parlance) is present...
                            String home = ((((ExtrinsicObjectType)identifiable).getHome() != null) && (((ExtrinsicObjectType)identifiable).getHome().getPath().length() > 0)) ? ((ExtrinsicObjectType)identifiable).getHome().getPath()
                                    : null;

                            ExternalIdentifierType[] externalIdentifiers = ((ExtrinsicObjectType)identifiable).getExternalIdentifier();

                            // Find the ExternalIdentifier that has the "XDSDocumentEntry.uniqueId" value...
                            String uniqueId = null;
                            for (ExternalIdentifierType externalIdentifier : externalIdentifiers)
                            {
                                String val = externalIdentifier.getName().getInternationalStringTypeSequence()[0].getLocalizedString().getValue().getFreeFormText();
                                if ((val != null) &&
                                        (val.compareToIgnoreCase("XDSDocumentEntry.uniqueId") == 0))
                                {
                                    log.info("Located XDSDocumentEntry.uniqueId ExternalIdentifier, uniqueId="
                                            + uniqueId);
                                    uniqueId = externalIdentifier.getValue().getLongName();
                                    break;
                                }
                            }

                            if (uniqueId != null)
                            {
                                documentUniqueIds.put(uniqueId,
                                        home);
                                documentUuids.put(uniqueId,
                                        ((ExtrinsicObjectType)identifiable).getId().getPath().replace("uuid:", ""));
                                log.info("Document ID added: "
                                        + uniqueId
                                        + ", homeCommunityId: "
                                        + home
                                        +", UUID: "
                                        + ((ExtrinsicObjectType)identifiable).getId().getPath().replace("uuid:", ""));
                            }
                        }
                        else
                        {
                            String home = ((identifiable.getHome() != null) && (identifiable.getHome().getPath().length() > 0)) ? identifiable.getHome().getPath()
                                    : null;
                            documentUniqueIds.put(identifiable.getId().getPath(),
                                    home);
                            log.info("Document ID added: "
                                    + identifiable.getId().getPath()
                                    + ", homeCommunityId: "
                                    + home);
                        }
                    }
                }

                if (documentTypeFound)
                {
                    log.info("Invoking XdsB repository connector retrieval...");
                    RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(xdsBRepositoryUniqueId,
                            documentUniqueIds,
                            referencedId);
                    log.info("XdsB repository connector retrieval succeeded");

                    if ((documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList() != null) &&
                            (documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList().getRegistryError().length > 0))
                    {
                        for (DocumentRepository_ServiceStub.RegistryError_type0 error : documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList().getRegistryError())
                        {
                            StringBuilder errorText = new StringBuilder();
                            if (error.getErrorCode() != null)
                            {
                                errorText.append("Error code=" + error.getErrorCode() + "\n");
                            }
                            if (error.getCodeContext() != null)
                            {
                                errorText.append("Error code context=" + error.getCodeContext() + "\n");
                            }


                            if (error.getLocation() != null)
                            {
                                errorText.append("Error location=" + error.getLocation());
                            }

                            log.error(errorText.toString());
                        }
                    }
                    else
                    {
                        result = new ArrayList<Consent>();

                        if (documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0() != null)
                        {
                            DocumentResponse_type0[] docResponseArray = documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0().getDocumentResponse();
                            if (docResponseArray != null)
                            {
                                try
                                {
                                    for (DocumentResponse_type0 document : docResponseArray)
                                    {
                                        log.info("Processing document ID="
                                                + document.getDocumentUniqueId().getLongName());

                                        String mimeType = document.getMimeType().getLongName();
                                        if (mimeType.compareToIgnoreCase("text/xml") == 0)
                                        {
                                            String filename = "test/" + document.getDocumentUniqueId().getLongName() + ".xml";
                                            log.info("Persisting document to filesystem, filename="
                                                    + filename);
                                            DataHandler dh = document.getDocument();
                                            File file = new File(filename);
                                            FileOutputStream fileOutStream = new FileOutputStream(file);
                                            dh.writeTo(fileOutStream);
                                            fileOutStream.close();

                                            IParser xmlParser = fhirCtxt.newXmlParser();
                                            Consent consent = (Consent)xmlParser.parseResource(new InputStreamReader(new FileInputStream(filename)));
                                            consent.setId(documentUuids.get(document.getDocumentUniqueId().getLongName()));
                                            result.add(consent);
                                        }
                                        else
                                        {
                                            log.info("Document retrieved is not XML - document ID="
                                                    + document.getDocumentUniqueId().getLongName());
                                        }
                                    }
                                }
                                catch (Exception e)
                                {
                                    log.error("Error encountered, "
                                            + e.getMessage());
                                    throw e;
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("Error encountered, "
                    + e.getMessage());
            throw new UnexpectedServerException("Error - " + e.getMessage());
        }

        return result;
    }

    /**
     * Consent find
     * @param id
     * @return
     * @throws Exception
     */
    @Read
    public Consent find(@IdParam final IdType id) throws Exception
    {
        if ((id == null) ||
                (id.isEmpty()))
        {
            throw new ContractIdParamMissingException("Consent ID parameter missing");
        }

        log.info(String.format("Entered FHIR Consent find (by ID) service, ID=%s",
                id.getValueAsString()));

        if (props == null)
        {
           loadProperties();
        }

        try
        {
            if (xdsB == null)
            {
                log.info("Instantiating XdsB connector...");
                xdsB = new XdsB(null,
                        null,
                        false);
                log.info("XdsB connector successfully started");
            }
        }
        catch (Exception e)
        {
            log.error("Error encountered instantiating XdsB connector, "
                    + e.getMessage());
            throw new UnexpectedServerException("Error - " + e.getMessage());
        }

        Consent result = null;
        HashMap<String, String> documentUniqueIds = new HashMap<String, String>();
        documentUniqueIds.put(id.getIdPart(),
                null);

        log.info("Invoking XdsB repository connector retrieval...");
        RetrieveDocumentSetResponse documentSetResponse = xdsB.retrieveDocumentSet(xdsBRepositoryUniqueId,
                documentUniqueIds,
                null);
        log.info("XdsB repository connector retrieval succeeded");

        if ((documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList() != null) &&
                (documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList().getRegistryError().length > 0))
        {
            for (DocumentRepository_ServiceStub.RegistryError_type0 error : documentSetResponse.getRetrieveDocumentSetResponse().getRegistryResponse().getRegistryErrorList().getRegistryError())
            {
                StringBuilder errorText = new StringBuilder();
                if (error.getErrorCode() != null)
                {
                    errorText.append("Error code=" + error.getErrorCode() + "\n");
                }
                if (error.getCodeContext() != null)
                {
                    errorText.append("Error code context=" + error.getCodeContext() + "\n");
                }

                if (error.getLocation() != null)
                {
                    errorText.append("Error location=" + error.getLocation());
                }

                log.error(errorText.toString());
            }
        }
        else
        {
            result = new Consent();

            if (documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0() != null)
            {
                DocumentResponse_type0[] docResponseArray = documentSetResponse.getRetrieveDocumentSetResponse().getRetrieveDocumentSetResponseTypeSequence_type0().getDocumentResponse();
                if (docResponseArray != null)
                {
                    try
                    {
                        if (docResponseArray.length > 1)
                        {
                            log.error(String.format("Multiple Consent resources found when only one expected during find operation, resource count=%d",
                                    docResponseArray.length));
                            throw new MultipleResourcesFoundException(String.format("Multiple Consent resources found when only one expected during find operation, resource count=%d",
                                    docResponseArray.length));
                        }

                        DocumentResponse_type0 document = docResponseArray[0];
                        log.info("Processing document ID="
                                + document.getDocumentUniqueId().getLongName());

                        String mimeType = document.getMimeType().getLongName();
                        if (mimeType.compareToIgnoreCase("text/xml") == 0)
                        {
                            String filename = "test/" + document.getDocumentUniqueId().getLongName() + ".xml";
                            log.info("Persisting document to filesystem, filename="
                                    + filename);
                            DataHandler dh = document.getDocument();
                            File file = new File(filename);
                            FileOutputStream fileOutStream = new FileOutputStream(file);
                            dh.writeTo(fileOutStream);
                            fileOutStream.close();

                            IParser xmlParser = fhirCtxt.newXmlParser();
                            result = (Consent)xmlParser.parseResource(new InputStreamReader(new FileInputStream(filename)));

                            Reference consentSubjectRef = result.getConsentor().get(0);
                            IBaseResource referencedSubject = consentSubjectRef.getResource();
                            String referencedId = referencedSubject.getIdElement().getIdPart();

                            // Need to query the registry to get the document UUID (XDSDocumentEntry. entryUUID).  This value is required for document replacement.
                            AdhocQueryResponse registryResponse = xdsB.registryStoredQuery("'"
                                            + referencedId
                                            + "^^^&"
                                            + iExHubDomainOid
                                            + "&"
                                            + iExHubAssigningAuthority
                                            + "'",
                                    null,
                                    null,
                                    null,
                                    "'" + id.getIdPart() + "'");

                            log.info("Call to XdsB registry successful");

                            // Determine if registry server returned any errors...
                            if ((registryResponse.getRegistryErrorList() != null) &&
                                    (registryResponse.getRegistryErrorList().getRegistryError().length > 0))
                            {
                                for (RegistryError_type0 error : registryResponse.getRegistryErrorList().getRegistryError())
                                {
                                    StringBuilder errorText = new StringBuilder();
                                    if (error.getErrorCode() != null)
                                    {
                                        errorText.append("Error code=" + error.getErrorCode() + "\n");
                                    }
                                    if (error.getCodeContext() != null)
                                    {
                                        errorText.append("Error code context=" + error.getCodeContext() + "\n");
                                    }

                                    if (error.getLocation() != null)
                                    {
                                        errorText.append("Error location=" + error.getLocation());
                                    }

                                    log.error(errorText.toString());
                                    throw new DocumentRegistryErrorException(errorText.toString());
                                }
                            }

                            // One document should be present...
                            boolean documentTypeFound = false;
                            RegistryObjectListType registryObjectList = registryResponse.getRegistryObjectList();
                            IdentifiableType[] documentObjects = registryObjectList.getIdentifiable();
                            if ((documentObjects != null) &&
                                    (documentObjects.length == 1))
                            {
                                log.info("Document found in the registry, retrieving XDSDocumentEntry. entryUUID...");

                                if (documentObjects[0].getClass().equals(ExtrinsicObjectType.class))
                                {
                                    // Determine if this is the privacy consent document type...
                                    ClassificationType[] classifications = ((ExtrinsicObjectType)documentObjects[0]).getClassification();
                                    for (ClassificationType classification : classifications)
                                    {
                                        if (classification.getClassificationScheme().getReferenceURI().getPath().compareToIgnoreCase(privacyConsentClassificationType) == 0)
                                        {
                                            documentTypeFound = true;
                                            break;
                                        }
                                    }

                                    if (documentTypeFound)
                                    {
                                        // Set FHIR resource ID to document UUID...
                                        result.setId(((ExtrinsicObjectType)documentObjects[0]).getId().getPath().replace("uuid:", ""));
                                        log.info("Found document UUID: "
                                                + ((ExtrinsicObjectType)documentObjects[0]).getId().getPath().replace("uuid:", ""));
                                    }
                                    else
                                    {
                                        log.error(String.format("Requested document is of invalid type, expected classification type %s",
                                                privacyConsentClassificationType));
                                        throw new InvalidDocumentTypeException(String.format("Requested document is of invalid type, expected classification type %s",
                                                privacyConsentClassificationType));
                                    }
                                }
                            }
                        }
                        else
                        {
                            log.info("Document retrieved is not XML - document ID="
                                    + document.getDocumentUniqueId().getLongName());
                        }
                    }
                    catch (Exception e)
                    {
                        log.error("Error encountered, "
                                + e.getMessage());
                        throw e;
                    }
                }
            }
        }

        log.info("Exiting FHIR Consent find (by ID) service");
        return result;
    }

    @Override
    public AddProfileTagEnum getAddProfileTag() {
        return AddProfileTagEnum.NEVER;
    }

    @Override
    public BundleInclusionRule getBundleInclusionRule() {
        return BundleInclusionRule.BASED_ON_INCLUDES;
    }

    @Override
    public ETagSupportEnum getETagSupport() {
        return ETagSupportEnum.DISABLED;
    }

    /** THE DEFAULTS */

    @Override
    public List<IServerInterceptor> getInterceptors() {
        return Collections.emptyList();
    }

    private Patient getLast(final List<Patient> list) {
        return list.get(list.size() - 1);
    }

    @Override
    public IPagingProvider getPagingProvider() {
        return PAGE_PROVIDER;
    }

    @Override
    public Class<Consent> getResourceType() {
        return Consent.class;
    }

    @Override
    public boolean isDefaultPrettyPrint() {
        return true;
    }

    @Override
    public boolean isUseBrowserFriendlyContentTypes() {
        return true;
    }

    @Update
    public MethodOutcome update(@ResourceParam final Consent consent, @ConditionalUrlParam String theConditional) throws Exception
    {
        log.info("Entered FHIR Consent update service");

        if (props == null)
        {
            loadProperties();
        }

        MethodOutcome result = new MethodOutcome().setCreated(false);

        try
        {
            if (xdsBRepositoryManager == null)
            {
                log.info("Instantiating XdsBRepositoryManager connector...");
                xdsBRepositoryManager = new XdsBRepositoryManager(null,
                        null,
                        false);
                log.info("XdsBRepositoryManager connector successfully started");
            }
        }
        catch (Exception e)
        {
            log.error("Error encountered instantiating XdsBRepositoryManager connector, "
                    + e.getMessage());
            throw new UnexpectedServerException("Error - " + e.getMessage());
        }

        try
        {
            // Serialize document to XML...
            IParser xmlParser = fhirCtxt.newXmlParser();
            xmlParser.setPrettyPrint(true);
            String xmlContent = xmlParser.encodeResourceToString(consent);

            // ITI-41 ProvideAndRegisterDocumentSet message...
            XdsBDocumentRepository.oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType response = xdsBRepositoryManager.provideAndRegisterDocumentSet(consent,
                    xmlContent.getBytes(),
                    "text/xml",
                    true);

            if ((response.getRegistryErrorList() == null) || (response.getRegistryErrorList().getRegistryError().isEmpty()))
            {
                result.setCreated(true);
                result.setId(consent.getIdElement());
                result.setResource(consent);
            }
        }
        catch (Exception e)
        {
            log.error("Error encountered, "
                    + e.getMessage());
            throw new UnexpectedServerException("Error - " + e.getMessage());
        }

        log.info("Exiting FHIR Consent create service");
        return result;
    }
}
