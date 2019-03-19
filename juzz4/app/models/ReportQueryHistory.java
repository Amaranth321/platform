/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import com.google.code.morphia.annotations.Entity;
import java.util.ArrayList;
import java.util.List;
import play.modules.morphia.Model;

/**
 *
 * @author nischal.regmi
 */
@Entity
public class ReportQueryHistory extends Model {

    public Long userId;
    public String eventType;
    public String dateFrom;
    public String dateTo;
    public List<DeviceSelected> deviceSelected;

    public ReportQueryHistory() {
        userId = 0L;
        eventType = "";
        dateFrom = "";
        dateTo = "";
        deviceSelected = new ArrayList<>();
    }

}
