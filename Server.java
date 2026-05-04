import java.io.*;
import java.net.*;
import java.util.*;
class ClientHandler extends Thread{
    Socket socket;
    String clientName = "";

    public ClientHandler(Socket socket){
        this.socket = socket;
    }
    public void run(){
        try{
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            while(true){
                String message = input.readUTF();

                if(message.startsWith("SUCCESS|")){
                    String[] successParts = message.split("\\|");
                    String cname = successParts[1];
                    String updatedVersion = successParts[2];
                    System.out.println(cname + " updated successfully to " + updatedVersion);
                    Server.writeLog("Update success: " + cname + " -> " + updatedVersion);
                    continue;
                }

                String[] parts = message.split("\\|");

                if(parts.length != 3){
                    System.out.println("Invalid data format recieved!");
                    continue;
                }
                String name = parts[0];
                String type = parts[1];
                String version = parts[2];
                
                System.out.println("[FIREWALL CHECK] Checking client: " + name);
                //validating components
                if(!(type.equalsIgnoreCase("PC") ||
                    type.equalsIgnoreCase("Router") ||
                    type.equalsIgnoreCase("Switch") ||
                    type.equalsIgnoreCase("Firewall"))) {
                        System.out.println("[FIREWALL CHECK] BLOCKED → " + name + " (" + type + ")");
                        Server.writeLog("Blocked client: "+ name + " | Invalid type: " + type);
                        continue;
                }
                clientName = name;
                if (Server.clientMap.containsKey(name)) {
                    System.out.println("Updating existing client: " + name);
                } else {
                    System.out.println("[FIREWALL CHECK] ALLOWED → " + name + " (" + type + ")");
                }
                Server.clientMap.put(name,
                        new ClientInfo(name, type, version, socket, output));

                displayClients();

            }
        }
        catch(Exception e){
            if (!clientName.equals("")) {
                Server.clientMap.remove(clientName);
                System.out.println(clientName + " disconnected and removed.");
                displayClients();
            } else {
                System.out.println("Client disconnected.");
            }
        }
    }
    public void displayClients() {
        System.out.println("\n--- Current Clients ---");
        for (ClientInfo c : Server.clientMap.values()) {
            System.out.println(c.name + " | " + c.type + " | " + c.version);
        }
        System.out.println("------------------------\n");
    }
}
class ClientInfo {
    String name;
    String type;
    String version;
    Socket socket;
    DataOutputStream output;

    public ClientInfo(String name, String type, String version, Socket socket, DataOutputStream output) {
        this.name = name;
        this.type = type;
        this.version = version;
        this.socket = socket;
        this.output = output;
    }
}

public class Server{
    static Map<String, ClientInfo> clientMap = new HashMap<>();
    public static void sendPatchByType(String targetType,String newVersion){
        for(ClientInfo client : clientMap.values()){
            try{
                if(targetType.equalsIgnoreCase("ALL")){
                    client.output.writeUTF("UPDATE: "+newVersion);
                    System.out.println("[ROUTING] Patch routed to: " + client.name + " (" + client.type + ")");
                    writeLog("Patch sent to " + client.name + " |New version: " + newVersion);
                }
                else if(client.type.equalsIgnoreCase(targetType)){
                    client.output.writeUTF("UPDATE: "+newVersion);
                    System.out.println("Patch sent to "+ client.name);
                }
            }
            catch(Exception e){
                System.out.println("Failed to send patch to: " + client.name);
            }
        }
    }
    public static void writeLog(String message){
        try{
            FileWriter fw = new FileWriter("log.txt", true);
            fw.write(message + "\n");
            fw.close();
        }
        catch(Exception e){
            System.out.println("Log writing failed!");
        }
    }
    public static void main(String args[]){
        try{
            ServerSocket serverSocket = new ServerSocket(1234);
            Scanner sc = new Scanner(System.in);
            System.out.println("Server started...Waiting for clients...");
            new Thread(()->{
                while(true) {
                    try{
                        Socket socket = serverSocket.accept();
                        System.out.println("[NETWORK] New client connected: " + socket);
                        writeLog("Client connected: "+socket);
                        ClientHandler client = new ClientHandler(socket);
                        client.start();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();
            while(true){
                System.out.println("===PATCH DEPLOYMENT MENU===");
                System.out.println("1.Update all clients");
                System.out.println("2.Update only PC");
                System.out.println("3.Update only Router");
                System.out.println("4.Update only Switch");
                System.out.println("5.Update only Firewall");
                System.out.println("Enter choice: ");
                int ch= sc.nextInt();
                sc.nextLine();
                System.out.print("Enter new patch version (example v2): ");
                String patchVersion = sc.nextLine();

                switch(ch){
                    case 1:
                        sendPatchByType("ALL", patchVersion);
                        break; 
                    case 2:
                        sendPatchByType("PC", patchVersion);
                        break;
                    case 3:
                        sendPatchByType("Router", patchVersion);
                        break;
                    case 4:
                        sendPatchByType("Switch", patchVersion);
                        break;
                    case 5:
                        sendPatchByType("Firewall", patchVersion);
                        break;
                    default:
                        System.out.println("Invalid choice");
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}