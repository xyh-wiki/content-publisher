package io.contentpublisher.platform.infrastructure.persistence;

import io.contentpublisher.platform.application.port.ProjectRepository;
import io.contentpublisher.platform.application.port.RepositorySnapshotStore;
import io.contentpublisher.platform.domain.Project;
import io.contentpublisher.platform.domain.RepositorySnapshot;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class JpaProjectPersistenceAdapter implements ProjectRepository, RepositorySnapshotStore {
    private final ProjectJpaRepository projects;
    private final SnapshotJpaRepository snapshots;
    private final JpaDomainMapper mapper;

    public JpaProjectPersistenceAdapter(ProjectJpaRepository projects, SnapshotJpaRepository snapshots,
                                        JpaDomainMapper mapper) {
        this.projects = projects;
        this.snapshots = snapshots;
        this.mapper = mapper;
    }

    @Override
    public Project save(Project project) {
        ProjectEntity entity = new ProjectEntity();
        entity.id = project.id(); entity.tenantId = project.tenantId(); entity.gitUrl = project.gitUrl();
        entity.name = project.name(); entity.description = project.description();
        entity.defaultBranch = project.defaultBranch(); entity.revision = project.revision();
        entity.languagesJson = mapper.stringsJson(project.languages()); entity.license = project.license();
        entity.status = project.status().name(); entity.createdBy = project.createdBy();
        entity.updatedBy = project.updatedBy(); entity.createdAt = project.createdAt();
        entity.updatedAt = project.updatedAt();
        return mapper.project(projects.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findProjectById(String tenantId, UUID id) {
        return projects.findByTenantIdAndId(tenantId, id).map(mapper::project);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findByGitUrl(String tenantId, String gitUrl) {
        return projects.findByTenantIdAndGitUrl(tenantId, gitUrl).map(mapper::project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findRecentProjects(String tenantId, int limit) {
        return projects.findByTenantIdOrderByUpdatedAtDesc(tenantId, PageRequest.of(0, limit)).stream()
                .map(mapper::project).toList();
    }

    @Override
    public void save(String tenantId, UUID projectId, RepositorySnapshot snapshot) {
        SnapshotEntity entity = new SnapshotEntity();
        entity.projectId = projectId; entity.tenantId = tenantId; entity.name = snapshot.name();
        entity.description = snapshot.description(); entity.defaultBranch = snapshot.defaultBranch();
        entity.revision = snapshot.revision(); entity.readme = snapshot.readme();
        entity.manifestSummary = snapshot.manifestSummary();
        entity.fileTreeJson = mapper.stringsJson(snapshot.fileTree());
        entity.languagesJson = mapper.stringsJson(snapshot.languages()); entity.license = snapshot.license();
        snapshots.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RepositorySnapshot> findByProjectId(String tenantId, UUID projectId) {
        return snapshots.findByTenantIdAndProjectId(tenantId, projectId).map(entity ->
                new RepositorySnapshot(entity.name, entity.description, entity.defaultBranch, entity.revision,
                        entity.readme, entity.manifestSummary, mapper.strings(entity.fileTreeJson),
                        mapper.strings(entity.languagesJson), entity.license));
    }
}
