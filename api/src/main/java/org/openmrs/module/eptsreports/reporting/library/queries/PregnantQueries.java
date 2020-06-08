/*
 * The contents of this file are subject to the OpenMRS Public License Version
 * 1.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.module.eptsreports.reporting.library.queries;

/** Re usable queries that can be used for finding patients who are pregnant */
public class PregnantQueries {

  /** GRAVIDAS INSCRITAS NO SERVIÇO TARV */
  public static String getPregnantWhileOnArt(
      int pregnantConcept,
      int pregnantResponseConcept,
      int weeksPregnantConcept,
      int pregnancyDueDateConcept,
      int adultInitailEncounter,
      int adultSegEncounter,
      int fichaResumo,
      int lastMenstrualPeriod,
      int etvProgram,
      int startARVCriteriaConcept,
      int bPLusConcept,
      int historicalARTStartDate) {
    
    
    return " SELECT patient_id"
             +"FROM   (SELECT patient_id,"
             +"            Max(pregnancy_date) AS pregnancy_date"
             +"     FROM   (SELECT p.patient_id,"
             +"                    Max(e.encounter_datetime) AS pregnancy_date"
             +"             FROM   patient p"
             +"                    INNER JOIN person pe"
             +"                            ON p.patient_id = pe.person_id"
             +"                    INNER JOIN encounter e"
             +"                            ON p.patient_id = e.patient_id"
             +"                    INNER JOIN obs o"
             +"                            ON e.encounter_id = o.encounter_id"
             +"             WHERE  p.voided = 0"
             +"                    AND e.voided = 0"
             +"                    AND o.voided = 0"
             +"                    AND concept_id = "+pregnantConcept
             +"                    AND value_coded = "+pregnantResponseConcept
             +"                    AND e.encounter_type IN ("+adultInitailEncounter+","+adultSegEncounter+")"
             +"                    AND e.encounter_datetime BETWEEN"
             +"                        :startDate AND :endDate"
             +"                    AND e.location_id = :location"
             +"                    AND pe.gender = 'F'"
             +"             GROUP  BY p.patient_id"
             +"             UNION"
             +"             SELECT p.patient_id,"
             +"                    Max(historical_date.value_datetime) AS pregnancy_date"
             +"             FROM   patient p"
             +"                    INNER JOIN person pe"
             +"                            ON p.patient_id = pe.person_id"
             +"                    INNER JOIN encounter e"
             +"                            ON p.patient_id = e.patient_id"
             +"                    INNER JOIN obs pregnancy"
             +"                            ON e.encounter_id = pregnancy.encounter_id"
             +"                    INNER JOIN obs historical_date"
             +"                            ON e.encounter_id = historical_date.encounter_id"
             +"             WHERE  p.voided = 0"
             +"                    AND e.voided = 0"
             +"                    AND pregnancy.voided = 0"
             +"                    AND pregnancy.concept_id = "+pregnantConcept
             +"                    AND pregnancy.value_coded = "+pregnantResponseConcept
             +"                    AND historical_date.voided = 0"
             +"                    AND historical_date.concept_id = "+historicalARTStartDate
             +"                    AND historical_date.value_datetime IS NOT NULL"
             +"                    AND e.encounter_type = "+fichaResumo
             +"                    AND historical_date.value_datetime BETWEEN"
             +"                        :startDate AND :endDate"
             +"                    AND e.location_id = :location"
             +"                    AND pe.gender = 'F'"
             +"             GROUP  BY p.patient_id"
             +"             UNION"
             +"             SELECT p.patient_id,"
             +"                    Max(e.encounter_datetime) AS pregnancy_date"
             +"             FROM   patient p"
             +"                    INNER JOIN person pe"
             +"                            ON p.patient_id = pe.person_id"
             +"                    INNER JOIN encounter e"
             +"                            ON p.patient_id = e.patient_id"
             +"                    INNER JOIN obs o"
             +"                            ON e.encounter_id = o.encounter_id"
             +"             WHERE  p.voided = 0"
             +"                    AND e.voided = 0"
             +"                    AND o.voided = 0"
             +"                    AND concept_id = "+weeksPregnantConcept
             +"                    AND e.encounter_type IN ("+adultInitailEncounter+","+adultSegEncounter+")"
             +"                    AND e.encounter_datetime BETWEEN"
             +"                        :startDate AND :endDate"
             +"                    AND e.location_id = :location"
             +"                    AND pe.gender = 'F'"
             +"             GROUP  BY p.patient_id"
             +"             UNION"
             +"             SELECT p.patient_id,"
             +"                    e.encounter_datetime AS pregnancy_date"
             +"             FROM   patient p"
             +"                    INNER JOIN person pe"
             +"                            ON p.patient_id = pe.person_id"
             +"                    INNER JOIN encounter e"
             +"                            ON p.patient_id = e.patient_id"
             +"                    INNER JOIN obs o"
             +"                            ON e.encounter_id = o.encounter_id"
             +"             WHERE  p.voided = 0"
             +"                    AND e.voided = 0"
             +"                    AND o.voided = 0"
             +"                    AND concept_id = "+pregnancyDueDateConcept
             +"                    AND e.encounter_type IN ("+adultInitailEncounter+","+adultSegEncounter+")"
             +"                    AND e.encounter_datetime BETWEEN"
             +"                        :startDate AND :endDate"
             +"                    AND e.location_id = :location"
             +"                    AND pe.gender = 'F'"
             +"             GROUP  BY p.patient_id"
             +"             UNION"
             +"             SELECT p.patient_id,"
             +"                    Max(e.encounter_datetime) AS pregnancy_date"
             +"             FROM   patient p"
             +"                    INNER JOIN person pe"
             +"                            ON p.patient_id = pe.person_id"
             +"                    INNER JOIN encounter e"
             +"                            ON p.patient_id = e.patient_id"
             +"                    INNER JOIN obs o"
             +"                            ON e.encounter_id = o.encounter_id"
             +"             WHERE  p.voided = 0"
             +"                    AND pe.voided = 0"
             +"                    AND e.voided = 0"
             +"                    AND o.voided = 0"
             +"                    AND concept_id = "+startARVCriteriaConcept
             +"                    AND value_coded = "+bPLusConcept
             +"                    AND e.encounter_type IN ("+adultInitailEncounter+","+adultSegEncounter+")"
             +"                    AND e.encounter_datetime BETWEEN"
             +"                        :startDate AND :endDate"
             +"                    AND e.location_id = :location"
             +"                    AND pe.gender = 'F'"
             +"             GROUP  BY p.patient_id"
             +"             UNION"
             +"             SELECT pp.patient_id,"
             +"                    Max(pp.date_enrolled) AS pregnancy_date"
             +"             FROM   patient_program pp"
             +"                    INNER JOIN person pe"
             +"                            ON pp.patient_id = pe.person_id"
             +"             WHERE  pp.program_id = "+etvProgram
             +"                    AND pp.voided = 0"
             +"                    AND pp.date_enrolled BETWEEN"
             +"                        :startDate AND :endDate"
             +"                    AND pp.location_id = :location"
             +"                    AND pe.gender = 'F'"
             +"             GROUP  BY pp.patient_id"
             +"             UNION"
             +"             SELECT p.patient_id,"
             +"                    Max(o.value_datetime) AS pregnancy_date"
             +"             FROM   patient p"
             +"                    INNER JOIN person pe"
             +"                            ON p.patient_id = pe.person_id"
             +"                    INNER JOIN encounter e"
             +"                            ON p.patient_id = e.patient_id"
             +"                    INNER JOIN obs o"
             +"                            ON e.encounter_id = o.encounter_id"
             +"             WHERE  p.voided = 0"
             +"                    AND e.voided = 0"
             +"                    AND o.voided = 0"
             +"                    AND concept_id = "+lastMenstrualPeriod
             +"                    AND e.encounter_type = "+adultSegEncounter
             +"                    AND o.value_datetime BETWEEN"
             +"                        :startDate AND :endDate"
             +"             GROUP  BY p.patient_id) pregnancy"
             +"     GROUP  BY patient_id) AS pregnant_women;  ";

  }
}
