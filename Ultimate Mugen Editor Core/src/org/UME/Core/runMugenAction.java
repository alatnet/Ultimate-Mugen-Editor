/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.UME.Core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.prefs.Preferences;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.Exceptions;
//import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

@ActionID(category = "File",
id = "org.UME.Core.runMugenAction")
@ActionRegistration(iconBase = "org/UME/Core/resources/Mugen_Icon_16x16.png",
displayName = "#CTL_runMugenAction")
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1465),
    @ActionReference(path = "Toolbars/File", position = 300),
    @ActionReference(path = "Shortcuts", name = "D-R")
})
//@Messages("CTL_runMugenAction=Run Mugen")
public final class runMugenAction implements ActionListener, Runnable {
    static private boolean unixSys = false, hasWine = false;

    static{
        if (File.separatorChar == '/'){
            unixSys = true;
            
//            Runtime r=Runtime.getRuntime();
//            try {
//                Process p=r.exec("wine -v");
//                p.waitFor();
//                if (p.exitValue() != 0) hasWine=true;
//            } catch (IOException ex) {
//                Exceptions.printStackTrace(ex);
//            } catch (InterruptedException ex) {
//                Exceptions.printStackTrace(ex);
//            } catch (Exception ex){
//                Exceptions.printStackTrace(ex);
//            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ProgressHandle threadExecProgress = ProgressHandleFactory.createHandle("Starting Mugen.");
        threadExecProgress.start();
        threadExecProgress.switchToIndeterminate();
        
        try {
            saveActionSystem.saveAll();
            Thread mugenThread = new Thread(this);
            mugenThread.start();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        threadExecProgress.finish();
    }

    @Override
    public void run() {
        ProgressHandle mugenExecProgress = ProgressHandleFactory.createHandle("Running Mugen.");
        mugenExecProgress.start();
        mugenExecProgress.switchToIndeterminate();
        
        Preferences pref = NbPreferences.forModule(UME_BasicPanel.class);
        
        String mugenFolder = pref.get("Mugen_Folder", "C:\\mugen");
        String mugenExec = pref.get("Mugen_Exec", "mugen.exe");
        
        String s = null;

        if (!unixSys) s = mugenFolder + File.separatorChar + mugenExec;
        else{
            if (mugenExec.endsWith(".exe")){
                if (hasWine){
                    s = "wine \"" + mugenFolder + File.separatorChar + mugenExec + "\"";
                }else{
                    return;
                }
            }
        }

        Runtime r=Runtime.getRuntime();
        Process p=null;
        try {
            
//            InputOutput io = IOProvider.getDefault().getIO ("Mugen Output", true);
            
            p=r.exec(s, null, new File(mugenFolder));
//            BufferedReader errorBuff = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//            BufferedReader inputBuff = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            
//            String inStr = inputBuff.readLine();
//            String errStr = errorBuff.readLine();
//            while (inStr != null && errStr != null){
//                if (inStr != null){
//                    io.getOut().println(inStr);
//                    inStr = inputBuff.readLine();
//                }
//                
//                if (errStr != null){
//                    io.getErr().println(errStr);
//                    errStr = errorBuff.readLine();
//                }
//            }
            
            p.waitFor();
                    
//            errorBuff.close();
//            inputBuff.close();
//            io.getErr().close();
//            io.getOut().close();
                
            
            
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
        dataReloadSystem.reloadAll();
        mugenExecProgress.finish();
    }
}
