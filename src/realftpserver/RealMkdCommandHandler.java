/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package realftpserver;

import java.io.File;
import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.ReplyCodes;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.fake.command.AbstractFakeCommandHandler;
import org.mockftpserver.fake.filesystem.DirectoryEntry;

/**
 *
 * @author nivx1
 */
public class RealMkdCommandHandler extends AbstractFakeCommandHandler {

    @Override
    protected void handle(Command command, Session session) {
        verifyLoggedIn(session);
        String path = getRealPath(session, command.getRequiredParameter(0));
        String parent = getFileSystem().getParent(path);

        this.replyCodeForFileSystemException = ReplyCodes.READ_FILE_ERROR;
        verifyFileSystemCondition(getFileSystem().exists(parent), parent, "filesystem.doesNotExist");
        verifyFileSystemCondition(!getFileSystem().exists(path), path, "filesystem.alreadyExists");

        // User must have write permission to the parent directory
        verifyWritePermission(session, parent);

        // User must have execute permission to the parent directory
        verifyExecutePermission(session, parent);

        DirectoryEntry dirEntry = new DirectoryEntry(path);
        File NewFile = new File(path);
        NewFile.mkdir();
        getFileSystem().add(dirEntry);
        dirEntry.setPermissions(getUserAccount(session).getDefaultPermissionsForNewDirectory());

        sendReply(session, ReplyCodes.MKD_OK, "mkd", list(path));
    }

}
