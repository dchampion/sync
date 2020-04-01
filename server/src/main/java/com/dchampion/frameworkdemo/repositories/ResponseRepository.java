package com.dchampion.frameworkdemo.repositories;

import com.dchampion.frameworkdemo.entities.Response;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseRepository extends JpaRepository<Response, Long> {
}