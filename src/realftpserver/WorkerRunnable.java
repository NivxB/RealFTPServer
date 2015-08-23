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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mockftpserver.core.MockFtpServerException;
import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.CommandHandler;
import org.mockftpserver.core.command.CommandNames;
import org.mockftpserver.core.command.ConnectCommandHandler;
import org.mockftpserver.core.command.ReplyTextBundleUtil;
import org.mockftpserver.core.command.UnsupportedCommandHandler;
import org.mockftpserver.core.session.DefaultSession;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.core.socket.DefaultServerSocketFactory;
import org.mockftpserver.core.socket.DefaultSocketFactory;
import org.mockftpserver.core.socket.ServerSocketFactory;
import org.mockftpserver.core.socket.SocketFactory;
import org.mockftpserver.core.util.Assert;
import org.mockftpserver.fake.ServerConfiguration;
import org.mockftpserver.fake.ServerConfigurationAware;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.command.AborCommandHandler;
import org.mockftpserver.fake.command.AcctCommandHandler;
import org.mockftpserver.fake.command.AlloCommandHandler;
import org.mockftpserver.fake.command.AppeCommandHandler;
import org.mockftpserver.fake.command.CdupCommandHandler;
import org.mockftpserver.fake.command.CwdCommandHandler;
import org.mockftpserver.fake.command.DeleCommandHandler;
import org.mockftpserver.fake.command.EprtCommandHandler;
import org.mockftpserver.fake.command.EpsvCommandHandler;
import org.mockftpserver.fake.command.HelpCommandHandler;
import org.mockftpserver.fake.command.ListCommandHandler;
import org.mockftpserver.fake.command.MkdCommandHandler;
import org.mockftpserver.fake.command.ModeCommandHandler;
import org.mockftpserver.fake.command.NlstCommandHandler;
import org.mockftpserver.fake.command.NoopCommandHandler;
import org.mockftpserver.fake.command.PassCommandHandler;
import org.mockftpserver.fake.command.PasvCommandHandler;
import org.mockftpserver.fake.command.PortCommandHandler;
import org.mockftpserver.fake.command.PwdCommandHandler;
import org.mockftpserver.fake.command.QuitCommandHandler;
import org.mockftpserver.fake.command.ReinCommandHandler;
import org.mockftpserver.fake.command.RestCommandHandler;
import org.mockftpserver.fake.command.RetrCommandHandler;
import org.mockftpserver.fake.command.RmdCommandHandler;
import org.mockftpserver.fake.command.RnfrCommandHandler;
import org.mockftpserver.fake.command.RntoCommandHandler;
import org.mockftpserver.fake.command.SiteCommandHandler;
import org.mockftpserver.fake.command.SmntCommandHandler;
import org.mockftpserver.fake.command.StatCommandHandler;
import org.mockftpserver.fake.command.StorCommandHandler;
import org.mockftpserver.fake.command.StouCommandHandler;
import org.mockftpserver.fake.command.StruCommandHandler;
import org.mockftpserver.fake.command.SystCommandHandler;
import org.mockftpserver.fake.command.TypeCommandHandler;
import org.mockftpserver.fake.command.UserCommandHandler;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.mockftpserver.fake.filesystem.WindowsFakeFileSystem;
import org.slf4j.LoggerFactory;

public class WorkerRunnable implements Session, ServerConfiguration {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(WorkerRunnable.class);
    protected Socket clientSocket = null;
    protected String serverText = null;
    protected ResourceBundle Resource;
    protected BufferedReader Input = null;
    protected Writer Output = null;
    protected Map commands;
    protected Map users;
    protected Map attributes;
    protected FileSystem filesystem;

    private InetAddress clientHost;
    private int clientPort;

    private InputStream dataInputStream;
    private OutputStream dataOutputStream;
    private Socket dataSocket;
    protected ServerSocketFactory serverSocketFactory = new DefaultServerSocketFactory();
    protected SocketFactory socketFactory = new DefaultSocketFactory();
    ServerSocket passiveModeDataSocket; // non-private for testing
    //  protected BufferedWriter Output = null;
//protected DataOutputStream Output = null;

    public WorkerRunnable(Socket clientSocket, String serverText, ResourceBundle resource) throws IOException {
        this.clientSocket = clientSocket;
        this.serverText = serverText;
        this.commands = new HashMap();
        users = new HashMap();
        attributes = new HashMap();
        filesystem = new WindowsFakeFileSystem();
        
        filesystem.add(new DirectoryEntry("C:\\"));
        filesystem.add(new FileEntry("C:\\Test.txt"));
        Resource = resource;

        initCommands();

        UserAccount AnonUser = new UserAccount("anonymous", "nopass","C:\\");
        
        AnonUser.setPasswordRequiredForLogin(false);
        users.put("anonymous", AnonUser);

        Input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        Output = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            System.out.println("Connectado " + clientSocket.getInetAddress());
            //220            
            CommandHandler connectCommandHandler = (CommandHandler) commands.get(CommandNames.CONNECT);
            connectCommandHandler.handleCommand(new Command(CommandNames.CONNECT, new String[0]), this);

            while (true) {
                readAndProcessCommand();
            }

        } catch (Exception e) {
        }
    }

    @Override
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

    Command parseCommand(String commandString) {
        Assert.notNullOrEmpty(commandString, "commandString");

        List parameters = new ArrayList();
        String name;

        int indexOfFirstSpace = commandString.indexOf(" ");
        if (indexOfFirstSpace != -1) {
            name = commandString.substring(0, indexOfFirstSpace);
            StringTokenizer tokenizer = new StringTokenizer(commandString.substring(indexOfFirstSpace + 1),
                    ",");
            while (tokenizer.hasMoreTokens()) {
                parameters.add(tokenizer.nextToken());
            }
        } else {
            name = commandString;
        }

        String[] parametersArray = new String[parameters.size()];
        return new Command(name, (String[]) parameters.toArray(parametersArray));
    }

    private void readAndProcessCommand() throws Exception {

        Command command = parseCommand(readCommand());
        if (command != null) {
            String normalizedCommandName = Command.normalizeName(command.getName());
            CommandHandler commandHandler = (CommandHandler) commands.get(normalizedCommandName);

            if (commandHandler == null) {
                commandHandler = (CommandHandler) commands.get(CommandNames.UNSUPPORTED);
            }

            Assert.notNull(commandHandler, "CommandHandler for command [" + normalizedCommandName + "]");
            commandHandler.handleCommand(command, this);
        }
    }

    private void initCommands() {
        setCommandHandler(CommandNames.ACCT, new AcctCommandHandler());
        setCommandHandler(CommandNames.ABOR, new AborCommandHandler());
        setCommandHandler(CommandNames.ALLO, new AlloCommandHandler());
        setCommandHandler(CommandNames.APPE, new AppeCommandHandler());
        setCommandHandler(CommandNames.CWD, new CwdCommandHandler());
        setCommandHandler(CommandNames.CDUP, new CdupCommandHandler());
        setCommandHandler(CommandNames.DELE, new DeleCommandHandler());
        setCommandHandler(CommandNames.EPRT, new EprtCommandHandler());
        setCommandHandler(CommandNames.EPSV, new EpsvCommandHandler());
        setCommandHandler(CommandNames.HELP, new HelpCommandHandler());
        setCommandHandler(CommandNames.LIST, new ListCommandHandler());
        setCommandHandler(CommandNames.MKD, new MkdCommandHandler());
        setCommandHandler(CommandNames.MODE, new ModeCommandHandler());
        setCommandHandler(CommandNames.NLST, new NlstCommandHandler());
        setCommandHandler(CommandNames.NOOP, new NoopCommandHandler());
        setCommandHandler(CommandNames.PASS, new PassCommandHandler());
        setCommandHandler(CommandNames.PASV, new PasvCommandHandler());
        setCommandHandler(CommandNames.PWD, new PwdCommandHandler());
        setCommandHandler(CommandNames.PORT, new PortCommandHandler());
        setCommandHandler(CommandNames.QUIT, new QuitCommandHandler());
        setCommandHandler(CommandNames.REIN, new ReinCommandHandler());
        setCommandHandler(CommandNames.REST, new RestCommandHandler());
        setCommandHandler(CommandNames.RETR, new RetrCommandHandler());
        setCommandHandler(CommandNames.RMD, new RmdCommandHandler());
        setCommandHandler(CommandNames.RNFR, new RnfrCommandHandler());
        setCommandHandler(CommandNames.RNTO, new RntoCommandHandler());
        setCommandHandler(CommandNames.SITE, new SiteCommandHandler());
        setCommandHandler(CommandNames.SMNT, new SmntCommandHandler());
        setCommandHandler(CommandNames.STAT, new StatCommandHandler());
        setCommandHandler(CommandNames.STOR, new StorCommandHandler());
        setCommandHandler(CommandNames.STOU, new StouCommandHandler());
        setCommandHandler(CommandNames.STRU, new StruCommandHandler());
        setCommandHandler(CommandNames.SYST, new SystCommandHandler());
        setCommandHandler(CommandNames.TYPE, new TypeCommandHandler());
        setCommandHandler(CommandNames.USER, new UserCommandHandler());
        setCommandHandler(CommandNames.XPWD, new PwdCommandHandler());
        setCommandHandler("XMKD",new MkdCommandHandler());

        // "Special" Command Handlers
        setCommandHandler(CommandNames.CONNECT, new ConnectCommandHandler());
        setCommandHandler(CommandNames.UNSUPPORTED, new UnsupportedCommandHandler());
    }

    private void setCommandHandler(String commandName, CommandHandler commandHandler) {
        Assert.notNull(commandName, "commandName");
        Assert.notNull(commandHandler, "commandHandler");
        commands.put(Command.normalizeName(commandName), commandHandler);
        initializeCommandHandler(commandHandler);
    }

    private void initializeCommandHandler(CommandHandler commandHandler) {
        if (commandHandler instanceof ServerConfigurationAware) {
            ServerConfigurationAware sca = (ServerConfigurationAware) commandHandler;
            sca.setServerConfiguration(this);
        }
        ReplyTextBundleUtil.setReplyTextBundleIfAppropriate(commandHandler, Resource);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isClosed() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void openDataConnection() {
        try {
            if (passiveModeDataSocket != null) {
                LOG.debug("Waiting for (passive mode) client connection from client host [" + clientHost
                        + "] on port " + passiveModeDataSocket.getLocalPort());
                // TODO set socket timeout
                try {
                    dataSocket = passiveModeDataSocket.accept();
                    LOG.debug("Successful (passive mode) client connection to port "
                            + passiveModeDataSocket.getLocalPort());
                } catch (SocketTimeoutException e) {
                    throw new MockFtpServerException(e);
                }
            } else {
                Assert.notNull(clientHost, "clientHost");
                LOG.debug("Connecting to client host [" + clientHost + "] on data port [" + clientPort
                        + "]");
                dataSocket = socketFactory.createSocket(clientHost, clientPort);
            }
            dataOutputStream = dataSocket.getOutputStream();
            dataInputStream = dataSocket.getInputStream();
        } catch (IOException e) {
            throw new MockFtpServerException(e);
        }
    }

    @Override
    public void closeDataConnection() {
        try {
            LOG.debug("Flushing and closing client data socket");
            dataOutputStream.flush();
            dataOutputStream.close();
            dataInputStream.close();
            dataSocket.close();

            if (passiveModeDataSocket != null) {
                passiveModeDataSocket.close();
            }
        } catch (IOException e) {
            LOG.error("Error closing client data socket", e);
        }
    }

    @Override
    public int switchToPassiveMode() {
        try {
            passiveModeDataSocket = serverSocketFactory.createServerSocket(0);
            return passiveModeDataSocket.getLocalPort();
        } catch (IOException e) {
            throw new MockFtpServerException("Error opening passive mode server data socket", e);
        }
    }

    @Override
    public void sendData(byte[] data, int numBytes) {
        Assert.notNull(data, "data");
        try {
            dataOutputStream.write(data, 0, numBytes);
        } catch (IOException e) {
            throw new MockFtpServerException(e);
        }
    }

    @Override
    public byte[] readData() {
        return readData(Integer.MAX_VALUE);
    }

    @Override
    public byte[] readData(int numBytes) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int numBytesRead = 0;
        try {
            while (numBytesRead < numBytes) {
                int b = dataInputStream.read();
                if (b == -1) {
                    break;
                }
                bytes.write(b);
                numBytesRead++;
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new MockFtpServerException(e);
        }
    }

    @Override
    public InetAddress getClientHost() {
        return clientSocket.getInetAddress();
    }

    @Override
    public InetAddress getServerHost() {
        return clientHost;
    }

    @Override
    public void setClientDataHost(InetAddress clientHost) {
        this.clientHost = clientHost;
    }

    @Override
    public void setClientDataPort(int clientDataPort) {
        this.clientPort = clientDataPort;
        if (passiveModeDataSocket != null) {
            try {
                this.passiveModeDataSocket.close();
            } catch (IOException e) {
                throw new MockFtpServerException(e);
            }
            passiveModeDataSocket = null;
        }
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        Assert.notNull(name, "name");
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Set getAttributeNames() {
        return attributes.keySet();
    }

    @Override
    public FileSystem getFileSystem() {
        return filesystem;
    }

    @Override
    public UserAccount getUserAccount(String username) {
        return (UserAccount) users.get(username);
    }

    @Override
    public String getSystemName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSystemStatus() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getHelpText(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
