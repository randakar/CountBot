package nl.kraaknet.countbot.repository;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BackendException extends RuntimeException {

    @Builder
    public BackendException(@NonNull final String message, final Throwable cause) {
        super(message, cause);
    }

    public BackendException(@NonNull final String message) {
        super(message);
    }

}
