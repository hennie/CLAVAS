#!/usr/bin/perl -w
use LWP::Simple;
use utf8;
use open ':encoding(utf8)';

my $skos_file = "";
open($skos_file, "< ./dist/OpenSKOSOrgNames.rdf") or die("Could not open OpenSKOSOrgNames.rdf", $!);

$content = do { local $/; <$skos_file> };

# add prefixes to properties (all skos properties covered)
$content =~ s/<datcat/<dcr:datcat/g;
$content =~ s/<source/<dcterms:source/g;
$content =~ s/<title/<dcterms:title/g;
$content =~ s/<description/<dcterms:description/g;
$content =~ s/<hasTopConcept/<skos:hasTopConcept/g;
$content =~ s/<topConceptOf/<skos:topConceptOf/g;
$content =~ s/<member/<skos:member/g;
$content =~ s/<prefLabel/<skos:prefLabel/g;
$content =~ s/<altLabel/<skos:altLabel/g;
$content =~ s/<hiddenLabel/<skos:hiddenLabel/g;
$content =~ s/<notation/<skos:notation/g;
$content =~ s/<note/<skos:note/g;
$content =~ s/<changeNote/<skos:changeNote/g;
$content =~ s/<definition/<skos:definition/g;
$content =~ s/<example/<skos:example/g;
$content =~ s/<scopeNote/<skos:scopeNote/g;
$content =~ s/<editorialNote/<skos:editorialNote/g;
$content =~ s/<historyNote/<skos:historyNote/g;
$content =~ s/<inScheme/<skos:inScheme/g;
$content =~ s/<broader/<skos:broader/g;
$content =~ s/<narrower/<skos:narrower/g;
$content =~ s/<related/<skos:related/g;
$content =~ s/<broaderTransitive/<skos:broaderTransitive/g;
$content =~ s/<narrowerTransitive/<skos:narrowerTransitive/g;
$content =~ s/<broadMatch/<skos:broadMatch/g;
$content =~ s/<closeMatch/<skos:closeMatch/g;
$content =~ s/<exactMatch/<skos:exactMatch/g;
$content =~ s/<narrowMatch/<skos:narrowMatch/g;
$content =~ s/<relatedMatch/<skos:relatedMatch/g;
$content =~ s/<mappingRelation/<skos:mappingRelation/g;

# add prefixes to all end tags
$content =~ s/<\/datcat/<\/dcr:datcat/g;
$content =~ s/<\/source/<\/dcterms:source/g;
$content =~ s/<\/title/<\/dcterms:title/g;
$content =~ s/<\/description/<\/dcterms:description/g;
$content =~ s/<\/hasTopConcept/<\/skos:hasTopConcept/g;
$content =~ s/<\/topConceptOf/<\/skos:topConceptOf/g;
$content =~ s/<\/member/<\/skos:member/g;
$content =~ s/<\/prefLabel/<\/skos:prefLabel/g;
$content =~ s/<\/altLabel/<\/skos:altLabel/g;
$content =~ s/<\/hiddenLabel/<\/skos:hiddenLabel/g;
$content =~ s/<\/notation/<\/skos:notation/g;
$content =~ s/<\/note/<\/skos:note/g;
$content =~ s/<\/changeNote/<\/skos:changeNote/g;
$content =~ s/<\/definition/<\/skos:definition/g;
$content =~ s/<\/example/<\/skos:example/g;
$content =~ s/<\/scopeNote/<\/skos:scopeNote/g;
$content =~ s/<\/editorialNote/<\/skos:editorialNote/g;
$content =~ s/<\/historyNote/<\/skos:historyNote/g;
$content =~ s/<\/inScheme/<\/skos:inScheme/g;
$content =~ s/<\/broader/<\/skos:broader/g;
$content =~ s/<\/narrower/<\/skos:narrower/g;
$content =~ s/<\/related/<\/skos:related/g;
$content =~ s/<\/broaderTransitive/<\/skos:broaderTransitive/g;
$content =~ s/<\/narrowerTransitive/<\/skos:narrowerTransitive/g;
$content =~ s/<\/broadMatch/<\/skos:broadMatch/g;
$content =~ s/<\/closeMatch/<\/skos:closeMatch/g;
$content =~ s/<\/exactMatch/<\/skos:exactMatch/g;
$content =~ s/<\/narrowMatch/<\/skos:narrowMatch/g;
$content =~ s/<\/relatedMatch/<\/skos:relatedMatch/g;
$content =~ s/<\/mappingRelation/<\/skos:mappingRelation/g;

# remove inline namespace declarations
$content =~ s/ xmlns="http:\/\/www.w3.org\/2004\/02\/skos\/core#"//g;
$content =~ s/ xmlns="http:\/\/www.isocat.org\/ns\/dcr.rdf#"//g;
$content =~ s/ xmlns="http:\/\/purl.org\/dc\/terms\/"//g;
$content =~ s/ xmlns="http:\/\/purl.org\/dc\/elements\/1.1\/"//g;

# add prefix definitions to RDF header
$content =~ s/rdf-syntax-ns#">/rdf-syntax-ns#" 
	xmlns:owl="http:\/\/www.w3.org\/2002\/07\/owl#" 
	xmlns:rdfs="http:\/\/www.w3.org\/2000\/01\/rdf-schema#" 
	xmlns:dc="http:\/\/purl.org\/dc\/elements\/1.1\/" 
	xmlns:dcterms="http:\/\/purl.org\/dc\/terms\/" 
	xmlns:dcr="http:\/\/www.isocat.org\/ns\/dcr.rdf#"
	xmlns:skos="http:\/\/www.w3.org\/2004\/02\/skos\/core#">/;

binmode(STDOUT, ":utf8");
print $content;
