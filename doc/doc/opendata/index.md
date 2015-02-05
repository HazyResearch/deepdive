---
layout: default
---

# DeepDive Open Datasets


Over the last few years, our collaborators have used DeepDive to build over [a dozen different
applications](http://deepdive.stanford.edu). Often, these applications are interesting because of the sheer volume of documents that one can read--millions of documents or billions of Web pages. However, many researchers can't get started on these problems because NLP preprocessing is simply too expensive and painful. With the generous support of Condor CHTC, we have been fortunate to be have a ready supply of CPU hours for our own projects. The datasets below have taken over 5+ million CPU hours to produce. It’s time that we gave back to the NLP community that has given us so many good ideas for our own systems.


Our work would not be possible without open data. As a result, our group decided to enhance as many [Creative Commons
datasets](http://creativecommons.org/) as we can find. Below, we describe the data format and provide a small list of data sets in this page. We plan to process more data sets and to provide richer data as well (extracted entities and relationships). Feel free to contact us to suggest more open data sets to process. 


**Acknowledgement.** We would like to thank the HTCondor research group and the Center for High Throughput Computing (CHTC) at the University of Wisconsin-Madison, who have provided millions of machine hours to our group. Thank you, Miron Livny. We would also like to thank the Stanford Natural Language Processing Group, whose tools we use in many of our applications. DeepDive is also generously supported by [our
sponsors](http://deepdive.stanford.edu/doc/support.html).

### Data Format (NLP Markups)

The datasets we provide are in two formats, and for
most datasets, we provide data in both formats. 

  - **DeepDive-ready DB Dump.** In this format, the
  data is a database table that can be loaded directly
  into a database with PostgreSQL or Greenplum. The schema of this
  table is the same as what we used in our [walkthrough example](http://deepdive.stanford.edu/doc/basics/walkthrough/walkthrough.html)
  such that you can start building your own DeepDive applications
  immediately after download.

  - **CoNLL-format Markups.** Using DeepDive provides us
  the opportunity to better support your application and
  related technical questions, however, you do not need to 
  be tied up to DeepDive to use our datasets. We also provide
  a format that is similar to what has been used in the [CoNLL-X
  shared task](https://code.google.com/p/clearparser/wiki/DataFormat#CoNLL-X_format_(conll)). The columns of the TSV file is arranged as follows:
    `ID` `FORM` `POSTAG` `NERTAG` `LEMMA` `DEPREL` `HEAD` `SENTID` `PROV`.
  The meaning for most of these names could be found in the CoNLL
  specification. `PROV` means a set of bounding boxes in the original
  documents that corresponds to the word, which we will describe in details.

**Provenance.** For each word in our dataset, we provide its
provenance back to the original document. Depending on the
format of the original document, the provenance is provided
in _one_ of the following two formats.

  - PDF provenance: If the original document is PDF or (scanned) image, the
  provenance for a word is a set of bounding boxes. For example,
    `[p8l1329t1388r1453b1405, p8l1313t1861r1440b1892],`
  means that the corresponding word appears in page 8 of the original
  document. It has two bounding boxes because it cross two lines.
  The first bounding box has left margin 1329, top margin 1388, right margin
  1453, and bottom margin 1405. All numbers are in pixels when the image
  is converted with 300dpi.

  - Pure-text provenance: If the original document is HTML or pure text,
  the provenance for a word is a set of intervals of character offsets.
  For example, `14/15` means that the corresponding word contains
  a single character that starts at character offset 14 (include)
  and ends at character offset 15 (not include).

Each dataset is versioned with date, and with the MD5 checksum in the file name.


## PMC-OA (PubMed Central Open Access Subset)

<div class="panel panel-default" style="position:relative;">
  <div class="panel-heading">Quick Statistics & Downloads</div>
  <table class="table">
    <tr>
      <th> Pipeline </th> 
      <td colspan=3>

      <p>
      <span class="label label-info">HTML</span> 
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">STRIP (html2text)</span>
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">NLP (Stanford CoreNLP)</span>
      </p>

      </td>
    <tr>
      <th> Size </th>  <td> 70 GB </td>
      <th> Document Type </th> <td> Journal Articles </td>
    </tr>
    <tr>
      <th> # Documents </th>  <td> 359,324 </td>
      <th> # Machine Hours </th> <td> 100 K </td>
    </tr>
    <tr>
      <th> # Words </th>  <td> 2.7 Billion </td>
      <th> # Sentences </th>  <td> 110 Million </td>
    </tr>
    <tr>
      <th> Downloads </th> <td colspan="3"> 
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-expanded="false"> Download Full Corpus <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <!--<li><a href="http://i.stanford.edu/hazy/opendata/pmc/pmc_teaser_dddb_20150131_3eb5601f6acecd58527a0dc6e55d0de7.zip">DeepDive-ready DB Dump</a></li>-->
            <li><a href="http://i.stanford.edu/hazy/opendata/pmc/pmc_full_conll_20150201_bb718da04c9553592d9a11a7f20d6543.zip">CoNLL-format Markups</a></li>
          </ul>
        </div>
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-expanded="false"> Download Small Teaser <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="http://i.stanford.edu/hazy/opendata/pmc/pmc_teaser_dddb_20150131_3eb5601f6acecd58527a0dc6e55d0de7.zip">DeepDive-ready DB Dump</a></li>
            <li><a href="http://i.stanford.edu/hazy/opendata/pmc/pmc_teaser_conll_20150131_6d23f74f71cd26dbf1e9a7df52e6e404.zip">CoNLL-format Markups</a></li>
          </ul>
        </div>
      </td>
    </tr>
  </table>
</div>

PubMed Central (PMC) is a free full-text archive of biomedical and life sciences journal literature at the U.S. National Institutes of Health's National Library of Medicine (NIH/NLM). DeepDive's PMC-OA corpus contains a full snapshot
that we downloaded in March 2014 from the PubMed Central
Open Access Subset.

<i>
<img src="http://mirrors.creativecommons.org/presskit/cc.primary.srr.gif" style="width:88px;"/> PMC
applies different creative common licenses.
Information obtained at [Jan 27, 2015](http://www.ncbi.nlm.nih.gov/pmc/tools/openftlist/).
</i>


## BMC (BioMed Central)

<div class="panel panel-default" style="position:relative;">
  <div class="panel-heading">Quick Statistics & Downloads</div>
  <table class="table">
    <tr>
      <th> Pipeline </th> 
      <td colspan=3>

      <span class="label label-info">HTML</span> 
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">STRIP (html2text)</span>
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">NLP (Stanford CoreNLP)</span>

      </td>
    <tr>
      <th> Size </th>  <td> 21 GB </td>
      <th> Document Type </th> <td> Journal Articles </td>
    </tr>
    <tr>
      <th> # Documents </th>  <td> 70,043 </td>
      <th> # Machine Hours </th> <td> 20 K </td>
    </tr>
    <tr>
      <th> # Sentences </th>  <td> 15 Million </td>
      <th> # Words </th>  <td> 400 Million </td>
    </tr>
    <tr>
      <th> Downloads </th> <td colspan="3"> 
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-expanded="false"> Download Full Corpus <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="http://i.stanford.edu/hazy/opendata/bmc/bmc_full_dddb_20150201_2abed5f1d8da6000216aa21d1b121ac6.zip">DeepDive-ready DB Dump</a></li>
            <li><a href="http://i.stanford.edu/hazy/opendata/bmc/bmc_full_conll_20150201_f7536a38362cbc46460fb17a166a43c5.zip">CoNLL-format Markups</a></li>
          </ul>
        </div>
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-expanded="false"> Download Small Teaser <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="http://i.stanford.edu/hazy/opendata/bmc/bmc_teaser_dddb_20150131_ce9710530c7dfa6b541ef9f74ac9d150.zip">DeepDive-ready DB Dump</a></li>
            <li><a href="http://i.stanford.edu/hazy/opendata/bmc/bmc_teaser_conll_20150131_4e4404b9042e11d00c85407bdca1551e.zip">CoNLL-format Markups</a></li>
          </ul>
        </div>
      </td>
    </tr>
  </table>
</div>

BioMed Central is an STM (Science, Technology and Medicine) publisher of 274 peer-reviewed open access journals. We plan to have DeepDive's BMC corpus
to contain a full snapshot of BioMed Central in
Jan 2015.

<i>
<img src="http://mirrors.creativecommons.org/presskit/buttons/88x31/png/by.png" style="width:88px;"/> BioMed Central
applies CC BY 4.0 license.
Information obtained at [Jan 27, 2015](http://www.biomedcentral.com/about).
</i>

## PLOS (Public Library of Science)

<!--
<div class="panel panel-default">
  <div class="panel-heading">Quick Statistics & Download</div>
  <table class="table">
    <tr>
      <th>  </th>  <th> # Documents </th> 
      <th> # Words </th> <th> Size </th> 
      <th> Download </th>
    </tr>
    <tr>
      <th> Full Corpus </th>
      <td> 110K </td> <td> XXX </td> <td> YYY </td>
      <td> 
           <span class="label label-info">DD-ready DB Dump</span> 
           <span class="label label-success">CoNLL Markups</span>
      </td>
    </tr>
    <tr>
      <th> Small Teaser </th>
      <td> 1 </td> <td> XXX </td> <td> YYY </td>
      <td> 
           <span class="label label-info">DD-ready DB Dump</span> 
           <span class="label label-success">CoNLL Markups</span>
      </td>
    </tr>
  </table>
</div>
-->

<div class="panel panel-default" style="position:relative;">
  <div class="panel-heading">Quick Statistics & Downloads</div>
  <table class="table" style="text-align: left !important;">
    <tr>
      <th> Pipeline </th> 
      <td colspan=3>
      <p>
      <!--<span class="label label-warning">1</span>-->
      <span class="label label-info">PDF</span> 
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">OCR (Tesseract)</span>
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">NLP (Stanford CoreNLP)</span>
      </p>

      </td>
    <tr>
      <th> Size </th>  <td> 70GB </td>
      <th> Document Type </th> <td> Journal Articles </td>
    </tr>
    <tr>
      <th> # Documents </th>  <td> 125,378 </td>
      <th> # Machine Hours </th> <td> 370 K </td>
    </tr>
    <tr>
      <th> # Words </th>  <td> 1.3 Billion </td>
      <th> # Sentences </th>  <td> 73 Million </td>
    </tr>
    <tr>
      <th> Downloads </th> <td colspan="3"> 
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-expanded="false"> Download Full Corpus <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="http://i.stanford.edu/hazy/opendata/plos/plos_full_dddb_20150201_6c738b31d4a7479ac0a7d864d5f84101.zip">DeepDive-ready DB Dump</a></li>
            <li><a href="http://i.stanford.edu/hazy/opendata/plos/plos_full_conll_20150201_cafb0c11ae45095faba4fda25b1d2c9c.zip">CoNLL-format Markups</a></li>
          </ul>
        </div>
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-expanded="false"> Download Small Teaser <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="http://i.stanford.edu/hazy/opendata/plos/plos_teaser_dddb_20150126_7be79e51e200de8638256c6485e2474b.zip">DeepDive-ready DB Dump</a></li>
            <li><a href="http://i.stanford.edu/hazy/opendata/plos/plos_teaser_conll_20150126_88ff219e641e8fb300dad914c4038eca.zip">CoNLL-format Markups</a></li>
          </ul>
        </div>
      </td>
    </tr>
  </table>
</div>

 PLOS is a nonprofit open access scientific publishing project aimed at creating a library of open access journals and other scientific literature under an open content license. DeepDive's PLOS corpus contains a full snapshot that we downloaded in Aug 2014 of the following PLOS journals: (1) PLOS Biology, (2) PLOS Medicine, (3) PLOS Computational Biology, (4) PLOS Genetics, (5) PLOS Pathogens, (6) PLOS Clinical Trials, (7) PLOS ONE, (8) PLOS Neglected Tropical Diseases, and (9) PLOS Currents.

<i>
<img src="http://mirrors.creativecommons.org/presskit/buttons/88x31/png/by.png" style="width:88px;"/> PLOS applies
CC BY 3.0 license. 
Information obtained at [Jan 26, 2015](http://www.plosone.org/static/license).
</i>


## BHL (Biodiversity Heritage Library)

<div class="panel panel-default" style="position:relative;">
  <div class="panel-heading">Quick Statistics & Downloads</div>
  <table class="table">
    <tr>
      <th> Pipeline </th> 
      <td colspan=3>
      <span class="label label-info">OCR'ed Text</span> 
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">NLP (Stanford CoreNLP)</span>
      </td>
    <tr>
      <th> Size </th>  <td> 229 GB </td>
      <th> Document Type </th> <td> Books </td>
    </tr>
    <tr>
      <th> # Documents </th>  <td> 98,099 </td>
      <th> # Machine Hours </th> <td> 500 K </td>
    </tr>
    <tr>
      <th> # Sentences </th>  <td> 1 Billion </td>
      <th> # Words </th>  <td> 8.7 Billion </td>
    </tr>
    <tr>
      <th> Downloads </th> <td colspan="3"> 
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-expanded="false"> Download Full Corpus <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <!--<li><a href="http://i.stanford.edu/hazy/opendata/bhl/bhl_teaser_dddb_20150131_4a2021210453c3f9985484a24347b980.zip">DeepDive-ready DB Dump</a></li>-->
            <li><a href="http://i.stanford.edu/hazy/opendata/bhl/bhl_full_conll_20150201_78378f9a611a5d6b28575b7d88d538f8.zip">CoNLL-format Markups</a></li>
          </ul>
        </div>
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggled" data-toggle="dropdown" aria-expanded="false"> Download Small Teaser <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="http://i.stanford.edu/hazy/opendata/bhl/bhl_teaser_dddb_20150131_4a2021210453c3f9985484a24347b980.zip">DeepDive-ready DB Dump</a></li>
            <li><a href="http://i.stanford.edu/hazy/opendata/bhl/bhl_teaser_conll_20150131_30f3c9a574438b138d3a442394540cff.zip">CoNLL-format Markups</a></li>
          </ul>
        </div>
      </td>
    </tr>
  </table>
</div>


The Biodiversity Heritage Library (BHL) is a consortium of natural history and botanical libraries that cooperate to digitize and make accessible the legacy literature of biodiversity held in their collections and to make that literature available for open access and responsible use as a part of a global ''biodiversity commons.'' DeepDive's BHL corpus contains a full snapshot that we downloaded in Jan 2014 from the Biodiversity Heritage Library.

<i>
<img src="https://licensebuttons.net/l/by-nc-sa/3.0/88x31.png" style="width:88px;"/> BHL applies
CC BY-NC-SA 4.0 license. 
Information obtained at [Jan 26, 2015](http://biodivlib.wikispaces.com/Licensing+and+Copyright).
</i>



## WIKI (Wikipedia)

<div class="panel panel-default" style="position:relative;">
  <div style="position:absolute; left: -20px; top: -30px;">
   <img src="/images/coming_soon.png" style="width:200px;">
  </div>
  <div class="panel-heading">Quick Statistics & Downloads</div>
  <table class="table">
    <tr>
      <th> Pipeline </th> 
      <td colspan=3>

      <span class="label label-info">WIKI PAGE</span> 
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">STRIP</span>
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">NLP (Stanford CoreNLP)</span>

      </td>
    <tr>
      <th> Size </th>  <td> - </td>
      <th> Document Type </th> <td> Web page </td>
    </tr>
    <tr>
      <th> # Documents </th>  <td> - </td>
      <th> # Machine Hours </th> <td> - </td>
    </tr>
    <tr>
      <th> # Sentences </th>  <td> - </td>
      <th> # Words </th>  <td> - </td>
    </tr>
    <tr>
      <th> Downloads </th> <td colspan="3"> 
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle disabled" data-toggle="dropdown" aria-expanded="false"> Download Full Corpus <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="#">DeepDive-ready DB Dump</a></li>
            <li><a href="#">CoNLL-format Markups</a></li>
          </ul>
        </div>
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggled disabled" data-toggle="dropdown" aria-expanded="false"> Download Small Teaser <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="http://i.stanford.edu/hazy/opendata/bhl/bhl_teaser_dddb_20150131_4a2021210453c3f9985484a24347b980.zip">DeepDive-ready DB Dump</a></li>
            <li><a href="http://i.stanford.edu/hazy/opendata/bhl/bhl_teaser_conll_20150131_30f3c9a574438b138d3a442394540cff.zip">CoNLL-format Markups</a></li>
          </ul>
        </div>
      </td>
    </tr>
  </table>
</div>

Wikipedia is a free-access, free content Internet encyclopedia, supported and hosted by the non-profit Wikimedia Foundation. We plan to have 
DeepDive's WIKI Corpus to contain a full snapshot 
of Wikipedia in Jan 2015.

<i>
<img src="http://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-sa.png" style="width:88px;"/> Wikipedia
applies CC BY-SA 3.0 Unported license.
Information obtained at [Jan 27, 2015](http://en.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License).
</i>



## PATENT (Google Patents)

<div class="panel panel-default" style="position:relative;">
  <div style="position:absolute; left: -20px; top: -30px;">
   <img src="/images/coming_soon.png" style="width:200px;">
  </div>
  <div class="panel-heading">Quick Statistics & Downloads</div>
  <table class="table">
    <tr>
      <th> Pipeline </th> 
      <td colspan=3>

      <span class="label label-info">PDF</span> 
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">OCR (Tesseract)</span>
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">NLP (Stanford CoreNLP)</span>

      </td>
    <tr>
      <th> Size </th>  <td> - </td>
      <th> Document Type </th> <td> Government Document </td>
    </tr>
    <tr>
      <th> # Documents </th>  <td> - </td>
      <th> # Machine Hours </th> <td> - </td>
    </tr>
    <tr>
      <th> # Sentences </th>  <td> - </td>
      <th> # Words </th>  <td> - </td>
    </tr>
    <tr>
      <th> Downloads </th> <td colspan="3"> 
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle disabled" data-toggle="dropdown" aria-expanded="false"> Download Full Corpus <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="#">DeepDive-ready DB Dump</a></li>
            <li><a href="#">CoNLL-format Markups</a></li>
          </ul>
        </div>
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle disabled" data-toggle="dropdown" aria-expanded="false"> Download Small Teaser <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="#">DeepDive-ready DB Dump</a></li>
            <li><a href="#">CoNLL-format Markups</a></li>
          </ul>
        </div>
      </td>
    </tr>
  </table>
</div>

We plan to have
DeepDive's PATENT Corpus to contain a full snapshot 
of patent applications from the United States Patent and Trademark Office (USPTO), European Patent Office (EPO), and World Intellectual Property Organization (WIPO),
indexed by Google Patents in Jan 2015.

<i>
<img src="http://mirrors.creativecommons.org/presskit/buttons/88x31/png/publicdomain.png" style="width:88px;"/> Patent applications we processed belong to the public domain.
Information obtained at [Jan 27, 2015](http://en.wikipedia.org/wiki/Google_Patents).
</i>


## CCRAWL (CommonCrawl)

<div class="panel panel-default" style="position:relative;">
  <div style="position:absolute; left: -20px; top: -30px;">
   <img src="/images/coming_soon.png" style="width:200px;">
  </div>
  <div class="panel-heading">Quick Statistics & Downloads</div>
  <table class="table">
    <tr>
      <th> Pipeline </th> 
      <td colspan=3>

      <span class="label label-info">HTML</span> 
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">STRIP (html2text)</span>
      <span class="label label-warning">&gt;</span>
      <span class="label label-info">NLP (Stanford CoreNLP)</span>

      </td>
    <tr>
      <th> Size </th>  <td> - </td>
      <th> Document Type </th> <td> Government Document </td>
    </tr>
    <tr>
      <th> # Documents </th>  <td> - </td>
      <th> # Machine Hours </th> <td> - </td>
    </tr>
    <tr>
      <th> # Sentences </th>  <td> - </td>
      <th> # Words </th>  <td> - </td>
    </tr>
    <tr>
      <th> Downloads </th> <td colspan="3"> 
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle disabled" data-toggle="dropdown" aria-expanded="false"> Download Full Corpus <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="#">DeepDive-ready DB Dump</a></li>
            <li><a href="#">CoNLL-format Markups</a></li>
          </ul>
        </div>
        <div class="btn-group" text-aligh="right">
          <button type="button" class="btn btn-primary dropdown-toggle disabled" data-toggle="dropdown" aria-expanded="false"> Download Small Teaser <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li><a href="#">DeepDive-ready DB Dump</a></li>
            <li><a href="#">CoNLL-format Markups</a></li>
          </ul>
        </div>
      </td>
    </tr>
  </table>
</div>

We plan to have
DeepDive's CCRAWL Corpus to process a full
snapshot of the [Common Crawl Corpus](http://commoncrawl.org/), which is a corpus of web crawl data composed of over 5 billion web pages. 


<i>
<img src="https://commoncrawl.atlassian.net/wiki/download/attachments/655375/CRWL?version=1&modificationDate=1341953825985&api=v2" style="width:88px;"/> This data set is freely available on Amazon S3 and is released under the Common Crawl Terms of Use. 
</i>


## More Datasets Are Coming -- Stay Tuned!

We are currently working hard to bring more (10+!) datasets
available in the next couple months. In the mean time,
we'd love to hear about your applications or interesting
datasets that you have in mind. [Just let us know!](mailto:contact.hazy@gmail.com)


To cite DeepDive open datasets, you can use the following BibTeX citation:

    @misc{DeepDive:2015:OpenData,
      author       = { Christopher R\'{e} and Ce Zhang },
      title        = { {DeepDive} open datasets },
      howpublished = { \url{http://deepdive.stanford.edu/doc/opendata} },
      year         = { 2015 }
    }




