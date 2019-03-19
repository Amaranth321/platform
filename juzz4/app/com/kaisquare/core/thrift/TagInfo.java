/**
 * Autogenerated by Thrift Compiler (0.8.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.kaisquare.core.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tag information
 * (1) tagId - ID of the tag
 * (2) macAddress - MAC address of the tag in the common notational format, e.g. 01:23:45:67:89:ab
 */
public class TagInfo implements org.apache.thrift.TBase<TagInfo, TagInfo._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TagInfo");

  private static final org.apache.thrift.protocol.TField TAG_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("tagId", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField MAC_ADDRESS_FIELD_DESC = new org.apache.thrift.protocol.TField("macAddress", org.apache.thrift.protocol.TType.STRING, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TagInfoStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TagInfoTupleSchemeFactory());
  }

  private String tagId; // required
  private String macAddress; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    TAG_ID((short)1, "tagId"),
    MAC_ADDRESS((short)2, "macAddress");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // TAG_ID
          return TAG_ID;
        case 2: // MAC_ADDRESS
          return MAC_ADDRESS;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.TAG_ID, new org.apache.thrift.meta_data.FieldMetaData("tagId", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.MAC_ADDRESS, new org.apache.thrift.meta_data.FieldMetaData("macAddress", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TagInfo.class, metaDataMap);
  }

  public TagInfo() {
  }

  public TagInfo(
    String tagId,
    String macAddress)
  {
    this();
    this.tagId = tagId;
    this.macAddress = macAddress;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TagInfo(TagInfo other) {
    if (other.isSetTagId()) {
      this.tagId = other.tagId;
    }
    if (other.isSetMacAddress()) {
      this.macAddress = other.macAddress;
    }
  }

  public TagInfo deepCopy() {
    return new TagInfo(this);
  }

  @Override
  public void clear() {
    this.tagId = null;
    this.macAddress = null;
  }

  public String getTagId() {
    return this.tagId;
  }

  public TagInfo setTagId(String tagId) {
    this.tagId = tagId;
    return this;
  }

  public void unsetTagId() {
    this.tagId = null;
  }

  /** Returns true if field tagId is set (has been assigned a value) and false otherwise */
  public boolean isSetTagId() {
    return this.tagId != null;
  }

  public void setTagIdIsSet(boolean value) {
    if (!value) {
      this.tagId = null;
    }
  }

  public String getMacAddress() {
    return this.macAddress;
  }

  public TagInfo setMacAddress(String macAddress) {
    this.macAddress = macAddress;
    return this;
  }

  public void unsetMacAddress() {
    this.macAddress = null;
  }

  /** Returns true if field macAddress is set (has been assigned a value) and false otherwise */
  public boolean isSetMacAddress() {
    return this.macAddress != null;
  }

  public void setMacAddressIsSet(boolean value) {
    if (!value) {
      this.macAddress = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case TAG_ID:
      if (value == null) {
        unsetTagId();
      } else {
        setTagId((String)value);
      }
      break;

    case MAC_ADDRESS:
      if (value == null) {
        unsetMacAddress();
      } else {
        setMacAddress((String)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case TAG_ID:
      return getTagId();

    case MAC_ADDRESS:
      return getMacAddress();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case TAG_ID:
      return isSetTagId();
    case MAC_ADDRESS:
      return isSetMacAddress();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TagInfo)
      return this.equals((TagInfo)that);
    return false;
  }

  public boolean equals(TagInfo that) {
    if (that == null)
      return false;

    boolean this_present_tagId = true && this.isSetTagId();
    boolean that_present_tagId = true && that.isSetTagId();
    if (this_present_tagId || that_present_tagId) {
      if (!(this_present_tagId && that_present_tagId))
        return false;
      if (!this.tagId.equals(that.tagId))
        return false;
    }

    boolean this_present_macAddress = true && this.isSetMacAddress();
    boolean that_present_macAddress = true && that.isSetMacAddress();
    if (this_present_macAddress || that_present_macAddress) {
      if (!(this_present_macAddress && that_present_macAddress))
        return false;
      if (!this.macAddress.equals(that.macAddress))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(TagInfo other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    TagInfo typedOther = (TagInfo)other;

    lastComparison = Boolean.valueOf(isSetTagId()).compareTo(typedOther.isSetTagId());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetTagId()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.tagId, typedOther.tagId);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMacAddress()).compareTo(typedOther.isSetMacAddress());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMacAddress()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.macAddress, typedOther.macAddress);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TagInfo(");
    boolean first = true;

    sb.append("tagId:");
    if (this.tagId == null) {
      sb.append("null");
    } else {
      sb.append(this.tagId);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("macAddress:");
    if (this.macAddress == null) {
      sb.append("null");
    } else {
      sb.append(this.macAddress);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TagInfoStandardSchemeFactory implements SchemeFactory {
    public TagInfoStandardScheme getScheme() {
      return new TagInfoStandardScheme();
    }
  }

  private static class TagInfoStandardScheme extends StandardScheme<TagInfo> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TagInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // TAG_ID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.tagId = iprot.readString();
              struct.setTagIdIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // MAC_ADDRESS
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.macAddress = iprot.readString();
              struct.setMacAddressIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, TagInfo struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.tagId != null) {
        oprot.writeFieldBegin(TAG_ID_FIELD_DESC);
        oprot.writeString(struct.tagId);
        oprot.writeFieldEnd();
      }
      if (struct.macAddress != null) {
        oprot.writeFieldBegin(MAC_ADDRESS_FIELD_DESC);
        oprot.writeString(struct.macAddress);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TagInfoTupleSchemeFactory implements SchemeFactory {
    public TagInfoTupleScheme getScheme() {
      return new TagInfoTupleScheme();
    }
  }

  private static class TagInfoTupleScheme extends TupleScheme<TagInfo> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TagInfo struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetTagId()) {
        optionals.set(0);
      }
      if (struct.isSetMacAddress()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetTagId()) {
        oprot.writeString(struct.tagId);
      }
      if (struct.isSetMacAddress()) {
        oprot.writeString(struct.macAddress);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TagInfo struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.tagId = iprot.readString();
        struct.setTagIdIsSet(true);
      }
      if (incoming.get(1)) {
        struct.macAddress = iprot.readString();
        struct.setMacAddressIsSet(true);
      }
    }
  }

}

