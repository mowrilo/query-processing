package searcher;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
//import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.FSDirectory;

import queryReductor.DataTreater;
import queryReductor.KNNClassifier;
import queryReductor.LogisticRegressionClassifier;
import queryReductor.PosTagger;

public class Test {
	
	protected static int getRelevants(int ntop,Map<Integer,Map<String,Integer> > relevance) {
		Map<String,Integer> thistop = relevance.get(ntop);
		int nrel = 0;
		for (Integer rel: thistop.values()) {
			nrel += rel.intValue();
		}
		return nrel;
	}
	
//	private Map<Integer,Map<String,Integer> > relevance;
	//"/home/murilo/Documentos/rm/project/data/relevance_judgements/qrels.201-250.disk2.disk3.parts1-5"
	protected static Map<Integer,Map<String,Integer> > loadRelevance(String path) throws IOException, NullPointerException {
		Map<Integer,Map<String,Integer> > relevance = new LinkedHashMap<Integer,Map<String,Integer> >();
		File folder = new File(path);
//		System.out.println(folder.exists());
        File[] files = folder.listFiles();
//        System.out.println(path);
        for (int i=0; i<files.length;i++) {
        	String fileName = files[i].getName();
//        	System.out.println(fileName);
        	BufferedReader rdr = new BufferedReader(new FileReader(path + fileName));
        	String line = rdr.readLine();
//        	int waitingTitle = 1;
        	while (line != null) {
        		String[] fields = line.split(" ");
        		int topic = Integer.parseInt(fields[0]);
        		System.out.println("Topic: " + topic + "\n" + "Doc: " + fields[2] + 
        				"\nRel: " + fields[3] + "\n\n");
        		if (!relevance.containsKey(topic)) {
        			Map<String,Integer> relevDocs = new HashMap<String,Integer>();
        			relevance.put(topic, relevDocs);
        		}
        		String doc = fields[2];
        		doc = doc.trim();
        		int rel = Integer.parseInt(fields[3]);
        		relevance.get(topic).put(doc, rel);
        		line = rdr.readLine();
        	}
        	rdr.close();
        }
        return relevance;
	}
	
    public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException {
        String indexPath = "/home/murilo/Documentos/rm/project/index";
        Analyzer analyzer = new StandardAnalyzer();

//        String corpusPath = "/home/murilo/Documentos/rm/project/data/postags/nlp-master/resources/corpora/brown/";
//        
//        PosTagger pos = new PosTagger();
//        pos.train(corpusPath);
//        pos.saveModel("/home/murilo/Documentos/rm/project/data/POSModel.ser");
        PosTagger pos2 = new PosTagger();
        pos2.loadModel("/home/murilo/Documentos/rm/project/data/POSModel.ser");
//        String a = pos2.tag("Several defendants in the Summerdale police burglary trial made statements indicating their guilt at the time of their arrest , Judge James B Parsons was told in Criminal court yesterday .");
//        String a = pos2.tag("what is the difference between deduction and induction in the process of reasoning");
//        String b = pos2.tag("what information is available on petroleum exploration in the south atlantic near the falkland islands");
//        System.out.println(a);
//        System.out.println(b);
        
        
        Map<Integer,Map<String,Integer> > relevance = loadRelevance("/home/murilo/Documentos/rm/project/data/relevance_judgments/test/test/");
        
//        Coping with overcrowded prisons
//        Antitrust Cases Pending
//        a = pos2.tag("Document discusses a pending antitrust case.");
//        System.out.println(a);
//        File folder = new File(corpusPath); 
//        File[] files = folder.listFiles();
//        for (int i=0; i<files.length;i++) {
//        	System.out.println(corpusPath + files[i].getName());
//        }
        // the "title" arg specifies the default field to use
        // when no field is explicitly specified in the query.
//        Query q = new QueryParser("contents", analyzer).parse(querystr);

        // 3. search
        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);
//        searcher.setSimilarity(new BM25Similarity());
        
        DataTreater dt = new DataTreater();
//        dt.buildCSV("/home/murilo/Documentos/rm/project/data/topics/train/",
//        		reader);
//        dt.buildData("/home/murilo/Documentos/rm/project/data/topics/train/", 
//        		reader);
//        dt.saveData("/home/murilo/Documentos/rm/project/data/trainData.ser");
        dt.loadData("/home/murilo/Documentos/rm/project/data/trainData.ser");
//        String originalQuery = "Document will discuss government assistance to Airbus Industrie or mention a trade dispute between Airbus and a US aircraft producer over the issue of subsidies";
//        String originalQuery = "Identify what is being done or what ideas are being proposed to ensure that Social Security will not go broke.";
//        String originalQuery = "What is the difference between deduction and induction in the process of reasoning?";
//        String originalQuery = "What are the prospects of the Quebec separatists achieving independence from the rest of Canada";
        //String originalQuery = "How widespread is the illegal disposal of medical waste in the U.S. and what is being done to combat this dumping?";
        //Identify what is being done or what ideas are being proposed to ensure that Social Security will not go broke
//        System.in.
//        int ntop = 207;
               
//        else if (j==3) {
//			double newLen = (sample.get(j) - minLen)/(maxLen-minLen);
//			sample2.add(newLen);
//		}
//        LogisticRegressionClassifier clas = new LogisticRegressionClassifier();
//        System.out.println("Training Model...");
//        clas.train("/home/murilo/Documentos/rm/project/data/trainData.ser",
//        		500000, .01);
//        System.out.println("Done!");
//        clas.saveModel("/home/murilo/Documentos/rm/project/data/logisticData.ser");
//        clas.loadModel("/home/murilo/Documentos/rm/project/data/logisticData.ser");
//        double threshold = .8;
        String testQueries = "/home/murilo/Documentos/rm/project/data/topics/test/with_relevance/testQueries";
        double threshold = .45;
//        File precisionFile = new File("/home/murilo/Documentos/rm/project/data/precision.csv");
//        BufferedWriter writer = null;
//        writer = new BufferedWriter(new FileWriter(precisionFile));
//        writer.write("threshold,ntopic,p_at_10_orig,r_at_10_orig,p_at_10_mod,r_at_10_mod\n");
        
//        File mapFile = new File("/home/murilo/Documentos/rm/project/data/map.csv");
//        BufferedWriter writerMap = null;
//        writerMap = new BufferedWriter(new FileWriter(mapFile));
//        writerMap.write("threshold,map_orig,map_mod\n");
        
        for (int th=1; th<10; th++) {
        	KNNClassifier clas = new KNNClassifier("/home/murilo/Documentos/rm/project/data/trainData.ser",th);
        	double map_mod = 0;
        	double map_orig = 0;
        	threshold += .05;
        	System.out.println("Threshold: " + threshold);
        	BufferedReader rdr = new BufferedReader(new FileReader(testQueries));
        	String a;
        	for(int nq = 0; nq < 50; nq++) {
        		a = rdr.readLine();
        		int ntop = Integer.parseInt(a);
        		a = rdr.readLine();
        		
        		String oq = a;
        		oq = oq.replaceAll("/", " or ");
        		if (oq.endsWith(".")) {
    				oq = oq.replaceAll("[\\.]", "");
    				oq += ".";
    			} else {
    				oq = oq.replaceAll("[\\.]", "");
    			}
        		String regex = "(?<=[\\d])(,)(?=[\\d])";
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(oq);
                oq = m.replaceAll("");
        		String originalQuery = oq;
//        		System.out.println(oq);
        		System.out.println("Ntop: " + ntop);
//        		writer.write(th + "," + ntop + ",");
//        		originalQuery = originalQuery.replaceAll("[\\.?!,:;]", "");
//        		originalQuery = originalQuery.replaceAll("/", " or ");
////        		System.out.println("\nOld query: " + originalQuery);
//        		originalQuery = originalQuery.toLowerCase();
        		
//        		System.out.println("Old query: " + originalQuery);
        		ArrayList<ArrayList<Double> > dataPhrase = dt.analyzeSentence(originalQuery, reader);
//        		originalQuery = originalQuery.replaceAll("\\((.*?)\\)", "");//\\\"
//        		if (originalQuery.endsWith(".")) {
//        			originalQuery = originalQuery.replaceAll("[\\.]", "");
//        			originalQuery += ".";
//        		}
        		originalQuery = originalQuery.replaceAll("[\\.?!,\")(:;]", " $0 ");
        		originalQuery = originalQuery.replaceAll("--", " -- ");
        		originalQuery = originalQuery.replaceAll("( )\\1+", " ");
        		originalQuery = originalQuery.toLowerCase();
        		ArrayList<Integer> results = clas.predictAll(dataPhrase);//,threshold);
        		
        		originalQuery = originalQuery.replaceAll("[\\.?!,:;]", "");
        		String[] terms = originalQuery.split(" ");
        		
        		String newQuery = "";
        		//
//        		System.out.println("terms: " + terms.length + "\nOld query: " + originalQuery);
        		
        		for (int i=0;i<results.size();i++) {
//        			if (i < results.size()) {
        			if ((!terms[i].equals(".")) && (!terms[i].equals(",")) &&
    						(!terms[i].equals(":")) && (!terms[i].equals("?")) && 
    						(!terms[i].equals("!")) && (!terms[i].equals("\"")) &&
    						(!terms[i].equals("--")) && (!terms[i].equals(":")) &&
    						(!terms[i].equals(";")) && (!terms[i].equals(")")) &&
    						(!terms[i].equals("("))) {
        				int go = results.get(i).intValue();
            			if (go == 0) {
            				newQuery += (terms[i] + " ");
            			}
        			}
        				
//        			}
        			
        		}
        		
//        		newQuery = "ideas social security broke";
        		if (oq.endsWith("?")) {
        			oq = oq.replaceAll("\\?", ".");
        			oq += "?";
        		} else {
        			oq = oq.replaceAll("\\?", ".");
        		}
        		System.out.println("\nOld query: " + oq + "\nNew Query: " + newQuery);

        		System.out.println("Results for original query:");
        		Query q = new QueryParser("contents", analyzer).parse(oq);
        		TopDocs docs = searcher.search(q, hitsPerPage);
                ScoreDoc[] hits = docs.scoreDocs;
                System.out.println("Found " + hits.length + " hits.");
                double pat10 = 0;
                double ap_orig = 0;
                double ap_mod = 0;
                for(int i=0;i<hits.length;++i) {
                    int docId = hits[i].doc;
                    Document d = searcher.doc(docId);
                    String docPath = d.get("path");
                    String[] paths = docPath.split("/");
                    String docName = paths[paths.length-1];
                    docName = docName.trim();
                    int rel = 0;
                    
                    if (relevance.get(ntop).containsKey(docName))	rel = relevance.get(ntop).get(docName).intValue();
//                    System.out.println((i + 1) + ". " + docName + " Relevance: " + rel);
                    pat10 += rel;
                    if (rel == 1) {
                    	ap_orig += pat10/(i+1);
                    }
                }
                double nrel = (double) getRelevants(ntop,relevance);
                double rat10 = pat10/nrel;
                pat10/=10;
                System.out.println("P@10 for original: " + pat10);
                System.out.println("R@10 for original: " + rat10);
//                writer.write(pat10 + "," + rat10 + ",");
                pat10 = 0;
                System.out.println("Results for new query:");
                if (newQuery.trim().isEmpty()) {
                	for (int i=0; i<10; i++) {
                		int rel = 0;
                		pat10 += rel;
                        if (rel == 1) {
                        	ap_mod += pat10/(i+1);
                        }
                	}
                } else {
                	q = new QueryParser("contents", analyzer).parse(newQuery);
            		docs = searcher.search(q, hitsPerPage);
                    ScoreDoc[] hits2 = docs.scoreDocs;
                    System.out.println("Found " + hits2.length + " hits.");
                    for(int i=0;i<hits2.length;++i) {
                        int docId = hits2[i].doc;
                        Document d = searcher.doc(docId);
                        String docPath = d.get("path");
                        String[] paths = docPath.split("/");
                        String docName = paths[paths.length-1];
                        docName = docName.trim();
                        int rel = 0;
                        if (relevance.get(ntop).containsKey(docName))	rel = relevance.get(ntop).get(docName).intValue();
//                        System.out.println((i + 1) + ". " + docName + " Relevance: " + rel);
                        pat10 += rel;
                        if (rel == 1) {
                        	ap_mod += pat10/(i+1);
                        }
                    }
                }
        		
                map_orig += ap_orig;
                map_mod += ap_mod;
                rat10 = pat10/nrel;
                pat10/=10;
                System.out.println("P@10 for modified: " + pat10);
                System.out.println("R@10 for modified: " + rat10);
//                writer.write(pat10 + "," + rat10 + "\n");
        	}
        	map_orig /= 50;
        	map_mod /= 50;
        	rdr.close();
//        	writerMap.write(th + "," + map_orig + "," + map_mod + "\n");
        }
//        writer.close();
//        writerMap.close();
//        originalQuery = originalQuery.replaceAll("\\((.*?)\\)", "");//\\\"
//		if (originalQuery.endsWith(".")) {
//		originalQuery = originalQuery.replaceAll("[\\.]", "");
//			originalQuery += ".";
//		}
//        System.out.println("\nOld query: " + originalQuery);
		
        reader.close();
    }

}