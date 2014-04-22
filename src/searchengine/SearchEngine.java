package searchengine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;
import java.math.RoundingMode;
import java.math.MathContext;
import java.io.FileReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *  
 * @author robertgriesmeyer
 */
public class SearchEngine {
    
    // The inverted index for the search engine
    private HashMap<String, Vector<Tuple>> ii;
    
    // frequency index
    private HashMap<String, Vector<Tuple>> freqIndex;
    
    //number of documents
    private int N;
    
    private Vector<Vector<Double>> documentVectors = new Vector();
    
    private int[] documentLengths;
    
    public SearchEngine(String filename){
        this.ii = this.constructInvertedIndex(filename);  
        this.documentLengths = new int[this.N];
        this.freqIndex = this.constructFrequencyIndex();
        this.computeDocumentVectors();
    }
    
    private double dotProduct(Vector<Double> a, Vector<Double> b) {
        double sum = 0;
        int len = a.size();
        for(int i = 0; i < len; ++i) 
            sum += a.get(i) * b.get(i);
        return sum;
    }
    
    private Vector<Double> computeDocumentVector(int docId) {
        //loop through all terms
        Iterator it = this.freqIndex.keySet().iterator();
        Vector<Double> docVec = new Vector();
        
        while(it.hasNext()) {
            String term = (String)it.next();
            double score = this.computeScore(term, docId);
            docVec.add(score);
        }
        return docVec;
    }
    
    /**
     * Compute the document vectors for all of the documents in 
     * the corpus.
     */
    private void computeDocumentVectors() {
       
        for(int i = 0; i < N; ++i) { // for each document
            Vector<Double> docVec = this.computeDocumentVector(i);
            this.documentVectors.add(docVec);
        }
    }

    private double computeScore(String term, int docId) {
        int Ftd = 0;
        int documentLen;
        Vector<Tuple> docTermFreq = this.freqIndex.get(term);
        
        for(int i = 0; i < docTermFreq.size(); ++i){
            if(docTermFreq.get(i).first == docId)
                // set frequecy term document
                Ftd = docTermFreq.get(i).second;
        }
        
        documentLen = this.getDocLength(docId);
        
        //Compute IDF
        return this.getTFIDF(term, Ftd, documentLen);
    }
    
    /**
     * Compute term-frequency - Inverse Document frequency
     * 
     */
    private double getTFIDF(String term, double Ftd, double normTerm) {
        double TF;
        double IDF;
        
        IDF = Math.log(this.N/(double)this.computeNt(term));
        
        //Compute TF
        if(Ftd > 0)
            TF = Math.log(Ftd) + 1;
        else
            TF = 0.0;
    
        return (TF*IDF) / normTerm;
    }
    
    public int getDocLength(int docId) {
        return this.documentLengths[docId];
    }

    /**
     * Return the top k documents which match closest to the 
     * query vector
     * 
     * @param query
     * @param k 
     */
    private Vector<Double> getQueryVector(HashSet<String> query, int k) {
        
        int queryLen = query.size();
        Iterator it = this.freqIndex.keySet().iterator();
        Vector<Double> docVec = new Vector();
        
        while(it.hasNext()) {
            String term = (String)it.next();
            if(query.contains(term)) {
                docVec.add(this.getTFIDF(term, 1, queryLen));
            } else {
                docVec.add((double)0.0);
            }
            
        }
        return docVec;
    }

    public SearchResult[] query(String query) {
        String[] terms = query.split(" ");
        
        // Return a number of results proportional to the size of the 
        // courpus.
        int k = 6;
        
        HashSet<String> queries = new HashSet();
        for(int i = 0; i < terms.length; ++i){
            queries.add(terms[i]);
        }
        
        return this.runQuery(queries, k);
    }
    
    private SearchResult[] runQuery(HashSet<String> query, int k) {
        SearchResult[] result = new SearchResult[k];
        
        Vector<Double> queryVec = this.getQueryVector(query, k);
        result = this.rankCosine(queryVec, k);
        
        return result;
    }
    
    /**
     * Return the top k documents for queryVec
     * 
     * @param queryVec
     * @param k
     * @return SearchResult[]
     */
    private SearchResult[] rankCosine(Vector<Double> queryVec, int k) {
        
        SearchResult[] results = new SearchResult[this.N];
        
        for(int i = 0; i < this.N; ++i) {
            // create new amp
            results[i] = new SearchResult();
            results[i].docId = i;
            double dotProd = this.dotProduct(queryVec, this.documentVectors.get(i));
            double queryMag = this.getVectorMagnitude(queryVec);
            double docMag = this.getVectorMagnitude(this.documentVectors.get(i));
            results[i].score = dotProd / (queryMag*docMag);
        }
        //TODO: sort the results and return the top k
        Arrays.sort(results);
        return results;
    }
    
    private double getVectorMagnitude(Vector<Double> x) {
        double sum = 0;
        for(int i = 0; i < x.size(); ++i){
            sum += Math.pow(x.get(i), 2);
        }
        return Math.sqrt(sum);
    }

    private int computeNt(String term) {
        return this.freqIndex.get(term).size();
    }
    
    private Tuple next(String term, int current) {
        Vector<Tuple> Pt = this.ii.get(term);
        int lt = Pt.size();
        
        if (term.contentEquals("") || lt < 1)
            return new Tuple(-1, -1);
        if(Pt.get(0).second > current)
            return Pt.get(0);
        
        return Pt.get(this.binarySearch(Pt, 0, lt - 1, current));
    }
    
    private Tuple getFirst(String term){
        return this.next(term, 0);
    }
    
    private int nextDoc(String term, int current) {
        int nextDocId = current;
        int counter = 0;
        Tuple curTup = this.getFirst(term);
        int lt = this.ii.get(term).size();
        while(nextDocId == current && counter < lt) {
            nextDocId = curTup.first;
            curTup = this.next(term, curTup.second);
            counter++;
        }
        return nextDocId;
    }
    
    private int firstDoc(String term) {
        return this.getFirst(term).first;
    }
    
    private int lastDoc(String term) {
        int lt = this.ii.get(term).size();
        if(lt < 1)
            return -1;
        return this.ii.get(term).get(lt - 1).first;
    }
    
    private String clean(String term) {
        term = term.replace(",", "");
        term = term.replace(".", "");
        term = term.replace(":", "");
        term = term.replace("\"", "");
        term = term.replace(";", "");
        term = term.toLowerCase();
        
        return term;
    }
    
    private HashMap<String, Vector<Tuple>> constructFrequencyIndex() {
        
        HashMap<String, Vector<Tuple>> freqIndex = new HashMap();
        Iterator it = this.ii.keySet().iterator();
        
        //for each term
        while(it.hasNext()) {
            String term = (String)it.next();
            Vector<Tuple> entries = this.ii.get(term);
            int lt = entries.size();
            
            int lastDocId = entries.get(0).first;
            int numEntries = 0;
            
            for(int i = 0; i < lt; ++i){
                int curDocId = entries.get(i).first;
                if(curDocId == lastDocId) {
                    numEntries++;
                    if(i == lt-1)
                        this.addFreqIndexEntry(freqIndex, term, curDocId, numEntries);
                } else {
                    this.addFreqIndexEntry(freqIndex, term, lastDocId, numEntries);
                    numEntries = 1;
                    this.addFreqIndexEntry(freqIndex, term, curDocId, numEntries);
                }
                lastDocId = curDocId;
            }
        }
        return freqIndex;
    }
    
    private void addDocLengthEntry(int docId, int numEntries) {
        this.documentLengths[docId] += numEntries;
    }
    
    private void addFreqIndexEntry(HashMap<String, Vector<Tuple>> fi, String term,
            int docId, int numEntries) {
        Tuple newFreqEntry = new Tuple(docId, numEntries);
        
        // add docid and num entries to documentLengths
        this.addDocLengthEntry(docId, numEntries);
        
        if (fi.containsKey(term)){
             fi.get(term).add(newFreqEntry);
         } else {
            Vector<Tuple> newFreqVec = new Vector();
            newFreqVec.add(newFreqEntry);
            fi.put(term, newFreqVec);
        }
    }
    
    private HashMap<String, Vector<Tuple>> constructInvertedIndex(String filename) {
        
        HashMap<String, Vector<Tuple>> ii = new HashMap();
        
        try {
            FileReader fr;
            int cr;
            int docCount = 0;
            int wordCount = 0;
            fr = new FileReader(filename);
            String term = new String();
            
            while(true){
                cr = fr.read();
                if(cr == ' ' || cr == -1 || cr == '\n'){
                    Tuple t = new Tuple(docCount, wordCount);
                    wordCount++;
                    
                    term = this.clean(term);
                    if (term.contentEquals("</document>"))
                        docCount++;
                    
                    if (ii.containsKey(term)){
                        ii.get(term).add(t);
                    } else {
                        Vector<Tuple> entries = new Vector();
                        entries.add(t);
                        ii.put(term, entries);
                    }
                    if(cr == -1)
                        break;
                    term = "";
                } else {
                    char[] arr = {(char)cr};               
                    String s = new String(arr);
                    term = term.concat(s);
                }
            }
            
            this.N = docCount;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ioex) {
            Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ioex);
        }
        
        return ii;
    }
    
    public static HashSet<String> readQueryFile(String filename) {
        
        HashSet<String> query = new HashSet();
        try{
            int cr;
            FileReader fr;
            fr = new FileReader(filename);
            String term = new String();
            while(true){
                cr = fr.read();
                if (cr == ' ' || cr == -1 || cr == '\n') {
                    
                    query.add(term);
                    term = "";
                    if (cr == -1)
                        break;
                 
                   
                } else {
                    char[] arr = {(char)cr};
                    String s = new String(arr);
                    term = term.concat(s);
                   
                }
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ioex) {
            Logger.getLogger(SearchEngine.class.getName()).log(Level.SEVERE, null, ioex);
        }
        return query;
    }
    
    public static void main(String[] args) {
        SearchEngine se = new SearchEngine("inputfile");
        
        Vector<String> articles = new Vector();
        articles.add("http://www.economist.com/news/business/21595462-google-buys-british-artificial-intelligence-startup-dont-be-evil-genius");
        articles.add("http://www.telegraph.co.uk/technology/google/10603933/Mass-unemployment-fears-over-Google-artificial-intelligence-plans.html");
        articles.add("http://www.as.miami.edu/theatrearts/");
        articles.add("http://drive.porsche.com/us/articles/porsche-north-america-factory-team-rolex-24-daytona");
        articles.add("http://www.sdtimes.com/content/article.aspx?ArticleID=68678&page=1");
        articles.add("This article has barely any content...");
        articles.add("http://online.wsj.com/news/articles/SB10001424052702304626804579362832468516974?mg=reno64-wsj&url=http%3A%2F%2Fonline.wsj.com%2Farticle%2FSB10001424052702304626804579362832468516974.html");
        articles.add("http://lifehacker.com/the-science-behind-how-we-learn-new-skills-908488422");
        articles.add("http://msdn.microsoft.com/en-us/magazine/cc163419.aspx");
        
        int k = articles.size();
        SearchResult[] res = new SearchResult[k];
        HashSet<String> query = SearchEngine.readQueryFile("/Users/robertgriesmeyer/NetBeansProjects/SearchEngine/src/searchengine/queryfile");
//        for(String s: args) {         
//            query.add(s);
//        }
//        query.add("machine");
//        query.add("learning");
        
        System.out.println(query);
        res = se.runQuery(query, k);
//        res = se.query("machine learning");
        for(int i = 0; i < k; ++i){
            System.out.print(res[i].score);
            System.out.print(", ");
            System.out.println(articles.get(res[i].docId));
        }
    }
    
    int binarySearch(Vector<Tuple> v, int low, int high, int current) {
        MathContext mc = new MathContext(1, RoundingMode.FLOOR);
        int mid;
        while(high - low > 1){
            mid = (int) Math.floor((low + high) / 2);
            if(v.get(mid).second <= current)
                low = mid;
            else
                high = mid;        
        }
        return high;
    }
}

