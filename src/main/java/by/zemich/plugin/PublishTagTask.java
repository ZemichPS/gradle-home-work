package by.zemich.plugin;

import groovy.lang.Closure;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PublishTagTask extends DefaultTask {

    public PublishTagTask() {
        setGroup("Versioning");
        setDescription("Publishes a git tag based on the current branch and versioning strategy.");
    }

    @TaskAction
    public void publishTag() throws IOException {

        String branchName = findBranchName();
        String latestTag = findLatestTag();

        printInfoToConsole("Current branch: " + branchName);

        if (hasUncommittedChanges()) {
            printInfoToConsole("Uncommitted changes found. Tagging as " + latestTag + ".uncommitted");
            return;
        }

        if (hasNoTagAlreadyForCurrentState() || hasNoAnyTag()) {
            String newVersion = determineNewVersion(branchName, latestTag);
            addTag(newVersion);
            printInfoToConsole("New tag added: " + newVersion);
        }
        doPush(branchName);
    }

    private String determineNewVersion(String branch, String latestTag) throws IOException {

        int major = 0;
        int minor = 0;
        latestTag = latestTag
                .replace("v", "")
                .replace("-SNAPSHOT", "");

        if (!hasNoAnyTag()) {
            String[] versionParts = latestTag.split("\\.");
            major = Integer.parseInt(versionParts[0]);
            minor = Integer.parseInt(versionParts[1]);
        }

        switch (branch) {
            case "dev":
            case "qa":
                minor += 1;
                return "v" + major + "." + minor;
            case "stage":
                minor += 1;
                return "v" + major + "." + minor + "-rc";
            case "master":
                major += 1;
                return "v" + major + ".0";
            default:
                return latestTag + "-SNAPSHOT";
        }
    }

    private boolean isMergeCommit() throws IOException {
        String latestHashCommit = findCurrentHash();
        String command = "git rev-list --parents -n 1 " + latestHashCommit;
        return executeCommand(command)
                .trim()
                .split(" ")
                .length > 1;

    }


    private String findHashByTag(String latestTag) throws IOException {
        String command = "git rev-parse " + latestTag;
        return executeCommand(command);
    }

    private String findCurrentHash() throws IOException {
        String command = "git rev-parse HEAD";
        return executeCommand(command);
    }

    private boolean hasNoTagAlreadyForCurrentState() throws IOException {
        String command = "git describe --exact-match --tags";
        return executeCommand(command)
                .toLowerCase()
                .contains("no tag exactly");
    }

    private boolean hasNoAnyTag() throws IOException {
        String command = "git tag";
        return executeCommand(command).isEmpty();
    }

    private void doPush(String branchName) throws IOException {
        String command = "git push origin " + branchName;
        System.out.println("---------------------------------");
        System.out.println("try to push");
        System.out.println("Command: " + command);
        System.out.println("Result: " + executeCommand(command));
        System.out.println("---------------------------------");
    }

    private void addTag(String tagName) throws IOException {
        String command = "git tag " + tagName;
        printInfoToConsole("try to add tag");
        System.out.println("Execute command " + command + ":" + executeCommand(command));
    }

    private String findLatestTag() throws IOException {
        String command = "git describe --tags --abbrev=0";
        return executeCommand(command);
    }

    private String findBranchName() throws IOException {
        String command = "git rev-parse --abbrev-ref HEAD";
        return executeCommand(command).trim();
    }

    private boolean hasUncommittedChanges() throws IOException {
        String command = "git status";

        return executeCommand(command)
                .trim()
                .toLowerCase()
                .contains("changes to be committed");
    }


    private String executeCommand(String command) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Closure<Void> execClosure = new Closure<Void>(this, this) {
                public void doCall(ExecSpec execSpec) {
                    execSpec.commandLine(command.split(" "));
                    execSpec.setStandardOutput(outputStream);
                    execSpec.setErrorOutput(outputStream);
                    execSpec.setIgnoreExitValue(true);
                }
            };

            getProject().exec(execClosure);
            return outputStream.toString().trim();
        }
    }

    private void printInfoToConsole(String message) {
        System.out.println("-----------------------");
        System.out.println(message);
        System.out.println("-----------------------");
    }


}

