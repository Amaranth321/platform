package models;

import java.util.Date;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Indexed;

@Embedded
public class SalesRecord {

    @Indexed
    public Date time;
    public int count;
    public double amount;

    public SalesRecord() {
        time = new Date();
        count = 0;
        amount = 0.0;
    }

}
