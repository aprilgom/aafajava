package distributeddrive;
import java.io.ByteArrayOutputStream;
public interface CloudDrive{
  public void uploadFile(java.io.File file,String filename);
  public ByteArrayOutputStream downloadFile(String filename);
  public long getSpaceLeft();
}
