package org.cmapi.mirrorcache.service.processor;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.cmapi.mirrorcache.MirrorCacheException;
import org.cmapi.mirrorcache.MirrorCacheException.Reason;
import org.cmapi.mirrorcache.Priority;
import org.cmapi.mirrorcache.service.ChannelManager;
import org.cmapi.mirrorcache.service.SessionManager;
import org.cmapi.mirrorcache.support.ProtoMessageEntry;
import org.cmapi.primitives.proto.CmapiProto.DeleteChannelCommand;
import org.cmapi.primitives.proto.CmapiProto.OneOfCommand;
import org.cmapi.primitives.proto.CmapiProto.ProtoMessage;
import org.cmapi.primitives.proto.CmapiProto.Status;
import org.slf4j.Logger;

@ApplicationScoped
public class DeleteChannelProcessor implements CommandProcessor {

    @Inject
    private Logger LOG;
    
    @Inject
    private ChannelManager channelManager;
    
    @Inject
    private SessionManager sessionManager;
    
    @Override
    public void process(String sessionId, ProtoMessage req) {
        final DeleteChannelCommand command = req.getCommand().getDeleteChannel();
        
        Status status = Status.SUCCESS;
        try {
            channelManager.deleteChannel(sessionId, command.getChannelName());
            
        } catch (MirrorCacheException e) {
            LOG.error("ERROR: " + e.getMessage(), e);
            status = Status.FAILURE;
        }
        
        final ProtoMessage res = ProtoMessage.newBuilder(req)
                .setPriority(Priority.MEDIUM.getValue())
                .setCommand(OneOfCommand.newBuilder()
                                        .setDeleteChannel(DeleteChannelCommand.newBuilder(command)
                                                                              .setStatus(status)))
                .build();
        
        try {
            if (!sessionManager.getOutboundQueue(sessionId).offer(new ProtoMessageEntry(res), 1, TimeUnit.SECONDS)) {
                throw new RuntimeException(Reason.QUEUE_OFFER_TIMEOUT.getMsg() + ", sessionId: " + sessionId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn(Thread.currentThread().getName() + " thread was interrupted.");
        }
    }
}