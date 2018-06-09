package distributeddrive;
import java.io.*;
import java.util.Collections.*;
import java.util.*;


public class DriveManager{
    private static int SEND_LIMIT = 209715200; //209715200byte = 200MiB
    private static Map<String,CloudDrive> CloudDriveMap = new HashMap<String,CloudDrive>();
    private static Map<String,Long> DriveSpaceMap = new HashMap<String,Long>();
    private static Map<String,List<String>> FileTableMap ;
    
    public DriveManager(){
        try{
            FileInputStream fis = new FileInputStream("filetablemap.bin");
            ObjectInputStream ois = new ObjectInputStream(fis);
            FileTableMap = (Map<String,List<String>>)ois.readObject();
        }catch(FileNotFoundException filenotfounde){
            try{
                FileTableMap = new HashMap<String,List<String>>();
                FileOutputStream fos = new FileOutputStream("filetablemap.bin");
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(FileTableMap);
                oos.close();
            }catch(Exception e){
                System.out.println("error while making new filetablemap"+e);
            }
        }catch(Exception e){
            System.out.println("error while loading filetablemap"+e);
        }
        System.out.println(FileTableMap.toString());
    }

    public void uploadFile(java.io.File file){
        try{
            long drive_space_sum = 0;
            long left_file_volume = file.length();
            String filename = file.getName();
            byte[] filebyte = new byte[(int)file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(filebyte);
            fis.close();
            //check if drives have enough space
            for(String key:DriveSpaceMap.keySet()){
                drive_space_sum += DriveSpaceMap.get(key);
            }
            if(drive_space_sum < left_file_volume){
                System.out.println("there is not enough space to upload this file..");
                return;
            }
            //check end
            List<String> filetable = new ArrayList<String>();

            int start_of_part = 0;
            int end_of_part = 0;
            int fileorder = 0;
            while(left_file_volume > 0){
                for(String key:CloudDriveMap.keySet()){
                    long leftspace = DriveSpaceMap.get(key).longValue();
                    System.out.println(key+" leftspace:"+leftspace);
                    System.out.println(left_file_volume + "bytes left");
                    if(leftspace == 0){
                        continue;
                    }
                    if(left_file_volume < SEND_LIMIT){
                        end_of_part = (int)file.length();
                        DriveSpaceMap.put(key,leftspace-left_file_volume);
                        left_file_volume = 0;
                    }else if(leftspace<2*SEND_LIMIT){
                        end_of_part = start_of_part + (int)leftspace;
                        DriveSpaceMap.put(key,(long)0);
                        left_file_volume -= leftspace;
                    }else{
                        end_of_part = start_of_part + SEND_LIMIT;
                        DriveSpaceMap.put(key,(long)(leftspace-SEND_LIMIT));
                        left_file_volume -= SEND_LIMIT;
                    }
                    CloudDriveMap.get(key).uploadFile(Arrays.copyOfRange(filebyte,start_of_part,end_of_part),filename + fileorder);
                    System.out.println(start_of_part + " ~ " + end_of_part + "sended to "+key);
                    
                    start_of_part = end_of_part;
                    fileorder += 1;
                    filetable.add(key);
                    if(left_file_volume <= 0)break;
                }
            }
            FileTableMap.put(file.getName(),filetable);
            FileOutputStream fos = new FileOutputStream("filetablemap.bin");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(FileTableMap);
            oos.close();
        }catch(Exception e){
            System.out.println(e);
        }
    }
    public void downloadFile(String filename){
        try{
            java.io.File result = new File(filename);
            FileOutputStream fos = new FileOutputStream(result);
            List<String> filetable = FileTableMap.get(filename);
            int fileorder = 0;
            for(String whichdrive:filetable){
                CloudDriveMap.get(whichdrive).downloadFile(filename + fileorder).writeTo(fos);
                fileorder++;
            }
        }catch(Exception e){
            System.out.println(e);
        }
    }
    public void addDrive(String drivename,CloudDrive drive){
        CloudDriveMap.put(drivename,drive);
        DriveSpaceMap.put(drivename,drive.getSpaceLeft());
    }
}