package platform.content.export;

import platform.content.FileFormat;

import java.io.InputStream;

/**
 * ALL reports must use this interface for file exporting.
 * DO NOT MODIFY this interface
 *
 * @author Aye Maung
 * @since v4.3
 */
public interface ReportBuilder
{

    /**
     * @return just the file name with extension. Do not use the full-path filename. Special characters should be removed.
     */
    String getFilename();

    /**
     * @return file format of the binary returned by <code>generate()</code>
     */
    FileFormat getFileFormat();

    /**
     * @return input stream of the generated file
     */
    InputStream generate();
}
