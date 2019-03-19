package models;

import play.modules.morphia.Model;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Index;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.Indexes;

@Entity
@Indexes({
	@Index("bucket, name"),
	@Index("bucket, name, parserType")
})
public class PosDataReport extends Model {
	
	@Indexed
	public String bucket;
	public String name;
	public String parserType;
	public SalesRecord sales;

    public PosDataReport() {
        bucket = "";
        name = "";
        parserType = "";
        sales = new SalesRecord();
    }
        
        
}
