package searcher;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
//import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
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
	
	protected static Map<Integer,Map<String,Integer> > loadRelevance(String path) throws IOException, NullPointerException {
		Map<Integer,Map<String,Integer> > relevance = new LinkedHashMap<Integer,Map<String,Integer> >();
		File folder = new File(path);
        File[] files = folder.listFiles();
        for (int i=0; i<files.length;i++) {
        	String fileName = files[i].getName();
        	System.out.println(fileName);
        	BufferedReader rdr = new BufferedReader(new FileReader(path + fileName));
        	String line = rdr.readLine();
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

        String corpusPath = "/home/murilo/Documentos/rm/project/data/postags/nlp-master/resources/corpora/brown/";
        
        PosTagger pos = new PosTagger();
        pos.train(corpusPath);
        pos.saveModel("/home/murilo/Documentos/rm/project/data/POSModel.ser");
//        PosTagger pos2 = new PosTagger();
//        pos2.loadModel("/home/murilo/Documentos/rm/project/data/POSModel.ser");
        
        
        
        Map<Integer,Map<String,Integer> > relevance = loadRelevance("/home/murilo/Documentos/rm/project/data/relevance_judgments/test/test/");

        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);
        
        DataTreater dt = new DataTreater("/home/murilo/Documentos/rm/project/data/POSModel.ser");
//        dt.buildCSV("/home/murilo/Documentos/rm/project/data/topics/train/",
//        		reader, "/home/murilo/Documentos/rm/project/data/data.csv");
//        dt.buildData("/home/murilo/Documentos/rm/project/data/topics/train/", 
//        		reader);
//        dt.saveData("/home/murilo/Documentos/rm/project/data/trainData.ser");
        dt.loadData("/home/murilo/Documentos/rm/project/data/trainData.ser");

//        LogisticRegressionClassifier clas = new LogisticRegressionClassifier();
//        System.out.println("Training Model...");
//        clas.train("/home/murilo/Documentos/rm/project/data/trainData.ser",
//        		500000, .01);
//        System.out.println("Done!");
//        clas.saveModel("/home/murilo/Documentos/rm/project/data/logisticData.ser");
//        clas.loadModel("/home/murilo/Documentos/rm/project/data/logisticData.ser");
        String testQueries = "/home/murilo/Documentos/rm/project/data/topics/test/with_relevance/testQueries";
        int k = 2;
        	KNNClassifier clas = new KNNClassifier("/home/murilo/Documentos/rm/project/data/trainData.ser",k);
        	double map_mod = 0;
        	double map_orig = 0;
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
        		ArrayList<ArrayList<Double> > dataPhrase = dt.analyzeSentence(originalQuery, reader);
        		originalQuery = originalQuery.replaceAll("[\\.?!,\")(:;]", " $0 ");
        		originalQuery = originalQuery.replaceAll("--", " -- ");
        		originalQuery = originalQuery.replaceAll("( )\\1+", " ");
        		originalQuery = originalQuery.toLowerCase();
        		ArrayList<Integer> results = clas.predictAll(dataPhrase);//,threshold);
        		
        		originalQuery = originalQuery.replaceAll("[\\.?!,:;]", "");
        		String[] terms = originalQuery.split(" ");
        		
        		String newQuery = "";
        		
        		for (int i=0;i<results.size();i++) {
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
        		}
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
                    pat10 += rel;
                    if (rel == 1) {
                    	ap_orig += pat10/(i+1);
                    }
                }
                if (pat10 > 0) {
        			ap_orig /= pat10;
        		}
                double nrel = (double) getRelevants(ntop,relevance);
                double rat10 = pat10/nrel;
                pat10/=10;
                System.out.println("P@10 for original: " + pat10);
                System.out.println("R@10 for original: " + rat10);
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
                        pat10 += rel;
                        if (rel == 1) {
                        	ap_mod += pat10/(i+1);
                        }
                    }
                }
        		if (pat10 > 0) {
        			ap_mod /= pat10;
        		}
                map_orig += ap_orig;
                map_mod += ap_mod;
                rat10 = pat10/nrel;
                pat10/=10;
                System.out.println("P@10 for modified: " + pat10);
                System.out.println("R@10 for modified: " + rat10);
        	}
        	map_orig /= 50;
        	map_mod /= 50;
        	rdr.close();
        	System.out.println("For KNN classifier with k = " + k + ":\n\t MAP of original queries: " + map_orig + 
        			"\n\t MAP of modified queries: " + map_mod + "\n");
		
        reader.close();
    }

}