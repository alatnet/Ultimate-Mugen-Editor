/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.UME.Core;

import org.UME.Core.mugen.MugenDefFile;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import org.UME.Core.mugen.MugenDefFileFactory;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.cookies.SaveCookie;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//UMECore//Settings//EN",
autostore = false)
@TopComponent.Description(preferredID = "SettingsTopComponent",
//iconBase="SET/PATH/TO/ICON/HERE", 
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "UMECore.SettingsTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_SettingsAction",
preferredID = "SettingsTopComponent")
public final class SettingsTopComponent extends TopComponent implements ActionListener, ChangeListener, ListSelectionListener, TableModelListener, SaveCookie, dataReloadInterface {

    public SettingsTopComponent() {
        this.finishedSettingUp = false;
        this.preInitComponents();
        initComponents();
        setName(NbBundle.getMessage(SettingsTopComponent.class, "CTL_SettingsTopComponent"));
        setToolTipText(NbBundle.getMessage(SettingsTopComponent.class, "HINT_SettingsTopComponent"));
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);
        
        associateLookup(new AbstractLookup(this.content)); //for saving system
        
        this.initPrefsSystem();
        
        saveActionSystem.registerSave(this);
        dataReloadSystem.registerReload(this);
        
        this.finishedSettingUp = true;
    }
    
    private void initPrefsSystem(){
        //String name = pref.get("namePreference", "");
        this.pref = NbPreferences.forModule(UME_BasicPanel.class);
        this.pref.addPreferenceChangeListener(
            new PreferenceChangeListener() {
                @Override
                public void preferenceChange(PreferenceChangeEvent evt) {
                    if (evt.getKey().equals("Mugen_Folder")) {
                        //reload mugen.cfg.
                        //reload the motif list.
                        //reload the plugins list.
                        //reload the stages list.
                        finishedSettingUp = false;
                        if (mugenCfgPath.length() != 0) MugenDefFileFactory.closeDefFile(mugenCfgPath);
                        mugenCfg = null;
                        mugenCfgPath = evt.getNewValue() + File.separatorChar + "data" + File.separatorChar + "mugen.cfg";
                        pluginsPath = evt.getNewValue() + File.separatorChar + pref.get("Mugen_Plugins_Folder", "plugins");
                        pluginsFolderName = pref.get("Mugen_Plugins_Folder", "plugins");
                        motifsPath = evt.getNewValue() + File.separatorChar + pref.get("Mugen_Motif_Folder", "data");
                        stagesPath = evt.getNewValue() + File.separatorChar + "stages";
                        
                        if (loadMugenCfg()){
                            loadSettings();
                        }
                        finishedSettingUp = true;
                    }else if(evt.getKey().equals("Mugen_Motif_Folder")){
                        //reload the motif list.
                        finishedSettingUp = false;
                        if (mugenCfgPath.length() != 0) MugenDefFileFactory.closeDefFile(mugenCfgPath);
                        mugenCfg = null;
                        motifsPath = pref.get("Mugen_Folder", "C:\\mugen") + File.separatorChar + evt.getNewValue();
                        
                        if (loadMugenCfg()){
                            loadSettings();
                        }
                        finishedSettingUp = true;
                    }else if(evt.getKey().equals("Mugen_Plugins_Folder")){
                        //reload the plugins list.
                        finishedSettingUp = false;
                        if (mugenCfgPath.length() != 0) MugenDefFileFactory.closeDefFile(mugenCfgPath);
                        mugenCfg = null;
                        pluginsPath = pref.get("Mugen_Folder", "C:\\mugen") + File.separatorChar + evt.getNewValue();
                        pluginsFolderName = evt.getNewValue();
                        
                        if (loadMugenCfg()){
                            loadSettings();
                        }
                        finishedSettingUp = true;
                    }
                }
            }
        );
        this.mugenCfgPath = this.pref.get("Mugen_Folder", "C:\\mugen") + File.separatorChar + "data" + File.separatorChar + "mugen.cfg";
        this.pluginsPath = this.pref.get("Mugen_Folder", "C:\\mugen") + File.separatorChar + this.pref.get("Mugen_Plugins_Folder", "plugins");
        this.pluginsFolderName = pref.get("Mugen_Plugins_Folder", "plugins");
        this.motifsPath = this.pref.get("Mugen_Folder", "C:\\mugen") + File.separatorChar + this.pref.get("Mugen_Motif_Folder", "data");
        this.stagesPath = this.pref.get("Mugen_Folder", "C:\\mugen") + File.separatorChar + "stages";
        
        this.mugenCfg = null;
        if (this.loadMugenCfg()){
            loadSettings();
        }
    }
    
    @Override
    public void reloadData(){
        if (mugenCfgPath.length() != 0) MugenDefFileFactory.closeDefFile(mugenCfgPath);
        this.mugenCfg = null;
        if (this.loadMugenCfg()){
            loadSettings();
        }
    }
    
    private void preInitComponents(){
        this.listMotifModel = new DefaultListModel<String>();
        this.listMotifModel.addElement("data/system.def");
        
        this.listStartStageModel = new DefaultListModel<String>();
        this.listStartStageModel.addElement("stages/stage0-720.def");
        
        this.listAvailablePluginsModel = new DefaultListModel<PluginDetails>();
        this.listCurrPluginsModel = new DefaultListModel<PluginDetails>();
        
        this.cmbxPluginParamsModel = new DefaultComboBoxModel<PluginDetails.PluginParam>();
        
        //<editor-fold defaultstate="collapsed" desc="AI Ramp Table">
        this.tblAIRampModel = new DefaultTableModel();
        this.tblAIRampModel.addColumn("Game Type");
        this.tblAIRampModel.addColumn("Start Match");
        this.tblAIRampModel.addColumn("Start Difference");
        this.tblAIRampModel.addColumn("End Match");
        this.tblAIRampModel.addColumn("End Difference");
        
        this.tblAIRampModel.addRow(new Object[]{"Arcade",2,0,4,2});
        this.tblAIRampModel.addRow(new Object[]{"Team",1,0,3,2});
        this.tblAIRampModel.addRow(new Object[]{"Survival",0,-3,16,4});
        this.tblAIRampModel.addTableModelListener(this);
        //</editor-fold>
        
        this.content = new InstanceContent(); //for saving system

        //<editor-fold defaultstate="collapsed" desc="Settings Class Creation">
        this.optionSettings = new OptionSettings();
        this.rulesSettings = new RulesSettings();
        this.configSettings = new ConfigSettings();
        this.debugSettings = new DebugSettings();
        this.videoSettings = new VideoSettings();
        this.soundSettings = new SoundSettings();
        this.musicSettings = new MusicSettings();
        this.miscSettings = new MiscSettings();
        this.arcadeSettings = new ArcadeSettings();
        this.inputSettings = new InputSettings();
        //</editor-fold>
    }
    
    private boolean loadMugenCfg(){
        ProgressHandle handle = ProgressHandleFactory.createHandle("Loading mugen.cfg.");
        handle.start();
        handle.switchToIndeterminate();
        
        //this.mugenCfg = new MugenDefFile(this.mugenCfgPath);
        try {
            //this.mugenCfg.read();
            this.mugenCfg = MugenDefFileFactory.requestDefFile(this.mugenCfgPath);
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
            handle.finish();
            return false;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            handle.finish();
            return false;
        }
        handle.finish();
        return true;
    }
    
    private void loadSettings(){
        ProgressHandle handle = ProgressHandleFactory.createHandle("Loading mugen.cfg data.");
        handle.start(10);
        this.optionSettings.setGroup(this.mugenCfg.getGroup("Options"));
        handle.progress(1);
        this.rulesSettings.setGroup(this.mugenCfg.getGroup("Rules"));
        handle.progress(2);
        this.configSettings.setGroup(this.mugenCfg.getGroup("Config"));
        handle.progress(3);
        this.debugSettings.setGroup(this.mugenCfg.getGroup("Debug"));
        handle.progress(4);
        this.videoSettings.setGroup(this.mugenCfg.getGroup("Video"));
        handle.progress(5);
        this.soundSettings.setGroup(this.mugenCfg.getGroup("Sound"));
        handle.progress(6);
        this.musicSettings.setGroup(this.mugenCfg.getGroup("Music"));
        handle.progress(7);
        this.miscSettings.setGroup(this.mugenCfg.getGroup("Misc"));
        handle.progress(8);
        this.arcadeSettings.setGroup(this.mugenCfg.getGroup("Arcade"));
        handle.progress(9);
        this.inputSettings.setGroup(
            this.mugenCfg.getGroup("Input"),
            this.mugenCfg.getGroup("P1 Keys"),
            this.mugenCfg.getGroup("P2 Keys"),
            this.mugenCfg.getGroup("P1 Joystick"),
            this.mugenCfg.getGroup("P2 Joystick")
        );
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

        settingsTabbedPane = new javax.swing.JTabbedPane();
        Settings_Options = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        cmbxDifficulty_Options = new javax.swing.JComboBox<String>();
        spinLife_Options = new javax.swing.JSpinner();
        spinTime_Options = new javax.swing.JSpinner();
        spinGameSpd_Options = new javax.swing.JSpinner();
        Settings_Motif_Options = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listMotif_Options = new javax.swing.JList(this.listMotifModel);
        Settings_Team_Options = new javax.swing.JPanel();
        cbxTeamLoseOnKO_Options = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        spinTeam1v2Life_Options = new javax.swing.JSpinner();
        Settings_Rules = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        txtGameType_Rules = new javax.swing.JTextField();
        txtAtkL2PM_Rules = new javax.swing.JTextField();
        txtGetHitL2PM_Rules = new javax.swing.JTextField();
        txtSuperTgtDefM_Rules = new javax.swing.JTextField();
        Settings_Config = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        spinGameSpd_Config = new javax.swing.JSpinner();
        jLabel13 = new javax.swing.JLabel();
        cmbxGameRes_Config = new javax.swing.JComboBox<String>();
        jLabel14 = new javax.swing.JLabel();
        txtLang_Config = new javax.swing.JTextField();
        cbxDrawShadows_Config = new javax.swing.JCheckBox();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        cbxFirstRun_Config = new javax.swing.JCheckBox();
        spinMaxAfterImgs_Config = new javax.swing.JSpinner();
        spinMaxLayeredSprites_Config = new javax.swing.JSpinner();
        spinSpriteDecomBuffSize_Config = new javax.swing.JSpinner();
        spinMaxExplods_Config = new javax.swing.JSpinner();
        spinMaxSysExplods_Config = new javax.swing.JSpinner();
        spinMaxHelpers_Config = new javax.swing.JSpinner();
        spinMaxPlayerProj_Config = new javax.swing.JSpinner();
        Settings_Debug = new javax.swing.JPanel();
        cbxDebug_Debug = new javax.swing.JCheckBox();
        cbxAllowDebugMode_Debug = new javax.swing.JCheckBox();
        cbxAllowDebugKeys_Debug = new javax.swing.JCheckBox();
        jLabel22 = new javax.swing.JLabel();
        spinSpdUp_Debug = new javax.swing.JSpinner();
        Settings_StartStage_Debug = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        listStartStage_Debug = new javax.swing.JList(this.listStartStageModel);
        cbxHideDevBuildBanner_Debug = new javax.swing.JCheckBox();
        Settings_Video = new javax.swing.JPanel();
        Settings_Resolution_Video = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        listRes_Video = new javax.swing.JList(this.listResModel);
        cbxFullscreen_Video = new javax.swing.JCheckBox();
        cbxVertRetrace_Video = new javax.swing.JCheckBox();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        cmbxBlitMode_Video = new javax.swing.JComboBox<String>();
        cmbxRenderMode_Video = new javax.swing.JComboBox<String>();
        cmbxDepth_Video = new javax.swing.JComboBox();
        jLabel88 = new javax.swing.JLabel();
        cbxSafeMode_Video = new javax.swing.JCheckBox();
        cbxResizable_Video = new javax.swing.JCheckBox();
        cbxKeepAspect_Video = new javax.swing.JCheckBox();
        cbxStageFit_Video = new javax.swing.JCheckBox();
        cbxSystemFit_Video = new javax.swing.JCheckBox();
        Settings_Sounds = new javax.swing.JPanel();
        cbxEnableSound_Sounds = new javax.swing.JCheckBox();
        cbxStereoEfx_Sounds = new javax.swing.JCheckBox();
        cbxReverseStereo_Sounds = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        txtSampleRate_Sounds = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        spinBuffSize_Sounds = new javax.swing.JSpinner();
        jLabel25 = new javax.swing.JLabel();
        spinPanWidth_Sounds = new javax.swing.JSpinner();
        jLabel26 = new javax.swing.JLabel();
        spinWaveChans_Sounds = new javax.swing.JSpinner();
        Settings_Volume_Sounds = new javax.swing.JPanel();
        jLabel27 = new javax.swing.JLabel();
        spinMasterVol_Sounds = new javax.swing.JSpinner();
        slideMasterVol_Sounds = new javax.swing.JSlider();
        jLabel28 = new javax.swing.JLabel();
        spinWavVol_Sounds = new javax.swing.JSpinner();
        slideWavVol_Sounds = new javax.swing.JSlider();
        jLabel29 = new javax.swing.JLabel();
        spinBGMVol_Sounds = new javax.swing.JSpinner();
        slideBGMVol_Sounds = new javax.swing.JSlider();
        Settings_Resample_Sound = new javax.swing.JPanel();
        jLabel30 = new javax.swing.JLabel();
        Settings_Resample_Quality_Sounds = new javax.swing.JPanel();
        jLabel31 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        cmbxSFXQuality_Sounds = new javax.swing.JComboBox<String>();
        cmbxBGMQuality_Sounds = new javax.swing.JComboBox<String>();
        cmbxSFXMethod_Sounds = new javax.swing.JComboBox<String>();
        Settings_Music = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        Settings_BuiltIn_Music = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jPanel9 = new javax.swing.JPanel();
        Settings_BuiltIn_mpg123_Music = new javax.swing.JPanel();
        jLabel33 = new javax.swing.JLabel();
        cmbxMpg123Decoder_Music = new javax.swing.JComboBox();
        jLabel34 = new javax.swing.JLabel();
        cmbxMpg123RVA_Music = new javax.swing.JComboBox();
        jLabel35 = new javax.swing.JLabel();
        txtMpg123Ext_Music = new javax.swing.JTextField();
        jLabel44 = new javax.swing.JLabel();
        spinMpg123Vol_Music = new javax.swing.JSpinner();
        slideMpg123Vol_Music = new javax.swing.JSlider();
        Settings_BuiltIn_sdlMix_Music = new javax.swing.JPanel();
        jLabel36 = new javax.swing.JLabel();
        spinSdlMixMidiVol_Music = new javax.swing.JSpinner();
        slideSdlMixMidiVol_Music = new javax.swing.JSlider();
        jLabel37 = new javax.swing.JLabel();
        spinSdlMixModVol_Music = new javax.swing.JSpinner();
        slideSdlMixModVol_Music = new javax.swing.JSlider();
        jSplitPane1 = new javax.swing.JSplitPane();
        Settings_AvaliablePlugins_Music = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        listAvailablePlugins_Music = new javax.swing.JList(this.listAvailablePluginsModel);
        btnAddPlugins = new javax.swing.JButton();
        Settings_CurrentPlugins_Music = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        listCurrPlugins_Music = new javax.swing.JList(this.listCurrPluginsModel);
        btnRmPlugins_Music = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        cmbxPluginParams_Music = new javax.swing.JComboBox<PluginDetails.PluginParam>();
        btnAddParam_Music = new javax.swing.JButton();
        btnRemoveParam_Music = new javax.swing.JButton();
        txtParamKey_Music = new javax.swing.JTextField();
        jLabel38 = new javax.swing.JLabel();
        txtParamVal_Music = new javax.swing.JTextField();
        Settings_Misc = new javax.swing.JPanel();
        jLabel41 = new javax.swing.JLabel();
        spinPlayerCache_Misc = new javax.swing.JSpinner();
        cbxPreCache_Misc = new javax.swing.JCheckBox();
        cbxBuffRead_Misc = new javax.swing.JCheckBox();
        cbxUnloadSys_Misc = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        jLabel42 = new javax.swing.JLabel();
        cmbxSFXBgMode_Misc = new javax.swing.JComboBox<String>();
        jLabel43 = new javax.swing.JLabel();
        cmbxBGMBgMode_Misc = new javax.swing.JComboBox<String>();
        cbxPauseOnDefocus_Misc = new javax.swing.JCheckBox();
        Settings_Arcade = new javax.swing.JPanel();
        cbxAIRandomCol_Arcade = new javax.swing.JCheckBox();
        cbxAICheat_Arcade = new javax.swing.JCheckBox();
        jScrollPane7 = new javax.swing.JScrollPane();
        tblAIRamp_Arcade = new javax.swing.JTable();
        jScrollPane8 = new javax.swing.JScrollPane();
        jLabel51 = new javax.swing.JLabel();
        Settings_Input = new javax.swing.JTabbedPane();
        Settings_Input_P1Key = new javax.swing.JPanel();
        cbxP1KeyEnable_Input = new javax.swing.JCheckBox();
        jLabel39 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        jLabel54 = new javax.swing.JLabel();
        jLabel55 = new javax.swing.JLabel();
        jLabel56 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        jLabel58 = new javax.swing.JLabel();
        jLabel59 = new javax.swing.JLabel();
        jLabel60 = new javax.swing.JLabel();
        txtP1KeyUp_Input = new javax.swing.JTextField();
        txtP1KeyDown_Input = new javax.swing.JTextField();
        txtP1KeyLeft_Input = new javax.swing.JTextField();
        txtP1KeyRight_Input = new javax.swing.JTextField();
        txtP1KeyA_Input = new javax.swing.JTextField();
        txtP1KeyB_Input = new javax.swing.JTextField();
        txtP1KeyC_Input = new javax.swing.JTextField();
        txtP1KeyX_Input = new javax.swing.JTextField();
        txtP1KeyY_Input = new javax.swing.JTextField();
        txtP1KeyZ_Input = new javax.swing.JTextField();
        txtP1KeyStart_Input = new javax.swing.JTextField();
        Settings_Input_P2Key = new javax.swing.JPanel();
        cbxP2KeyEnable_Input = new javax.swing.JCheckBox();
        jLabel45 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jLabel61 = new javax.swing.JLabel();
        jLabel62 = new javax.swing.JLabel();
        jLabel63 = new javax.swing.JLabel();
        jLabel64 = new javax.swing.JLabel();
        jLabel65 = new javax.swing.JLabel();
        jLabel66 = new javax.swing.JLabel();
        jLabel67 = new javax.swing.JLabel();
        jLabel68 = new javax.swing.JLabel();
        jLabel69 = new javax.swing.JLabel();
        txtP2KeyStart_Input = new javax.swing.JTextField();
        txtP2KeyZ_Input = new javax.swing.JTextField();
        txtP2KeyY_Input = new javax.swing.JTextField();
        txtP2KeyX_Input = new javax.swing.JTextField();
        txtP2KeyC_Input = new javax.swing.JTextField();
        txtP2KeyB_Input = new javax.swing.JTextField();
        txtP2KeyA_Input = new javax.swing.JTextField();
        txtP2KeyRight_Input = new javax.swing.JTextField();
        txtP2KeyLeft_Input = new javax.swing.JTextField();
        txtP2KeyDown_Input = new javax.swing.JTextField();
        txtP2KeyUp_Input = new javax.swing.JTextField();
        Settings_Input_P1Joy = new javax.swing.JPanel();
        cbxP1JoyEnable_Input = new javax.swing.JCheckBox();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel70 = new javax.swing.JLabel();
        jLabel71 = new javax.swing.JLabel();
        jLabel72 = new javax.swing.JLabel();
        jLabel73 = new javax.swing.JLabel();
        jLabel74 = new javax.swing.JLabel();
        jLabel75 = new javax.swing.JLabel();
        jLabel76 = new javax.swing.JLabel();
        jLabel77 = new javax.swing.JLabel();
        jLabel78 = new javax.swing.JLabel();
        txtP1JoyStart_Input = new javax.swing.JTextField();
        txtP1JoyZ_Input = new javax.swing.JTextField();
        txtP1JoyY_Input = new javax.swing.JTextField();
        txtP1JoyX_Input = new javax.swing.JTextField();
        txtP1JoyC_Input = new javax.swing.JTextField();
        txtP1JoyB_Input = new javax.swing.JTextField();
        txtP1JoyA_Input = new javax.swing.JTextField();
        txtP1JoyRight_Input = new javax.swing.JTextField();
        txtP1JoyLeft_Input = new javax.swing.JTextField();
        txtP1JoyDown_Input = new javax.swing.JTextField();
        txtP1JoyUp_Input = new javax.swing.JTextField();
        Settings_Input_P2Joy = new javax.swing.JPanel();
        cbxP2JoyEnable_Input = new javax.swing.JCheckBox();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jLabel79 = new javax.swing.JLabel();
        jLabel80 = new javax.swing.JLabel();
        jLabel81 = new javax.swing.JLabel();
        jLabel82 = new javax.swing.JLabel();
        jLabel83 = new javax.swing.JLabel();
        jLabel84 = new javax.swing.JLabel();
        jLabel85 = new javax.swing.JLabel();
        jLabel86 = new javax.swing.JLabel();
        jLabel87 = new javax.swing.JLabel();
        txtP2JoyStart_Input = new javax.swing.JTextField();
        txtP2JoyZ_Input = new javax.swing.JTextField();
        txtP2JoyY_Input = new javax.swing.JTextField();
        txtP2JoyX_Input = new javax.swing.JTextField();
        txtP2JoyC_Input = new javax.swing.JTextField();
        txtP2JoyB_Input = new javax.swing.JTextField();
        txtP2JoyA_Input = new javax.swing.JTextField();
        txtP2JoyRight_Input = new javax.swing.JTextField();
        txtP2JoyLeft_Input = new javax.swing.JTextField();
        txtP2JoyDown_Input = new javax.swing.JTextField();
        txtP2JoyUp_Input = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        cbxInputDirMode_Input = new javax.swing.JCheckBox();
        cbxAnyKeyUnpauses_Input = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel3.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel4.text")); // NOI18N

        cmbxDifficulty_Options.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Easy 1", "Easy 2", "Medium 3", "Medium 4", "Medium 5", "Hard 6", "Hard 7", "Hard 8" }));
        cmbxDifficulty_Options.addActionListener(this);
        cmbxDifficulty_Options.setSelectedIndex(3);

        spinLife_Options.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(100), Integer.valueOf(0), null, Integer.valueOf(1)));
        spinLife_Options.addChangeListener(this);

        spinTime_Options.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(99), Integer.valueOf(0), null, Integer.valueOf(1)));
        spinTime_Options.addChangeListener(this);

        spinGameSpd_Options.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        spinGameSpd_Options.addChangeListener(this);

        Settings_Motif_Options.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Motif_Options.border.title"))); // NOI18N
        Settings_Motif_Options.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Motif_Options.toolTipText")); // NOI18N

        listMotif_Options.addListSelectionListener(this);
        listMotif_Options.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listMotif_Options.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.listMotif_Options.toolTipText")); // NOI18N
        jScrollPane1.setViewportView(listMotif_Options);

        javax.swing.GroupLayout Settings_Motif_OptionsLayout = new javax.swing.GroupLayout(Settings_Motif_Options);
        Settings_Motif_Options.setLayout(Settings_Motif_OptionsLayout);
        Settings_Motif_OptionsLayout.setHorizontalGroup(
            Settings_Motif_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Motif_OptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                .addContainerGap())
        );
        Settings_Motif_OptionsLayout.setVerticalGroup(
            Settings_Motif_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Motif_OptionsLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)
                .addContainerGap())
        );

        Settings_Team_Options.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Team_Options.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cbxTeamLoseOnKO_Options, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxTeamLoseOnKO_Options.text")); // NOI18N
        cbxTeamLoseOnKO_Options.addChangeListener(this);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel7.text")); // NOI18N

        spinTeam1v2Life_Options.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(150), Integer.valueOf(0), null, Integer.valueOf(1)));
        spinTeam1v2Life_Options.addChangeListener(this);

        javax.swing.GroupLayout Settings_Team_OptionsLayout = new javax.swing.GroupLayout(Settings_Team_Options);
        Settings_Team_Options.setLayout(Settings_Team_OptionsLayout);
        Settings_Team_OptionsLayout.setHorizontalGroup(
            Settings_Team_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Team_OptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_Team_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(Settings_Team_OptionsLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinTeam1v2Life_Options, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE))
                    .addComponent(cbxTeamLoseOnKO_Options))
                .addContainerGap())
        );
        Settings_Team_OptionsLayout.setVerticalGroup(
            Settings_Team_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Team_OptionsLayout.createSequentialGroup()
                .addGroup(Settings_Team_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(spinTeam1v2Life_Options, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbxTeamLoseOnKO_Options))
        );

        javax.swing.GroupLayout Settings_OptionsLayout = new javax.swing.GroupLayout(Settings_Options);
        Settings_Options.setLayout(Settings_OptionsLayout);
        Settings_OptionsLayout.setHorizontalGroup(
            Settings_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_OptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(Settings_OptionsLayout.createSequentialGroup()
                        .addComponent(Settings_Motif_Options, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(Settings_OptionsLayout.createSequentialGroup()
                        .addComponent(Settings_Team_Options, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(Settings_OptionsLayout.createSequentialGroup()
                        .addGroup(Settings_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(Settings_OptionsLayout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinGameSpd_Options, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE))
                            .addGroup(Settings_OptionsLayout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinTime_Options, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE))
                            .addGroup(Settings_OptionsLayout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinLife_Options, javax.swing.GroupLayout.DEFAULT_SIZE, 387, Short.MAX_VALUE))
                            .addGroup(Settings_OptionsLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbxDifficulty_Options, 0, 362, Short.MAX_VALUE)))
                        .addGap(10, 10, 10))))
        );
        Settings_OptionsLayout.setVerticalGroup(
            Settings_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_OptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(cmbxDifficulty_Options, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(spinLife_Options, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(spinTime_Options, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_OptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(spinGameSpd_Options, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Settings_Team_Options, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Settings_Motif_Options, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        settingsTabbedPane.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Options.TabConstraints.tabTitle"), Settings_Options); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel8.text")); // NOI18N
        jLabel8.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel8.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel9, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel9.text")); // NOI18N
        jLabel9.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel9.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel10, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel10.text")); // NOI18N
        jLabel10.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel10.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel11, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel11.text")); // NOI18N
        jLabel11.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel11.toolTipText")); // NOI18N

        txtGameType_Rules.setEditable(false);
        txtGameType_Rules.addActionListener(this);
        txtGameType_Rules.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtGameType_Rules.text")); // NOI18N
        txtGameType_Rules.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtGameType_Rules.toolTipText")); // NOI18N

        txtAtkL2PM_Rules.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtAtkL2PM_Rules.text")); // NOI18N
        txtAtkL2PM_Rules.addActionListener(this);
        txtAtkL2PM_Rules.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtAtkL2PM_Rules.toolTipText")); // NOI18N

        txtGetHitL2PM_Rules.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtGetHitL2PM_Rules.text")); // NOI18N
        txtGetHitL2PM_Rules.addActionListener(this);
        txtGetHitL2PM_Rules.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtGetHitL2PM_Rules.toolTipText")); // NOI18N

        txtSuperTgtDefM_Rules.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtSuperTgtDefM_Rules.text")); // NOI18N
        txtSuperTgtDefM_Rules.addActionListener(this);
        txtSuperTgtDefM_Rules.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtSuperTgtDefM_Rules.toolTipText")); // NOI18N

        javax.swing.GroupLayout Settings_RulesLayout = new javax.swing.GroupLayout(Settings_Rules);
        Settings_Rules.setLayout(Settings_RulesLayout);
        Settings_RulesLayout.setHorizontalGroup(
            Settings_RulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_RulesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_RulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(Settings_RulesLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtGameType_Rules, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE))
                    .addGroup(Settings_RulesLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtAtkL2PM_Rules, javax.swing.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE))
                    .addGroup(Settings_RulesLayout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtGetHitL2PM_Rules, javax.swing.GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE))
                    .addGroup(Settings_RulesLayout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSuperTgtDefM_Rules, javax.swing.GroupLayout.DEFAULT_SIZE, 253, Short.MAX_VALUE)))
                .addContainerGap())
        );
        Settings_RulesLayout.setVerticalGroup(
            Settings_RulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_RulesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_RulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(txtGameType_Rules, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_RulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtAtkL2PM_Rules, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_RulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(txtGetHitL2PM_Rules, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_RulesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(txtSuperTgtDefM_Rules, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(315, Short.MAX_VALUE))
        );

        settingsTabbedPane.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Rules.TabConstraints.tabTitle"), Settings_Rules); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel12, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel12.text")); // NOI18N
        jLabel12.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel12.toolTipText")); // NOI18N

        spinGameSpd_Config.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(60), Integer.valueOf(10), null, Integer.valueOf(1)));
        spinGameSpd_Config.addChangeListener(this);
        spinGameSpd_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinGameSpd_Config.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel13, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel13.text")); // NOI18N
        jLabel13.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel13.toolTipText")); // NOI18N

        cmbxGameRes_Config.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "640x480 SD 4:3", "1280x720 HD 16:9", "1920x1080 Full HD 16:9" }));
        cmbxGameRes_Config.addActionListener(this);
        cmbxGameRes_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cmbxGameRes_Config.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel14, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel14.text")); // NOI18N
        jLabel14.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel14.toolTipText")); // NOI18N

        txtLang_Config.addActionListener(this);
        txtLang_Config.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtLang_Config.text")); // NOI18N
        txtLang_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtLang_Config.toolTipText")); // NOI18N

        cbxDrawShadows_Config.setSelected(true);
        cbxDrawShadows_Config.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxDrawShadows_Config, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxDrawShadows_Config.text")); // NOI18N
        cbxDrawShadows_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxDrawShadows_Config.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel15, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel15.text")); // NOI18N
        jLabel15.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel15.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel16, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel16.text")); // NOI18N
        jLabel16.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel16.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel17, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel17.text")); // NOI18N
        jLabel17.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel17.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel18, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel18.text")); // NOI18N
        jLabel18.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel18.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel19, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel19.text")); // NOI18N
        jLabel19.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel19.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel20, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel20.text")); // NOI18N
        jLabel20.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel20.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel21, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel21.text")); // NOI18N
        jLabel21.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel21.toolTipText")); // NOI18N

        cbxFirstRun_Config.setSelected(true);
        cbxFirstRun_Config.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxFirstRun_Config, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxFirstRun_Config.text")); // NOI18N
        cbxFirstRun_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxFirstRun_Config.toolTipText")); // NOI18N

        spinMaxAfterImgs_Config.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(16), Integer.valueOf(1), null, Integer.valueOf(1)));
        spinMaxAfterImgs_Config.addChangeListener(this);
        spinMaxAfterImgs_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinMaxAfterImgs_Config.toolTipText")); // NOI18N

        spinMaxLayeredSprites_Config.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(256), Integer.valueOf(32), null, Integer.valueOf(1)));
        spinMaxLayeredSprites_Config.addChangeListener(this);
        spinMaxLayeredSprites_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinMaxLayeredSprites_Config.toolTipText")); // NOI18N

        spinSpriteDecomBuffSize_Config.setModel(new javax.swing.SpinnerNumberModel(Long.valueOf(16384L), Long.valueOf(256L), null, Long.valueOf(1L)));
        spinSpriteDecomBuffSize_Config.addChangeListener(this);
        spinSpriteDecomBuffSize_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinSpriteDecomBuffSize_Config.toolTipText")); // NOI18N

        spinMaxExplods_Config.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(256), Integer.valueOf(8), null, Integer.valueOf(1)));
        spinMaxExplods_Config.addChangeListener(this);
        spinMaxExplods_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinMaxExplods_Config.toolTipText")); // NOI18N

        spinMaxSysExplods_Config.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(128), Integer.valueOf(8), null, Integer.valueOf(1)));
        spinMaxSysExplods_Config.addChangeListener(this);
        spinMaxSysExplods_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinMaxSysExplods_Config.toolTipText")); // NOI18N

        spinMaxHelpers_Config.setModel(new javax.swing.SpinnerNumberModel(56, 4, 56, 1));
        spinMaxHelpers_Config.addChangeListener(this);
        spinMaxHelpers_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinMaxHelpers_Config.toolTipText")); // NOI18N

        spinMaxPlayerProj_Config.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(32), Integer.valueOf(5), null, Integer.valueOf(1)));
        spinMaxPlayerProj_Config.addChangeListener(this);
        spinMaxPlayerProj_Config.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinMaxPlayerProj_Config.toolTipText")); // NOI18N

        javax.swing.GroupLayout Settings_ConfigLayout = new javax.swing.GroupLayout(Settings_Config);
        Settings_Config.setLayout(Settings_ConfigLayout);
        Settings_ConfigLayout.setHorizontalGroup(
            Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_ConfigLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(Settings_ConfigLayout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinGameSpd_Config, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE))
                    .addGroup(Settings_ConfigLayout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbxGameRes_Config, 0, 324, Short.MAX_VALUE))
                    .addGroup(Settings_ConfigLayout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtLang_Config, javax.swing.GroupLayout.DEFAULT_SIZE, 357, Short.MAX_VALUE))
                    .addGroup(Settings_ConfigLayout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinMaxLayeredSprites_Config, javax.swing.GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE))
                    .addGroup(Settings_ConfigLayout.createSequentialGroup()
                        .addComponent(jLabel17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinSpriteDecomBuffSize_Config, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE))
                    .addGroup(Settings_ConfigLayout.createSequentialGroup()
                        .addComponent(jLabel18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinMaxExplods_Config, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE))
                    .addGroup(Settings_ConfigLayout.createSequentialGroup()
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinMaxSysExplods_Config, javax.swing.GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE))
                    .addGroup(Settings_ConfigLayout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinMaxHelpers_Config, javax.swing.GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE))
                    .addGroup(Settings_ConfigLayout.createSequentialGroup()
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinMaxAfterImgs_Config, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE))
                    .addGroup(Settings_ConfigLayout.createSequentialGroup()
                        .addComponent(jLabel21)
                        .addGap(0, 0, 0)
                        .addComponent(spinMaxPlayerProj_Config, javax.swing.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE))
                    .addComponent(cbxDrawShadows_Config)
                    .addComponent(cbxFirstRun_Config))
                .addContainerGap())
        );
        Settings_ConfigLayout.setVerticalGroup(
            Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_ConfigLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(spinGameSpd_Config, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(cmbxGameRes_Config, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(txtLang_Config, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbxDrawShadows_Config)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(spinMaxAfterImgs_Config, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(spinMaxLayeredSprites_Config, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(spinSpriteDecomBuffSize_Config, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(spinMaxExplods_Config, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(spinMaxSysExplods_Config, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(spinMaxHelpers_Config, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_ConfigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(spinMaxPlayerProj_Config, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbxFirstRun_Config)
                .addContainerGap(113, Short.MAX_VALUE))
        );

        settingsTabbedPane.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Config.TabConstraints.tabTitle"), Settings_Config); // NOI18N

        cbxDebug_Debug.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxDebug_Debug, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxDebug_Debug.text")); // NOI18N
        cbxDebug_Debug.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxDebug_Debug.toolTipText")); // NOI18N

        cbxAllowDebugMode_Debug.setSelected(true);
        cbxAllowDebugMode_Debug.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxAllowDebugMode_Debug, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxAllowDebugMode_Debug.text")); // NOI18N
        cbxAllowDebugMode_Debug.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxAllowDebugMode_Debug.toolTipText")); // NOI18N

        cbxAllowDebugKeys_Debug.setSelected(true);
        cbxAllowDebugKeys_Debug.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxAllowDebugKeys_Debug, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxAllowDebugKeys_Debug.text")); // NOI18N
        cbxAllowDebugKeys_Debug.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxAllowDebugKeys_Debug.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel22, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel22.text")); // NOI18N
        jLabel22.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel22.toolTipText")); // NOI18N

        spinSpdUp_Debug.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        spinSpdUp_Debug.addChangeListener(this);
        spinSpdUp_Debug.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinSpdUp_Debug.toolTipText")); // NOI18N

        Settings_StartStage_Debug.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_StartStage_Debug.border.title"))); // NOI18N
        Settings_StartStage_Debug.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_StartStage_Debug.toolTipText")); // NOI18N

        listStartStage_Debug.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listStartStage_Debug.addListSelectionListener(this);
        listStartStage_Debug.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.listStartStage_Debug.toolTipText")); // NOI18N
        jScrollPane2.setViewportView(listStartStage_Debug);

        javax.swing.GroupLayout Settings_StartStage_DebugLayout = new javax.swing.GroupLayout(Settings_StartStage_Debug);
        Settings_StartStage_Debug.setLayout(Settings_StartStage_DebugLayout);
        Settings_StartStage_DebugLayout.setHorizontalGroup(
            Settings_StartStage_DebugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_StartStage_DebugLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        Settings_StartStage_DebugLayout.setVerticalGroup(
            Settings_StartStage_DebugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_StartStage_DebugLayout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 317, Short.MAX_VALUE)
                .addContainerGap())
        );

        org.openide.awt.Mnemonics.setLocalizedText(cbxHideDevBuildBanner_Debug, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxHideDevBuildBanner_Debug.text")); // NOI18N

        javax.swing.GroupLayout Settings_DebugLayout = new javax.swing.GroupLayout(Settings_Debug);
        Settings_Debug.setLayout(Settings_DebugLayout);
        Settings_DebugLayout.setHorizontalGroup(
            Settings_DebugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_DebugLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_DebugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Settings_StartStage_Debug, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(Settings_DebugLayout.createSequentialGroup()
                        .addComponent(cbxDebug_Debug)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxAllowDebugMode_Debug)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxAllowDebugKeys_Debug)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxHideDevBuildBanner_Debug)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(Settings_DebugLayout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinSpdUp_Debug)))
                .addContainerGap())
        );
        Settings_DebugLayout.setVerticalGroup(
            Settings_DebugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_DebugLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_DebugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbxDebug_Debug)
                    .addComponent(cbxAllowDebugMode_Debug)
                    .addComponent(cbxAllowDebugKeys_Debug)
                    .addComponent(cbxHideDevBuildBanner_Debug))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_DebugLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(spinSpdUp_Debug, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Settings_StartStage_Debug, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        settingsTabbedPane.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Debug.TabConstraints.tabTitle"), Settings_Debug); // NOI18N

        Settings_Resolution_Video.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Resolution_Video.border.title"))); // NOI18N

        listRes_Video.addListSelectionListener(this);
        listRes_Video.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(listRes_Video);

        javax.swing.GroupLayout Settings_Resolution_VideoLayout = new javax.swing.GroupLayout(Settings_Resolution_Video);
        Settings_Resolution_Video.setLayout(Settings_Resolution_VideoLayout);
        Settings_Resolution_VideoLayout.setHorizontalGroup(
            Settings_Resolution_VideoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Resolution_VideoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3)
                .addContainerGap())
        );
        Settings_Resolution_VideoLayout.setVerticalGroup(
            Settings_Resolution_VideoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Resolution_VideoLayout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                .addContainerGap())
        );

        cbxFullscreen_Video.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxFullscreen_Video, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxFullscreen_Video.text")); // NOI18N
        cbxFullscreen_Video.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxFullscreen_Video.toolTipText")); // NOI18N

        cbxVertRetrace_Video.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxVertRetrace_Video, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxVertRetrace_Video.text")); // NOI18N
        cbxVertRetrace_Video.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxVertRetrace_Video.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel23, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel23.text")); // NOI18N
        jLabel23.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel23.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel24, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel24.text")); // NOI18N
        jLabel24.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel24.toolTipText")); // NOI18N

        cmbxBlitMode_Video.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Normal", "PageFlip" }));
        cmbxBlitMode_Video.addActionListener(this);
        cmbxBlitMode_Video.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cmbxBlitMode_Video.toolTipText")); // NOI18N

        cmbxRenderMode_Video.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "OpenGL", "System", "DirectX" }));
        cmbxRenderMode_Video.addActionListener(this);
        cmbxRenderMode_Video.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cmbxRenderMode_Video.toolTipText")); // NOI18N

        cmbxDepth_Video.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "16", "32" }));
        cmbxDepth_Video.addActionListener(this);
        cmbxDepth_Video.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cmbxDepth_Video.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel88, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel88.text")); // NOI18N
        jLabel88.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel88.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cbxSafeMode_Video, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxSafeMode_Video.text")); // NOI18N
        cbxSafeMode_Video.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxSafeMode_Video.toolTipText")); // NOI18N

        cbxResizable_Video.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(cbxResizable_Video, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxResizable_Video.text")); // NOI18N
        cbxResizable_Video.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxResizable_Video.toolTipText")); // NOI18N

        cbxKeepAspect_Video.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(cbxKeepAspect_Video, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxKeepAspect_Video.text")); // NOI18N
        cbxKeepAspect_Video.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxKeepAspect_Video.toolTipText")); // NOI18N

        cbxStageFit_Video.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(cbxStageFit_Video, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxStageFit_Video.text")); // NOI18N
        cbxStageFit_Video.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxStageFit_Video.toolTipText")); // NOI18N

        cbxSystemFit_Video.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(cbxSystemFit_Video, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxSystemFit_Video.text")); // NOI18N
        cbxSystemFit_Video.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxSystemFit_Video.toolTipText")); // NOI18N

        javax.swing.GroupLayout Settings_VideoLayout = new javax.swing.GroupLayout(Settings_Video);
        Settings_Video.setLayout(Settings_VideoLayout);
        Settings_VideoLayout.setHorizontalGroup(
            Settings_VideoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_VideoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_VideoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Settings_Resolution_Video, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(Settings_VideoLayout.createSequentialGroup()
                        .addComponent(jLabel24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbxRenderMode_Video, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(Settings_VideoLayout.createSequentialGroup()
                        .addComponent(jLabel23)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbxBlitMode_Video, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(Settings_VideoLayout.createSequentialGroup()
                        .addGroup(Settings_VideoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(Settings_VideoLayout.createSequentialGroup()
                                .addComponent(cbxFullscreen_Video)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxVertRetrace_Video)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxResizable_Video)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxKeepAspect_Video))
                            .addGroup(Settings_VideoLayout.createSequentialGroup()
                                .addComponent(cbxSafeMode_Video)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxStageFit_Video)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxSystemFit_Video)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel88)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbxDepth_Video, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 82, Short.MAX_VALUE)))
                .addContainerGap())
        );
        Settings_VideoLayout.setVerticalGroup(
            Settings_VideoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, Settings_VideoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Settings_Resolution_Video, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_VideoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbxFullscreen_Video)
                    .addComponent(cbxVertRetrace_Video)
                    .addComponent(cbxResizable_Video)
                    .addComponent(cbxKeepAspect_Video))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_VideoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbxDepth_Video, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel88)
                    .addComponent(cbxSafeMode_Video)
                    .addComponent(cbxStageFit_Video)
                    .addComponent(cbxSystemFit_Video))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_VideoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(cmbxBlitMode_Video, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_VideoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(cmbxRenderMode_Video, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        settingsTabbedPane.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Video.TabConstraints.tabTitle"), Settings_Video); // NOI18N

        cbxEnableSound_Sounds.setSelected(true);
        cbxEnableSound_Sounds.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxEnableSound_Sounds, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxEnableSound_Sounds.text")); // NOI18N
        cbxEnableSound_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxEnableSound_Sounds.toolTipText")); // NOI18N

        cbxStereoEfx_Sounds.setSelected(true);
        cbxStereoEfx_Sounds.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxStereoEfx_Sounds, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxStereoEfx_Sounds.text")); // NOI18N
        cbxStereoEfx_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxStereoEfx_Sounds.toolTipText")); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), cbxStereoEfx_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        cbxReverseStereo_Sounds.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxReverseStereo_Sounds, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxReverseStereo_Sounds.text")); // NOI18N
        cbxReverseStereo_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxReverseStereo_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), cbxReverseStereo_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel5.text")); // NOI18N
        jLabel5.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel5.toolTipText")); // NOI18N

        txtSampleRate_Sounds.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtSampleRate_Sounds.text")); // NOI18N
        txtSampleRate_Sounds.addActionListener(this);
        txtSampleRate_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtSampleRate_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtSampleRate_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel6.text")); // NOI18N
        jLabel6.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel6.toolTipText")); // NOI18N

        spinBuffSize_Sounds.setModel(new javax.swing.SpinnerNumberModel(11, 8, 15, 1));
        spinBuffSize_Sounds.addChangeListener(this);
        spinBuffSize_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinBuffSize_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), spinBuffSize_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel25, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel25.text")); // NOI18N
        jLabel25.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel25.toolTipText")); // NOI18N

        spinPanWidth_Sounds.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(240), Integer.valueOf(0), null, Integer.valueOf(1)));
        spinPanWidth_Sounds.addChangeListener(this);
        spinPanWidth_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinPanWidth_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), spinPanWidth_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel26, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel26.text")); // NOI18N
        jLabel26.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel26.toolTipText")); // NOI18N

        spinWaveChans_Sounds.setModel(new javax.swing.SpinnerNumberModel(12, 1, 16, 1));
        spinWaveChans_Sounds.addChangeListener(this);
        spinWaveChans_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinWaveChans_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), spinWaveChans_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        Settings_Volume_Sounds.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Volume_Sounds.border.title"))); // NOI18N
        Settings_Volume_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Volume_Sounds.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel27, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel27.text")); // NOI18N
        jLabel27.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel27.toolTipText")); // NOI18N

        spinMasterVol_Sounds.setModel(new javax.swing.SpinnerNumberModel(100, 0, 100, 1));
        spinMasterVol_Sounds.addChangeListener(this);
        spinMasterVol_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinMasterVol_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, slideMasterVol_Sounds, org.jdesktop.beansbinding.ELProperty.create("${value}"), spinMasterVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), spinMasterVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        slideMasterVol_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.slideMasterVol_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, spinMasterVol_Sounds, org.jdesktop.beansbinding.ELProperty.create("${value}"), slideMasterVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), slideMasterVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel28, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel28.text")); // NOI18N
        jLabel28.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel28.toolTipText")); // NOI18N

        spinWavVol_Sounds.setModel(new javax.swing.SpinnerNumberModel(80, 0, 100, 1));
        spinWavVol_Sounds.addChangeListener(this);
        spinWavVol_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinWavVol_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, slideWavVol_Sounds, org.jdesktop.beansbinding.ELProperty.create("${value}"), spinWavVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), spinWavVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        slideWavVol_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.slideWavVol_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, spinWavVol_Sounds, org.jdesktop.beansbinding.ELProperty.create("${value}"), slideWavVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), slideWavVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel29, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel29.text")); // NOI18N
        jLabel29.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel29.toolTipText")); // NOI18N

        spinBGMVol_Sounds.setModel(new javax.swing.SpinnerNumberModel(75, 0, 100, 1));
        spinBGMVol_Sounds.addChangeListener(this);
        spinBGMVol_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinBGMVol_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, slideBGMVol_Sounds, org.jdesktop.beansbinding.ELProperty.create("${value}"), spinBGMVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), spinBGMVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        slideBGMVol_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.slideBGMVol_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, spinBGMVol_Sounds, org.jdesktop.beansbinding.ELProperty.create("${value}"), slideBGMVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), slideBGMVol_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout Settings_Volume_SoundsLayout = new javax.swing.GroupLayout(Settings_Volume_Sounds);
        Settings_Volume_Sounds.setLayout(Settings_Volume_SoundsLayout);
        Settings_Volume_SoundsLayout.setHorizontalGroup(
            Settings_Volume_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Volume_SoundsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_Volume_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, Settings_Volume_SoundsLayout.createSequentialGroup()
                        .addComponent(jLabel27)
                        .addGap(7, 7, 7)
                        .addComponent(slideMasterVol_Sounds, javax.swing.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE))
                    .addGroup(Settings_Volume_SoundsLayout.createSequentialGroup()
                        .addGroup(Settings_Volume_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel28)
                            .addComponent(jLabel29))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(Settings_Volume_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(slideBGMVol_Sounds, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                            .addComponent(slideWavVol_Sounds, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Volume_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(spinBGMVol_Sounds)
                    .addComponent(spinWavVol_Sounds)
                    .addComponent(spinMasterVol_Sounds, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE))
                .addContainerGap())
        );
        Settings_Volume_SoundsLayout.setVerticalGroup(
            Settings_Volume_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Volume_SoundsLayout.createSequentialGroup()
                .addGroup(Settings_Volume_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spinMasterVol_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(slideMasterVol_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel27))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(Settings_Volume_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel28)
                    .addComponent(spinWavVol_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(slideWavVol_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(Settings_Volume_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel29)
                    .addComponent(spinBGMVol_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(slideBGMVol_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        Settings_Resample_Sound.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Resample_Sound.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel30, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel30.text")); // NOI18N
        jLabel30.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel30.toolTipText")); // NOI18N

        Settings_Resample_Quality_Sounds.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Resample_Quality_Sounds.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel31, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel31.text")); // NOI18N
        jLabel31.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel31.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel32, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel32.text")); // NOI18N
        jLabel32.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel32.toolTipText")); // NOI18N

        cmbxSFXQuality_Sounds.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Medium", "High" }));
        cmbxSFXQuality_Sounds.addActionListener(this);
        cmbxSFXQuality_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cmbxSFXQuality_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), cmbxSFXQuality_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        cmbxBGMQuality_Sounds.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Medium", "High" }));
        cmbxBGMQuality_Sounds.addActionListener(this);
        cmbxBGMQuality_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cmbxBGMQuality_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), cmbxBGMQuality_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout Settings_Resample_Quality_SoundsLayout = new javax.swing.GroupLayout(Settings_Resample_Quality_Sounds);
        Settings_Resample_Quality_Sounds.setLayout(Settings_Resample_Quality_SoundsLayout);
        Settings_Resample_Quality_SoundsLayout.setHorizontalGroup(
            Settings_Resample_Quality_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Resample_Quality_SoundsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_Resample_Quality_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(Settings_Resample_Quality_SoundsLayout.createSequentialGroup()
                        .addComponent(jLabel31)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbxSFXQuality_Sounds, 0, 322, Short.MAX_VALUE))
                    .addGroup(Settings_Resample_Quality_SoundsLayout.createSequentialGroup()
                        .addComponent(jLabel32)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbxBGMQuality_Sounds, 0, 319, Short.MAX_VALUE)))
                .addContainerGap())
        );
        Settings_Resample_Quality_SoundsLayout.setVerticalGroup(
            Settings_Resample_Quality_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Resample_Quality_SoundsLayout.createSequentialGroup()
                .addGroup(Settings_Resample_Quality_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel31)
                    .addComponent(cmbxSFXQuality_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Resample_Quality_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel32)
                    .addComponent(cmbxBGMQuality_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        cmbxSFXMethod_Sounds.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "SDL", "libresample" }));
        cmbxSFXMethod_Sounds.setSelectedIndex(1);
        cmbxSFXMethod_Sounds.addActionListener(this);
        cmbxSFXMethod_Sounds.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cmbxSFXMethod_Sounds.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxEnableSound_Sounds, org.jdesktop.beansbinding.ELProperty.create("${selected}"), cmbxSFXMethod_Sounds, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout Settings_Resample_SoundLayout = new javax.swing.GroupLayout(Settings_Resample_Sound);
        Settings_Resample_Sound.setLayout(Settings_Resample_SoundLayout);
        Settings_Resample_SoundLayout.setHorizontalGroup(
            Settings_Resample_SoundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Resample_SoundLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_Resample_SoundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Settings_Resample_Quality_Sounds, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(Settings_Resample_SoundLayout.createSequentialGroup()
                        .addComponent(jLabel30)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbxSFXMethod_Sounds, 0, 315, Short.MAX_VALUE)))
                .addContainerGap())
        );
        Settings_Resample_SoundLayout.setVerticalGroup(
            Settings_Resample_SoundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Resample_SoundLayout.createSequentialGroup()
                .addGroup(Settings_Resample_SoundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel30)
                    .addComponent(cmbxSFXMethod_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Settings_Resample_Quality_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout Settings_SoundsLayout = new javax.swing.GroupLayout(Settings_Sounds);
        Settings_Sounds.setLayout(Settings_SoundsLayout);
        Settings_SoundsLayout.setHorizontalGroup(
            Settings_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, Settings_SoundsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(Settings_Resample_Sound, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Settings_Volume_Sounds, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, Settings_SoundsLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtSampleRate_Sounds, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, Settings_SoundsLayout.createSequentialGroup()
                        .addComponent(cbxEnableSound_Sounds)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxStereoEfx_Sounds)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxReverseStereo_Sounds))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, Settings_SoundsLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinBuffSize_Sounds, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, Settings_SoundsLayout.createSequentialGroup()
                        .addComponent(jLabel25)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinPanWidth_Sounds, javax.swing.GroupLayout.DEFAULT_SIZE, 335, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, Settings_SoundsLayout.createSequentialGroup()
                        .addComponent(jLabel26)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinWaveChans_Sounds, javax.swing.GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE)))
                .addContainerGap())
        );
        Settings_SoundsLayout.setVerticalGroup(
            Settings_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_SoundsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbxEnableSound_Sounds)
                    .addComponent(cbxStereoEfx_Sounds)
                    .addComponent(cbxReverseStereo_Sounds))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtSampleRate_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(spinBuffSize_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel25)
                    .addComponent(spinPanWidth_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_SoundsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel26)
                    .addComponent(spinWaveChans_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Settings_Volume_Sounds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Settings_Resample_Sound, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(27, Short.MAX_VALUE))
        );

        settingsTabbedPane.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Sounds.TabConstraints.tabTitle"), Settings_Sounds); // NOI18N

        jSplitPane2.setDividerLocation(250);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        Settings_BuiltIn_Music.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_BuiltIn_Music.border.title"))); // NOI18N

        Settings_BuiltIn_mpg123_Music.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_BuiltIn_mpg123_Music.border.title"))); // NOI18N
        Settings_BuiltIn_mpg123_Music.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_BuiltIn_mpg123_Music.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel33, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel33.text")); // NOI18N
        jLabel33.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel33.toolTipText")); // NOI18N

        cmbxMpg123Decoder_Music.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "omit", "SSE", "3DNowExt", "3DNow", "MMX", "i586", "i586_dither", "i386", "generic", "generic_dither" }));
        cmbxMpg123Decoder_Music.addActionListener(this);
        cmbxMpg123Decoder_Music.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cmbxMpg123Decoder_Music.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel34, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel34.text")); // NOI18N
        jLabel34.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel34.toolTipText")); // NOI18N

        cmbxMpg123RVA_Music.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "off", "track", "album" }));
        cmbxMpg123RVA_Music.addActionListener(this);
        cmbxMpg123RVA_Music.setSelectedIndex(1);
        cmbxMpg123RVA_Music.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cmbxMpg123RVA_Music.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel35, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel35.text")); // NOI18N
        jLabel35.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel35.toolTipText")); // NOI18N

        txtMpg123Ext_Music.addActionListener(this);
        txtMpg123Ext_Music.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtMpg123Ext_Music.text")); // NOI18N
        txtMpg123Ext_Music.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtMpg123Ext_Music.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel44, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel44.text")); // NOI18N
        jLabel44.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel44.toolTipText")); // NOI18N

        spinMpg123Vol_Music.setModel(new javax.swing.SpinnerNumberModel(100, 0, 200, 1));
        spinMpg123Vol_Music.addChangeListener(this);
        spinMpg123Vol_Music.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinMpg123Vol_Music.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, slideMpg123Vol_Music, org.jdesktop.beansbinding.ELProperty.create("${value}"), spinMpg123Vol_Music, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        slideMpg123Vol_Music.setMaximum(200);
        slideMpg123Vol_Music.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.slideMpg123Vol_Music.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, spinMpg123Vol_Music, org.jdesktop.beansbinding.ELProperty.create("${value}"), slideMpg123Vol_Music, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout Settings_BuiltIn_mpg123_MusicLayout = new javax.swing.GroupLayout(Settings_BuiltIn_mpg123_Music);
        Settings_BuiltIn_mpg123_Music.setLayout(Settings_BuiltIn_mpg123_MusicLayout);
        Settings_BuiltIn_mpg123_MusicLayout.setHorizontalGroup(
            Settings_BuiltIn_mpg123_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createSequentialGroup()
                        .addComponent(jLabel33)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbxMpg123Decoder_Music, 0, 262, Short.MAX_VALUE))
                    .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createSequentialGroup()
                        .addComponent(jLabel34)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbxMpg123RVA_Music, 0, 282, Short.MAX_VALUE))
                    .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createSequentialGroup()
                        .addComponent(jLabel35)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtMpg123Ext_Music, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE))
                    .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createSequentialGroup()
                        .addComponent(jLabel44)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(slideMpg123Vol_Music, javax.swing.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinMpg123Vol_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        Settings_BuiltIn_mpg123_MusicLayout.setVerticalGroup(
            Settings_BuiltIn_mpg123_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createSequentialGroup()
                .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel33)
                    .addComponent(cmbxMpg123Decoder_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel34)
                    .addComponent(cmbxMpg123RVA_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel35)
                    .addComponent(txtMpg123Ext_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(Settings_BuiltIn_mpg123_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel44)
                        .addComponent(spinMpg123Vol_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(slideMpg123Vol_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        Settings_BuiltIn_sdlMix_Music.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_BuiltIn_sdlMix_Music.border.title"))); // NOI18N
        Settings_BuiltIn_sdlMix_Music.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_BuiltIn_sdlMix_Music.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel36, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel36.text")); // NOI18N
        jLabel36.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel36.toolTipText")); // NOI18N

        spinSdlMixMidiVol_Music.setModel(new javax.swing.SpinnerNumberModel(100, 0, 200, 1));
        spinSdlMixMidiVol_Music.addChangeListener(this);
        spinSdlMixMidiVol_Music.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinSdlMixMidiVol_Music.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, slideSdlMixMidiVol_Music, org.jdesktop.beansbinding.ELProperty.create("${value}"), spinSdlMixMidiVol_Music, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        slideSdlMixMidiVol_Music.setMaximum(200);
        slideSdlMixMidiVol_Music.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.slideSdlMixMidiVol_Music.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, spinSdlMixMidiVol_Music, org.jdesktop.beansbinding.ELProperty.create("${value}"), slideSdlMixMidiVol_Music, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel37, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel37.text")); // NOI18N
        jLabel37.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel37.toolTipText")); // NOI18N

        spinSdlMixModVol_Music.setModel(new javax.swing.SpinnerNumberModel(100, 0, 200, 1));
        spinSdlMixModVol_Music.addChangeListener(this);
        spinSdlMixModVol_Music.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinSdlMixModVol_Music.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, slideSdlMixModVol_Music, org.jdesktop.beansbinding.ELProperty.create("${value}"), spinSdlMixModVol_Music, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        slideSdlMixModVol_Music.setMaximum(200);
        slideSdlMixModVol_Music.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.slideSdlMixModVol_Music.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, spinSdlMixModVol_Music, org.jdesktop.beansbinding.ELProperty.create("${value}"), slideSdlMixModVol_Music, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout Settings_BuiltIn_sdlMix_MusicLayout = new javax.swing.GroupLayout(Settings_BuiltIn_sdlMix_Music);
        Settings_BuiltIn_sdlMix_Music.setLayout(Settings_BuiltIn_sdlMix_MusicLayout);
        Settings_BuiltIn_sdlMix_MusicLayout.setHorizontalGroup(
            Settings_BuiltIn_sdlMix_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_BuiltIn_sdlMix_MusicLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_BuiltIn_sdlMix_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(Settings_BuiltIn_sdlMix_MusicLayout.createSequentialGroup()
                        .addComponent(jLabel36)
                        .addGap(6, 6, 6)
                        .addComponent(slideSdlMixMidiVol_Music, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE))
                    .addGroup(Settings_BuiltIn_sdlMix_MusicLayout.createSequentialGroup()
                        .addComponent(jLabel37)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(slideSdlMixModVol_Music, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_BuiltIn_sdlMix_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(spinSdlMixModVol_Music)
                    .addComponent(spinSdlMixMidiVol_Music))
                .addContainerGap())
        );
        Settings_BuiltIn_sdlMix_MusicLayout.setVerticalGroup(
            Settings_BuiltIn_sdlMix_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_BuiltIn_sdlMix_MusicLayout.createSequentialGroup()
                .addComponent(spinSdlMixMidiVol_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(spinSdlMixModVol_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(Settings_BuiltIn_sdlMix_MusicLayout.createSequentialGroup()
                .addGroup(Settings_BuiltIn_sdlMix_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel36)
                    .addComponent(slideSdlMixMidiVol_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addGroup(Settings_BuiltIn_sdlMix_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(slideSdlMixModVol_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel37)))
        );

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Settings_BuiltIn_mpg123_Music, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Settings_BuiltIn_sdlMix_Music, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Settings_BuiltIn_mpg123_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Settings_BuiltIn_sdlMix_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane4.setViewportView(jPanel9);

        javax.swing.GroupLayout Settings_BuiltIn_MusicLayout = new javax.swing.GroupLayout(Settings_BuiltIn_Music);
        Settings_BuiltIn_Music.setLayout(Settings_BuiltIn_MusicLayout);
        Settings_BuiltIn_MusicLayout.setHorizontalGroup(
            Settings_BuiltIn_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_BuiltIn_MusicLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
                .addContainerGap())
        );
        Settings_BuiltIn_MusicLayout.setVerticalGroup(
            Settings_BuiltIn_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_BuiltIn_MusicLayout.createSequentialGroup()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane2.setBottomComponent(Settings_BuiltIn_Music);

        jSplitPane1.setDividerLocation(200);

        Settings_AvaliablePlugins_Music.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_AvaliablePlugins_Music.border.title"))); // NOI18N

        jScrollPane5.setViewportView(listAvailablePlugins_Music);

        org.openide.awt.Mnemonics.setLocalizedText(btnAddPlugins, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.btnAddPlugins.text")); // NOI18N
        btnAddPlugins.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddPluginsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout Settings_AvaliablePlugins_MusicLayout = new javax.swing.GroupLayout(Settings_AvaliablePlugins_Music);
        Settings_AvaliablePlugins_Music.setLayout(Settings_AvaliablePlugins_MusicLayout);
        Settings_AvaliablePlugins_MusicLayout.setHorizontalGroup(
            Settings_AvaliablePlugins_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_AvaliablePlugins_MusicLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_AvaliablePlugins_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
                    .addComponent(btnAddPlugins))
                .addContainerGap())
        );
        Settings_AvaliablePlugins_MusicLayout.setVerticalGroup(
            Settings_AvaliablePlugins_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, Settings_AvaliablePlugins_MusicLayout.createSequentialGroup()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnAddPlugins)
                .addContainerGap())
        );

        jSplitPane1.setLeftComponent(Settings_AvaliablePlugins_Music);

        Settings_CurrentPlugins_Music.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_CurrentPlugins_Music.border.title"))); // NOI18N

        listCurrPlugins_Music.setModel(this.listCurrPluginsModel);
        listCurrPlugins_Music.addListSelectionListener(this);
        listCurrPlugins_Music.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane6.setViewportView(listCurrPlugins_Music);

        org.openide.awt.Mnemonics.setLocalizedText(btnRmPlugins_Music, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.btnRmPlugins_Music.text")); // NOI18N
        btnRmPlugins_Music.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRmPlugins_MusicActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jPanel1.border.title"))); // NOI18N

        cmbxPluginParams_Music.setModel(this.cmbxPluginParamsModel);

        org.openide.awt.Mnemonics.setLocalizedText(btnAddParam_Music, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.btnAddParam_Music.text")); // NOI18N
        btnAddParam_Music.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddParam_MusicActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btnRemoveParam_Music, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.btnRemoveParam_Music.text")); // NOI18N
        btnRemoveParam_Music.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveParam_MusicActionPerformed(evt);
            }
        });

        txtParamKey_Music.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtParamKey_Music.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel38, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel38.text")); // NOI18N

        txtParamVal_Music.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtParamVal_Music.text")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(txtParamKey_Music, javax.swing.GroupLayout.DEFAULT_SIZE, 16, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel38)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtParamVal_Music, javax.swing.GroupLayout.DEFAULT_SIZE, 17, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnAddParam_Music))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(cmbxPluginParams_Music, 0, 29, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRemoveParam_Music)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtParamKey_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddParam_Music)
                    .addComponent(txtParamVal_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel38))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbxPluginParams_Music, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRemoveParam_Music)))
        );

        javax.swing.GroupLayout Settings_CurrentPlugins_MusicLayout = new javax.swing.GroupLayout(Settings_CurrentPlugins_Music);
        Settings_CurrentPlugins_Music.setLayout(Settings_CurrentPlugins_MusicLayout);
        Settings_CurrentPlugins_MusicLayout.setHorizontalGroup(
            Settings_CurrentPlugins_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, Settings_CurrentPlugins_MusicLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_CurrentPlugins_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                    .addComponent(btnRmPlugins_Music, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        Settings_CurrentPlugins_MusicLayout.setVerticalGroup(
            Settings_CurrentPlugins_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, Settings_CurrentPlugins_MusicLayout.createSequentialGroup()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnRmPlugins_Music)
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(Settings_CurrentPlugins_Music);

        jSplitPane2.setLeftComponent(jSplitPane1);

        javax.swing.GroupLayout Settings_MusicLayout = new javax.swing.GroupLayout(Settings_Music);
        Settings_Music.setLayout(Settings_MusicLayout);
        Settings_MusicLayout.setHorizontalGroup(
            Settings_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2)
        );
        Settings_MusicLayout.setVerticalGroup(
            Settings_MusicLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2)
        );

        settingsTabbedPane.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Music.TabConstraints.tabTitle"), Settings_Music); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel41, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel41.text")); // NOI18N
        jLabel41.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel41.toolTipText")); // NOI18N

        spinPlayerCache_Misc.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(4), Integer.valueOf(1), null, Integer.valueOf(1)));
        spinPlayerCache_Misc.addChangeListener(this);
        spinPlayerCache_Misc.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.spinPlayerCache_Misc.toolTipText")); // NOI18N

        cbxPreCache_Misc.setSelected(true);
        cbxPreCache_Misc.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxPreCache_Misc, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxPreCache_Misc.text")); // NOI18N
        cbxPreCache_Misc.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxPreCache_Misc.toolTipText")); // NOI18N

        cbxBuffRead_Misc.setSelected(true);
        cbxBuffRead_Misc.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxBuffRead_Misc, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxBuffRead_Misc.text")); // NOI18N
        cbxBuffRead_Misc.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxBuffRead_Misc.toolTipText")); // NOI18N

        cbxUnloadSys_Misc.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxUnloadSys_Misc, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxUnloadSys_Misc.text")); // NOI18N
        cbxUnloadSys_Misc.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxUnloadSys_Misc.toolTipText")); // NOI18N

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jPanel2.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel42, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel42.text")); // NOI18N

        cmbxSFXBgMode_Misc.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Mute", "Play" }));
        cmbxSFXBgMode_Misc.addActionListener(this);
        cmbxSFXBgMode_Misc.setSelectedIndex(1);
        cmbxSFXBgMode_Misc.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cmbxSFXBgMode_Misc.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel43, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel43.text")); // NOI18N

        cmbxBGMBgMode_Misc.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Pause", "Mute", "Play" }));
        cmbxBGMBgMode_Misc.addActionListener(this);
        cmbxBGMBgMode_Misc.setSelectedIndex(2);
        cmbxBGMBgMode_Misc.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cmbxBGMBgMode_Misc.toolTipText")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel42)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbxSFXBgMode_Misc, 0, 354, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel43)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbxBGMBgMode_Misc, 0, 351, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel42)
                    .addComponent(cmbxSFXBgMode_Misc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel43)
                    .addComponent(cmbxBGMBgMode_Misc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        org.openide.awt.Mnemonics.setLocalizedText(cbxPauseOnDefocus_Misc, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxPauseOnDefocus_Misc.text")); // NOI18N

        javax.swing.GroupLayout Settings_MiscLayout = new javax.swing.GroupLayout(Settings_Misc);
        Settings_Misc.setLayout(Settings_MiscLayout);
        Settings_MiscLayout.setHorizontalGroup(
            Settings_MiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_MiscLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_MiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(Settings_MiscLayout.createSequentialGroup()
                        .addComponent(jLabel41)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinPlayerCache_Misc))
                    .addGroup(Settings_MiscLayout.createSequentialGroup()
                        .addComponent(cbxPreCache_Misc)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxBuffRead_Misc)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxUnloadSys_Misc)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxPauseOnDefocus_Misc)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        Settings_MiscLayout.setVerticalGroup(
            Settings_MiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_MiscLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_MiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel41)
                    .addComponent(spinPlayerCache_Misc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_MiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbxPreCache_Misc)
                    .addComponent(cbxBuffRead_Misc)
                    .addComponent(cbxUnloadSys_Misc)
                    .addComponent(cbxPauseOnDefocus_Misc))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(293, Short.MAX_VALUE))
        );

        settingsTabbedPane.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Misc.TabConstraints.tabTitle"), Settings_Misc); // NOI18N

        cbxAIRandomCol_Arcade.setSelected(true);
        cbxAIRandomCol_Arcade.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxAIRandomCol_Arcade, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxAIRandomCol_Arcade.text")); // NOI18N
        cbxAIRandomCol_Arcade.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxAIRandomCol_Arcade.toolTipText")); // NOI18N

        cbxAICheat_Arcade.setSelected(true);
        cbxAICheat_Arcade.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxAICheat_Arcade, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxAICheat_Arcade.text")); // NOI18N
        cbxAICheat_Arcade.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxAICheat_Arcade.toolTipText")); // NOI18N

        tblAIRamp_Arcade.setModel(this.tblAIRampModel);
        jScrollPane7.setViewportView(tblAIRamp_Arcade);

        jScrollPane8.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jScrollPane8.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel51, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel51.text")); // NOI18N
        jLabel51.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jScrollPane8.setViewportView(jLabel51);

        javax.swing.GroupLayout Settings_ArcadeLayout = new javax.swing.GroupLayout(Settings_Arcade);
        Settings_Arcade.setLayout(Settings_ArcadeLayout);
        Settings_ArcadeLayout.setHorizontalGroup(
            Settings_ArcadeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, Settings_ArcadeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_ArcadeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, Settings_ArcadeLayout.createSequentialGroup()
                        .addComponent(cbxAIRandomCol_Arcade)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cbxAICheat_Arcade)))
                .addContainerGap())
        );
        Settings_ArcadeLayout.setVerticalGroup(
            Settings_ArcadeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_ArcadeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_ArcadeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbxAIRandomCol_Arcade)
                    .addComponent(cbxAICheat_Arcade))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                .addContainerGap())
        );

        settingsTabbedPane.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Arcade.TabConstraints.tabTitle"), Settings_Arcade); // NOI18N

        cbxP1KeyEnable_Input.setSelected(true);
        cbxP1KeyEnable_Input.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxP1KeyEnable_Input, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxP1KeyEnable_Input.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel39, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel39.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel40, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel40.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel52, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel52.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel53, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel53.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel54, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel54.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel55, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel55.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel56, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel56.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel57, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel57.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel58, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel58.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel59, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel59.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel60, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel60.text")); // NOI18N

        txtP1KeyUp_Input.addActionListener(this);
        txtP1KeyUp_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1KeyUp_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1KeyUp_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyUp_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyUp_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1KeyDown_Input.addActionListener(this);
        txtP1KeyDown_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1KeyDown_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1KeyDown_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyDown_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyDown_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1KeyLeft_Input.addActionListener(this);
        txtP1KeyLeft_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1KeyLeft_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1KeyLeft_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyLeft_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyLeft_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1KeyRight_Input.addActionListener(this);
        txtP1KeyRight_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1KeyRight_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1KeyRight_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyRight_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyRight_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1KeyA_Input.addActionListener(this);
        txtP1KeyA_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1KeyA_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1KeyA_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyA_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyA_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1KeyB_Input.addActionListener(this);
        txtP1KeyB_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1KeyB_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1KeyB_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyB_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyB_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1KeyC_Input.addActionListener(this);
        txtP1KeyC_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1KeyC_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1KeyC_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyC_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyC_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1KeyX_Input.addActionListener(this);
        txtP1KeyX_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1KeyX_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1KeyX_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyX_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyX_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1KeyY_Input.addActionListener(this);
        txtP1KeyY_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1KeyY_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1KeyY_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyY_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyY_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1KeyZ_Input.addActionListener(this);
        txtP1KeyZ_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1KeyZ_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1KeyZ_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyZ_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyZ_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1KeyStart_Input.addActionListener(this);
        txtP1KeyStart_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1KeyStart_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1KeyStart_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyStart_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1KeyStart_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout Settings_Input_P1KeyLayout = new javax.swing.GroupLayout(Settings_Input_P1Key);
        Settings_Input_P1Key.setLayout(Settings_Input_P1KeyLayout);
        Settings_Input_P1KeyLayout.setHorizontalGroup(
            Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Input_P1KeyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbxP1KeyEnable_Input)
                    .addGroup(Settings_Input_P1KeyLayout.createSequentialGroup()
                        .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel40)
                            .addComponent(jLabel39)
                            .addComponent(jLabel52)
                            .addComponent(jLabel53)
                            .addComponent(jLabel54)
                            .addGroup(Settings_Input_P1KeyLayout.createSequentialGroup()
                                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel56)
                                    .addComponent(jLabel55)
                                    .addComponent(jLabel57)
                                    .addComponent(jLabel58)
                                    .addComponent(jLabel59)
                                    .addComponent(jLabel60))
                                .addGap(1, 1, 1)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtP1KeyDown_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1KeyUp_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1KeyLeft_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1KeyRight_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1KeyA_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1KeyB_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1KeyC_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1KeyX_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1KeyY_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1KeyZ_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1KeyStart_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE))))
                .addContainerGap())
        );
        Settings_Input_P1KeyLayout.setVerticalGroup(
            Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Input_P1KeyLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cbxP1KeyEnable_Input)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel39)
                    .addComponent(txtP1KeyUp_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel40)
                    .addComponent(txtP1KeyDown_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel52)
                    .addComponent(txtP1KeyLeft_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel53)
                    .addComponent(txtP1KeyRight_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel54)
                    .addComponent(txtP1KeyA_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel55)
                    .addComponent(txtP1KeyB_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel56)
                    .addComponent(txtP1KeyC_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel57)
                    .addComponent(txtP1KeyX_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel58)
                    .addComponent(txtP1KeyY_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel59)
                    .addComponent(txtP1KeyZ_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel60)
                    .addComponent(txtP1KeyStart_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(84, Short.MAX_VALUE))
        );

        Settings_Input.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Input_P1Key.TabConstraints.tabTitle"), Settings_Input_P1Key); // NOI18N

        cbxP2KeyEnable_Input.setSelected(true);
        cbxP2KeyEnable_Input.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxP2KeyEnable_Input, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxP2KeyEnable_Input.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel45, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel45.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel46, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel46.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel61, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel61.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel62, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel62.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel63, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel63.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel64, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel64.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel65, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel65.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel66, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel66.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel67, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel67.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel68, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel68.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel69, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel69.text")); // NOI18N

        txtP2KeyStart_Input.addActionListener(this);
        txtP2KeyStart_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2KeyStart_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2KeyStart_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyStart_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyStart_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2KeyZ_Input.addActionListener(this);
        txtP2KeyZ_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2KeyZ_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2KeyZ_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyZ_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyZ_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2KeyY_Input.addActionListener(this);
        txtP2KeyY_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2KeyY_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2KeyY_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyY_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyY_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2KeyX_Input.addActionListener(this);
        txtP2KeyX_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2KeyX_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2KeyX_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyX_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyX_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2KeyC_Input.addActionListener(this);
        txtP2KeyC_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2KeyC_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2KeyC_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyC_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyC_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2KeyB_Input.addActionListener(this);
        txtP2KeyB_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2KeyB_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2KeyB_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyB_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyB_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2KeyA_Input.addActionListener(this);
        txtP2KeyA_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2KeyA_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2KeyA_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyA_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyA_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2KeyRight_Input.addActionListener(this);
        txtP2KeyRight_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2KeyRight_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2KeyRight_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyRight_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyRight_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2KeyLeft_Input.addActionListener(this);
        txtP2KeyLeft_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2KeyLeft_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2KeyLeft_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyLeft_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyLeft_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2KeyDown_Input.addActionListener(this);
        txtP2KeyDown_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2KeyDown_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2KeyDown_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyDown_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyDown_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2KeyUp_Input.addActionListener(this);
        txtP2KeyUp_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2KeyUp_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2KeyUp_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyUp_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2KeyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2KeyUp_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout Settings_Input_P2KeyLayout = new javax.swing.GroupLayout(Settings_Input_P2Key);
        Settings_Input_P2Key.setLayout(Settings_Input_P2KeyLayout);
        Settings_Input_P2KeyLayout.setHorizontalGroup(
            Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Input_P2KeyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbxP2KeyEnable_Input)
                    .addGroup(Settings_Input_P2KeyLayout.createSequentialGroup()
                        .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel46)
                            .addComponent(jLabel45)
                            .addComponent(jLabel61)
                            .addComponent(jLabel62)
                            .addComponent(jLabel63)
                            .addGroup(Settings_Input_P2KeyLayout.createSequentialGroup()
                                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel65)
                                    .addComponent(jLabel64)
                                    .addComponent(jLabel66)
                                    .addComponent(jLabel67)
                                    .addComponent(jLabel68)
                                    .addComponent(jLabel69))
                                .addGap(1, 1, 1)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtP2KeyDown_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2KeyUp_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2KeyLeft_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2KeyRight_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2KeyA_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2KeyB_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2KeyC_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2KeyX_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2KeyY_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2KeyZ_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2KeyStart_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE))))
                .addContainerGap())
        );
        Settings_Input_P2KeyLayout.setVerticalGroup(
            Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Input_P2KeyLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cbxP2KeyEnable_Input)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel45)
                    .addComponent(txtP2KeyUp_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel46)
                    .addComponent(txtP2KeyDown_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel61)
                    .addComponent(txtP2KeyLeft_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel62)
                    .addComponent(txtP2KeyRight_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel63)
                    .addComponent(txtP2KeyA_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel64)
                    .addComponent(txtP2KeyB_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel65)
                    .addComponent(txtP2KeyC_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel66)
                    .addComponent(txtP2KeyX_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel67)
                    .addComponent(txtP2KeyY_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel68)
                    .addComponent(txtP2KeyZ_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2KeyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel69)
                    .addComponent(txtP2KeyStart_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(84, Short.MAX_VALUE))
        );

        Settings_Input.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Input_P2Key.TabConstraints.tabTitle"), Settings_Input_P2Key); // NOI18N

        cbxP1JoyEnable_Input.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxP1JoyEnable_Input, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxP1JoyEnable_Input.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel47, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel47.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel48, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel48.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel70, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel70.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel71, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel71.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel72, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel72.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel73, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel73.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel74, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel74.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel75, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel75.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel76, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel76.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel77, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel77.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel78, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel78.text")); // NOI18N

        txtP1JoyStart_Input.addActionListener(this);
        txtP1JoyStart_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1JoyStart_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1JoyStart_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyStart_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyStart_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1JoyZ_Input.addActionListener(this);
        txtP1JoyZ_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1JoyZ_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1JoyZ_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyZ_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyZ_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1JoyY_Input.addActionListener(this);
        txtP1JoyY_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1JoyY_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1JoyY_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyY_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyY_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1JoyX_Input.addActionListener(this);
        txtP1JoyX_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1JoyX_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1JoyX_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyX_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyX_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1JoyC_Input.addActionListener(this);
        txtP1JoyC_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1JoyC_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1JoyC_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyC_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyC_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1JoyB_Input.addActionListener(this);
        txtP1JoyB_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1JoyB_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1JoyB_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyB_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyB_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1JoyA_Input.addActionListener(this);
        txtP1JoyA_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1JoyA_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1JoyA_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyA_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyA_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1JoyRight_Input.addActionListener(this);
        txtP1JoyRight_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1JoyRight_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1JoyRight_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyRight_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyRight_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1JoyLeft_Input.addActionListener(this);
        txtP1JoyLeft_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1JoyLeft_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1JoyLeft_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyLeft_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyLeft_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1JoyDown_Input.addActionListener(this);
        txtP1JoyDown_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1JoyDown_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1JoyDown_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyDown_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyDown_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP1JoyUp_Input.addActionListener(this);
        txtP1JoyUp_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP1JoyUp_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP1JoyUp_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyUp_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP1JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP1JoyUp_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout Settings_Input_P1JoyLayout = new javax.swing.GroupLayout(Settings_Input_P1Joy);
        Settings_Input_P1Joy.setLayout(Settings_Input_P1JoyLayout);
        Settings_Input_P1JoyLayout.setHorizontalGroup(
            Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Input_P1JoyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbxP1JoyEnable_Input)
                    .addGroup(Settings_Input_P1JoyLayout.createSequentialGroup()
                        .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel48)
                            .addComponent(jLabel47)
                            .addComponent(jLabel70)
                            .addComponent(jLabel71)
                            .addComponent(jLabel72)
                            .addGroup(Settings_Input_P1JoyLayout.createSequentialGroup()
                                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel74)
                                    .addComponent(jLabel73)
                                    .addComponent(jLabel75)
                                    .addComponent(jLabel76)
                                    .addComponent(jLabel77)
                                    .addComponent(jLabel78))
                                .addGap(1, 1, 1)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtP1JoyDown_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1JoyUp_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1JoyLeft_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1JoyRight_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1JoyA_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1JoyB_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1JoyC_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1JoyX_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1JoyY_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1JoyZ_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP1JoyStart_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE))))
                .addContainerGap())
        );
        Settings_Input_P1JoyLayout.setVerticalGroup(
            Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Input_P1JoyLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cbxP1JoyEnable_Input)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel47)
                    .addComponent(txtP1JoyUp_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel48)
                    .addComponent(txtP1JoyDown_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel70)
                    .addComponent(txtP1JoyLeft_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel71)
                    .addComponent(txtP1JoyRight_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel72)
                    .addComponent(txtP1JoyA_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel73)
                    .addComponent(txtP1JoyB_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel74)
                    .addComponent(txtP1JoyC_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel75)
                    .addComponent(txtP1JoyX_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel76)
                    .addComponent(txtP1JoyY_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel77)
                    .addComponent(txtP1JoyZ_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P1JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel78)
                    .addComponent(txtP1JoyStart_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(84, Short.MAX_VALUE))
        );

        Settings_Input.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Input_P1Joy.TabConstraints.tabTitle"), Settings_Input_P1Joy); // NOI18N

        cbxP2JoyEnable_Input.addChangeListener(this);
        org.openide.awt.Mnemonics.setLocalizedText(cbxP2JoyEnable_Input, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxP2JoyEnable_Input.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel49, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel49.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel50, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel50.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel79, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel79.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel80, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel80.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel81, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel81.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel82, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel82.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel83, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel83.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel84, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel84.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel85, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel85.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel86, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel86.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel87, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jLabel87.text")); // NOI18N

        txtP2JoyStart_Input.addActionListener(this);
        txtP2JoyStart_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2JoyStart_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2JoyStart_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyStart_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyStart_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2JoyZ_Input.addActionListener(this);
        txtP2JoyZ_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2JoyZ_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2JoyZ_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyZ_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyZ_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2JoyY_Input.addActionListener(this);
        txtP2JoyY_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2JoyY_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2JoyY_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyY_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyY_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2JoyX_Input.addActionListener(this);
        txtP2JoyX_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2JoyX_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2JoyX_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyX_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyX_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2JoyC_Input.addActionListener(this);
        txtP2JoyC_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2JoyC_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2JoyC_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyC_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyC_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2JoyB_Input.addActionListener(this);
        txtP2JoyB_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2JoyB_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2JoyB_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyB_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyB_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2JoyA_Input.addActionListener(this);
        txtP2JoyA_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2JoyA_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2JoyA_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyA_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyA_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2JoyRight_Input.addActionListener(this);
        txtP2JoyRight_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2JoyRight_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2JoyRight_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyRight_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyRight_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2JoyLeft_Input.addActionListener(this);
        txtP2JoyLeft_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2JoyLeft_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2JoyLeft_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyLeft_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyLeft_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2JoyDown_Input.addActionListener(this);
        txtP2JoyDown_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2JoyDown_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2JoyDown_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyDown_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyDown_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        txtP2JoyUp_Input.addActionListener(this);
        txtP2JoyUp_Input.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        txtP2JoyUp_Input.setText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.txtP2JoyUp_Input.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyUp_Input, org.jdesktop.beansbinding.BeanProperty.create("editable"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbxP2JoyEnable_Input, org.jdesktop.beansbinding.ELProperty.create("${selected}"), txtP2JoyUp_Input, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout Settings_Input_P2JoyLayout = new javax.swing.GroupLayout(Settings_Input_P2Joy);
        Settings_Input_P2Joy.setLayout(Settings_Input_P2JoyLayout);
        Settings_Input_P2JoyLayout.setHorizontalGroup(
            Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Input_P2JoyLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbxP2JoyEnable_Input)
                    .addGroup(Settings_Input_P2JoyLayout.createSequentialGroup()
                        .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel50)
                            .addComponent(jLabel49)
                            .addComponent(jLabel79)
                            .addComponent(jLabel80)
                            .addComponent(jLabel81)
                            .addGroup(Settings_Input_P2JoyLayout.createSequentialGroup()
                                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel83)
                                    .addComponent(jLabel82)
                                    .addComponent(jLabel84)
                                    .addComponent(jLabel85)
                                    .addComponent(jLabel86)
                                    .addComponent(jLabel87))
                                .addGap(1, 1, 1)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtP2JoyDown_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2JoyUp_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2JoyLeft_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2JoyRight_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2JoyA_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2JoyB_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2JoyC_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2JoyX_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2JoyY_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2JoyZ_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                            .addComponent(txtP2JoyStart_Input, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE))))
                .addContainerGap())
        );
        Settings_Input_P2JoyLayout.setVerticalGroup(
            Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Settings_Input_P2JoyLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cbxP2JoyEnable_Input)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel49)
                    .addComponent(txtP2JoyUp_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel50)
                    .addComponent(txtP2JoyDown_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel79)
                    .addComponent(txtP2JoyLeft_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel80)
                    .addComponent(txtP2JoyRight_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel81)
                    .addComponent(txtP2JoyA_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel82)
                    .addComponent(txtP2JoyB_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel83)
                    .addComponent(txtP2JoyC_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel84)
                    .addComponent(txtP2JoyX_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel85)
                    .addComponent(txtP2JoyY_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel86)
                    .addComponent(txtP2JoyZ_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(Settings_Input_P2JoyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel87)
                    .addComponent(txtP2JoyStart_Input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(84, Short.MAX_VALUE))
        );

        Settings_Input.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Input_P2Joy.TabConstraints.tabTitle"), Settings_Input_P2Joy); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cbxInputDirMode_Input, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxInputDirMode_Input.text")); // NOI18N
        cbxInputDirMode_Input.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxInputDirMode_Input.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cbxAnyKeyUnpauses_Input, org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxAnyKeyUnpauses_Input.text")); // NOI18N
        cbxAnyKeyUnpauses_Input.setToolTipText(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.cbxAnyKeyUnpauses_Input.toolTipText")); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cbxInputDirMode_Input)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbxAnyKeyUnpauses_Input)
                .addContainerGap(181, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbxInputDirMode_Input)
                    .addComponent(cbxAnyKeyUnpauses_Input))
                .addContainerGap(366, Short.MAX_VALUE))
        );

        Settings_Input.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.jPanel3.TabConstraints.tabTitle"), jPanel3); // NOI18N

        settingsTabbedPane.addTab(org.openide.util.NbBundle.getMessage(SettingsTopComponent.class, "SettingsTopComponent.Settings_Input.TabConstraints.tabTitle"), Settings_Input); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(settingsTabbedPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(settingsTabbedPane)
        );

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

private void btnAddPluginsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddPluginsActionPerformed
    int[] indexes = listAvailablePlugins_Music.getSelectedIndices();
    for (int i=0;i<indexes.length;i++) listCurrPluginsModel.addElement(listAvailablePluginsModel.getElementAt(indexes[i]));
    this.contentModified(true);
}//GEN-LAST:event_btnAddPluginsActionPerformed

private void btnRmPlugins_MusicActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRmPlugins_MusicActionPerformed
    this.finishedSettingUp = false;
    int index = listCurrPlugins_Music.getSelectedIndex();
    PluginDetails pluginDetails = (PluginDetails)listCurrPluginsModel.getElementAt(index);
    pluginDetails.clearParams();
    //listCurrPlugins_Music.setSelectedIndex(index-1);
    listCurrPlugins_Music.setSelectedValue(null, false);
    listCurrPluginsModel.remove(index);
    cmbxPluginParamsModel.removeAllElements();
    this.contentModified(true);
    this.finishedSettingUp = true;
}//GEN-LAST:event_btnRmPlugins_MusicActionPerformed

private void btnAddParam_MusicActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddParam_MusicActionPerformed
    PluginDetails.PluginParam param = new PluginDetails.PluginParam(txtParamKey_Music.getText(), txtParamVal_Music.getText());
    txtParamKey_Music.setText("");
    txtParamVal_Music.setText("");
    cmbxPluginParamsModel.addElement(param);
    PluginDetails pluginDetails = (PluginDetails)listCurrPluginsModel.getElementAt(listCurrPlugins_Music.getSelectedIndex());
    ArrayList<PluginDetails.PluginParam> params = pluginDetails.getPluginParams();
    params.add(param);
    this.contentModified(true);
}//GEN-LAST:event_btnAddParam_MusicActionPerformed

private void btnRemoveParam_MusicActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveParam_MusicActionPerformed
    this.finishedSettingUp = false;
    int index = cmbxPluginParams_Music.getSelectedIndex();
    //cmbxPluginParams_Music.setSelectedIndex(index-1);
    cmbxPluginParams_Music.setSelectedItem(null);
    cmbxPluginParamsModel.removeElementAt(index);
    PluginDetails pluginDetails = (PluginDetails)listCurrPluginsModel.getElementAt(listCurrPlugins_Music.getSelectedIndex());
    ArrayList<PluginDetails.PluginParam> params = pluginDetails.getPluginParams();
    params.remove(index);
    this.contentModified(true);
    this.finishedSettingUp = true;
}//GEN-LAST:event_btnRemoveParam_MusicActionPerformed

    //<editor-fold defaultstate="collapsed" desc="Variables">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Settings_Arcade;
    private javax.swing.JPanel Settings_AvaliablePlugins_Music;
    private javax.swing.JPanel Settings_BuiltIn_Music;
    private javax.swing.JPanel Settings_BuiltIn_mpg123_Music;
    private javax.swing.JPanel Settings_BuiltIn_sdlMix_Music;
    private javax.swing.JPanel Settings_Config;
    private javax.swing.JPanel Settings_CurrentPlugins_Music;
    private javax.swing.JPanel Settings_Debug;
    private javax.swing.JTabbedPane Settings_Input;
    private javax.swing.JPanel Settings_Input_P1Joy;
    private javax.swing.JPanel Settings_Input_P1Key;
    private javax.swing.JPanel Settings_Input_P2Joy;
    private javax.swing.JPanel Settings_Input_P2Key;
    private javax.swing.JPanel Settings_Misc;
    private javax.swing.JPanel Settings_Motif_Options;
    private javax.swing.JPanel Settings_Music;
    private javax.swing.JPanel Settings_Options;
    private javax.swing.JPanel Settings_Resample_Quality_Sounds;
    private javax.swing.JPanel Settings_Resample_Sound;
    private javax.swing.JPanel Settings_Resolution_Video;
    private javax.swing.JPanel Settings_Rules;
    private javax.swing.JPanel Settings_Sounds;
    private javax.swing.JPanel Settings_StartStage_Debug;
    private javax.swing.JPanel Settings_Team_Options;
    private javax.swing.JPanel Settings_Video;
    private javax.swing.JPanel Settings_Volume_Sounds;
    private javax.swing.JButton btnAddParam_Music;
    private javax.swing.JButton btnAddPlugins;
    private javax.swing.JButton btnRemoveParam_Music;
    private javax.swing.JButton btnRmPlugins_Music;
    private javax.swing.JCheckBox cbxAICheat_Arcade;
    private javax.swing.JCheckBox cbxAIRandomCol_Arcade;
    private javax.swing.JCheckBox cbxAllowDebugKeys_Debug;
    private javax.swing.JCheckBox cbxAllowDebugMode_Debug;
    private javax.swing.JCheckBox cbxAnyKeyUnpauses_Input;
    private javax.swing.JCheckBox cbxBuffRead_Misc;
    private javax.swing.JCheckBox cbxDebug_Debug;
    private javax.swing.JCheckBox cbxDrawShadows_Config;
    private javax.swing.JCheckBox cbxEnableSound_Sounds;
    private javax.swing.JCheckBox cbxFirstRun_Config;
    private javax.swing.JCheckBox cbxFullscreen_Video;
    private javax.swing.JCheckBox cbxHideDevBuildBanner_Debug;
    private javax.swing.JCheckBox cbxInputDirMode_Input;
    private javax.swing.JCheckBox cbxKeepAspect_Video;
    private javax.swing.JCheckBox cbxP1JoyEnable_Input;
    private javax.swing.JCheckBox cbxP1KeyEnable_Input;
    private javax.swing.JCheckBox cbxP2JoyEnable_Input;
    private javax.swing.JCheckBox cbxP2KeyEnable_Input;
    private javax.swing.JCheckBox cbxPauseOnDefocus_Misc;
    private javax.swing.JCheckBox cbxPreCache_Misc;
    private javax.swing.JCheckBox cbxResizable_Video;
    private javax.swing.JCheckBox cbxReverseStereo_Sounds;
    private javax.swing.JCheckBox cbxSafeMode_Video;
    private javax.swing.JCheckBox cbxStageFit_Video;
    private javax.swing.JCheckBox cbxStereoEfx_Sounds;
    private javax.swing.JCheckBox cbxSystemFit_Video;
    private javax.swing.JCheckBox cbxTeamLoseOnKO_Options;
    private javax.swing.JCheckBox cbxUnloadSys_Misc;
    private javax.swing.JCheckBox cbxVertRetrace_Video;
    private javax.swing.JComboBox cmbxBGMBgMode_Misc;
    private javax.swing.JComboBox cmbxBGMQuality_Sounds;
    private javax.swing.JComboBox cmbxBlitMode_Video;
    private javax.swing.JComboBox cmbxDepth_Video;
    private javax.swing.JComboBox cmbxDifficulty_Options;
    private javax.swing.JComboBox cmbxGameRes_Config;
    private javax.swing.JComboBox cmbxMpg123Decoder_Music;
    private javax.swing.JComboBox cmbxMpg123RVA_Music;
    private javax.swing.JComboBox cmbxPluginParams_Music;
    private javax.swing.JComboBox cmbxRenderMode_Video;
    private javax.swing.JComboBox cmbxSFXBgMode_Misc;
    private javax.swing.JComboBox cmbxSFXMethod_Sounds;
    private javax.swing.JComboBox cmbxSFXQuality_Sounds;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JLabel jLabel67;
    private javax.swing.JLabel jLabel68;
    private javax.swing.JLabel jLabel69;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel70;
    private javax.swing.JLabel jLabel71;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JLabel jLabel73;
    private javax.swing.JLabel jLabel74;
    private javax.swing.JLabel jLabel75;
    private javax.swing.JLabel jLabel76;
    private javax.swing.JLabel jLabel77;
    private javax.swing.JLabel jLabel78;
    private javax.swing.JLabel jLabel79;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel80;
    private javax.swing.JLabel jLabel81;
    private javax.swing.JLabel jLabel82;
    private javax.swing.JLabel jLabel83;
    private javax.swing.JLabel jLabel84;
    private javax.swing.JLabel jLabel85;
    private javax.swing.JLabel jLabel86;
    private javax.swing.JLabel jLabel87;
    private javax.swing.JLabel jLabel88;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JList listAvailablePlugins_Music;
    private javax.swing.JList listCurrPlugins_Music;
    private javax.swing.JList listMotif_Options;
    private javax.swing.JList listRes_Video;
    private javax.swing.JList listStartStage_Debug;
    private javax.swing.JTabbedPane settingsTabbedPane;
    private javax.swing.JSlider slideBGMVol_Sounds;
    private javax.swing.JSlider slideMasterVol_Sounds;
    private javax.swing.JSlider slideMpg123Vol_Music;
    private javax.swing.JSlider slideSdlMixMidiVol_Music;
    private javax.swing.JSlider slideSdlMixModVol_Music;
    private javax.swing.JSlider slideWavVol_Sounds;
    private javax.swing.JSpinner spinBGMVol_Sounds;
    private javax.swing.JSpinner spinBuffSize_Sounds;
    private javax.swing.JSpinner spinGameSpd_Config;
    private javax.swing.JSpinner spinGameSpd_Options;
    private javax.swing.JSpinner spinLife_Options;
    private javax.swing.JSpinner spinMasterVol_Sounds;
    private javax.swing.JSpinner spinMaxAfterImgs_Config;
    private javax.swing.JSpinner spinMaxExplods_Config;
    private javax.swing.JSpinner spinMaxHelpers_Config;
    private javax.swing.JSpinner spinMaxLayeredSprites_Config;
    private javax.swing.JSpinner spinMaxPlayerProj_Config;
    private javax.swing.JSpinner spinMaxSysExplods_Config;
    private javax.swing.JSpinner spinMpg123Vol_Music;
    private javax.swing.JSpinner spinPanWidth_Sounds;
    private javax.swing.JSpinner spinPlayerCache_Misc;
    private javax.swing.JSpinner spinSdlMixMidiVol_Music;
    private javax.swing.JSpinner spinSdlMixModVol_Music;
    private javax.swing.JSpinner spinSpdUp_Debug;
    private javax.swing.JSpinner spinSpriteDecomBuffSize_Config;
    private javax.swing.JSpinner spinTeam1v2Life_Options;
    private javax.swing.JSpinner spinTime_Options;
    private javax.swing.JSpinner spinWavVol_Sounds;
    private javax.swing.JSpinner spinWaveChans_Sounds;
    private javax.swing.JTable tblAIRamp_Arcade;
    private javax.swing.JTextField txtAtkL2PM_Rules;
    private javax.swing.JTextField txtGameType_Rules;
    private javax.swing.JTextField txtGetHitL2PM_Rules;
    private javax.swing.JTextField txtLang_Config;
    private javax.swing.JTextField txtMpg123Ext_Music;
    private javax.swing.JTextField txtP1JoyA_Input;
    private javax.swing.JTextField txtP1JoyB_Input;
    private javax.swing.JTextField txtP1JoyC_Input;
    private javax.swing.JTextField txtP1JoyDown_Input;
    private javax.swing.JTextField txtP1JoyLeft_Input;
    private javax.swing.JTextField txtP1JoyRight_Input;
    private javax.swing.JTextField txtP1JoyStart_Input;
    private javax.swing.JTextField txtP1JoyUp_Input;
    private javax.swing.JTextField txtP1JoyX_Input;
    private javax.swing.JTextField txtP1JoyY_Input;
    private javax.swing.JTextField txtP1JoyZ_Input;
    private javax.swing.JTextField txtP1KeyA_Input;
    private javax.swing.JTextField txtP1KeyB_Input;
    private javax.swing.JTextField txtP1KeyC_Input;
    private javax.swing.JTextField txtP1KeyDown_Input;
    private javax.swing.JTextField txtP1KeyLeft_Input;
    private javax.swing.JTextField txtP1KeyRight_Input;
    private javax.swing.JTextField txtP1KeyStart_Input;
    private javax.swing.JTextField txtP1KeyUp_Input;
    private javax.swing.JTextField txtP1KeyX_Input;
    private javax.swing.JTextField txtP1KeyY_Input;
    private javax.swing.JTextField txtP1KeyZ_Input;
    private javax.swing.JTextField txtP2JoyA_Input;
    private javax.swing.JTextField txtP2JoyB_Input;
    private javax.swing.JTextField txtP2JoyC_Input;
    private javax.swing.JTextField txtP2JoyDown_Input;
    private javax.swing.JTextField txtP2JoyLeft_Input;
    private javax.swing.JTextField txtP2JoyRight_Input;
    private javax.swing.JTextField txtP2JoyStart_Input;
    private javax.swing.JTextField txtP2JoyUp_Input;
    private javax.swing.JTextField txtP2JoyX_Input;
    private javax.swing.JTextField txtP2JoyY_Input;
    private javax.swing.JTextField txtP2JoyZ_Input;
    private javax.swing.JTextField txtP2KeyA_Input;
    private javax.swing.JTextField txtP2KeyB_Input;
    private javax.swing.JTextField txtP2KeyC_Input;
    private javax.swing.JTextField txtP2KeyDown_Input;
    private javax.swing.JTextField txtP2KeyLeft_Input;
    private javax.swing.JTextField txtP2KeyRight_Input;
    private javax.swing.JTextField txtP2KeyStart_Input;
    private javax.swing.JTextField txtP2KeyUp_Input;
    private javax.swing.JTextField txtP2KeyX_Input;
    private javax.swing.JTextField txtP2KeyY_Input;
    private javax.swing.JTextField txtP2KeyZ_Input;
    private javax.swing.JTextField txtParamKey_Music;
    private javax.swing.JTextField txtParamVal_Music;
    private javax.swing.JTextField txtSampleRate_Sounds;
    private javax.swing.JTextField txtSuperTgtDefM_Rules;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
    private DefaultListModel listMotifModel, listAvailablePluginsModel, listCurrPluginsModel, listResModel, listStartStageModel;
    private DefaultTableModel tblAIRampModel;
    private DefaultComboBoxModel cmbxPluginParamsModel;
    private InstanceContent content; //for saving system
    private boolean finishedSettingUp;
    private MugenDefFile mugenCfg;
    private String mugenCfgPath,pluginsPath,pluginsFolderName,stagesPath,motifsPath;
    private Preferences pref;
    private OptionSettings optionSettings;
    private RulesSettings rulesSettings;
    private ConfigSettings configSettings;
    private DebugSettings debugSettings;
    private VideoSettings videoSettings;
    private SoundSettings soundSettings;
    private MusicSettings musicSettings;
    private MiscSettings miscSettings;
    private ArcadeSettings arcadeSettings;
    private InputSettings inputSettings;
    //</editor-fold>
    
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

    //<editor-fold defaultstate="collapsed" desc="Listeners">
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!this.finishedSettingUp) return;
        this.checkContentModified();
    }
    
    @Override
    public void stateChanged(ChangeEvent e) {
        if (!this.finishedSettingUp) return;
        this.checkContentModified();
    }
    
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!this.finishedSettingUp) return;
        this.checkContentModified();
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (!this.finishedSettingUp) return;
        this.checkContentModified();
    }
    //</editor-fold>
    
    private void checkContentModified(){
        switch (this.settingsTabbedPane.getSelectedIndex()){
            case 0: //options
                if (this.optionSettings.checkContent()) this.contentModified(true);
                break;
            case 1: //rules
                if (this.rulesSettings.checkContent()) this.contentModified(true);
                break;
            case 2: //config
                if (this.configSettings.checkContent()) this.contentModified(true);
                break;
            case 3: //debug
                if (this.debugSettings.checkContent()) this.contentModified(true);
                break;
            case 4: //video
                if (this.videoSettings.checkContent()) this.contentModified(true);
                break;
            case 5: //sound
                if (this.soundSettings.checkContent()) this.contentModified(true);
                break;
            case 6: //music
                if (this.musicSettings.checkContent()) this.contentModified(true);
                break;
            case 7: //misc
                if (this.miscSettings.checkContent()) this.contentModified(true);
                break;
            case 8: //arcade
                if (this.arcadeSettings.checkContent()) this.contentModified(true);
                break;
            case 9: //input
                if (this.inputSettings.checkContent()) this.contentModified(true);
                break;
        }
    }
    
    private void contentModified(boolean modified){
        if (modified) this.content.add(this);
        else this.content.remove(this);
    }
    
    private String getKVString(MugenDefFile.KVEntry entry){
        if (entry.getValue() instanceof MugenDefFile.StringEntry) return ((MugenDefFile.StringEntry)entry.getValue()).getString();
        return "";
    }
    
    private String getListString(MugenDefFile.ListEntry entry, int index){
        MugenDefFile.Entry listEntry = entry.getEntry(index);
        if (listEntry instanceof MugenDefFile.StringEntry) return ((MugenDefFile.StringEntry)listEntry).getString();
        return "";
    }
    
    @Override
    public void save() throws IOException {
        ProgressHandle handle = ProgressHandleFactory.createHandle("Saving mugen.cfg.");
        handle.start(11);
        handle.progress("Saving Options Settings.",0);
        this.optionSettings.save();
        handle.progress("Saving Rules Settings.",1);
        this.rulesSettings.save();
        handle.progress("Saving Config Settings.",2);
        this.configSettings.save();
        handle.progress("Saving Debug Settings.",3);
        this.debugSettings.save();
        handle.progress("Saving Video Settings.",4);
        this.videoSettings.save();
        handle.progress("Saving Sound Settings.",5);
        this.soundSettings.save();
        handle.progress("Saving Music Settings.",6);
        this.musicSettings.save();
        handle.progress("Saving Misc Settings.",7);
        this.miscSettings.save();
        handle.progress("Saving Arcade Settings.",8);
        this.arcadeSettings.save();
        handle.progress("Saving Input Settings.",9);
        this.inputSettings.save();
        handle.progress("Writing mugen.cfg.",10);
        this.mugenCfg.write();
        handle.finish();
        this.contentModified(false);
        //throw new UnsupportedOperationException("Not supported yet.");
    }
    
    //<editor-fold defaultstate="collapsed" desc="Settings Classes">
    private class OptionSettings {
        private MugenDefFile.GroupEntry optionsGroup;
        private MugenDefFile.KVEntry difficulty,life,time,gamespd,team1v2life,teamloseonko,motif;
        private int difficultyInt,lifeInt,timeInt,gamespdInt,team1v2lifeInt, motifIndex;
        private boolean teamloseonkoBool;
        private String motifStr;
        
        public void setGroup(MugenDefFile.GroupEntry optionsGroup){
            this.optionsGroup = optionsGroup;
            this.reload();
        }
        
        private void reload(){
            for (int i=0;i<optionsGroup.size();i++){
                MugenDefFile.Entry entry = optionsGroup.getEntry(i);
                
                if (entry instanceof MugenDefFile.KVEntry){
                    MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry)entry;
                    if (kvEntry.getKey().equals("Difficulty")) this.difficulty = kvEntry;
                    else if (kvEntry.getKey().equals("Life")) this.life = kvEntry;
                    else if (kvEntry.getKey().equals("Time")) this.time = kvEntry;
                    else if (kvEntry.getKey().equals("GameSpeed")) this.gamespd = kvEntry;
                    else if (kvEntry.getKey().equals("Team.1VS2Life")) this.team1v2life = kvEntry;
                    else if (kvEntry.getKey().equals("Team.LoseOnKO")) this.teamloseonko = kvEntry;
                    else if (kvEntry.getKey().equals("motif")) this.motif = kvEntry;
                }
            }
            this.difficultyInt = Integer.parseInt(getKVString(this.difficulty));
            this.lifeInt = Integer.parseInt(getKVString(this.life));
            this.timeInt = Integer.parseInt(getKVString(this.time));
            this.gamespdInt = Integer.parseInt(getKVString(this.gamespd));
            this.team1v2lifeInt = Integer.parseInt(getKVString(this.team1v2life));
            if (Integer.parseInt(getKVString(this.teamloseonko))==1) this.teamloseonkoBool = true;
            else this.teamloseonkoBool = false;
            this.motifStr = getKVString(this.motif);
            this.motifIndex = 0;
            
            this.loadMotif();
            this.updateComponents();
        }
        
        private void updateComponents(){
            cmbxDifficulty_Options.setSelectedIndex(this.difficultyInt-1);
            spinLife_Options.setValue(this.lifeInt);
            spinTime_Options.setValue(this.timeInt);
            spinGameSpd_Options.setValue(this.gamespdInt);
            spinTeam1v2Life_Options.setValue(this.team1v2lifeInt);
            cbxTeamLoseOnKO_Options.setSelected(this.teamloseonkoBool);
        }
        
        private void loadMotif(){
            MugenDefFile.StringEntry valueEntry = (MugenDefFile.StringEntry)this.motif.getValue();
            
            String motifFolderStr = pref.get("Mugen_Motif_Folder", "data");
            String motifStr1 = "";
            String motifStr2 = "";
            
            File motifsFolder = new File(motifsPath);
            ArrayList<File> directories = new ArrayList<File>();
            listMotifModel.clear();
            listMotifModel.addElement("data/system.def");
            
            if (valueEntry.getString().equals("data/system.def") || valueEntry.getString().equals("data\\system.def")) listMotif_Options.setSelectedIndex(0);
            
            ProgressHandle dirScanProgHandle = ProgressHandleFactory.createHandle("Scanning the motifs directory.");
            ProgressHandle parseDirProgHandle = ProgressHandleFactory.createHandle("Parsing the motifs directory.");
            
            if (motifsFolder != null){
                File[] fileList = motifsFolder.listFiles();
                if (fileList != null){
                    dirScanProgHandle.start(fileList.length);
                    for (int i=0;i<fileList.length;i++){
                        if (fileList[i].isDirectory()) directories.add(fileList[i]);
                        dirScanProgHandle.progress(i);
                    }
                    dirScanProgHandle.finish();
                }
                
                parseDirProgHandle.start(directories.size());
                for (int i=0;i<directories.size();i++){
                    motifStr1 = motifFolderStr + "/" + directories.get(i).getName() + "/system.def";
                    motifStr2 = motifFolderStr + "\\" + directories.get(i).getName() + "\\system.def";
                    listMotifModel.addElement(motifStr1);
                    if (valueEntry.getString().equals(motifStr1) || valueEntry.getString().equals(motifStr2)){
                        listMotif_Options.setSelectedIndex(i+1);
                        this.motifIndex = i+1;
                    }
                    parseDirProgHandle.progress(i);
                }
                parseDirProgHandle.finish();
            }
        }
        
        public boolean checkContent(){
            boolean contentModified = false;
            if (cmbxDifficulty_Options.getSelectedIndex() != (this.difficultyInt-1)) contentModified=true;
            if (((Integer)spinLife_Options.getValue()) != this.lifeInt) contentModified=true;
            if (((Integer)spinTime_Options.getValue()) != this.timeInt) contentModified=true;
            if (((Integer)spinGameSpd_Options.getValue()) != this.gamespdInt) contentModified=true;
            if (((Integer)spinTeam1v2Life_Options.getValue()) != this.team1v2lifeInt) contentModified=true;
            if (cbxTeamLoseOnKO_Options.isSelected()) if (!this.teamloseonkoBool) contentModified = true;
            else if (this.teamloseonkoBool) contentModified = true;
            if (listMotif_Options.getSelectedIndex() != this.motifIndex) contentModified = true;
            return contentModified;
        }
        
        public void save(){
            this.difficultyInt = cmbxDifficulty_Options.getSelectedIndex()+1;
            this.lifeInt = (Integer)spinLife_Options.getValue();
            this.timeInt = (Integer)spinTime_Options.getValue();
            this.gamespdInt = (Integer)spinGameSpd_Options.getValue();
            this.team1v2lifeInt = (Integer)spinTeam1v2Life_Options.getValue();
            this.teamloseonkoBool = cbxTeamLoseOnKO_Options.isSelected();
            this.motifIndex = listMotif_Options.getSelectedIndex();
            this.motifStr = (String)listMotif_Options.getSelectedValue();
            
            this.difficulty.setValue(Integer.toString(this.difficultyInt));
            this.life.setValue(Integer.toString(this.lifeInt));
            this.time.setValue(Integer.toString(this.timeInt));
            this.gamespd.setValue(Integer.toString(this.gamespdInt));
            this.team1v2life.setValue(Integer.toString(this.team1v2lifeInt));
            if (this.teamloseonkoBool) this.teamloseonko.setValue("1");
            else this.teamloseonko.setValue("0");
            this.motif.setValue(this.motifStr);
        }
    }
    
    private class RulesSettings {
        private MugenDefFile.GroupEntry rulesGroup;
        private MugenDefFile.KVEntry gameType,atkLife2PwrMul,hitLife2PwrMul,superTgtDefMul;
        private String gameTypeStr,atkLife2PwrMulStr,hitLife2PwrMulStr,superTgtDefMulStr;
        
        public void setGroup(MugenDefFile.GroupEntry rulesGroup){
            this.rulesGroup = rulesGroup;
            this.reload();
        }
        
        private void reload(){
            for (int i=0;i<rulesGroup.size();i++){
                MugenDefFile.Entry entry = rulesGroup.getEntry(i);
                
                if (entry instanceof MugenDefFile.KVEntry){
                    MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry)entry;
                    if (kvEntry.getKey().equals("GameType")) this.gameType = kvEntry;
                    else if (kvEntry.getKey().equals("Default.Attack.LifeToPowerMul")) this.atkLife2PwrMul = kvEntry;
                    else if (kvEntry.getKey().equals("Default.GetHit.LifeToPowerMul")) this.hitLife2PwrMul = kvEntry;
                    else if (kvEntry.getKey().equals("Super.TargetDefenceMul")) this.superTgtDefMul = kvEntry;
                }
            }
            this.updateComponents();
        }
        
        private void updateComponents(){
            this.gameTypeStr = getKVString(this.gameType);
            this.atkLife2PwrMulStr = getKVString(this.atkLife2PwrMul);
            this.hitLife2PwrMulStr = getKVString(this.hitLife2PwrMul);
            this.superTgtDefMulStr = getKVString(this.superTgtDefMul);
            
            txtGameType_Rules.setText(this.gameTypeStr);
            txtAtkL2PM_Rules.setText(this.atkLife2PwrMulStr);
            txtGetHitL2PM_Rules.setText(this.hitLife2PwrMulStr);
            txtSuperTgtDefM_Rules.setText(this.superTgtDefMulStr);
        }
        
        public boolean checkContent(){
            boolean modified = false;
            if (!txtGameType_Rules.getText().equals(this.gameTypeStr)) modified = true;
            if (!txtAtkL2PM_Rules.getText().equals(this.atkLife2PwrMulStr)) modified = true;
            if (!txtGetHitL2PM_Rules.getText().equals(this.hitLife2PwrMulStr)) modified = true;
            if (!txtSuperTgtDefM_Rules.getText().equals(this.superTgtDefMulStr)) modified = true;
            return modified;
        }
        
        public void save(){
            this.gameTypeStr = txtGameType_Rules.getText();
            this.atkLife2PwrMulStr = txtAtkL2PM_Rules.getText();
            this.hitLife2PwrMulStr = txtGetHitL2PM_Rules.getText();
            this.superTgtDefMulStr = txtSuperTgtDefM_Rules.getText();
            
            this.gameType.setValue(this.gameTypeStr);
            this.atkLife2PwrMul.setValue(this.atkLife2PwrMulStr);
            this.hitLife2PwrMul.setValue(this.hitLife2PwrMulStr);
            this.superTgtDefMul.setValue(this.superTgtDefMulStr);
        }
    }
    
    private class ConfigSettings {
        private MugenDefFile.GroupEntry configGroup;
        private MugenDefFile.KVEntry
                    gameSpd,
                    gameWidth,
                    gameHeight,
                    lang,
                    drawShadows,
                    afterImgMax,
                    layeredSpriteMax,
                    spriteDecompBuffSize,
                    explodMax,
                    sysExplodMax,
                    helperMax,
                    playerProjMax,
                    firstRun;
        private int gameSpdInt, gameSizeIndex, afterImgMaxInt, layeredSpriteMaxInt, explodMaxInt, sysExplodMaxInt, helperMaxInt, playerProjMaxInt;
        private long spriteDecompBuffSizeLong;
        private String langStr;
        private boolean drawShadowsBool, firstRunBool;
        
        public void setGroup(MugenDefFile.GroupEntry configGroup){
            this.configGroup = configGroup;
            this.reload();
        }
        
        private void reload(){
            for (int i=0;i<configGroup.size();i++){
                MugenDefFile.Entry entry = configGroup.getEntry(i);
                
                if (entry instanceof MugenDefFile.KVEntry){
                    MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry)entry;
                    if (kvEntry.getKey().equals("GameSpeed")) this.gameSpd = kvEntry; //int
                    else if (kvEntry.getKey().equals("GameWidth")) this.gameWidth = kvEntry; //int
                    else if (kvEntry.getKey().equals("GameHeight")) this.gameHeight = kvEntry; //int
                    else if (kvEntry.getKey().equals("Language")) this.lang = kvEntry; //str
                    else if (kvEntry.getKey().equals("DrawShadows")) this.drawShadows = kvEntry; //bool
                    else if (kvEntry.getKey().equals("AfterImageMax")) this.afterImgMax = kvEntry; //int
                    else if (kvEntry.getKey().equals("LayeredSpriteMax")) this.layeredSpriteMax = kvEntry; //int
                    else if (kvEntry.getKey().equals("SpriteDecompressionBufferSize")) this.spriteDecompBuffSize = kvEntry; //long
                    else if (kvEntry.getKey().equals("ExplodMax")) this.explodMax = kvEntry; //int
                    else if (kvEntry.getKey().equals("SysExplodMax")) this.sysExplodMax = kvEntry; //int
                    else if (kvEntry.getKey().equals("HelperMax")) this.helperMax = kvEntry; //int
                    else if (kvEntry.getKey().equals("PlayerProjectileMax")) this.playerProjMax = kvEntry; //int
                    else if (kvEntry.getKey().equals("FirstRun")) this.firstRun = kvEntry; //bool
                }
            }
            this.updateComponents();
        }
        
        private void updateComponents(){
            int gameWidthInt, gameHeightInt;
            
            this.gameSpdInt = Integer.parseInt(getKVString(this.gameSpd));
            gameWidthInt = Integer.parseInt(getKVString(this.gameWidth));
            gameHeightInt = Integer.parseInt(getKVString(this.gameHeight));
            this.langStr = getKVString(this.lang);
            if (Integer.parseInt(getKVString(this.drawShadows)) == 0) this.drawShadowsBool = false;
            else this.drawShadowsBool = true;
            this.afterImgMaxInt = Integer.parseInt(getKVString(this.afterImgMax));
            this.layeredSpriteMaxInt = Integer.parseInt(getKVString(this.layeredSpriteMax));
            this.spriteDecompBuffSizeLong = Long.parseLong(getKVString(this.spriteDecompBuffSize));
            this.explodMaxInt = Integer.parseInt(getKVString(this.explodMax));
            this.sysExplodMaxInt = Integer.parseInt(getKVString(this.sysExplodMax));
            this.helperMaxInt = Integer.parseInt(getKVString(this.helperMax));
            this.playerProjMaxInt = Integer.parseInt(getKVString(this.playerProjMax));
            if (Integer.parseInt(getKVString(this.firstRun)) == 0) this.firstRunBool = false;
            else this.firstRunBool = true;
            
            spinGameSpd_Config.setValue(this.gameSpdInt);
            if (gameWidthInt == 640 && gameHeightInt == 480) this.gameSizeIndex=0;
            else if (gameWidthInt == 1280 && gameHeightInt == 720) this.gameSizeIndex=1;
            else if (gameWidthInt == 1920 && gameHeightInt == 1080) this.gameSizeIndex=2;
            cmbxGameRes_Config.setSelectedIndex(this.gameSizeIndex);
            txtLang_Config.setText(this.langStr);
            cbxDrawShadows_Config.setSelected(this.drawShadowsBool);
            spinMaxAfterImgs_Config.setValue(this.afterImgMaxInt);
            spinMaxLayeredSprites_Config.setValue(this.layeredSpriteMaxInt);
            spinSpriteDecomBuffSize_Config.setValue(this.spriteDecompBuffSizeLong);
            spinMaxExplods_Config.setValue(this.explodMaxInt);
            spinMaxSysExplods_Config.setValue(this.sysExplodMaxInt);
            spinMaxHelpers_Config.setValue(this.helperMaxInt);
            spinMaxPlayerProj_Config.setValue(this.playerProjMaxInt);
            cbxFirstRun_Config.setSelected(this.firstRunBool);
        }
        
        public boolean checkContent(){
            boolean modified = false;
            if (((Integer)spinGameSpd_Config.getValue()) != this.gameSpdInt) modified = true;
            if (((Integer)cmbxGameRes_Config.getSelectedIndex()) != this.gameSizeIndex) modified = true;
            if (!txtLang_Config.getText().equals(this.langStr)) modified = true;
            if (cbxDrawShadows_Config.isSelected() != this.drawShadowsBool) modified = true;
            if (((Integer)spinMaxAfterImgs_Config.getValue()) != this.afterImgMaxInt) modified = true;
            if (((Integer)spinMaxLayeredSprites_Config.getValue()) != this.layeredSpriteMaxInt) modified = true;
            if (((Long)spinSpriteDecomBuffSize_Config.getValue()) != this.spriteDecompBuffSizeLong) modified = true;
            if (((Integer)spinMaxExplods_Config.getValue()) != this.explodMaxInt) modified = true;
            if (((Integer)spinMaxSysExplods_Config.getValue()) != this.sysExplodMaxInt) modified = true;
            if (((Integer)spinMaxHelpers_Config.getValue()) != this.helperMaxInt) modified = true;
            if (((Integer)spinMaxPlayerProj_Config.getValue()) != this.playerProjMaxInt) modified = true;
            if (cbxFirstRun_Config.isSelected() != this.firstRunBool) modified = true;
            return modified;
        }
        
        public void save(){
            this.gameSpdInt = (Integer)spinGameSpd_Config.getValue();
            this.gameSizeIndex = cmbxGameRes_Config.getSelectedIndex();
            this.langStr = txtLang_Config.getText();
            this.drawShadowsBool = cbxDrawShadows_Config.isSelected();
            this.afterImgMaxInt = (Integer)spinMaxAfterImgs_Config.getValue();
            this.layeredSpriteMaxInt = (Integer)spinMaxLayeredSprites_Config.getValue();
            this.spriteDecompBuffSizeLong = (Long)spinSpriteDecomBuffSize_Config.getValue();
            this.explodMaxInt = (Integer)spinMaxExplods_Config.getValue();
            this.sysExplodMaxInt = (Integer)spinMaxSysExplods_Config.getValue();
            this.helperMaxInt = (Integer)spinMaxHelpers_Config.getValue();
            this.playerProjMaxInt = (Integer)spinMaxPlayerProj_Config.getValue();
            this.firstRunBool = cbxFirstRun_Config.isSelected();
            
            this.gameSpd.setValue(Integer.toString(this.gameSpdInt));
            
            switch(this.gameSizeIndex){
                case 0:
                    this.gameWidth.setValue("640");
                    this.gameHeight.setValue("480");
                    break;
                case 1:
                    this.gameWidth.setValue("1280");
                    this.gameHeight.setValue("720");
                    break;
                case 2:
                    this.gameWidth.setValue("1920");
                    this.gameHeight.setValue("1080");
                    break;
            }
                    
            this.lang.setValue(this.langStr);
            
            if (this.drawShadowsBool) this.drawShadows.setValue("1");
            else this.drawShadows.setValue("0");
            
            this.afterImgMax.setValue(Integer.toString(this.gameSpdInt));
            this.layeredSpriteMax.setValue(Integer.toString(this.gameSpdInt));
            this.spriteDecompBuffSize.setValue(Long.toString(0));
            this.explodMax.setValue(Integer.toString(this.gameSpdInt));
            this.sysExplodMax.setValue(Integer.toString(this.gameSpdInt));
            this.helperMax.setValue(Integer.toString(this.gameSpdInt));
            this.playerProjMax.setValue(Integer.toString(this.gameSpdInt));
            
            if (this.firstRunBool) this.firstRun.setValue("1");
            else this.firstRun.setValue("0");
        }
    }
    
    private class DebugSettings {
        private MugenDefFile.GroupEntry debugGroup;
        private MugenDefFile.KVEntry debug, allowDebugMode, allowDebugKeys, speedup, startstage,allowHideDevBuildBanner;
        private boolean debugBool, allowDebugModeBool, allowDebugKeysBool, allowHideDevBuildBannerBool;
        private int speedupInt, startstageInt;
        
        public void setGroup(MugenDefFile.GroupEntry debugGroup){
            this.debugGroup = debugGroup;
            this.reload();
        }
        
        private void reload(){
            for (int i=0;i<debugGroup.size();i++){
                MugenDefFile.Entry entry = debugGroup.getEntry(i);
                
                if (entry instanceof MugenDefFile.KVEntry){
                    MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry)entry;
                    if (kvEntry.getKey().equals("Debug")) this.debug = kvEntry; //bool
                    else if (kvEntry.getKey().equals("AllowDebugMode")) this.allowDebugMode = kvEntry; //bool
                    else if (kvEntry.getKey().equals("AllowDebugKeys")) this.allowDebugKeys = kvEntry; //bool
                    else if (kvEntry.getKey().equals("Speedup")) this.speedup = kvEntry; //int
                    else if (kvEntry.getKey().equals("StartStage")) this.startstage = kvEntry; //str
                    else if (kvEntry.getKey().equals("HideDevelopmentBuildBanner")) this.allowHideDevBuildBanner = kvEntry; //bool
                }
            }
            this.updateComponents();
        }
        
        private void updateComponents(){
            if (Integer.parseInt(getKVString(this.debug)) == 0) this.debugBool = false;
            else this.debugBool = true;
            if (Integer.parseInt(getKVString(this.allowDebugMode)) == 0) this.allowDebugKeysBool = false;
            else this.allowDebugKeysBool = true;
            if (Integer.parseInt(getKVString(this.allowDebugKeys)) == 0) this.allowDebugModeBool = false;
            else this.allowDebugModeBool = true;
            if (Integer.parseInt(getKVString(this.allowHideDevBuildBanner)) == 0) this.allowHideDevBuildBannerBool = false;
            else this.allowHideDevBuildBannerBool = true;
            this.speedupInt = Integer.parseInt(getKVString(this.speedup));
            
            cbxDebug_Debug.setSelected(this.debugBool);
            cbxAllowDebugKeys_Debug.setSelected(this.allowDebugKeysBool);
            cbxAllowDebugMode_Debug.setSelected(this.allowDebugModeBool);
            cbxHideDevBuildBanner_Debug.setSelected(allowHideDevBuildBannerBool);
            spinSpdUp_Debug.setValue(this.speedupInt);
            
            this.loadStages();
        }
        
        public boolean checkContent(){
            boolean modified = false;
            if (cbxDebug_Debug.isSelected() != this.debugBool) modified = true;
            if (cbxAllowDebugKeys_Debug.isSelected() != this.allowDebugKeysBool) modified = true;
            if (cbxAllowDebugMode_Debug.isSelected() != this.allowDebugModeBool) modified = true;
            if (cbxHideDevBuildBanner_Debug.isSelected() != this.allowHideDevBuildBannerBool) modified = true;
            if ((Integer)spinSpdUp_Debug.getValue() != this.speedupInt) modified = true;
            if (listStartStage_Debug.getSelectedIndex() != this.startstageInt) modified = true;
            return modified;
        }
        
        public void save(){
            this.debugBool = cbxDebug_Debug.isSelected();
            this.allowDebugKeysBool = cbxAllowDebugKeys_Debug.isSelected();
            this.allowDebugModeBool = cbxAllowDebugMode_Debug.isSelected();
            this.allowHideDevBuildBannerBool = cbxHideDevBuildBanner_Debug.isSelected();
            this.speedupInt = (Integer)spinSpdUp_Debug.getValue();
            this.startstageInt = listStartStage_Debug.getSelectedIndex();
            
            if (this.debugBool) this.debug.setValue("1");
            else this.debug.setValue("0");
            if (this.allowDebugKeysBool) this.allowDebugKeys.setValue("1");
            else this.allowDebugKeys.setValue("0");
            if (this.allowDebugModeBool) this.allowDebugMode.setValue("1");
            else this.allowDebugMode.setValue("0");
            if (this.allowHideDevBuildBannerBool) this.allowHideDevBuildBanner.setValue("1");
            else this.allowHideDevBuildBanner.setValue("0");
            
            this.speedup.setValue(Integer.toString((Integer)spinSpdUp_Debug.getValue()));
            this.startstage.setValue((String)listStartStageModel.getElementAt(this.startstageInt));
        }
        
        private void loadStages(){
            File stagesFolder = new File(stagesPath);
            listStartStageModel.clear();
            String startstageStr = getKVString(this.startstage);
            
            if (stagesFolder != null){
                File[] stagesList = stagesFolder.listFiles();
                
                ArrayList<String> stagesArrayList = new ArrayList<String>();
                
                for (int i=0;i<stagesList.length;i++){
                    File stageFile = stagesList[i];
                    if (stageFile.isFile() && stageFile.getName().endsWith(".def")){
                        stagesArrayList.add(stageFile.getName());
                        listStartStageModel.addElement("stages/"+stageFile.getName());
                    }
                }
                
                for (int i=0;i<stagesArrayList.size();i++){
                    String stageFolderName1 = "stages/"+stagesArrayList.get(i);
                    String stageFolderName2 = "stages\\"+stagesArrayList.get(i);
                    if (startstageStr.equals(stageFolderName1) || startstageStr.equals(stageFolderName2)){
                        listStartStage_Debug.setSelectedIndex(i);
                        this.startstageInt = i;
                    }
                }
            }
        }
    }

    private class VideoSettings {
        private MugenDefFile.GroupEntry videoGroup;
        private MugenDefFile.KVEntry width, height, depth, vretrace, fullscreen, blitmode, rendermode,safemode,resizable,keepaspect,stagefit,systemfit;
        private DisplayMode[] screenDisplayModes = null;
        private int screenResInt, blitModeInt, renderModeInt, depthInt;
        private boolean vretraceBool, fullscreenBool,safemodeBool,resizableBool,keepaspectBool,stagefitBool,systemfitBool;

        public VideoSettings(){
            this.updateResList();
        }

        public void setGroup(MugenDefFile.GroupEntry videoGroup){
            this.videoGroup = videoGroup;
            this.reload();
        }

        private void reload(){
            for (int i=0;i<videoGroup.size();i++){
                MugenDefFile.Entry entry = videoGroup.getEntry(i);

                if (entry instanceof MugenDefFile.KVEntry){
                    MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry)entry;
                    if (kvEntry.getKey().equals("Width")) this.width = kvEntry; //int
                    else if (kvEntry.getKey().equals("Height")) this.height = kvEntry; //int
                    else if (kvEntry.getKey().equals("Depth")) this.depth = kvEntry; //int
                    else if (kvEntry.getKey().equals("VRetrace")) this.vretrace = kvEntry; //bool
                    else if (kvEntry.getKey().equals("FullScreen")) this.fullscreen = kvEntry; //bool
                    else if (kvEntry.getKey().equals("BlitMode")) this.blitmode = kvEntry; //str
                    else if (kvEntry.getKey().equals("RenderMode")) this.rendermode = kvEntry; //str
                    else if (kvEntry.getKey().equals("SafeMode")) this.safemode = kvEntry; //bool
                    else if (kvEntry.getKey().equals("Resizable")) this.resizable = kvEntry; //bool
                    else if (kvEntry.getKey().equals("KeepAspect")) this.keepaspect = kvEntry; //bool
                    else if (kvEntry.getKey().equals("StageFit")) this.stagefit = kvEntry; //bool
                    else if (kvEntry.getKey().equals("SystemFit")) this.systemfit = kvEntry; //bool
                }
            }
            this.updateComponents();
        }

        private void updateComponents(){
            listRes_Video.setSelectedIndex(0);
            this.screenResInt = 0;
            if (this.width != null && this.height != null){
                int widthInt = Integer.parseInt(getKVString(this.width));
                int heightInt = Integer.parseInt(getKVString(this.height));

                //640x480 SD 4:3, 1280x720 HD 16:9, 1920x1080 Full HD 16:9
                if ((widthInt == 640 && heightInt == 480) || (widthInt == 1280 && heightInt == 720) ||(widthInt == 1920 && heightInt == 1080)){
                    listRes_Video.setSelectedIndex(0);
                    this.screenResInt = 0;
                }else{
                    for (int i=0;i<this.screenDisplayModes.length;i++){
                        if (this.screenDisplayModes[i].getWidth() == widthInt && this.screenDisplayModes[i].getHeight() == heightInt){
                            listRes_Video.setSelectedIndex(i);
                            this.screenResInt = i;
                            break;
                        }
                    }
                }
            }

            switch (Integer.parseInt(getKVString(this.depth))){
                case 16:
                    cmbxDepth_Video.setSelectedIndex(0);
                    this.depthInt = 0;
                    break;
                case 32:
                    cmbxDepth_Video.setSelectedIndex(1);
                    this.depthInt = 1;
                    break;
            }

            if (Integer.parseInt(getKVString(this.vretrace)) == 0) this.vretraceBool = false;
            else  this.vretraceBool = true;
            if (Integer.parseInt(getKVString(this.fullscreen)) == 0) this.fullscreenBool = false;
            else  this.fullscreenBool = true;

            if (Integer.parseInt(getKVString(this.safemode)) == 0) this.safemodeBool = false;
            else  this.safemodeBool = true;
            if (Integer.parseInt(getKVString(this.resizable)) == 0) this.resizableBool = false;
            else  this.resizableBool = true;
            if (Integer.parseInt(getKVString(this.keepaspect)) == 0) this.keepaspectBool = false;
            else  this.keepaspectBool = true;
            if (Integer.parseInt(getKVString(this.stagefit)) == 0) this.stagefitBool = false;
            else  this.stagefitBool = true;
            if (Integer.parseInt(getKVString(this.systemfit)) == 0) this.systemfitBool = false;
            else  this.systemfitBool = true;

            cbxVertRetrace_Video.setSelected(this.vretraceBool);
            cbxFullscreen_Video.setSelected(this.fullscreenBool);
            cbxSafeMode_Video.setSelected(this.safemodeBool);
            cbxResizable_Video.setSelected(this.resizableBool);
            cbxKeepAspect_Video.setSelected(this.keepaspectBool);
            cbxStageFit_Video.setSelected(this.stagefitBool);
            cbxSystemFit_Video.setSelected(this.systemfitBool);

            String blitModeStr = getKVString(this.blitmode);
            String renderModeStr = getKVString(this.rendermode);

            if (blitModeStr.equals("Normal")) this.blitModeInt = 0;
            else if (blitModeStr.equals("PageFlip")) this.blitModeInt = 1;
            cmbxBlitMode_Video.setSelectedIndex(this.blitModeInt);

            if (renderModeStr.equals("OpenGL")) this.renderModeInt = 0;
            else if (renderModeStr.equals("System")) this.renderModeInt = 1;
            else if (renderModeStr.equals("DirectX")) this.renderModeInt = 2;
            cmbxRenderMode_Video.setSelectedIndex(this.renderModeInt);

        }

        private void updateResList(){
            //<editor-fold defaultstate="collapsed" desc="Resolution List">
            listResModel = new DefaultListModel();
            listResModel.addElement("Use Resolution from Config Section.");

            GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
            DisplayMode[] tmp = screen.getDisplayModes();
            ArrayList<DisplayMode> tmpList = new ArrayList<DisplayMode>();

            boolean added = false;
            for (int i=0;i<tmp.length;i++){
                if (tmp[i].getBitDepth() == 16){
                    added = false;
                    for (int i2=0;i2<tmpList.size();i2++){
                        if (
                                tmp[i].getWidth() == tmpList.get(i2).getWidth() &&
                                tmp[i].getHeight() == tmpList.get(i2).getHeight()
                        ){
                            added = true;
                            break;
                        }
                    }
                    if (!added) tmpList.add(tmp[i]);
                }
            }

            this.screenDisplayModes = new DisplayMode[tmpList.size()+1];
            this.screenDisplayModes[0] = new DisplayMode(1,1,DisplayMode.BIT_DEPTH_MULTI,DisplayMode.REFRESH_RATE_UNKNOWN);

            for (int i=0;i<tmpList.size();i++) this.screenDisplayModes[i+1] = tmpList.get(i);

            for (int i=1;i<this.screenDisplayModes.length;i++) listResModel.addElement(this.screenDisplayModes[i].getWidth() + "x" + this.screenDisplayModes[i].getHeight());
            //</editor-fold>
        }

        public boolean checkContent(){
            boolean modified = false;
            if (listRes_Video.getSelectedIndex() != this.screenResInt) modified = true;
            if (cmbxDepth_Video.getSelectedIndex() != this.depthInt) modified = true;
            if (cbxVertRetrace_Video.isSelected() != this.vretraceBool) modified = true;
            if (cbxFullscreen_Video.isSelected() != this.fullscreenBool) modified = true;
            if (cmbxBlitMode_Video.getSelectedIndex() != this.blitModeInt) modified = true;
            if (cmbxRenderMode_Video.getSelectedIndex() != this.renderModeInt) modified = true;
            if (cbxSafeMode_Video.isSelected() != this.safemodeBool) modified = true;
            if (cbxResizable_Video.isSelected() != this.resizableBool) modified = true;
            if (cbxKeepAspect_Video.isSelected() != this.keepaspectBool) modified = true;
            if (cbxStageFit_Video.isSelected() != this.stagefitBool) modified = true;
            if (cbxSystemFit_Video.isSelected() != this.systemfitBool) modified = true;
            return modified;
        }

        public void save(){
            this.screenResInt = listRes_Video.getSelectedIndex();
            if (this.width != null && this.height != null){ //has the width and height
                if (listRes_Video.getSelectedIndex() != 0){ //use a res from the list in video.
                    this.width.setValue(Integer.toString(this.screenDisplayModes[listRes_Video.getSelectedIndex()].getWidth()));
                    this.height.setValue(Integer.toString(this.screenDisplayModes[listRes_Video.getSelectedIndex()].getHeight()));
                }else{ //use the res in the config section.
                    int widthInt=1, heightInt=1;
                    switch (cmbxGameRes_Config.getSelectedIndex()){
                        case 0:
                            widthInt = 640;
                            heightInt = 480;
                            break;
                        case 1:
                            widthInt = 1280;
                            heightInt = 720;
                            break;
                        case 2:
                            widthInt = 1920;
                            heightInt = 1080;
                            break;
                    }
                    this.width.setValue(Integer.toString(widthInt));
                    this.height.setValue(Integer.toString(heightInt));
                }
            }else{ //doesnt, add them
                if (listRes_Video.getSelectedIndex() != 0){ //use a res from the list in video.
                    this.width = new MugenDefFile.KVEntry("Width", Integer.toString(this.screenDisplayModes[listRes_Video.getSelectedIndex()].getWidth()));
                    this.height = new MugenDefFile.KVEntry("Height", Integer.toString(this.screenDisplayModes[listRes_Video.getSelectedIndex()].getHeight()));
                    this.videoGroup.addEntry(this.width);
                    this.videoGroup.addEntry(this.height);
                }else{ //use the res in the config section.
                    int widthInt=1, heightInt=1;
                    switch (cmbxGameRes_Config.getSelectedIndex()){
                        case 0:
                            widthInt = 640;
                            heightInt = 480;
                            break;
                        case 1:
                            widthInt = 1280;
                            heightInt = 720;
                            break;
                        case 2:
                            widthInt = 1920;
                            heightInt = 1080;
                            break;
                    }
                    this.width = new MugenDefFile.KVEntry("Width", Integer.toString(widthInt));
                    this.height = new MugenDefFile.KVEntry("Height", Integer.toString(heightInt));
                    this.videoGroup.addEntry(this.width);
                    this.videoGroup.addEntry(this.height);
                }
            }
            this.vretraceBool = cbxVertRetrace_Video.isSelected();
            this.fullscreenBool = cbxFullscreen_Video.isSelected();
            this.safemodeBool = cbxSafeMode_Video.isSelected();
            this.resizableBool = cbxResizable_Video.isSelected();
            this.keepaspectBool = cbxKeepAspect_Video.isSelected();
            this.stagefitBool = cbxStageFit_Video.isSelected();
            this.systemfitBool = cbxSystemFit_Video.isSelected();

            this.blitModeInt = cmbxBlitMode_Video.getSelectedIndex();
            this.renderModeInt = cmbxRenderMode_Video.getSelectedIndex();
            this.depthInt = cmbxDepth_Video.getSelectedIndex();

            switch (cmbxDepth_Video.getSelectedIndex()){
                case 0:
                    this.depth.setValue("16");
                    break;
                case 1:
                    this.depth.setValue("32");
                    break;
            }

            if (cbxVertRetrace_Video.isSelected()) this.vretrace.setValue("1");
            else  this.vretrace.setValue("0");
            if (cbxFullscreen_Video.isSelected()) this.fullscreen.setValue("1");
            else  this.fullscreen.setValue("0");
            if (cbxSafeMode_Video.isSelected()) this.safemode.setValue("1");
            else  this.safemode.setValue("0");
            if (cbxResizable_Video.isSelected()) this.resizable.setValue("1");
            else  this.resizable.setValue("0");
            if (cbxKeepAspect_Video.isSelected()) this.keepaspect.setValue("1");
            else  this.keepaspect.setValue("0");
            if (cbxStageFit_Video.isSelected()) this.stagefit.setValue("1");
            else  this.stagefit.setValue("0");
            if (cbxSystemFit_Video.isSelected()) this.systemfit.setValue("1");
            else  this.systemfit.setValue("0");

            switch (cmbxBlitMode_Video.getSelectedIndex()){
                case 0: //Normal
                    this.blitmode.setValue("Normal");
                    break;
                case 1: //PageFlip
                    this.blitmode.setValue("PageFlip");
                    break;
            }

            switch (cmbxRenderMode_Video.getSelectedIndex()){
                case 0: //OpenGL
                    this.rendermode.setValue("OpenGL");
                    break;
                case 1: //System
                    this.rendermode.setValue("System");
                    break;
                case 3: //DirectX
                    this.rendermode.setValue("DirectX");
                    break;
            }
        }
    }

    private class SoundSettings {
        private MugenDefFile.GroupEntry soundGroup;
        private MugenDefFile.KVEntry
                    sound,
                    sampleRate,
                    buffSize,
                    stereoFx,
                    panWidth,
                    reverseStereo,
                    wavChan,
                    mastVol,
                    wavVol,
                    bgmVol,
                    sfxResampleMethod,
                    sfxResampleQuality,
                    bgmResampleQuality;
        private boolean soundBool, stereoFxBool, reverseStereoBool;
        private int buffSizeInt, panWidthInt, wavChanInt, mastVolInt, wavVolInt, bgmVolInt, sfxResampleQualityInt, bgmResampleQualityInt,sfxResampleMethodInt;
        private String sampleRateStr;

        public void setGroup(MugenDefFile.GroupEntry soundGroup){
            this.soundGroup = soundGroup;
            this.reload();
        }

        private void reload(){
            for (int i=0;i<soundGroup.size();i++){
                MugenDefFile.Entry entry = soundGroup.getEntry(i);

                if (entry instanceof MugenDefFile.KVEntry){
                    MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry)entry;
                    if (kvEntry.getKey().equals("Sound")) this.sound = kvEntry; //bool
                    else if (kvEntry.getKey().equals("SampleRate")) this.sampleRate = kvEntry; //int/str
                    else if (kvEntry.getKey().equals("BufferSize")) this.buffSize = kvEntry; //int
                    else if (kvEntry.getKey().equals("StereoEffects")) this.stereoFx = kvEntry; //bool
                    else if (kvEntry.getKey().equals("PanningWidth")) this.panWidth = kvEntry; //int
                    else if (kvEntry.getKey().equals("ReverseStereo")) this.reverseStereo = kvEntry; //bool
                    else if (kvEntry.getKey().equals("WavChannels")) this.wavChan = kvEntry; //int
                    else if (kvEntry.getKey().equals("MasterVolume")) this.mastVol = kvEntry; //int
                    else if (kvEntry.getKey().equals("WavVolume")) this.wavVol = kvEntry; //int
                    else if (kvEntry.getKey().equals("BGMVolume")) this.bgmVol = kvEntry; //int
                    else if (kvEntry.getKey().equals("SFXResampleMethod")) this.sfxResampleMethod = kvEntry; //str
                    else if (kvEntry.getKey().equals("SFXResampleQuality")) this.sfxResampleQuality = kvEntry; //int
                    else if (kvEntry.getKey().equals("BGMResampleQuality")) this.bgmResampleQuality = kvEntry; //int
                }
            }
            this.updateComponents();
        }

        private void updateComponents(){
            if (Integer.parseInt(getKVString(this.sound)) == 0) this.soundBool = false;
            else this.soundBool = true;
            if (Integer.parseInt(getKVString(this.stereoFx)) == 0) this.stereoFxBool = false;
            else this.stereoFxBool = true;
            if (Integer.parseInt(getKVString(this.reverseStereo)) == 0) this.reverseStereoBool = false;
            else this.reverseStereoBool = true;

            this.sampleRateStr = getKVString(this.sampleRate);
            txtSampleRate_Sounds.setText(sampleRateStr);

            this.buffSizeInt = Integer.parseInt(getKVString(this.buffSize));
            this.panWidthInt = Integer.parseInt(getKVString(this.panWidth));
            this.wavChanInt = Integer.parseInt(getKVString(this.wavChan));
            this.mastVolInt = Integer.parseInt(getKVString(this.mastVol));
            this.wavVolInt = Integer.parseInt(getKVString(this.wavVol));
            this.bgmVolInt = Integer.parseInt(getKVString(this.bgmVol));
            this.sfxResampleQualityInt = Integer.parseInt(getKVString(this.sfxResampleQuality));
            this.bgmResampleQualityInt = Integer.parseInt(getKVString(this.bgmResampleQuality));

            spinBuffSize_Sounds.setValue(this.buffSizeInt);
            spinPanWidth_Sounds.setValue(this.panWidthInt);
            spinWaveChans_Sounds.setValue(this.wavChanInt);
            spinMasterVol_Sounds.setValue(this.mastVolInt);
            spinWavVol_Sounds.setValue(this.wavVolInt);
            spinBGMVol_Sounds.setValue(this.bgmVolInt);

            cmbxSFXQuality_Sounds.setSelectedIndex(this.sfxResampleQualityInt);
            cmbxBGMQuality_Sounds.setSelectedIndex(this.bgmResampleQualityInt);

            String sfxResampleMethodStr = getKVString(this.sfxResampleMethod);
            if (sfxResampleMethodStr.equals("SDL")) this.sfxResampleMethodInt = 0;
            else if (sfxResampleMethodStr.equals("libresample")) this.sfxResampleMethodInt = 1;
            cmbxSFXMethod_Sounds.setSelectedIndex(this.sfxResampleMethodInt);

            cbxEnableSound_Sounds.setSelected(this.soundBool);
            cbxStereoEfx_Sounds.setSelected(this.stereoFxBool);
            cbxReverseStereo_Sounds.setSelected(this.reverseStereoBool);
        }

        public boolean checkContent(){
            boolean modified = false;
            if (cbxEnableSound_Sounds.isSelected() != this.soundBool) modified = true;
            if (cbxStereoEfx_Sounds.isSelected() != this.stereoFxBool) modified = true;
            if (cbxReverseStereo_Sounds.isSelected() != this.reverseStereoBool) modified = true;
            if ((Integer)spinBuffSize_Sounds.getValue() != this.buffSizeInt) modified = true;
            if ((Integer)spinPanWidth_Sounds.getValue() != this.panWidthInt) modified = true;
            if ((Integer)spinWaveChans_Sounds.getValue() != this.wavChanInt) modified = true;
            if ((Integer)spinMasterVol_Sounds.getValue() != this.mastVolInt) modified = true;
            if ((Integer)spinWavVol_Sounds.getValue() != this.wavVolInt) modified = true;
            if ((Integer)spinBGMVol_Sounds.getValue() != this.bgmVolInt) modified = true;
            if (cmbxSFXQuality_Sounds.getSelectedIndex() != this.sfxResampleQualityInt) modified = true;
            if (cmbxBGMQuality_Sounds.getSelectedIndex() != this.bgmResampleQualityInt) modified = true;
            if (cmbxSFXMethod_Sounds.getSelectedIndex() != this.sfxResampleMethodInt) modified = true;
            return modified;
        }

        public void save(){
            this.soundBool = cbxEnableSound_Sounds.isSelected();
            this.stereoFxBool = cbxStereoEfx_Sounds.isSelected();
            this.reverseStereoBool = cbxReverseStereo_Sounds.isSelected();

            this.buffSizeInt = (Integer)spinBuffSize_Sounds.getValue();
            this.panWidthInt = (Integer)spinPanWidth_Sounds.getValue();
            this.wavChanInt = (Integer)spinWaveChans_Sounds.getValue();
            this.mastVolInt = (Integer)spinMasterVol_Sounds.getValue();
            this.wavVolInt = (Integer)spinWavVol_Sounds.getValue();
            this.bgmVolInt = (Integer)spinBGMVol_Sounds.getValue();

            this.sfxResampleMethodInt = cmbxSFXMethod_Sounds.getSelectedIndex();
            this.sfxResampleQualityInt = cmbxSFXQuality_Sounds.getSelectedIndex();
            this.bgmResampleQualityInt = cmbxBGMQuality_Sounds.getSelectedIndex();

            if (this.soundBool) this.sound.setValue("1");
            else this.sound.setValue("0");
            if (this.stereoFxBool) this.stereoFx.setValue("1");
            else this.stereoFx.setValue("0");
            if (this.reverseStereoBool) this.reverseStereo.setValue("1");
            else this.reverseStereo.setValue("0");

            this.buffSize.setValue(Integer.toString(this.buffSizeInt));
            this.panWidth.setValue(Integer.toString(this.panWidthInt));
            this.wavChan.setValue(Integer.toString(this.wavChanInt));
            this.mastVol.setValue(Integer.toString(this.mastVolInt));
            this.wavVol.setValue(Integer.toString(this.wavVolInt));
            this.bgmVol.setValue(Integer.toString(this.bgmVolInt));

            switch (this.sfxResampleMethodInt){
                case 0:
                    this.sfxResampleMethod.setValue("SDL");
                    break;
                case 1:
                    this.sfxResampleMethod.setValue("libresample");
                    break;
            }

            this.sfxResampleQuality.setValue(Integer.toString(this.sfxResampleQualityInt));
            this.bgmResampleQuality.setValue(Integer.toString(this.bgmResampleQualityInt));
        }
    }

    private class MusicSettings {
        private MugenDefFile.GroupEntry musicGroup;

//        plugin = mpg123
//        decoder = ;omited
//        rva = track
//        extensions =
//        volume = 100.0
        private MugenDefFile.CommentEntry mpg123_comment;
        private String mpg123_extensions;
        private int mpg123_volume, mpg123_decoder_int, mpg123_rva_int;

//        plugin = sdlmix
//        midivolume =
//        modvolume =
        private MugenDefFile.CommentEntry sdlmix_comment;
        private int sdlmix_midiVol, sdlmix_modVol;

        private int currSelectedPlugin = -1;

        public MusicSettings(){
            this.mpg123_decoder_int = 0;
            this.mpg123_rva_int = 1;
            this.mpg123_extensions = "";
            this.mpg123_volume = 100;
            this.sdlmix_midiVol = 100;
            this.sdlmix_modVol = 100;
        }

        public void setGroup(MugenDefFile.GroupEntry musicGroup){
            this.musicGroup = musicGroup;
            listCurrPluginsModel.clear();
            listAvailablePluginsModel.clear();
            if (this.musicGroup != null) this.reload();
        }

        private void reload(){
            //<editor-fold defaultstate="collapsed" desc="Load current list of plugins.">
            for (int i=0;i<this.musicGroup.size();i++){
                MugenDefFile.Entry entry = this.musicGroup.getEntry(i);

                if (entry instanceof MugenDefFile.KVEntry){
                    MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry)entry;
                    if (kvEntry.getKey().equals("plugin")){
                        if (kvEntry.getValue() instanceof MugenDefFile.ListEntry){ //it's a custom plugin
                            //<editor-fold defaultstate="collapsed" desc="Custom Plugin">
                            ArrayList<MugenDefFile.KVEntry> params = new ArrayList<MugenDefFile.KVEntry>();
                            MugenDefFile.KVEntry nextKVEntry=null;
                            if (i+1 < this.musicGroup.size()){
                                MugenDefFile.Entry nextEntry = this.musicGroup.getEntry(i+1);
                                if (nextEntry instanceof MugenDefFile.KVEntry) nextKVEntry = (MugenDefFile.KVEntry)nextEntry;
                            }else{
                                if (params != null) i+= params.size();
                                break;
                            }

                            while (!nextKVEntry.getKey().equals("plugin")){
                                if (i+params.size()+1 < this.musicGroup.size()){
                                    MugenDefFile.Entry nextEntry = this.musicGroup.getEntry(i+params.size()+1);
                                    if (nextEntry instanceof MugenDefFile.KVEntry) nextKVEntry = (MugenDefFile.KVEntry)nextEntry;
                                }else{
                                    if (params != null) i+= params.size();
                                    break;
                                }

                                if (!nextKVEntry.getKey().equals("plugin")) params.add(nextKVEntry);
                                else{
                                    if (params != null) i+= params.size();
                                    break;
                                }
                            }
                            if (params.size()>0) listCurrPluginsModel.addElement(new PluginDetails(kvEntry,params));
                            else listCurrPluginsModel.addElement(new PluginDetails(kvEntry));
                            params = null;
                            //</editor-fold>
                        }else if (kvEntry.getValue() instanceof MugenDefFile.StringEntry){ //it's a built-in plugin
                            //<editor-fold defaultstate="collapsed" desc="Built-In plugins">
                            MugenDefFile.StringEntry builtIn = (MugenDefFile.StringEntry)kvEntry.getValue();
                            if (builtIn.getString().equals("mpg123")){  //mpg123 plugin (3 or 4 params)
                                //<editor-fold defaultstate="collapsed" desc="mpg123">
                                this.mpg123_comment = kvEntry.getMultilineComment();
                                MugenDefFile.Entry paramEntry1 = this.musicGroup.getEntry(i+1);
                                MugenDefFile.Entry paramEntry2 = this.musicGroup.getEntry(i+2);
                                MugenDefFile.Entry paramEntry3 = this.musicGroup.getEntry(i+3);
                                MugenDefFile.Entry paramEntry4 = this.musicGroup.getEntry(i+4);

                                //Decoder: SSE, 3DNowExt, 3DNow, MMX, i586, i586_dither, i386, generic, generic_dither.

                                //<editor-fold defaultstate="collapsed" desc="Param 1">
                                if (paramEntry1 instanceof MugenDefFile.KVEntry){
                                    MugenDefFile.KVEntry paramKVEntry1 = (MugenDefFile.KVEntry)paramEntry1;
                                    String paramKey1 = paramKVEntry1.getKey();
                                    String paramVal1 = getKVString(paramKVEntry1);
                                    if (paramKey1.equals("decoder")){
                                        if (paramVal1.equals("SSE")){
                                            this.mpg123_decoder_int = 1;
                                        }else if (paramVal1.equals("3DNowExt")){
                                            this.mpg123_decoder_int = 2;
                                        }else if (paramVal1.equals("3DNow")){
                                            this.mpg123_decoder_int = 3;
                                        }else if (paramVal1.equals("MMX")){
                                            this.mpg123_decoder_int = 4;
                                        }else if (paramVal1.equals("i586")){
                                            this.mpg123_decoder_int = 5;
                                        }else if (paramVal1.equals("i586_dither")){
                                            this.mpg123_decoder_int = 6;
                                        }else if (paramVal1.equals("i386")){
                                            this.mpg123_decoder_int = 7;
                                        }else if (paramVal1.equals("generic")){
                                            this.mpg123_decoder_int = 8;
                                        }else if (paramVal1.equals("generic_dither")){
                                            this.mpg123_decoder_int = 9;
                                        }else{
                                            this.mpg123_decoder_int = 0;
                                        }
                                    }else if (paramKey1.equals("rva")){
                                        if (paramVal1.equals("off")){
                                            this.mpg123_rva_int = 0;
                                        }else if (paramVal1.equals("track")){
                                            this.mpg123_rva_int = 1;
                                        }else if (paramVal1.equals("album")){
                                            this.mpg123_rva_int = 2;
                                        }
                                    }else if (paramKey1.equals("extensions")){
                                        this.mpg123_extensions = paramVal1;
                                    }else if (paramKey1.equals("volume")){
                                        this.mpg123_volume = (int)Float.parseFloat(paramVal1);
                                    }
                                }
                                //</editor-fold>

                                //<editor-fold defaultstate="collapsed" desc="Param 2">
                                if (paramEntry2 instanceof MugenDefFile.KVEntry){
                                    MugenDefFile.KVEntry paramKVEntry2 = (MugenDefFile.KVEntry)paramEntry2;
                                    String paramKey2 = paramKVEntry2.getKey();
                                    String paramVal2 = getKVString(paramKVEntry2);
                                    if (paramKey2.equals("decoder")){
                                        if (paramVal2.equals("SSE")){
                                            this.mpg123_decoder_int = 1;
                                        }else if (paramVal2.equals("3DNowExt")){
                                            this.mpg123_decoder_int = 2;
                                        }else if (paramVal2.equals("3DNow")){
                                            this.mpg123_decoder_int = 3;
                                        }else if (paramVal2.equals("MMX")){
                                            this.mpg123_decoder_int = 4;
                                        }else if (paramVal2.equals("i586")){
                                            this.mpg123_decoder_int = 5;
                                        }else if (paramVal2.equals("i586_dither")){
                                            this.mpg123_decoder_int = 6;
                                        }else if (paramVal2.equals("i386")){
                                            this.mpg123_decoder_int = 7;
                                        }else if (paramVal2.equals("generic")){
                                            this.mpg123_decoder_int = 8;
                                        }else if (paramVal2.equals("generic_dither")){
                                            this.mpg123_decoder_int = 9;
                                        }else{
                                            this.mpg123_decoder_int = 0;
                                        }
                                    }else if (paramKey2.equals("rva")){
                                        if (paramKey2.equals("off")){
                                            this.mpg123_rva_int = 0;
                                        }else if (paramKey2.equals("track")){
                                            this.mpg123_rva_int = 1;
                                        }else if (paramKey2.equals("album")){
                                            this.mpg123_rva_int = 2;
                                        }
                                    }else if (paramKey2.equals("extensions")){
                                        this.mpg123_extensions = paramVal2;
                                    }else if (paramKey2.equals("volume")){
                                        this.mpg123_volume = (int)Float.parseFloat(paramVal2);
                                    }
                                }
                                //</editor-fold>

                                //<editor-fold defaultstate="collapsed" desc="Param 3">
                                if (paramEntry3 instanceof MugenDefFile.KVEntry){
                                    MugenDefFile.KVEntry paramKVEntry3 = (MugenDefFile.KVEntry)paramEntry3;
                                    String paramKey3 = paramKVEntry3.getKey();
                                    String paramVal3 = getKVString(paramKVEntry3);
                                    if (paramKey3.equals("decoder")){
                                        if (paramVal3.equals("SSE")){
                                            this.mpg123_decoder_int = 1;
                                        }else if (paramVal3.equals("3DNowExt")){
                                            this.mpg123_decoder_int = 2;
                                        }else if (paramVal3.equals("3DNow")){
                                            this.mpg123_decoder_int = 3;
                                        }else if (paramVal3.equals("MMX")){
                                            this.mpg123_decoder_int = 4;
                                        }else if (paramVal3.equals("i586")){
                                            this.mpg123_decoder_int = 5;
                                        }else if (paramVal3.equals("i586_dither")){
                                            this.mpg123_decoder_int = 6;
                                        }else if (paramVal3.equals("i386")){
                                            this.mpg123_decoder_int = 7;
                                        }else if (paramVal3.equals("generic")){
                                            this.mpg123_decoder_int = 8;
                                        }else if (paramVal3.equals("generic_dither")){
                                            this.mpg123_decoder_int = 9;
                                        }else{
                                            this.mpg123_decoder_int = 0;
                                        }
                                    }else if (paramKey3.equals("rva")){
                                        if (paramVal3.equals("off")){
                                            this.mpg123_rva_int = 0;
                                        }else if (paramVal3.equals("track")){
                                            this.mpg123_rva_int = 1;
                                        }else if (paramVal3.equals("album")){
                                            this.mpg123_rva_int = 2;
                                        }
                                    }else if (paramKey3.equals("extensions")){
                                        this.mpg123_extensions = paramVal3;
                                    }else if (paramKey3.equals("volume")){
                                        this.mpg123_volume = (int)Float.parseFloat(paramVal3);
                                    }
                                }
                                //</editor-fold>

                                //check if there is 3 or 4 parameters. if 4 get that parameter.
                                //<editor-fold defaultstate="collapsed" desc="Param 4">
                                if (paramEntry4 instanceof MugenDefFile.KVEntry){
                                    MugenDefFile.KVEntry paramKVEntry4 = (MugenDefFile.KVEntry)paramEntry4;
                                    String paramKey4 = paramKVEntry4.getKey();
                                    if (paramKey4.equals("plugin")){
                                        i+=3;
                                        this.mpg123_decoder_int = 0;
                                    }else{
                                        String paramVal4 = getKVString(paramKVEntry4);
                                        if (paramKey4.equals("decoder")){
                                            if (paramVal4.equals("SSE")){
                                                this.mpg123_decoder_int = 1;
                                            }else if (paramVal4.equals("3DNowExt")){
                                                this.mpg123_decoder_int = 2;
                                            }else if (paramVal4.equals("3DNow")){
                                                this.mpg123_decoder_int = 3;
                                            }else if (paramVal4.equals("MMX")){
                                                this.mpg123_decoder_int = 4;
                                            }else if (paramVal4.equals("i586")){
                                                this.mpg123_decoder_int = 5;
                                            }else if (paramVal4.equals("i586_dither")){
                                                this.mpg123_decoder_int = 6;
                                            }else if (paramVal4.equals("i386")){
                                                this.mpg123_decoder_int = 7;
                                            }else if (paramVal4.equals("generic")){
                                                this.mpg123_decoder_int = 8;
                                            }else if (paramVal4.equals("generic_dither")){
                                                this.mpg123_decoder_int = 9;
                                            }else{
                                                this.mpg123_decoder_int = 0;
                                            }
                                        }else if (paramKey4.equals("rva")){
                                            if (paramVal4.equals("off")){
                                                this.mpg123_rva_int = 0;
                                            }else if (paramVal4.equals("track")){
                                                this.mpg123_rva_int = 1;
                                            }else if (paramVal4.equals("album")){
                                                this.mpg123_rva_int = 2;
                                            }
                                        }else if (paramKey4.equals("extensions")){
                                            this.mpg123_extensions = paramVal4;
                                        }else if (paramKey4.equals("volume")){
                                            this.mpg123_volume = (int)Float.parseFloat(paramVal4);
                                        }
                                        i+=4;
                                    }
                                }
                                //</editor-fold>
                                if (this.mpg123_volume>200){ this.mpg123_volume=200; }
                                if (this.mpg123_volume<0){ this.mpg123_volume=0; }
                                //</editor-fold>
                            }else if (builtIn.getString().equals("sdlmix")){  //sdlmix plugin (2 params)
                                //<editor-fold defaultstate="collapsed" desc="sdlmix">
                                this.sdlmix_comment = kvEntry.getMultilineComment();
                                MugenDefFile.Entry paramEntry1 = this.musicGroup.getEntry(i+1);
                                MugenDefFile.Entry paramEntry2 = this.musicGroup.getEntry(i+2);

                                if (paramEntry1 instanceof MugenDefFile.KVEntry){
                                    MugenDefFile.KVEntry paramKVEntry1 = (MugenDefFile.KVEntry)paramEntry1;
                                    String paramKey1 = paramKVEntry1.getKey();
                                    String paramVal1 = ((MugenDefFile.StringEntry)paramKVEntry1.getValue()).getString();
                                    if (paramVal1.length() == 0) paramVal1 = "100.0";
                                    if (paramKey1.equals("midivolume")){
                                        this.sdlmix_midiVol = (int)Float.parseFloat(paramVal1);
                                    }else if (paramKey1.equals("modvolume")){
                                        this.sdlmix_modVol = (int)Float.parseFloat(paramVal1);
                                    }
                                }

                                if (paramEntry2 instanceof MugenDefFile.KVEntry){
                                    MugenDefFile.KVEntry paramKVEntry2 = (MugenDefFile.KVEntry)paramEntry2;
                                    String paramKey2 = paramKVEntry2.getKey();
                                    String paramVal2 = ((MugenDefFile.StringEntry)paramKVEntry2.getValue()).getString();
                                    if (paramVal2.length() == 0) paramVal2 = "100.0";
                                    if (paramKey2.equals("midivolume")){
                                        this.sdlmix_midiVol = (int)Float.parseFloat(paramVal2);
                                    }else if (paramKey2.equals("modvolume")){
                                        this.sdlmix_modVol = (int)Float.parseFloat(paramVal2);
                                    }
                                }
                                i+=2;
                                if (this.sdlmix_midiVol>200){ this.sdlmix_midiVol=200; }
                                if (this.sdlmix_midiVol<0){ this.sdlmix_midiVol=0; }
                                if (this.sdlmix_modVol>200){ this.sdlmix_modVol=200; }
                                if (this.sdlmix_modVol<0){ this.sdlmix_modVol=0; }
                                //</editor-fold>
                            }
                            //</editor-fold>
                        }
                    }
                }
            }
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="Load list of available plugins.">
            File pluginsFolder = new File(pluginsPath);
            listAvailablePluginsModel.clear();

            if (pluginsFolder != null){
                File[] pluginsList = pluginsFolder.listFiles();

                for (int i=0;i<pluginsList.length;i++){
                    File pluginFile = pluginsList[i];
                    if (pluginFile.isFile()){
                        String pluginFileName = pluginFile.getName();
                        if (pluginFileName.endsWith(".dll")){
                            String pluginName = pluginFileName.substring(0, pluginFileName.length()-4);
                            listAvailablePluginsModel.addElement(new PluginDetails(pluginName,pluginsFolderName + "/" + pluginFileName));
                        }
                    }
                }
            }
            //</editor-fold>

            this.updateComponents();
        }

        private void updateComponents(){
            cmbxMpg123Decoder_Music.setSelectedIndex(this.mpg123_decoder_int);
            cmbxMpg123RVA_Music.setSelectedIndex(this.mpg123_rva_int);
            txtMpg123Ext_Music.setText(this.mpg123_extensions);
            spinMpg123Vol_Music.setValue(this.mpg123_volume);
            spinSdlMixMidiVol_Music.setValue(this.sdlmix_midiVol);
            spinSdlMixModVol_Music.setValue(this.sdlmix_modVol);
        }

        public boolean checkContent(){ //also modifies which param list to use.
            boolean modified = false;
            if (cmbxMpg123Decoder_Music.getSelectedIndex() != this.mpg123_decoder_int) modified = true;
            if (cmbxMpg123RVA_Music.getSelectedIndex() != this.mpg123_rva_int) modified = true;
            if (!txtMpg123Ext_Music.getText().equals(this.mpg123_extensions)) modified = true;
            if ((Integer)spinMpg123Vol_Music.getValue() != this.mpg123_volume) modified = true;
            if ((Integer)spinSdlMixMidiVol_Music.getValue() != this.sdlmix_midiVol) modified = true;
            if ((Integer)spinSdlMixModVol_Music.getValue() != this.sdlmix_modVol) modified = true;

            if (listCurrPluginsModel.isEmpty()) this.currSelectedPlugin = -1;

            if (this.currSelectedPlugin != listCurrPlugins_Music.getSelectedIndex() && !listCurrPluginsModel.isEmpty()){
                this.currSelectedPlugin = listCurrPlugins_Music.getSelectedIndex();
                cmbxPluginParamsModel.removeAllElements();
                ArrayList<PluginDetails.PluginParam> params = ((PluginDetails)listCurrPluginsModel.getElementAt(this.currSelectedPlugin)).getPluginParams();

                for (int i=0;i<params.size();i++) cmbxPluginParamsModel.addElement(params.get(i));
            }

            return modified;
        }

        public void save(){
            this.mpg123_decoder_int = cmbxMpg123Decoder_Music.getSelectedIndex();
            this.mpg123_rva_int = cmbxMpg123RVA_Music.getSelectedIndex();
            this.mpg123_extensions = txtMpg123Ext_Music.getText();
            this.mpg123_volume = (Integer)spinMpg123Vol_Music.getValue();
            this.sdlmix_midiVol = (Integer)spinSdlMixMidiVol_Music.getValue();
            this.sdlmix_modVol = (Integer)spinSdlMixModVol_Music.getValue();

            this.musicGroup.clear();
            //<editor-fold defaultstate="collapsed" desc="Add mpg123 plugin">
            this.musicGroup.addEntry(new MugenDefFile.KVEntry("plugin", "mpg123", this.mpg123_comment));
            if (this.mpg123_decoder_int != 0){
                String decoderStr = "";
                switch(this.mpg123_decoder_int){
                    case 1:
                        decoderStr="SSE";
                        break;
                    case 2:
                        decoderStr="3DNowExt";
                        break;
                    case 3:
                        decoderStr="3DNow";
                        break;
                    case 4:
                        decoderStr="MMX";
                        break;
                    case 5:
                        decoderStr="i586";
                        break;
                    case 6:
                        decoderStr="i586_dither";
                        break;
                    case 7:
                        decoderStr="i386";
                        break;
                    case 8:
                        decoderStr="generic";
                        break;
                    case 9:
                        decoderStr="generic_dither";
                        break;
                }
                this.musicGroup.addEntry(new MugenDefFile.KVEntry("decoder", decoderStr));
            }
            switch (this.mpg123_rva_int){
                case 0:
                    this.musicGroup.addEntry(new MugenDefFile.KVEntry("rva", "off"));
                    break;
                case 1:
                    this.musicGroup.addEntry(new MugenDefFile.KVEntry("rva", "track"));
                    break;
                case 2:
                    this.musicGroup.addEntry(new MugenDefFile.KVEntry("rva", "album"));
                    break;
            }
            this.musicGroup.addEntry(new MugenDefFile.KVEntry("volume", Float.toString((float)this.mpg123_volume)));
            this.musicGroup.addEntry(new MugenDefFile.KVEntry("extensions", this.mpg123_extensions));
            //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Add sdlmix plugin">
            this.musicGroup.addEntry(new MugenDefFile.KVEntry("plugin", "sdlmix", this.sdlmix_comment));
            this.musicGroup.addEntry(new MugenDefFile.KVEntry("midivolume", Float.toString((float)this.sdlmix_midiVol)));
            this.musicGroup.addEntry(new MugenDefFile.KVEntry("modvolume", Float.toString((float)this.sdlmix_modVol)));
            //</editor-fold>

            //add the other plugins
            for (int i=0;i<listCurrPluginsModel.size();i++){
                PluginDetails plugin = (PluginDetails)listCurrPluginsModel.getElementAt(i);
                this.musicGroup.addEntry(plugin.getPluginEntry());
                ArrayList<PluginDetails.PluginParam> pluginParams = plugin.getPluginParams();
                for (int i2=0;i2<pluginParams.size();i2++) this.musicGroup.addEntry(pluginParams.get(i2).getKVEntry());
            }
        }
    }

    private class MiscSettings {
        private MugenDefFile.GroupEntry miscGroup;
        private MugenDefFile.KVEntry playerCache, preCache, buffRead, unloadSys, sfxBgMode, bgmBgMode,pauseOnDefocus;
        private int playerCacheInt, sfxBgModeInt, bgmBgModeInt;
        private boolean preCacheBool, buffReadBool, unloadSysBool,pauseOnDefocusBool;

        public void setGroup(MugenDefFile.GroupEntry miscGroup){
            this.miscGroup = miscGroup;
            this.reload();
        }

        private void reload(){
            for (int i=0;i<miscGroup.size();i++){
                MugenDefFile.Entry entry = miscGroup.getEntry(i);

                if (entry instanceof MugenDefFile.KVEntry){
                    MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry)entry;
                    if (kvEntry.getKey().equals("PlayerCache")) this.playerCache = kvEntry; //int
                    else if (kvEntry.getKey().equals("Precache")) this.preCache = kvEntry; //bool
                    else if (kvEntry.getKey().equals("BufferedRead")) this.buffRead = kvEntry; //bool
                    else if (kvEntry.getKey().equals("UnloadSystem")) this.unloadSys = kvEntry; //bool
                    else if (kvEntry.getKey().equals("SFXBackgroundMode")) this.sfxBgMode = kvEntry; //str
                    else if (kvEntry.getKey().equals("BGMBackgroundMode")) this.bgmBgMode = kvEntry; //str
                    else if (kvEntry.getKey().equals("PauseOnDefocus")) this.pauseOnDefocus = kvEntry; //bool
                }
            }
            this.updateComponents();
        }

        private void updateComponents(){
            this.playerCacheInt = Integer.parseInt(getKVString(this.playerCache));
            if (Integer.parseInt(getKVString(this.preCache)) == 0) this.preCacheBool = false;
            else this.preCacheBool = true;
            if (Integer.parseInt(getKVString(this.buffRead)) == 0) this.buffReadBool = false;
            else this.buffReadBool = true;
            if (Integer.parseInt(getKVString(this.unloadSys)) == 0) this.unloadSysBool = false;
            else this.unloadSysBool = true;
            
            if (Integer.parseInt(getKVString(this.pauseOnDefocus)) == 0) this.pauseOnDefocusBool = false;
            else this.pauseOnDefocusBool = true;

            String sfxBgModeStr = getKVString(this.sfxBgMode);
            String bgmBgModeStr = getKVString(this.bgmBgMode);

            if (sfxBgModeStr.equals("Mute")) this.sfxBgModeInt = 0;
            else if (sfxBgModeStr.equals("Play")) this.sfxBgModeInt = 1;

            if (bgmBgModeStr.equals("Pause")) this.bgmBgModeInt = 0;
            else if (bgmBgModeStr.equals("Mute")) this.bgmBgModeInt = 1;
            else if (bgmBgModeStr.equals("Play")) this.bgmBgModeInt = 2;

            spinPlayerCache_Misc.setValue(this.playerCacheInt);
            cbxPreCache_Misc.setSelected(this.preCacheBool);
            cbxBuffRead_Misc.setSelected(this.buffReadBool);
            cbxUnloadSys_Misc.setSelected(this.unloadSysBool);
            cmbxSFXBgMode_Misc.setSelectedIndex(this.sfxBgModeInt);
            cmbxBGMBgMode_Misc.setSelectedIndex(this.bgmBgModeInt);
        }

        public boolean checkContent(){
            boolean modified = false;
            if ((Integer)spinPlayerCache_Misc.getValue() != this.playerCacheInt) modified = true;
            if (cbxPreCache_Misc.isSelected() != this.preCacheBool) modified = true;
            if (cbxBuffRead_Misc.isSelected() != this.buffReadBool) modified = true;
            if (cbxUnloadSys_Misc.isSelected() != this.unloadSysBool) modified = true;
            if (cmbxSFXBgMode_Misc.getSelectedIndex() != this.sfxBgModeInt) modified = true;
            if (cmbxBGMBgMode_Misc.getSelectedIndex() != this.bgmBgModeInt) modified = true;
            if (cbxPauseOnDefocus_Misc.isSelected() != this.pauseOnDefocusBool) modified = true;
            return modified;
        }

        public void save(){
            this.playerCacheInt = (Integer)spinPlayerCache_Misc.getValue();
            this.preCacheBool = cbxPreCache_Misc.isSelected();
            this.buffReadBool = cbxBuffRead_Misc.isSelected();
            this.unloadSysBool = cbxUnloadSys_Misc.isSelected();
            this.sfxBgModeInt = cmbxSFXBgMode_Misc.getSelectedIndex();
            this.bgmBgModeInt = cmbxBGMBgMode_Misc.getSelectedIndex();
            this.pauseOnDefocusBool = cbxPauseOnDefocus_Misc.isSelected();
            
            this.playerCache.setValue(Integer.toString(this.playerCacheInt));
            if (this.preCacheBool) this.preCache.setValue("1");
            else this.preCache.setValue("0");
            if (this.buffReadBool) this.buffRead.setValue("1");
            else this.buffRead.setValue("0");
            if (this.unloadSysBool) this.unloadSys.setValue("1");
            else this.unloadSys.setValue("0");
            if (this.pauseOnDefocusBool) this.pauseOnDefocus.setValue("1");
            else this.pauseOnDefocus.setValue("0");

            switch (this.sfxBgModeInt){
                case 0:
                    this.sfxBgMode.setValue("Mute");
                    break;
                case 1:
                    this.sfxBgMode.setValue("Play");
                    break;
            }

            switch (this.bgmBgModeInt){
                case 0:
                    this.bgmBgMode.setValue("Pause");
                    break;
                case 1:
                    this.bgmBgMode.setValue("Mute");
                    break;
                case 2:
                    this.bgmBgMode.setValue("Play");
                    break;
            }
        }
    }

    private class ArcadeSettings {
        private MugenDefFile.GroupEntry arcadeGroup;
        private MugenDefFile.KVEntry randCol, aiCheat, arcadeRampStart, arcadeRampEnd, teamRampStart, teamRampEnd, survRampStart, survRampEnd;
        private MugenDefFile.ListEntry arcadeRampStartList, arcadeRampEndList, teamRampStartList, teamRampEndList, survRampStartList, survRampEndList;
        private boolean randColBool, aiCheatBool;
        private int arcadeStartMatch, arcadeStartDiff, arcadeEndMatch, arcadeEndDiff;
        private int teamStartMatch, teamStartDiff, teamEndMatch, teamEndDiff;
        private int survivalStartMatch, survivalStartDiff, survivalEndMatch, survivalEndDiff;

        public void setGroup(MugenDefFile.GroupEntry arcadeGroup){
            this.arcadeGroup = arcadeGroup;
            this.reload();
        }

        private void reload(){
            for (int i=0;i<arcadeGroup.size();i++){
                MugenDefFile.Entry entry = arcadeGroup.getEntry(i);

                if (entry instanceof MugenDefFile.KVEntry){
                    MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry)entry;
                    if (kvEntry.getKey().equals("AI.RandomColor")) this.randCol = kvEntry; //bool
                    else if (kvEntry.getKey().equals("AI.Cheat")) this.aiCheat = kvEntry; //bool
                    else if (kvEntry.getKey().equals("arcade.AIramp.start")) this.arcadeRampStart = kvEntry; //int,int
                    else if (kvEntry.getKey().equals("arcade.AIramp.end")) this.arcadeRampEnd = kvEntry; //int,int
                    else if (kvEntry.getKey().equals("team.AIramp.start")) this.teamRampStart = kvEntry; //int,int
                    else if (kvEntry.getKey().equals("team.AIramp.end")) this.teamRampEnd = kvEntry; //int,int
                    else if (kvEntry.getKey().equals("survival.AIramp.start")) this.survRampStart = kvEntry; //int,int
                    else if (kvEntry.getKey().equals("survival.AIramp.end")) this.survRampEnd = kvEntry; //int,int
                }
            }
            this.updateComponents();
        }
        
        private void updateComponents(){
            if (Integer.parseInt(getKVString(this.randCol)) == 0) this.randColBool = false;
            else this.randColBool = true;
            if (Integer.parseInt(getKVString(this.aiCheat)) == 0) this.aiCheatBool = false;
            else this.aiCheatBool = true;
            
            this.arcadeRampStartList = (MugenDefFile.ListEntry)this.arcadeRampStart.getValue();
            this.arcadeRampEndList = (MugenDefFile.ListEntry)this.arcadeRampEnd.getValue();
            this.teamRampStartList = (MugenDefFile.ListEntry)this.teamRampStart.getValue();
            this.teamRampEndList = (MugenDefFile.ListEntry)this.teamRampEnd.getValue();
            this.survRampStartList = (MugenDefFile.ListEntry)this.survRampStart.getValue();
            this.survRampEndList = (MugenDefFile.ListEntry)this.survRampEnd.getValue();
            
            this.arcadeStartMatch = Integer.parseInt(getListString(this.arcadeRampStartList,0));
            this.arcadeStartDiff = Integer.parseInt(getListString(this.arcadeRampStartList,1));
            this.arcadeEndMatch = Integer.parseInt(getListString(this.arcadeRampEndList,0));
            this.arcadeEndDiff = Integer.parseInt(getListString(this.arcadeRampEndList,1));
            
            this.teamStartMatch = Integer.parseInt(getListString(this.teamRampStartList,0));
            this.teamStartDiff = Integer.parseInt(getListString(this.teamRampStartList,1));
            this.teamEndMatch = Integer.parseInt(getListString(this.teamRampEndList,0));
            this.teamEndDiff = Integer.parseInt(getListString(this.teamRampEndList,1));
            
            this.survivalStartMatch = Integer.parseInt(getListString(this.survRampStartList,0));
            this.survivalStartDiff = Integer.parseInt(getListString(this.survRampStartList,1));
            this.survivalEndMatch = Integer.parseInt(getListString(this.survRampEndList,0));
            this.survivalEndDiff = Integer.parseInt(getListString(this.survRampEndList,1));
            
            cbxAICheat_Arcade.setSelected(this.aiCheatBool);
            cbxAIRandomCol_Arcade.setSelected(this.randColBool);
            
            tblAIRampModel.setValueAt(this.arcadeStartMatch, 0, 1);
            tblAIRampModel.setValueAt(this.arcadeStartDiff, 0, 2);
            tblAIRampModel.setValueAt(this.arcadeEndMatch, 0, 3);
            tblAIRampModel.setValueAt(this.arcadeEndDiff, 0, 4);
            
            tblAIRampModel.setValueAt(this.teamStartMatch, 1, 1);
            tblAIRampModel.setValueAt(this.teamStartDiff, 1, 2);
            tblAIRampModel.setValueAt(this.teamEndMatch, 1, 3);
            tblAIRampModel.setValueAt(this.teamEndDiff, 1, 4);
            
            tblAIRampModel.setValueAt(this.survivalStartMatch, 2, 1);
            tblAIRampModel.setValueAt(this.survivalStartDiff, 2, 2);
            tblAIRampModel.setValueAt(this.survivalEndMatch, 2, 3);
            tblAIRampModel.setValueAt(this.survivalEndDiff, 2, 4);
        }
        
        public boolean checkContent(){
            boolean modified = false;
            if (cbxAICheat_Arcade.isSelected() != this.aiCheatBool) modified = true;
            if (cbxAIRandomCol_Arcade.isSelected() != this.randColBool) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(0, 1) != this.arcadeStartMatch) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(0, 2) != this.arcadeStartDiff) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(0, 3) != this.arcadeEndMatch) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(0, 4) != this.arcadeEndDiff) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(1, 1) != this.teamStartMatch) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(1, 2) != this.teamStartDiff) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(1, 3) != this.teamEndMatch) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(1, 4) != this.teamEndDiff) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(2, 1) != this.survivalStartMatch) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(2, 2) != this.survivalStartDiff) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(2, 3) != this.survivalEndMatch) modified = true;
            if ((Integer)tblAIRampModel.getValueAt(2, 4) != this.survivalEndDiff) modified = true;
            return modified;
        }
        
        public void save(){
            this.aiCheatBool = cbxAICheat_Arcade.isSelected();
            this.randColBool = cbxAIRandomCol_Arcade.isSelected();
            this.arcadeStartMatch = (Integer)tblAIRampModel.getValueAt(0, 1);
            this.arcadeStartDiff = (Integer)tblAIRampModel.getValueAt(0, 2);
            this.arcadeEndMatch = (Integer)tblAIRampModel.getValueAt(0, 3);
            this.arcadeEndDiff = (Integer)tblAIRampModel.getValueAt(0, 4);
            this.teamStartMatch = (Integer)tblAIRampModel.getValueAt(1, 1);
            this.teamStartDiff = (Integer)tblAIRampModel.getValueAt(1, 2);
            this.teamEndMatch = (Integer)tblAIRampModel.getValueAt(1, 3);
            this.teamEndDiff = (Integer)tblAIRampModel.getValueAt(1, 4);
            this.survivalStartMatch = (Integer)tblAIRampModel.getValueAt(2, 1);
            this.survivalStartDiff = (Integer)tblAIRampModel.getValueAt(2, 2);
            this.survivalEndMatch = (Integer)tblAIRampModel.getValueAt(2, 3);
            this.survivalEndDiff = (Integer)tblAIRampModel.getValueAt(2, 4);
            
            if (this.aiCheatBool) this.aiCheat.setValue("1");
            else this.aiCheat.setValue("0");
            if (this.randColBool) this.randCol.setValue("1");
            else this.randCol.setValue("0");
            
            this.arcadeRampStartList.clear();
            this.arcadeRampStartList.addEntry(Integer.toString(this.arcadeStartMatch));
            this.arcadeRampStartList.addEntry(Integer.toString(this.arcadeStartDiff));
            this.arcadeRampEndList.clear();
            this.arcadeRampEndList.addEntry(Integer.toString(this.arcadeEndMatch));
            this.arcadeRampEndList.addEntry(Integer.toString(this.arcadeEndDiff));
            this.teamRampStartList.clear();
            this.teamRampStartList.addEntry(Integer.toString(this.teamStartMatch));
            this.teamRampStartList.addEntry(Integer.toString(this.teamStartDiff));
            this.teamRampEndList.clear();
            this.teamRampEndList.addEntry(Integer.toString(this.teamEndMatch));
            this.teamRampEndList.addEntry(Integer.toString(this.teamEndDiff));
            this.survRampStartList.clear();
            this.survRampStartList.addEntry(Integer.toString(this.survivalStartMatch));
            this.survRampStartList.addEntry(Integer.toString(this.survivalStartDiff));
            this.survRampEndList.clear();
            this.survRampEndList.addEntry(Integer.toString(this.survivalEndMatch));
            this.survRampEndList.addEntry(Integer.toString(this.survivalEndDiff));
        }
    }
    
    private class InputSettings {
        private MugenDefFile.GroupEntry inputGroup;
        private MugenDefFile.KVEntry useP1Kb, useP2Kb, useP1Joy, useP2Joy,inputDirMode,anyKeyUnpauses;
        private InputDetails p1Kb, p2Kb, p1Joy, p2Joy;
        private boolean useP1KbBool, useP2KbBool, useP1JoyBool, useP2JoyBool,inputDirModeBool,anyKeyUnpausesBool;
        
        public InputSettings(){
            p1Kb = new InputDetails();
            p2Kb = new InputDetails();
            p1Joy = new InputDetails();
            p2Joy = new InputDetails();
        }
        
        public void setGroup(
                MugenDefFile.GroupEntry inputGroup,
                MugenDefFile.GroupEntry p1KeysGroup,
                MugenDefFile.GroupEntry p2KeysGroup,
                MugenDefFile.GroupEntry p1JoyGroup,
                MugenDefFile.GroupEntry p2JoyGroup
        ){
            this.inputGroup = inputGroup;
            p1Kb.setGroup(p1KeysGroup);
            p2Kb.setGroup(p2KeysGroup);
            p1Joy.setGroup(p1JoyGroup);
            p2Joy.setGroup(p2JoyGroup);
            this.reload();
        }
        
        private void reload(){
            for (int i=0;i<inputGroup.size();i++){
                MugenDefFile.Entry entry = inputGroup.getEntry(i);
                
                if (entry instanceof MugenDefFile.KVEntry){
                    MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry)entry;
                    if (kvEntry.getKey().equals("P1.UseKeyboard")) this.useP1Kb = kvEntry;
                    else if (kvEntry.getKey().equals("P2.UseKeyboard")) this.useP2Kb = kvEntry;
                    else if (kvEntry.getKey().equals("P1.Joystick.type")) this.useP1Joy = kvEntry;
                    else if (kvEntry.getKey().equals("P2.Joystick.type")) this.useP2Joy = kvEntry;
                    else if (kvEntry.getKey().equals("InputDirMode")) this.inputDirMode = kvEntry;
                    else if (kvEntry.getKey().equals("AnyKeyUnpauses")) this.anyKeyUnpauses = kvEntry;
                }
            }
            this.updateComponents();
        }
        
        private void updateComponents(){
            if (Integer.parseInt(getKVString(this.useP1Kb))==0) this.useP1KbBool = false;
            else this.useP1KbBool = true;
            if (Integer.parseInt(getKVString(this.useP2Kb))==0) this.useP2KbBool = false;
            else this.useP2KbBool = true;
            if (Integer.parseInt(getKVString(this.useP1Joy))==0) this.useP1JoyBool = false;
            else this.useP1JoyBool = true;
            if (Integer.parseInt(getKVString(this.useP2Joy))==0) this.useP2JoyBool = false;
            else this.useP2JoyBool = true;
            if (Integer.parseInt(getKVString(this.inputDirMode))==0) this.inputDirModeBool = false;
            else this.inputDirModeBool = true;
            if (Integer.parseInt(getKVString(this.anyKeyUnpauses))==0) this.anyKeyUnpausesBool = false;
            else this.anyKeyUnpausesBool = true;
            
            cbxP1KeyEnable_Input.setSelected(this.useP1KbBool);
            cbxP2KeyEnable_Input.setSelected(this.useP2KbBool);
            cbxP1JoyEnable_Input.setSelected(this.useP1JoyBool);
            cbxP2JoyEnable_Input.setSelected(this.useP2JoyBool);
            
            cbxInputDirMode_Input.setSelected(this.inputDirModeBool);
            cbxAnyKeyUnpauses_Input.setSelected(this.anyKeyUnpausesBool);
            
            txtP1KeyUp_Input.setText(this.p1Kb.getUp());
            txtP1KeyDown_Input.setText(this.p1Kb.getDown());
            txtP1KeyLeft_Input.setText(this.p1Kb.getLeft());
            txtP1KeyRight_Input.setText(this.p1Kb.getRight());
            txtP1KeyA_Input.setText(this.p1Kb.getA());
            txtP1KeyB_Input.setText(this.p1Kb.getB());
            txtP1KeyC_Input.setText(this.p1Kb.getC());
            txtP1KeyX_Input.setText(this.p1Kb.getX());
            txtP1KeyY_Input.setText(this.p1Kb.getY());
            txtP1KeyZ_Input.setText(this.p1Kb.getZ());
            txtP1KeyStart_Input.setText(this.p1Kb.getStart());
            
            txtP2KeyUp_Input.setText(this.p2Kb.getUp());
            txtP2KeyDown_Input.setText(this.p2Kb.getDown());
            txtP2KeyLeft_Input.setText(this.p2Kb.getLeft());
            txtP2KeyRight_Input.setText(this.p2Kb.getRight());
            txtP2KeyA_Input.setText(this.p2Kb.getA());
            txtP2KeyB_Input.setText(this.p2Kb.getB());
            txtP2KeyC_Input.setText(this.p2Kb.getC());
            txtP2KeyX_Input.setText(this.p2Kb.getX());
            txtP2KeyY_Input.setText(this.p2Kb.getY());
            txtP2KeyZ_Input.setText(this.p2Kb.getZ());
            txtP2KeyStart_Input.setText(this.p2Kb.getStart());
            
            txtP1JoyUp_Input.setText(this.p1Joy.getUp());
            txtP1JoyDown_Input.setText(this.p1Joy.getDown());
            txtP1JoyLeft_Input.setText(this.p1Joy.getLeft());
            txtP1JoyRight_Input.setText(this.p1Joy.getRight());
            txtP1JoyA_Input.setText(this.p1Joy.getA());
            txtP1JoyB_Input.setText(this.p1Joy.getB());
            txtP1JoyC_Input.setText(this.p1Joy.getC());
            txtP1JoyX_Input.setText(this.p1Joy.getX());
            txtP1JoyY_Input.setText(this.p1Joy.getY());
            txtP1JoyZ_Input.setText(this.p1Joy.getZ());
            txtP1JoyStart_Input.setText(this.p1Joy.getStart());
            
            txtP2JoyUp_Input.setText(this.p2Joy.getUp());
            txtP2JoyDown_Input.setText(this.p2Joy.getDown());
            txtP2JoyLeft_Input.setText(this.p2Joy.getLeft());
            txtP2JoyRight_Input.setText(this.p2Joy.getRight());
            txtP2JoyA_Input.setText(this.p2Joy.getA());
            txtP2JoyB_Input.setText(this.p2Joy.getB());
            txtP2JoyC_Input.setText(this.p2Joy.getC());
            txtP2JoyX_Input.setText(this.p2Joy.getX());
            txtP2JoyY_Input.setText(this.p2Joy.getY());
            txtP2JoyZ_Input.setText(this.p2Joy.getZ());
            txtP2JoyStart_Input.setText(this.p2Joy.getStart());
        }
        
        public boolean checkContent(){
            boolean modified=false;
            if (cbxP1KeyEnable_Input.isSelected() != this.useP1KbBool) modified = true;
            if (cbxP2KeyEnable_Input.isSelected() != this.useP2KbBool) modified = true;
            if (cbxP1JoyEnable_Input.isSelected() != this.useP1JoyBool) modified = true;
            if (cbxP2JoyEnable_Input.isSelected() != this.useP2JoyBool) modified = true;
            if (cbxInputDirMode_Input.isSelected() != this.inputDirModeBool) modified = true;
            if (cbxAnyKeyUnpauses_Input.isSelected() != this.anyKeyUnpausesBool) modified = true;
            
            if (!txtP1KeyUp_Input.getText().equals(this.p1Kb.getUp())) modified = true;
            if (!txtP1KeyDown_Input.getText().equals(this.p1Kb.getDown())) modified = true;
            if (!txtP1KeyLeft_Input.getText().equals(this.p1Kb.getLeft())) modified = true;
            if (!txtP1KeyRight_Input.getText().equals(this.p1Kb.getRight())) modified = true;
            if (!txtP1KeyA_Input.getText().equals(this.p1Kb.getA())) modified = true;
            if (!txtP1KeyB_Input.getText().equals(this.p1Kb.getB())) modified = true;
            if (!txtP1KeyC_Input.getText().equals(this.p1Kb.getC())) modified = true;
            if (!txtP1KeyX_Input.getText().equals(this.p1Kb.getX())) modified = true;
            if (!txtP1KeyY_Input.getText().equals(this.p1Kb.getY())) modified = true;
            if (!txtP1KeyZ_Input.getText().equals(this.p1Kb.getZ())) modified = true;
            if (!txtP1KeyStart_Input.getText().equals(this.p1Kb.getStart())) modified = true;
            
            if (!txtP2KeyUp_Input.getText().equals(this.p2Kb.getUp())) modified = true;
            if (!txtP2KeyDown_Input.getText().equals(this.p2Kb.getDown())) modified = true;
            if (!txtP2KeyLeft_Input.getText().equals(this.p2Kb.getLeft())) modified = true;
            if (!txtP2KeyRight_Input.getText().equals(this.p2Kb.getRight())) modified = true;
            if (!txtP2KeyA_Input.getText().equals(this.p2Kb.getA())) modified = true;
            if (!txtP2KeyB_Input.getText().equals(this.p2Kb.getB())) modified = true;
            if (!txtP2KeyC_Input.getText().equals(this.p2Kb.getC())) modified = true;
            if (!txtP2KeyX_Input.getText().equals(this.p2Kb.getX())) modified = true;
            if (!txtP2KeyY_Input.getText().equals(this.p2Kb.getY())) modified = true;
            if (!txtP2KeyZ_Input.getText().equals(this.p2Kb.getZ())) modified = true;
            if (!txtP2KeyStart_Input.getText().equals(this.p2Kb.getStart())) modified = true;
            
            if (!txtP1JoyUp_Input.getText().equals(this.p1Joy.getUp())) modified = true;
            if (!txtP1JoyDown_Input.getText().equals(this.p1Joy.getDown())) modified = true;
            if (!txtP1JoyLeft_Input.getText().equals(this.p1Joy.getLeft())) modified = true;
            if (!txtP1JoyRight_Input.getText().equals(this.p1Joy.getRight())) modified = true;
            if (!txtP1JoyA_Input.getText().equals(this.p1Joy.getA())) modified = true;
            if (!txtP1JoyB_Input.getText().equals(this.p1Joy.getB())) modified = true;
            if (!txtP1JoyC_Input.getText().equals(this.p1Joy.getC())) modified = true;
            if (!txtP1JoyX_Input.getText().equals(this.p1Joy.getX())) modified = true;
            if (!txtP1JoyY_Input.getText().equals(this.p1Joy.getY())) modified = true;
            if (!txtP1JoyZ_Input.getText().equals(this.p1Joy.getZ())) modified = true;
            if (!txtP1JoyStart_Input.getText().equals(this.p1Joy.getStart())) modified = true;
            
            if (!txtP2JoyUp_Input.getText().equals(this.p2Joy.getUp())) modified = true;
            if (!txtP2JoyDown_Input.getText().equals(this.p2Joy.getDown())) modified = true;
            if (!txtP2JoyLeft_Input.getText().equals(this.p2Joy.getLeft())) modified = true;
            if (!txtP2JoyRight_Input.getText().equals(this.p2Joy.getRight())) modified = true;
            if (!txtP2JoyA_Input.getText().equals(this.p2Joy.getA())) modified = true;
            if (!txtP2JoyB_Input.getText().equals(this.p2Joy.getB())) modified = true;
            if (!txtP2JoyC_Input.getText().equals(this.p2Joy.getC())) modified = true;
            if (!txtP2JoyX_Input.getText().equals(this.p2Joy.getX())) modified = true;
            if (!txtP2JoyY_Input.getText().equals(this.p2Joy.getY())) modified = true;
            if (!txtP2JoyZ_Input.getText().equals(this.p2Joy.getZ())) modified = true;
            if (!txtP2JoyStart_Input.getText().equals(this.p2Joy.getStart())) modified = true;
            return modified;
        }
        
        public void save(){
            this.useP1KbBool = cbxP1KeyEnable_Input.isSelected();
            this.useP2KbBool = cbxP2KeyEnable_Input.isSelected();
            this.useP1JoyBool = cbxP1JoyEnable_Input.isSelected();
            this.useP2JoyBool = cbxP2JoyEnable_Input.isSelected();
            this.inputDirModeBool = cbxInputDirMode_Input.isSelected();
            this.anyKeyUnpausesBool = cbxAnyKeyUnpauses_Input.isSelected();
            
            if (this.useP1KbBool) this.useP1Kb.setValue("1");
            else this.useP1Kb.setValue("0");
            if (this.useP2KbBool) this.useP2Kb.setValue("1");
            else this.useP2Kb.setValue("0");
            if (this.useP1JoyBool) this.useP1Joy.setValue("1");
            else this.useP1Joy.setValue("0");
            if (this.useP2JoyBool) this.useP2Joy.setValue("1");
            else this.useP2Joy.setValue("0");
            if (this.inputDirModeBool) this.inputDirMode.setValue("1");
            else this.inputDirMode.setValue("0");
            if (this.anyKeyUnpausesBool) this.anyKeyUnpauses.setValue("1");
            else this.anyKeyUnpauses.setValue("0");
            
            this.p1Kb.setUp(txtP1KeyUp_Input.getText());
            this.p1Kb.setDown(txtP1KeyDown_Input.getText());
            this.p1Kb.setLeft(txtP1KeyLeft_Input.getText());
            this.p1Kb.setRight(txtP1KeyRight_Input.getText());
            this.p1Kb.setA(txtP1KeyA_Input.getText());
            this.p1Kb.setB(txtP1KeyB_Input.getText());
            this.p1Kb.setC(txtP1KeyC_Input.getText());
            this.p1Kb.setX(txtP1KeyX_Input.getText());
            this.p1Kb.setY(txtP1KeyY_Input.getText());
            this.p1Kb.setZ(txtP1KeyZ_Input.getText());
            this.p1Kb.setStart(txtP1KeyStart_Input.getText());
            
            this.p2Kb.setUp(txtP2KeyUp_Input.getText());
            this.p2Kb.setDown(txtP2KeyDown_Input.getText());
            this.p2Kb.setLeft(txtP2KeyLeft_Input.getText());
            this.p2Kb.setRight(txtP2KeyRight_Input.getText());
            this.p2Kb.setA(txtP2KeyA_Input.getText());
            this.p2Kb.setB(txtP2KeyB_Input.getText());
            this.p2Kb.setC(txtP2KeyC_Input.getText());
            this.p2Kb.setX(txtP2KeyX_Input.getText());
            this.p2Kb.setY(txtP2KeyY_Input.getText());
            this.p2Kb.setZ(txtP2KeyZ_Input.getText());
            this.p2Kb.setStart(txtP2KeyStart_Input.getText());
            
            this.p1Joy.setUp(txtP1JoyUp_Input.getText());
            this.p1Joy.setDown(txtP1JoyDown_Input.getText());
            this.p1Joy.setLeft(txtP1JoyLeft_Input.getText());
            this.p1Joy.setRight(txtP1JoyRight_Input.getText());
            this.p1Joy.setA(txtP1JoyA_Input.getText());
            this.p1Joy.setB(txtP1JoyB_Input.getText());
            this.p1Joy.setC(txtP1JoyC_Input.getText());
            this.p1Joy.setX(txtP1JoyX_Input.getText());
            this.p1Joy.setY(txtP1JoyY_Input.getText());
            this.p1Joy.setZ(txtP1JoyZ_Input.getText());
            this.p1Joy.setStart(txtP1JoyStart_Input.getText());
            
            this.p2Joy.setUp(txtP2JoyUp_Input.getText());
            this.p2Joy.setDown(txtP2JoyDown_Input.getText());
            this.p2Joy.setLeft(txtP2JoyLeft_Input.getText());
            this.p2Joy.setRight(txtP2JoyRight_Input.getText());
            this.p2Joy.setA(txtP2JoyA_Input.getText());
            this.p2Joy.setB(txtP2JoyB_Input.getText());
            this.p2Joy.setC(txtP2JoyC_Input.getText());
            this.p2Joy.setX(txtP2JoyX_Input.getText());
            this.p2Joy.setY(txtP2JoyY_Input.getText());
            this.p2Joy.setZ(txtP2JoyZ_Input.getText());
            this.p2Joy.setStart(txtP2JoyStart_Input.getText());
            
            this.p1Kb.save();
            this.p2Kb.save();
            this.p1Joy.save();
            this.p2Joy.save();
        }
    }
    //</editor-fold>
    
    private class InputDetails {
        private MugenDefFile.GroupEntry group;
        private MugenDefFile.KVEntry up,down,left,right,a,b,c,x,y,z,start;
        private String upStr,downStr,leftStr,rightStr,aStr,bStr,cStr,xStr,yStr,zStr,startStr;
        
        public  void setGroup(MugenDefFile.GroupEntry group){
            this.group = group;
            this.reload();
        }
        
        private void reload(){
            for (int i=0;i<group.size();i++){
                MugenDefFile.Entry entry = group.getEntry(i);
                
                if (entry instanceof MugenDefFile.KVEntry){
                    MugenDefFile.KVEntry kvEntry = (MugenDefFile.KVEntry)entry;
                    if (kvEntry.getKey().equals("Jump")) this.up = kvEntry;
                    else if (kvEntry.getKey().equals("Crouch")) this.down = kvEntry;
                    else if (kvEntry.getKey().equals("Left")) this.left = kvEntry;
                    else if (kvEntry.getKey().equals("Right")) this.right = kvEntry;
                    else if (kvEntry.getKey().equals("A")) this.a = kvEntry;
                    else if (kvEntry.getKey().equals("B")) this.b = kvEntry;
                    else if (kvEntry.getKey().equals("C")) this.c = kvEntry;
                    else if (kvEntry.getKey().equals("X")) this.x = kvEntry;
                    else if (kvEntry.getKey().equals("Y")) this.y = kvEntry;
                    else if (kvEntry.getKey().equals("Z")) this.z = kvEntry;
                    else if (kvEntry.getKey().equals("Start")) this.start = kvEntry;
                }
            }
            this.updateData();
        }
        
        private void updateData(){
            this.upStr = getKVString(this.up);
            this.downStr = getKVString(this.down);
            this.leftStr = getKVString(this.left);
            this.rightStr = getKVString(this.right);
            this.aStr = getKVString(this.a);
            this.bStr = getKVString(this.b);
            this.cStr = getKVString(this.c);
            this.xStr = getKVString(this.x);
            this.yStr = getKVString(this.y);
            this.zStr = getKVString(this.z);
            this.startStr = getKVString(this.start);
        }
        
        public void save(){
            this.up.setValue(this.upStr);
            this.down.setValue(this.downStr);
            this.left.setValue(this.leftStr);
            this.right.setValue(this.rightStr);
            this.a.setValue(this.aStr);
            this.b.setValue(this.bStr);
            this.c.setValue(this.cStr);
            this.x.setValue(this.xStr);
            this.y.setValue(this.yStr);
            this.z.setValue(this.zStr);
            this.start.setValue(this.startStr);
        }
        
        public String getUp(){ return this.upStr; }
        public String getDown(){ return this.downStr; }
        public String getLeft(){ return this.leftStr; }
        public String getRight(){ return this.rightStr; }
        public String getA(){ return this.aStr; }
        public String getB(){ return this.bStr; }
        public String getC(){ return this.cStr; }
        public String getX(){ return this.xStr; }
        public String getY(){ return this.yStr; }
        public String getZ(){ return this.zStr; }
        public String getStart(){ return this.startStr; }
        
        public void setUp(String up){ this.upStr=up; }
        public void setDown(String down){ this.downStr=down; }
        public void setLeft(String left){ this.leftStr=left; }
        public void setRight(String right){ this.rightStr=right; }
        public void setA(String a){ this.aStr=a; }
        public void setB(String b){ this.bStr=b; }
        public void setC(String c){ this.cStr=c; }
        public void setX(String x){ this.xStr=x; }
        public void setY(String y){ this.yStr=y; }
        public void setZ(String z){ this.zStr=z; }
        public void setStart(String start){ this.startStr=start; }
    }
    
    private static class PluginDetails {
        private String pluginName,pluginFile;
        private MugenDefFile.KVEntry entry;
        private ArrayList<PluginParam> params;
        
        public PluginDetails(String pluginName, String pluginFile){
            this.pluginName = pluginName;
            this.pluginFile = pluginFile;
            MugenDefFile.ListEntry value = new MugenDefFile.ListEntry();
            value.addEntry(pluginName);
            value.addEntry(pluginFile);
            this.entry = new MugenDefFile.KVEntry("plugin",value);
            this.params = new ArrayList<PluginParam>();
        }
        
        public PluginDetails(MugenDefFile.KVEntry entry){
            this.entry = entry;
            this.params = new ArrayList<PluginParam>();
            
            MugenDefFile.Entry valueEntry = entry.getValue();
            
            if (valueEntry instanceof MugenDefFile.ListEntry){
                MugenDefFile.ListEntry valueListEntry = (MugenDefFile.ListEntry)valueEntry;
                if (valueListEntry.size()==2){
                    MugenDefFile.Entry nameEntry = valueListEntry.getEntry(0);
                    MugenDefFile.Entry fileEntry = valueListEntry.getEntry(1);
                    
                    if (nameEntry instanceof MugenDefFile.StringEntry) this.pluginName = ((MugenDefFile.StringEntry)nameEntry).getString();
                    if (fileEntry instanceof MugenDefFile.StringEntry) this.pluginFile = ((MugenDefFile.StringEntry)fileEntry).getString();
                }else{
                    MugenDefFile.Entry nameEntry = valueListEntry.getEntry(0);
                    if (nameEntry instanceof MugenDefFile.StringEntry) this.pluginName = ((MugenDefFile.StringEntry)nameEntry).getString();
                    this.pluginFile = "";
                }
            }else if (valueEntry instanceof MugenDefFile.StringEntry){
                this.pluginName = ((MugenDefFile.StringEntry)valueEntry).getString();
                this.pluginFile = "";
            }
        }
        
        public PluginDetails(MugenDefFile.KVEntry entry, ArrayList<MugenDefFile.KVEntry> params){
            this.entry = entry;
            this.params = new ArrayList<PluginParam>();
            for (int i=0;i<params.size();i++) this.params.add(new PluginParam(params.get(i)));
            
            MugenDefFile.Entry valueEntry = entry.getValue();
            
            if (valueEntry instanceof MugenDefFile.ListEntry){
                MugenDefFile.ListEntry valueListEntry = (MugenDefFile.ListEntry)valueEntry;
                if (valueListEntry.size()==2){
                    MugenDefFile.Entry nameEntry = valueListEntry.getEntry(0);
                    MugenDefFile.Entry fileEntry = valueListEntry.getEntry(1);
                    
                    if (nameEntry instanceof MugenDefFile.StringEntry) this.pluginName = ((MugenDefFile.StringEntry)nameEntry).getString();
                    if (fileEntry instanceof MugenDefFile.StringEntry) this.pluginFile = ((MugenDefFile.StringEntry)fileEntry).getString();
                }else{
                    MugenDefFile.Entry nameEntry = valueListEntry.getEntry(0);
                    if (nameEntry instanceof MugenDefFile.StringEntry) this.pluginName = ((MugenDefFile.StringEntry)nameEntry).getString();
                    this.pluginFile = "";
                }
            }else if (valueEntry instanceof MugenDefFile.StringEntry){
                this.pluginName = ((MugenDefFile.StringEntry)valueEntry).getString();
                this.pluginFile = "";
            }
        }
        
        public MugenDefFile.KVEntry getPluginEntry(){ return this.entry; }
        public ArrayList<PluginParam> getPluginParams(){ return this.params; }
        
        public void clearParams(){ this.params.clear(); }
        
        @Override
        public String toString(){ return this.pluginName; }
        
        public static class PluginParam {
            //String key,val;
            private MugenDefFile.KVEntry entry;
            
            public PluginParam(String key, String val){
                //this.key = key;
                //this.val = val;
                this.entry = new MugenDefFile.KVEntry(key, val);
            }
            
            public PluginParam(MugenDefFile.KVEntry entry){
                this.entry = entry;
                //this.key = entry.getKey();
                //MugenDefFile.Entry value = entry.getValue();
                //if (value instanceof MugenDefFile.StringEntry) this.val = ((MugenDefFile.StringEntry)value).getString();
                //else this.val = "";
            }
            
            public MugenDefFile.KVEntry getKVEntry(){ return this.entry; }
            
            @Override
            public String toString(){
                //return this.key+"="+this.val;
                return this.entry.getKey() + "=" + entry.getValue().toString();
            }
        }
    }
}
