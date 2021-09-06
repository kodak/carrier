package gg.hops.carrier;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gg.hops.carrier.message.Message;
import gg.hops.carrier.message.handler.IncomingMessageHandler;
import gg.hops.carrier.message.handler.MessageExceptionHandler;
import gg.hops.carrier.message.listener.MessageListener;
import gg.hops.carrier.message.listener.MessageListenerData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

public class Carrier {

    private final String channel;
    private final JedisPool jedisPool;
    private final Gson gson;

    private JedisPubSub jedisPubSub;
    private final Map<String, List<MessageListenerData>> listeners = new HashMap<>();

    public Carrier(String channel, JedisPool jedisPool, Gson gson) {
        this.channel = channel;
        this.jedisPool = jedisPool;
        this.gson = gson;

        this.setupPubSub();
    }

    public void sendMessage(Message message) {
        sendMessage(message, new MessageExceptionHandler());
    }

    public void sendMessage(Message message, MessageExceptionHandler exceptionHandler) {
        try (Jedis client = jedisPool.getResource()) {
            client.publish(channel, message.id() + ";" + gson.toJsonTree(message.data()).toString());
        } catch (Exception e) {
            exceptionHandler.onException(e);
        }
    }

    public void registerListener(MessageListener messageListener) {
        for (Method method : messageListener.getClass().getDeclaredMethods()) {
            if (method.getDeclaredAnnotation(IncomingMessageHandler.class) != null && method.getParameters().length != 0) {
                if (!JsonObject.class.isAssignableFrom(method.getParameters()[0].getType())) {
                    throw new IllegalStateException("First parameter should be of type JsonObject.");
                }
                String messageId = method.getDeclaredAnnotation(IncomingMessageHandler.class).value();
                listeners.putIfAbsent(messageId, new ArrayList<>());
                listeners.get(messageId).add(new MessageListenerData(messageListener, method, messageId));
            }
        }
    }

    public void close() {
        if (jedisPubSub != null && jedisPubSub.isSubscribed()) {
            jedisPubSub.unsubscribe();
        }

        jedisPool.close();
    }

    public void setupPubSub() {
        this.jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (channel.equalsIgnoreCase(Carrier.this.channel)) {
                    try {
                        int breakAt = message.indexOf(';');
                        String messageId = message.substring(0, breakAt);

                        if (Carrier.this.listeners.containsKey(messageId)) {
                            JsonObject messageData = gson.fromJson(message.substring(breakAt + 1), JsonObject.class);

                            for (MessageListenerData listener : Carrier.this.listeners.get(messageId)) {
                                listener.method().invoke(listener.instance(), messageData);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        ForkJoinPool.commonPool().execute(() -> {
            try (Jedis client = jedisPool.getResource()) {
                client.subscribe(jedisPubSub, channel);
            }
        });
    }

}
