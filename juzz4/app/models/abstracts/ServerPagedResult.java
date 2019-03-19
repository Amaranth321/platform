package models.abstracts;

import java.util.List;

/**
 * @author Aye Maung
 * @since v4.2
 */
public class ServerPagedResult<T> {
    private long totalCount;
    private List<T> resultsForOnePage;

    public ServerPagedResult() {
    }

    public long getTotalCount() {
        return totalCount;
    }

    /**
     * @param totalCount total number of elements in db
     */
    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }


    public List<T> getResultsForOnePage() {
        return resultsForOnePage;
    }

    /**
     * @param resultsForOnePage results for the queried page
     */
    public void setResultsForOnePage(List<T> resultsForOnePage) {
        this.resultsForOnePage = resultsForOnePage;
    }
}
