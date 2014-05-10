<?xml version="1.0" encoding="iso-8859-1" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" />
<xsl:template match="/"><html>
<body>
<xsl:for-each select="tables/table">
<table border="1">
<caption>
<xsl:value-of select="title"/>
</caption>
<xsl:for-each select="header/header_line">
<tr>
<xsl:for-each select="header_element">
<th bgcolor="#ccdddd" colspan="{@colspan}">
<xsl:value-of select="." /> 
</th>
</xsl:for-each>
</tr>
</xsl:for-each>
<xsl:for-each select="tbody/data_row">
<tr>
<xsl:for-each select="cell">
<td colspan="{@colspan}">
<xsl:if test="@format='bold'">
<b>
<xsl:value-of select="." />
</b>
</xsl:if>
<xsl:if test="@format='italic'">
<i>
<xsl:value-of select="." />
</i>
</xsl:if>
<xsl:if test="@format='bolditalic'">
<b><i>
<xsl:value-of select="." />
</i></b>
</xsl:if>
<xsl:if test="@format=''">
<xsl:value-of select="." />
</xsl:if>
</td>
</xsl:for-each>
</tr>
</xsl:for-each>
<BR>   </BR>
<BR>   </BR>
<BR>   </BR>
</table>
</xsl:for-each>
</body>
</html>
</xsl:template>
</xsl:stylesheet>
