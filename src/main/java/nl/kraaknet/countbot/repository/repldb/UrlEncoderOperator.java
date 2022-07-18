package nl.kraaknet.countbot.repository.repldb;

import lombok.NonNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;

public class UrlEncoderOperator implements UnaryOperator<String> {

    @Override
    public String apply(@NonNull final String toEncode) {
        return URLEncoder.encode(toEncode, StandardCharsets.UTF_8);
    }
}
