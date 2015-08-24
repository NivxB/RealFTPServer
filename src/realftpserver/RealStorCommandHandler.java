/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realftpserver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mockftpserver.core.command.AbstractCommandHandler;
import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.ReplyCodes;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.fake.command.AbstractFakeCommandHandler;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystemException;

/**
 *
 * @author nivx1
 */
public class RealStorCommandHandler extends AbstractFakeCommandHandler {

    public void handle(Command command, Session session) {
        verifyLoggedIn(session);
        this.replyCodeForFileSystemException = ReplyCodes.WRITE_FILE_ERROR;

        String filename = getOutputFile(command);
        String path = getRealPath(session, filename);
        verifyFileSystemCondition(!getFileSystem().isDirectory(path), path, "filesystem.isDirectory");
        String parentPath = getFileSystem().getParent(path);
        verifyFileSystemCondition(getFileSystem().isDirectory(parentPath), parentPath, "filesystem.isNotADirectory");

        // User must have write permission to the file, if an existing file, or else to the directory if a new file
        String pathMustBeWritable = getFileSystem().exists(path) ? path : parentPath;
        verifyWritePermission(session, pathMustBeWritable);

        // User must have execute permission to the parent directory
        verifyExecutePermission(session, parentPath);

        sendReply(session, ReplyCodes.TRANSFER_DATA_INITIAL_OK);

        session.openDataConnection();
        byte[] contents = session.readData();
        session.closeDataConnection();

        FileEntry file = (FileEntry) getFileSystem().getEntry(path);
        if (file == null) {
            file = new FileEntry(path);
            getFileSystem().add(file);
        }
        file.setPermissions(getUserAccount(session).getDefaultPermissionsForNewFile());

        if (contents != null && contents.length > 0) {
            OutputStream out = file.createOutputStream(appendToOutputFile());
            try {
                out.write(contents);
            }
            catch (IOException e) {
                LOG.error("Error writing to file [" + file.getPath() + "]", e);
                throw new FileSystemException(file.getPath(), null, e);
            }
            finally {
                try {
                    out.close();
                } catch (IOException e) {
                    LOG.error("Error closing OutputStream for file [" + file.getPath() + "]", e);
                }
            }
        }
        
        try {
            Files.write(new File(path).toPath(), contents);
        } catch (IOException ex) {
            Logger.getLogger(RealStorCommandHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        sendReply(session, ReplyCodes.TRANSFER_DATA_FINAL_OK, getMessageKey(), list(filename));
    }

    protected String getOutputFile(Command command) {
        return command.getRequiredParameter(0);
    }


    protected boolean appendToOutputFile() {
        return false;
    }

    protected String getMessageKey(){
        return "stor";
    }

    


}
