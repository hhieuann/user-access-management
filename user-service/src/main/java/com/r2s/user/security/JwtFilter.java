package com.r2s.user.security;

import com.r2s.core.response.ApiResponseWriter;
import com.r2s.core.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired token";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final ApiResponseWriter apiResponseWriter;   // ← dùng chung helper (DRY)

    public JwtFilter(JwtUtil jwtUtil, UserDetailsService uds, ApiResponseWriter apiResponseWriter) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = uds;
        this.apiResponseWriter = apiResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
            } catch (JwtException | IllegalArgumentException e) {
                // Token lỗi/hết hạn → trả 401 ApiResponse format, không leak chi tiết
                apiResponseWriter.writeError(
                        response, HttpServletResponse.SC_UNAUTHORIZED, INVALID_TOKEN_MESSAGE);
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                apiResponseWriter.writeError(
                        response, HttpServletResponse.SC_UNAUTHORIZED, INVALID_TOKEN_MESSAGE);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
