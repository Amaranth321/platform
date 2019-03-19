package platform.content.export;

import com.opencsv.CSVWriter;
import lib.util.Util;
import platform.ReportManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * A wrapper class to enforce consistency.
 * Uses {@link com.opencsv.CSVWriter}
 *
 * @author Aye Maung
 * @since v4.4
 */
public class ReportCsvWriter implements AutoCloseable
{
    private final CSVWriter csvWriter;
    private final File csvFile;
    private int lineCount;

    public ReportCsvWriter() throws IOException
    {
        String fileName = String.format("%s_export.csv", UUID.randomUUID().toString());
        String tmpFolder = ReportManager.getInstance().getTmpDirectory();

        this.csvFile = new File(Util.combine(tmpFolder, fileName));
        this.csvWriter = new CSVWriter(new FileWriter(csvFile), ',');
    }

    public void writeRow(List entryList)
    {
        String[] entries = new String[entryList.size()];
        for (int i = 0; i < entryList.size(); i++)
        {
            entries[i] = String.valueOf(entryList.get(i));
        }
        csvWriter.writeNext(entries);
        lineCount++;
    }

    public File getCsvFile() throws IOException
    {
        return csvFile;
    }

    public int lineCount()
    {
        return lineCount;
    }

    @Override
    public void close()
    {
        try
        {
            csvWriter.close();
        }
        catch (Exception e)
        {
        }
    }
}
