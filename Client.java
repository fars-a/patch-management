import java.io.*;
import java.net.*;
import java.util.*;
class Client{
    
    public static void receiveFile(DataInputStream input){
        try{
            DataInputStream dis = input;
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();
            File dir = new File("client_updates");
            if(!dir.exists())
                dir.mkdir();
            FileOutputStream fos = new FileOutputStream("client_updates/" + fileName);
            byte[] buffer = new byte[4096];
            int bytes;
            long remaining = fileSize;
            while((bytes = dis.read(buffer, 0, (int)Math.min(buffer.length, remaining))) > 0){
                fos.write(buffer, 0, bytes);
                remaining -= bytes;
            }
            fos.close();
            System.out.println("[FILE] Received: " + fileName);
        }
        catch(Exception e){
            System.out.println("File receive failed!");
        }
    }

    public static void main(String args[]){
        try{
            Socket socket = new Socket("localhost",1234);
            System.out.println("Connected to server...");
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream());
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter name: ");
            String name = sc.nextLine();
            System.out.println("Enter type: ");
            String type = sc.nextLine();
            System.out.println("Enter version: ");
            String version = sc.nextLine();
            String data = name + "|" + type + "|" + version;
            output.writeUTF(data);
            while(true){
                String serverMessage = input.readUTF();

                if(serverMessage.equals("SEND_FILE")){
                    receiveFile(input);
                }

                else if(serverMessage.startsWith("UPDATE:")){
                    String newVersion = serverMessage.split(":")[1].trim();

                    System.out.println("\nPatch recieved");
                    System.out.println("Old version: "+ version);
                    System.out.println("New version: "+ newVersion);

                    version = newVersion;
                    //send the updated version to the server

                    String updatedData = name + "|" + type + "|" + version;
                    output.writeUTF(updatedData);
                    System.out.println("[UPDATE STATUS] Patch installed successfully");
                    System.out.println("[UPDATE STATUS] Current Version: " + version);
                    output.writeUTF("SUCCESS|" + name + "|" + version);
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}