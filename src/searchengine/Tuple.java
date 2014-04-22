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
public class Tuple {
    public int first;  // docid
    public int second; //term record
    
    public Tuple(int first, int second) { this.first = first; this.second = second; }
}
