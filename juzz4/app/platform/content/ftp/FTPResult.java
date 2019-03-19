package platform.content.ftp;

/**
 * @author Aye Maung
 * @since v4.3
 */
public enum FTPResult
{
    OK("ok"),
    INVALID_PROTOCOL("invalid-ftp-protocol"),
    INVALID_SERVER("invalid-ftp-server"),
    INCORRECT_CREDENTIALS("incorrect-ftp-credentials"),
    LOCAL_FILE_NOT_FOUND("local-file-not-found"),
    REMOTE_FILE_NOT_FOUND("remote-file-not-found"),
    DIR_NOT_FOUND("ftp-remote-dir-not-found"),
    UPLOAD_FAILED("ftp-upload-failed"),
    DOWNLOAD_FAILED("ftp-download-failed"),
    UNKNOWN_ERROR("ftp-unknown-error");

    private final String msgKey;

    FTPResult(String msgKey)
    {
        this.msgKey = msgKey;
    }

    public String getMessage()
    {
        return msgKey;
    }

}
