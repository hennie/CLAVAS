package extendicat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openrdf.model.Statement;

/**
 *
 * @author hennieb
 */
public class ExtendIsoCat {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new ExtendIsoCat().startConversion(args);
    }

    public void startConversion(String[] args) {
        String rdfString = "";

        Map<String, String> argMap = processArgs(args);

        if (argMap.isEmpty()) {   // no arguments, fall back on stdin
            BufferedReader in;
            try {
                String s;
                
                in = new BufferedReader(new InputStreamReader(System.in, "UTF8"));
                while ((s = in.readLine()) != null) {
                    rdfString += s;
                }
            } catch (IOException ex) {
                Logger.getLogger(ExtendIsoCat.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        String rdfFileName = argMap.get("-rdffile");
        //    String rdfString = argMap.get("-rdfstring");

        SesameStore sesameStore = null;

        if (rdfFileName != null) {
            File rdfFile = new File(rdfFileName);
            sesameStore = new SesameStore(rdfFile);
        } else if (rdfString != null) {
            sesameStore = new SesameStore(rdfString);
        }

        addTopConceptTriples(sesameStore);

        // reorder to satisfy OpenSKOS
        SesameStore sesameStore2 = new SesameStore();
        reorderTriples(sesameStore, sesameStore2);

        // export to RDF/XML file
        sesameStore2.exportToRDFXML(null);
    }

    public Map<String, String> processArgs(String[] args) {
        Map<String, String> argMap = new HashMap<String, String>();

        for (String arg : args) {
            // split at first '=', in argname and argvalue
            int splitPosition = arg.indexOf("=");
            if (splitPosition < 0 || !arg.startsWith("-")) {
                //   System.out.println("syntax: java -jar Isocat-extend-SKOS.jar [-rdffile=<filename>][-rdfstring=<rdfstring>]");
                System.out.println("syntax: java -jar Isocat-extend-SKOS.jar [-rdffile=<filename>]");
                break;
            }
            String argName = arg.substring(0, splitPosition);
            String argValue = arg.substring(splitPosition + 1);

            argMap.put(argName, argValue);
        }

        return argMap;
    }

    /**
     *
     */
    public void addTopConceptTriples(SesameStore sesameStore) {
        // build and execute SPARQL query
        String query =
                "SELECT ?x ?y WHERE { ?x  <http://www.w3.org/2004/02/skos/core#inScheme>  ?y }";

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
}
