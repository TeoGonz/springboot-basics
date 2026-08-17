package com.example.demo.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.service.JpaUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Traduce {@code Authorization: Bearer <token>} en una autenticación del
 * contexto de Spring Security.
 *
 * <p>Si no hay cabecera, o el token no vale, el filtro <b>no</b> corta la
 * petición: la deja seguir sin autenticar y son las reglas de
 * {@code SecurityConfig} las que deciden si eso es un 401 o no. Así las rutas
 * públicas siguen siendo públicas aunque llegue un token roto.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final JpaUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, JpaUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length());
        String username = jwtService.extractUsername(token);
        if (username != null) {
            try {
                UserDetails user = userDetailsService.loadUserByUsername(username);
                if (user.isEnabled()) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (UsernameNotFoundException ex) {
                // Token firmado por nosotros pero de un usuario ya borrado.
                // Se ignora: la petición sigue como anónima.
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Por defecto un {@code OncePerRequestFilter} se salta el reenvío interno a
     * {@code /error}. Al saltarse este, esa segunda pasada llegaba sin
     * autenticación y un 404 acababa contestando 401: con token válido y ruta
     * inexistente, el error que se veía era el equivocado.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    /**
     * Lo mismo con el reenvío ASYNC. Cuando termina una respuesta asíncrona —el
     * SSE de seguimiento del pedido— el contenedor vuelve a pasar la petición por
     * la cadena, y saltándose este filtro esa pasada llega anónima: Security la
     * deniega y deja dos trazas de error por cada stream que se cierra, aunque el
     * cliente ya haya recibido todo. La cabecera sigue en la petición, así que
     * autenticar de nuevo es leerla otra vez.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
}
