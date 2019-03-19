package platform.content.ftp;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftps.FtpsFileSystemConfigBuilder;
import play.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FTPHandler
{
    // region fields

    private FileSystemManager fsManager;
    private FileSystemOptions fsOptions;
    private FTPDetails serverDetails;

    private static List<String> supportedProtocols = new ArrayList<>(Arrays.asList("ftp", "sftp"));
    private static Random myRandom = new Random();

    // endregion

    public FTPHandler(FTPDetails serverDetails)
    {
        this.serverDetails = serverDetails;
    }

    // region public methods

    public FTPResult validate()
    {
        try
        {
            // check supported protocols
            if (!supportedProtocols.contains(serverDetails.getProtocol().trim().toLowerCase()))
            {
                return FTPResult.INVALID_PROTOCOL;
            }

            // check connection
            boolean connectSuccess = connect();
            if (!connectSuccess)
            {
                return FTPResult.INVALID_SERVER;
            }

            // check directory
            FileObject fileObject = fsManager.resolveFile(toUri(serverDetails.getDirectory()), fsOptions);
            if (!fileObject.exists() || !fileObject.isFolder())
            {
                return FTPResult.DIR_NOT_FOUND;
            }

            return FTPResult.OK;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return FTPResult.UNKNOWN_ERROR;
        }
    }

    public boolean connect()
    {
        try
        {
            fsOptions = new FileSystemOptions();
            StaticUserAuthenticator userAuthenticator = new StaticUserAuthenticator("", serverDetails.getUsername(), serverDetails.getPassword());
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(fsOptions, userAuthenticator);

            // protocol-specific configs
            switch (serverDetails.getProtocol().toLowerCase())
            {
                case "ftp":
                    FtpFileSystemConfigBuilder.getInstance().setPassiveMode(fsOptions, true);
                    break;
                case "ftps":
                    FtpsFileSystemConfigBuilder.getInstance().setPassiveMode(fsOptions, true);
                    break;
                case "sftp":
                    break;
            }
            fsManager = VFS.getManager();

            // initialize connection by trying to resolve root
            FileObject rootFileObject = fsManager.resolveFile(toUri(""), fsOptions);
            return rootFileObject.exists();
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public Iterable<FileObject> getRemoteChildren(String remoteRelativePath)
    {
        String resourceUri = toUri(remoteRelativePath);
        return getChildren(resourceUri);
    }

    public boolean download(String remoteRelativePath, String localAbsolutePath)
    {
        String srcResourceUri = toUri(remoteRelativePath);
        String desResourceUri = localAbsolutePath;
        return copy(srcResourceUri, desResourceUri);
    }

    public boolean upload(String localAbsolutePath, String remoteRelativePath)
    {
        String srcResourceUri = localAbsolutePath;
        String desResourceUri = toUri(remoteRelativePath);
        return copy(srcResourceUri, desResourceUri);
    }

    public boolean upload(InputStream localInputStream, String remoteResource)
    {
        try
        {
            String srcResourceUri = "ram://path/to/something/" + myRandom.nextInt();
            String desResourceUri = toUri(remoteResource);

            // save inputstream to ram filesystem
            FileObject srcFileObject = fsManager.resolveFile(srcResourceUri, fsOptions);
            srcFileObject.createFile();
            OutputStream srcOutputStream = srcFileObject.getContent().getOutputStream();
            IOUtils.copy(localInputStream, srcOutputStream);
            srcOutputStream.flush();

            return copy(srcResourceUri, desResourceUri);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
    }

    public boolean moveRemote(String srcResource, String desResource)
    {
        String srcResourceUri = toUri(srcResource);
        String desResourceUri = toUri(desResource);
        return move(srcResourceUri, desResourceUri);
    }

    public boolean deleteRemote(String remoteResource)
    {
        String resourceUri = toUri(remoteResource);
        return delete(resourceUri);
    }

    public boolean createRemoteFolder(String remoteResource)
    {
        String resourceUri = toUri(remoteResource);
        return createFolder(resourceUri);
    }

    // endregion

    // region private methods

    private boolean copy(String srcResourceUri, String desResourceUri)
    {
        FileObject srcFileObject = null;
        FileObject desFileObject = null;

        try
        {
            srcFileObject = fsManager.resolveFile(srcResourceUri, fsOptions);
            desFileObject = fsManager.resolveFile(desResourceUri, fsOptions);
            desFileObject.copyFrom(srcFileObject, Selectors.SELECT_SELF);

            return true;
        } catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        } finally
        {
            try
            {
                if (srcFileObject != null)
                {
                    srcFileObject.close();
                }

                if (desFileObject != null)
                {
                    desFileObject.close();
                }
            } catch (Exception e)
            {
                //
            }
        }
    }

    private boolean move(String srcResourceUri, String desResourceUri)
    {
        FileObject srcFileObject = null;
        FileObject desFileObject = null;

        try
        {
            srcFileObject = fsManager.resolveFile(srcResourceUri, fsOptions);
            desFileObject = fsManager.resolveFile(desResourceUri, fsOptions);

            srcFileObject.moveTo(desFileObject);
            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
        finally
        {
            try
            {
                if (srcFileObject != null)
                {
                    srcFileObject.close();
                }

                if (desFileObject != null)
                {
                    desFileObject.close();
                }
            }
            catch (Exception e)
            {
                //
            }
        }
    }

    private Iterable<FileObject> getChildren(String resourceUri)
    {
        FileObject fileObject = null;

        try
        {
            fileObject = fsManager.resolveFile(resourceUri, fsOptions);
            FileObject[] fileObjects = fileObject.getChildren();
            return Arrays.asList(fileObjects);
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return null;
        }
        finally
        {
            try
            {
                if (fileObject != null)
                {
                    fileObject.close();
                }
            }
            catch (Exception e)
            {
                //
            }
        }
    }

    private boolean createFolder(String resourceUri)
    {
        FileObject fileObject = null;
        try
        {
            fileObject = fsManager.resolveFile(resourceUri, fsOptions);
            if (!fileObject.exists())
            {
                fileObject.createFolder();
            }

            return true;
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            return false;
        }
        finally
        {
            try
            {
                fileObject.close();
            }
            catch (Exception e)
            {
                //
            }
        }
    }

    private boolean delete(String resourceUri)
    {
        FileObject fileObject = null;
        try
        {
            fileObject = fsManager.resolveFile(resourceUri, fsOptions);
            fileObject.delete();

            return true;
        }
        catch (Exception e)
        {
            Logger.error(e, "");
            return false;
        }
        finally
        {
            try
            {
                fileObject.close();
            }
            catch (Exception e)
            {
                //
            }
        }
    }

    private String toUri(String relativeResourcePath)
    {
        return String.format("%s://%s:%s/%s", serverDetails.getProtocol(), serverDetails.getHost(), serverDetails.getPort(), relativeResourcePath);
    }

    // endregion
}