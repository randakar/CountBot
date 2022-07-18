package nl.kraaknet.countbot.repository.repldb;

import lombok.NonNull;
import nl.kraaknet.countbot.repository.BackendException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;

public class UrlDecoderOperator implements UnaryOperator<String> {

    @Override
    public String apply(@NonNull final String toDecode) {
        try {
            // equivalent to js's decodeURIComponent
            return URLDecoder.decode(toDecode, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            throw new BackendException("Invalid encodimg", e);
        }
    }
}
