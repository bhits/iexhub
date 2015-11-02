package com.InfoExchangeHub.Connectors;

import java.util.List;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.log4j.Logger;

import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.AdhocQueryRequest;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.AdhocQueryResponse;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.AdhocQueryType;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.LongName;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.ResponseOptionType;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.ReturnType_type0;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.SlotType1;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.ValueListType;
import com.InfoExchangeHub.Services.Client.DocumentRegistry_ServiceStub.ValueListTypeSequence;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.DocumentRequest_type0;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.RetrieveDocumentSetRequest;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.RetrieveDocumentSetRequestType;
import com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.RetrieveDocumentSetResponse;

public class XdsB
{
    /** Logger */
    public static Logger log = Logger.getLogger(XdsB.class);

	private static SOAPFactory soapFactory = OMAbstractFactory.getSOAP12Factory();
    private static DocumentRegistry_ServiceStub registryStub = null;
	private static DocumentRepository_ServiceStub repositoryStub = null;
	
	public XdsB(String registryEndpointURI,
			String repositoryEndpointURI) throws AxisFault, Exception
	{
		try
		{
			// Instantiate DocumentRegistry client stub and enable WS-Addressing...
			registryStub = new DocumentRegistry_ServiceStub(registryEndpointURI);
			registryStub._getServiceClient().engageModule("addressing");

			// Instantiate DocumentRepository client stub and enable WS-Addressing and MTOM...
			repositoryStub = new DocumentRepository_ServiceStub(repositoryEndpointURI);
			repositoryStub._getServiceClient().engageModule("addressing");
			repositoryStub._getServiceClient().getOptions().setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
			
			log.info("XdsB connector successfully initialized, registryEndpointURI="
					+ registryEndpointURI
					+ ", repositoryEndpointURI="
					+ repositoryEndpointURI);
		}
		catch (Exception e)
		{
			throw e;
		}
	}
	
	public AdhocQueryResponse RegistryStoredQuery(String patientID,
			String queryStartDate,
			String queryEndDate) throws Exception
	{
		AdhocQueryRequest request = new AdhocQueryRequest();
		AdhocQueryType adhocQuery = new AdhocQueryType();
		
//		patientID = "'086666c2fd154f7^^^&1.3.6.1.4.1.21367.2005.13.20.3000&ISO'";
		
		SlotType1 slot = new SlotType1();
		LongName name = new LongName();
		name.setLongName("$XDSDocumentEntryPatientId");
		slot.setName(name);
		ValueListType valueList = new ValueListType();
		ValueListTypeSequence[] valueListSequenceArray = new ValueListTypeSequence[1];
		ValueListTypeSequence valueListSequence = new ValueListTypeSequence(); 
		LongName valueName = new LongName();
		valueName.setLongName(patientID);
		valueListSequence.setValue(valueName);
		valueListSequenceArray[0] = valueListSequence;
		valueList.setValueListTypeSequence(valueListSequenceArray);
		slot.setValueList(valueList);
		adhocQuery.addSlot(slot);
		
		slot = new SlotType1();
		name = new LongName();
		name.setLongName("$XDSDocumentEntryStatus");
		slot.setName(name);
		valueList = new ValueListType();
		valueListSequenceArray = new ValueListTypeSequence[1];
		valueListSequence = new ValueListTypeSequence();
		valueName = new LongName();
		valueName.setLongName("('urn:oasis:names:tc:ebxml-regrep:StatusType:Approved')");
		valueListSequence.setValue(valueName);
		valueListSequenceArray[0] = valueListSequence;
		valueList.setValueListTypeSequence(valueListSequenceArray);
		slot.setValueList(valueList);
		adhocQuery.addSlot(slot);

		if (queryStartDate != null)
		{
			slot = new SlotType1();
			name = new LongName();
			name.setLongName("$XDSDocumentEntryCreationTimeFrom");
			slot.setName(name);
			valueList = new ValueListType();
			valueListSequenceArray = new ValueListTypeSequence[1];
			valueListSequence = new ValueListTypeSequence(); 
			valueName = new LongName();
			valueName.setLongName(queryStartDate);
			valueListSequence.setValue(valueName);
			valueListSequenceArray[0] = valueListSequence;
			valueList.setValueListTypeSequence(valueListSequenceArray);
			slot.setValueList(valueList);
			adhocQuery.addSlot(slot);
		}

		if (queryEndDate != null)
		{
			slot = new SlotType1();
			name = new LongName();
			name.setLongName("$XDSDocumentEntryCreationTimeTo");
			slot.setName(name);
			valueList = new ValueListType();
			valueListSequenceArray = new ValueListTypeSequence[1];
			valueListSequence = new ValueListTypeSequence(); 
			valueName = new LongName();
			valueName.setLongName(queryEndDate);
			valueListSequence.setValue(valueName);
			valueListSequenceArray[0] = valueListSequence;
			valueList.setValueListTypeSequence(valueListSequenceArray);
			slot.setValueList(valueList);
			adhocQuery.addSlot(slot);
		}
		
		try
		{
			org.apache.axis2.databinding.types.URI id = new org.apache.axis2.databinding.types.URI();
			id.setPath("urn:uuid:14d4debf-8f97-4251-9a74-a90016b0af0d");
			adhocQuery.setId(id);
			request.setAdhocQuery(adhocQuery);
			
			ResponseOptionType responseOption = new ResponseOptionType();
			responseOption.setReturnComposedObjects(true);
			responseOption.setReturnType(ReturnType_type0.LeafClass);
			request.setResponseOption(responseOption);
	
			return registryStub.documentRegistry_RegistryStoredQuery(request);
		}
		catch (Exception e)
		{
			throw e;
		}
	}
	
	public RetrieveDocumentSetResponse RetrieveDocumentSet(String repositoryUniqueIdVal,
			List<String> documentUniqueIds) throws Exception
	{
		try
		{
			RetrieveDocumentSetRequestType documentSetRequestType = new RetrieveDocumentSetRequestType();
			RetrieveDocumentSetRequest documentSetRequest = new RetrieveDocumentSetRequest();
			
			for (String documentId : documentUniqueIds)
			{
				DocumentRequest_type0 documentRequest = new DocumentRequest_type0();
				com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName repositoryUniqueId = new com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName();
				repositoryUniqueId.setLongName(repositoryUniqueIdVal);
				documentRequest.setRepositoryUniqueId(repositoryUniqueId);
				com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName documentUniqueId = new com.InfoExchangeHub.Services.Client.DocumentRepository_ServiceStub.LongName();
				documentUniqueId.setLongName(documentId);
				documentRequest.setDocumentUniqueId(documentUniqueId);
				documentSetRequestType.addDocumentRequest(documentRequest);
			}
			
			documentSetRequest.setRetrieveDocumentSetRequest(documentSetRequestType);
			
			return repositoryStub.documentRepository_RetrieveDocumentSet(documentSetRequest);
		}
		catch (Exception e)
		{
			throw e;
		}
	}
}