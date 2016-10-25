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

package org.hl7.v3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PatientProfileQueryReasonCode.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="PatientProfileQueryReasonCode">
 *   &lt;restriction base="{urn:hl7-org:v3}cs">
 *     &lt;enumeration value="ADMREV"/>
 *     &lt;enumeration value="PATCAR"/>
 *     &lt;enumeration value="PATREQ"/>
 *     &lt;enumeration value="PRCREV"/>
 *     &lt;enumeration value="REGUL"/>
 *     &lt;enumeration value="RSRCH"/>
 *     &lt;enumeration value="LEGAL"/>
 *     &lt;enumeration value="VALIDATION"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "PatientProfileQueryReasonCode")
@XmlEnum
public enum PatientProfileQueryReasonCode {

    ADMREV,
    PATCAR,
    PATREQ,
    PRCREV,
    REGUL,
    RSRCH,
    LEGAL,
    VALIDATION;

    public String value() {
        return name();
    }

    public static PatientProfileQueryReasonCode fromValue(String v) {
        return valueOf(v);
    }

}
