<?xml version="1.0" ?>
<!-- :mode=xsl:tabSize=2:indentSize=2:folding=none: -->
<!-- apply to modes/catalog to look for unused rules in mode files

   Copyright © 2012 - Eric Le Lay <kerik-sf@users.sf.net>
   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
  -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.0">

    <!-- output simple one line messages for now -->
	<xsl:output method="text" encoding="UTF-8"/>
    
    <!-- main template: grab rulesets and references in all mode files
         and compute errors
      -->
	<xsl:template match="/">
    
    <!-- use the other templates to grab -->
		<xsl:variable name="refs" as="node()*">
			<xsl:apply-templates/>
		</xsl:variable>
    
    <!-- analyze the relations -->
		<xsl:for-each select="$refs/rules">
    	
    	<!-- named RULES section not referenced anywhere -->
			<xsl:if test="not($refs//ref[@name = current()/@set])
			 and not(ends-with(current()/@set, '::MAIN'))">
				<xsl:text> mode </xsl:text>
				<xsl:value-of select="parent::mode/@file"/>
				<xsl:text> ruleset </xsl:text>
				<xsl:value-of select="@set"/>
				<xsl:text> not referenced anywhere &#10;</xsl:text>
			</xsl:if>
	
			<xsl:for-each select="ref">
	    	<!-- reference to RULES that can't be found -->
				<xsl:if test="not($refs//rules[@set = current()/@name])">
					<xsl:text> mode </xsl:text>
					<xsl:value-of select="ancestor::mode/@file"/>
					<xsl:text> undefined ref </xsl:text>
					<xsl:value-of select="@name"/>
					<xsl:text>&#10;</xsl:text>
				</xsl:if>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template match="MODE[@FILE]">
		<mode file="{@FILE}" name="{@NAME}">
			<xsl:apply-templates select="document(@FILE,.)/MODE">
				<xsl:with-param name="mode-name" tunnel="yes" select="@NAME" as="xs:string"/>
			</xsl:apply-templates>
		</mode>
	</xsl:template>
	
	<xsl:template match="RULES[@SET]">
		<xsl:param name="mode-name" tunnel="yes" as="xs:string"/>
		<rules set="{$mode-name}::{@SET}">
			<xsl:apply-templates/>
		</rules>
	</xsl:template>
	
	<!-- name default RULES mode::MAIN  -->
	<xsl:template match="RULES">
		<xsl:param name="mode-name" tunnel="yes" as="xs:string"/>
		<rules set="{$mode-name}::MAIN">
			<xsl:apply-templates/>
		</rules>
	</xsl:template>
	
	<!-- the default template doesn't apply templates to attributes -->
	<xsl:template match="*">
		<xsl:apply-templates select="@*"/>
		<xsl:apply-templates select="*"/>
	</xsl:template>
	
	<!-- in general, ignore attributes and text to avoid noise in the output -->
	<xsl:template match="@*"/>
	<xsl:template match="text()"/>
	
	<!-- SPAN, IMPORT -->
	<xsl:template match="@DELEGATE">
		<xsl:param name="mode-name" tunnel="yes" as="xs:string"/>
		<xsl:choose>
			<xsl:when test="contains(.,':')">
				<ref name="{.}"/>
			</xsl:when>
			<xsl:otherwise>
				<!-- always output qualified references -->
				<ref name="{$mode-name}::{.}"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
