package me.kodak.carrier.message.listener;

import java.lang.reflect.Method;

public record MessageListenerData(Object instance, Method method, String id) {

}
