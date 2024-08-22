package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.service.WorkService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;

import java.util.Collections;
import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Log4j2
@AllArgsConstructor
@Profile({"init-domain", "init-default-data"})
@ChangeUnit(id = "init-domain", order = "1000", author = "bisegni")
public class M1000_InitDomain {
    private final LOVService lovService;
    private final DomainService domainService;
    @Execution
    public void changeSet() {
        initLOV();
        initTECDomain();
    }


    @RollbackExecution
    public void rollback() {

    }

    /**
     * Init all LOVs
     */
    public void initLOV() {
        //Facility: {Batch, Cater, Database, EPICS, Micro, PEPII, SCP, Touch Panels, Other}
        lovService.createNew(
                "FacilityGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Batch")
                                .description("Batch")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Cater")
                                .description("Cater")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Database")
                                .description("Database")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("EPICS")
                                .description("EPICS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Micro")
                                .description("Micro")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PEPII")
                                .description("PEPII")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("SCP")
                                .description("SCP")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Touch Panels")
                                .description("Touch Panels")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Other")
                                .description("Other")
                                .build()
                )
        );
        //Urgency: {Scheduled, Immediate, Downtime, Low Priority}
        lovService.createNew(
                "UrgencyGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Scheduled")
                                .description("Scheduled")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Immediate")
                                .description("Immediate")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Downtime")
                                .description("Downtime")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Low Priority")
                                .description("Low Priority")
                                .build()
                )
        );

        lovService.createNew(
                "AccessRequirementsGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Any Access")
                                .description("Any access is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Controlled Access")
                                .description("Controlled access is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("No Access")
                                .description("No access is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Restricted Access")
                                .description("Restricted access is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Permitted Access")
                                .description("Permitted access is allowed")
                                .build()
                )
        );

        lovService.createNew(
                "BeamRequirementsGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Beam")
                                .description("Beam is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("No Beam")
                                .description("No beam is allowed")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("VVSs on")
                                .description("VVSs are on")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("ACC trig")
                                .description("ACC is triggered")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("STBY Trig")
                                .description("STBY is triggered")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("No Requirement")
                                .description("No beam is allowed")
                                .build()
                )
        );

        lovService.createNew(
                "SchedulingPriorityGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Benign")
                                .description("Benign work type")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Downtime")
                                .description("Downtime work type")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PAMM")
                                .description("PAMM work type")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("POMM")
                                .description("POMM work type")
                                .build()
                )
        );

        lovService.createNew(
                "TaskPriorityGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("1")
                                .description("Highest priority")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("2")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("3")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("4")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("5")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("6")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("7")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("8")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("9")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("10")
                                .description("Lowest priority")
                                .build()
                )
        );

        //{Yes, No, Unknown, Do Not Need}
        lovService.createNew(
                "DocSolutionGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Yes")
                                .description("Yes")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("No")
                                .description("No")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Unknown")
                                .description("Unknown")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Do Not Need")
                                .description("Do Not Need")
                                .build()
                )
        );


        lovService.createNew(
                "SubSystemGroup",
                List.of(
                        NewLOVElementDTO.builder().value("ACM").description("ACM").build(),
                        NewLOVElementDTO.builder().value("AIDA").description("AIDA").build(),
                        NewLOVElementDTO.builder().value("ALARMS").description("Alarms").build(),
                        NewLOVElementDTO.builder().value("ALC CF HVAC").description("ALC CF HVAC").build(),
                        NewLOVElementDTO.builder().value("ALIGN DIAG SYS").description("Align Diag Sys").build(),
                        NewLOVElementDTO.builder().value("ARCHIVER").description("Archiver").build(),
                        NewLOVElementDTO.builder().value("ATCA").description("ATCA-Based Common Platform hardware and software. Hardware includes: ATCA Crates, ATCA Shelf Managers, ATCA 10G Ethernet Switches, and Industrial Rack-mount Computers. Software includes: linuxRT OS, CPSW API/Library communication layer, IPMI Management Software, and Common Platform Software tools for diagnostics and maintenance.").build(),
                        NewLOVElementDTO.builder().value("BCM").description("For DEPOT Use").build(),
                        NewLOVElementDTO.builder().value("BCS").description("Beam Containment System").build(),
                        NewLOVElementDTO.builder().value("BLD").description("BLD").build(),
                        NewLOVElementDTO.builder().value("BLEN").description("For DEPOT Use").build(),
                        NewLOVElementDTO.builder().value("BLF").description("BLF").build(),
                        NewLOVElementDTO.builder().value("BLM").description("BLM").build(),
                        NewLOVElementDTO.builder().value("BPM").description("BPMs, muxes, processors, strip-line monitors").build(),
                        NewLOVElementDTO.builder().value("CAMAC").description("crate, power supply, modules, serial link").build(),
                        NewLOVElementDTO.builder().value("CATER").description("").build(),
                        NewLOVElementDTO.builder().value("CATV").description("Cable, amplifiers, Sytek, System 20 boxes").build(),
                        NewLOVElementDTO.builder().value("CMLOG").description("CMLOG").build(),
                        NewLOVElementDTO.builder().value("COMPRESSED AIR").description("").build(),
                        NewLOVElementDTO.builder().value("COMPUTER").description("SLC/MCC computer systems, ethernet, terminals, workstations, printers etc.").build(),
                        NewLOVElementDTO.builder().value("COMPUTING INFRASTRUCTURE").description("Computing Infrastructure").build(),
                        NewLOVElementDTO.builder().value("CONTROLS INFRASTRUCTURE").description("Controls Production Environment, Data Management, Matlab support (Matlab software and license server), Controls Software Management (Python, Perl, all third party packages required by controls software, libraries etc), Controls Application Support (daemon, interactive, cron, application wrappers etc.)").build(),
                        NewLOVElementDTO.builder().value("CRATE VERIFIER").description("Crate Verifier").build(),
                        NewLOVElementDTO.builder().value("CRYO DISTRIBUTION").description("Cryogenic distribution for the superconducting linac.").build(),
                        NewLOVElementDTO.builder().value("CRYOPLANT AUXILIARY").description("Auxiliary subsystems for the superconducting linac cryoplants.").build(),
                        NewLOVElementDTO.builder().value("CRYOPLANT C1").description("One of the two cryoplants for the superconducting linac.").build(),
                        NewLOVElementDTO.builder().value("CRYOPLANT C2").description("The second of the two cryoplants for the superconducting linac.").build(),
                        NewLOVElementDTO.builder().value("DC MAGNETS").description("DC magnet, power supply or interface chassis").build(),
                        NewLOVElementDTO.builder().value("DOCUMENTATION").description("Documentation").build(),
                        NewLOVElementDTO.builder().value("DUMPS/COLLIMATORS").description("Slits, collimators, dumps, FARCs, masks").build(),
                        NewLOVElementDTO.builder().value("EDM").description("EDM").build(),
                        NewLOVElementDTO.builder().value("ELECTRICITY").description("VVSs, AC/DC wiring, breakers, switches").build(),
                        NewLOVElementDTO.builder().value("EPICS EXTENSIONS").description("EPICS Extensions").build(),
                        NewLOVElementDTO.builder().value("EPICS INFRASTRUCTURE").description("EPICS Infrastructure")        .build(),
                        NewLOVElementDTO.builder().value("EVENT").description("Event").build(),
                        NewLOVElementDTO.builder().value("EXPERIMENTAL").description("Experimantal").build(),
                        NewLOVElementDTO.builder().value("FARADAY CUP").description("Faraday Cup").build(),
                        NewLOVElementDTO.builder().value("FAST FEEDBACK").description("Fast Feedback").build(),
                        NewLOVElementDTO.builder().value("FEEDBACK").description("Other than FAST FEEDBACK").build(),
                        NewLOVElementDTO.builder().value("FIRE ALARMS").description("Smoke detectors, heat sensors, alarms").build(),
                        NewLOVElementDTO.builder().value("GAS").description("Gas").build(),
                        NewLOVElementDTO.builder().value("HELP SYSTEM").description("Help System").build(),
                        NewLOVElementDTO.builder().value("JAVA APPS").description("JAVA Applications").build(),
                        NewLOVElementDTO.builder().value("KICKER").description("DR and FF kickers, flux concentrator, single beam dumpers, scav. line kicker").build(),
                        NewLOVElementDTO.builder().value("KLYSTRON / MODULATOR").description("Includes DR compressors, e+ target klystron").build(),
                        NewLOVElementDTO.builder().value("KNOBS").description("Knob Manager").build(),
                        NewLOVElementDTO.builder().value("LASER").description("LCLS and Photon Science LASER systems.").build(),
                        NewLOVElementDTO.builder().value("MAGNET").description("Magnet").build(),
                        NewLOVElementDTO.builder().value("MATLAB APPS").description("MATLAB Applications").build(),
                        NewLOVElementDTO.builder().value("MICRO").description("Multibus chassis, CPU, MBCD, PNET, modem, KISNET").build(),
                        NewLOVElementDTO.builder().value("MOTION CONTROL").description("Motion Control").build(),
                        NewLOVElementDTO.builder().value("MOVERS").description("Magnet Movers").build(),
                        NewLOVElementDTO.builder().value("MPS").description("MPS, TIU, rate limiting, PIC's PLIC, temperature detectors, VETO").build(),
                        NewLOVElementDTO.builder().value("NETWORK INFRASTRUCTURE").description("Network Infrastructure").build(),
                        NewLOVElementDTO.builder().value("ODM").description("Oxygen Deficiency Monitoring Systems").build(),
                        NewLOVElementDTO.builder().value("OPS SOFTWARE").description("Software written by the operations group.").build(),
                        NewLOVElementDTO.builder().value("OTHER").description("Scopes, non-PPS videos and anything else not in one of the other subsystems").build(),
                        NewLOVElementDTO.builder().value("PDU").description("PDU").build(),
                        NewLOVElementDTO.builder().value("PIOP").description("PIOP").build(),
                        NewLOVElementDTO.builder().value("POWER").description("Power").build(),
                        NewLOVElementDTO.builder().value("PPS").description("Doors/hatches, keybanks, interlocks, stoppers, PPS video").build(),
                        NewLOVElementDTO.builder().value("PROF").description("Profile monitors/screens").build(),
                        NewLOVElementDTO.builder().value("PULSED MAGNETS").description("Pulsed Magnets").build(),
                        NewLOVElementDTO.builder().value("PYDM").description("PyDM (Python Display Manager)").build(),
                        NewLOVElementDTO.builder().value("RADIOS").description("Tunnel and hand-held radios, paging system.").build(),
                        NewLOVElementDTO.builder().value("RDB").description("RDB").build(),
                        NewLOVElementDTO.builder().value("REAL TIME").description("Real Time").build(),
                        NewLOVElementDTO.builder().value("RF").description("PEP II RF, DR RF, PAD's MDL, phase hardware, master source").build(),
                        NewLOVElementDTO.builder().value("RF-Safety").description("NIRP and other RF Safety related system issues.").build(),
                        NewLOVElementDTO.builder().value("SAFETY").description("Any personnel safety item (excluding PPS)").build(),
                        NewLOVElementDTO.builder().value("SMOKE").description("Smoke").build(),
                        NewLOVElementDTO.builder().value("SRF").description("Superconducting RF Systems").build(),
                        NewLOVElementDTO.builder().value("SUBBOOSTER").description("Any sector 0-30 subbooster").build(),
                        NewLOVElementDTO.builder().value("TEMPERATURE").description("Temperature").build(),
                        NewLOVElementDTO.builder().value("TIMING").description("FIDO's, fiducial generators").build(),
                        NewLOVElementDTO.builder().value("TOROIDS").description("toroids").build(),
                        NewLOVElementDTO.builder().value("UNDULATOR").description("Undulator").build(),
                        NewLOVElementDTO.builder().value("VACUUM").description("Guages, valves, pumps and all of their controllers, interlocks, leaks").build(),
                        NewLOVElementDTO.builder().value("VME").description("VME crates, power supplies, VME modules").build(),
                        NewLOVElementDTO.builder().value("VMS").description("VMS").build(),
                        NewLOVElementDTO.builder().value("WATER").description("Cooling towers, pumps, hoses/pipes, temperature regulation, flow switches, interlocks, filters").build(),
                        NewLOVElementDTO.builder().value("WIRES").description("Wire scanners").build()
                )
        );
    }

    /**
     * Initialize the TEC domain
     */
    private void initTECDomain() {
        String domainId = wrapCatch(
                () -> domainService.createNew(
                        NewDomainDTO
                                .builder()
                                .name("TEC")
                                .description("TEC")
                                .build()
                ),
                -1
        );

        createProblemReportWork(domainId);
        createRecordActivityWork(domainId);
        createRequestWork(domainId);
    }

    private void createProblemReportWork(String domainId) {
        var newProblemReportId = domainService.createNew(
                domainId,
                NewWorkTypeDTO.builder()
                        .title("Problem report")
                        .description("It is used to report a problem")
                        .customFields(
                                Collections.emptyList()
                        )
                        .build()
        );
    }

    private void createRecordActivityWork(String domainId) {
        var newRecordActivityId = domainService.createNew(
                domainId,
                NewWorkTypeDTO.builder()
                        .title("Record activity")
                        .description("It is used to record an activity")
                        .customFields(
                                Collections.emptyList()
                        )
                        .build()
        );
    }

    private void createRequestWork(String domainId) {
        var newRequestId = domainService.createNew(
                domainId,
                NewWorkTypeDTO.builder()
                        .title("Request")
                        .description("It is used to request something")
                        .customFields(
                                Collections.emptyList()
                        )
                        .build()
        );
    }
}