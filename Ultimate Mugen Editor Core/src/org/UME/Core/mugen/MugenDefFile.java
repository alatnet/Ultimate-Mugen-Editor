/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.UME.Core.mugen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author alatnet
 */
public class MugenDefFile implements Cloneable {
    protected String filename = null;
    protected InputStream stream = null;
    protected ArrayList<Entry> entries;
    protected boolean readOnly = false;

    public MugenDefFile(String filename){
        this.filename = filename;
        this.entries = new ArrayList<Entry>();
    }
    public MugenDefFile(InputStream stream){
        this.stream = stream;
        this.readOnly = true;
        this.entries = new ArrayList<Entry>();
    }

    public void setFileName(String filename){ this.filename = filename; }

    public void addEntry(Entry line){ this.entries.add(line); }
    public Entry getEntry(int index){ return this.entries.get(index); }
    public Entry removeEntry(int index){ return this.entries.remove(index); }
    public void removeEntry(Entry line){ this.entries.remove(line); }

    public GroupEntry getGroup(String group){
        GroupEntry ret = null;
        GroupEntry test = null;
        Entry entry = null;
        
        for (int i=0;i<this.entries.size();i++){
            entry = null;
            test = null;
            entry = this.entries.get(i);
            if (entry.getClass() == GroupEntry.class){
                test = (GroupEntry) entry;
                if (test.getName().equals(group)){
                    ret = test;
                    break;
                }
            }
        }

        return ret;
    }

    public int size(){ return this.entries.size(); }

    public Entry[] toArray(){
        Entry[] ret = new Entry[this.entries.size()];
        for (int i=0;i<this.entries.size();i++) ret[i] = this.entries.get(i);
        return ret;
    }

    public ArrayList<Entry> getArrayList(){ return this.entries; }

    static public ListEntry parseListEntry(String str){
        ListEntry listEntry = new ListEntry();

        for (int i=0;i<str.length();i++){
            if (str.charAt(i) == ';'){
                listEntry.setInlineComment(new CommentEntry(str.substring(i+1)));
                str = str.substring(0,i);
                break;
            }
        } //scan for inlineComment pos


        Scanner StrData = new Scanner(str);
        StrData.useDelimiter(",");

        while (StrData.hasNext()){
            String data = StrData.next();
            Entry entry = null;
            boolean entryIsString = true;

            for (int i2=0;i2<data.length();i2++){ //scan to see if the data is a key-value pair
                if (data.charAt(i2)=='='){
                    KVEntry kventry = parseKVEntry(data);
                    kventry.setMultilineComment(null);
                    kventry.setInlineComment(null);
                    entry = kventry;
                    entryIsString = false;
                    break;
                }
            }

            if (entryIsString && entry == null) entry = new StringEntry(data.trim());

            listEntry.addEntry(entry);
        }
        return listEntry;
    }
    static public KVEntry parseKVEntry(String str){
        Entry entry = null;
        boolean entryIsList = false;

        String key = "", value = "";

        for (int i=0;i<str.length();i++){ //get pos of '=' and split to k-v pair
            if (str.charAt(i)=='='){
                key = str.substring(0, i);
                value = str.substring(i+1);
                break;
            }
        }

        //if (key.charAt(key.length()-1)==' ') key = key.substring(0, key.length()-1);
        key = key.trim();
        //if (value.length() != 0 && value.charAt(0)==' ') value = value.substring(1);
        //value = StringEntry.removeWhitespaces(value);

        String comment = null;

        for (int i=0;i<value.length();i++){
            if (value.charAt(i) == ';'){
                comment = value.substring(i+1);
                value = value.substring(0,i);
                break;
            }
        }

        value = value.trim();
        if (comment != null) comment = comment.trim();

        for (int i=0;i<value.length();i++){ //scan to see if value is a list
            if (value.charAt(i)==','){
                entry = parseListEntry(value);
                entryIsList=true;
            }else if (value.charAt(i)=='='){
                entry = parseKVEntry(value);
                entryIsList=true;
            }
        }

        if (!entryIsList && entry == null) entry = new StringEntry(value);

        //System.out.println("KEY: "+key+"  VAL: "+entry.toString()+"  COMMENT: "+comment);

        return new KVEntry(key,entry,new CommentEntry(comment));
    }
    
    public void read() throws FileNotFoundException, IOException{
        this.entries.clear();
        BufferedReader BR = null;
        if (this.filename != null) BR = new BufferedReader(new FileReader(this.filename));
        else if (this.stream != null) BR = new BufferedReader(new InputStreamReader(this.stream));

        if (BR == null) throw new IOException("Error opening the buffered reader.");

        String LINE = "";
        char CHARAT = ' ';

        GroupEntry currentGroup = null;
        CommentEntry currentComment = null;

        boolean isStringEntry = true;
        
        while (LINE!=null){
            isStringEntry = true;
            
            LINE = BR.readLine();

            if (LINE == null) break; //check if we have reached the end of the data (precautionary);

            if (LINE.length() != 0){ //check if we have data;
                for (int i=0;i<LINE.length();i++){
                    CHARAT = LINE.charAt(i);
                    if (CHARAT == '['){ //new group
                        /*if (currentComment != null){
                            this.entries.add(currentComment);
                            currentComment = null;
                        }*/
                        int i2 = i;
                        for (;i2<LINE.length();i2++) if (LINE.charAt(i2) == ']') break;
                        currentGroup = new GroupEntry(LINE.substring(i+1,i2));

                        if (currentComment != null){
                            if (currentComment.size() > 0){
                                if (currentComment.size() == 1){
                                    currentGroup.setInlineComment(currentComment);
                                }else{
                                    currentGroup.setMultilineComment(currentComment);
                                }
                            }
                            currentComment = null;
                        }

                        for (;i2<LINE.length();i2++){
                            if (LINE.charAt(i2) == ';'){
                                String comment = LINE.substring(i2+1);
                                if (comment.length() > 0){
                                    if (currentGroup.getInlineComment()!=null) currentGroup.setMultilineComment(currentGroup.getInlineComment());
                                    currentGroup.setInlineComment(new CommentEntry(comment));
                                }
                                break;
                            }
                        }

                        this.entries.add(currentGroup);
                        break;
                    }else if (CHARAT == ';'){ //it's a inlineComment, add it to entries
                        if (currentComment==null) currentComment = new CommentEntry(LINE.substring(i+1));
                        else currentComment.addString(LINE.substring(i+1));
                        break;
                    }else if (CHARAT == ' '){ //Ignore Whitespace
                    }else{ //parse for entry (string, list, or key-value)
                        if (currentGroup != null){
                            for (int i2=i;i2<LINE.length();i2++){ //check if it's a k-v pair or a list
                                if (LINE.charAt(i2)==','){ //list entry
                                    ListEntry listEntry = parseListEntry(LINE);
                                    if (currentComment != null){
                                        if (currentComment.size() == 1) listEntry.setInlineComment(currentComment);
                                        else listEntry.setMultilineComment(currentComment);
                                        currentComment = null;
                                    }
                                    currentGroup.addEntry(listEntry);
                                    isStringEntry = false;
                                    break;
                                }else if (LINE.charAt(i2)=='='){ //Key-Value entry
                                    KVEntry kvEntry = parseKVEntry(LINE);
                                    if (currentComment != null){
                                        if (currentComment.size() == 1) kvEntry.setInlineComment(currentComment);
                                        else kvEntry.setMultilineComment(currentComment);
                                        currentComment = null;
                                    }
                                    currentGroup.addEntry(kvEntry);
                                    isStringEntry = false;
                                    break;
                                }
                            }
                            if (isStringEntry){
                                StringEntry strEntry = new StringEntry(LINE);
                                if (currentComment!=null){
                                    if (currentComment.size() == 1) strEntry.setInlineComment(currentComment);
                                    else strEntry.setMultilineComment(currentComment);
                                }

                                String comment = "";

                                for (int i2=0;i2<LINE.length();i2++){
                                    if (LINE.charAt(i2) == ';'){
                                        comment = LINE.substring(i2+1);
                                        break;
                                    }
                                }

                                if (comment.length() > 0){
                                    if (currentComment!=null) strEntry.setMultilineComment(strEntry.getInlineComment());
                                    strEntry.setInlineComment(new CommentEntry(comment));
                                }

                                currentGroup.addEntry(strEntry);
                                currentComment = null;
                            }
                            break;
                        }
                    }
                }
            }
        }
        BR.close();
    }
    
    public void write() throws IOException {
        if (!this.readOnly){
            PrintWriter PW = new PrintWriter(new BufferedWriter(new FileWriter(this.filename)));
            PW.print(this.toString());
            PW.close();
        }
    }
    public void write(String fileName) throws IOException {
        PrintWriter PW = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        PW.print(this.toString());
        PW.close();
     }

    public interface Entry {
        @Override
        public String toString();
    }
    static public class KVEntry implements Entry, Cloneable {
        private String key = null;
        private Entry val = null;
        private CommentEntry inlineComment = null, multilineComment=null;

        public KVEntry(String key){ this(key,"",null); }
        public KVEntry(String key, Entry val){ this(key,val,null); }
        public KVEntry(String key, String val){ this(key,new StringEntry(val),null); }
        public KVEntry(String key, Entry val, CommentEntry commentEntry){
            this.key = key.trim();
            this.val = val;
            if (commentEntry != null){
                if (commentEntry.size() == 1) this.inlineComment = commentEntry;
                else this.multilineComment = commentEntry;
            }
        }
        public KVEntry(String key, String val, CommentEntry commentEntry){ this(key,new StringEntry(val),commentEntry); }

        public String getKey(){ return this.key; }
        public Entry getValue(){ return this.val; }
        public void setValue(Entry val){ this.val = val; }
        public void setValue(String val){ this.val = new StringEntry(val); }
        public CommentEntry getInlineComment(){ return this.inlineComment; }
        public void setInlineComment(CommentEntry inlineComment){ this.inlineComment = inlineComment; }
        public CommentEntry getMultilineComment(){ return this.multilineComment; }
        public void setMultilineComment(CommentEntry multilineComment){ this.multilineComment = multilineComment; }

        @Override
        public String toString(){
            String ret = "";
            if (this.multilineComment != null && this.multilineComment.size() > 0) ret+=this.multilineComment.toString()+"\r\n";
            ret += this.key + "=" + this.val;
            if (this.inlineComment != null && this.inlineComment.size() == 1 && this.inlineComment.getString(0) != null) ret += " " + this.inlineComment.toString();
            return ret;
        }

        @Override
        public KVEntry clone(){
            KVEntry ret = new KVEntry(this.key);
            if (this.val instanceof ListEntry){
                ret.setValue(((ListEntry)this.val).clone());
            }else if (this.val instanceof KVEntry){
                ret.setValue(((KVEntry)this.val).clone());
            }else if (this.val instanceof StringEntry){
                ret.setValue(((StringEntry)this.val).clone());
            }

            if (this.inlineComment != null) ret.setInlineComment(this.inlineComment.clone());
            if (this.multilineComment != null) ret.setMultilineComment(this.multilineComment.clone());
            //ret.setValue();
            return null;
        }
    }
    static public class ListEntry implements Entry, Cloneable {
        private CommentEntry inlineComment = null, multilineComment = null;
        private ArrayList<Entry> entries;

        public ListEntry(){ this.entries = new ArrayList<Entry>(); }
        public ListEntry(Entry[] entries){
            this.entries = new ArrayList<Entry>();
            if (entries != null) for (int i=0; i<entries.length;i++) this.entries.add(entries[i]);
        }

        public void addEntry(Entry entry){ this.entries.add(entry); }
        public void addEntry(String str){ this.entries.add(new StringEntry(str)); }
        public void addEntry(int index, Entry entry){ this.entries.add(index,entry); }
        public void addEntry(int index, String str) { this.entries.add(index,new StringEntry(str)); }
        public Entry getEntry(int index){ return this.entries.get(index); }
        public Entry removeEntry(int index){ return this.entries.remove(index); }
        public void removeEntry(Entry entry){ this.entries.remove(entry); }

        public void setEntry(int index, Entry entry){ this.entries.set(index, entry); }
        public void setEntry(int index, String str){
            if (this.entries.get(index).getClass() == KVEntry.class){
                KVEntry kvEntry = (KVEntry) this.entries.get(index);
                kvEntry.setValue(new StringEntry(str));
            }else{
                this.entries.set(index, new StringEntry(str));
            }
        }

        public Entry[] toArray(){
            Entry[] ret = new Entry[this.entries.size()];
            for (int i=0;i<this.entries.size();i++) ret[i] = this.entries.get(i);
            return ret;
        }

        public void setInlineComment(CommentEntry inlineComment){ this.inlineComment = inlineComment; }
        public CommentEntry getInlineComment(){ return this.inlineComment; }

        public void setMultilineComment(CommentEntry multilineComment){ this.multilineComment = multilineComment; }
        public CommentEntry getMultilineComment(){ return this.multilineComment; }

        public int size(){ return this.entries.size(); }
        public void clear() { this.entries.clear(); }

        @Override
        public String toString(){
            String ret = "";

            if (this.multilineComment != null && this.multilineComment.size() > 0) ret+=this.multilineComment.toString()+"\r\n";

            for (int i=0;i<this.entries.size();i++){
                ret += this.entries.get(i).toString();
                if (i != this.entries.size()-1) ret+=",";
            }

            if (this.inlineComment != null && this.inlineComment.size() == 1 && this.inlineComment.getString(0) != null) ret+= " " + this.inlineComment.toString();
            
            return ret;
        }

        @Override
        public ListEntry clone(){
            ListEntry ret = new ListEntry();

            for (int i=0;i<this.entries.size();i++){
                if (this.entries.get(i) instanceof ListEntry){
                    ret.addEntry(((ListEntry)this.entries.get(i)).clone());
                }else if (this.entries.get(i) instanceof KVEntry){
                    ret.addEntry(((KVEntry)this.entries.get(i)).clone());
                }else if (this.entries.get(i) instanceof StringEntry){
                    ret.addEntry(((StringEntry)this.entries.get(i)).clone());
                }
            }

            if (this.inlineComment != null) ret.setInlineComment(this.inlineComment.clone());
            if (this.multilineComment != null) ret.setInlineComment(this.multilineComment.clone());
            return ret;
        }
    }
    static public class CommentEntry implements Entry, Cloneable {
        private ArrayList<String> str;

        public CommentEntry(){ this.str = new ArrayList<String>(); }

        public CommentEntry(String str){
            this();
            if (str != null) if (str.length() != 0 && str.charAt(0) == ';') str = str.substring(1);
            this.str.add(str);
        }

        public void addString(String str){
            if (str.length() > 0 && str.charAt(0) == ';') str = str.substring(1);
            this.str.add(str);
        }
        public String removeString(int index){ return this.str.remove(index); }
        public String getString(int index){ return this.str.get(index); }

        public int size() { return this.str.size(); }

        @Override
        public String toString(){
            String ret="";
            for (int i=0;i<this.str.size();i++){
                ret+=";"+this.str.get(i);
                if (i!=this.str.size()-1) ret+="\r\n";
            }
            return ret;
        }

        @Override
        public CommentEntry clone(){
            CommentEntry ret = new CommentEntry();
            for (int i=0;i<this.str.size();i++) ret.addString(this.str.get(i));
            return ret;
        }
    }
    static public class StringEntry implements Entry, Cloneable {
        private String str;
        private CommentEntry inlineComment = null, multilineComment=null;

        public StringEntry(){ this.str = ""; }
        public StringEntry(String str){ this.str = str.trim(); }
        public StringEntry(String str, CommentEntry inlineComment){
            this.str = str.trim();
            this.inlineComment = inlineComment;
        }
        public StringEntry(String str, CommentEntry inlineComment, CommentEntry multilineComment){
            this(str,inlineComment);
            this.multilineComment = multilineComment;
        }

        public CommentEntry getInlineComment(){ return this.inlineComment; }
        public void setInlineComment(CommentEntry inlineComment){ this.inlineComment = inlineComment; }
        public CommentEntry getMultilineComment(){ return this.multilineComment; }
        public void setMultilineComment(CommentEntry multilineComment){ this.multilineComment = multilineComment; }

        public String getString(){ return this.str; }

        @Override
        public String toString(){
            String ret = "";
            if (this.multilineComment != null && this.multilineComment.size() > 0) ret+=this.multilineComment.toString()+"\r\n";
            ret += this.str;
            if (this.inlineComment != null && this.inlineComment.size() == 1 && this.inlineComment.getString(0) != null) ret += " " + this.inlineComment.toString();
            return ret;
            //return this.str;
        }

        @Override
        public StringEntry clone(){ return new StringEntry(this.str); }
    }
    //TODO Change group entry so that it has comments?
    static public class GroupEntry implements Entry, Cloneable {
        private String name = null;
        private ArrayList<Entry> entries=null;
        private CommentEntry inlineComment = null, multilineComment=null;

        public GroupEntry(String name){
            this.name = name;
            this.entries = new ArrayList<Entry>();
        }
        public GroupEntry(String name, CommentEntry inlineComment){
            this(name);
            this.inlineComment = inlineComment;
        }
        public GroupEntry(String name, CommentEntry inlineComment, CommentEntry multilineComment){
            this(name,inlineComment);
            this.multilineComment = multilineComment;
        }
        
        public void addEntry(Entry entry){ this.entries.add(entry); }
        public void addEntry(int index, Entry entry){ this.entries.add(index,entry); }
        public Entry getEntry(int index){ return this.entries.get(index); }
        public Entry removeEntry(int index){ return this.entries.remove(index); }
        public void removeEntry(Entry entry){ this.entries.remove(entry); }
        
        public void clear(){ this.entries.clear(); }

        public CommentEntry getInlineComment(){ return this.inlineComment; }
        public void setInlineComment(CommentEntry inlineComment){ this.inlineComment = inlineComment; }
        public CommentEntry getMultilineComment(){ return this.multilineComment; }
        public void setMultilineComment(CommentEntry multilineComment){ this.multilineComment = multilineComment; }

        public Entry[] toArray(){
            Entry[] ret = new Entry[this.entries.size()];
            for (int i=0;i<this.entries.size();i++) ret[i] = this.entries.get(i);
            return ret;
        }
        
        public int size(){ return this.entries.size(); }

        public String getName(){ return this.name; }
        
        @Override
        public String toString(){
            String ret = "["+this.name+"]\r\n";

            for (int i=0;i<this.entries.size();i++){
                ret+=this.entries.get(i).toString()+"\r\n";
            }

            return ret;
        }

        @Override
        public GroupEntry clone(){
            GroupEntry ret = new GroupEntry(this.name);

            for (int i=0;i<this.entries.size();i++){
                if (this.entries.get(i) instanceof ListEntry){
                    ret.addEntry(((ListEntry)this.entries.get(i)).clone());
                }else if (this.entries.get(i) instanceof KVEntry){
                    ret.addEntry(((KVEntry)this.entries.get(i)).clone());
                }else if (this.entries.get(i) instanceof StringEntry){
                    ret.addEntry(((StringEntry)this.entries.get(i)).clone());
                }else if (this.entries.get(i) instanceof CommentEntry){
                    ret.addEntry(((CommentEntry)this.entries.get(i)).clone());
                }
            }

            return ret;
        }
    }

    @Override
    public String toString(){
        String ret = "";

        for (int i=0;i<this.entries.size();i++){
            ret += this.entries.get(i).toString();
            if (i!=this.entries.size()-1) ret+="\r\n";
        }

        return ret;
    }

    @Override
    public MugenDefFile clone(){
        MugenDefFile ret = null;
        if (this.filename != null) ret = new MugenDefFile(this.filename);
        else ret = new MugenDefFile(this.stream);

        if (ret != null){
            for (int i=0;i<this.entries.size();i++){
                if (this.entries.get(i) instanceof ListEntry){
                    ret.addEntry(((ListEntry)this.entries.get(i)).clone());
                }else if (this.entries.get(i) instanceof KVEntry){
                    ret.addEntry(((KVEntry)this.entries.get(i)).clone());
                }else if (this.entries.get(i) instanceof StringEntry){
                    ret.addEntry(((StringEntry)this.entries.get(i)).clone());
                }else if (this.entries.get(i) instanceof CommentEntry){
                    ret.addEntry(((CommentEntry)this.entries.get(i)).clone());
                }
            }
        }
        return ret;
    }
}
