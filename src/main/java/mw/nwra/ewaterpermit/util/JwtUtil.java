package mw.nwra.ewaterpermit.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtUtil {

	private SecretKey SECRET_KEY;

	private long EXPIRATION_TIME;

	public JwtUtil(@Value("${jwt-secret.value}") String plainKey, @Value("${jwt-secret.expiration}") long expiration) {
		this.EXPIRATION_TIME = expiration;

		byte[] keyBytes = plainKey.getBytes(StandardCharsets.UTF_8);
		this.SECRET_KEY = Keys.hmacShaKeyFor(keyBytes);
	}

//	@Value("${jwt-secret}")
//	private String planKey;
//
//	private SecretKey SECRET_KEY;
//
//	private static final long EXPIRATION_TIME = 86400000; // 24 hours
//
//	public JwtUtil() {
//		byte[] keyBytes = this.planKey.getBytes(StandardCharsets.UTF_8);
//		this.SECRET_KEY = Keys.hmacShaKeyFor(keyBytes);
//	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token).getPayload();
	}

	public Boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	public String generateToken(UserDetails userDetails, boolean rememberMe) {
		Map<String, Object> claims = new HashMap<>();
		return createToken(claims, userDetails.getUsername());
	}

	private String createToken(Map<String, Object> claims, String subject) {

		return Jwts.builder().claims(claims).subject(subject).issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)).signWith(SECRET_KEY).compact();
	}

	public Boolean validateToken(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}
}