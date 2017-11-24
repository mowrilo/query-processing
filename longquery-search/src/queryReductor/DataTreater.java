package queryReductor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringTokenizer;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import queryReductor.PosTagger;

public class DataTreater {
	
	private long docCount;
	
	public DataTreater() {
		
	}
	
	public static void main(String[] args) throws IOException {
//		System.out.println("Hello world!");
//		buildData("/home/murilo/Documentos/rm/project/data/topics/train/");
	}
	
	public static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
	
	public void buildData(String path, IndexReader reader) throws IOException, ClassNotFoundException {
		File folder = new File(path);
		this.docCount = reader.getDocCount("contents");
        File[] files = folder.listFiles();
        String fileName = "";
        File dataFile = new File("/home/murilo/Documentos/rm/project/data/data.csv");
        BufferedWriter writer = null;
        writer = new BufferedWriter(new FileWriter(dataFile));
        writer.write("idf,total_tf,query_place,pos,last_pos,get_out");
        long docCount = reader.getDocCount("contents");
        PosTagger pos = new PosTagger();
    	pos.loadModel("/home/murilo/Documentos/rm/project/data/POSModel.ser");
        for (int i=0; i<files.length;i++) {
        	fileName = files[i].getName();
        	System.out.println(fileName);
        	BufferedReader rdr = new BufferedReader(new FileReader(path + fileName));
        	String line = rdr.readLine();
//        	int waitingTitle = 1;
        	while (line != null) {
        		if (line.startsWith("<title>")) {
        			line = line.replaceAll("<title>", "");
        			String title = line;
        			line = rdr.readLine();
        			line = rdr.readLine();
        			line = rdr.readLine();
        			String desc = "";
        			while (!line.trim().isEmpty()) {
        				desc += (line + " ");
        				line = rdr.readLine();
        			}
//        			System.out.println("Title: " + title + 
//        					" Description: " + desc);
        			desc = desc.replaceAll("\\((.*?)\\)", "");//\\\"
        			if (desc.endsWith(".")) {
        				desc = desc.replaceAll("[\\.]", "");
        				desc += ".";
        			}
        			desc = desc.replaceAll("[\\.?!,]", " $0 ");
        			title = title.toLowerCase();
        			desc = desc.toLowerCase();
        			String tags = pos.tag(desc);
        			System.out.println(tags);
        			StringTokenizer tokens = new StringTokenizer(desc);
        			StringTokenizer posTags = new StringTokenizer(tags);
//        			System.out.println("tokens " + tokens.countTokens() + " tags " + posTags.countTokens());
        			String lastTag = "<s>";
        			//"idf,total_tf,query_place,pos,last_pos"
        			int place = 0;
        			int phraseSize = tokens.countTokens();
        			while (tokens.hasMoreElements()) {
        				String tok = tokens.nextElement().toString();
        				String tg = posTags.nextElement().toString();
        				tg = tg.replaceAll("[$s]", "");
        				if ((!tg.equals(".")) && (!tg.equals(",")) &&
        						(!tg.equals("*")) && (!tg.equals(":"))) {
        					writer.write("\n");
            				place++;
            				
            				Term t = new Term("contents",tok);
            		        int df = reader.docFreq(t);
            		        double idf = ((double) df)/((double) docCount);
            		        writer.write(String.valueOf(idf)+",");
            		        
            		        long totalTermFreq = reader.totalTermFreq(t);
            		        double termsPerDoc = ((double) totalTermFreq)/((double) df);
            		        if (df == 0) {
            		        	termsPerDoc = 0.;
            		        }
            		        writer.write(String.valueOf(termsPerDoc)+",");
            		        
            		        double queryPlace = ((double) place)/((double) phraseSize);
            				writer.write(String.valueOf(queryPlace)+",");
            		        
            				writer.write(tg+",");
            				writer.write(lastTag+",");
            				
            				int getOut = 1;
            				if (title.contains(tok)) {
            					getOut = 0;
            				}
            				writer.write(String.valueOf(getOut)+"\n");
        				}
        				lastTag = tg;
        			}
        		}
        		line = rdr.readLine();
        	}
//        	String content = readFile(path + fileName, StandardCharsets.UTF_8);
//        	int a = content.indexOf("<title>");
        	
        	rdr.close();
        }
        writer.close();
//        Term t = new Term("contents","children");
//        int ch = reader.docFreq(t);
//        System.out.println("Total doc freq of 'children': " + ch);
//        
//        long dictSize = reader.getSumTotalTermFreq("contents");
//        System.out.println("Dictionary size: " + dictSize);
//        
//        long totalTermFreq = reader.totalTermFreq(t);
//        System.out.println("Total term frequency of 'children': " + 
//        		totalTermFreq);
//        
//        long docCount = reader.getDocCount("contents");
//        System.out.println("Total doc count: " + docCount);
//        
//        long docCount2 = reader.getDocCount("path");
//        System.out.println("Total doc count in 'path': " + docCount2);
        
//        reader.
	}
}
