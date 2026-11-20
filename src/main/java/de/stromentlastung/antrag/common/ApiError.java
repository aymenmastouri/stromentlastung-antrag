package de.stromentlastung.antrag.common;

/** Fehlerkörper aller Dienste: ein Regelcode für die Oberfläche, eine Diagnose für Entwickler. */
public record ApiError(String code, String message) {
}
