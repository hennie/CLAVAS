<?xml version="1.0" encoding="UTF-8"?>
<!-- 
  Copyright 2013 The Language Archive, Max Planck Institute for Psycholinguistics (MPIP)

  This code is part of ISOcat (www.isocat.org). 
  
  ISOcat is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 3 as
  published by the Free Software Foundation.

  ISOcat is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with ISOcat. If not, see <http://www.gnu.org/licenses/>.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0" xmlns:dcif="http://www.isocat.org/ns/dcif" xmlns:dcr="http://www.isocat.org/ns/dcr.rdf#" xmlns:isocat="http://www.isocat.org/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:skos="http://www.w3.org/2004/02/skos/core#" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" exclude-result-prefixes="dcif isocat">
    <xsl:output method="xml" encoding="utf-8" indent="yes"/>
    <xsl:param name="base"/>
    <xsl:param name="props"/>
    <xsl:variable name="NL" select="system-property('line.separator')"/>
    <xsl:variable name="indent" select="'&#160;&#160;&#160;&#160;'"/>
    <xsl:template match="text()"/>
    <xsl:key name="scheme" match="dcif:dataCategory[@type='complex'][dcif:conceptualDomain/@type='closed']" use=".//dcif:value/@pid"/>
    <xsl:template match="dcif:dataCategorySelection">
        <rdf:RDF>
            <xsl:if test="normalize-space($base)!=''">
                <xsl:attribute name="xml:base" select="concat($base,'#')"/>
            </xsl:if>
            <xsl:apply-templates/>
        </rdf:RDF>
    </xsl:template>
    <xsl:template match="dcif:dataCategory[@type='complex'][dcif:conceptualDomain/@type='closed']">
        <skos:ConceptScheme>
            <xsl:choose>
                <xsl:when test="normalize-space($base)!=''">
                    <xsl:attribute name="rdf:ID" select="concat('DC-',substring-after(@pid,'DC-'))"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="rdf:about" select="@pid"/>
                </xsl:otherwise>
            </xsl:choose>
            <dcr:datcat>
                <xsl:value-of select="@pid"/>
            </dcr:datcat>
            <dcterms:source>
                <xsl:value-of select="@pid"/>
            </dcterms:source>
            <dc:title xml:lang="en">
                <xsl:value-of select="@isocat:name"/>
            </dc:title>
            <xsl:if test="exists(dcif:descriptionSection/dcif:languageSection[dcif:language='en']/dcif:definitionSection/dcif:definition)">
                <dc:description xml:lang="en">
                    <xsl:value-of select="(dcif:descriptionSection/dcif:languageSection[dcif:language='en']/dcif:definitionSection/dcif:definition)[1]"/>
                </dc:description>
            </xsl:if>
        </skos:ConceptScheme>
    </xsl:template>
    <xsl:template match="dcif:dataCategory[@type='simple'][exists(key('scheme',@pid))]">
        <xsl:variable name="name" select="@isocat:name"/>
        <skos:Concept>
            <xsl:choose>
                <xsl:when test="exists($base) and normalize-space($base)!=''">
                    <xsl:attribute name="rdf:ID" select="concat('DC-',substring-after(@pid,'DC-'))"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="rdf:about" select="@pid"/>
                </xsl:otherwise>
            </xsl:choose>
            <dcr:datcat>
                <xsl:value-of select="@pid"/>
            </dcr:datcat>
            <dcterms:source>
                <xsl:value-of select="@pid"/>
            </dcterms:source>
            <skos:notation>
                <xsl:text>DC-</xsl:text>
                <xsl:value-of select="substring-after(@pid,'DC-')"/>
            </skos:notation>
            
            <!-- administration section:
            dcif:identifier -> skos:notation            
            dcif:version
            dcif:registrationStatus
            dcif:origin -> skos:note
            dcif:justification -> skos:note
            dcif:explanatoryComment -> skos:editorialNote
            dcif:unresolvedIssue 
            dcif:effectiveDate
            dcif:untilDate
            dcif:creation -> skos:changeNote
            dcif:lastChange -> skos:changeNote
            -->
            
            <!-- xsl:for-each to change context, but there will be only one Administration Record -->
            <xsl:for-each select="dcif:administrationInformationSection/dcif:administrationRecord">
                
                <skos:notation>
                    <xsl:value-of select="dcif:identifier"/>
                </skos:notation>
                
                <xsl:if test="normalize-space(dcif:origin)">
                    <skos:note>
                        <xsl:copy-of select="@xml:lang"/>
                        <xsl:value-of select="normalize-space(dcif:origin)"/>
                    </skos:note>
                </xsl:if>
                
                <xsl:if test="normalize-space(dcif:justification)">
                    <skos:note>
                        <xsl:copy-of select="@xml:lang"/>
                        <xsl:value-of select="normalize-space(dcif:justification)"/>
                    </skos:note>
                </xsl:if>
                
                <xsl:for-each select="dcif:explanatoryComment">
                    <skos:editorialNote>
                        <xsl:copy-of select="@xml:lang"/>
                        <xsl:value-of select="normalize-space(.)"/>
                    </skos:editorialNote>
                </xsl:for-each>
                
                <skos:changeNote>
                    <xsl:copy-of select="@xml:lang"/>
                    <xsl:value-of select="normalize-space(dcif:creation/dcif:changeDescription)"/>
                </skos:changeNote>
                
                <xsl:if test="exists(dcif:lastChange)">
                    <skos:changeNote>
                        <xsl:copy-of select="dcif:lastChange/@xml:lang"/>
                        <xsl:value-of select="normalize-space(dcif:lastChange/dcif:changeDescription)"/>
                    </skos:changeNote>
                </xsl:if>
                
            </xsl:for-each>

            <!-- description section:
            dcif:profile
            -->
            
            <!-- xsl:for-each to change context, but there will be only one Description Section -->
            <xsl:for-each select="dcif:descriptionSection">
                
                <!-- data element names:
               dcif:dataElementName -> skos:notation
               -->
                   
                <xsl:for-each select="dcif:dataElementNameSection">
                    <skos:notation>
                        <xsl:value-of select="normalize-space(dcif:dataElementName)"/>
                    </skos:notation>
                </xsl:for-each>
                
                <!-- language sections:
                dcif:name (preferred) -> skos:prefLabel
                dcif:name (standardized) -> skos:prefLabel
                dcif:name (admitted) -> skos:altLabel
                dcif:name (deprecated) -> skos:hiddenLabel
                dcif:name (superseded) -> skos:hiddenLabel
                dcif:definition -> skos:definition
                dcif:example -> skos:example
                dcif:explanation -> skos:scopeNote
                dcif:note -> skos:note
                -->
                
                <xsl:for-each select="dcif:languageSection">
                    <xsl:variable name="lang" select="dcif:language"/>
                    
                    <xsl:if test="$lang='en'">
                        <skos:prefLabel  xml:lang="{$lang}">
                            <xsl:value-of select="normalize-space($name)"/>
                        </skos:prefLabel>
                    </xsl:if>
                    
                    <xsl:for-each-group select="dcif:nameSection[not($lang='en' and dcif:name=$name)]" group-by="dcif:nameStatus">
                        <xsl:choose>
                            <xsl:when test="current-grouping-key()='preferred name'">
                                <xsl:variable name="pref" select="if ($lang='en') then () else (current-group()[1])"/>
                                <xsl:if test="exists($pref)">
                                    <skos:prefLabel xml:lang="{$lang}">
                                        <xsl:value-of select="normalize-space(dcif:name)"/>
                                    </skos:prefLabel>
                                </xsl:if>
                                <xsl:for-each select="current-group()[dcif:name ne $pref/dcif:name]">
                                    <skos:altLabel xml:lang="{$lang}">
                                        <xsl:value-of select="normalize-space(dcif:name)"/>
                                    </skos:altLabel>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:when test="current-grouping-key()='standardized name'">
                                <xsl:for-each select="current-group()">
                                    <skos:altLabel xml:lang="{$lang}">
                                        <xsl:value-of select="normalize-space(dcif:name)"/>
                                    </skos:altLabel>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:when test="current-grouping-key()='admitted name'">
                                <xsl:for-each select="current-group()">
                                    <skos:altLabel xml:lang="{$lang}">
                                        <xsl:value-of select="normalize-space(dcif:name)"/>
                                    </skos:altLabel>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:for-each select="current-group()">
                                    <skos:hiddenLabel xml:lang="{$lang}">
                                        <xsl:value-of select="normalize-space(dcif:name)"/>
                                    </skos:hiddenLabel>
                                </xsl:for-each>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each-group>
                    
                    <xsl:for-each select="dcif:definitionSection">
                        <skos:definition xml:lang="{$lang}">
                            <xsl:value-of select="normalize-space(dcif:definition)"/>
                        </skos:definition>
                    </xsl:for-each>
                    
                    <xsl:for-each select="dcif:exampleSection">
                        <skos:example xml:lang="{$lang}">
                            <xsl:value-of select="normalize-space(dcif:example)"/>
                        </skos:example>
                    </xsl:for-each>
                    
                    <xsl:for-each select="dcif:explanationSection">
                        <skos:scopeNote xml:lang="{$lang}">
                            <xsl:value-of select="normalize-space(dcif:explanation)"/>
                        </skos:scopeNote>
                    </xsl:for-each>
                    
                    <xsl:for-each select="dcif:note">
                        <skos:note xml:lang="{$lang}">
                            <xsl:value-of select="normalize-space(.)"/>
                        </skos:note>
                    </xsl:for-each>
                    
                </xsl:for-each>
                
                
            </xsl:for-each>
            
            <!-- conceptual domains:            
            isA -> skos:broader
            -->
            
            <xsl:if test="exists(dcif:isA)">
                <skos:broaderTransitive>
                    <xsl:choose>
                        <xsl:when test="normalize-space($base)!=''">
                            <xsl:attribute name="rdf:resource" select="concat($base,'#DC-',substring-after(@pid,'DC-'))"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="rdf:resource" select="@pid"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </skos:broaderTransitive>
            </xsl:if>
            
            <!-- memberships:
            is in a closed value domain -> skos:inScheme
            -->
            
            <xsl:for-each select="key('scheme',@pid)">
                <skos:inScheme>
                    <xsl:choose>
                        <xsl:when test="normalize-space($base)!=''">
                            <xsl:attribute name="rdf:resource" select="concat($base,'#DC-',substring-after(@pid,'DC-'))"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="rdf:resource" select="@pid"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </skos:inScheme>
            </xsl:for-each>
            
        </skos:Concept>
    </xsl:template>
</xsl:stylesheet>
