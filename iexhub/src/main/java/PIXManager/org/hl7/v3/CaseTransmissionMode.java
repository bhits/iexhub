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

package PIXManager.org.hl7.v3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CaseTransmissionMode.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CaseTransmissionMode">
 *   &lt;restriction base="{urn:hl7-org:v3}cs">
 *     &lt;enumeration value="AIRTRNS"/>
 *     &lt;enumeration value="ANANTRNS"/>
 *     &lt;enumeration value="ANHUMTRNS"/>
 *     &lt;enumeration value="BLDTRNS"/>
 *     &lt;enumeration value="BDYFLDTRNS"/>
 *     &lt;enumeration value="ENVTRNS"/>
 *     &lt;enumeration value="FECTRNS"/>
 *     &lt;enumeration value="FOMTRNS"/>
 *     &lt;enumeration value="FOODTRNS"/>
 *     &lt;enumeration value="HUMHUMTRNS"/>
 *     &lt;enumeration value="INDTRNS"/>
 *     &lt;enumeration value="LACTTRNS"/>
 *     &lt;enumeration value="NOSTRNS"/>
 *     &lt;enumeration value="PARTRNS"/>
 *     &lt;enumeration value="SEXTRNS"/>
 *     &lt;enumeration value="DERMTRNS"/>
 *     &lt;enumeration value="TRNSFTRNS"/>
 *     &lt;enumeration value="PLACTRNS"/>
 *     &lt;enumeration value="VECTRNS"/>
 *     &lt;enumeration value="WATTRNS"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CaseTransmissionMode")
@XmlEnum
public enum CaseTransmissionMode {

    AIRTRNS,
    ANANTRNS,
    ANHUMTRNS,
    BLDTRNS,
    BDYFLDTRNS,
    ENVTRNS,
    FECTRNS,
    FOMTRNS,
    FOODTRNS,
    HUMHUMTRNS,
    INDTRNS,
    LACTTRNS,
    NOSTRNS,
    PARTRNS,
    SEXTRNS,
    DERMTRNS,
    TRNSFTRNS,
    PLACTRNS,
    VECTRNS,
    WATTRNS;

    public String value() {
        return name();
    }

    public static CaseTransmissionMode fromValue(String v) {
        return valueOf(v);
    }

}
