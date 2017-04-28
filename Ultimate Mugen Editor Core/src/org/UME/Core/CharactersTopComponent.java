/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.UME.Core;

import javax.swing.event.ChangeEvent;
import org.UME.Core.mugen.MugenDefFile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import java.util.Enumeration;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.swing.DefaultListModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import org.UME.Core.mugen.MugenDefFileFactory;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
//import org.openide.util.Exceptions;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
//import org.openide.DialogDisplayer;
//import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.cookies.SaveCookie;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//UMECore//Characters//EN",
autostore = false)
@TopComponent.Description(preferredID = "CharactersTopComponent",
//iconBase="SET/PATH/TO/ICON/HERE", 
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "UMECore.CharactersTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_CharactersAction",
preferredID = "CharactersTopComponent")
public final class CharactersTopComponent extends TopComponent implements ListSelectionListener, TableModelListener, ActionListener, ChangeListener, SaveCookie, dataReloadInterface{

    public CharactersTopComponent() {
        this.finishedSettingUp = false;
        preInitComponents();
        initComponents();
        setName(NbBundle.getMessage(CharactersTopComponent.class, "CTL_CharactersTopComponent"));
        setToolTipText(NbBundle.getMessage(CharactersTopComponent.class, "HINT_CharactersTopComponent"));
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);
        
        associateLookup(new AbstractLookup(this.content)); //for saving system
        
        this.initPrefsSystem();
        
        saveActionSystem.registerSave(this);
        //dataReloadSystem.registerReload(this);
        compressCharactersAction.setCharacterDataReloadInterface(this);
        
        this.finishedSettingUp = true;
    }
    
    private void initPrefsSystem(){
        Preferences pref = NbPreferences.forModule(UME_BasicPanel.class);
        //String name = pref.get("namePreference", "");
        
        //setup listener when options change.
        pref.addPreferenceChangeListener(
            new PreferenceChangeListener() {
                @Override
                public void preferenceChange(PreferenceChangeEvent evt) {
                    if (evt.getKey().equals("Mugen_Folder")) {
                        if (selectDefPath.length() != 0) MugenDefFileFactory.closeDefFile(selectDefPath);
                        selectDef = null;
                        selectDefPath = evt.getNewValue() + File.separatorChar + "data" + File.separatorChar + "select.def";
                        charsPath = evt.getNewValue() + File.separatorChar + "chars";
                        
                        finishedSettingUp = false;
                        if (loadSelectDef()){
                            loadCurrentCharacters();
                            loadAvailableCharacters();
                            loadMatches();
                        }
                        finishedSettingUp = true;
                    }
                }
            }
        );
        
        //initial Load
        //if (this.selectDefPath.length() != 0) MugenDefFileFactory.closeDefFile(this.selectDefPath);
        this.selectDefPath = pref.get("Mugen_Folder", "C:\\mugen") + File.separatorChar + "data" + File.separatorChar + "select.def";
        this.charsPath = pref.get("Mugen_Folder", "C:\\mugen") + File.separatorChar + "chars";
        this.selectDef = null;
        if (loadSelectDef()){
            loadCurrentCharacters();
            loadAvailableCharacters();
            loadMatches();
        }
    }

    @Override
    public void reloadData() {
        this.finishedSettingUp = false;
        if (selectDefPath.length() != 0) MugenDefFileFactory.closeDefFile(selectDefPath);
        this.selectDef = null;
        this.CurrentCharsList.setSelectedValue(null, false);
        this.AvailableCharsList.setSelectedValue(null, false);
        if (loadSelectDef()){
            loadCurrentCharacters();
            loadAvailableCharacters();
        }
        this.finishedSettingUp = true;
    }
    
    private void preInitComponents(){
        this.listAvailableCharsModel = new DefaultListModel();
        this.listCurrCharsModel  = new DefaultListModel();
        this.tblMatchesModel = new DefaultTableModel();
        this.tblMatchesModel.addTableModelListener(this);
        
        for (int i=1;i<=10;i++) this.tblMatchesModel.addColumn(i);
        this.tblMatchesModel.addRow(new Object[] {0,0,0,0,0,0,0,0,0,0});
        this.tblMatchesModel.addRow(new Object[] {0,0,0,0,0,0,0,0,0,0});
        
        this.selectDefPath = "";
        this.charsPath = "";
        this.selectDef = null;
        this.currSelChar = -1;
        this.content = new InstanceContent(); //for saving system
    }
    
    private boolean loadSelectDef(){
        ProgressHandle handle = ProgressHandleFactory.createHandle("Loading select.def.");
        handle.start();
        handle.switchToIndeterminate();
        //this.selectDef = new MugenDefFile(this.selectDefPath);
        
        try {
            //this.selectDef.read();
            this.selectDef = MugenDefFileFactory.requestDefFile(this.selectDefPath);
        } catch (FileNotFoundException ex) {
            //Logger.getLogger(CharactersTopComponent.class.getName()).log(Level.SEVERE, "Error! Cannot find select.def.", ex);
            Exceptions.printStackTrace(ex);
            handle.finish();
            return false;
        } catch (IOException ex) {
            //Logger.getLogger(CharactersTopComponent.class.getName()).log(Level.SEVERE, "Error! Cannot read select.def.", ex);
            Exceptions.printStackTrace(ex);
            handle.finish();
            return false;
        }
        handle.finish();
        return true;
    }
    
    private void loadCurrentCharacters(){
        ProgressHandle handle = ProgressHandleFactory.createHandle("Reading select.def characters.");
        this.charsGroup = this.selectDef.getGroup("Characters");
        this.listCurrCharsModel.clear();
        
        handle.start(this.charsGroup.size());
        
        for (int i=0;i<this.charsGroup.size();i++){
            handle.progress(i);
            MugenDefFile.Entry entry = this.charsGroup.getEntry(i);
            if (entry instanceof MugenDefFile.ListEntry) this.listCurrCharsModel.addElement(new CharacterDetails((MugenDefFile.ListEntry)entry));
            else if (entry instanceof MugenDefFile.StringEntry) this.listCurrCharsModel.addElement(new CharacterDetails(((MugenDefFile.StringEntry)entry).getString()));
        }
        handle.finish();
    }
    
    private void loadAvailableCharacters(){
        ProgressHandle dirScanProgHandle = ProgressHandleFactory.createHandle("Scanning the chars directory.");
        ProgressHandle parseDirProgHandle = ProgressHandleFactory.createHandle("Parsing the chars directory.");
        
        File charsFolder = new File(this.charsPath);
        ArrayList<File> directories = new ArrayList<File>();
        this.listAvailableCharsModel.clear();
        
        if (charsFolder != null){
            //get the directories
            File[] fileList = charsFolder.listFiles();
            if (fileList != null){
                dirScanProgHandle.start(fileList.length);
                for (int i=0;i<fileList.length;i++){
                    if (fileList[i].isDirectory()) directories.add(fileList[i]);
                    else if (fileList[i].isFile() && fileList[i].getName().endsWith(".zip")) directories.add(fileList[i]);
                    dirScanProgHandle.progress(i);
                }
                dirScanProgHandle.finish();
            
                //parse the directories
                parseDirProgHandle.start(directories.size());
                for (int i=0;i<directories.size();i++){
                    File currentDirectory = directories.get(i);
                    if (currentDirectory.isDirectory()){ //it is a directory, scan it.
                        File[] directoryListing = currentDirectory.listFiles();
                        ArrayList<String> bufferFileNames = new ArrayList<String>();

                        for (int i2=0;i2<directoryListing.length;i2++){
                            if (directoryListing[i2].isFile() && directoryListing[i2].getName().endsWith(".def") && !directoryListing[i2].getName().equals("intro.def") && !directoryListing[i2].getName().equals("ending.def")){
                                bufferFileNames.add(directoryListing[i2].getName());
                            }
                        }

                        if (bufferFileNames.size() == 1) this.listAvailableCharsModel.addElement(new CharacterDetails(currentDirectory.getName()));
                        else for (int i2=0;i2<bufferFileNames.size();i2++) this.listAvailableCharsModel.addElement(new CharacterDetails(currentDirectory.getName() + "\\" + bufferFileNames.get(i2)));
                    }else if (currentDirectory.isFile() && currentDirectory.getName().endsWith(".zip")){ //it is a zip file, open and scan it.
                        ProgressHandle parseZipProgHandle = ProgressHandleFactory.createHandle("Parsing " + currentDirectory.getName() + " for character files.");
                        parseZipProgHandle.start();
                        parseZipProgHandle.switchToIndeterminate();
                        try {
                            ZipFile zipFile = new ZipFile(currentDirectory);
                            ArrayList<String> bufferFileNames = new ArrayList<String>();

                            Enumeration<? extends ZipEntry> entries = zipFile.entries();
                            while (entries.hasMoreElements()){
                                ZipEntry nextElement = entries.nextElement();
                                String name=nextElement.getName();
                                if (name.endsWith(".def") && !name.equals("intro.def") && !name.equals("ending.def")) bufferFileNames.add(name);
                            }

                            if (bufferFileNames.size() == 1) this.listAvailableCharsModel.addElement(new CharacterDetails(currentDirectory.getName()));
                            else for (int i2=0;i2<bufferFileNames.size();i2++) this.listAvailableCharsModel.addElement(new CharacterDetails(currentDirectory.getName() + ":" + bufferFileNames.get(i2)));

                            parseZipProgHandle.finish();
                        } catch (ZipException ex) {
                            parseZipProgHandle.finish();
                            Exceptions.printStackTrace(ex);
                        } catch (IOException ex) {
                            parseZipProgHandle.finish();
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    parseDirProgHandle.progress(i);
                }
                parseDirProgHandle.finish();
            }
        }
    }
    
    private void loadMatches(){
        ProgressHandle handle = ProgressHandleFactory.createHandle("Loading Matches data.");
        handle.start();
        handle.switchToIndeterminate();
        
        MugenDefFile.GroupEntry group = this.selectDef.getGroup("Options");
        
        this.arcadeMatches = (MugenDefFile.KVEntry)group.getEntry(0);
        this.teamMatches = (MugenDefFile.KVEntry)group.getEntry(1);
        
        //this.tblMatchesModel.setValueAt("object", 1, 2); //object (data), row (arcade or team), column (order)
        
        MugenDefFile.Entry entry = this.arcadeMatches.getValue();
        if (entry instanceof MugenDefFile.ListEntry){
            MugenDefFile.ListEntry listEntry = (MugenDefFile.ListEntry)entry;
            for (int i=0;i<10;i++){
                MugenDefFile.Entry entry2 = listEntry.getEntry(i);
                if (entry2 instanceof MugenDefFile.StringEntry){
                    this.tblMatchesModel.setValueAt(((MugenDefFile.StringEntry)entry2).getString(),0,i);
                }else{
                    this.tblMatchesModel.setValueAt(0,0,i);
                }
            }
        }
        
        entry = this.teamMatches.getValue();
        if (entry instanceof MugenDefFile.ListEntry){
            MugenDefFile.ListEntry listEntry = (MugenDefFile.ListEntry)entry;
            for (int i=0;i<10;i++){
                MugenDefFile.Entry entry2 = listEntry.getEntry(i);
                if (entry2 instanceof MugenDefFile.StringEntry){
                    this.tblMatchesModel.setValueAt(((MugenDefFile.StringEntry)entry2).getString(),1,i);
                }else{
                    this.tblMatchesModel.setValueAt(0,1,i);
                }
            }
        }
        handle.finish();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        StageBtnGroup = new javax.swing.ButtonGroup();
        OrderBtnGroup = new javax.swing.ButtonGroup();
        MusicBtnGroup = new javax.swing.ButtonGroup();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        AvailableCharsList = new javax.swing.JList(this.listAvailableCharsModel);
        AddBtn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        CurrentCharsList = new javax.swing.JList(this.listCurrCharsModel);
        jPanel3 = new javax.swing.JPanel();
        NoStageRadioBtn = new javax.swing.JRadioButton();
        RandomStageRadioBtn = new javax.swing.JRadioButton();
        OtherStageRadioBtn = new javax.swing.JRadioButton();
        txtOtherStage = new javax.swing.JTextField();
        cbxAvoidInclude = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        NoOrderRadioBtn = new javax.swing.JRadioButton();
        SelectedOrderRadioBtn = new javax.swing.JRadioButton();
        SelectedOrderSpin = new javax.swing.JSpinner();
        RemoveBtn = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        NoMusicRadioBtn = new javax.swing.JRadioButton();
        SelectedMusicBtn = new javax.swing.JRadioButton();
        txtSelectedMusic = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblMatches = new javax.swing.JTable();
        lblTeam_Matches = new javax.swing.JLabel();
        lblArcade_Matches = new javax.swing.JLabel();
        lblOrder_Matches = new javax.swing.JLabel();

        jSplitPane1.setDividerLocation(220);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.jPanel1.border.title"))); // NOI18N

        AvailableCharsList.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.AvailableCharsList.toolTipText")); // NOI18N
        jScrollPane1.setViewportView(AvailableCharsList);

        org.openide.awt.Mnemonics.setLocalizedText(AddBtn, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.AddBtn.text")); // NOI18N
        AddBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(AddBtn)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(AddBtn)
                .addContainerGap())
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.jPanel2.border.title"))); // NOI18N

        CurrentCharsList.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.CurrentCharsList.toolTipText")); // NOI18N
        CurrentCharsList.addListSelectionListener(this);
        jScrollPane2.setViewportView(CurrentCharsList);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.jPanel3.border.title"))); // NOI18N

        StageBtnGroup.add(NoStageRadioBtn);
        NoStageRadioBtn.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(NoStageRadioBtn, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.NoStageRadioBtn.text")); // NOI18N
        NoStageRadioBtn.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.NoStageRadioBtn.toolTipText")); // NOI18N
        NoStageRadioBtn.addActionListener(this);

        StageBtnGroup.add(RandomStageRadioBtn);
        org.openide.awt.Mnemonics.setLocalizedText(RandomStageRadioBtn, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.RandomStageRadioBtn.text")); // NOI18N
        RandomStageRadioBtn.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.RandomStageRadioBtn.toolTipText")); // NOI18N
        RandomStageRadioBtn.addActionListener(this);

        StageBtnGroup.add(OtherStageRadioBtn);
        org.openide.awt.Mnemonics.setLocalizedText(OtherStageRadioBtn, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.OtherStageRadioBtn.text")); // NOI18N
        OtherStageRadioBtn.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.OtherStageRadioBtn.toolTipText")); // NOI18N
        OtherStageRadioBtn.addActionListener(this);

        txtOtherStage.setText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.txtOtherStage.text")); // NOI18N
        txtOtherStage.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.txtOtherStage.toolTipText")); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, OtherStageRadioBtn, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtOtherStage, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, OtherStageRadioBtn, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtOtherStage, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtOtherStage.addActionListener(this);

        org.openide.awt.Mnemonics.setLocalizedText(cbxAvoidInclude, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.cbxAvoidInclude.text")); // NOI18N
        cbxAvoidInclude.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.cbxAvoidInclude.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, OtherStageRadioBtn, org.jdesktop.beansbinding.ELProperty.create("${selected}"), cbxAvoidInclude, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        cbxAvoidInclude.addActionListener(this);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(OtherStageRadioBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtOtherStage, javax.swing.GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(NoStageRadioBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(RandomStageRadioBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxAvoidInclude)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NoStageRadioBtn)
                    .addComponent(RandomStageRadioBtn)
                    .addComponent(cbxAvoidInclude))
                .addGap(3, 3, 3)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtOtherStage, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(OtherStageRadioBtn))
                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.jPanel4.border.title"))); // NOI18N

        OrderBtnGroup.add(NoOrderRadioBtn);
        NoOrderRadioBtn.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(NoOrderRadioBtn, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.NoOrderRadioBtn.text")); // NOI18N
        NoOrderRadioBtn.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.NoOrderRadioBtn.toolTipText")); // NOI18N
        NoOrderRadioBtn.addActionListener(this);

        OrderBtnGroup.add(SelectedOrderRadioBtn);
        org.openide.awt.Mnemonics.setLocalizedText(SelectedOrderRadioBtn, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.SelectedOrderRadioBtn.text")); // NOI18N
        SelectedOrderRadioBtn.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.SelectedOrderRadioBtn.toolTipText")); // NOI18N
        SelectedOrderRadioBtn.addActionListener(this);

        SelectedOrderSpin.setModel(new javax.swing.SpinnerNumberModel(1, 1, 10, 1));
        SelectedOrderSpin.addChangeListener(this);
        SelectedOrderSpin.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.SelectedOrderSpin.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, SelectedOrderRadioBtn, org.jdesktop.beansbinding.ELProperty.create("${selected}"), SelectedOrderSpin, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(NoOrderRadioBtn)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(SelectedOrderRadioBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(SelectedOrderSpin, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(NoOrderRadioBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(SelectedOrderRadioBtn)
                    .addComponent(SelectedOrderSpin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        org.openide.awt.Mnemonics.setLocalizedText(RemoveBtn, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.RemoveBtn.text")); // NOI18N
        RemoveBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemoveBtnActionPerformed(evt);
            }
        });

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.jPanel5.border.title"))); // NOI18N

        MusicBtnGroup.add(NoMusicRadioBtn);
        NoMusicRadioBtn.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(NoMusicRadioBtn, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.NoMusicRadioBtn.text")); // NOI18N
        NoMusicRadioBtn.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.NoMusicRadioBtn.toolTipText")); // NOI18N
        NoMusicRadioBtn.addActionListener(this);

        MusicBtnGroup.add(SelectedMusicBtn);
        org.openide.awt.Mnemonics.setLocalizedText(SelectedMusicBtn, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.SelectedMusicBtn.text")); // NOI18N
        SelectedMusicBtn.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.SelectedMusicBtn.toolTipText")); // NOI18N
        SelectedMusicBtn.addActionListener(this);

        txtSelectedMusic.setText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.txtSelectedMusic.text")); // NOI18N
        txtSelectedMusic.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.txtSelectedMusic.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, SelectedMusicBtn, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtSelectedMusic, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, SelectedMusicBtn, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtSelectedMusic, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtSelectedMusic.addActionListener(this);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(NoMusicRadioBtn)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(SelectedMusicBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSelectedMusic, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(NoMusicRadioBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(SelectedMusicBtn)
                    .addComponent(txtSelectedMusic, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE)
                    .addComponent(RemoveBtn, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 46, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(RemoveBtn)
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(jPanel2);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.jPanel6.border.title"))); // NOI18N
        jPanel6.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.jPanel6.toolTipText")); // NOI18N

        tblMatches.setModel(this.tblMatchesModel);
        tblMatches.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.tblMatches.toolTipText")); // NOI18N
        jScrollPane3.setViewportView(tblMatches);

        org.openide.awt.Mnemonics.setLocalizedText(lblTeam_Matches, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.lblTeam_Matches.text")); // NOI18N
        lblTeam_Matches.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.lblTeam_Matches.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lblArcade_Matches, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.lblArcade_Matches.text")); // NOI18N
        lblArcade_Matches.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.lblArcade_Matches.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lblOrder_Matches, org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.lblOrder_Matches.text")); // NOI18N
        lblOrder_Matches.setToolTipText(org.openide.util.NbBundle.getMessage(CharactersTopComponent.class, "CharactersTopComponent.lblOrder_Matches.toolTipText")); // NOI18N

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblTeam_Matches)
                    .addComponent(lblArcade_Matches)
                    .addComponent(lblOrder_Matches))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 59, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addComponent(lblOrder_Matches)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblArcade_Matches)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblTeam_Matches)))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 282, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

    private void AddBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddBtnActionPerformed
        int[] indexes = this.AvailableCharsList.getSelectedIndices();
        
        for (int i=0;i<indexes.length;i++){
            CharacterDetails character = (CharacterDetails)this.listAvailableCharsModel.getElementAt(indexes[i]); //this.AvailableCharsList.getSelectedIndex()
            this.listCurrCharsModel.addElement(character);
            this.charsGroup.addEntry(character.getDefEntry());
        }
        this.contentModified(true);
    }//GEN-LAST:event_AddBtnActionPerformed

    private void RemoveBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RemoveBtnActionPerformed
        //int index = this.CurrentCharsList.getSelectedIndex();
        this.checkContentModified(); //check and update data.
        this.finishedSettingUp = false;
        this.currSelChar = -1;
        int[] indexes = this.CurrentCharsList.getSelectedIndices();
        this.CurrentCharsList.setSelectedValue(null, false);
        for (int i=indexes.length-1;i>=0;i--){
            this.listCurrCharsModel.remove(indexes[i]);
            this.charsGroup.removeEntry(indexes[i]);
        }
        this.finishedSettingUp = true;
        this.contentModified(true);
        
    }//GEN-LAST:event_RemoveBtnActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AddBtn;
    private javax.swing.JList AvailableCharsList;
    private javax.swing.JList CurrentCharsList;
    private javax.swing.ButtonGroup MusicBtnGroup;
    private javax.swing.JRadioButton NoMusicRadioBtn;
    private javax.swing.JRadioButton NoOrderRadioBtn;
    private javax.swing.JRadioButton NoStageRadioBtn;
    private javax.swing.ButtonGroup OrderBtnGroup;
    private javax.swing.JRadioButton OtherStageRadioBtn;
    private javax.swing.JRadioButton RandomStageRadioBtn;
    private javax.swing.JButton RemoveBtn;
    private javax.swing.JRadioButton SelectedMusicBtn;
    private javax.swing.JRadioButton SelectedOrderRadioBtn;
    private javax.swing.JSpinner SelectedOrderSpin;
    private javax.swing.ButtonGroup StageBtnGroup;
    private javax.swing.JCheckBox cbxAvoidInclude;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel lblArcade_Matches;
    private javax.swing.JLabel lblOrder_Matches;
    private javax.swing.JLabel lblTeam_Matches;
    private javax.swing.JTable tblMatches;
    private javax.swing.JTextField txtOtherStage;
    private javax.swing.JTextField txtSelectedMusic;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
    private String selectDefPath,charsPath;
    private DefaultListModel listAvailableCharsModel, listCurrCharsModel; //CurrentCharsList
    private DefaultTableModel tblMatchesModel;
    private MugenDefFile selectDef;
    private MugenDefFile.GroupEntry charsGroup;
    private MugenDefFile.KVEntry arcadeMatches,teamMatches;
    //private CharacterDetails currentlySelectedChar;
    private int currSelChar;
    private InstanceContent content; //for saving system
    private boolean finishedSettingUp;

    private boolean checkContentModified(){
        if (!this.finishedSettingUp) return false;
        boolean modified = false;
        
        //get current character data.
        if (this.currSelChar != -1){
            CharacterDetails currChar = (CharacterDetails)this.listCurrCharsModel.getElementAt(this.currSelChar);
            StageType stageType = currChar.getStageType();
            OrderType orderType = currChar.getOrderType();
            MusicType musicType = currChar.getMusicType();
            String otherStage="", otherMusic="";
            int otherOrder=1;
            boolean avoidInclude = currChar.getAvoidInclude();
            if (stageType==StageType.Stage) otherStage = currChar.getStage();
            if (orderType==OrderType.Order) otherOrder = currChar.getOrder();
            if (musicType==MusicType.Music) otherMusic = currChar.getMusic();

            //get gui data.
            StageType stageTypeCheck=StageType.No_Stage; //NoStageRadioBtn or RandomStageRadioBtn or OtherStageRadioBtn
            OrderType orderTypeCheck=OrderType.No_Order; //NoOrderRadioBtn or SelectedOrderRadioBtn
            MusicType musicTypeCheck=MusicType.No_Music; //NoMusicRadioBtn or SelectedMusicBtn
            String otherStageCheck=this.txtOtherStage.getText(), otherMusicCheck=this.txtSelectedMusic.getText();
            int otherOrderCheck=(Integer)this.SelectedOrderSpin.getValue();
            boolean avoidIncludeCheck=this.cbxAvoidInclude.isSelected();

            if (this.NoStageRadioBtn.isSelected()) stageTypeCheck=StageType.No_Stage;
            else if (this.RandomStageRadioBtn.isSelected()) stageTypeCheck=StageType.Random;
            else if (this.OtherStageRadioBtn.isSelected()) stageTypeCheck=StageType.Stage;

            if (this.NoOrderRadioBtn.isSelected()) orderTypeCheck=OrderType.No_Order;
            else if (this.SelectedOrderRadioBtn.isSelected()) orderTypeCheck=OrderType.Order;

            if (this.NoMusicRadioBtn.isSelected()) musicTypeCheck=MusicType.No_Music;
            else if (this.SelectedMusicBtn.isSelected()) musicTypeCheck=MusicType.Music;

            //check if the data was modified and update accordingly.
            if (stageType != stageTypeCheck){
                if (stageTypeCheck == StageType.Stage){
                    if (!otherStageCheck.equals(otherStage)){
                        currChar.setStageType(stageTypeCheck, otherStageCheck);
                        modified = true;
                    }else currChar.setStageType(stageTypeCheck, otherStageCheck);
                }else{
                    currChar.setStageType(stageTypeCheck, "");
                    modified = true;
                }
            }else{
                if (stageTypeCheck == StageType.Stage){
                    if (!otherStageCheck.equals(otherStage)){
                        currChar.setStageType(stageTypeCheck, otherStageCheck);
                        modified = true;
                    }else currChar.setStageType(stageTypeCheck, otherStageCheck);
                }
            }
            if (orderType != orderTypeCheck){
                if (orderTypeCheck == OrderType.Order){
                    if (otherOrderCheck != otherOrder){
                        currChar.setOrderType(orderTypeCheck, otherOrderCheck);
                        modified = true;
                    }else currChar.setOrderType(orderTypeCheck, otherOrderCheck);
                }else{
                    currChar.setOrderType(orderTypeCheck, 1);
                    modified = true;
                }
            }else{
                if (orderTypeCheck == OrderType.Order){
                    if (otherOrderCheck != otherOrder){
                        currChar.setOrderType(orderTypeCheck, otherOrderCheck);
                        modified = true;
                    }else currChar.setOrderType(orderTypeCheck, otherOrderCheck);
                }
            }
            if (musicType != musicTypeCheck){
                if (musicTypeCheck == MusicType.Music){
                    if (!otherMusicCheck.equals(otherMusic)){
                        currChar.setMusicType(musicTypeCheck, otherMusicCheck);
                        modified = true;
                    }else currChar.setMusicType(musicTypeCheck, otherMusicCheck);
                }else{
                    currChar.setMusicType(musicTypeCheck, "");
                    modified = true;
                }
            }else{
                if (musicTypeCheck == MusicType.Music){
                    if (!otherMusicCheck.equals(otherMusic)){
                        currChar.setMusicType(musicTypeCheck, otherMusicCheck);
                        modified = true;
                    }else currChar.setMusicType(musicTypeCheck, otherMusicCheck);
                }
            }

            if (avoidInclude!=avoidIncludeCheck) currChar.setAvoidInclude(avoidIncludeCheck);
        }
        
        //if there is no characters, change currSelChar to -1.
        if (this.listCurrCharsModel.isEmpty()) this.currSelChar = -1;
        
        //if a different character is selected, change display.
        if (this.currSelChar != this.CurrentCharsList.getSelectedIndex() && !listCurrCharsModel.isEmpty()){
            //update with new data.
            this.currSelChar = this.CurrentCharsList.getSelectedIndex();
            this.NoStageRadioBtn.setSelected(true);
            this.NoOrderRadioBtn.setSelected(true);
            this.NoMusicRadioBtn.setSelected(true);
            this.txtOtherStage.setText("");
            this.SelectedOrderSpin.setValue(1);
            this.txtSelectedMusic.setText("");
            this.cbxAvoidInclude.setSelected(false);
            CharacterDetails newChar = (CharacterDetails)this.listCurrCharsModel.getElementAt(this.currSelChar);
            switch (newChar.getStageType()){
                case No_Stage:
                    this.NoStageRadioBtn.setSelected(true);
                    this.txtOtherStage.setText("");
                    break;
                case Random:
                    this.RandomStageRadioBtn.setSelected(true);
                    this.txtOtherStage.setText("");
                    break;
                case Stage:
                    this.OtherStageRadioBtn.setSelected(true);
                    this.txtOtherStage.setText(newChar.getStage());
                    break;
            }
            switch (newChar.getOrderType()){
                case No_Order:
                    this.NoOrderRadioBtn.setSelected(true);
                    this.SelectedOrderSpin.setValue(1);
                    break;
                case Order:
                    this.SelectedOrderRadioBtn.setSelected(true);
                    this.SelectedOrderSpin.setValue(newChar.getOrder());
                    break;
            }
            switch (newChar.getMusicType()){
                case No_Music:
                    this.NoMusicRadioBtn.setSelected(true);
                    this.txtSelectedMusic.setText("");
                    break;
                case Music:
                    this.SelectedMusicBtn.setSelected(true);
                    this.txtSelectedMusic.setText(newChar.getMusic());
                    break;
            }
            this.cbxAvoidInclude.setSelected(newChar.getAvoidInclude());
        }
        
        return modified;
    }

    private void contentModified(boolean modified){
        if (modified) this.content.add(this);
        else this.content.remove(this);
    }
    
    @Override
    public void save() throws IOException {
        for (int i=0;i<this.listCurrCharsModel.size();i++) ((CharacterDetails)this.listCurrCharsModel.get(i)).updateDefEntry();
        this.selectDef.write();
        this.contentModified(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (this.checkContentModified()) this.contentModified(true);
    }
    
    @Override
    public void stateChanged(ChangeEvent e) {
        if (this.checkContentModified()) this.contentModified(true);
    }
    
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (this.checkContentModified()) this.contentModified(true);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (this.finishedSettingUp){
            int column = this.tblMatches.getSelectedColumn(); //order
            int row = this.tblMatches.getSelectedRow(); //arcade or team

            int value = Integer.parseInt((String)this.tblMatchesModel.getValueAt(row, column));
            MugenDefFile.ListEntry listEntry = null;
            MugenDefFile.StringEntry strEntry = null;

            switch(row){
                case 0: //arcade
                    listEntry = (MugenDefFile.ListEntry)this.arcadeMatches.getValue();
                    strEntry = (MugenDefFile.StringEntry)listEntry.getEntry(column);
                    if (value != Integer.parseInt(strEntry.getString())){
                        listEntry.setEntry(column, Integer.toString(value));
                        this.contentModified(true);
                    }
                    break;
                case 1: //team
                    listEntry = (MugenDefFile.ListEntry)this.teamMatches.getValue();
                    strEntry = (MugenDefFile.StringEntry)listEntry.getEntry(column);
                    if (value != Integer.parseInt(strEntry.getString())){
                        listEntry.setEntry(column, Integer.toString(value));
                        this.contentModified(true);
                    }
                    break;
            }
        }
    }
    
    //enumerations for character details.
    private enum StageType{No_Stage,Random,Stage};
    private enum MusicType{No_Music,Music};
    private enum OrderType{No_Order,Order};
    
    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
    
    //character details class to deal with the data from select.def
    private class CharacterDetails {
        private StageType stageType=StageType.No_Stage;
        private MusicType musicType=MusicType.No_Music;
        private OrderType orderType=OrderType.No_Order;
        private String name="",stage="",music="";
        private boolean avoidInclude=false;
        private int order=1;
        //private MugenDefFile.CommentEntry inlineComment=null, multilineComment=null;
        private MugenDefFile.ListEntry listEntry=null;
        
        public CharacterDetails(String name){
            this.listEntry = new MugenDefFile.ListEntry();
            this.name = name;
            this.listEntry.addEntry(name);
        }
        
        public CharacterDetails(MugenDefFile.ListEntry entry){
            this.listEntry = entry;
            //kfm, stages/mybg.def, music=sound/song.mp3, order=1, includestage=0
            
            this.name=((MugenDefFile.StringEntry)entry.getEntry(0)).getString();
            //this.inlineComment = entry.getInlineComment();
            //this.multilineComment = entry.getMultilineComment();
            
            MugenDefFile.Entry entry2;
            
            switch (entry.size()){
                case 1: //only has name
                    break;
                case 2: //can have stage, music, or order.
                    entry2 = entry.getEntry(1);
                    if (entry2 instanceof MugenDefFile.StringEntry){ //entry is a stage
                        MugenDefFile.StringEntry strEntry = (MugenDefFile.StringEntry)entry2;
                        if (strEntry.getString().equals("random")){
                            this.stageType = StageType.Random;
                            this.stage="";
                        }else{
                            this.stageType = StageType.Stage;
                            this.stage=entry2.toString();
                        }
                    }else if (entry2 instanceof MugenDefFile.KVEntry){ //entry is probably order, music, or includestage (but if it is includestage, dont do anything)
                        MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry) entry2;
                        String key = kvEntry.getKey();
                        MugenDefFile.StringEntry value = (MugenDefFile.StringEntry)kvEntry.getValue();
                        
                        if (key.equals("music")){
                            this.musicType = MusicType.Music;
                            this.music = value.getString();
                        }else if(key.equals("order")){
                            this.orderType = OrderType.Order;
                            this.order = Integer.parseInt(value.getString());
                        }else if(key.equals("includestage")){} //dont to anything for this.
                    }
                    break;
                case 3: //can have two of the following: stage, music, order, or includestage (if stage is defined).
                case 4: //can have three of the following: stage, music, order, or includestage (if stage is defined).
                case 5: //has stage, music, order, and includestage=0.
                    boolean hasStage=false;
                    boolean dontIncludeStage=false;
                    for (int i=1;i<entry.size();i++){
                        entry2 = entry.getEntry(i);
                        if (entry2 instanceof MugenDefFile.StringEntry){ //entry is a stage
                            MugenDefFile.StringEntry strEntry = (MugenDefFile.StringEntry)entry2;
                            if (strEntry.getString().equals("random")){
                                this.stageType = StageType.Random;
                                this.stage="";
                            }else{
                                this.stageType = StageType.Stage;
                                this.stage=entry2.toString();
                                hasStage=true;
                            }
                        }else if (entry2 instanceof MugenDefFile.KVEntry){ //entry is probably order, music, or includestage
                            MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry) entry2;
                            String key = kvEntry.getKey();
                            MugenDefFile.StringEntry value = (MugenDefFile.StringEntry)kvEntry.getValue();

                            if (key.equals("music")){
                                this.musicType = MusicType.Music;
                                this.music = value.getString();
                            }else if(key.equals("order")){
                                this.orderType = OrderType.Order;
                                this.order = Integer.parseInt(value.getString());
                            }else if(key.equals("includestage")){
                                dontIncludeStage=true;
                            }
                        }
                        if (hasStage && dontIncludeStage) this.avoidInclude=true;
                    }
                    break;
            }
        }
        
        public String getName(){ return this.name; }
        
        public void setStageType(StageType type, String stage){
            this.stageType = type;
            switch (type){
                case No_Stage:
                case Random:
                    this.stage = "";
                    break;
                case Stage:
                    this.stage = stage;
                    break;
            }
        }
        public StageType getStageType(){ return this.stageType; }
        public String getStage(){ return this.stage; }
        
        public void setMusicType(MusicType type, String music){
            this.musicType = type;
            switch (type){
                case No_Music:
                    this.music = "";
                    break;
                case Music:
                    this.music = music;
                    break;
            }
        }
        public MusicType getMusicType(){ return this.musicType; }
        public String getMusic(){ return this.music; }
        
        public void setOrderType(OrderType type, int order){
            this.orderType = type;
            switch (type){
                case No_Order:
                    this.order = 1;
                    break;
                case Order:
                    this.order = order;
                    break;
            }
        }
        public OrderType getOrderType(){ return this.orderType; }
        public int getOrder(){ return this.order; }
        
        public void setAvoidInclude(boolean avoidInclude){ this.avoidInclude = avoidInclude; }
        public boolean getAvoidInclude(){ return this.avoidInclude; }
        
        @Override
        public String toString(){ return this.name; }
        
        public void updateDefEntry(){
            this.listEntry.clear();
            
            this.listEntry.addEntry(this.name);
            
            switch(this.stageType){
                case Random:
                    //ret+=",random";
                    this.listEntry.addEntry("random");
                    break;
                case Stage:
                    //ret+=","+this.stage;
                    this.listEntry.addEntry(this.stage);
                    break;
            }
            
            if (this.musicType == MusicType.Music) this.listEntry.addEntry(new MugenDefFile.KVEntry("music", this.music));
            if (this.orderType == OrderType.Order) this.listEntry.addEntry(new MugenDefFile.KVEntry("order", Integer.toString(this.order)));
            if (this.avoidInclude) this.listEntry.addEntry("includestage=0");//ret+=",includestage=0";
        }
        
        public MugenDefFile.ListEntry getDefEntry() { return this.listEntry; }
        
        public String toDefString(){
            String ret=this.name;
            
            switch(this.stageType){
                case Random:
                    ret+=",random";
                    break;
                case Stage:
                    ret+=","+this.stage;
                    break;
            }
            
            if (this.musicType == MusicType.Music) ret+=",music="+this.music;
            if (this.orderType == OrderType.Order) ret+=",order="+this.order;
            if (this.avoidInclude) ret+=",includestage=0";
            
            return ret;
        }
    }
}
