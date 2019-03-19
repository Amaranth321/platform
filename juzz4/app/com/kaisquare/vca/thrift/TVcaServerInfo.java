/**
 * Autogenerated by Thrift Compiler (0.8.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.kaisquare.vca.thrift;

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

public class TVcaServerInfo implements org.apache.thrift.TBase<TVcaServerInfo, TVcaServerInfo._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TVcaServerInfo");

  private static final org.apache.thrift.protocol.TField SERVER_START_TIME_FIELD_DESC = new org.apache.thrift.protocol.TField("serverStartTime", org.apache.thrift.protocol.TType.I64, (short)1);
  private static final org.apache.thrift.protocol.TField RELEASE_NUMBER_FIELD_DESC = new org.apache.thrift.protocol.TField("releaseNumber", org.apache.thrift.protocol.TType.DOUBLE, (short)2);
  private static final org.apache.thrift.protocol.TField THREADS_FIELD_DESC = new org.apache.thrift.protocol.TField("threads", org.apache.thrift.protocol.TType.LIST, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TVcaServerInfoStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TVcaServerInfoTupleSchemeFactory());
  }

  private long serverStartTime; // required
  private double releaseNumber; // required
  private List<String> threads; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    SERVER_START_TIME((short)1, "serverStartTime"),
    RELEASE_NUMBER((short)2, "releaseNumber"),
    THREADS((short)3, "threads");

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
        case 1: // SERVER_START_TIME
          return SERVER_START_TIME;
        case 2: // RELEASE_NUMBER
          return RELEASE_NUMBER;
        case 3: // THREADS
          return THREADS;
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
  private static final int __SERVERSTARTTIME_ISSET_ID = 0;
  private static final int __RELEASENUMBER_ISSET_ID = 1;
  private BitSet __isset_bit_vector = new BitSet(2);
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.SERVER_START_TIME, new org.apache.thrift.meta_data.FieldMetaData("serverStartTime", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.RELEASE_NUMBER, new org.apache.thrift.meta_data.FieldMetaData("releaseNumber", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.DOUBLE)));
    tmpMap.put(_Fields.THREADS, new org.apache.thrift.meta_data.FieldMetaData("threads", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TVcaServerInfo.class, metaDataMap);
  }

  public TVcaServerInfo() {
  }

  public TVcaServerInfo(
    long serverStartTime,
    double releaseNumber,
    List<String> threads)
  {
    this();
    this.serverStartTime = serverStartTime;
    setServerStartTimeIsSet(true);
    this.releaseNumber = releaseNumber;
    setReleaseNumberIsSet(true);
    this.threads = threads;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TVcaServerInfo(TVcaServerInfo other) {
    __isset_bit_vector.clear();
    __isset_bit_vector.or(other.__isset_bit_vector);
    this.serverStartTime = other.serverStartTime;
    this.releaseNumber = other.releaseNumber;
    if (other.isSetThreads()) {
      List<String> __this__threads = new ArrayList<String>();
      for (String other_element : other.threads) {
        __this__threads.add(other_element);
      }
      this.threads = __this__threads;
    }
  }

  public TVcaServerInfo deepCopy() {
    return new TVcaServerInfo(this);
  }

  @Override
  public void clear() {
    setServerStartTimeIsSet(false);
    this.serverStartTime = 0;
    setReleaseNumberIsSet(false);
    this.releaseNumber = 0.0;
    this.threads = null;
  }

  public long getServerStartTime() {
    return this.serverStartTime;
  }

  public TVcaServerInfo setServerStartTime(long serverStartTime) {
    this.serverStartTime = serverStartTime;
    setServerStartTimeIsSet(true);
    return this;
  }

  public void unsetServerStartTime() {
    __isset_bit_vector.clear(__SERVERSTARTTIME_ISSET_ID);
  }

  /** Returns true if field serverStartTime is set (has been assigned a value) and false otherwise */
  public boolean isSetServerStartTime() {
    return __isset_bit_vector.get(__SERVERSTARTTIME_ISSET_ID);
  }

  public void setServerStartTimeIsSet(boolean value) {
    __isset_bit_vector.set(__SERVERSTARTTIME_ISSET_ID, value);
  }

  public double getReleaseNumber() {
    return this.releaseNumber;
  }

  public TVcaServerInfo setReleaseNumber(double releaseNumber) {
    this.releaseNumber = releaseNumber;
    setReleaseNumberIsSet(true);
    return this;
  }

  public void unsetReleaseNumber() {
    __isset_bit_vector.clear(__RELEASENUMBER_ISSET_ID);
  }

  /** Returns true if field releaseNumber is set (has been assigned a value) and false otherwise */
  public boolean isSetReleaseNumber() {
    return __isset_bit_vector.get(__RELEASENUMBER_ISSET_ID);
  }

  public void setReleaseNumberIsSet(boolean value) {
    __isset_bit_vector.set(__RELEASENUMBER_ISSET_ID, value);
  }

  public int getThreadsSize() {
    return (this.threads == null) ? 0 : this.threads.size();
  }

  public java.util.Iterator<String> getThreadsIterator() {
    return (this.threads == null) ? null : this.threads.iterator();
  }

  public void addToThreads(String elem) {
    if (this.threads == null) {
      this.threads = new ArrayList<String>();
    }
    this.threads.add(elem);
  }

  public List<String> getThreads() {
    return this.threads;
  }

  public TVcaServerInfo setThreads(List<String> threads) {
    this.threads = threads;
    return this;
  }

  public void unsetThreads() {
    this.threads = null;
  }

  /** Returns true if field threads is set (has been assigned a value) and false otherwise */
  public boolean isSetThreads() {
    return this.threads != null;
  }

  public void setThreadsIsSet(boolean value) {
    if (!value) {
      this.threads = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case SERVER_START_TIME:
      if (value == null) {
        unsetServerStartTime();
      } else {
        setServerStartTime((Long)value);
      }
      break;

    case RELEASE_NUMBER:
      if (value == null) {
        unsetReleaseNumber();
      } else {
        setReleaseNumber((Double)value);
      }
      break;

    case THREADS:
      if (value == null) {
        unsetThreads();
      } else {
        setThreads((List<String>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case SERVER_START_TIME:
      return Long.valueOf(getServerStartTime());

    case RELEASE_NUMBER:
      return Double.valueOf(getReleaseNumber());

    case THREADS:
      return getThreads();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case SERVER_START_TIME:
      return isSetServerStartTime();
    case RELEASE_NUMBER:
      return isSetReleaseNumber();
    case THREADS:
      return isSetThreads();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TVcaServerInfo)
      return this.equals((TVcaServerInfo)that);
    return false;
  }

  public boolean equals(TVcaServerInfo that) {
    if (that == null)
      return false;

    boolean this_present_serverStartTime = true;
    boolean that_present_serverStartTime = true;
    if (this_present_serverStartTime || that_present_serverStartTime) {
      if (!(this_present_serverStartTime && that_present_serverStartTime))
        return false;
      if (this.serverStartTime != that.serverStartTime)
        return false;
    }

    boolean this_present_releaseNumber = true;
    boolean that_present_releaseNumber = true;
    if (this_present_releaseNumber || that_present_releaseNumber) {
      if (!(this_present_releaseNumber && that_present_releaseNumber))
        return false;
      if (this.releaseNumber != that.releaseNumber)
        return false;
    }

    boolean this_present_threads = true && this.isSetThreads();
    boolean that_present_threads = true && that.isSetThreads();
    if (this_present_threads || that_present_threads) {
      if (!(this_present_threads && that_present_threads))
        return false;
      if (!this.threads.equals(that.threads))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(TVcaServerInfo other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    TVcaServerInfo typedOther = (TVcaServerInfo)other;

    lastComparison = Boolean.valueOf(isSetServerStartTime()).compareTo(typedOther.isSetServerStartTime());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetServerStartTime()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.serverStartTime, typedOther.serverStartTime);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetReleaseNumber()).compareTo(typedOther.isSetReleaseNumber());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetReleaseNumber()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.releaseNumber, typedOther.releaseNumber);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetThreads()).compareTo(typedOther.isSetThreads());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetThreads()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.threads, typedOther.threads);
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
    StringBuilder sb = new StringBuilder("TVcaServerInfo(");
    boolean first = true;

    sb.append("serverStartTime:");
    sb.append(this.serverStartTime);
    first = false;
    if (!first) sb.append(", ");
    sb.append("releaseNumber:");
    sb.append(this.releaseNumber);
    first = false;
    if (!first) sb.append(", ");
    sb.append("threads:");
    if (this.threads == null) {
      sb.append("null");
    } else {
      sb.append(this.threads);
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
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bit_vector = new BitSet(1);
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TVcaServerInfoStandardSchemeFactory implements SchemeFactory {
    public TVcaServerInfoStandardScheme getScheme() {
      return new TVcaServerInfoStandardScheme();
    }
  }

  private static class TVcaServerInfoStandardScheme extends StandardScheme<TVcaServerInfo> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TVcaServerInfo struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // SERVER_START_TIME
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.serverStartTime = iprot.readI64();
              struct.setServerStartTimeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // RELEASE_NUMBER
            if (schemeField.type == org.apache.thrift.protocol.TType.DOUBLE) {
              struct.releaseNumber = iprot.readDouble();
              struct.setReleaseNumberIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // THREADS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                struct.threads = new ArrayList<String>(_list0.size);
                for (int _i1 = 0; _i1 < _list0.size; ++_i1)
                {
                  String _elem2; // required
                  _elem2 = iprot.readString();
                  struct.threads.add(_elem2);
                }
                iprot.readListEnd();
              }
              struct.setThreadsIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, TVcaServerInfo struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(SERVER_START_TIME_FIELD_DESC);
      oprot.writeI64(struct.serverStartTime);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(RELEASE_NUMBER_FIELD_DESC);
      oprot.writeDouble(struct.releaseNumber);
      oprot.writeFieldEnd();
      if (struct.threads != null) {
        oprot.writeFieldBegin(THREADS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.threads.size()));
          for (String _iter3 : struct.threads)
          {
            oprot.writeString(_iter3);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TVcaServerInfoTupleSchemeFactory implements SchemeFactory {
    public TVcaServerInfoTupleScheme getScheme() {
      return new TVcaServerInfoTupleScheme();
    }
  }

  private static class TVcaServerInfoTupleScheme extends TupleScheme<TVcaServerInfo> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TVcaServerInfo struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetServerStartTime()) {
        optionals.set(0);
      }
      if (struct.isSetReleaseNumber()) {
        optionals.set(1);
      }
      if (struct.isSetThreads()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetServerStartTime()) {
        oprot.writeI64(struct.serverStartTime);
      }
      if (struct.isSetReleaseNumber()) {
        oprot.writeDouble(struct.releaseNumber);
      }
      if (struct.isSetThreads()) {
        {
          oprot.writeI32(struct.threads.size());
          for (String _iter4 : struct.threads)
          {
            oprot.writeString(_iter4);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TVcaServerInfo struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.serverStartTime = iprot.readI64();
        struct.setServerStartTimeIsSet(true);
      }
      if (incoming.get(1)) {
        struct.releaseNumber = iprot.readDouble();
        struct.setReleaseNumberIsSet(true);
      }
      if (incoming.get(2)) {
        {
          org.apache.thrift.protocol.TList _list5 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.threads = new ArrayList<String>(_list5.size);
          for (int _i6 = 0; _i6 < _list5.size; ++_i6)
          {
            String _elem7; // required
            _elem7 = iprot.readString();
            struct.threads.add(_elem7);
          }
        }
        struct.setThreadsIsSet(true);
      }
    }
  }

}

