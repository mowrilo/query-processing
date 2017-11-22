package searcher;

import java.util.StringTokenizer;

import java.io.BufferedReader;
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
import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.FSDirectory;

import queryReductor.PosTagger;

public class Test {
    public static void main(String[] args) throws IOException, ParseException {
        // 0. Specify the analyzer for tokenizing text.
        //    The same analyzer should be used for indexing and searching
//        StandardAnalyzer analyzer = new StandardAnalyzer();
    	String docsPath = "/home/murilo/Documentos/rm/project/data/docs";
    	final Path docDir = Paths.get(docsPath);
        String indexPath = "/home/murilo/Documentos/rm/project/index";
        Directory dir = FSDirectory.open(Paths.get(indexPath));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

          // Create a new index in the directory, removing any
          // previously indexed documents:
//       iwc.setOpenMode(OpenMode.CREATE);
//
//        // Optional: for better indexing performance, if you
//        // are indexing many documents, increase the RAM
//        // buffer.  But if you do this, increase the max heap
//        // size to the JVM (eg add -Xmx512m or -Xmx1g):
//        //
//        // iwc.setRAMBufferSizeMB(256.0);
//
//        IndexWriter writer = new IndexWriter(dir, iwc);
//        indexDocs(writer, docDir);
//
//        // NOTE: if you want to maximize search performance,
//        // you can optionally call forceMerge here.  This can be
//        // a terribly costly operation, so generally it's only
//        // worth it when your index is relatively static (ie
//        // you're done adding documents to it):
//        //
//        // writer.forceMerge(1);
//
//        writer.close();

        // 2. query
        String querystr = "(children) of, bodom.";//args.length > 0 ? args[0] : "lUciene";
        
        System.out.println(querystr);
        querystr = querystr.replaceAll("[\\.,()\\\"]", " $0 ");
        System.out.println(querystr);
//        querystr = querystr.replaceAll("/[()\"]/", " ");
        
        StringTokenizer st = new StringTokenizer(querystr);
        while (st.hasMoreElements()) {
        	System.out.println(st.nextElement());
        }
        String corpusPath = "/home/murilo/Documentos/rm/project/data/postags/nlp-master/resources/corpora/brown/";
        
        PosTagger pos = new PosTagger();
        pos.train(corpusPath);
        
//        File folder = new File(corpusPath); 
//        File[] files = folder.listFiles();
//        for (int i=0; i<files.length;i++) {
//        	System.out.println(corpusPath + files[i].getName());
//        }
        // the "title" arg specifies the default field to use
        // when no field is explicitly specified in the query.
        Query q = new QueryParser("contents", analyzer).parse(querystr);

        // 3. search
        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        // 4. display results
        System.out.println("Found " + hits.length + " hits.");
        for(int i=0;i<hits.length;++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            //System.out.println((i + 1) + ". " + d.get("path"));
        }

        // reader can only be closed when there
        // is no need to access the documents any more.
        reader.close();
    }
    
    
    
    static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
//        	System.out.println("OLAR\n");
          Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
              try {
                indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
              } catch (IOException ignore) {
                // don't index files that can't be read.
              }
              return FileVisitResult.CONTINUE;
            }
          });
        } else {
//        	System.out.println(path);
          indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
      }

    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
          // make a new, empty document
          Document doc = new Document();
          
          // Add the path of the file as a field named "path".  Use a
          // field that is indexed (i.e. searchable), but don't tokenize 
          // the field into separate words and don't index term frequency
          // or positional information:
          Field pathField = new StringField("path", file.toString(), Field.Store.YES);
          doc.add(pathField);
          
          // Add the last modified date of the file a field named "modified".
          // Use a LongPoint that is indexed (i.e. efficiently filterable with
          // PointRangeQuery).  This indexes to milli-second resolution, which
          // is often too fine.  You could instead create a number based on
          // year/month/day/hour/minutes/seconds, down the resolution you require.
          // For example the long value 2011021714 would mean
          // February 17, 2011, 2-3 PM.
          doc.add(new LongPoint("modified", lastModified));
          
          // Add the contents of the file to a field named "contents".  Specify a Reader,
          // so that the text of the file is tokenized and indexed, but not stored.
          // Note that FileReader expects the file to be in UTF-8 encoding.
          // If that's not the case searching for special characters will fail.
          doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
          
          if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            System.out.println("adding " + file);
            writer.addDocument(doc);
          } else {
            // Existing index (an old copy of this document may have been indexed) so 
            // we use updateDocument instead to replace the old one matching the exact 
            // path, if present:
            System.out.println("updating " + file);
            writer.updateDocument(new Term("path", file.toString()), doc);
          }
        }
      }
}