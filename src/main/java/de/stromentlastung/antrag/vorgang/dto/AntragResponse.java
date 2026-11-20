package de.stromentlastung.antrag.vorgang.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import de.stromentlastung.antrag.rechenkern.Entlastungsrechner;
import de.stromentlastung.antrag.vorgang.AntragZustand;
import de.stromentlastung.antrag.vorgang.EntscheidungVorschlag;

public record AntragResponse(String aktenzeichen, String unternehmen, String dienststelle, int entnahmejahr, AntragZustand zustand,
                             long versteuertKwh, long elektromobilitaetKwh, boolean elektromobilitaetGeschaetzt, long nutzenergieDritteKwh,
                             boolean versicherungKeineRueckforderung, boolean versicherungKeineSchwierigkeiten, boolean selbsterklaerungBeihilfen,
                             Long selbstberechnetCent, Long nachberechnetCent, Long festgesetztCent, LocalDate eingereichtAm,
                             Integer antragsjahr, String bearbeitung, EntscheidungVorschlag entscheidungVorschlag, String entscheidungGrund,
                             String entscheidungBegruendung, String gezeichnetVon, String rueckfrageText, String rueckfrageAntwort,
                             String pruefvermerk, Long korrigiertVersteuertKwh, Long korrigiertElektromobilitaetKwh,
                             Long korrigiertNutzenergieDritteKwh, String aenderungBegruendung, Long bisherFestgesetztCent,
                             Long rueckforderungCent, String letzterBescheid, Instant angelegtAm, Instant geaendertAm,
                             Entlastungsrechner.Berechnung berechnung, FristenResponse fristen, List<NachweisResponse> nachweise,
                             List<String> aktionen) {
}
