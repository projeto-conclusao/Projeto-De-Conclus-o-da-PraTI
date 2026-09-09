package com.achadosedevolvidos.match.service;

import com.achadosedevolvidos.match.dto.MatchCandidate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Lógica pura de pontuação de compatibilidade entre um item perdido e um item
 * encontrado. Não importa nada de JPA, Spring Web ou do resto da aplicação além
 * de {@link MatchCandidate} — dá pra instanciar com "new MatchEngineService()" e
 * testar sem subir contexto Spring nem tocar em banco de dados.
 */
@Component
public class MatchEngineService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public double calculateMatchScore(MatchCandidate lost, MatchCandidate found) {
        if (lost.type() == found.type() || !lost.categoryId().equals(found.categoryId())) {
            return 0.0;
        }

        double score = 20.0; // Mesma categoria garante 20%

        // Proximidade geográfica (peso de 40%)
        double distanceKm = calculateDistance(
                lost.latitude(), lost.longitude(),
                found.latitude(), found.longitude()
        );

        if (distanceKm <= 1.0) score += 40.0;
        else if (distanceKm <= 5.0) score += 30.0;
        else if (distanceKm <= 15.0) score += 15.0;
        else if (distanceKm <= 30.0) score += 5.0;

        // Proximidade temporal (peso de 25%)
        long diffDays = Math.abs(Duration.between(lost.eventDate(), found.eventDate()).toDays());

        if (diffDays <= 1) score += 25.0;
        else if (diffDays <= 3) score += 18.0;
        else if (diffDays <= 7) score += 10.0;

        // Correspondência de título (peso de 15%)
        String[] lostTitleWords = lost.title().toLowerCase().split("\\s+");
        String foundTitle = found.title().toLowerCase();

        for (String word : lostTitleWords) {
            if (word.length() > 3 && foundTitle.contains(word)) {
                score += 15.0;
                break;
            }
        }

        return Math.min(Math.round(score), 100.0);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
