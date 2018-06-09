package distributeddrive;


import java.security.GeneralSecurityException;

import java.io.IOException;
import java.io.*;

public class DriveQuickstart {


    public static void main(String... args) {
        // Build a new authorized API client service.
        try{

          DriveManager driveManager = new DriveManager();
          driveManager.addDrive("GoogleDrive",new GoogleDrive());
          driveManager.addDrive("Dropbox",new Dropbox());
          driveManager.addDrive("OneDrive",new OneDrive());

          java.io.File file = new java.io.File("files/tetra.rar");

          //driveManager.uploadFile(file);
          //driveManager.downloadFile("tetra.rar");
          //driveManager.listFile();
          driveManager.deleteFile("tetra.rar");
          byte[] filebyte = new byte[(int)file.length()];
          FileInputStream fis = new FileInputStream(file);
          fis.read(filebyte);
          fis.close();

        }catch(Exception e){

        }
    }

}
