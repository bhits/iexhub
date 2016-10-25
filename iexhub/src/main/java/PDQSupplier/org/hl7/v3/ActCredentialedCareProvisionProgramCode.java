/*******************************************************************************
 * Copyright (c) 2015, 2016 Substance Abuse and Mental Health Services Administration (SAMHSA)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Eversolve, LLC - initial IExHub implementation for Health Information Exchange (HIE) integration
 *     Anthony Sute, Ioana Singureanu
 *******************************************************************************/

package PDQSupplier.org.hl7.v3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ActCredentialedCareProvisionProgramCode.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ActCredentialedCareProvisionProgramCode">
 *   &lt;restriction base="{urn:hl7-org:v3}cs">
 *     &lt;enumeration value="AAMC"/>
 *     &lt;enumeration value="AALC"/>
 *     &lt;enumeration value="ABHC"/>
 *     &lt;enumeration value="ACAC"/>
 *     &lt;enumeration value="AHOC"/>
 *     &lt;enumeration value="ACHC"/>
 *     &lt;enumeration value="ALTC"/>
 *     &lt;enumeration value="AOSC"/>
 *     &lt;enumeration value="CACS"/>
 *     &lt;enumeration value="CAMI"/>
 *     &lt;enumeration value="CAST"/>
 *     &lt;enumeration value="CBAR"/>
 *     &lt;enumeration value="CCAR"/>
 *     &lt;enumeration value="COPD"/>
 *     &lt;enumeration value="CCAD"/>
 *     &lt;enumeration value="CDEP"/>
 *     &lt;enumeration value="CDIA"/>
 *     &lt;enumeration value="CDGD"/>
 *     &lt;enumeration value="CEPI"/>
 *     &lt;enumeration value="CFEL"/>
 *     &lt;enumeration value="CHFC"/>
 *     &lt;enumeration value="CHRO"/>
 *     &lt;enumeration value="CHYP"/>
 *     &lt;enumeration value="CMIH"/>
 *     &lt;enumeration value="CMSC"/>
 *     &lt;enumeration value="CONC"/>
 *     &lt;enumeration value="CORT"/>
 *     &lt;enumeration value="COJR"/>
 *     &lt;enumeration value="CPAD"/>
 *     &lt;enumeration value="CPND"/>
 *     &lt;enumeration value="CPST"/>
 *     &lt;enumeration value="CSIC"/>
 *     &lt;enumeration value="CSLD"/>
 *     &lt;enumeration value="CSPT"/>
 *     &lt;enumeration value="CSDM"/>
 *     &lt;enumeration value="CTBU"/>
 *     &lt;enumeration value="CVDC"/>
 *     &lt;enumeration value="CWOH"/>
 *     &lt;enumeration value="CWMA"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ActCredentialedCareProvisionProgramCode")
@XmlEnum
public enum ActCredentialedCareProvisionProgramCode {

    AAMC,
    AALC,
    ABHC,
    ACAC,
    AHOC,
    ACHC,
    ALTC,
    AOSC,
    CACS,
    CAMI,
    CAST,
    CBAR,
    CCAR,
    COPD,
    CCAD,
    CDEP,
    CDIA,
    CDGD,
    CEPI,
    CFEL,
    CHFC,
    CHRO,
    CHYP,
    CMIH,
    CMSC,
    CONC,
    CORT,
    COJR,
    CPAD,
    CPND,
    CPST,
    CSIC,
    CSLD,
    CSPT,
    CSDM,
    CTBU,
    CVDC,
    CWOH,
    CWMA;

    public String value() {
        return name();
    }

    public static ActCredentialedCareProvisionProgramCode fromValue(String v) {
        return valueOf(v);
    }

}
