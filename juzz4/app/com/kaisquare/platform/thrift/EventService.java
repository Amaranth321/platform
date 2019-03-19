/**
 * Autogenerated by Thrift Compiler (0.8.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.kaisquare.platform.thrift;

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

public class EventService {

  /**
   * EventService - this service provides API for event sources to push events to Platform.
   */
  public interface Iface {

    /**
     * Push an event to the Platform.
     * 
     * (1) eventId - ID of the event
     * (2) details - a structure with more details about this event
     * 
     * 
     * @param eventId
     * @param details
     */
    public boolean pushEvent(String eventId, com.kaisquare.events.thrift.EventDetails details) throws PlatformException, org.apache.thrift.TException;

  }

  public interface AsyncIface {

    public void pushEvent(String eventId, com.kaisquare.events.thrift.EventDetails details, org.apache.thrift.async.AsyncMethodCallback<AsyncClient.pushEvent_call> resultHandler) throws org.apache.thrift.TException;

  }

  public static class Client extends org.apache.thrift.TServiceClient implements Iface {
    public static class Factory implements org.apache.thrift.TServiceClientFactory<Client> {
      public Factory() {}
      public Client getClient(org.apache.thrift.protocol.TProtocol prot) {
        return new Client(prot);
      }
      public Client getClient(org.apache.thrift.protocol.TProtocol iprot, org.apache.thrift.protocol.TProtocol oprot) {
        return new Client(iprot, oprot);
      }
    }

    public Client(org.apache.thrift.protocol.TProtocol prot)
    {
      super(prot, prot);
    }

    public Client(org.apache.thrift.protocol.TProtocol iprot, org.apache.thrift.protocol.TProtocol oprot) {
      super(iprot, oprot);
    }

    public boolean pushEvent(String eventId, com.kaisquare.events.thrift.EventDetails details) throws PlatformException, org.apache.thrift.TException
    {
      send_pushEvent(eventId, details);
      return recv_pushEvent();
    }

    public void send_pushEvent(String eventId, com.kaisquare.events.thrift.EventDetails details) throws org.apache.thrift.TException
    {
      pushEvent_args args = new pushEvent_args();
      args.setEventId(eventId);
      args.setDetails(details);
      sendBase("pushEvent", args);
    }

    public boolean recv_pushEvent() throws PlatformException, org.apache.thrift.TException
    {
      pushEvent_result result = new pushEvent_result();
      receiveBase(result, "pushEvent");
      if (result.isSetSuccess()) {
        return result.success;
      }
      if (result.platformExp != null) {
        throw result.platformExp;
      }
      throw new org.apache.thrift.TApplicationException(org.apache.thrift.TApplicationException.MISSING_RESULT, "pushEvent failed: unknown result");
    }

  }
  public static class AsyncClient extends org.apache.thrift.async.TAsyncClient implements AsyncIface {
    public static class Factory implements org.apache.thrift.async.TAsyncClientFactory<AsyncClient> {
      private org.apache.thrift.async.TAsyncClientManager clientManager;
      private org.apache.thrift.protocol.TProtocolFactory protocolFactory;
      public Factory(org.apache.thrift.async.TAsyncClientManager clientManager, org.apache.thrift.protocol.TProtocolFactory protocolFactory) {
        this.clientManager = clientManager;
        this.protocolFactory = protocolFactory;
      }
      public AsyncClient getAsyncClient(org.apache.thrift.transport.TNonblockingTransport transport) {
        return new AsyncClient(protocolFactory, clientManager, transport);
      }
    }

    public AsyncClient(org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.async.TAsyncClientManager clientManager, org.apache.thrift.transport.TNonblockingTransport transport) {
      super(protocolFactory, clientManager, transport);
    }

    public void pushEvent(String eventId, com.kaisquare.events.thrift.EventDetails details, org.apache.thrift.async.AsyncMethodCallback<pushEvent_call> resultHandler) throws org.apache.thrift.TException {
      checkReady();
      pushEvent_call method_call = new pushEvent_call(eventId, details, resultHandler, this, ___protocolFactory, ___transport);
      this.___currentMethod = method_call;
      ___manager.call(method_call);
    }

    public static class pushEvent_call extends org.apache.thrift.async.TAsyncMethodCall {
      private String eventId;
      private com.kaisquare.events.thrift.EventDetails details;
      public pushEvent_call(String eventId, com.kaisquare.events.thrift.EventDetails details, org.apache.thrift.async.AsyncMethodCallback<pushEvent_call> resultHandler, org.apache.thrift.async.TAsyncClient client, org.apache.thrift.protocol.TProtocolFactory protocolFactory, org.apache.thrift.transport.TNonblockingTransport transport) throws org.apache.thrift.TException {
        super(client, protocolFactory, transport, resultHandler, false);
        this.eventId = eventId;
        this.details = details;
      }

      public void write_args(org.apache.thrift.protocol.TProtocol prot) throws org.apache.thrift.TException {
        prot.writeMessageBegin(new org.apache.thrift.protocol.TMessage("pushEvent", org.apache.thrift.protocol.TMessageType.CALL, 0));
        pushEvent_args args = new pushEvent_args();
        args.setEventId(eventId);
        args.setDetails(details);
        args.write(prot);
        prot.writeMessageEnd();
      }

      public boolean getResult() throws PlatformException, org.apache.thrift.TException {
        if (getState() != org.apache.thrift.async.TAsyncMethodCall.State.RESPONSE_READ) {
          throw new IllegalStateException("Method call not finished!");
        }
        org.apache.thrift.transport.TMemoryInputTransport memoryTransport = new org.apache.thrift.transport.TMemoryInputTransport(getFrameBuffer().array());
        org.apache.thrift.protocol.TProtocol prot = client.getProtocolFactory().getProtocol(memoryTransport);
        return (new Client(prot)).recv_pushEvent();
      }
    }

  }

  public static class Processor<I extends Iface> extends org.apache.thrift.TBaseProcessor<I> implements org.apache.thrift.TProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Processor.class.getName());
    public Processor(I iface) {
      super(iface, getProcessMap(new HashMap<String, org.apache.thrift.ProcessFunction<I, ? extends org.apache.thrift.TBase>>()));
    }

    protected Processor(I iface, Map<String,  org.apache.thrift.ProcessFunction<I, ? extends  org.apache.thrift.TBase>> processMap) {
      super(iface, getProcessMap(processMap));
    }

    private static <I extends Iface> Map<String,  org.apache.thrift.ProcessFunction<I, ? extends  org.apache.thrift.TBase>> getProcessMap(Map<String,  org.apache.thrift.ProcessFunction<I, ? extends  org.apache.thrift.TBase>> processMap) {
      processMap.put("pushEvent", new pushEvent());
      return processMap;
    }

    private static class pushEvent<I extends Iface> extends org.apache.thrift.ProcessFunction<I, pushEvent_args> {
      public pushEvent() {
        super("pushEvent");
      }

      protected pushEvent_args getEmptyArgsInstance() {
        return new pushEvent_args();
      }

      protected pushEvent_result getResult(I iface, pushEvent_args args) throws org.apache.thrift.TException {
        pushEvent_result result = new pushEvent_result();
        try {
          result.success = iface.pushEvent(args.eventId, args.details);
          result.setSuccessIsSet(true);
        } catch (PlatformException platformExp) {
          result.platformExp = platformExp;
        }
        return result;
      }
    }

  }

  public static class pushEvent_args implements org.apache.thrift.TBase<pushEvent_args, pushEvent_args._Fields>, java.io.Serializable, Cloneable   {
    private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("pushEvent_args");

    private static final org.apache.thrift.protocol.TField EVENT_ID_FIELD_DESC = new org.apache.thrift.protocol.TField("eventId", org.apache.thrift.protocol.TType.STRING, (short)1);
    private static final org.apache.thrift.protocol.TField DETAILS_FIELD_DESC = new org.apache.thrift.protocol.TField("details", org.apache.thrift.protocol.TType.STRUCT, (short)2);

    private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
    static {
      schemes.put(StandardScheme.class, new pushEvent_argsStandardSchemeFactory());
      schemes.put(TupleScheme.class, new pushEvent_argsTupleSchemeFactory());
    }

    private String eventId; // required
    private com.kaisquare.events.thrift.EventDetails details; // required

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements org.apache.thrift.TFieldIdEnum {
      EVENT_ID((short)1, "eventId"),
      DETAILS((short)2, "details");

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
          case 1: // EVENT_ID
            return EVENT_ID;
          case 2: // DETAILS
            return DETAILS;
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
      tmpMap.put(_Fields.EVENT_ID, new org.apache.thrift.meta_data.FieldMetaData("eventId", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
      tmpMap.put(_Fields.DETAILS, new org.apache.thrift.meta_data.FieldMetaData("details", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, com.kaisquare.events.thrift.EventDetails.class)));
      metaDataMap = Collections.unmodifiableMap(tmpMap);
      org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(pushEvent_args.class, metaDataMap);
    }

    public pushEvent_args() {
    }

    public pushEvent_args(
      String eventId,
      com.kaisquare.events.thrift.EventDetails details)
    {
      this();
      this.eventId = eventId;
      this.details = details;
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public pushEvent_args(pushEvent_args other) {
      if (other.isSetEventId()) {
        this.eventId = other.eventId;
      }
      if (other.isSetDetails()) {
        this.details = new com.kaisquare.events.thrift.EventDetails(other.details);
      }
    }

    public pushEvent_args deepCopy() {
      return new pushEvent_args(this);
    }

    @Override
    public void clear() {
      this.eventId = null;
      this.details = null;
    }

    public String getEventId() {
      return this.eventId;
    }

    public pushEvent_args setEventId(String eventId) {
      this.eventId = eventId;
      return this;
    }

    public void unsetEventId() {
      this.eventId = null;
    }

    /** Returns true if field eventId is set (has been assigned a value) and false otherwise */
    public boolean isSetEventId() {
      return this.eventId != null;
    }

    public void setEventIdIsSet(boolean value) {
      if (!value) {
        this.eventId = null;
      }
    }

    public com.kaisquare.events.thrift.EventDetails getDetails() {
      return this.details;
    }

    public pushEvent_args setDetails(com.kaisquare.events.thrift.EventDetails details) {
      this.details = details;
      return this;
    }

    public void unsetDetails() {
      this.details = null;
    }

    /** Returns true if field details is set (has been assigned a value) and false otherwise */
    public boolean isSetDetails() {
      return this.details != null;
    }

    public void setDetailsIsSet(boolean value) {
      if (!value) {
        this.details = null;
      }
    }

    public void setFieldValue(_Fields field, Object value) {
      switch (field) {
      case EVENT_ID:
        if (value == null) {
          unsetEventId();
        } else {
          setEventId((String)value);
        }
        break;

      case DETAILS:
        if (value == null) {
          unsetDetails();
        } else {
          setDetails((com.kaisquare.events.thrift.EventDetails)value);
        }
        break;

      }
    }

    public Object getFieldValue(_Fields field) {
      switch (field) {
      case EVENT_ID:
        return getEventId();

      case DETAILS:
        return getDetails();

      }
      throw new IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new IllegalArgumentException();
      }

      switch (field) {
      case EVENT_ID:
        return isSetEventId();
      case DETAILS:
        return isSetDetails();
      }
      throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object that) {
      if (that == null)
        return false;
      if (that instanceof pushEvent_args)
        return this.equals((pushEvent_args)that);
      return false;
    }

    public boolean equals(pushEvent_args that) {
      if (that == null)
        return false;

      boolean this_present_eventId = true && this.isSetEventId();
      boolean that_present_eventId = true && that.isSetEventId();
      if (this_present_eventId || that_present_eventId) {
        if (!(this_present_eventId && that_present_eventId))
          return false;
        if (!this.eventId.equals(that.eventId))
          return false;
      }

      boolean this_present_details = true && this.isSetDetails();
      boolean that_present_details = true && that.isSetDetails();
      if (this_present_details || that_present_details) {
        if (!(this_present_details && that_present_details))
          return false;
        if (!this.details.equals(that.details))
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    public int compareTo(pushEvent_args other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;
      pushEvent_args typedOther = (pushEvent_args)other;

      lastComparison = Boolean.valueOf(isSetEventId()).compareTo(typedOther.isSetEventId());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetEventId()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.eventId, typedOther.eventId);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = Boolean.valueOf(isSetDetails()).compareTo(typedOther.isSetDetails());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetDetails()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.details, typedOther.details);
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
      StringBuilder sb = new StringBuilder("pushEvent_args(");
      boolean first = true;

      sb.append("eventId:");
      if (this.eventId == null) {
        sb.append("null");
      } else {
        sb.append(this.eventId);
      }
      first = false;
      if (!first) sb.append(", ");
      sb.append("details:");
      if (this.details == null) {
        sb.append("null");
      } else {
        sb.append(this.details);
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

    private static class pushEvent_argsStandardSchemeFactory implements SchemeFactory {
      public pushEvent_argsStandardScheme getScheme() {
        return new pushEvent_argsStandardScheme();
      }
    }

    private static class pushEvent_argsStandardScheme extends StandardScheme<pushEvent_args> {

      public void read(org.apache.thrift.protocol.TProtocol iprot, pushEvent_args struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();
        while (true)
        {
          schemeField = iprot.readFieldBegin();
          if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
            break;
          }
          switch (schemeField.id) {
            case 1: // EVENT_ID
              if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
                struct.eventId = iprot.readString();
                struct.setEventIdIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 2: // DETAILS
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.details = new com.kaisquare.events.thrift.EventDetails();
                struct.details.read(iprot);
                struct.setDetailsIsSet(true);
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

      public void write(org.apache.thrift.protocol.TProtocol oprot, pushEvent_args struct) throws org.apache.thrift.TException {
        struct.validate();

        oprot.writeStructBegin(STRUCT_DESC);
        if (struct.eventId != null) {
          oprot.writeFieldBegin(EVENT_ID_FIELD_DESC);
          oprot.writeString(struct.eventId);
          oprot.writeFieldEnd();
        }
        if (struct.details != null) {
          oprot.writeFieldBegin(DETAILS_FIELD_DESC);
          struct.details.write(oprot);
          oprot.writeFieldEnd();
        }
        oprot.writeFieldStop();
        oprot.writeStructEnd();
      }

    }

    private static class pushEvent_argsTupleSchemeFactory implements SchemeFactory {
      public pushEvent_argsTupleScheme getScheme() {
        return new pushEvent_argsTupleScheme();
      }
    }

    private static class pushEvent_argsTupleScheme extends TupleScheme<pushEvent_args> {

      @Override
      public void write(org.apache.thrift.protocol.TProtocol prot, pushEvent_args struct) throws org.apache.thrift.TException {
        TTupleProtocol oprot = (TTupleProtocol) prot;
        BitSet optionals = new BitSet();
        if (struct.isSetEventId()) {
          optionals.set(0);
        }
        if (struct.isSetDetails()) {
          optionals.set(1);
        }
        oprot.writeBitSet(optionals, 2);
        if (struct.isSetEventId()) {
          oprot.writeString(struct.eventId);
        }
        if (struct.isSetDetails()) {
          struct.details.write(oprot);
        }
      }

      @Override
      public void read(org.apache.thrift.protocol.TProtocol prot, pushEvent_args struct) throws org.apache.thrift.TException {
        TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(2);
        if (incoming.get(0)) {
          struct.eventId = iprot.readString();
          struct.setEventIdIsSet(true);
        }
        if (incoming.get(1)) {
          struct.details = new com.kaisquare.events.thrift.EventDetails();
          struct.details.read(iprot);
          struct.setDetailsIsSet(true);
        }
      }
    }

  }

  public static class pushEvent_result implements org.apache.thrift.TBase<pushEvent_result, pushEvent_result._Fields>, java.io.Serializable, Cloneable   {
    private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("pushEvent_result");

    private static final org.apache.thrift.protocol.TField SUCCESS_FIELD_DESC = new org.apache.thrift.protocol.TField("success", org.apache.thrift.protocol.TType.BOOL, (short)0);
    private static final org.apache.thrift.protocol.TField PLATFORM_EXP_FIELD_DESC = new org.apache.thrift.protocol.TField("platformExp", org.apache.thrift.protocol.TType.STRUCT, (short)1);

    private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
    static {
      schemes.put(StandardScheme.class, new pushEvent_resultStandardSchemeFactory());
      schemes.put(TupleScheme.class, new pushEvent_resultTupleSchemeFactory());
    }

    private boolean success; // required
    private PlatformException platformExp; // required

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements org.apache.thrift.TFieldIdEnum {
      SUCCESS((short)0, "success"),
      PLATFORM_EXP((short)1, "platformExp");

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
          case 0: // SUCCESS
            return SUCCESS;
          case 1: // PLATFORM_EXP
            return PLATFORM_EXP;
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
    private static final int __SUCCESS_ISSET_ID = 0;
    private BitSet __isset_bit_vector = new BitSet(1);
    public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
    static {
      Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
      tmpMap.put(_Fields.SUCCESS, new org.apache.thrift.meta_data.FieldMetaData("success", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
      tmpMap.put(_Fields.PLATFORM_EXP, new org.apache.thrift.meta_data.FieldMetaData("platformExp", org.apache.thrift.TFieldRequirementType.DEFAULT, 
          new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT)));
      metaDataMap = Collections.unmodifiableMap(tmpMap);
      org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(pushEvent_result.class, metaDataMap);
    }

    public pushEvent_result() {
    }

    public pushEvent_result(
      boolean success,
      PlatformException platformExp)
    {
      this();
      this.success = success;
      setSuccessIsSet(true);
      this.platformExp = platformExp;
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public pushEvent_result(pushEvent_result other) {
      __isset_bit_vector.clear();
      __isset_bit_vector.or(other.__isset_bit_vector);
      this.success = other.success;
      if (other.isSetPlatformExp()) {
        this.platformExp = new PlatformException(other.platformExp);
      }
    }

    public pushEvent_result deepCopy() {
      return new pushEvent_result(this);
    }

    @Override
    public void clear() {
      setSuccessIsSet(false);
      this.success = false;
      this.platformExp = null;
    }

    public boolean isSuccess() {
      return this.success;
    }

    public pushEvent_result setSuccess(boolean success) {
      this.success = success;
      setSuccessIsSet(true);
      return this;
    }

    public void unsetSuccess() {
      __isset_bit_vector.clear(__SUCCESS_ISSET_ID);
    }

    /** Returns true if field success is set (has been assigned a value) and false otherwise */
    public boolean isSetSuccess() {
      return __isset_bit_vector.get(__SUCCESS_ISSET_ID);
    }

    public void setSuccessIsSet(boolean value) {
      __isset_bit_vector.set(__SUCCESS_ISSET_ID, value);
    }

    public PlatformException getPlatformExp() {
      return this.platformExp;
    }

    public pushEvent_result setPlatformExp(PlatformException platformExp) {
      this.platformExp = platformExp;
      return this;
    }

    public void unsetPlatformExp() {
      this.platformExp = null;
    }

    /** Returns true if field platformExp is set (has been assigned a value) and false otherwise */
    public boolean isSetPlatformExp() {
      return this.platformExp != null;
    }

    public void setPlatformExpIsSet(boolean value) {
      if (!value) {
        this.platformExp = null;
      }
    }

    public void setFieldValue(_Fields field, Object value) {
      switch (field) {
      case SUCCESS:
        if (value == null) {
          unsetSuccess();
        } else {
          setSuccess((Boolean)value);
        }
        break;

      case PLATFORM_EXP:
        if (value == null) {
          unsetPlatformExp();
        } else {
          setPlatformExp((PlatformException)value);
        }
        break;

      }
    }

    public Object getFieldValue(_Fields field) {
      switch (field) {
      case SUCCESS:
        return Boolean.valueOf(isSuccess());

      case PLATFORM_EXP:
        return getPlatformExp();

      }
      throw new IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new IllegalArgumentException();
      }

      switch (field) {
      case SUCCESS:
        return isSetSuccess();
      case PLATFORM_EXP:
        return isSetPlatformExp();
      }
      throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object that) {
      if (that == null)
        return false;
      if (that instanceof pushEvent_result)
        return this.equals((pushEvent_result)that);
      return false;
    }

    public boolean equals(pushEvent_result that) {
      if (that == null)
        return false;

      boolean this_present_success = true;
      boolean that_present_success = true;
      if (this_present_success || that_present_success) {
        if (!(this_present_success && that_present_success))
          return false;
        if (this.success != that.success)
          return false;
      }

      boolean this_present_platformExp = true && this.isSetPlatformExp();
      boolean that_present_platformExp = true && that.isSetPlatformExp();
      if (this_present_platformExp || that_present_platformExp) {
        if (!(this_present_platformExp && that_present_platformExp))
          return false;
        if (!this.platformExp.equals(that.platformExp))
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    public int compareTo(pushEvent_result other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;
      pushEvent_result typedOther = (pushEvent_result)other;

      lastComparison = Boolean.valueOf(isSetSuccess()).compareTo(typedOther.isSetSuccess());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetSuccess()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.success, typedOther.success);
        if (lastComparison != 0) {
          return lastComparison;
        }
      }
      lastComparison = Boolean.valueOf(isSetPlatformExp()).compareTo(typedOther.isSetPlatformExp());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetPlatformExp()) {
        lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.platformExp, typedOther.platformExp);
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
      StringBuilder sb = new StringBuilder("pushEvent_result(");
      boolean first = true;

      sb.append("success:");
      sb.append(this.success);
      first = false;
      if (!first) sb.append(", ");
      sb.append("platformExp:");
      if (this.platformExp == null) {
        sb.append("null");
      } else {
        sb.append(this.platformExp);
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

    private static class pushEvent_resultStandardSchemeFactory implements SchemeFactory {
      public pushEvent_resultStandardScheme getScheme() {
        return new pushEvent_resultStandardScheme();
      }
    }

    private static class pushEvent_resultStandardScheme extends StandardScheme<pushEvent_result> {

      public void read(org.apache.thrift.protocol.TProtocol iprot, pushEvent_result struct) throws org.apache.thrift.TException {
        org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();
        while (true)
        {
          schemeField = iprot.readFieldBegin();
          if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
            break;
          }
          switch (schemeField.id) {
            case 0: // SUCCESS
              if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
                struct.success = iprot.readBool();
                struct.setSuccessIsSet(true);
              } else { 
                org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
              }
              break;
            case 1: // PLATFORM_EXP
              if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
                struct.platformExp = new PlatformException();
                struct.platformExp.read(iprot);
                struct.setPlatformExpIsSet(true);
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

      public void write(org.apache.thrift.protocol.TProtocol oprot, pushEvent_result struct) throws org.apache.thrift.TException {
        struct.validate();

        oprot.writeStructBegin(STRUCT_DESC);
        oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
        oprot.writeBool(struct.success);
        oprot.writeFieldEnd();
        if (struct.platformExp != null) {
          oprot.writeFieldBegin(PLATFORM_EXP_FIELD_DESC);
          struct.platformExp.write(oprot);
          oprot.writeFieldEnd();
        }
        oprot.writeFieldStop();
        oprot.writeStructEnd();
      }

    }

    private static class pushEvent_resultTupleSchemeFactory implements SchemeFactory {
      public pushEvent_resultTupleScheme getScheme() {
        return new pushEvent_resultTupleScheme();
      }
    }

    private static class pushEvent_resultTupleScheme extends TupleScheme<pushEvent_result> {

      @Override
      public void write(org.apache.thrift.protocol.TProtocol prot, pushEvent_result struct) throws org.apache.thrift.TException {
        TTupleProtocol oprot = (TTupleProtocol) prot;
        BitSet optionals = new BitSet();
        if (struct.isSetSuccess()) {
          optionals.set(0);
        }
        if (struct.isSetPlatformExp()) {
          optionals.set(1);
        }
        oprot.writeBitSet(optionals, 2);
        if (struct.isSetSuccess()) {
          oprot.writeBool(struct.success);
        }
        if (struct.isSetPlatformExp()) {
          struct.platformExp.write(oprot);
        }
      }

      @Override
      public void read(org.apache.thrift.protocol.TProtocol prot, pushEvent_result struct) throws org.apache.thrift.TException {
        TTupleProtocol iprot = (TTupleProtocol) prot;
        BitSet incoming = iprot.readBitSet(2);
        if (incoming.get(0)) {
          struct.success = iprot.readBool();
          struct.setSuccessIsSet(true);
        }
        if (incoming.get(1)) {
          struct.platformExp = new PlatformException();
          struct.platformExp.read(iprot);
          struct.setPlatformExpIsSet(true);
        }
      }
    }

  }

}
