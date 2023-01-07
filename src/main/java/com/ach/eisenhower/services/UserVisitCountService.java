package com.ach.eisenhower.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

public class UserVisitCountService {

    Logger logger = LoggerFactory.getLogger(UserVisitCountService.class);

    private Map<LocalDate, Integer> visitsByMonth = new HashMap<>();

    public void deserialize(String json) {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        var typeRef = new TypeReference<HashMap<LocalDate, Integer>>() {};
        try {
            visitsByMonth = mapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize user visit count");
        }
    }

    public Optional<String> serialize() {
        try {
            var mapper = new ObjectMapper();
            return Optional.ofNullable(
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(visitsByMonth)
            );
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize user visit count");
            return Optional.empty();
        }
    }

    public int getNumVisits(LocalDate date) {
        date = date.withDayOfMonth(1);
        return visitsByMonth.getOrDefault(date, 0);
    }

    public void incrementVisitsCount(LocalDate date) {
        date = date.withDayOfMonth(1);
        var visits = visitsByMonth.getOrDefault(date, 0);
        visitsByMonth.put(date, visits + 1);
    }
}

