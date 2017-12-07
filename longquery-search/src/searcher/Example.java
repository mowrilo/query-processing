package searcher;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.apache.lucene.store.FSDirectory;

import queryReductor.DataTreater;
import queryReductor.KNNClassifier;
import queryReductor.PosTagger;

public class Example {

	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException {
		String indexPath = "/path/to/index";
        Analyzer analyzer = new StandardAnalyzer();

        String corpusPath = "/path/to/POS/corpus";
        
        PosTagger pos = new PosTagger();
        pos.train(corpusPath);
        pos.saveModel("/path/to/POS/model");
        pos.loadModel("/path/to/POS/model");
        
        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);
        
        DataTreater dt = new DataTreater("/path/to/POS/model");
        dt.loadData("/path/to/training/data");
        
        int k = 2;
    	KNNClassifier clas = new KNNClassifier("/path/to/training/data",k);
    	
//      LogisticRegressionClassifier clas = new LogisticRegressionClassifier();
//      System.out.println("Training Model...");
//      clas.train("/path/to/training/data",
//      		500000, .01);
//      System.out.println("Done!");
//      clas.saveModel("/path/to/logisticRegression/data");
//      clas.loadModel("/path/to/logisticRegression/data");
    	
    	String query= "What are the prospects of the Quebec separatists achieving independence from the rest of Canada?";
		
		if (query.endsWith(".")) {
			query= query.replaceAll("[\\.]", "");
			query+= ".";
		} else {
			query = query.replaceAll("[\\.]", "");
		}
		String regex = "(?<=[\\d])(,)(?=[\\d])";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(query);
        query = m.replaceAll("");
		String originalQuery = query;
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
		
		query = query.replaceAll("\\?", ".");
		
		System.out.println("\nOld query: " + query + "\nNew Query: " + newQuery);

		System.out.println("Results for original query:");
		Query q = new QueryParser("contents", analyzer).parse(query);
		TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;
        System.out.println("Found " + hits.length + " hits.");
        for(int i=0;i<hits.length;++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            String docPath = d.get("path");
            String[] paths = docPath.split("/");
            String docName = paths[paths.length-1];
            docName = docName.trim();
            System.out.println("\t" + docName);
        }
        

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
            System.out.println("\t" + docName);
        }
	}
	
}
