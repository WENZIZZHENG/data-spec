package com.dataspec.project;

import com.dataspec.project.entity.Project;
import com.dataspec.project.repository.ProjectRepository;
import com.dataspec.project.service.impl.ProjectServiceImpl;
import com.dataspec.standards.BuiltInStandardsImportService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectServiceImplTest {

    @Test
    void create_importsBuiltInStandardsByDefault() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        BuiltInStandardsImportService standardsImportService = mock(BuiltInStandardsImportService.class);
        when(projectRepository.existsByName("示例项目")).thenReturn(false);
        doAnswer(invocation -> {
            Project inserted = invocation.getArgument(0);
            inserted.setId(7L);
            return 1;
        }).when(projectRepository).insert(any(Project.class));
        ProjectServiceImpl service = new ProjectServiceImpl(projectRepository, standardsImportService);

        Project project = new Project();
        project.setName("示例项目");
        Project created = service.create(project);

        assertEquals(7L, created.getId());
        assertEquals("postgresql", created.getDbType());
        verify(standardsImportService).importBuiltInStandards(7L);
    }

    @Test
    void create_skipsBuiltInStandardsWhenDisabled() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        BuiltInStandardsImportService standardsImportService = mock(BuiltInStandardsImportService.class);
        when(projectRepository.existsByName("空白项目")).thenReturn(false);
        doAnswer(invocation -> {
            Project inserted = invocation.getArgument(0);
            inserted.setId(8L);
            return 1;
        }).when(projectRepository).insert(any(Project.class));
        ProjectServiceImpl service = new ProjectServiceImpl(projectRepository, standardsImportService);

        Project project = new Project();
        project.setName("空白项目");
        service.create(project, false);

        verify(standardsImportService, never()).importBuiltInStandards(anyLong());
    }
}
