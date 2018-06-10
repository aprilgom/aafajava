package distributeddrive;


import java.security.GeneralSecurityException;

import java.io.IOException;
import java.io.*;

public class DriveQuickstart {


    public static void main(String... args) {
        // Build a new authorized API client service.
        try{
          String filename;
          DriveManager driveManager = new DriveManager();
          driveManager.addDrive("GoogleDrive",new GoogleDrive());
          driveManager.addDrive("Dropbox",new Dropbox());
          driveManager.addDrive("OneDrive",new OneDrive());

          
          switch(args[0]){
            case "download":
            filename = args[1];
            driveManager.downloadFile(filename);
            break;
            case "upload":
            filename = args[1];
            driveManager.uploadFile(filename);
            break;
            case "delete":
            filename = args[1];
            driveManager.deleteFile(filename);
            break;
            case "list":
            driveManager.listFile();
            break;
          }
          

          //driveManager.uploadFile(file);
          //driveManager.downloadFile("tetra.rar");
          //driveManager.listFile();
          //driveManager.deleteFile("tetra.rar");


        }catch(Exception e){

        }
    }

}
