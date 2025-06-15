//package yeonjae.snapguide.security.authentication.exception;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.AuthenticationEntryPoint;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
//    private final ObjectMapper objectMapper;
//    private final String UTF_8 = "utf-8";
//
//    @Override
//    public void commence(HttpServletRequest request, HttpServletResponse response,
//                         AuthenticationException authException) throws IOException, ServletException {
//        response.setStatus(HttpStatus.UNAUTHORIZED.value());
//        response.setCharacterEncoding(UTF_8);
//        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//
//        response.getWriter().write(
//                objectMapper.writeValueAsString(
//                        ResponseDto.create(AUTHENTICATION_FAILED.getMessage())
//                )
//        );
//    }
//}