package arunyilvantarto.domain;

import com.fasterxml.jackson.annotation.*;

import java.time.Instant;
import java.util.Objects;

public class Message {

    public @JsonIdentityReference
    User sender;
    public Instant timestamp;
    public String text;
    public Subject subject;


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = OpenPeriodSubject.class, name = "OpenPeriod"),
            @JsonSubTypes.Type(value = ClosePeriodSubject.class, name = "ClosePeriod")
    })
    public interface Subject {
    }

    public static class OpenPeriodSubject implements Subject {

        @JsonProperty
        public final int periodID;

        @JsonCreator
        public OpenPeriodSubject(@JsonProperty("periodID") int periodID) {
            this.periodID = periodID;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OpenPeriodSubject that = (OpenPeriodSubject) o;
            return periodID == that.periodID;
        }

        @Override
        public int hashCode() {
            return Objects.hash(periodID);
        }
    }


    public static class ClosePeriodSubject implements Subject {
        @JsonProperty
        public final int periodID;

        @JsonCreator
        public ClosePeriodSubject(@JsonProperty("periodID") int periodID) {
            this.periodID = periodID;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClosePeriodSubject that = (ClosePeriodSubject) o;
            return periodID == that.periodID;
        }

        @Override
        public int hashCode() {
            return Objects.hash(periodID);
        }
    }
}
