package ru.mail.jira.plugins.mrimsender.protocol.packages;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import ru.mail.jira.plugins.mrimsender.protocol.CommandProcessor;

import java.io.IOException;
import java.net.Socket;

public class Worker {
    private static final Logger log = Logger.getLogger(Worker.class);

    private static final String ADDRESS_RESOLVER_HOST = "mrim.mail.ru";
    private static final int ADDRESS_RESOLVER_PORT = 2042;

    private final Socket socket;
    private final int timeout;

    private int messageSeq = 1;
    private byte[] headerBytes, dataBytes;
    private int headerBytesLength, dataBytesLength;

    public Worker(String host, Integer port) throws Exception {
        if (host == null || port == null) {
            Socket addressResolverSocket = new Socket(ADDRESS_RESOLVER_HOST, ADDRESS_RESOLVER_PORT);
            try {
                String s = IOUtils.toString(addressResolverSocket.getInputStream(), Consts.CP1251_MRIM_ENCODING);

                s = s.trim();
                int i = s.lastIndexOf(":");
                if (i < 0)
                    throw new Exception(String.format("Requested an invalid host:port pair <%s> from %s:%d", s, ADDRESS_RESOLVER_HOST, ADDRESS_RESOLVER_PORT));

                host = s.substring(0, i);
                port = Integer.parseInt(s.substring(i + 1));
            } finally {
                addressResolverSocket.close();
            }
        }

        socket = new Socket(host, port);
        try {
            writeBytes(PackageFactory.getMrimCsHello());

            MrimCsHeader answer = readNextPackage(true);
            if (answer instanceof MrimCsHelloAck)
                timeout = ((MrimCsHelloAck) answer).getTimeout();
            else
                throw new IOException(String.format("Unexpected message type %d", answer.getMessageType()));
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    public void close() throws IOException {
        socket.close();
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getTimeout() {
        return timeout;
    }

    public boolean login(String login, String password) throws IOException {
        if (log.isDebugEnabled())
            log.debug("[SEND] Trying to login. Login = " + login);
        writeBytes(PackageFactory.getMrimCsLogin2(messageSeq++, login, password));

        MrimCsHeader answer = readNextPackage(true);
        switch (answer.getMessageType()) {
            case Consts.MRIM_CS_LOGIN_ACK:
                return true;
            case Consts.MRIM_CS_LOGIN_REJ:
                return false;
            default:
                throw new IOException(String.format("Unexpected message type %d", answer.getMessageType()));
        }
    }

    public void sendMessage(String email, String message) throws IOException {
        if (log.isDebugEnabled())
            log.debug(String.format("[SEND] To = %s Message = %s", email, message));
        writeBytes(PackageFactory.getMrimCsMessage(messageSeq++, 0, email, message));
    }

    public void ping() throws IOException {
        writeBytes(PackageFactory.getMrimCsPing(messageSeq++));
    }

    public void processAvailablePackages() throws IOException {
        MrimCsHeader mrimCsHeader;
        while ((mrimCsHeader = readNextPackage(false)) != null) {
            if (mrimCsHeader instanceof MrimCsMessageAck) {
                MrimCsMessageAck mrimCsMessageAck = (MrimCsMessageAck) mrimCsHeader;

                // Indicate that we received the message
                if (!mrimCsMessageAck.checkFlag(Consts.MESSAGE_FLAG_NORECV)) {
                    if (log.isDebugEnabled())
                        log.debug("[SEND] Indication that we received the message");
                    writeBytes(PackageFactory.getMrimCsMessageRecv(messageSeq++, mrimCsMessageAck.getFromEmail(), mrimCsMessageAck.getMsgId()));
                }

                // User requested authorization from mrimsender account
                if (mrimCsMessageAck.checkFlag(Consts.MESSAGE_FLAG_AUTHORIZE)) {
                    if (log.isDebugEnabled())
                        log.debug("[SEND] User requested authorization from mrimsender account");
                    writeBytes(PackageFactory.getMrimCsAuthorize(messageSeq++, mrimCsMessageAck.getFromEmail()));
                }

                // Process incoming message
                if (!mrimCsMessageAck.checkFlag(Consts.MESSAGE_FLAG_OFFLINE) &&
                        !mrimCsMessageAck.checkFlag(Consts.MESSAGE_FLAG_AUTHORIZE) &&
                        !mrimCsMessageAck.checkFlag(Consts.MESSAGE_FLAG_NOTIFY)) {
                    if (log.isDebugEnabled())
                        log.debug(String.format("[RECEIVE] From = %s Message = %s", mrimCsMessageAck.getFromEmail(), mrimCsMessageAck.getMessage()));
                    CommandProcessor.processMessage(mrimCsMessageAck.getFromEmail(), mrimCsMessageAck.getMessage());
                }
            }

            // Remove offline messages from the server
            if (mrimCsHeader instanceof MrimCsOfflineMessageAck) {
                MrimCsOfflineMessageAck mrimCsOfflineMessageAck = (MrimCsOfflineMessageAck) mrimCsHeader;
                if (log.isDebugEnabled())
                    log.debug("[SEND] Remove offline messages from server command");
                writeBytes(PackageFactory.getMrimCsDeleteOfflineMessage(messageSeq++, mrimCsOfflineMessageAck.getUidl()));
            }
        }
    }

    private MrimCsHeader readNextPackage(boolean blockingMode) throws IOException {
        // Read header bytes
        if (headerBytes == null)
            headerBytes = new byte[MrimCsHeader.SIZE];
        while (headerBytesLength < MrimCsHeader.SIZE)
            if (blockingMode || socket.getInputStream().available() > 0)
                headerBytesLength += socket.getInputStream().read(headerBytes, headerBytesLength, MrimCsHeader.SIZE - headerBytesLength);
            else
                return null;

        // Process header
        MrimCsHeader header = new MrimCsHeader(headerBytes);

        // Read data bytes
        if (dataBytes == null || (dataBytes.length != header.getDataLength() && header.getDataLength() > 0))
            dataBytes = new byte[header.getDataLength()];
        while (dataBytesLength < header.getDataLength())
            if (blockingMode || socket.getInputStream().available() > 0)
                dataBytesLength += socket.getInputStream().read(dataBytes, dataBytesLength, header.getDataLength() - dataBytesLength);
            else
                return null;

        // Process special message types
        switch (header.getMessageType()) {
            case Consts.MRIM_CS_HELLO_ACK:
                header = new MrimCsHelloAck(headerBytes, dataBytes);
                break;
            case Consts.MRIM_CS_MESSAGE_ACK:
                header = new MrimCsMessageAck(headerBytes, dataBytes);
                break;
            case Consts.MRIM_CS_OFFLINE_MESSAGE_ACK:
                header = new MrimCsOfflineMessageAck(headerBytes, dataBytes);
                break;
        }

        // Return the result
        headerBytesLength = dataBytesLength = 0;
        return header;
    }

    private void writeBytes(byte[] buff) throws IOException {
        socket.getOutputStream().write(buff);
        socket.getOutputStream().flush();
    }
}
