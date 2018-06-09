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

          

          java.io.File file = new java.io.File("files/testfile2.rar");

          //driveManager.uploadFile(file);
          driveManager.downloadFile("testfile2.rar");
          byte[] filebyte = new byte[(int)file.length()];
          FileInputStream fis = new FileInputStream(file);
          fis.read(filebyte);
          fis.close();
          //googleDrive.uploadFile(filebyte,"name");
          //dropbox.uploadFile(filebyte,"name");
          //oneDrive.uploadFile(filebyte,"testfile.rar");
          //java.io.File downfile = new java.io.File("files/name");
          //FileOutputStream fos = new FileOutputStream(downfile);
          //googleDrive.downloadFile("name").writeTo(fos);
          //dropbox.downloadFile("photo.jpg").writeTo(fos);
          //oneDrive.downloadFile("testfile.rar").writeTo(fos);
          //System.out.println(googleDrive.getSpaceLeft());
          //System.out.println(dropbox.getSpaceLeft());
          //System.out.println(oneDrive.getSpaceLeft());
        }catch(Exception e){

        }
    }

}
