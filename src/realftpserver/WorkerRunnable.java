/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realftpserver;

/**
 *
 * @author nivx1
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkerRunnable implements Runnable{

    protected Socket clientSocket = null;
    protected String serverText = null;
    protected ResourceBundle Resource;
    protected BufferedReader Input = null;
    protected Writer Output = null;
    protected Map commands;
    //  protected BufferedWriter Output = null;
//protected DataOutputStream Output = null;

    public WorkerRunnable(Socket clientSocket, String serverText, ResourceBundle resource) throws IOException {
        this.clientSocket = clientSocket;
        this.serverText = serverText;
        this.commands = new HashMap();
        Resource = resource;

        Input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        Output = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    
    public void run() {
        try {
            System.out.println("Connectado " + clientSocket.getInetAddress());
            sendReply(220,"Coneccion Exitosa");
            System.out.println(readCommand());
            /*
             Input = new BufferedReader(new InputStreamReader(A));
            
             //Output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

             //DeflaterOutputStream a = new DeflaterOutputStream(clientSocket.getOutputStream());
             Writer a = new PrintWriter(clientSocket.getOutputStream(),true);
             String Line = TextCode(220,"Service ready for new user.");
             a.write(Line+"\r\n ");
             a.flush();
            
             while (true){
             //System.out.println(Input.ready());
             if (Input.ready()){
             System.out.println(Input.readLine());
             break;
             }
             }
            
             // DataOutputStream Out = new DataOutputStream(clientSocket.getOutputStream());
             //DataInputStream In = new DataInputStream(clientSocket.getInputStream());
             long time = System.currentTimeMillis();
             System.out.println("Request processed: " + time);
             */
        } catch (Exception e) {
            //report exception somewhere.
            e.printStackTrace();
        }
    }



   
    public void sendReply(int Code, String DataCode) {
        StringBuilder buffer = new StringBuilder(Integer.toString(Code));
        if ((DataCode != null) && (DataCode.length() > 0)) {
            String replyText = DataCode.trim();
            if (replyText.contains("\n")) {
                int lastIndex = replyText.lastIndexOf("\n");
                buffer.append("-");
                for (int i = 0; i < replyText.length(); i++) {
                    char c = replyText.charAt(i);
                    buffer.append(c);
                    if (i == lastIndex) {
                        buffer.append(Integer.toString(Code));
                        buffer.append(" ");
                    }
                }
            } else {
                buffer.append(" ");
                buffer.append(replyText);
            }
        }

        try {
            Output.write(buffer.toString() + "\r\n");
            Output.flush();
        } catch (IOException ex) {
            Logger.getLogger(WorkerRunnable.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    String readCommand() {
        final long socketReadIntervalMilliseconds = 20L;

        try {
            while (true) {
                // Don't block; only read command when it is available
                if (Input.ready()) {
                    String command = Input.readLine();
                    // LOG.info("Received command: [" + command + "]");
                    if (command == null) {
                        return null;
                    }
                    return command;
                }
                try {
                    Thread.sleep(socketReadIntervalMilliseconds);
                } catch (InterruptedException e) {
                }
            }
        } catch (IOException e) {
        }
        return null;
    }
}
