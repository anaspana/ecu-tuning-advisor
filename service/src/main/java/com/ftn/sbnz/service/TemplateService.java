package com.ftn.sbnz.service;

import com.ftn.sbnz.model.enums.EcuType;
import com.ftn.sbnz.model.models.TuningDecision;
import com.ftn.sbnz.model.models.TuningRequest;
import org.drools.template.DataProvider;
import org.drools.template.DataProviderCompiler;
import org.drools.template.objects.ArrayDataProvider;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Servis koji ucitava ECU limite iz CSV fajla, kompajlira DRT template
 * u DRL i pokrece Drools sesiju za proveru zahteva za tuning.
 *
 * CSV format (templ.csv):
 *   ecuType,brand,maxBoost,maxTemp,maxTorque
 *   BOSCH_EDC17,VW,1.8,850,400,750
 *   ...
 *
 */
@Service
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    /** Putanja do CSV fajla sa ECU limitima (u classpath-u service modula) */
    private static final String CSV_PATH = "/rules/template/ecu-limits.csv";

    /** Putanja do DRT template fajla (u classpath-u kjar modula) */
    private static final String DRT_PATH = "/rules/template/ecu-limits.drt";

    /**
     * Proverava da li je zahtev za tuning dozvoljeno primeniti.
     *
     * @param request zahtev sa ECU tipom, markom i trazenim parametrima
     * @return TuningDecision sa ALLOW ili BLOCK odlukom
     */
    public TuningDecision evaluateTuning(TuningRequest request) {
        log.info("Evaluacija tuning zahteva za vozilo: {} ({}/{})",
                request.getVehicleId(), request.getEcuType(), request.getBrand());

        // 1. Ucitaj CSV i pripremi podatke za template
        String[][] tableData = loadCsvData();

        // 2. Ucitaj DRT template
        InputStream drtStream = getClass().getResourceAsStream(DRT_PATH);
        if (drtStream == null) {
            throw new IllegalStateException("DRT template fajl nije pronadjen na: " + DRT_PATH);
        }

        // 3. Kompajliraj template + podatke u DRL
        DataProvider dataProvider = new ArrayDataProvider(tableData);
        DataProviderCompiler compiler = new DataProviderCompiler();
        String drl = compiler.compile(dataProvider, drtStream);

        // log.debug("Generisani DRL:\n{}", drl);

        // 4. Napravi KieSession iz generisanog DRL-a
        KieSession kieSession = buildKieSession(drl);

        try {
            // 5. Ubaci zahtev u radnu memoriju i pokrni pravila
            kieSession.insert(request);
            kieSession.fireAllRules();

            // 6. Pokupi odluku iz radne memorije
            Collection<?> facts = kieSession.getObjects();
            Optional<TuningDecision> decision = facts.stream()
                    .filter(f -> f instanceof TuningDecision)
                    .map(f -> (TuningDecision) f)
                    .findFirst();

            if (decision.isPresent()) {
                log.info("Odluka za vozilo {}: {} - {}",
                        request.getVehicleId(), decision.get().getDecision(), decision.get().getReason());
                return decision.get();
            } else {
                // Nema pravila za ovu kombinaciju ECU/brand
                String msg = "Nema definisanih limita za kombinaciju: "
                        + request.getEcuType() + "/" + request.getBrand();
                log.warn(msg);
                return new TuningDecision(request.getVehicleId(),
                        TuningDecision.Decision.BLOCK, msg,
                        request.getEcuType() + "/" + request.getBrand());
            }
        } finally {
            kieSession.dispose();
        }
    }

    /**
     * Ucitava CSV fajl i vraca dvodimenzionalni niz stringova
     * (bez header reda, samo podaci: ecuType, brand, maxBoost, maxTemp, maxTorque)
     */
    private String[][] loadCsvData() {
        InputStream csvStream = getClass().getResourceAsStream(CSV_PATH);
        if (csvStream == null) {
            throw new IllegalStateException("CSV fajl nije pronadjen na: " + CSV_PATH);
        }

        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false; // preskoci header
                    continue;
                }
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] cols = line.split(",");
                // cols: [ecuType, brand, maxBoost, maxTemp, maxTorque]
                // template header: ecuType, brand, maxBoost, maxTemp, maxTorque
                rows.add(new String[]{
                        cols[0].trim(), // ecuType
                        cols[1].trim(), // brand
                        cols[2].trim(), // maxBoost
                        cols[3].trim(), // maxTemp
                        cols[4].trim()  // maxTorque
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Greska pri citanju CSV fajla: " + e.getMessage(), e);
        }

        log.debug("Ucitano {} redova iz CSV tabele ECU limita.", rows.size());
        return rows.toArray(new String[0][]);
    }

    /**
     * Kreira KieSession iz generisanog DRL stringa koristeci KieHelper.
     */
    private KieSession buildKieSession(String drl) {
        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);

        Results results = kieHelper.verify();
        if (results.hasMessages(Message.Level.WARNING, Message.Level.ERROR)) {
            results.getMessages(Message.Level.WARNING, Message.Level.ERROR)
                    .forEach(m -> log.error("DRL greska: {}", m.getText()));
            throw new IllegalStateException("Kompajliranje generisanog DRL-a nije uspelo. Proverite logove.");
        }

        return kieHelper.build().newKieSession();
    }
}
