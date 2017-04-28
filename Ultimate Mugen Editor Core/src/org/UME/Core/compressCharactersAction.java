/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.UME.Core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import java.util.zip.*;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Cancellable;
import org.openide.util.NbPreferences;

@ActionID(category = "Utilities",
id = "org.UME.Core.compressCharactersAction")
@ActionRegistration(displayName = "#CTL_compressCharactersAction")
@ActionReferences({
    @ActionReference(path = "Menu/Utilities"/*, position = 3333, separatorAfter = 3383*/)
})
@Messages("CTL_compressCharactersAction=Compress Mugen Characters")
public final class compressCharactersAction implements ActionListener, Runnable, Cancellable {
    private boolean stop = false;
    static private dataReloadInterface characterDataReloadInterface = null;
    
    static void setCharacterDataReloadInterface(dataReloadInterface charDataReloadInterface){ characterDataReloadInterface = charDataReloadInterface; }
    static void removeCharacterDataReloadInterface() { characterDataReloadInterface = null; }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.stop=false;
        Thread compressThread = new Thread(this);
        compressThread.start();
    }
    
    private void addFileToZip(String zipPath, File file, ZipOutputStream zip) throws Exception{
        byte[] buf = new byte[1024];
        int len;
        FileInputStream in = new FileInputStream(file);
        if (zipPath.equals("")) zip.putNextEntry(new ZipEntry(file.getName()));
        else zip.putNextEntry(new ZipEntry(zipPath + "/" + file.getName()));
        while ((len = in.read(buf)) > 0) {
            /*
            * Write the Result 
            */
            zip.write(buf, 0, len);
        }
    }
    
    private void addFoldertoZip(String zipPath, File folder, ZipOutputStream zip) throws Exception{
        if (zipPath.equals("")) zip.putNextEntry(new ZipEntry(folder.getName() + "/"));
        else zip.putNextEntry(new ZipEntry(zipPath + "/" + folder.getName() + "/"));
        File[] fileList = folder.listFiles();
        
        ProgressHandle characterCompressionFileProgress = ProgressHandleFactory.createHandle("Compressing Character File...");
        characterCompressionFileProgress.start(fileList.length);
        
        if (fileList != null){
            for (int i=0;i<fileList.length;i++){
                if (zipPath.equals("")) characterCompressionFileProgress.progress("Adding \'" + fileList[i].getName() + "\' to zip file.", i);
                else characterCompressionFileProgress.progress("Adding \'" + zipPath + "/" + fileList[i].getName() + "\' to zip file.", i);
                if (fileList[i].isDirectory()){
                    if (zipPath.equals("")) addFoldertoZip(folder.getName(),fileList[i],zip);
                    else addFoldertoZip(zipPath + "/" + folder.getName(),fileList[i],zip);
                }else{
                    if (zipPath.equals("")) addFileToZip(folder.getName(),fileList[i],zip);
                    else addFileToZip(zipPath + "/" + folder.getName(),fileList[i],zip);
                }
            }
        }
        characterCompressionFileProgress.finish();
    }
    
    @Override
    public void run() {
        ProgressHandle threadExecProgress = ProgressHandleFactory.createHandle("Compressing Characters.", this);
        threadExecProgress.start();
        threadExecProgress.switchToIndeterminate();
        
        String charsFolderPath = NbPreferences.forModule(UME_BasicPanel.class).get("Mugen_Folder", "C:\\mugen") + File.separatorChar + "chars";
        ArrayList<File> folders = new ArrayList<File>();
        ArrayList<String> fileNames = new ArrayList<String>();
        
        //get list of just directories
        ProgressHandle characterListProgress = ProgressHandleFactory.createHandle("Getting Character List.");
        characterListProgress.start();
        characterListProgress.switchToIndeterminate();
        File charsFolderFile = new File(charsFolderPath);
        if (charsFolderFile != null){
            File[] charsFileList = charsFolderFile.listFiles();
            for (int i=0;i<charsFileList.length;i++){
                if (charsFileList[i].isDirectory()) folders.add(charsFileList[i]);
                else if (charsFileList[i].getName().endsWith(".zip")){
                    String fileName = charsFileList[i].getName();
                    fileName = fileName.substring(0, fileName.length()-4);
                    fileNames.add(fileName);
                }
            }
        }
        characterListProgress.finish();
        
        //check to see if there are characters already compressed. (might be having some problems or something...)
        if (!folders.isEmpty() && !fileNames.isEmpty()){
            ProgressHandle checkAlreadyCompressedProgress = ProgressHandleFactory.createHandle("Checking for already compressed characters.");
            checkAlreadyCompressedProgress.start();
            checkAlreadyCompressedProgress.switchToIndeterminate();
            for (int i=0;i<folders.size();i++){
                if (fileNames.isEmpty()) break;
                for (int i2=0;i2<fileNames.size();i2++){
                    if (fileNames.get(i2).equals(folders.get(i).getName())){
                        folders.remove(i);
                        fileNames.remove(i2);
                        break;
                    }
                }
            }
            checkAlreadyCompressedProgress.finish();
        }
        
        //start compression.
        if (!folders.isEmpty()){
            threadExecProgress.switchToDeterminate(folders.size()+1);
            
            Preferences pref = NbPreferences.forModule(UME_BasicPanel.class);
            int CompressionLevel = pref.getInt("Zip_Compression_Level", 0)-1;
            
            for (int i=0;i<folders.size();i++){
                File folder = folders.get(i);
                File[] fileList = folder.listFiles();

                threadExecProgress.progress("Compressing: " + folder.getName(),i);

                if (fileList != null){
                    try {
                        FileOutputStream fileWriter = new FileOutputStream(charsFolderPath + File.separatorChar + folder.getName() + ".zip");
                        ZipOutputStream zip = new ZipOutputStream(fileWriter);
                        zip.setLevel(CompressionLevel);

                        ProgressHandle characterCompressionFileProgress = ProgressHandleFactory.createHandle("Compressing Character File...");
                        characterCompressionFileProgress.start(fileList.length);
                        for (int i2=0;i2<fileList.length;i2++){
                            characterCompressionFileProgress.progress("Adding \'" + fileList[i2].getName() + "\' to zip file.", i2);
                            if (fileList[i2].isDirectory()){
                                addFoldertoZip("",fileList[i2],zip);
                            }else{
                                addFileToZip("",fileList[i2],zip);
                            }
                        }
                        characterCompressionFileProgress.finish();

                        zip.flush();
                        zip.close();
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }

                if (this.stop) break;
            }
        }
        
        threadExecProgress.finish();
        
        if (characterDataReloadInterface != null) characterDataReloadInterface.reloadData();
    }

    @Override
    public boolean cancel() {
        this.stop = true;
        return true;
    }
}
