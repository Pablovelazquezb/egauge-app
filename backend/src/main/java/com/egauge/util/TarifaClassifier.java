package com.egauge.util;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.HashSet;

public class TarifaClassifier {
    
    private static final ZoneId MEXICO_TIMEZONE = ZoneId.of("America/Mexico_City");
    
    // Días festivos CFE (puedes expandir según necesidad)
    private static Set<LocalDate> getDiasFestivos(int year) {
        Set<LocalDate> festivos = new HashSet<>();
        festivos.add(LocalDate.of(year, 1, 1));   // Año Nuevo
        festivos.add(LocalDate.of(year, 5, 1));   // Día del Trabajo
        festivos.add(LocalDate.of(year, 9, 16));  // Independencia
        festivos.add(LocalDate.of(year, 12, 25)); // Navidad
        return festivos;
    }
    
    // Calcular primer domingo de abril
    private static LocalDate primerDomingoAbril(int year) {
        LocalDate inicio = LocalDate.of(year, 4, 1);
        while (inicio.getDayOfWeek() != DayOfWeek.SUNDAY) {
            inicio = inicio.plusDays(1);
        }
        return inicio;
    }
    
    // Calcular último domingo de octubre
    private static LocalDate ultimoDomingoOctubre(int year) {
        LocalDate fin = LocalDate.of(year, 10, 31);
        while (fin.getDayOfWeek() != DayOfWeek.SUNDAY) {
            fin = fin.minusDays(1);
        }
        return fin;
    }
    
    // Determinar si es horario de verano CFE
    private static boolean esHorarioVeranoCFE(LocalDateTime fechaHora) {
        int year = fechaHora.getYear();
        LocalDate fecha = fechaHora.toLocalDate();
        
        LocalDate inicioVerano = primerDomingoAbril(year);
        LocalDate finVerano = ultimoDomingoOctubre(year);
        
        return !fecha.isBefore(inicioVerano) && fecha.isBefore(finVerano);
    }
    
    /**
     * Clasifica la tarifa CFE basada en fecha/hora
     * @param fechaHora LocalDateTime en timezone Mexico
     * @return "Base", "Intermedio", o "Punta"
     */
    public static String clasificarTarifa(LocalDateTime fechaHora) {
        return clasificarTarifa(fechaHora, null);
    }
    
    /**
     * Clasifica la tarifa CFE basada en fecha/hora con días festivos
     * @param fechaHora LocalDateTime en timezone Mexico
     * @param diasFestivos Set de días festivos adicionales
     * @return "Base", "Intermedio", o "Punta"
     */
    public static String clasificarTarifa(LocalDateTime fechaHora, Set<LocalDate> diasFestivos) {
        if (fechaHora == null) {
            return "Base"; // Default
        }
        
        // Convertir a timezone México si es necesario
        ZonedDateTime zonedDateTime = fechaHora.atZone(MEXICO_TIMEZONE);
        LocalDateTime fechaLocal = zonedDateTime.toLocalDateTime();
        
        DayOfWeek diaSemana = fechaLocal.getDayOfWeek();
        int hora = fechaLocal.getHour();
        int minutos = fechaLocal.getMinute();
        int minutosDelDia = hora * 60 + minutos;
        
        LocalDate fecha = fechaLocal.toLocalDate();
        int year = fecha.getYear();
        
        // Determinar tipo de día
        boolean esLunesAViernes = diaSemana.getValue() <= 5; // 1=Monday, 7=Sunday
        boolean esSabado = diaSemana == DayOfWeek.SATURDAY;
        boolean esDomingo = diaSemana == DayOfWeek.SUNDAY;
        
        // Verificar días festivos
        Set<LocalDate> festivos = getDiasFestivos(year);
        if (diasFestivos != null) {
            festivos.addAll(diasFestivos);
        }
        boolean esFestivo = festivos.contains(fecha);
        boolean esFestivoODomingo = esFestivo || esDomingo;
        
        // Determinar temporada
        boolean esVerano = esHorarioVeranoCFE(fechaLocal);
        
        if (esVerano) {
            // REGLAS DE VERANO
            if (esLunesAViernes) {
                // Lunes a Viernes - Verano
                if (minutosDelDia >= 0 && minutosDelDia < 360) {        // 00:00-06:00
                    return "Base";
                } else if (minutosDelDia >= 1200 && minutosDelDia < 1320) { // 20:00-22:00
                    return "Punta";
                } else if ((minutosDelDia >= 360 && minutosDelDia < 1200) || // 06:00-20:00
                          (minutosDelDia >= 1320 && minutosDelDia < 1440)) {  // 22:00-24:00
                    return "Intermedio";
                }
            } else if (esSabado) {
                // Sábado - Verano
                if (minutosDelDia >= 0 && minutosDelDia < 420) {       // 00:00-07:00
                    return "Base";
                } else {                                               // 07:00-24:00
                    return "Intermedio";
                }
            } else if (esFestivoODomingo) {
                // Domingo/Festivos - Verano
                if (minutosDelDia >= 0 && minutosDelDia < 1140) {      // 00:00-19:00
                    return "Base";
                } else {                                               // 19:00-24:00
                    return "Intermedio";
                }
            }
        } else {
            // REGLAS DE INVIERNO
            if (esLunesAViernes) {
                // Lunes a Viernes - Invierno
                if (minutosDelDia >= 0 && minutosDelDia < 360) {        // 00:00-06:00
                    return "Base";
                } else if (minutosDelDia >= 1080 && minutosDelDia < 1320) { // 18:00-22:00
                    return "Punta";
                } else if ((minutosDelDia >= 360 && minutosDelDia < 1080) || // 06:00-18:00
                          (minutosDelDia >= 1320 && minutosDelDia < 1440)) {  // 22:00-24:00
                    return "Intermedio";
                }
            } else if (esSabado) {
                // Sábado - Invierno
                if (minutosDelDia >= 0 && minutosDelDia < 480) {       // 00:00-08:00
                    return "Base";
                } else if (minutosDelDia >= 1140 && minutosDelDia < 1260) { // 19:00-21:00
                    return "Punta";
                } else if ((minutosDelDia >= 480 && minutosDelDia < 1140) || // 08:00-19:00
                          (minutosDelDia >= 1260 && minutosDelDia < 1440)) {  // 21:00-24:00
                    return "Intermedio";
                }
            } else if (esFestivoODomingo) {
                // Domingo/Festivos - Invierno
                if (minutosDelDia >= 0 && minutosDelDia < 1080) {      // 00:00-18:00
                    return "Base";
                } else {                                               // 18:00-24:00
                    return "Intermedio";
                }
            }
        }
        
        // Default fallback
        return "Base";
    }
}