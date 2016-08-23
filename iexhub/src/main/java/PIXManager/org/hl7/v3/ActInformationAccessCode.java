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
 * <p>Java class for ActInformationAccessCode.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ActInformationAccessCode">
 *   &lt;restriction base="{urn:hl7-org:v3}cs">
 *     &lt;enumeration value="ACADR"/>
 *     &lt;enumeration value="ACALLG"/>
 *     &lt;enumeration value="ACOBS"/>
 *     &lt;enumeration value="ACDEMO"/>
 *     &lt;enumeration value="ACIMMUN"/>
 *     &lt;enumeration value="ACLAB"/>
 *     &lt;enumeration value="ACMEDC"/>
 *     &lt;enumeration value="ACMED"/>
 *     &lt;enumeration value="ACPOLPRG"/>
 *     &lt;enumeration value="ACPSERV"/>
 *     &lt;enumeration value="ACPROV"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ActInformationAccessCode")
@XmlEnum
public enum ActInformationAccessCode {

    ACADR,
    ACALLG,
    ACOBS,
    ACDEMO,
    ACIMMUN,
    ACLAB,
    ACMEDC,
    ACMED,
    ACPOLPRG,
    ACPSERV,
    ACPROV;

    public String value() {
        return name();
    }

    public static ActInformationAccessCode fromValue(String v) {
        return valueOf(v);
    }

}
