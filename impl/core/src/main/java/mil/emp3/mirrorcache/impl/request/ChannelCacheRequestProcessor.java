package mil.emp3.mirrorcache.impl.request;

import java.util.HashSet;

import org.cmapi.primitives.proto.CmapiProto.ChannelCacheOperation;
import org.cmapi.primitives.proto.CmapiProto.OneOfOperation;
import org.cmapi.primitives.proto.CmapiProto.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.emp3.mirrorcache.Message;
import mil.emp3.mirrorcache.MessageDispatcher;
import mil.emp3.mirrorcache.MirrorCacheException;
import mil.emp3.mirrorcache.MirrorCacheException.Reason;
import mil.emp3.mirrorcache.Priority;
import mil.emp3.mirrorcache.channel.ChannelCache;
import mil.emp3.mirrorcache.impl.channel.ClientChannelCache;

public class ChannelCacheRequestProcessor extends BaseRequestProcessor<Message, ChannelCache> {
    static final private Logger LOG = LoggerFactory.getLogger(ChannelCacheRequestProcessor.class);
    
    final private MessageDispatcher dispatcher;
    
    public ChannelCacheRequestProcessor(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
    
    @Override
    public ChannelCache executeSync(Message reqMessage) throws MirrorCacheException {
        if (reqMessage == null) {
            throw new IllegalStateException("reqMessage == null");
        }

        dispatcher.dispatchMessage(reqMessage.setPriority(Priority.MEDIUM));
        
        try {
            final Message resMessage = dispatcher.awaitResponse(reqMessage);
            
            final ChannelCacheOperation operation = resMessage.getOperation().as(OneOfOperation.class).getChannelCache();
            if (operation.getStatus() == Status.SUCCESS) {
                
                final ChannelCache cache = new ClientChannelCache(operation.getChannelName(), new HashSet<>(operation.getEntityIdList()));
                return cache;
                
            } else {
                throw new MirrorCacheException(Reason.CHANNEL_CACHE_FAILURE).withDetail("channelName: " + operation.getChannelName());
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.info(Thread.currentThread().getName() + " thread interrupted.");
        }
        
        throw new IllegalStateException(new MirrorCacheException(Reason.UNKNOWN));
    }
}
