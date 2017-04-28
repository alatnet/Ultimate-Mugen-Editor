/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.UME.Core.mugen;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author alatnet
 */
public class MugenSffFile {
    private String filename;
    private InputStream stream;
    SFF_Version sffVer, compatVer;
    SFF_Header sffHeader;
    
    public void read() throws FileNotFoundException, IOException{
        DataInputStream DIS = null;

        if (this.filename != null) DIS = new DataInputStream(new FileInputStream(this.filename));
        else if (this.stream != null) DIS = new DataInputStream(this.stream);
        if (DIS == null) throw new IOException("Cannot read file.");
        
        String elecbyteSig = "";
        
        for (int i=0;i<12;i++) elecbyteSig += (char)DIS.read();
        
        if (!elecbyteSig.equals("ElecbyteSpr\0")){ //check if it is an sff file
            DIS.close();
            throw new IOException("File is not SFF File.");
        }
        
        this.sffVer = new SFF_Version(DIS);
        if (this.sffVer.toString().equals("2.0.0.0")){ //sff version 2.0
            DIS.read();DIS.read(); //reserved
            compatVer = new SFF_Version(DIS);
            DIS.read();DIS.read(); //reserved
            this.sffHeader = new SFF_Header_v2(DIS);
        }else if (this.sffVer.toString().equals("1.0.1.0")){
        }
    }
    
    public interface SFF_Header {}
    public interface SFF_SpriteNode {}
    public interface SFF_PalNode {}
    
    static public class SFF_Version {
        private int lo3;
        private int lo2;
        private int lo1;
        private int hi;

        public SFF_Version(byte[] data, int offset) {
            this.lo3 = getInt_1Byte(data, offset);
            this.lo2 = getInt_1Byte(data, offset + 1);
            this.lo1 = getInt_1Byte(data, offset + 2);
            this.hi = getInt_1Byte(data, offset + 3);
        }
        
        public SFF_Version(DataInputStream DIS) throws IOException {
            this.lo3 = DIS.read();
            this.lo2 = DIS.read();
            this.lo1 = DIS.read();
            this.hi = DIS.read();
        }
        
        public int getHi(){ return this.hi; }
        public int getLo1(){ return this.lo1; }
        public int getLo2(){ return this.lo2; }
        public int getLo3(){ return this.lo3; }
        
        public void write(DataOutputStream DOS) throws IOException {
            //if (!compatVer) DOS.writeChars("ElecbyteSpr\0");
            DOS.writeByte(this.lo3);
            DOS.writeByte(this.lo2);
            DOS.writeByte(this.lo1);
            DOS.writeByte(this.hi);
            //DOS.writeInt(0); //reserved
            //DOS.writeInt(0); //reserved
        }
        
        @Override
        public String toString(){
            return this.hi + "." + this.lo1 + "." + this.lo2 + "." + this.lo3;
        }
    }
    
    static public class SFF_Header_v2 implements SFF_Header {
        private int offsetFirstNode = 0;
        private int totalNumSprites = 0;
        private int offsetFirstPalette = 0;
        private int totalNumPalettes = 0;
        private int ldataOffset = 0;
        private int ldataLen = 0;
        private int tdataOffset = 0;
        private int tdataLen = 0;

        public SFF_Header_v2(byte[] data) {
            this.offsetFirstNode = getInt_4Byte(data, 36);
            this.totalNumSprites = getInt_4Byte(data, 40);
            this.offsetFirstPalette = getInt_4Byte(data, 44);
            this.totalNumPalettes = getInt_4Byte(data, 48);
            this.ldataOffset = getInt_4Byte(data, 52);
            this.ldataLen = getInt_4Byte(data, 56);
            this.tdataOffset = getInt_4Byte(data, 60);
            this.tdataLen = getInt_4Byte(data, 64);
        }
        
        public SFF_Header_v2(DataInputStream DIS) throws IOException {
            this.offsetFirstNode = DIS.readInt();
            this.totalNumSprites = DIS.readInt();
            this.offsetFirstPalette = DIS.readInt();
            this.totalNumPalettes = DIS.readInt();
            this.ldataOffset = DIS.readInt();
            this.ldataLen = DIS.readInt();
            this.tdataOffset = DIS.readInt();
            this.tdataLen = DIS.readInt();
            DIS.read();DIS.read(); //reserved
            for (int i=0;i<436;i++) DIS.read(); //unused;
        }
        
        public int getOffsetFirstNode(){ return this.offsetFirstNode; }
        public int getNumSprites() { return this.totalNumSprites; }
        public int getOffsetFirstPalette() { return this.offsetFirstPalette; }
        public int getNumPalettes() { return this.totalNumPalettes; }
        public int lDataOffset() { return this.ldataOffset; }
        public int lDataLen() { return this.ldataLen; }
        public int tDataOffset() { return this.tdataOffset; }
        public int tDataLen() { return this.tdataLen; }
        
        public void write(DataOutputStream DOS) throws IOException {
            /*
            dec  hex  size   meaning
             0    0    12   "ElecbyteSpr\0" signature
            12    C     1   verlo3; 0
            13    D     1   verlo2; 0
            14    E     1   verlo1; 0
            15    F     1   verhi; 2
            16   10     4   reserved; 0
            20   14     4   reserved; 0
            24   18     1   compatverlo3; 0
            25   19     1   compatverlo1; 0
            26   1A     1   compatverlo2; 0
            27   1B     1   compatverhi; 2
            28   1C     4   reserved; 0
            32   20     4   reserved; 0
            36   24     4   offset where first sprite node header data is located
            40   28     4   Total number of sprites
            44   2C     4   offset where first palette node header data is located
            48   30     4   Total number of palettes
            52   34     4   ldata offset
            56   38     4   ldata length
            60   3C     4   tdata offset
            64   40     4   tdata length
            68   44     4   reserved; 0
            72   48     4   reserved; 0
            76   4C   436   unused
             */
            //DOS.writeInt(0); //reserved
            //DOS.writeInt(0); //reserved
            DOS.writeInt(this.offsetFirstNode); //offset where first sprite node header data is located
            DOS.writeInt(this.totalNumSprites); //Total number of sprites
            DOS.writeInt(this.offsetFirstPalette); //offset where first palette node header data is located
            DOS.writeInt(this.totalNumPalettes); //Total number of palettes
            DOS.writeInt(this.ldataOffset); //ldata offset
            DOS.writeInt(this.ldataLen); //ldata length
            DOS.writeInt(this.tdataOffset); //tdata offset
            DOS.writeInt(this.tdataLen); //tdata length
            DOS.writeInt(0); //reserved
            DOS.writeInt(0); //reserved
            for (int i=0;i<436;i++) DOS.writeByte(0); //unused
        }
        
        @Override
        public String toString(){
            String ret = "SFF Header v2:\n";
            ret += "-Offset First Node: " + this.offsetFirstNode + "\n";
            ret += "-Total Number of Sprites: " + this.totalNumSprites + "\n";
            ret += "-Offset First Palette: " + this.offsetFirstPalette + "\n";
            ret += "-Total Number of Palettes: " + this.totalNumPalettes + "\n";
            ret += "-l Data Offset: " + this.ldataOffset + "\n";
            ret += "-l Data Size: " + this.ldataLen + "\n";
            ret += "-t Data Offset: " + this.tdataOffset + "\n";
            ret += "-t Data Size: " + this.tdataLen;
            return ret;
        }
    }
    
    static public class SFF_Header_v101 implements SFF_Header {
    }
    
    private static boolean convertInt2Bool(int i) {
        if (i >= 1) return true;
        return false;
    }

    private static void copyByteData(byte[] src, byte[] dest, int offset) {
        for (int i = offset, i2 = 0; i < offset + dest.length; i++, i2++) {
            if (i2 >= dest.length-1) break;
            if (i >= src.length-1) break;
            dest[i2] = src[i];
        }
    }

    private static int getInt_4Byte(byte[] data, int offset) { return (((data[offset] & 0xFF)) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)); }
    private static int getInt_2Byte(byte[] data, int offset) { return (((data[offset] & 0xFF)) | ((data[offset + 1] & 0xFF) << 8)); }
    private static int getInt_1Byte(byte[] data, int offset) { return (data[offset] & 0xFF); }
}
