package com.maf.exchangeapiv1.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;


    public String generateAccessToken(String userId, String email) {
        return generateToken(userId, email, accessExpirationMs);
    }

    public String generateRefreshToken(String userId, String email) {
        return generateToken(userId, email, refreshExpirationMs);
    }

    private String generateToken(String userId, String email, long expirationMs) {
        try {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(userId)
                    .claim("email", email)
                    .expirationTime(Date.from(Instant.now().plusMillis(expirationMs)))
                    .issueTime(Date.from(Instant.now()))
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            MACSigner signer = new MACSigner(secret.getBytes());
            signedJWT.sign(signer);
            log.debug("JWT has been created. User: {}", userId);
            return signedJWT.serialize();

        } catch (JOSEException e) {
            log.error("JWT can not be created : {}", userId, e);
            throw new RuntimeException("JWT can not be created", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            MACVerifier verifier = new MACVerifier(secret.getBytes());

            if (!signedJWT.verify(verifier)) {
                return false;
            }

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            return expirationTime.after(Date.from(Instant.now()));

        } catch (Exception e) {
            log.debug("[SECURITY]:Token verification Error: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new RuntimeException("[SECURITY]:Token can't be parsed", e);
        }
    }

    public String getEmailFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return (String) signedJWT.getJWTClaimsSet().getClaim("email");
        } catch (ParseException e) {
            throw new RuntimeException("[SECURITY]:Token can't be parsed", e);
        }
    }
}
