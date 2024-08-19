package by.zemich.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;


public class VersioningPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        project.getTasks().create("publishTag", PublishTagTask.class);
    }
}
