package distributeddrive;
import java.io.*;
import java.util.Collections.*;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class DriveManager{
    private static int SEND_LIMIT = 209715200; //209715200byte = 200MiB
    private static Map<String,CloudDrive> CloudDriveMap = new HashMap<String,CloudDrive>();
    private static Map<String,Long> DriveSpaceMap = new HashMap<String,Long>();
    private static Map<String,List<String>> FileTableMap ;
    private static int THREAD_LIMIT = 4;
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
        
    }

    public void uploadFile(String filepath){
        try{
            FileInputStream fis = new FileInputStream(filepath);
            long drive_space_sum = 0;
            Path path = Paths.get(filepath);
            long file_volume = Files.size(path);
            List<byte[]> filebytes = new ArrayList<byte[]>();
            String filename = path.getFileName().toString();
            //check if drives have enough space
            for(String key:DriveSpaceMap.keySet()){
                drive_space_sum += DriveSpaceMap.get(key);
            }
            if(drive_space_sum < file_volume){
                System.out.println("there is not enough space to upload this file..");
                return;
            }
            //check end
            List<String> filetable = new ArrayList<String>();


            int fileorder = 0;
            int start_of_part = 0;
            int end_of_part = 0;
            int thread_num = 0;
            int is_file_ended = 0;
            
            long file_remain = file_volume%SEND_LIMIT;
            if(file_volume<SEND_LIMIT){
                end_of_part = (int)file_volume;
            }else{
                end_of_part = SEND_LIMIT;
            }
            byte[] initbyte = new byte[SEND_LIMIT];
            is_file_ended = fis.read(initbyte);
            filebytes.add(initbyte);
            while(is_file_ended != -1){
                
                for(String key:CloudDriveMap.keySet()){
                    long leftspace = DriveSpaceMap.get(key).longValue();
                    byte[] filebyte = new byte[SEND_LIMIT];
                    System.out.println(key+" leftspace:"+leftspace);
                    if(leftspace == 0){
                        continue;
                    }
                    if(end_of_part == SEND_LIMIT){ 
                        //if all sent
                        is_file_ended = fis.read(filebyte);
                        filebytes.add(filebyte);
                        start_of_part = 0;
                        end_of_part = 0;
                    }
                    
                    if(is_file_ended != -1){
                        //if file is not ended
                        if(leftspace<SEND_LIMIT){
                            end_of_part = start_of_part + (int)leftspace;
                            DriveSpaceMap.put(key,(long)0);
                        }else{
                            end_of_part = SEND_LIMIT;
                            DriveSpaceMap.put(key,(long)(leftspace-SEND_LIMIT));
                        }
                    }else{
                        if(leftspace<file_remain){
                            end_of_part = start_of_part + (int)leftspace;
                            DriveSpaceMap.put(key,(long)0);
                        }else{
                            end_of_part = start_of_part + (int)file_remain;
                            DriveSpaceMap.put(key,(long)(leftspace-file_remain));
                        }
                    }
                    System.out.println("upload "+filename+fileorder+" on "+key);
                    System.out.println(start_of_part+"to"+end_of_part);
                    UploadThread uploadthread = new UploadThread(
                        this,
                        CloudDriveMap.get(key),
                        Arrays.copyOfRange(filebytes.get(0),start_of_part,end_of_part),
                        filename+fileorder
                    );
                    
                    uploadthread.start();
                    if(end_of_part == SEND_LIMIT){
                        filebytes.remove(0);
                    }
                    thread_num ++;
                    if(thread_num >= THREAD_LIMIT){
                        synchronized(this){
                            this.wait();
                        }
                        thread_num --;
                        System.gc();
                    }
                    start_of_part = end_of_part;
                    fileorder += 1;
                    filetable.add(key);
                    if(is_file_ended == -1)break;
                }
            }
            

            FileTableMap.put(filename,filetable);
            FileOutputStream fos = new FileOutputStream("filetablemap.bin");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(FileTableMap);
            oos.close();
        }catch(Exception e){

            System.out.println("DriveManager error while uploading:"+e+e.getMessage());
        }
    }
    public void downloadFile(String filename){
        try{
            java.io.File result = new File(filename);
            FileOutputStream fos = new FileOutputStream(result);
            List<String> filetable = FileTableMap.get(filename);
            List<DownloadThread> downloadThreadList = new ArrayList<DownloadThread>();
            int fileorder = -1;
            int thread_num = 0;
            for(String whichdrive:filetable){
                fileorder++;
                System.out.println("download "+filename+fileorder+whichdrive);
                DownloadThread dt = new DownloadThread(CloudDriveMap.get(whichdrive),filename,fileorder);
                downloadThreadList.add(dt);
                dt.start();
                thread_num++;
                if(thread_num >= THREAD_LIMIT ){
                    System.out.println("try to write "+filename+(fileorder-THREAD_LIMIT+1)+" from "+whichdrive);
                    downloadThreadList.get(0).join();
                    downloadThreadList.get(0).getResult().writeTo(fos);
                    downloadThreadList.remove(0);
                    System.out.println("writed "+filename+(fileorder-THREAD_LIMIT+1));
                    thread_num--;
                }
            }
            for(DownloadThread dt:downloadThreadList){
                dt.join();
                dt.getResult().writeTo(fos);
            }
            
        }catch(Exception e){
            System.out.println("Drive manager error while downloading file"+e);
        }
    }
    public void deleteFile(String filename){
      try{
        List<String> filetable = FileTableMap.get(filename);
        int fileorder = 0;
        for(String whichdrive:filetable){
          CloudDriveMap.get(whichdrive).deleteFile(filename+fileorder);
          fileorder++;
        }
        FileTableMap.remove(filename);
        FileOutputStream fos = new FileOutputStream("filetablemap.bin");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(FileTableMap);
        oos.close();
      }catch(Exception e){
        System.out.println(e);
      }
    }
    public void listFile(){
      for(String key:FileTableMap.keySet()){
        System.out.println(key);
      }
    }
    public void addDrive(String drivename,CloudDrive drive){
        CloudDriveMap.put(drivename,drive);
        DriveSpaceMap.put(drivename,drive.getSpaceLeft());
    }
}

class UploadThread extends Thread{
  private CloudDrive cloudDrive;
  private byte[] bytearray;
  private String filename;
  private DriveManager driveManager;
  public UploadThread(DriveManager dm,CloudDrive drive,byte[] bytearr,String name){
    cloudDrive = drive;
    bytearray = bytearr;
    filename = name;
    driveManager = dm;
  }
  public void run(){
    System.out.println("Thread tries to upload file");
    cloudDrive.uploadFile(bytearray,filename);
    System.out.println("Thread upload done");
    synchronized(driveManager){
        driveManager.notifyAll();
    }
  }
}

class DownloadThread extends Thread{
  private CloudDrive cloudDrive;
  private ByteArrayOutputStream bos;
  private int fileorder;
  private String filename;
  public DownloadThread(CloudDrive drive,String name,int order){
    cloudDrive = drive;
    filename = name;
    fileorder = order;
  }
  public void run(){
    try{
      System.out.println("Thread"+fileorder+" tries to download file");
      bos = cloudDrive.downloadFile(filename + fileorder);
      
      System.out.println("Thread"+fileorder+" download success");
    }catch(Exception e){
      System.out.println("Thread"+fileorder+ " error while downloading "+e);
    }
  }
  public ByteArrayOutputStream getResult(){
    return bos;
  }
}

