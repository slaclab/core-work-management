package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.mapper.WorkMapper;
import edu.stanford.slac.core_work_management.exception.WorkNotFound;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
public class WorkService {
    WorkMapper workMapper;
    WorkRepository workRepository;
    WorkTypeRepository workTypeRepository;

    /**
     * Create a new work type
     * @param newWorkTypeDTO the DTO to create the work type
     * @return the id of the created work type
     */
    public String ensureWorkType(NewWorkTypeDTO newWorkTypeDTO) {
        return wrapCatch(
                () -> workTypeRepository.save(
                        workMapper.toModel(newWorkTypeDTO)
                ),
                -1
        ).getId();
    }

    /**
     * Return all the work types
     * @return the list of work types
     */
    public List<WorkTypeDTO> findAllWorkTypes() {
        var workTypeList = wrapCatch(
                () -> workTypeRepository.findAll(),
                -1
        );
        return workTypeList.stream().map(workMapper::toDTO).toList();
    }

    /**
     * Create a new work
     * @param newWorkDTO the DTO to create the work
     * @return the id of the created work
     */
    public String createNew(NewWorkDTO newWorkDTO) {
        Work workToSave = workMapper.toModel(newWorkDTO);
        Work savedWork = wrapCatch(
                () -> workRepository.save(workToSave),
                -1
        );
        return savedWork.getId();
    }

    /**
     * Return the work by his id
     * @param id the id of the work
     * @return the work
     */
    public WorkDTO findWorkById(String id) {
        return wrapCatch(
                () -> workRepository.findById(id).map(workMapper::toDTO).orElseThrow(
                        ()-> WorkNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .workId(id)
                                .build()
                ),
                -1
        );
    }
}
