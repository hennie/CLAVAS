package orgname2skos;

import au.com.bytecode.opencsv.CSVReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openrdf.model.Statement;

/**
 *
 * @author hennieb
 */
public class OrgnameConverter {

    public static final int LANGCODE = 0;
    public static final int ALTLABELS = 1;
    public static final int SELECTEDVARIANT = 2;
    public static final int EDITORIALNOTE = 3;
    public static final int BROADER = 4;
    private Map<String, List> conceptRecords; // retrieve conceptRecord by prefLabel
    private Map<String, List> expandedConceptRecords; // result after expansion of org hierarchy
    private Map<String, List> recordsForCuration; // problematic concepts, need curation
    private Map<String, String> conceptsBySelectedVariant; // retrieve prefLabel by selectedVariant

//    private Set<String> langCodeSet;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new OrgnameConverter().startConversion(args);
    }

    public void startConversion(String[] args) {
        conceptRecords = new HashMap<String, List>();
        expandedConceptRecords = new HashMap<String, List>();
        recordsForCuration = new HashMap<String, List>();
        conceptsBySelectedVariant = new HashMap<String, String>(); // alternative index

        //       langCodeSet = new HashSet<String>();

        String inputString = "";
        BufferedReader in = null;

        Map<String, String> argMap = processArgs(args);

        if (argMap.isEmpty()) {   // no arguments, use stdin
            try {
                in = new BufferedReader(new InputStreamReader(System.in, "UTF8"));
            } catch (IOException ex) {
                Logger.getLogger(OrgnameConverter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {  // argument present
            String inputFileName = argMap.get("-inputfile");
            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(inputFileName), "UTF8"));
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(OrgnameConverter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(OrgnameConverter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (in == null) {
            return;
        }

        List<String[]> postponedRecords = new ArrayList<String[]>();

        retrieveConceptRecords(in, postponedRecords);
        processPostponedRecords(postponedRecords); // with altlabels, or not unique, or unclear or incomplete.
        expandConceptHierarchy();

        //          for (String prefLabel : expandedConceptRecords.keySet()) {
        //              printConceptRecord(prefLabel, expandedConceptRecords.get(prefLabel));
        //          }
        //   System.out.println("num of orgs, not expanded: " + conceptRecords.size());
        //   System.out.println("curation concepts: " + curationIndex);
        //   System.out.println("total num of orgs: " + expandedConceptRecords.size());

        //       for (String lc : langCodeSet) {
        //          System.out.println(lc);
        //       }

        SesameStore sesameStore = new SesameStore();

        createSKOSGraph(sesameStore);
        addTopConceptTriples(sesameStore);

        // reorder to satisfy OpenSKOS
        SesameStore sesameStore2 = new SesameStore();
        reorderTriples(sesameStore, sesameStore2);

        // export to RDF/XML file
        sesameStore2.exportToRDFXML(null);
    }

    /**
     * Read CSV file. On first pass, create SKOS records with unique prefLabels
     * for all records that a unique Uniform SKOS name.
     *
     * @param in BufferedReader on CSV file
     * @param postponedRecords Non-prefLabel records and records needing
     * curation
     */
    public void retrieveConceptRecords(BufferedReader in, List<String[]> postponedRecords) {
        try {
            // tokenize line: comma separated, with explicit strings that can contain commas
            CSVReader reader = new CSVReader(in);
            String[] nextLine = reader.readNext();  // skip the first, header, line
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                String prefLabel = nextLine[3];
                List cRecord;
                Set<String> altLabels;
                Set<String> selectedVariants;

                // first pass: only process records with prefLabel. Else,
                // postpone processing
                if (prefLabel != null && !prefLabel.equals("")) {                  
                    cRecord = conceptRecords.get(prefLabel);
                    if (cRecord != null) { // not unique, preflabel already exists
                        altLabels = (Set<String>) cRecord.get(1);
                        selectedVariants = (Set<String>) cRecord.get(2);
                    }
                    else {
                        // create conceptRecord
                        cRecord = new ArrayList();
                        conceptRecords.put(prefLabel, cRecord);
                        
                        cRecord.add("");    // initialize langcode field
                        
                        altLabels = new HashSet<String>();
                        cRecord.add(altLabels);
                        
                        selectedVariants = new HashSet<String>();
                        cRecord.add(selectedVariants);
                        
                        cRecord.add("");    // initialize remark field
                        cRecord.add("");    // initialize broader field
                    }

                    String langCode = nextLine[4];
                    if (langCode == null) {
                        langCode = "";
                    }

                    langCode = fixLangCodeErrors(langCode);
                    cRecord.set(0, langCode);

              /*      if (conceptRecords.get(prefLabel) != null) { // not unique, preflabel already exists
                        altLabels.add(prefLabel);   // store non-unique label as altLabel
                        prefLabel = generateUniquePrefLabel();

                        nextLine[6] = "prefLabel not unique. " + nextLine[6];
                    } */

                    String vloName = nextLine[0];
                    if (vloName != null && !vloName.equals("") && !vloName.equals(prefLabel)) {
                        altLabels.add(vloName);
                    }

                    String selectedVariant = nextLine[2];
                    if (selectedVariant != null)
                        selectedVariants.add(selectedVariant);
                            
                    if (!selectedVariant.equals(prefLabel)) {
                        altLabels.add(selectedVariant);
                    }

                    String altSKOSName = nextLine[5];
                    if (altSKOSName != null && !altSKOSName.equals("") && !altSKOSName.equals(prefLabel)) {
                        altLabels.add(altSKOSName);
                    }

                    String remark = nextLine[6];
                    if (remark == null) {
                        remark = "";
                    }
                    
                    cRecord.set(3, remark);
                    cRecord.set(4, "");

                } else { // else, store for 2nd pass
                    postponedRecords.add(nextLine);
                }
            }

            indexBySelectedVariant();

            //          System.out.println("number of concepts: " + conceptRecords.size());
            //         System.out.println("number of postponed records: " + postponedRecords.size());

        } catch (IOException ex) {
            Logger.getLogger(OrgnameConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

/*    public void retrieveConceptRecords(BufferedReader in, List<String[]> postponedRecords) {
        try {
            // tokenize line: comma separated, with explicit strings that can contain commas
            CSVReader reader = new CSVReader(in);
            String[] nextLine = reader.readNext();  // skip the first, header, line
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                String prefLabel = nextLine[3];

                // first pass: only process records with prefLabel. Else,
                // postpone processing
                if (prefLabel != null && !prefLabel.equals("")) {
                    // create conceptRecord
                    List cRecord = new ArrayList();

                    String langCode = nextLine[4];
                    if (langCode == null) {
                        langCode = "";
                    }

                    langCode = fixLangCodeErrors(langCode);

                    cRecord.add(langCode);

                    Set altLabels = new HashSet<String>();
                    cRecord.add(altLabels);

                    if (conceptRecords.get(prefLabel) != null) { // not unique, preflabel already exists
                        altLabels.add(prefLabel);   // store non-unique label as altLabel
                        prefLabel = generateUniquePrefLabel();

                        nextLine[6] = "prefLabel not unique. " + nextLine[6];
                    }

                    String vloName = nextLine[0];
                    if (vloName != null && !vloName.equals("") && !vloName.equals(prefLabel)) {
                        altLabels.add(vloName);
                    }

                    String selectedVariant = nextLine[2];
                    if (selectedVariant == null) {
                        selectedVariant = "";
                    }
                    cRecord.add(selectedVariant);
                    if (!selectedVariant.equals(prefLabel)) {
                        altLabels.add(selectedVariant);
                    }

                    String altSKOSName = nextLine[5];
                    if (altSKOSName != null && !altSKOSName.equals("") && !altSKOSName.equals(prefLabel)) {
                        altLabels.add(altSKOSName);
                    }

                    String remark = nextLine[6];
                    if (remark == null) {
                        remark = "";
                    }
                    cRecord.add(remark);
                    cRecord.add("");    // broader

                    conceptRecords.put(prefLabel, cRecord);

                } else { // else, store for 2nd pass
                    postponedRecords.add(nextLine);
                }
            }

            indexBySelectedVariant();

            //          System.out.println("number of concepts: " + conceptRecords.size());
            //         System.out.println("number of postponed records: " + postponedRecords.size());

        } catch (IOException ex) {
            Logger.getLogger(OrgnameConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
    } */
        
    /**
     * Calculate hash of selectedVariant versus prefLabel. This hash is used to
     * determine where to add additional altLabels to.
     */
    public void indexBySelectedVariant() {
        for (String prefLabel : conceptRecords.keySet()) {
            Set<String> selectedVariants = (Set<String>) conceptRecords.get(prefLabel).get(SELECTEDVARIANT);
            for (String selectedVariant : selectedVariants) {
                if (selectedVariant != null && !selectedVariant.equals("")) {                
                    conceptsBySelectedVariant.put(selectedVariant, prefLabel);
                }
            }
        }
    }

    /**
     * Second pass, over records that do not have a Uniform SKOS name, or a name
     * that is not unique within the Organisation names ConceptScheme.
     *
     * @param postponedRecords
     */
    public void processPostponedRecords(List<String[]> postponedRecords) {
        for (String[] record : postponedRecords) {

            String vloName = record[0];
            String selectedVariant = record[2];
            String prefLabel = record[3];
            String langCode = record[4];
            String altSkosName = record[5];
            String remark = record[6];

            // make sure there are no null values
            if (vloName == null) {
                vloName = "";
            }
            if (selectedVariant == null) {
                selectedVariant = "";
            }
            if (prefLabel == null) {
                prefLabel = "";
            }
            if (langCode == null) {
                langCode = "";
            }
            if (altSkosName == null) {
                altSkosName = "";
            }
            if (remark == null) {
                remark = "";
            }

            if (!selectedVariant.equals("") && !prefLabel.equals("")) { // non-unique preflabel, curate
                System.out.println("THIS SHOULD NOT OCCUR, CHECK DATA");

            } else if (selectedVariant.equals("") && !prefLabel.equals("")) { // curation list
                System.out.println("THIS SHOULD NOT OCCUR, CHECK DATA");

            } else if (!selectedVariant.equals("") && prefLabel.equals("")) { // add vloname as altlabel
                // find concept with matching selectedVariant 
                String matchingPrefLabel = conceptsBySelectedVariant.get(selectedVariant);

                if (!selectedVariant.equals(matchingPrefLabel)) {
                    
                    if (matchingPrefLabel == null) {    // selectedVariant without prefLabel, curation case                                        
                        addInfoAsCurationConcept(langCode, selectedVariant, vloName, altSkosName, remark, "SelectedVariant occurred without Uniform SKOS name.");

                    } else {  // add as altLabel to matchingPrefLabel's record
                        if (!vloName.equals("")) {
                            List matchingRecord = conceptRecords.get(matchingPrefLabel);
                            Set<String> altLabels = (Set<String>) matchingRecord.get(ALTLABELS);
                            altLabels.add(selectedVariant);
                        }
                    }
                } else {  // selected variant equals matchingPrefLabel, add vloName
                    if (!vloName.equals("")) {
                        List matchingRecord = conceptRecords.get(matchingPrefLabel);
                        Set<String> altLabels = (Set<String>) matchingRecord.get(ALTLABELS);
                        altLabels.add(vloName);
                    }
                }

            } else if (selectedVariant.equals("") && prefLabel.equals("")) { //curation list
                // if vloName != "" or altSkosName != "" create curation concept
                addInfoAsCurationConcept(langCode, selectedVariant, vloName, altSkosName, remark, "Incomplete record.");
            }

        }
    }

    public void addInfoAsCurationConcept(String langCode,
            String selectedVariant,
            String vloName,
            String altSkosName,
            String remark,
            String curationMessage) {

        String curationPrefLabel = generateUniquePrefLabel();

        List cRecord = new ArrayList();
        Set<String> altLabels = new HashSet<String>();
        Set<String> selectedVariants = new HashSet<String>();

        cRecord.add(langCode);

        altLabels.add(selectedVariant);
        if (!vloName.equals("")) {
            altLabels.add(vloName);
        }
        if (!altSkosName.equals("")) {
            altLabels.add(altSkosName);
        }
        cRecord.add(altLabels);

        if (!selectedVariant.equals("")) {
            selectedVariants.add(selectedVariant);
        }
        cRecord.add(selectedVariants);
        
        cRecord.add(curationMessage + " " + remark);

        cRecord.add("");    // broader

        conceptRecords.put(curationPrefLabel, cRecord);
    }

    /**
     * PrefLabels in 'conceptRecords' represent organisation hierarchy by means
     * of slashes in the label. Split them, identify or create explicit parent
     * organisations, and adapt the labels.
     */
    public void expandConceptHierarchy() {
        // first, copy non-hierarchical concepts to another result Hash
        for (String prefLabel : conceptRecords.keySet()) {
            if (!prefLabel.contains("/")) {
                expandedConceptRecords.put(prefLabel, conceptRecords.get(prefLabel));
            }
        }

        // on second pass, process all hierarchical concepts. Add results to expandedConceptRecord hash.
        for (String prefLabel : conceptRecords.keySet()) {
            if (prefLabel.contains("/")) {
                List cRecord = conceptRecords.get(prefLabel);
                String langCode = (String) cRecord.get(LANGCODE);

                int index = 0;
                String[] subOrganizations = prefLabel.split("/");
                for (String s : subOrganizations) {
                
                    // construct concatenated prefLabel, except for last sub org in hierarchy
                    String newPrefLabel = customTrim(s);
                    List alreadyExistingOrg = expandedConceptRecords.get(newPrefLabel);

                    if (index < subOrganizations.length - 1) {
                        newPrefLabel += ", " + customTrim(subOrganizations[index + 1]);
                        alreadyExistingOrg = expandedConceptRecords.get(newPrefLabel);
                        
                        // construct potential parent prefLabel
                        String parentPrefLabel = customTrim(subOrganizations[index + 1]);

                        String expandedParentLabel = parentPrefLabel;
                        String parentsParentLabel = "";
                        if (subOrganizations.length > index + 2) {
                            parentsParentLabel = customTrim(subOrganizations[index + 2]);
                            expandedParentLabel += ", " + parentsParentLabel;
                        }

                        List parentCandidateRecord = expandedConceptRecords.get(expandedParentLabel);
                        if (parentCandidateRecord == null) {    // no concatenated parent label                            
                            parentCandidateRecord = expandedConceptRecords.get(parentPrefLabel);
                            if (parentCandidateRecord != null) {
                                // if parent's parent can be identified, or if last suborganization, then set as parent
                                String broaderLabel = (String) parentCandidateRecord.get(BROADER);
                                if (broaderLabel != null && broaderLabel.equals(parentsParentLabel)) {
                        //            expandedParentLabel = parentPrefLabel; // reset to base prefLabel
                                }
                                else {
                                    parentCandidateRecord = null;   // reset to 'not found'
                                }
                            }
                        }

                        if (parentCandidateRecord != null) {
                            cRecord.set(BROADER, expandedParentLabel); // add 'broader org' at index BROADER
                            expandedConceptRecords.put(newPrefLabel, cRecord);

                            cRecord = parentCandidateRecord; // go to next in hierarchy

                        } else {  // no existing organisation identified as parent                            
                            // create parent (is added at next iteration)
                            List newCRecord = new ArrayList();
                            newCRecord.add(langCode);    // langCode, inherit from base prefLabel
                            newCRecord.add(new HashSet<String>()); // altLabels
                            newCRecord.add(new HashSet<String>().add(expandedParentLabel));   // selectedVariant
                            newCRecord.add("Automatically constructed from imported hierarchy");   // editorialNote
                            newCRecord.add("");     // broader
                            expandedConceptRecords.put(parentPrefLabel, newCRecord);

                            if (alreadyExistingOrg != null) {
                                alreadyExistingOrg.set(BROADER, expandedParentLabel);
                            }
                            else {
                                cRecord.set(BROADER, expandedParentLabel); // add 'broader org' at index BROADER
                                expandedConceptRecords.put(newPrefLabel, cRecord);
                            }

                            cRecord = newCRecord;
                        }
                    }

                    index++;
                }
            }
        }
    }

    /**
     * Excludes a list of manually identified potentially problematic
     * suborganization names.
     *
     * @param parentCandidate
     * @return
     */
    public boolean isNotManuallyExcluded(String parentCandidate) {
        if (!"Institute of Linguistics".equals(parentCandidate)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create Graph from expandedConceptRecords.
     */
    public void createSKOSGraph(SesameStore store) {
        // create fixed URIs for RDF classes and properties
        URI rdfTypeURI = URI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        URI skosConceptURI = URI.create("http://www.w3.org/2004/02/skos/core#Concept");
        URI skosConceptSchemeURI = URI.create("http://www.w3.org/2004/02/skos/core#ConceptScheme");
        URI skosPrefLabelURI = URI.create("http://www.w3.org/2004/02/skos/core#prefLabel");
        URI skosAltLabelURI = URI.create("http://www.w3.org/2004/02/skos/core#altLabel");
        URI skosEditorialNoteURI = URI.create("http://www.w3.org/2004/02/skos/core#editorialNote");
        URI skosScopeNoteURI = URI.create("http://www.w3.org/2004/02/skos/core#scopeNote");
        URI skosBroaderURI = URI.create("http://www.w3.org/2004/02/skos/core#broader");
        URI skosInschemeURI = URI.create("http://www.w3.org/2004/02/skos/core#inScheme");
        URI titleURI = URI.create("http://purl.org/dc/terms/title");
        URI descriptionURI = URI.create("http://purl.org/dc/terms/title");

        URI orgSchemeURI = URI.create("http://openskos.meertens.knaw.nl/Organisations");
        URI curationSchemeURI = URI.create("http://openskos.meertens.knaw.nl/CurateOrganisations");

        // create URIs for all prefLabels and store in Hash
        // Necessary to preprocess because of 'forward' broader concept references
        Map<String, URI> conceptURIs = new HashMap<String, URI>();
        for (String prefLabel : expandedConceptRecords.keySet()) {
            conceptURIs.put(prefLabel, URI.create("http://openskos.meertens.knaw.nl/Organisations/" + UUID.randomUUID()));
        }

        // create ConceptSchemes, for orgnames and for curation orgnames
        store.addTriple(orgSchemeURI, rdfTypeURI, skosConceptSchemeURI);
        store.addTriple(orgSchemeURI, titleURI, "OpenSKOS-CLARIN Organizations", "en");
        store.addTriple(orgSchemeURI, descriptionURI, "Organization Vocabulary, bootstrapped by manual curation from the list of VLO organization names (http://www.clarin.eu/vlo).", "en");

        store.addTriple(curationSchemeURI, rdfTypeURI, skosConceptSchemeURI);
        store.addTriple(curationSchemeURI, titleURI, "Organizations - need curation", "en");
        store.addTriple(curationSchemeURI, descriptionURI, "Part of OpenSKOS-CLARIN Organization Vocabulary that needs curation", "en");

        // create Concepts
        for (String prefLabel : expandedConceptRecords.keySet()) {
            List cRecord = expandedConceptRecords.get(prefLabel);

            // rdf:type
            URI conceptURI = conceptURIs.get(prefLabel);
            store.addTriple(conceptURI, rdfTypeURI, skosConceptURI);

            // prefLabel and langcode
            String langCode = (String) cRecord.get(LANGCODE);
            if (langCode.equals("")) {
                store.addTriple(conceptURI, skosPrefLabelURI, prefLabel, "en");
            } else {
                store.addTriple(conceptURI, skosPrefLabelURI, prefLabel, langCode);
                //         store.addTriple(conceptURI, skosPrefLabelURI, prefLabel, "en");
            }

            // altLabels
            Set<String> altLabels = (Set<String>) cRecord.get(ALTLABELS);
            for (String altLabel : altLabels) {
                store.addTriple(conceptURI, skosAltLabelURI, altLabel, "en");
            }

            // skos notes
            String editorialNote = (String) cRecord.get(EDITORIALNOTE);
            if (!editorialNote.equals("")) {
//                store.addTriple(conceptURI, skosEditorialNoteURI, editorialNote);
                store.addTriple(conceptURI, skosEditorialNoteURI, editorialNote, "en");
            }

            // skos broader
            String broader = (String) cRecord.get(BROADER);
            if (!broader.equals("")) {
                URI broaderURI = conceptURIs.get(broader);
                if (broaderURI != null) {
                    store.addTriple(conceptURI, skosBroaderURI, broaderURI);
                }
                //       else {
                //           System.out.println("no uri found for broader: " + broader);
                //       }
            }

            // skos inScheme properties
            store.addTriple(conceptURI, skosInschemeURI, orgSchemeURI);
            if (prefLabel.startsWith("CurationCandidate")) {
                store.addTriple(conceptURI, skosInschemeURI, curationSchemeURI);
            }
        }
    }

    /**
     *
     */
    public void addTopConceptTriples(SesameStore sesameStore) {
        // build and execute SPARQL query
        String query =
                "SELECT ?x ?y WHERE { ?x  <http://www.w3.org/2004/02/skos/core#inScheme>  ?y . FILTER(NOT EXISTS { ?x <http://www.w3.org/2004/02/skos/core#broader> ?broader })}";

        List<HashMap<String, String>> tuples = sesameStore.executeTupleQuery(query);

        // use tuples to construct and add triples to the rdf store
        for (HashMap<String, String> tuple : tuples) {
            URI csURI = URI.create(tuple.get("y"));
            URI conceptURI = URI.create(tuple.get("x"));
            sesameStore.addTriple(csURI, sesameStore.SKOS_HASTOPCONCEPT, conceptURI);
        }
    }

    public void reorderTriples(SesameStore fromStore, SesameStore toStore) {
        String query =
                "SELECT ?x WHERE { ?x  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.w3.org/2004/02/skos/core#ConceptScheme> }";

        List<HashMap<String, String>> tuples = fromStore.executeTupleQuery(query);

        for (HashMap<String, String> tuple : tuples) {
            URI conceptURI = URI.create(tuple.get("x"));

            String query2 = "CONSTRUCT { <" + tuple.get("x") + "> ?p ?o } "
                    + "WHERE { <" + tuple.get("x") + "> ?p ?o }";

            List<Statement> resultStatements = fromStore.executeGraphQuery(query2);
            toStore.addStatements(resultStatements);
        }

        query =
                "SELECT ?x WHERE { ?x  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.w3.org/2004/02/skos/core#Concept> }";

        tuples = fromStore.executeTupleQuery(query);

        for (HashMap<String, String> tuple : tuples) {
            URI conceptURI = URI.create(tuple.get("x"));

            String query2 = "CONSTRUCT { <" + tuple.get("x") + "> ?p ?o } "
                    + "WHERE { <" + tuple.get("x") + "> ?p ?o }";

            List<Statement> resultStatements = fromStore.executeGraphQuery(query2);
            toStore.addStatements(resultStatements);
        }
    }

    /**
     * Utility method to inspect collected Concept data.
     *
     * @param cRecord
     */
    public void printConceptRecord(String prefLabel, List cRecord) {
        String langcode = (String) cRecord.get(LANGCODE);
        Set<String> altLabels = (Set<String>) cRecord.get(ALTLABELS);
        String editorialNote = (String) cRecord.get(EDITORIALNOTE);
        String broader = (String) cRecord.get(BROADER);

        System.out.println(prefLabel + "(" + langcode + ")");

        for (String s : altLabels) {
            System.out.println("\t ALT:" + s);
        }
        System.out.println("\t EDN:" + editorialNote);
        System.out.println("\t BRD:" + broader);
        System.out.println("");
    }

    public Map<String, String> processArgs(String[] args) {
        Map<String, String> argMap = new HashMap<String, String>();

        for (String arg : args) {
            // split at first '=', in argname and argvalue
            int splitPosition = arg.indexOf("=");
            if (splitPosition < 0 || !arg.startsWith("-")) {
                System.out.println("syntax: java -jar Orgnames-to-SKOS.jar [-inputfile=<filename>]");
                break;
            }
            String argName = arg.substring(0, splitPosition);
            String argValue = arg.substring(splitPosition + 1);

            argMap.put(argName, argValue);
        }

        return argMap;
    }
    public static int curationIndex = 1;

    public static String generateUniquePrefLabel() {
        return "CurationCandidate-" + curationIndex++;
    }

    /**
     * The original table contains some language code errors. This is the place
     * where this is fixed.
     *
     * @param langCode
     * @return
     */
    public String fixLangCodeErrors(String langCode) {
        if (langCode.equals("DK")) {
            langCode = "DA";
        }
        if (langCode.equals("CZ")) {
            langCode = "CS";
        }
        if (langCode.equals("SE")) {
            langCode = "SV";
        }

        return langCode;
    }

    /**
     * Java String.trim() does not remove nbsp (non-breaking space) characters
     * '\u00A0' This utility method does.
     *
     * @param s
     * @return
     */
    public String customTrim(String s) {
        s = s.trim();

        while (s.startsWith("\u00A0")) {
            s = s.substring(1);
        }
        while (s.endsWith("\u00A0")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
