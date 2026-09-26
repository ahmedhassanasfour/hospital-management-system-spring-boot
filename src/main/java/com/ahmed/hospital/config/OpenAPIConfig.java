package com.ahmed.hospital.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI hospitalOpenAPI() {

        return new OpenAPI()
                .info(buildInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local development server")
                ))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        BEARER_AUTH,
                                        new SecurityScheme()
                                                .name(BEARER_AUTH)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "Provide a valid JWT access token obtained from POST /api/auth/login. " +
                                                        "Format: Bearer <token>"
                                                )
                                )
                )
                .security(List.of(
                        new SecurityRequirement().addList(BEARER_AUTH)
                ));
    }

    private Info buildInfo() {
        return new Info()
                .title("Hospital Management System API")
                .version("1.0.0")
                .description("""
                        ## Hospital Management System REST API

                        A comprehensive backend for managing hospital operations including:
                        - **Authentication** — JWT-based login, registration, and token refresh
                        - **Patients & Doctors** — Account management and profile operations
                        - **Appointments** — Scheduling and status management
                        - **Medical Records** — Patient history linked to appointments
                        - **Prescriptions** — Doctor-issued prescriptions with file attachments
                        - **Lab Orders & Results** — Lab test ordering and result recording
                        - **Billing & Payments** — Invoice creation and idempotent payment processing
                        - **Notifications** — In-app notification delivery with WebSocket support
                        - **File Uploads** — Secure medical document management

                        ### Authentication
                        Most endpoints require a **Bearer JWT** token.
                        1. Call `POST /api/auth/login` with your credentials.
                        2. Copy the `accessToken` from the response.
                        3. Click the **Authorize** button (🔒) above and paste: `Bearer <token>`.

                        ### Roles
                        | Role | Prefix |
                        |------|--------|
                        | ADMIN | `/api/admin/**` |
                        | DOCTOR | `/api/doctor/**` |
                        | RECEPTIONIST | `/api/receptionist/**` |
                        | PATIENT | `/api/patient/**` |
                        | Any authenticated | `/api/users/**`, `/api/notifications/**` |

                        ### Public endpoints (no token required)
                        - `POST /api/auth/register`
                        - `POST /api/auth/login`
                        - `POST /api/auth/refresh`
                        - `GET /actuator/health`
                        """)
                .contact(new Contact()
                        .name("Hospital System Team")
                        .email("admin@hospital.com"))
                .license(new License()
                        .name("Private — Internal Use Only"));
    }
}