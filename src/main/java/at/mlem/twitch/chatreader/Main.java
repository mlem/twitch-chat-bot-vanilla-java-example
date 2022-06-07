package at.mlem.twitch.chatreader;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.stream.Collectors.joining;

/**
 * to get a token, call https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=f1sjgf9y0ytdx2re1oapwfs7l11lh3&redirect_uri=http://localhost&scope=chat%3Aread+chat%3Aedit
 * <p>
 * After you press "Authorize" you can copy the access_token from the url
 * example of such an url:
 * http://localhost/#access_token=asdfljkasfdafds&scope=chat%3Aread+chat%3Aedit&token_type=bearer
 */
public class Main {

    // args: https://www.twitch.tv/amaz 7yrqz00xs69ift41c5g28q4b57tbqk

    /**
     * @param args <channelname> <oauthToken> <botName>
     */
    public static void main(String[] args) {
        // set to true for more debug information on websocket activity
        //System.setProperty("jdk.internal.httpclient.websocket.debug", "true");

        Args arguments = new Args(args);

        HttpClient client = createClient();

        client.newWebSocketBuilder()
                .buildAsync(
                        URI.create("wss://irc-ws.chat.twitch.tv:443"),
                        new WebSocketListener(arguments)
                ).join();

        while (true) {
            // we keep the program alive to continuously fetch messages from websocket
        }
    }

    private static HttpClient createClient() {
        try {
            SSLContext instance = SSLContext.getInstance("TLSv1.2");
            instance.init(null, null, new SecureRandom());

            return HttpClient.newBuilder()
                    .sslContext(instance)
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(20))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class WebSocketListener implements WebSocket.Listener {
        private final String channelName;
        private final String oauthToken;
        private final String botName;

        public WebSocketListener(Args arguments) {
            this.channelName = arguments.channelName;
            this.botName = arguments.botName;
            this.oauthToken = arguments.oauthToken;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
            System.out.println("WebSocket Client Connected");
            sendText(webSocket, "CAP REQ :twitch.tv/membership twitch.tv/tags twitch.tv/commands");
            sendText(webSocket, "PASS oauth:" + oauthToken);
            sendText(webSocket, "NICK " + botName);
            sendText(webSocket, "JOIN #" + channelName);

        }

        private CompletableFuture<WebSocket> sendText(WebSocket webSocket, String s) {
            CompletableFuture<WebSocket> webSocketCompletableFuture = webSocket.sendText(s, true);
            System.out.println("Sending " + s);
            return webSocketCompletableFuture;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            System.out.println(String.format("Received Binary over WebSocket: %s", data));
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            CompletionStage<?> completionStage = WebSocket.Listener.super.onText(webSocket, data, last);
            System.out.println(String.format("Received Text over WebSocket: %s", data));
            String receivedMessage = data.toString();
            if (receivedMessage.contains("PRIVMSG")) {
                PrivMsg privMsg = new PrivMsg(receivedMessage);
                System.out.println(privMsg.userType);
            } else if (receivedMessage.contains("PING")) {
                int indexOfLastDoppelpunkt = receivedMessage.lastIndexOf(":");
                String responsePong = "PONG " + receivedMessage.substring(indexOfLastDoppelpunkt, receivedMessage.length() - 1);
                webSocket.sendText(responsePong, true);
                System.out.println("answering ping with: " + responsePong);
            }
            return completionStage;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            CompletionStage<?> completionStage = WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
            System.out.println(String.format("Closing WebSocket: %s ; StatusCode: %s", reason, statusCode));
            return completionStage;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            error.printStackTrace();
            WebSocket.Listener.super.onError(webSocket, error);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            System.out.println(String.format("Received Ping over WebSocket: %s", message));
            return WebSocket.Listener.super.onPing(webSocket, message);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            System.out.println(String.format("Received Pong over WebSocket: %s", message));
            return WebSocket.Listener.super.onPong(webSocket, message);
        }

        private class PrivMsg {
            private String userType;
            private String userId;
            private String turbo;
            private String sentTimestamp;
            private String subscriber;
            private String roomId;
            private String mod;
            private String id;
            private String flags;
            private String firstMsg;
            private String emotes;
            private String displayName;
            private String color;
            private String clientNonce;
            private String badges;
            private String badgeInfo;

            public PrivMsg(String receivedMessage) {
                String[] split = receivedMessage.split(";");
                Arrays.stream(split).forEach(part -> {
                    if (part.startsWith("@badge-info")) {
                        badgeInfo = extractValue(part);
                    } else if (part.startsWith("badges")) {
                        badges = extractValue(part);

                    } else if (part.startsWith("client-nonce")) {
                        clientNonce = extractValue(part);

                    } else if (part.startsWith("color")) {
                        color = extractValue(part);

                    } else if (part.startsWith("display-name")) {
                        displayName = extractValue(part);

                    } else if (part.startsWith("emotes")) {
                        emotes = extractValue(part);

                    } else if (part.startsWith("first-msg")) {
                        firstMsg = extractValue(part);

                    } else if (part.startsWith("flags")) {
                        flags = extractValue(part);

                    } else if (part.startsWith("id")) {
                        id = extractValue(part);

                    } else if (part.startsWith("mod")) {
                        mod = extractValue(part);

                    } else if (part.startsWith("room-id")) {
                        roomId = extractValue(part);

                    } else if (part.startsWith("subscriber")) {
                        subscriber = extractValue(part);

                    } else if (part.startsWith("tmi-sent-ts")) {
                        sentTimestamp = extractValue(part);

                    } else if (part.startsWith("turbo")) {
                        turbo = extractValue(part);

                    } else if (part.startsWith("user-id")) {
                        userId = extractValue(part);

                    } else if (part.startsWith("user-type")) {
                        userType = extractValue(part);

                    }
                });
            }

            private String extractValue(String part) {
                String[] split = part.split("=");
                if (split.length > 1) {
                    return split[1];
                } else {
                    return null;
                }
            }
        }
    }

    private static class Args {

        private String botName;
        private String channelName;
        private String oauthToken;

        public Args(String[] args) {
            System.out.println(
                    String.format(
                            "Command line args passed: %s",
                            Arrays.stream(args).collect(joining(" ; "))
                    )
            );
            if (args.length >= 3) {
                String twitchChannelUrl = args[0];
                String[] split = twitchChannelUrl.split("/");
                channelName = split[split.length - 1];
                oauthToken = args[1];
                botName = args[2];
            }
        }
    }
}
