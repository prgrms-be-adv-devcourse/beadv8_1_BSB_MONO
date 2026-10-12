package com.bukang.product.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bukang.product.domain.Race;

public interface RaceRepository extends JpaRepository<Race, Integer> {
}
