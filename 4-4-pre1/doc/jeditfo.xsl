<?xml version='1.0'?>

<!-- :folding=explicit:collapseFolds=1: -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'>

<xsl:import href="docbook-wrapper-fo.xsl"/>

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

<xsl:param name="funcsynopsis.style">ansi</xsl:param>

<xsl:param name="generate.toc">
book      toc
part      nop
</xsl:param>

<xsl:param name="fop.extensions" select="1"></xsl:param>

<xsl:param name="ulink.show" select="0"></xsl:param>
<xsl:param name="ulink.footnotes" select="1"></xsl:param>

<xsl:param name="alignment">left</xsl:param>
<xsl:param name="hyphenate">false</xsl:param>

</xsl:stylesheet>
