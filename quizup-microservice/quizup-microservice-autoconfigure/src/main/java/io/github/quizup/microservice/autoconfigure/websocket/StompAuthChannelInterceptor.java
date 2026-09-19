package io.github.quizup.microservice.autoconfigure.websocket;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

/**
 * Authentifie la trame STOMP {@code CONNECT} en décodant le JWT porté par le header natif
 * {@code Authorization: Bearer <token>}, et pose le {@code Principal} de la session
 * (nom = claim {@code user_id}, fallback {@code sub}).
 *
 * <p>Mode permissif par défaut ({@code microservice.websocket.require-auth=false}) : une trame
 * sans token valide reste anonyme et n'est pas rejetée, ce qui préserve les flux temps réel
 * existants. En mode strict, toute trame {@code CONNECT} sans JWT valide est rejetée.</p>
 */
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;
    private final boolean requireAuth;

    public StompAuthChannelInterceptor(ObjectProvider<JwtDecoder> jwtDecoderProvider, boolean requireAuth) {
        this.jwtDecoderProvider = jwtDecoderProvider;
        this.requireAuth = requireAuth;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = resolveToken(accessor);
        JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();

        if (token == null || jwtDecoder == null) {
            return rejectIfRequired(message, "Missing STOMP CONNECT token");
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            accessor.setUser(new JwtAuthenticationToken(jwt, List.of(), principalName(jwt)));
        } catch (JwtException e) {
            return rejectIfRequired(message, "Invalid STOMP CONNECT token");
        }

        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private String principalName(Jwt jwt) {
        String userId = jwt.getClaimAsString("user_id");
        return userId != null && !userId.isBlank() ? userId : jwt.getSubject();
    }

    private Message<?> rejectIfRequired(Message<?> message, String reason) {
        if (requireAuth) {
            throw new MessagingException(message, reason);
        }
        return message;
    }
}
