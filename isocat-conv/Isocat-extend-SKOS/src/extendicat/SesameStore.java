/*
 */

package extendicat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.rio.turtle.TurtleWriter;
import org.openrdf.sail.memory.MemoryStore;

/**
 *
 * @author hennieb
 */
public class SesameStore {
    public static final URI SKOS_HASTOPCONCEPT = URI.create("http://www.w3.org/2004/02/skos/core#hasTopConcept");

    public static final String CATCHPLUS = "http://www.catchplus.nl/annotation/";
    public static final URI CP_TEXTANNOTATION = URI.create("http://www.catchplus.nl/annotation/TextAnnotation");
    public static final URI CP_MONKANNOTATION = URI.create("http://www.catchplus.nl/annotation/MonkAnnotation");
    public static final URI CP_IMAGEANNOTATION = URI.create("http://www.catchplus.nl/annotation/ImageAnnotation");
    public static final URI CP_ENTITYANNOTATION = URI.create("http://www.catchplus.nl/annotation/EntityAnnotation");
    public static final URI CP_CHARS = URI.create("http://www.catchplus.nl/annotation/chars");
    public static final URI CP_INLINETEXTCONSTRAINT = URI.create("http://www.catchplus.nl/annotation/InlineTextConstraint");
    public static final URI CP_SVGCONSTRAINT = URI.create("http://www.catchplus.nl/annotation/SvgConstraint");
    public static final URI CP_TRAILINGTAGS = URI.create("http://www.catchplus.nl/annotation/trailingTags");

    public static final URI SC_CANVAS = URI.create("http://dms.stanford.edu/ns/Canvas");

    public static final URI DC_TITLE = URI.create("http://purl.org/dc/elements/1.1/title");
    public static final URI DC_FORMAT = URI.create("http://purl.org/dc/elements/1.1/format");
    public static final URI DC_IDENTIFIER = URI.create("http://purl.org/dc/elements/1.1/identifier");

    public static final URI DCTERMS_CREATOR = URI.create("http://purl.org/dc/terms/creator");
    public static final URI DCTERMS_CREATED = URI.create("http://purl.org/dc/terms/created");

    public static final URI EXIF_HEIGHT = URI.create("http://www.w3.org/2003/12/exif/ns#height");
    public static final URI EXIF_WIDTH = URI.create("http://www.w3.org/2003/12/exif/ns#width");
    public static final URI RDF_TYPE = URI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

    public static final URI OAC_ANNOTATION = URI.create("http://www.openannotation.org/ns/Annotation");
    public static final URI OAC_BODY = URI.create("http://www.openannotation.org/ns/Body");
    public static final URI OAC_HASBODY = URI.create("http://www.openannotation.org/ns/hasBody");
    public static final URI OAC_HASTARGET = URI.create("http://www.openannotation.org/ns/hasTarget");
    public static final URI OAC_CONSTRAINEDBODY = URI.create("http://www.openannotation.org/ns/ConstrainedBody");
    public static final URI OAC_CONSTRAINEDTARGET = URI.create("http://www.openannotation.org/ns/ConstrainedTarget");
    public static final URI OAC_CONSTRAINT = URI.create("http://www.openannotation.org/ns/Constraint");
    public static final URI OAC_CONSTRAINS = URI.create("http://www.openannotation.org/ns/constrains");
    public static final URI OAC_CONSTRAINEDBY = URI.create("http://www.openannotation.org/ns/constrainedBy");

    public static final URI CNT_CONTENTASTEXT = URI.create("http://www.w3.org/2008/content#ContentAsText");
    public static final URI CNT_CHARS = URI.create("http://www.w3.org/2008/content#chars");
    public static final URI CNT_CHARACTERENCODING = URI.create("http://www.w3.org/2008/content#characterEncoding");


    private Repository localRDFRepository;
    private ValueFactory f;
    private RepositoryConnection con;
    
    private static PrintStream originalOut = System.out;
    private static PrintStream originalErr = System.err;

    public SesameStore() {
        System.err.println("creating and initializing RDF store");
        
        // send all messages to stderr iso stdout
        System.setOut(originalErr);

        localRDFRepository = new SailRepository(new MemoryStore());

        try {
            localRDFRepository.initialize();

        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        f = localRDFRepository.getValueFactory();

        try {
            con = localRDFRepository.getConnection();

        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public SesameStore(File rdfFile) {
        System.err.println("creating and initializing RDF store from file");

        // send all messages to stderr iso stdout
        System.setOut(originalErr);
        
        localRDFRepository = new SailRepository(new MemoryStore());

        try {
            localRDFRepository.initialize();

        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        f = localRDFRepository.getValueFactory();

        try {
            con = localRDFRepository.getConnection();
            if (rdfFile != null) {
                try {
                    con.add(rdfFile, null, RDFFormat.RDFXML);
                    
                } catch (IOException ex) {
                    Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
                } catch (RDFParseException ex) {
                    Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public SesameStore(String rdfString) {
        System.err.println("creating and initializing RDF store from String");

        // send all messages to stderr iso stdout
        System.setOut(originalErr);
        
        localRDFRepository = new SailRepository(new MemoryStore());

        try {
            localRDFRepository.initialize();

        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        f = localRDFRepository.getValueFactory();

        try {
            con = localRDFRepository.getConnection();
            if (rdfString != null) {
                try {
                    String baseURI = "https://catalog.clarin.eu/isocat/rest/profile/5.clavas#";
                    con.add(new StringReader(rdfString), baseURI, RDFFormat.RDFXML);
                    
                } catch (IOException ex) {
                    Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
                } catch (RDFParseException ex) {
                    Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
    public void addTriple(URI subject, URI predicate, String literal) {
        org.openrdf.model.URI s = f.createURI(subject.toString());
        org.openrdf.model.URI p = f.createURI(predicate.toString());
        Literal l = f.createLiteral(literal);
        
        try {
            con.add(s, p, l);

        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addTriple(URI subject, URI predicate, URI object) {
        org.openrdf.model.URI s = f.createURI(subject.toString());

        org.openrdf.model.URI p;
        if (predicate.equals(SesameStore.RDF_TYPE)) {
            p = RDF.TYPE;
        }
        else {
            p = f.createURI(predicate.toString());
        }

        org.openrdf.model.URI o = f.createURI(object.toString());

        try {
            con.add(s, p, o);

        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List<HashMap<String,String>> executeTupleQuery(String queryString) {
        List<HashMap<String,String>> tuples = new ArrayList<HashMap<String,String>>();

        try {
              TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
              TupleQueryResult result = tupleQuery.evaluate();

              // do something with the result
              List<String> bindingNames = result.getBindingNames();
              while (result.hasNext()) {
                HashMap tuple = new HashMap<String,String>();
                BindingSet bindingSet = result.next();
                for (String name : bindingNames) {
                    String valueString = bindingSet.getValue(name).toString();
                    tuple.put(name, valueString);
                }
                tuples.add(tuple);
              }
        }
        catch (OpenRDFException e) {
           // handle exception
        }

        return tuples;
    }

    public List<Statement> executeGraphQuery(String queryString) {
        GraphQueryResult graphResult;
        List<Statement> result = new ArrayList<Statement>();

        try {
            graphResult = con.prepareGraphQuery(QueryLanguage.SPARQL, queryString).evaluate();

            while (graphResult.hasNext()) {
               Statement st = graphResult.next();
               result.add(st);
            }

        } catch (QueryEvaluationException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedQueryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    public void addStatements(List<Statement> statements) {
        try {
            con.add(statements);
            
        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void exportToRDFXML(File exportFile) {
        // restore system out
        System.setOut(originalOut);
        
        try {
            BufferedWriter writer = null;
            String fileName = "/Users/HennieB/Documents/CODA/CODE/resultfiles/" + "test.rdf";
            if (exportFile != null) {
                fileName = exportFile.getAbsolutePath();
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF8"));

            }
            else {
                writer = new BufferedWriter(new OutputStreamWriter(System.out, "UTF8"));
            }
            
            RDFXMLWriter rdfDocWriter = new RDFXMLWriter(writer);
            con.export(rdfDocWriter);

        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RDFHandlerException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // and set to stderr again
        System.setOut(originalErr);
    }

    public void exportAsTurtle() {
        System.setOut(originalOut);

        TurtleWriter turtleWriter = new TurtleWriter(System.out);
        try {
            con.prepareGraphQuery(QueryLanguage.SPARQL, "CONSTRUCT {?x ?p ?y} WHERE {?x ?p ?y}").evaluate(turtleWriter);
            
        } catch (QueryEvaluationException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RDFHandlerException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedQueryException ex) {
            Logger.getLogger(SesameStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.setOut(originalErr);
    }
}
