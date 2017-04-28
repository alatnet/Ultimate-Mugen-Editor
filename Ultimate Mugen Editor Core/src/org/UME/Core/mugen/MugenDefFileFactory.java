/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.UME.Core.mugen;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author alatnet
 */
public class MugenDefFileFactory {
    static private ArrayList<DefFileHandle> files = new ArrayList<DefFileHandle>();
    
    static synchronized public MugenDefFile requestDefFile(String filename) throws FileNotFoundException, IOException{
        for (int i=0;i<files.size();i++){
            DefFileHandle currFile = files.get(i);
            if (currFile.getFilename().equals(filename)) return currFile.requestDefFile();
        }
        DefFileHandle newDefFile = new DefFileHandle(filename);
        files.add(newDefFile);
        MugenDefFile file = newDefFile.requestDefFile();
        file.read();
        return file;
    }
    
    static synchronized public void closeDefFile(String filename){
        for (int i=0;i<files.size();i++){
            DefFileHandle currFile = files.get(i);
            if (currFile.getFilename().equals(filename)){
                if (currFile.closeDefFile() <= 0) files.remove(i);
                break;
            }
        }
    }
    
    private static class DefFileHandle {
        String filename;
        MugenDefFile defFile;
        int numOpen;
        public DefFileHandle(String filename){
            this.defFile = new MugenDefFile(filename);
            this.filename = filename;
            this.numOpen = 0;
        }
        
        public String getFilename(){ return this.filename; }
        public MugenDefFile requestDefFile(){
            this.numOpen++;
            return this.defFile;
        }
        public int getNumOpen(){ return this.numOpen; }
        public int closeDefFile(){
            this.numOpen--;
            if (this.numOpen<=0) this.defFile = null;
            return this.numOpen;
        }
    }
}
