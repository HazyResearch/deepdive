---
layout: default
source-url: https://docs.google.com/document/d/1ruBshA-oSoqJ0CCqOQR9DIdBWlfeA3fQs8qytmZyWPE/edit
---

## DeepDive Applications

<p class="c4"><span class="c5">Here are a list of notable public DeepDive applications.</span></p>
<ul>
<li><span class="c1"><a class="c3" href="#tac-kbp">TAC-KBP Challenge</a></span></li>
<li><span class="c1"><a class="c3" href="#wisci">Wisci(-pedia)</a></span></li>
<li><span class="c1"><a class="c3" href="#geo-paleo">Geology and Paleontology</a></span></li>
<li><span class="c1"><a class="c3" href="#memex">Memex / Human trafficking</a></span></li>
<li><span class="c1"><a class="c3" href="#genetics">Medical Genetics</a></span></li>
</ul>


<h3 class="c0">
<a name="tac-kbp"></a><span>TAC-KBP Challenge</span>
</h3>
<p class="c4"><span>TAC-KBP (Text Analysis Conference, Knowledge Base Population track, organized by NIST) is a research competition where the task is to extract common properties of people and organizations (e.g., age, birthplace, spouses, and shareholders) from a few million of newswire and web documents -- this task is also termed </span><span class="c1"><a class="c3" href="http://surdeanu.info/kbp2014/KBP2014_TaskDefinition_EnglishSlotFilling_1.1.pdf">Slot Filling</a></span><span>. In the 2014 evaluation, 31 US and international teams participated in the competition, including </span><span class="c1"><a class="c3" href="http://i.stanford.edu/hazy/papers/2014kbp-systemdescription.pdf">a solution based on DeepDive</a></span><span>&nbsp;from Stanford. The DeepDive based solution achieved </span><span class="c1"><a class="c3" href="http://nlp.cs.rpi.edu/paper/sf2014overview.pdf">the highest precision, recall, and F1</a></span><span>&nbsp;among all submissions.</span></p>
<p class="c4"><img src="../../images/showcase/tac-kbp.png"></p>


<h3 class="c0">
<a name="wisci"></a><span>Wisci(-pedia)</span>
</h3>
<p class="c4"><span>Wisci is a first incarnation of the &ldquo;</span><span class="c1"><a class="c3" href="https://www.youtube.com/watch?v=Q1IpE9_pBu4">encyclopedia built by the machines, for the people</a></span><span>&rdquo; vision. (It was developed by the Hazy research group when the team was at University of Wisconsin-Madison. Hence the project name.) We applied similar techniques as our solution to the TAC-KBP challenge -- namely </span><span class="c1"><a class="c3" href="http://www.cs.stanford.edu/people/chrismre/papers/deepdive_vlds.pdf">NLP, distant supervision, and probabilistic inference</a></span><span>&nbsp;-- over the ClueWeb09 corpus that contains 500 million web pages. The extraction and inference results include millions of common properties of people and organizations, as well as confidence scores and provenance. They are used to augment a Wikipedia mirror, where we supplement human-authored page content and infoboxes with related facts, references, excerpts, and videos discovered by DeepDive. Wisci also accepts user feedback and learns from it.</span></p>
<p class="c4"><img src="../../images/showcase/wisci.png"></p>


<h3 class="c0">
<a name="geo-paleo"></a><span>Geology and Paleontology</span>
</h3>
<p class="c4"><span class="c1"><a class="c3" href="http://en.wikipedia.org/wiki/Geology">Geology</a></span><span>&nbsp;studies history of the solid Earth; </span><span class="c1"><a class="c3" href="http://en.wikipedia.org/wiki/Paleontology">paleontology</a></span><span>&nbsp;studies</span><span>&nbsp;fossils and ancient organisms. At the core of both disciplines are discovery and knowledge sharing. </span><span>In particular, t</span><span>he research communities have maintained two live databases: </span><span class="c1"><a class="c3" href="http://macrostrat.org/about.php">Macrostrat</a></span><span>, which contains tens of thousands of rock units and their attributes, and the </span><span class="c1"><a class="c3" href="https://paleobiodb.org/#/">Paleobiology Database</a></span><span>&nbsp;(PBDB), which contains hundreds of thousands of taxonomic names and their attributes. However, both projects require researchers to laboriously sift through massive amounts of scientific publications, find relevant statements, and manually enter them into the database. For example, PBDB has taken approximately nine continuous person years to read from roughly 40K documents in the past two decades.</span></p>
<p class="c4"><span>In collaboration with </span><span class="c1"><a class="c3" href="http://geoscience.wisc.edu/geoscience/people/faculty/shanan-peters/">Prof. Shanan Peters</a></span><span>&nbsp;at UW-Madison, we developed two DeepDive programs, </span><span class="c1"><a class="c3" href="http://www.cs.stanford.edu/people/chrismre/papers/gdd_demo.pdf">GeoDeepDive</a></span><span>&nbsp;and </span><span class="c1"><a class="c3" href="http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0113523">PaleoDeepDive</a></span><span>, that process roughly 300K scientific documents (including text, tables, and figures). On the document set covered by both DeepDive and PBDB contributors (12K), DeepDive achieves recall roughly 2.5X that of humans, and precision that is as high as or higher than humans.</span></p>
<p><img src="../../images/showcase/paleontology.jpg"></p>


<h3 class="c0">
<a name="memex"></a><span>Memex / Human trafficking</span>
</h3>
<p class="c4"><span class="c1"><a class="c3" href="http://www.darpa.mil/newsevents/releases/2014/02/09.aspx">Memex</a></span><span>&nbsp;is a DARPA program to explore next-generation domain-specific information retrieval systems. As an ongoing application, the federal government has been using the Memex technology to </span><span class="c1"><a class="c3" href="http://www.cbsnews.com/news/new-search-engine-exposes-the-dark-web/">fight human trafficking</a></span><span>. For this application, the input is a portion of the dark web where human traffickers are likely to (surreptitiously) post supply and demand information about illegal labor, involuntary sex workers, etc. DeepDive would process the web documents to extract evidential data such as names, addresses, phone numbers, job types, job requirements, etc. Together with provenance information, such evidential data are then passed on to other collaborators on the Memex program as well as law enforcement for structured analysis and consumption by operational applications. This application has been </span><span class="c1"><a class="c3" href="http://www.scientificamerican.com/article/human-traffickers-caught-on-hidden-internet/">featured</a></span><span>&nbsp;</span><span class="c1"><a class="c3" href="http://www.wsj.com/articles/sleuthing-search-engine-even-better-than-google-1423703464">extensively</a></span><span>&nbsp;</span><span class="c1"><a class="c3" href="http://www.wired.com/2015/02/darpa-memex-dark-web/">in</a></span><span>&nbsp;</span><span class="c1"><a class="c3" href="http://www.bbc.com/news/technology-31808104">the</a></span><span>&nbsp;</span><span class="c1"><a class="c3" href="http://www.defenseone.com/technology/2015/02/darpas-new-search-engine-puts-google-dust/105342">media</a></span><span>. It is supporting </span><span class="c1"><a class="c3" href="http://www.defenseone.com/technology/2015/02/darpas-new-search-engine-puts-google-dust/105342/">actual investigations</a></span><span>. [Link to </span><span class="c1"><a class="c3" href="memex.html">Mike&rsquo;s article</a></span><span>.]</span></p>
<p class="c4"><img src="../../images/showcase/memex-human-trafficking.jpg"></p>


<h3 class="c0">
<a name="genetics"></a><span>Medical Genetics</span>
</h3>
<p class="c4"><span>The body of literature in life sciences has been </span><span class="c1"><a class="c3" href="http://www.nlm.nih.gov/bsd/index_stats_comp.html">growing at accelerating speeds</a></span><span>&nbsp;to the extent that it has been unrealistic for scientists to perform research solely based on reading and memorization (even with the help of keyword search)</span><span>. As a result, there have been numerous initiatives to build structured knowledge bases from literature. For example, </span><span class="c1"><a class="c3" href="http://omim.org/">OMIM</a></span><span>&nbsp;is an authoritative database of human genes and genetic disorders. It dates back to the 1960s, and so far contains about 6,000 hereditary diseases or phenotypes. Because OMIM is curated by humans, it has been growing at a rate of </span><span class="c1"><a class="c3" href="http://omim.org/statistics/update">roughly 50 records / month for many years</a></span><span>. In collaboration with </span><span class="c1"><a class="c3" href="http://bejerano.stanford.edu/pi.html">Prof. Gill Bejerano</a></span><span>&nbsp;at Stanford, we are developing DeepDive applications in the field of </span><span class="c1"><a class="c3" href="http://en.wikipedia.org/wiki/Medical_genetics">medical genetics</a></span><span>. Specifically, we use DeepDive to extract mentions of genes, diseases, and phenotypes from the literature, and statistically infer their relationships.</span></p>
<p class="c4"><img src="../../images/showcase/genetics.jpg"></p>

