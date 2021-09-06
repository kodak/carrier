package me.kodak.carrier.message;

import java.util.Map;

public record Message(String id, Map<String, ?> data) {

}
