<?xml version='1.0'?>

<!-- :folding=explicit:collapseFolds=1: -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'>

<xsl:import href="docbook-wrapper-html.xsl"/>

<!-- {{{ Various customizations -->

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

<xsl:template match="keycap">
  <xsl:call-template name="inline.monoseq"/>
</xsl:template>

<xsl:template match="property">
  <xsl:call-template name="inline.monoseq"/>
</xsl:template>

<xsl:param name="use.id.as.filename">1</xsl:param>

<xsl:param name="toc.list.type">ul</xsl:param>

<xsl:param name="funcsynopsis.style">ansi</xsl:param>
<!-- xsl:template match="void"><xsl:text>();</xsl:text></xsl:template -->

<xsl:param name="chunk.first.sections">1</xsl:param>

<!-- xsl:template match="*" mode="object.title.markup.textonly">
  <xsl:variable name="title">
    <xsl:apply-templates select="." mode="title.markup"/>
  </xsl:variable>
  <xsl:value-of select="$title"/>
</xsl:template -->

<!-- xsl:template name="header.navigation">
</xsl:template>

<xsl:template name="footer.navigation">
</xsl:template -->

<!-- }}} -->

<!-- {{{ Stuff for FAQ -->

<!-- <xsl:param name="generate.qandaset.toc">1</xsl:param> -->
<!-- <xsl:param name="generate.qandaset.div">1</xsl:param> -->

<!-- xsl:param name="local.l10n.xml" select="document('')"/ -->

<!-- }}} -->

<!-- {{{ Swing HTML control doesn't support &ldquo; and &rdquo; -->
<!-- i18n:i18n xmlns:i18n="http://docbook.sourceforge.net/xmlns/l10n/1.0">
<i18n:l10n language="en">

<i18n:dingbat key="startquote" text="&quot;"/>
<i18n:dingbat key="endquote" text="&quot;"/>
<i18n:dingbat key="nestedstartquote" text="&quot;"/>
<i18n:dingbat key="nestedendquote" text="&quot;"/>

<i18n:context name="section-xref">
   <i18n:template name="bridgehead" text="the section called &quot;%t&quot;"/>
   <i18n:template name="sect1" text="the section called &quot;%t&quot;"/>
   <i18n:template name="sect2" text="the section called &quot;%t&quot;"/>
   <i18n:template name="sect3" text="the section called &quot;%t&quot;"/>
   <i18n:template name="sect4" text="the section called &quot;%t&quot;"/>
   <i18n:template name="sect5" text="the section called &quot;%t&quot;"/>
   <i18n:template name="section" text="the section called &quot;%t&quot;"/>
   <i18n:template name="simplesect" text="the section called &quot;%t&quot;"/>
</i18n:context>

</i18n:l10n>
</i18n:i18n -->
<!-- }}} -->

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
