<?xml version='1.0'?>

<!-- You should use this XSL stylesheet to create plugin documentation.

     If you want all output in a single HTML file, specify the path to
     your DocBook-XSL "html/docbook.xsl" file in the <xsl:import>
     statement below. If you want each chapter to have its own file,
     specify the path to your "html/chunk.xsl".

	 This stylesheet assumes the user's guide XML source is in a
	 subdirectory of the plugin's main dir (e.g., "docs/userguide.xml").
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'
                xmlns="http://www.w3.org/TR/xhtml1/transitional"
                exclude-result-prefixes="#default">

<xsl:import href="file:///@docs.style.sheet@" />

<!-- Swing HTML control doesn't support &ldquo; and &rdquo; -->
<xsl:template match="quote">&quot;<xsl:apply-templates/>&quot;</xsl:template>

<xsl:template match="guibutton">
  <xsl:call-template name="inline.boldseq"/>
</xsl:template>

<xsl:template match="guiicon">
  <xsl:call-template name="inline.boldseq"/>
</xsl:template>

<xsl:template match="guilabel">
  <xsl:call-template name="inline.boldseq"/>
</xsl:template>

<xsl:template match="guimenu">
  <xsl:call-template name="inline.boldseq"/>
</xsl:template>

<xsl:template match="guimenuitem">
  <xsl:call-template name="inline.boldseq"/>
</xsl:template>

<xsl:template match="guisubmenu">
  <xsl:call-template name="inline.boldseq"/>
</xsl:template>

<xsl:param name="toc.list.type">ul</xsl:param>
<xsl:param name="use.id.as.filename">1</xsl:param>
<xsl:param name="shade.verbatim">1</xsl:param>

<xsl:param name="funcsynopsis.style">ansi</xsl:param>
<xsl:template match="void"><xsl:apply-templates/></xsl:template>


<xsl:param name="chunk.first.sections">1</xsl:param>

<xsl:template match="*" mode="object.title.markup.textonly">
  <xsl:variable name="title">
    <xsl:apply-templates select="." mode="title.markup"/>
  </xsl:variable>
  <xsl:value-of select="$title"/>
</xsl:template>

<!-- {{{ TOC generation -->
<xsl:template match="/">
  <xsl:call-template name="toc"/>
</xsl:template>

<xsl:template name="toc">
  <xsl:apply-templates/>
  <xsl:call-template name="write.chunk">
    <xsl:with-param name="filename" select="'toc.xml'"/>
    <xsl:with-param name="method" select="'xml'"/>
    <xsl:with-param name="indent" select="'yes'"/>
    <xsl:with-param name="content">
      <xsl:call-template name="toc.content"/>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

<xsl:template name="toc.content">
  <TOC>
    <xsl:apply-templates select="." mode="my.toc"/>
  </TOC>
</xsl:template>

<xsl:template match="set" mode="my.toc">
  <ENTRY>
   <xsl:attribute name="HREF">
      <xsl:call-template name="href.target">
        <xsl:with-param name="object" select="."/>
      </xsl:call-template>
   </xsl:attribute>
   <TITLE>
    <xsl:apply-templates mode="title.markup" select="."/>
   </TITLE>
   <xsl:apply-templates select="book" mode="my.toc"/>
  </ENTRY>
</xsl:template>

<xsl:template match="book" mode="my.toc">
  <ENTRY>
   <xsl:attribute name="HREF">
      <xsl:call-template name="href.target">
        <xsl:with-param name="object" select="."/>
      </xsl:call-template>
   </xsl:attribute>
   <TITLE>
    <xsl:apply-templates mode="title.markup" select="."/>
   </TITLE>
   <xsl:apply-templates select="part|reference|preface|chapter|appendix|article|colophon"
                         mode="my.toc"/>
  </ENTRY>
</xsl:template>

<xsl:template match="part|reference|preface|chapter|appendix|article"
              mode="my.toc">
  <ENTRY>
   <xsl:attribute name="HREF">
      <xsl:call-template name="href.target">
        <xsl:with-param name="object" select="."/>
      </xsl:call-template>
   </xsl:attribute>
   <TITLE>
    <xsl:apply-templates mode="title.markup" select="."/>
   </TITLE>
   <xsl:apply-templates
      select="preface|chapter|appendix|refentry|section|sect1"
      mode="my.toc"/>
  </ENTRY>
</xsl:template>

<xsl:template match="section" mode="my.toc">
  <ENTRY>
   <xsl:attribute name="HREF">
      <xsl:call-template name="href.target">
        <xsl:with-param name="object" select="."/>
      </xsl:call-template>
   </xsl:attribute>
   <TITLE>
    <xsl:apply-templates mode="title.markup" select="."/>
   </TITLE>
   <xsl:apply-templates select="section" mode="my.toc"/>
  </ENTRY>
</xsl:template>

<xsl:template match="sect1" mode="my.toc">
  <ENTRY>
   <xsl:attribute name="HREF">
      <xsl:call-template name="href.target">
        <xsl:with-param name="object" select="."/>
      </xsl:call-template>
   </xsl:attribute>
   <TITLE>
    <xsl:apply-templates mode="title.markup" select="."/>
   </TITLE>
   <xsl:apply-templates select="sect2" mode="my.toc"/>
  </ENTRY>
</xsl:template>

<xsl:template match="sect2" mode="my.toc">
  <ENTRY>
   <xsl:attribute name="HREF">
      <xsl:call-template name="href.target">
        <xsl:with-param name="object" select="."/>
      </xsl:call-template>
   </xsl:attribute>
   <TITLE>
    <xsl:apply-templates mode="title.markup" select="."/>
   </TITLE>
   <xsl:apply-templates select="sect3" mode="my.toc"/>
  </ENTRY>
</xsl:template>

<xsl:template match="sect3" mode="my.toc">
  <ENTRY>
   <xsl:attribute name="HREF">
      <xsl:call-template name="href.target">
        <xsl:with-param name="object" select="."/>
      </xsl:call-template>
   </xsl:attribute>
   <TITLE>
    <xsl:apply-templates mode="title.markup" select="."/>
   </TITLE>
   <xsl:apply-templates select="sect4" mode="my.toc"/>
  </ENTRY>
</xsl:template>

<xsl:template match="sect4" mode="my.toc">
  <ENTRY>
   <xsl:attribute name="HREF">
      <xsl:call-template name="href.target">
        <xsl:with-param name="object" select="."/>
      </xsl:call-template>
   </xsl:attribute>
   <TITLE>
    <xsl:apply-templates mode="title.markup" select="."/>
   </TITLE>
   <xsl:apply-templates select="sect5" mode="my.toc"/>
  </ENTRY>
</xsl:template>

<xsl:template match="sect5|colophon" mode="my.toc">
  <ENTRY>
   <xsl:attribute name="HREF">
      <xsl:call-template name="href.target">
        <xsl:with-param name="object" select="."/>
      </xsl:call-template>
   </xsl:attribute>
   <TITLE>
    <xsl:apply-templates mode="title.markup" select="."/>
   </TITLE>
  </ENTRY>
</xsl:template>

<!-- }}} -->


</xsl:stylesheet>
