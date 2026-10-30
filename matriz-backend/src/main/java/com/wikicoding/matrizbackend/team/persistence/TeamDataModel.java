package com.wikicoding.matrizbackend.team.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** JPA representation of the {@code team} table. */
@Entity
@Table(name = "team")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class TeamDataModel {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

}
