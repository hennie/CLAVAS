CLAVAS
======

CLARIN-NL application of OpenSKOS for three vocabularies in the domain of language resources

This distribution contains 4 components:

1. Vocabulary harvesting application
Simple web app with 3 tabs, implemented with html, javascript, jquery and jquery-ui.

2. ISO-639-3 to SKOS harvesting and conversion module
Perl cgi script.

3. ISOcat harvesting and conversion module
Perl cgi script for harvesting and conversion. Processing at the level of RDF is delegated by this
perl script to a Java program that uses the Sesame RDF API. This Java program is made available as
part of a Netbeans project.

4. Organisation Names SKOS conversion module
Implemented as a Java program that makes use of the Sesame RDF API. Included in a Netbeans project.
