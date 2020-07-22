package org.openmrs.module.eptsreports.reporting.calculation.txcurr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.eptsreports.metadata.HivMetadata;
import org.openmrs.module.eptsreports.reporting.calculation.AbstractPatientCalculation;
import org.openmrs.module.eptsreports.reporting.calculation.BooleanResult;
import org.openmrs.module.eptsreports.reporting.calculation.common.EPTSCalculationService;
import org.openmrs.module.eptsreports.reporting.utils.EptsCalculationUtils;
import org.openmrs.module.reporting.common.TimeQualifier;
import org.springframework.stereotype.Component;

@Component
public class LessThan3MonthsOfArvDispensationCalculation extends AbstractPatientCalculation {
  @Override
  public CalculationResultMap evaluate(
      Collection<Integer> cohort, Map<String, Object> map, PatientCalculationContext context) {

    HivMetadata hivMetadata = Context.getRegisteredComponents(HivMetadata.class).get(0);
    EPTSCalculationService ePTSCalculationService =
        Context.getRegisteredComponents(EPTSCalculationService.class).get(0);

    CalculationResultMap resultMap = new CalculationResultMap();

    Location location = (Location) context.getFromCache("location");
    Date onOrBefore = (Date) context.getFromCache("onOrBefore");

    EncounterType fila = hivMetadata.getARVPharmaciaEncounterType();
    EncounterType ficha = hivMetadata.getAdultoSeguimentoEncounterType();

    Concept returnVisitDateForArvDrugs = hivMetadata.getReturnVisitDateForArvDrugConcept();
    Concept typeOfDispensation = hivMetadata.getTypeOfDispensationConcept();
    Concept monthly = hivMetadata.getMonthlyConcept();

    CalculationResultMap getFilaEncounterMap =
        ePTSCalculationService.getEncounter(
            Arrays.asList(fila), TimeQualifier.LAST, cohort, location, onOrBefore, context);
    CalculationResultMap getFichaEncounterMap =
        ePTSCalculationService.getEncounter(
            Arrays.asList(ficha), TimeQualifier.LAST, cohort, location, onOrBefore, context);

    for (Integer pId : cohort) {
      boolean found = false;

      Encounter lastFilaEncounter = EptsCalculationUtils.resultForPatient(getFilaEncounterMap, pId);
      Encounter lastFichaEncounter =
          EptsCalculationUtils.resultForPatient(getFichaEncounterMap, pId);

      Date lastFilaEncounterDate = null;
      Date lastFichaEncounterDate = null;

      if (lastFilaEncounter != null && lastFilaEncounter.getEncounterDatetime() != null) {
        lastFilaEncounterDate = lastFilaEncounter.getEncounterDatetime();
      }

      if (lastFichaEncounter != null && lastFichaEncounter.getEncounterDatetime() != null) {
        lastFichaEncounterDate = lastFichaEncounter.getEncounterDatetime();
      }

      if (lastFilaEncounterDate != null
          && lastFichaEncounterDate != null
          && lastFilaEncounterDate.after(lastFichaEncounterDate)) {
        if (lastFilaEncounter.getAllObs() != null) {
          for (Obs obs : lastFilaEncounter.getAllObs()) {
            if (obs.getConcept().equals(returnVisitDateForArvDrugs)
                && obs.getValueDatetime() != null
                && EptsCalculationUtils.daysSince(lastFilaEncounterDate, obs.getValueDatetime())
                    < 83) {
              found = true;
              break;
            }
          }
        }
      } else if (lastFilaEncounterDate != null
          && lastFichaEncounterDate != null
          && lastFichaEncounterDate.after(lastFilaEncounterDate)) {
        if (lastFichaEncounter.getAllObs() != null) {
          for (Obs obs : lastFichaEncounter.getAllObs()) {
            if (obs.getConcept().equals(typeOfDispensation)
                && obs.getValueCoded().equals(monthly)) {
              found = true;
              break;
            }
          }
        }

      } else if (lastFilaEncounterDate != null
          && lastFichaEncounterDate == null
          && lastFilaEncounter.getAllObs() != null) {
        for (Obs obs : lastFilaEncounter.getAllObs()) {
          if (obs.getConcept().equals(returnVisitDateForArvDrugs)
              && obs.getValueDatetime() != null
              && EptsCalculationUtils.daysSince(lastFilaEncounterDate, obs.getValueDatetime())
                  < 83) {
            found = true;
            break;
          }
        }
      } else if (lastFilaEncounterDate == null
          && lastFichaEncounterDate != null
          && lastFichaEncounter.getAllObs() != null) {
        for (Obs obs : lastFichaEncounter.getAllObs()) {
          if (obs.getConcept().equals(typeOfDispensation) && obs.getValueCoded().equals(monthly)) {
            found = true;
            break;
          }
        }
      } else if (lastFilaEncounterDate != null
          && lastFilaEncounterDate.equals(lastFichaEncounterDate)) {

        for (Obs obs : lastFilaEncounter.getAllObs()) {
          if (obs.getConcept().equals(returnVisitDateForArvDrugs)
              && obs.getValueDatetime() != null
              && EptsCalculationUtils.daysSince(lastFilaEncounterDate, obs.getValueDatetime())
                  < 83) {
            found = true;
            break;
          }
        }
        if (!found) {
          for (Obs obs : lastFichaEncounter.getAllObs()) {
            if (obs.getConcept().equals(typeOfDispensation)
                && obs.getValueCoded().equals(monthly)) {
              found = true;
              break;
            }
          }
        }
      }
      resultMap.put(pId, new BooleanResult(found, this));
    }
    return resultMap;
  }
}
