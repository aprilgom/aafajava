package distributeddrive;
import java.util.*;
import java.io.Serializable;

public class FileTable implements Serializable{

    private List<String> filetable;
    public FileTable(){
        filetable = new ArrayList<String>();
    }
    public void writeOn(String which_drive){
        filetable.add(which_drive);
    }
    public List<String> getFileTable(){ 
        return filetable;
    }
}