package gr.hua.dit.ds.shelter.config;

import gr.hua.dit.ds.shelter.entities.UserType;
import gr.hua.dit.ds.shelter.service.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String targetUrl = "/";

        // If user has admin role, redirect to admin area (optional)
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            response.sendRedirect("/");
            return;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            UserType type = cud.getUserType();
            if (type != null) {
                switch (type) {
                    case VET -> targetUrl = "/vet/" + cud.getUserId();
                    case VISITOR -> targetUrl = "/visitor/" + cud.getUserId();
                    case SHELTER -> targetUrl = "/shelter/" + cud.getUserId();
                    default -> targetUrl = "/";
                }
            }
        }

        response.sendRedirect(targetUrl);
    }
}