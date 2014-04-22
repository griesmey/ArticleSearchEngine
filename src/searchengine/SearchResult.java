/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package searchengine;

/**
 *
 * @author robertgriesmeyer
 */
public class SearchResult implements Comparable<SearchResult>{
    public int docId;
    public double score;
    
    public SearchResult() {
        // default construtor
    }
    
    @Override
    public int compareTo(SearchResult o) {
        if (this.score < o.score)
            return 1;
        else if (o.score < this.score)
            return -1;
        else
            return 0;
    }
    
    public SearchResult(int docId, double score) {
        this.docId = docId;
        this.score = score;
    }
}
