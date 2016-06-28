<?xml version="1.0" encoding="UTF-8"?>
<<<<<<< HEAD
<xsl:stylesheet version="1.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xacml="urn:oasis:names:tc:xacml:2.0:policy:schema:os"
    xmlns:fhir="http://hl7.org/fhir">
=======
<xsl:stylesheet version="1.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xacml="urn:oasis:names:tc:xacml:2.0:policy:schema:os"
                xmlns:fhir="http://hl7.org/fhir">
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
    <xsl:output method="xml" />
    <xsl:variable name="id">
        <xsl:value-of select="fhir:Contract/fhir:id/@value"/>
    </xsl:variable>
    <xsl:template match="/">
        <xsl:apply-templates select="fhir:Contract"/>
    </xsl:template>
    <xsl:template match="fhir:Contract">
<<<<<<< HEAD
        <Policy xmlns="urn:oasis:names:tc:xacml:2.0:policy:schema:os"          
            RuleCombiningAlgId="urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:permit-overrides">   
=======
        <Policy xmlns="urn:oasis:names:tc:xacml:2.0:policy:schema:os"
                RuleCombiningAlgId="urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:permit-overrides">
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
            <xsl:attribute name="PolicyId">
                <xsl:value-of select="fhir:id/@value"/>
            </xsl:attribute>
            <Description>Consent for <xsl:value-of select="/fhir:Contract/fhir:type[1]/fhir:coding[1]/fhir:code[1]/@value"/>
            </Description>
            <Target/>
            <Rule Effect="Permit" RuleId="primary-group-rule">
                <Target>
                    <!-- Fixed  -->
                    <Resources>
                        <Resource>
                            <ResourceMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:string-equal">
                                <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">urn:oasis:names:tc:ebxml-regrep:StatusType:Approved</AttributeValue>
                                <ResourceAttributeDesignator AttributeId="xacml:status"
<<<<<<< HEAD
                                    DataType="http://www.w3.org/2001/XMLSchema#string"/>
=======
                                                             DataType="http://www.w3.org/2001/XMLSchema#string"/>
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
                            </ResourceMatch>
                        </Resource>
                    </Resources>
                    <!-- Fixed "Actions" element:  "xdsquery" and "xdsretrieve" are specific to C2S-->
                    <Actions>
                        <Action>
                            <ActionMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:string-equal">
                                <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">xdsquery</AttributeValue>
                                <ActionAttributeDesignator AttributeId="urn:oasis:names:tc:xacml:1.0:action:action-id"
<<<<<<< HEAD
                                    DataType="http://www.w3.org/2001/XMLSchema#string"/>
=======
                                                           DataType="http://www.w3.org/2001/XMLSchema#string"/>
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
                            </ActionMatch>
                        </Action>
                        <Action>
                            <ActionMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:string-equal">
                                <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">xdsretrieve</AttributeValue>
                                <ActionAttributeDesignator AttributeId="urn:oasis:names:tc:xacml:1.0:action:action-id"
<<<<<<< HEAD
                                    DataType="http://www.w3.org/2001/XMLSchema#string"/>
=======
                                                           DataType="http://www.w3.org/2001/XMLSchema#string"/>
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
                            </ActionMatch>
                        </Action>
                    </Actions>
                </Target>
                <Condition>
                    <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:and">
                        <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:or">
                            <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-equal">
                                <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-one-and-only">
                                    <SubjectAttributeDesignator MustBePresent="false"
<<<<<<< HEAD
                                        AttributeId="urn:oasis:names:tc:xacml:1.0:subject-category:intermediary-subject"
                                        DataType="http://www.w3.org/2001/XMLSchema#string"/>
=======
                                                                AttributeId="urn:oasis:names:tc:xacml:1.0:subject-category:intermediary-subject"
                                                                DataType="http://www.w3.org/2001/XMLSchema#string"/>
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
                                </Apply>
                                <!-- Authoring Provider fhir:actor[1]/fhir:entity[1]/fhir:reference[1]/@value -->
                                <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select="substring-after(fhir:actor[1]/fhir:entity[1]/fhir:reference[1]/@value,'#')"/></AttributeValue>
                            </Apply>
                        </Apply>
                        <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:or">
                            <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-equal">
                                <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-one-and-only">
                                    <SubjectAttributeDesignator MustBePresent="false"
<<<<<<< HEAD
                                        AttributeId="urn:oasis:names:tc:xacml:1.0:subject-category:recipient-subject"
                                        DataType="http://www.w3.org/2001/XMLSchema#string"/>
=======
                                                                AttributeId="urn:oasis:names:tc:xacml:1.0:subject-category:recipient-subject"
                                                                DataType="http://www.w3.org/2001/XMLSchema#string"/>
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
                                </Apply>
                                <!-- Receiving Provider - one per recipient provider -->
                                <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select="substring-after(fhir:term[1]/fhir:actor[1]/fhir:entity[1]/fhir:reference[1]/@value,'#')"/></AttributeValue>
                            </Apply>
                        </Apply>
                        <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:or">
                            <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-equal">
                                <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:string-one-and-only">
                                    <SubjectAttributeDesignator MustBePresent="false"
<<<<<<< HEAD
                                        AttributeId="urn:oasis:names:tc:xspa:1.0:subject:purposeofuse"
                                        DataType="http://www.w3.org/2001/XMLSchema#string"/>
                                </Apply>
                                <!-- Purpose of consent/intended use of the data "TREAT" for "treatment", one per consent 
=======
                                                                AttributeId="urn:oasis:names:tc:xspa:1.0:subject:purposeofuse"
                                                                DataType="http://www.w3.org/2001/XMLSchema#string"/>
                                </Apply>
                                <!-- Purpose of consent/intended use of the data "TREAT" for "treatment", one per consent
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
                                    /fhir:Contract/fhir:actionReason[1]/fhir:coding[1]/fhir:code[1]/@value
                                -->
                                <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select="fhir:actionReason[1]/fhir:coding[1]/fhir:code[1]/@value"/></AttributeValue>
                            </Apply>
                        </Apply>
                        <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:dateTime-greater-than-or-equal">
                            <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:dateTime-one-and-only">
                                <EnvironmentAttributeDesignator MustBePresent="false"
<<<<<<< HEAD
                                    AttributeId="urn:oasis:names:tc:xacml:1.0:environment:current-dateTime"
                                    DataType="http://www.w3.org/2001/XMLSchema#dateTime"/>
=======
                                                                AttributeId="urn:oasis:names:tc:xacml:1.0:environment:current-dateTime"
                                                                DataType="http://www.w3.org/2001/XMLSchema#dateTime"/>
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
                            </Apply>
                            <!-- Start date
                                /fhir:Contract/fhir:term[1]/fhir:applies[1]/fhir:start[1]/@value
                            -->
                            <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#dateTime"><xsl:value-of select="fhir:term[1]/fhir:applies[1]/fhir:start[1]/@value"/></AttributeValue>
                        </Apply>
                        <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:dateTime-less-than-or-equal">
                            <Apply FunctionId="urn:oasis:names:tc:xacml:1.0:function:dateTime-one-and-only">
                                <EnvironmentAttributeDesignator MustBePresent="false"
<<<<<<< HEAD
                                    AttributeId="urn:oasis:names:tc:xacml:1.0:environment:current-dateTime"
                                    DataType="http://www.w3.org/2001/XMLSchema#dateTime"/>
                            </Apply>
                            <!-- Expiration date. In case of revocation, this date is the date of revocation 
=======
                                                                AttributeId="urn:oasis:names:tc:xacml:1.0:environment:current-dateTime"
                                                                DataType="http://www.w3.org/2001/XMLSchema#dateTime"/>
                            </Apply>
                            <!-- Expiration date. In case of revocation, this date is the date of revocation
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
                                /fhir:Contract/fhir:term[1]/fhir:applies[1]/fhir:end[1]
                            -->
                            <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#dateTime"><xsl:value-of select="fhir:term[1]/fhir:applies[1]/fhir:end[1]/@value"/></AttributeValue>
                        </Apply>
                    </Apply>
                </Condition>
            </Rule>
<<<<<<< HEAD
            <!-- Similar to the "legal" consent, this list identifies the protected information permitted for disclosure. 
                Each type of informatiaon has a value set defining that type of information (e.g. substance abuse related, HIV related). 
                Unless listed here, protected information will be redacted. What constitutes protected information may differ from site-to-site.
                42 CFR will be applicable in all cases.                
=======
            <!-- Similar to the "legal" consent, this list identifies the protected information permitted for disclosure.
                Each type of informatiaon has a value set defining that type of information (e.g. substance abuse related, HIV related).
                Unless listed here, protected information will be redacted. What constitutes protected information may differ from site-to-site.
                42 CFR will be applicable in all cases.
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
            -->
            <xsl:choose>
                <xsl:when test="fhir:contained/fhir:List[1]/fhir:code[1]/fhir:coding[1]/fhir:code[1]/@value = 'I'">
                    <Rule Effect="Permit" RuleId="Protected-Data-To-Be-Shared"/>
                </xsl:when>
                <xsl:when test="fhir:contained/fhir:List[1]/fhir:code[1]/fhir:coding[1]/fhir:code[1]/@value = 'E'">
                    <Rule Effect="Deny" RuleId="Protected-Data-To-Be-Excluded"/>
                </xsl:when>
            </xsl:choose>
            <xsl:if test="//fhir:List">
                <Obligations>
                    <xsl:for-each select="//fhir:List[1]/fhir:entry/fhir:item/fhir:reference">
                        <xsl:variable name="itemId"><xsl:value-of select="substring-after(@value,'#')"/></xsl:variable>
                        <Obligation ObligationId="urn:samhsa:names:tc:consent2share:1.0:obligation:redact-document-section-code"
<<<<<<< HEAD
                            FulfillOn="Permit">
                            <AttributeAssignment AttributeId="urn:oasis:names:tc:xacml:3.0:example:attribute:text"
                                DataType="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select="//fhir:Basic[fhir:id/@value = $itemId]/fhir:code[1]/fhir:coding[1]/fhir:code[1]/@value"/></AttributeAssignment>
                        </Obligation>
                    </xsl:for-each>
            <xsl:for-each select="//fhir:Basic">                
                                  
            </xsl:for-each>
               </Obligations>
            </xsl:if>   
        </Policy>      
    </xsl:template>
    
</xsl:stylesheet>
=======
                                    FulfillOn="Permit">
                            <AttributeAssignment AttributeId="urn:oasis:names:tc:xacml:3.0:example:attribute:text"
                                                 DataType="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select="//fhir:Basic[fhir:id/@value = $itemId]/fhir:code[1]/fhir:coding[1]/fhir:code[1]/@value"/></AttributeAssignment>
                        </Obligation>
                    </xsl:for-each>
                    <xsl:for-each select="//fhir:Basic">

                    </xsl:for-each>
                </Obligations>
            </xsl:if>
        </Policy>
    </xsl:template>

</xsl:stylesheet>
>>>>>>> 4fe707ef1574a11df350119da10aa7daa617cf13
