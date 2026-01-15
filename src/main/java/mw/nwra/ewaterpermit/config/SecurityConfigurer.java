package mw.nwra.ewaterpermit.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import mw.nwra.ewaterpermit.filter.JwtRequestFilter;
import mw.nwra.ewaterpermit.filter.WRMISIPWhitelistFilter;
import mw.nwra.ewaterpermit.service.CustomUserDetailsService;
import mw.nwra.ewaterpermit.util.CustomPasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfigurer {
	@Autowired
	private CustomUserDetailsService customUserDetailsService;
	@Autowired
	private JwtRequestFilter jwtRequestFilter;
	@Autowired
	private WRMISIPWhitelistFilter wrmisIPWhitelistFilter;

	@Value("${cors.mode}")
	private String corsMode;

	@Value("#{'${whitelisted-domains}'.split(',')}")
	private List<String> whitelistedDomains;

	private static final String[] AUTH_WHITELIST = { "/v1/swagger-resources/**", "/swagger-ui/**", "/v3/api-docs/**",
			"/webjars/**", "/test/**", "/v1/auth/**", "/v1/configs/**", "/v1/applications/**/process-payment",
			"/v1/applications/**/payment-status", "/v1/applications/**/invoice/download", "/uploads/**",
			"/v1/wrmis/auth/**" }; // WRMIS authentication endpoints (IP-restricted but publicly accessible)

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);

		// Enable/disable CORS based on mode: dev (Spring handles) or production (Nginx handles)
		if ("production".equalsIgnoreCase(corsMode)) {
			http.cors(AbstractHttpConfigurer::disable);
		} else {
			http.cors(withDefaults());
		}

		http.authorizeHttpRequests(requests -> requests
				.requestMatchers(HttpMethod.GET, "**").permitAll()
				.requestMatchers(AUTH_WHITELIST).permitAll()
				.anyRequest().authenticated())
			.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.logout(withDefaults())
			.exceptionHandling(handling -> handling.accessDeniedPage("/v1/configs/accessdenied"));

		// Add filters in order (both relative to UsernamePasswordAuthenticationFilter which has a registered order)
		// WRMIS IP filter runs first, then JWT filter
		http.addFilterBefore(wrmisIPWhitelistFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		if ("production".equalsIgnoreCase(corsMode)) {
			// Production: Empty config (CORS handled by Nginx)
			configuration.setAllowedOrigins(List.of());
		} else {
			// Dev: Spring handles CORS with whitelisted domains
			configuration.setAllowedOrigins(whitelistedDomains);
			configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
			configuration.setAllowedHeaders(List.of("*"));
			configuration.setAllowCredentials(true);
		}

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

		authProvider.setUserDetailsService(this.customUserDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());

		return authProvider;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new CustomPasswordEncoder();
	}
//	
//	@Bean
//	public AuthenticationFailureHandler authenticationFailureHandler() {
//	    return new CustomAuthenticationFailureHandler();
//	} 

//	@Bean
//	public AuthenticationSuccessHandler authenticationSuccessHandler() {
//	   return new CustomAuthenticationSuccessHandler();
//	}

//	@Bean
//	public AccessDeniedHandler accessDeniedHandler() {
//	   return new CustomAccessDeniedHandler();
//	}
}
