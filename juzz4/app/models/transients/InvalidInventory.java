/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models.transients;

/**
 *
 * @author nischal.regmi
 */
public class InvalidInventory {
    public static final String REG_COMPULSORY_FIELD = "reg-compulsory-field";
    public static final String MAC_COMPULSORY_FIELD = "mac-compulsory-field";
    public static final String MODEL_COMPULSORY_FIELD = "model-compulsory-field";
    public static final String MAC_INVALID = "invalid-mac-address";
    public static final String USED_DEVICE = "mac-address-already-in-use";
    public static final String REG_INVALID = "reg-already-in-use";
    public static final String MAC_DUPLICATE_VALUE = "mac-duplicate";
    public static final String REG_DUPLICATE_VALUE = "reg-duplicate";
    public static final String MODEL_INVALID = "invalid-model-number";
    //column names
    public static final String REG_COLUMN = "registration-no";
    public static final String MODEL_COLUMN = "model-no";
    public static final String MAC_COLUMN = "mac-address";
    //empty data
    public static final String EMPTY_DATA = "empty";
    
    
    
    public int rowNumber;
    public String columnName;
    public String data;
    public String error;
    

    public InvalidInventory() {
        rowNumber = -1;
        columnName = "";
        data = "";
        error = "";
    }
    
    

}
