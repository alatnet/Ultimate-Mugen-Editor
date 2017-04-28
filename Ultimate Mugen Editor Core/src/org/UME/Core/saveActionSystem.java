/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.UME.Core;

import java.io.IOException;
import java.util.ArrayList;
import org.openide.cookies.SaveCookie;

/**
 *
 * @author alatnet
 */
public class saveActionSystem {
    static private ArrayList<SaveCookie> saveCookies;
    
    static public void registerSave(SaveCookie saveCookie){
        if (saveCookies == null) saveCookies = new ArrayList<SaveCookie>();
        saveCookies.add(saveCookie);
    }
    
    static public void unregisterSave(SaveCookie saveCookie){
        if (saveCookies == null) return;
        saveCookies.remove(saveCookie);
        if (saveCookies.isEmpty()) saveCookies = null;
    }
    
    static public void saveAll() throws IOException { if (saveCookies != null) for (int i=0;i<saveCookies.size();i++) saveCookies.get(i).save(); }
}
