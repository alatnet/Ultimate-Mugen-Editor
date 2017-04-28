/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.UME.Core;

import java.util.ArrayList;

/**
 *
 * @author alatnet
 */
public class dataReloadSystem {
    static private ArrayList<dataReloadInterface> reloadDataList;
    
    static public void registerReload(dataReloadInterface dataReload){
        if (reloadDataList == null) reloadDataList = new ArrayList<dataReloadInterface>();
        reloadDataList.add(dataReload);
    }
    
    static public void unregisterReload(dataReloadInterface dataReload){
        if (reloadDataList == null) return;
        reloadDataList.remove(dataReload);
        if (reloadDataList.isEmpty()) reloadDataList = null;
    }
    
    static public void reloadAll(){ for (int i=0;i<reloadDataList.size();i++) reloadDataList.get(i).reloadData(); }
}
