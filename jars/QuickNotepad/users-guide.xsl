<?xml version='1.0'?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'
                xmlns="http://www.w3.org/TR/xhtml1/transitional"
                exclude-result-prefixes="#default">

<!-- NOTE: the following element must point to the location of onechunk.xsl -->
<!-- (for a single html page) or chunk.xsl (for multiple pages) in your     -->
<!-- installation of the DocBook XSL stylesheets.                           -->
<xsl:import href="docbook-wrapper.xsl"/>

<xsl:param name="use.id.as.filename" select="'1'"/>

<!-- Change these variables to '1' when using xalan -->
<xsl:param name="use.extensions" select="'0'"/>
<xsl:param name="tablecolumns.extension" select="'0'"/>

<xsl:template match="guibutton">
  <xsl:call-template name="inline.sansserifseq"/>
</xsl:template>

<xsl:template match="guiicon">
  <xsl:call-template name="inline.sansserifseq"/>
</xsl:template>

<xsl:template match="guilabel">
  <xsl:call-template name="inline.sansserifseq"/>
</xsl:template>

<xsl:template match="guimenu">
  <xsl:call-template name="inline.sansserifseq"/>
</xsl:template>

<xsl:template match="guimenuitem">
  <xsl:call-template name="inline.sansserifseq"/>
</xsl:template>

<xsl:template match="guisubmenu">
  <xsl:call-template name="inline.sansserifseq"/>
</xsl:template>

<xsl:template match="keycap">
  <xsl:call-template name="inline.sansserifseq"/>
</xsl:template>

<xsl:template match="keypress">
  <xsl:call-template name="inline.sansserifseq"/>
</xsl:template>

<xsl:template name="inline.sansserifseq">
  <xsl:param name="content">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates/>
  </xsl:param>
  <font face="Arial,Helvetica" size="-1">
  <strong><xsl:copy-of select="$content"/></strong>
  </font>
</xsl:template>

<xsl:template match="keycombo">
  <xsl:variable name="action" select="@action"/>
  <xsl:variable name="joinchar">
    <xsl:choose>
      <xsl:when test="$action='seq'"><xsl:text> </xsl:text></xsl:when>
      <xsl:when test="$action='simul'">+</xsl:when>
      <xsl:when test="$action='press'">-</xsl:when>
      <xsl:when test="$action='click'">-</xsl:when>
      <xsl:when test="$action='double-click'">-</xsl:when>
      <xsl:when test="$action='other'"></xsl:when>
      <xsl:otherwise>-</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:for-each select="./*">
    <xsl:if test="position()>1"><xsl:value-of select="$joinchar"/></xsl:if>
	<xsl:call-template name="inline.sansserifseq"/>
  </xsl:for-each>
</xsl:template>

<xsl:variable name="shade.verbatim">1</xsl:variable>

<xsl:variable name="funcsynopsis.style">ansi</xsl:variable>
<xsl:template match="void"><xsl:apply-templates/></xsl:template>

<xsl:variable name="toc.list.type">ul</xsl:variable>

<xsl:param name="local.l10n.xml" select="document('')"/>

<!-- Swing HTML control doesn't support &ldquo; and &rdquo; -->

<i18n xmlns="http://docbook.sourceforge.net/xmlns/l10n/1.0">
<l10n language="en">
<dingbat key="startquote" text="&quot;"/>
<dingbat key="endquote" text="&quot;"/>
<dingbat key="nestedstartquote" text="&apos;"/>
<dingbat key="nestedendquote" text="&apos;"/>
</l10n>
</i18n>

<xsl:template name="header.navigation">
</xsl:template>

<xsl:template name="footer.navigation">
</xsl:template>

</xsl:stylesheet>
