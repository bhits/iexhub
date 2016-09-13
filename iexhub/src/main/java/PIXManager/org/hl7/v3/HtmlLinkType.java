
package PIXManager.org.hl7.v3;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


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
@XmlType(name = "HtmlLinkType")
@XmlEnum
public enum HtmlLinkType {

    @XmlEnumValue("alternate")
    ALTERNATE("alternate"),
    @XmlEnumValue("appendix")
    APPENDIX("appendix"),
    @XmlEnumValue("bookmark")
    BOOKMARK("bookmark"),
    @XmlEnumValue("chapter")
    CHAPTER("chapter"),
    @XmlEnumValue("contents")
    CONTENTS("contents"),
    @XmlEnumValue("copyright")
    COPYRIGHT("copyright"),
    @XmlEnumValue("glossary")
    GLOSSARY("glossary"),
    @XmlEnumValue("help")
    HELP("help"),
    @XmlEnumValue("index")
    INDEX("index"),
    @XmlEnumValue("next")
    NEXT("next"),
    @XmlEnumValue("prev")
    PREV("prev"),
    @XmlEnumValue("section")
    SECTION("section"),
    @XmlEnumValue("start")
    START("start"),
    @XmlEnumValue("stylesheet")
    STYLESHEET("stylesheet"),
    @XmlEnumValue("subsection")
    SUBSECTION("subsection");
    private final String value;

    HtmlLinkType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static HtmlLinkType fromValue(String v) {
        for (HtmlLinkType c: HtmlLinkType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
