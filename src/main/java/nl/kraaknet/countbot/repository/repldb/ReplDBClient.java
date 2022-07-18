package nl.kraaknet.countbot.repository.repldb;

/*

MIT License

Copyright (c) 2021 Abhay

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

*/

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.kraaknet.countbot.repository.BackendException;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * The ReplDBClient class is a single class Java Client for the Database available in a replit.com repl. It offers simplistic usage and is also safe, making sure applications using this do not crash
 *
 * @author Abhay
 * @version 1.0-SNAPSHOT
 * @see <a href="https://replit.com">https://replit.com</a>
 * @since 2021-10-09
 */
@Slf4j
@Builder
public class ReplDBClient {

    public static final String REPLIT_DB_URL = "REPLIT_DB_URL";

    @NonNull
    @Builder.Default
    private String url = System.getenv(REPLIT_DB_URL);

    private final boolean encoded;

    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

    private final UnaryOperator<String> encoder = new UrlEncoderOperator();

    private final UnaryOperator<String> decoder = new UrlDecoderOperator();

    @PostConstruct
    public void init() {
        init(url);
    }

    private void init(final String url) {
        final URI uri = getConnectUri(url);

        if (!uri.getHost().equals("kv.replit.com")) {
            log.error("Invalid host {} for provided database URL. Please provide one with the host \"kv.replit.com\"", uri.getHost());
            throw createInvalidUriException(url);
        }
        final String scheme = uri.getScheme();
        if (!scheme.equals("https")) {
            log.error("Provided URL scheme {} is not https", scheme);
            throw createInvalidUriException(url);
        }
    }

    private static URI getConnectUri(@Nullable final String url) {
        final String uri = url == null ? System.getenv(REPLIT_DB_URL) : url;
        try {
            return new URI(uri);
        } catch (final Exception e) {
            if (e.getMessage() != null)
                log.error(e.getMessage());
            throw createInvalidUriException(url);
        }
    }

    private static BackendException createInvalidUriException(final String url) {
        return new BackendException(format("Invalid URL provided for ReplDBClient: %s", url));
    }

    // Get value by key

    /**
     * Gets the value associated with a key from the database or cache
     *
     * @param key The key which to get the value from
     * @return The value in the database associated with the passed in key
     */
    public String get(final String key) {
        final String actualKey = encode(key);
        final HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(this.url + "/" + actualKey))
                .build(); // Create HTTP Request

        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // Send HTTP request
            log.debug("Received response: {} through GET HTTP request", response);
            return decode(response.body());
        } catch (IOException | InterruptedException e) {
            final String message = format("Error getting %s: %s", key, e);
            throw error(message, e);
        }
    }


    /**
     * Gets the values associated with a keys from the database or cache
     *
     * @param keys They raw keys to get the value from
     * @return The values in the database associated with the passed in keys
     * @see #get(String) get
     */
    public List<String> get(@NonNull final String... keys) {
        return Stream.of(keys)
                .map(this::get)
                .collect(toUnmodifiableList());
    }

    // Set key to value

    /**
     * Sets a key to a value in the database
     *
     * @param key   The key for the pair
     * @param value They value for the pair
     */
    public void set(@NonNull final String key, @NonNull final String value) {
        final String encodedKey = encode(key);
        final String encodedValue = encode(value);

        final HttpRequest request = HttpRequest.newBuilder().POST(getPostData(encodedKey, encodedValue))
                .uri(URI.create(this.url))
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // Send HTTP request
            log.debug(format("Set \"%s\" to \"%s\"", encodedKey, encodedValue));
        } catch (final IOException | InterruptedException e) {
            final String message = format("Error setting %s to %s: %s", encodedKey, encodedValue, e);
            throw error(message, e);
        }
    }

    private static BackendException error(@NonNull String message,
                                          @NonNull final Throwable e) {
        log.error(message);
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new BackendException(message, e);
    }

    /**
     * Sets keys to values in the database
     *
     * @param map map of keys and values to set
     * @see #set(String, String) set
     */
    public void set(@NonNull final Map<String, String> map) {
        log.debug("set({})", map);
        map.forEach(this::set);
    }


    // Delete key/value pair

    /**
     * Deletes a pair with the associated key in the database
     *
     * @param key The key of the pair to delete
     */
    public void delete(@NonNull final String key) {
        final String encodedKey = encode(key);
        final HttpRequest request = HttpRequest.newBuilder().DELETE().uri(URI.create(this.url + "/" + encodedKey)).build();
        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // Send HTTP request
        } catch (final IOException | InterruptedException e) {
            final String message = format("Error deleting: %s", key);
            throw error(message, e);
        }
    }

    /**
     * Deletes the pair with the associated keys in the database
     *
     * @param keys The keys of the pairs to delete
     * @see #delete(String) delete
     */
    public void delete(@NonNull final List<String> keys) {
        keys.forEach(this::delete);
    }

    /**
     * Empties the database (Deletes all keys)
     *
     * @see #delete(String) delete
     */
    public void empty() {
        delete(list());
    }

    // List keys

    /**
     * Lists the keys in the database
     *
     * @return The keys
     * @see #list(String) list
     */
    public List<String> list() {
        return list("");
    }

    /**
     * Lists the keys in the database
     *
     * @param prefix The prefix of the keys to list
     * @return The keys
     * @see #list(String) list
     */
    public List<String> list(final String prefix) {
        final HttpResponse<String> response = fetchList(prefix);
        final String[] tokens = decode(response.body())
                .split("\n");
        final List<String> result = List.of(tokens);
        log.debug("Recieved list of keys {} with prefix \"{}}\" from http request.", result, prefix);
        return result;
    }

    private HttpResponse<String> fetchList(String prefix) {
        final String encodedPrefix = encode(prefix);
        final String reqURL = this.url + "?prefix=" + encodedPrefix;
        final HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(reqURL)).build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (final IOException | InterruptedException e) {
            final String message = format("Error while fetching keys for prefix: %s", prefix);
            throw error(message, e);
        }
    }

    // Utilities

    private HttpRequest.BodyPublisher getPostData(String keyRaw, String valueRaw) { // Convert key/value pair to urlencoded String
        return HttpRequest.BodyPublishers.ofString(encode(keyRaw) + "=" + encode(valueRaw));
    }

    private String encode(final String toEncode) {
        return toEncode == null || toEncode.isBlank() ? "" : encoder.apply(toEncode);
    }

    private String decode(String toDecode) {
        return toDecode == null || toDecode.isBlank() ? "" : decoder.apply(toDecode);
    }

}