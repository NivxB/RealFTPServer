
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.util.FileUtils;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.WindowsFakeFileSystem;

public class ThreadPooledServer implements Runnable {

    public static final String HOME_DIR = "C:\\FTP\\DATA";
    protected int serverPort = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean isStopped = false;
    protected Thread runningThread = null;
    protected ExecutorService threadPool
            = Executors.newFixedThreadPool(10);
    protected Map users;
    protected WindowsFakeFileSystem filesystem;

    private ResourceBundle resource;

    public ThreadPooledServer(int port, Map user) {
        this.users = new HashMap();
        this.serverPort = port;
        resource = ResourceBundle.getBundle("ReplyText");
        this.users = user;
        filesystem = new WindowsFakeFileSystem();
        addDir(new File(ThreadPooledServer.HOME_DIR));
    }

    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while (!isStopped()) {
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if (isStopped()) {
                    System.out.println("Server Stopped.");
                    break;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }
            try {
                this.threadPool.execute(
                        new WorkerRunnable(clientSocket,
                                "Thread Pooled Server", resource, users,filesystem));
            } catch (IOException ex) {
                Logger.getLogger(ThreadPooledServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.threadPool.shutdown();
        System.out.println("Server Stopped.");
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop() {
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port 8080", e);
        }
    }

    public void setusers(UserAccount usuario) {
        this.users.put(usuario.getUsername(), usuario);
    }

    public void addDir(File dir) {

        filesystem.add(new DirectoryEntry(dir.getAbsolutePath()));
        for (File file : dir.listFiles()) {
            try {
                if (file.isFile()) {
                    filesystem.add(new FileEntry(file.getAbsolutePath(), FileUtils.readFully(new FileReader(file))));
                } else if (file.isDirectory() && null != file) {
                    addDir(file);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        //fakeFtpServer.setFileSystem(fileSystem);
    }
    
    public void addUserDir(String USERNAME){
        DirectoryEntry newEntry = new DirectoryEntry(ThreadPooledServer.HOME_DIR+"\\"+USERNAME);
        newEntry.setOwner(USERNAME);
        filesystem.add(newEntry);
    }
}
