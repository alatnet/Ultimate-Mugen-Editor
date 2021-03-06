/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.UME.Core;

import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import org.openide.util.NbPreferences;

final class UME_BasicPanel extends javax.swing.JPanel {

    private final UME_BasicOptionsPanelController controller;

    UME_BasicPanel(UME_BasicOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        this.cmbxZipCompressionLvl.setSelectedIndex(0);
        // TODO listen to changes in form fields and call controller.changed()
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel3 = new javax.swing.JPanel();
        ZipCompressionSettings = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        cmbxZipCompressionLvl = new javax.swing.JComboBox();
        MugenSettings = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtFieldMugenFolder = new javax.swing.JTextField();
        txtFieldMugenExec = new javax.swing.JTextField();
        txtFieldMugenMotifFld = new javax.swing.JTextField();
        btnMugenFolderBrowse = new javax.swing.JButton();
        btnMugenExecBrowse = new javax.swing.JButton();
        btnMugenMotifFldBrowse = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        btnMugenPluginsBrowse = new javax.swing.JButton();
        txtFieldMugenPluginsFolder = new javax.swing.JTextField();

        setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.border.title"))); // NOI18N

        ZipCompressionSettings.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.ZipCompressionSettings.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.jLabel5.text")); // NOI18N

        cmbxZipCompressionLvl.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-1 (Default Compression)", "0 (No Compression)", "1 (Best Speed)", "2", "3", "4", "5", "6", "7", "8", "9 (Best Compression)" }));

        javax.swing.GroupLayout ZipCompressionSettingsLayout = new javax.swing.GroupLayout(ZipCompressionSettings);
        ZipCompressionSettings.setLayout(ZipCompressionSettingsLayout);
        ZipCompressionSettingsLayout.setHorizontalGroup(
            ZipCompressionSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ZipCompressionSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmbxZipCompressionLvl, 0, 231, Short.MAX_VALUE)
                .addContainerGap())
        );
        ZipCompressionSettingsLayout.setVerticalGroup(
            ZipCompressionSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ZipCompressionSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel5)
                .addComponent(cmbxZipCompressionLvl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        MugenSettings.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.MugenSettings.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.jLabel3.text")); // NOI18N

        txtFieldMugenFolder.setText(org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.txtFieldMugenFolder.text")); // NOI18N

        txtFieldMugenExec.setText(org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.txtFieldMugenExec.text")); // NOI18N

        txtFieldMugenMotifFld.setText(org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.txtFieldMugenMotifFld.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btnMugenFolderBrowse, org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.btnMugenFolderBrowse.text")); // NOI18N
        btnMugenFolderBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMugenFolderBrowseActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btnMugenExecBrowse, org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.btnMugenExecBrowse.text")); // NOI18N
        btnMugenExecBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMugenExecBrowseActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(btnMugenMotifFldBrowse, org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.btnMugenMotifFldBrowse.text")); // NOI18N
        btnMugenMotifFldBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMugenMotifFldBrowseActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.jLabel4.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btnMugenPluginsBrowse, org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.btnMugenPluginsBrowse.text")); // NOI18N
        btnMugenPluginsBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMugenPluginsBrowseActionPerformed(evt);
            }
        });

        txtFieldMugenPluginsFolder.setText(org.openide.util.NbBundle.getMessage(UME_BasicPanel.class, "UME_BasicPanel.txtFieldMugenPluginsFolder.text")); // NOI18N

        javax.swing.GroupLayout MugenSettingsLayout = new javax.swing.GroupLayout(MugenSettings);
        MugenSettings.setLayout(MugenSettingsLayout);
        MugenSettingsLayout.setHorizontalGroup(
            MugenSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MugenSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(MugenSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(MugenSettingsLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFieldMugenFolder, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnMugenFolderBrowse))
                    .addGroup(MugenSettingsLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFieldMugenExec, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnMugenExecBrowse))
                    .addGroup(MugenSettingsLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFieldMugenMotifFld, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnMugenMotifFldBrowse))
                    .addGroup(MugenSettingsLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFieldMugenPluginsFolder, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnMugenPluginsBrowse)))
                .addContainerGap())
        );
        MugenSettingsLayout.setVerticalGroup(
            MugenSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MugenSettingsLayout.createSequentialGroup()
                .addGroup(MugenSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtFieldMugenFolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnMugenFolderBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MugenSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtFieldMugenExec, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnMugenExecBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MugenSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtFieldMugenMotifFld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnMugenMotifFldBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MugenSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(btnMugenPluginsBrowse)
                    .addComponent(txtFieldMugenPluginsFolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(ZipCompressionSettings, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(MugenSettings, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(MugenSettings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ZipCompressionSettings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnMugenFolderBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMugenFolderBrowseActionPerformed
        File mugenFolder = null;
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setDialogTitle("Locate Mugen Folder");

        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            mugenFolder = fileChooser.getSelectedFile();
            this.txtFieldMugenFolder.setText(mugenFolder.getPath());
        }
    }//GEN-LAST:event_btnMugenFolderBrowseActionPerformed

    private void btnMugenExecBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMugenExecBrowseActionPerformed
        File mugenExecutable = null;
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setCurrentDirectory(new File(this.txtFieldMugenFolder.getText()));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setDialogTitle("Locate Mugen Mugen Executable");

        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            mugenExecutable = fileChooser.getSelectedFile();
            this.txtFieldMugenExec.setText(mugenExecutable.getName());
        }
    }//GEN-LAST:event_btnMugenExecBrowseActionPerformed

    private void btnMugenMotifFldBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMugenMotifFldBrowseActionPerformed
        File mugenFolder = null;
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setCurrentDirectory(new File(this.txtFieldMugenFolder.getText()));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setDialogTitle("Locate Motif Folder");

        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            mugenFolder = fileChooser.getSelectedFile();
            this.txtFieldMugenMotifFld.setText(mugenFolder.getName());
        }
    }//GEN-LAST:event_btnMugenMotifFldBrowseActionPerformed

    private void btnMugenPluginsBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMugenPluginsBrowseActionPerformed
        File mugenFolder = null;
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setCurrentDirectory(new File(this.txtFieldMugenFolder.getText()));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setDialogTitle("Locate Plugins Folder");

        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            mugenFolder = fileChooser.getSelectedFile();
            this.txtFieldMugenPluginsFolder.setText(mugenFolder.getName());
        }
    }//GEN-LAST:event_btnMugenPluginsBrowseActionPerformed

    void load() {
        // TODO read settings and initialize GUI
        // Example:        
        // someCheckBox.setSelected(Preferences.userNodeForPackage(UME_BasicPanel.class).getBoolean("someFlag", false));
        // or for org.openide.util with API spec. version >= 7.4:
        // someCheckBox.setSelected(NbPreferences.forModule(UME_BasicPanel.class).getBoolean("someFlag", false));
        // or:
        // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());
        Preferences prefs = NbPreferences.forModule(UME_BasicPanel.class);
        
        this.txtFieldMugenFolder.setText(prefs.get("Mugen_Folder", "C:\\Mugen"));
        this.txtFieldMugenExec.setText(prefs.get("Mugen_Exec", "mugen.exe"));
        this.txtFieldMugenMotifFld.setText(prefs.get("Mugen_Motif_Folder", "data"));
        this.txtFieldMugenPluginsFolder.setText(prefs.get("Mugen_Plugins_Folder", "plugins"));
        this.cmbxZipCompressionLvl.setSelectedIndex(prefs.getInt("Zip_Compression_Level", 0));
    }

    void store() {
        // TODO store modified settings
        // Example:
        // Preferences.userNodeForPackage(UME_BasicPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(UME_BasicPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());
        Preferences prefs = NbPreferences.forModule(UME_BasicPanel.class);
        
        prefs.put("Mugen_Folder", this.txtFieldMugenFolder.getText());
        prefs.put("Mugen_Exec", this.txtFieldMugenExec.getText());
        prefs.put("Mugen_Motif_Folder", this.txtFieldMugenMotifFld.getText());
        prefs.put("Mugen_Plugins_Folder", this.txtFieldMugenPluginsFolder.getText());
        prefs.putInt("Zip_Compression_Level", this.cmbxZipCompressionLvl.getSelectedIndex());
    }

    boolean valid() {
        // TODO check whether form is consistent and complete
        return true;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel MugenSettings;
    private javax.swing.JPanel ZipCompressionSettings;
    private javax.swing.JButton btnMugenExecBrowse;
    private javax.swing.JButton btnMugenFolderBrowse;
    private javax.swing.JButton btnMugenMotifFldBrowse;
    private javax.swing.JButton btnMugenPluginsBrowse;
    private javax.swing.JComboBox cmbxZipCompressionLvl;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField txtFieldMugenExec;
    private javax.swing.JTextField txtFieldMugenFolder;
    private javax.swing.JTextField txtFieldMugenMotifFld;
    private javax.swing.JTextField txtFieldMugenPluginsFolder;
    // End of variables declaration//GEN-END:variables
}
