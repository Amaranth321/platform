package com.kaisquare.kaisync.file;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

class TransportOutputStream extends OutputStream
{
    private volatile IFileTransport mTransport;
    
    public TransportOutputStream(String identifier, String host, int port, long position, String keystore, String keypass, Map<String, String> metadata) throws IOException
    {
        this(identifier, host, port, position, keystore, keypass, metadata, 0);
    }
    
    public TransportOutputStream(String identifier, String host, int port, long position, String keystore, String keypass, Map<String, String> metadata, int timeout) throws IOException
    {
        super();
        
        mTransport = FileServer.createTransport(host, port, keystore, keypass);
        mTransport.setWriteTimeout(timeout);
        mTransport.openFile(identifier, FileOptions.WRITE, position, metadata);
    }
    
    IFileTransport getTransport()
    {
        return mTransport;
    }

    @Override
    public void write(int b) throws IOException
    {
        mTransport.writeFile(ByteBuffer.wrap(new byte[] { (byte) b }), 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        mTransport.writeFile(ByteBuffer.wrap(b, off, len), len);
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        mTransport.close();
    }

}
