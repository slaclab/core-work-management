package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.api.v1.dto.NewDomainDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLOVElementDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkTypeDTO;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.LOVService;
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
@Profile({"init-domain"})
@ChangeUnit(id = "init-lov", order = "1000", author = "bisegni")
public class M1000_InitLOV {
    private final LOVService lovService;
    @Execution
    public void changeSet() {
        initLOV();
    }


    @RollbackExecution
    public void rollback() {}

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

        // subsystem group
        lovService.createNew(
                "SubsystemGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("ACM")
                                .description("ACM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("AIDA")
                                .description("AIDA")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("ALARMS")
                                .description("Alarms")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("ALC CF HVAC")
                                .description("ALC CF HVAC")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("ALIGN DIAG SYS")
                                .description("Align Diag Sys")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("ARCHIVER")
                                .description("Archiver")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("ATCA")
                                .description("ATCA-Based Common Platform hardware and software. Hardware includes: ATCA Crates, ATCA Shelf Managers, ATCA 10G Ethernet Switches, and Industrial Rack-mount Computers. Software includes: linuxRT OS, CPSW API/Library communication layer, IPMI Management Software, and Common Platform Software tools for diagnostics and maintenance.")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BCM")
                                .description("For DEPOT Use")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BCS")
                                .description("Beam Containment System")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BLD")
                                .description("BLD")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BLEN")
                                .description("For DEPOT Use")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BLF")
                                .description("BLF")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BLM")
                                .description("BLM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BPM")
                                .description("BPMs, muxes, processors, strip-line monitors")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CAMAC")
                                .description("crate, power supply, modules, serial link")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CATER")
                                .description("")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CATV")
                                .description("Cable, amplifiers, Sytek, System 20 boxes")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CMLOG")
                                .description("CMLOG")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("COMPRESSED AIR")
                                .description("")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("COMPUTER")
                                .description("SLC/MCC computer systems, ethernet, terminals, workstations, printers etc.")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("COMPUTING INFRASTRUCTURE")
                                .description("Computing Infrastructure")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CONTROLS INFRASTRUCTURE")
                                .description("Controls Production Environment, Data Management, Matlab support (Matlab software and license server), Controls Software Management (Python, Perl, all third party packages required by controls software, libraries etc), Controls Application Support (daemon, interactive, cron, application wrappers etc.)")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CRATE VERIFIER")
                                .description("Crate Verifier")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CRYO DISTRIBUTION")
                                .description("Cryogenic distribution for the superconducting linac.")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CRYOPLANT AUXILIARY")
                                .description("Auxiliary subsystems for the superconducting linac cryoplants.")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CRYOPLANT C1")
                                .description("One of the two cryoplants for the superconducting linac.")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CRYOPLANT C2")
                                .description("The second of the two cryoplants for the superconducting linac.")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("DC MAGNETS")
                                .description("DC magnet, power supply or interface chassis")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("DOCUMENTATION")
                                .description("Documentation")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("DUMPS/COLLIMATORS")
                                .description("Slits, collimators, dumps, FARCs, masks")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("EDM")
                                .description("EDM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("ELECTRICITY")
                                .description("VVSs, AC/DC wiring, breakers, switches")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("EPICS EXTENSIONS")
                                .description("EPICS Extensions")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("EPICS INFRASTRUCTURE")
                                .description("EPICS Infrastructure")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("EVENT")
                                .description("Event")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("EXPERIMENTAL")
                                .description("Experimantal")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FARADAY CUP")
                                .description("Faraday Cup")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FAST FEEDBACK")
                                .description("Fast Feedback")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FEEDBACK")
                                .description("Other than FAST FEEDBACK")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FIRE ALARMS")
                                .description("Smoke detectors, heat sensors, alarms")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("GAS")
                                .description("Gas")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("HELP SYSTEM")
                                .description("Help System")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("JAVA APPS")
                                .description("JAVA Applications")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("KICKER")
                                .description("DR and FF kickers, flux concentrator, single beam dumpers, scav. line kicker")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("KLYSTRON / MODULATOR")
                                .description("Includes DR compressors, e+ target klystron")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("KNOBS")
                                .description("Knob Manager")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LASER")
                                .description("LCLS and Photon Science LASER systems.")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MAGNET")
                                .description("Magnet")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MATLAB APPS")
                                .description("MATLAB Applications")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MICRO")
                                .description("Multibus chassis, CPU, MBCD, PNET, modem, KISNET")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MOTION CONTROL")
                                .description("Motion Control")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MOVERS")
                                .description("Magnet Movers")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MPS")
                                .description("MPS, TIU, rate limiting, PIC's PLIC, temperature detectors, VETO")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("NETWORK INFRASTRUCTURE")
                                .description("Network Infrastructure")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("ODM")
                                .description("Oxygen Deficiency Monitoring Systems")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("OPS SOFTWARE")
                                .description("Software written by the operations group.")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("OTHER")
                                .description("Scopes, non-PPS videos and anything else not in one of the other subsystems")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PDU")
                                .description("PDU")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PIOP")
                                .description("PIOP")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("POWER")
                                .description("Power")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PPS")
                                .description("Doors/hatches, keybanks, interlocks, stoppers, PPS video")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PROF")
                                .description("Profile monitors/screens")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PULSED MAGNETS")
                                .description("Pulsed Magnets")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PYDM")
                                .description("PyDM (Python Display Manager)")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("RADIOS")
                                .description("Tunnel and hand-held radios, paging system.")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("RDB")
                                .description("RDB")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("REAL TIME")
                                .description("Real Time")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("RF")
                                .description("PEP II RF, DR RF, PAD's MDL, phase hardware, master source")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("RF-Safety")
                                .description("NIRP and other RF Safety related system issues.")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("SAFETY")
                                .description("Any personnel safety item (excluding PPS)")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("SMOKE")
                                .description("Smoke")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("SRF")
                                .description("Superconducting RF Systems")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("SUBBOOSTER")
                                .description("Any sector 0-30 subbooster")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("TEMPERATURE")
                                .description("Temperature")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("TIMING")
                                .description("FIDO's, fiducial generators")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("TOROIDS")
                                .description("toroids")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("UNDULATOR")
                                .description("Undulator")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("VACUUM")
                                .description("Guages, valves, pumps and all of their controllers, interlocks, leaks")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("VME")
                                .description("VME crates, power supplies, VME modules")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("VMS")
                                .description("VMS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("WATER")
                                .description("Cooling towers, pumps, hoses/pipes, temperature regulation, flow switches, interlocks, filters")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("WIRES")
                                .description("Wire scanners")
                                .build()
                )
        );

        // project group
        lovService.createNew(
                "ProjectGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("CBXFEL Project")
                                .description("CBXFEL Project")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("COMMON")
                                .description("COMMON")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("DASEL")
                                .description("DASEL")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FACET")
                                .description("FACET")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FACET User Area")
                                .description("FACET User Area")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LCLS")
                                .description("LCLS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LCLS-II")
                                .description("LCLS-II")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LCLS-II HE Project")
                                .description("LCLS-II HE Project")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("OTHER")
                                .description("OTHER")
                                .build()
                )
        );
        // urgency group
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

        // micro group
        lovService.createNew(
                "MicroGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("AB01")
                                .description("AB01")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("AMOO")
                                .description("AMOO")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CBOO")
                                .description("CBOO")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("EP01")
                                .description("EP01")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("EP02")
                                .description("EP02")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("EP05")
                                .description("EP05")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI10")
                                .description("LI10")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI11")
                                .description("LI11")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI12")
                                .description("LI12")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI13")
                                .description("LI13")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI14")
                                .description("LI14")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI15")
                                .description("LI15")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI16")
                                .description("LI16")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI17")
                                .description("LI17")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI18")
                                .description("LI18")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI19")
                                .description("LI19")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI20")
                                .description("LI20")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI21")
                                .description("LI21")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI22")
                                .description("LI22")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("L123")
                                .description("L123")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI24")
                                .description("LI24")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI25")
                                .description("LI25")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI26")
                                .description("LI26")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI27")
                                .description("LI27")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI28")
                                .description("LI28")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("L129")
                                .description("L129")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LI30")
                                .description("LI30")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MC00")
                                .description("MC00")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MPOO")
                                .description("MPOO")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MP01")
                                .description("MP01")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PROO")
                                .description("PROO")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PRO2")
                                .description("PRO2")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PRO3")
                                .description("PRO3")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PRO4")
                                .description("PRO4")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PR06")
                                .description("PR06")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PR08")
                                .description("PR08")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PR10")
                                .description("PR10")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PR12")
                                .description("PR12")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PT01")
                                .description("PT01")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("RFOO")
                                .description("RFOO")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("SPOO")
                                .description("SPOO")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("SP01")
                                .description("SP01")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("TA01")
                                .description("TA01")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("TA02")
                                .description("TA02")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("TA03")
                                .description("TA03")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("TLOO")
                                .description("TLOO")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("TL01")
                                .description("TL01")
                                .build()
                )
        );

        // primary group
        lovService.createNew(
                "PrimaryGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("АССС")
                                .description("АССС")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("AMPL")
                                .description("AMPL")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("ARRY")
                                .description("ARRY")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("ASTS")
                                .description("ASTS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BCS")
                                .description("BCS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BEND")
                                .description("BEND")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BNDS")
                                .description("BNDS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BPMP")
                                .description("BPMP")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BPMS")
                                .description("BPMS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("BTRM")
                                .description("BTRM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CALP")
                                .description("CALP")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CAR")
                                .description("CAR")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CNTS")
                                .description("CNTS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("COLL")
                                .description("COLL")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("CSTR")
                                .description("CSTR")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("DAOC")
                                .description("DAOC")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("DIM")
                                .description("DIM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("DODD")
                                .description("DODD")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("DOM")
                                .description("DOM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("DTIZ")
                                .description("DTIZ")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FBCK")
                                .description("FBCK")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FCOM")
                                .description("FCOM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FELB")
                                .description("FELB")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FMTX")
                                .description("FMTX")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("FPRM")
                                .description("FPRM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("GADC")
                                .description("GADC")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LDIM")
                                .description("LDIM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LGPS")
                                .description("LGPS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("LVDT")
                                .description("LVDT")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MISC")
                                .description("MISC")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MPS")
                                .description("MPS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("MPSC")
                                .description("MPSC")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PAU")
                                .description("PAU")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PDU")
                                .description("PDU")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PICP")
                                .description("PICP")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PICS")
                                .description("PICS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("POLY")
                                .description("POLY")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PPS")
                                .description("PPS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PROF")
                                .description("PROF")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("QUAD")
                                .description("QUAD")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("QUAS")
                                .description("QUAS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("RTDP")
                                .description("RTDP")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("RTDS")
                                .description("RTDS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("SCLR")
                                .description("SCLR")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("STEP")
                                .description("STEP")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("STPR")
                                .description("STPR")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("TORO")
                                .description("TORO")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("TRBR")
                                .description("TRBR")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("TRIG")
                                .description("TRIG")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("VACG")
                                .description("VACG")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("VACS")
                                .description("VACS")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("VACV")
                                .description("VACV")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("WIRE")
                                .description("WIRE")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("WNDW")
                                .description("WNDW")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("XCOR")
                                .description("XCOR")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("XMOV")
                                .description("XMOV")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("YCOR")
                                .description("YCOR")
                                .build()
                )
        );

        // unity group
        lovService.createNew(
                "UnitGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("1")
                                .description("1")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("2")
                                .description("2")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("3")
                                .description("3")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("4")
                                .description("4")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("5")
                                .description("5")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("6")
                                .description("6")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("7")
                                .description("7")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("8")
                                .description("8")
                                .build()
                )
        );

        // build group
        lovService.createNew(
                "BuildingGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("1")
                                .description("1")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("2")
                                .description("2")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("3")
                                .description("3")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("4")
                                .description("4")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("5")
                                .description("5")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("6")
                                .description("6")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("7")
                                .description("7")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("8")
                                .description("8")
                                .build()
                )
        );

        // issue priority group
        lovService.createNew(
                "IssuePriorityGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("1 - Highest")
                                .description("1 - Highest")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("2")
                                .description("2")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("3")
                                .description("3")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("4")
                                .description("4")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("5")
                                .description("5")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("6")
                                .description("6")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("7")
                                .description("7")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("8")
                                .description("8")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("9")
                                .description("9")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("10 - Lowest")
                                .description("10 - Lowest")
                                .build()
                )
        );
        lovService.createNew(
                "SchedulingPriorityGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Benign")
                                .description("Benign")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Downtime")
                                .description("Downtime")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("POMM")
                                .description("POMM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("PAMM")
                                .description("PAMM")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("SML")
                                .description("SML")
                                .build()
                )
        );
        lovService.createNew(
                "AccessRequirementsGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Any Access")
                                .description("Any Access")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Controlled Access")
                                .description("Controlled Access")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("No Access")
                                .description("No Access")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Permitted Access")
                                .description("Permitted Access")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Restricted Access")
                                .description("Restricted Access")
                                .build()
                )
        );

        lovService.createNew(
                "PPSZoneGroup",
                List.of(
                        NewLOVElementDTO.builder().value("ANY").description("Unknown").build(),
                        NewLOVElementDTO.builder().value("All").description("Unknown").build(),
                        NewLOVElementDTO.builder().value("BDE").description("Beam Dump East").build(),
                        NewLOVElementDTO.builder().value("BSY and BTHW").description("Beam Switch Yard").build(),
                        NewLOVElementDTO.builder().value("BTH (LTU and UND)").description("This Zone includes the Linac To Undulator (LTU) and the UND").build(),
                        NewLOVElementDTO.builder().value("CID").description("CID").build(),
                        NewLOVElementDTO.builder().value("DMP (EBD)").description("Electron Beam Dump is East of the BTH PPS zone and West of the Front End Enclosure (FEE) PPS zone").build(),
                        NewLOVElementDTO.builder().value("DRIP").description("DRIP").build(),
                        NewLOVElementDTO.builder().value("E+ Vault").description("Positron Vault").build(),
                        NewLOVElementDTO.builder().value("EBD-FEE").description("Electron Beam Dump - Front End Enclosure").build(),
                        NewLOVElementDTO.builder().value("ESA").description("End Station A").build(),
                        NewLOVElementDTO.builder().value("FEE").description("Front End Enclosure").build(),
                        NewLOVElementDTO.builder().value("FEH").description("Far Experimental Hall B999 (XCS CXI MEC)").build(),
                        NewLOVElementDTO.builder().value("FFTB").description("FFTB").build(),
                        NewLOVElementDTO.builder().value("GTL").description("Gun Test Lab B006").build(),
                        NewLOVElementDTO.builder().value("HX-2").description("The HX-2 (Heat Exchanger 2) enclosure").build(),
                        NewLOVElementDTO.builder().value("Injector West").description("LCLS2 Injector PPS Zone").build(),
                        NewLOVElementDTO.builder().value("LCLS-INJ").description("LCLS injector vault at sector 20").build(),
                        NewLOVElementDTO.builder().value("LI00/LI01").description("Linac Sector 00 & 01").build(),
                        NewLOVElementDTO.builder().value("LI01-LI07").description("Linac West Sectors").build(),
                        NewLOVElementDTO.builder().value("LI02/LI03").description("Linac Sector 02 & 03").build(),
                        NewLOVElementDTO.builder().value("LI04/LI05").description("Linac Sector 04 & 05").build(),
                        NewLOVElementDTO.builder().value("LI04/Li07").description("Unknown").build(),
                        NewLOVElementDTO.builder().value("LI06/LI07").description("Linac Sector 06 & 07").build(),
                        NewLOVElementDTO.builder().value("LI08-LI10A").description("Linac West Sectors").build(),
                        NewLOVElementDTO.builder().value("LI08/LI09").description("Linac Sector 08 & 09").build(),
                        NewLOVElementDTO.builder().value("LI10/LI11").description("Linac Sector 10 & 11").build(),
                        NewLOVElementDTO.builder().value("LI12/LI13").description("Linac Sector 12 & 13").build(),
                        NewLOVElementDTO.builder().value("LI14/LI15").description("Linac Sector 14 & 15").build(),
                        NewLOVElementDTO.builder().value("LI16/17/18").description("Unknown").build(),
                        NewLOVElementDTO.builder().value("LI16/LI17").description("Linac Sector 16 & 17").build(),
                        NewLOVElementDTO.builder().value("LI18").description("New zone").build(),
                        NewLOVElementDTO.builder().value("LI18/LI19").description("Linac Sector 18 & 19").build(),
                        NewLOVElementDTO.builder().value("LI19/LI20").description("New Zone").build(),
                        NewLOVElementDTO.builder().value("LI20/LI21").description("Linac Sector 20 & 21").build(),
                        NewLOVElementDTO.builder().value("LI20/LI25").description("Unknown").build(),
                        NewLOVElementDTO.builder().value("LI21").description("New zone").build(),
                        NewLOVElementDTO.builder().value("LI21-LI23").description("New Linac East PPS Zone 21-23").build(),
                        NewLOVElementDTO.builder().value("LI22/LI23").description("Linac Sector 22 & 23").build(),
                        NewLOVElementDTO.builder().value("LI24/LI25").description("Linac Sector 24 & 25").build(),
                        NewLOVElementDTO.builder().value("LI26-LI29").description("Linac East Sectors 26, 27, 28, 29").build(),
                        NewLOVElementDTO.builder().value("LI26/LI27").description("Linac Sector 26 & 27").build(),
                        NewLOVElementDTO.builder().value("LI28/LI29").description("Linac Sector 28 & 29").build(),
                        NewLOVElementDTO.builder().value("LI30").description("Linac Sector 30").build(),
                        NewLOVElementDTO.builder().value("NA").description("Unknown").build(),
                        NewLOVElementDTO.builder().value("NARC").description("North SLC Arc").build(),
                        NewLOVElementDTO.builder().value("NDR").description("North Damping Ring").build(),
                        NewLOVElementDTO.builder().value("NEH").description("Near Experimental Hall/Cup B950 (AMO SXR XPP)").build(),
                        NewLOVElementDTO.builder().value("NFF").description("North Final Focus").build(),
                        NewLOVElementDTO.builder().value("NIT").description("NIT").build(),
                        NewLOVElementDTO.builder().value("PEP Z02").description("PEP Zone 02").build(),
                        NewLOVElementDTO.builder().value("PEP Z04").description("PEP Zone 04").build(),
                        NewLOVElementDTO.builder().value("PEP Z08").description("PEP II ring, PPS Zone 8").build(),
                        NewLOVElementDTO.builder().value("PEP Z08/12").description("Unknown").build(),
                        NewLOVElementDTO.builder().value("PEP Z12").description("PEP Zone 12").build(),
                        NewLOVElementDTO.builder().value("Research Yard").description("Research Yard Fenced Area").build(),
                        NewLOVElementDTO.builder().value("S10 Injector").description("FACET2 Injector").build(),
                        NewLOVElementDTO.builder().value("SARC").description("South SLC Arc").build(),
                        NewLOVElementDTO.builder().value("SDR").description("South Damping Ring").build(),
                        NewLOVElementDTO.builder().value("SFF").description("South Final Focus").build(),
                        NewLOVElementDTO.builder().value("SIT").description("SIT").build(),
                        NewLOVElementDTO.builder().value("XRT").description("X-ray Transport Tunnel B960").build(),
                        NewLOVElementDTO.builder().value("li06-li09").description("Unknown").build()
                )
        );

        lovService.createNew(
                "SolutionTypeGroup",
                List.of(
                        NewLOVElementDTO.builder()
                                .value("Bug Fix")
                                .description("Bug Fix")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Database")
                                .description("Database")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Design")
                                .description("Design")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Duplicate")
                                .description("Duplicate")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Enhancement")
                                .description("Enhancement")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("No Trouble Found")
                                .description("No Trouble Found")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Other")
                                .description("Other")
                                .build(),
                        NewLOVElementDTO.builder()
                                .value("Panel")
                                .description("Panel")
                                .build()
                )
        );
    }
}