/*
Kube Helper
Copyright (C) 2021 JDev

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.kubehelper.services;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.io.Files;
import com.kubehelper.common.Global;
import com.kubehelper.domain.models.CronJobsModel;
import com.kubehelper.domain.results.CommandsResult;
import com.kubehelper.domain.results.CronJobResult;
import com.kubehelper.domain.results.FileSourceResult;
import com.moandjiezana.toml.Toml;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author JDev
 */
@Service
public class CronJobsService {

    private static Logger logger = LoggerFactory.getLogger(CronJobsService.class);
    private String reportEntryTemplate;

    private KubernetesClient fabric8Client = new DefaultKubernetesClient();

    @Value("${kubehelper.predefined.commanmds.path}")
    private String predefinedCommandsPath;

    @Value("${kubehelper.user.commands.location.search.path}")
    private String userCommandsLocationSearchPath;

    @Value("${kubehelper.cron.jobs.reports.path}")
    private String cronJobsReportsPath;

    @Autowired
    private CommonService commonService;

    @Autowired
    private SchedulerService schedulerService;


    @PostConstruct
    private void postConstruct() {
    }

    /**
     * Creates cron job and checks if folder with cron job name exists.
     *
     * @param model - @{@link CronJobsModel}
     * @param job   - @{@link CronJobResult}
     */
    public void startCronJob(CronJobsModel model, CronJobResult job) {
        job.buildReportsFolderPath(cronJobsReportsPath);

        File reportsFolder = new File(job.getReportsFolderPath());
        if (reportsFolder.exists() && reportsFolder.isDirectory()) {
            model.addException(new RuntimeException("Cron job with this name already exists or existed. Please choose another name."));
            return;
        }
        schedulerService.startCronJob(job);
    }

    public void rerunCronJob(CronJobResult job) {
        schedulerService.rerunCronJob(job);
    }

    public List<CronJobResult> getActiveCronJobs() {
        List<CronJobResult> activeCronJobs = new ArrayList<>();
        Global.CRON_JOBS.forEach((jobName, scheduler) -> {
            CronJobResult jobResult = new CronJobResult(scheduler.getId())
                    .setName(jobName)
                    .setCommand(scheduler.getCommand())
                    .setExpression(scheduler.getExpression())
                    .setDescription(scheduler.getDescription())
                    .setEmail(scheduler.getEmail())
                    .setShell(scheduler.getShell())
                    .setRuns(scheduler.getRuns())
                    .setDone(scheduler.isDone());
            activeCronJobs.add(jobResult);
        });
        return activeCronJobs;
    }

    /**
     * Reads toml commands resources from init/commands and parse commands.
     *
     * @param cronJobsModel - commands model.
     */
    public void parsePredefinedCommands(CronJobsModel cronJobsModel) {
        HashMap<String, Toml> commands = new HashMap<>();
        try {
            org.springframework.core.io.Resource[] resources = commonService.getFilesPathsFromClasspathByDirAndExtension(predefinedCommandsPath, ".toml");
            for (org.springframework.core.io.Resource resource : resources) {
                commands.put(Files.getNameWithoutExtension(resource.getFilename()), new Toml().read(resource.getInputStream()));
            }
        } catch (IOException e) {
            cronJobsModel.addException("Error at parse predefined commands." + e.getMessage(), e);
            logger.error(e.getMessage(), e);
        }
        parseCommands(cronJobsModel, commands);
    }

    /**
     * Reads toml commands resources from git folder and parse commands.
     *
     * @param cronJobsModel- commands model.
     */
    public void parseUserCommands(CronJobsModel cronJobsModel) {
        HashMap<String, Toml> commands = new HashMap<>();
        try {
            Set<String> userCommandFiles = commonService.getFilesPathsByDirAndExtension(userCommandsLocationSearchPath, 10, ".toml");
            for (String filePath : userCommandFiles) {
                commands.put(Files.getNameWithoutExtension(filePath), new Toml().read(commonService.getResourceAsStringByPath(filePath)));
            }
        } catch (IOException e) {
            cronJobsModel.addException("Error at parse user commands." + e.getMessage(), e);
            logger.error(e.getMessage(), e);
        }
        parseCommands(cronJobsModel, commands);
    }


    /**
     * Writes execution result output to file.
     *
     * @param cronJobsModel - commands model.
     */
    private void writeCommandExecutionToHistory(CronJobsModel cronJobsModel) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        File file = new File(cronJobsReportsPath + today + ".txt");
        try {
            file.createNewFile();
            String composedHistoryEntry = new StringSubstitutor(buildHistoryEntry(cronJobsModel)).replace(reportEntryTemplate);
            String fileContent = commonService.getResourceAsStringByPath(file.getPath());
            fileContent = composedHistoryEntry + fileContent;
            FileUtils.writeStringToFile(file, fileContent, StandardCharsets.UTF_8.toString());
        } catch (IOException e) {
            cronJobsModel.addNotificationException("Cannot write command to execution: Error." + e.getMessage());
            logger.debug(e.getMessage(), e);
        }
    }

    /**
     * Builds history entry from commands model for replacement in template.
     *
     * @param model - commands model.
     * @return - map with history entry
     */
    private Map<String, String> buildHistoryEntry(CronJobsModel model) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss yyyy-MM-dd"));
        return Map.of("time", time, "command", model.getCommandToExecute(), "output", model.getExecutedCommandOutput());
    }


    /**
     * Parse predefined commands from commands map..
     *
     * @param cronJobsModel - commands Model.
     */
    public void parseCommands(CronJobsModel cronJobsModel, HashMap<String, Toml> commands) {
        for (Map.Entry<String, Toml> commandsMap : commands.entrySet()) {
            for (Map.Entry<String, Object> commandEntry : commandsMap.getValue().entrySet()) {
                CommandsResult cr = new CommandsResult(cronJobsModel.getCommandsResults().size() + 1);
                try {
                    Toml command = (Toml) commandEntry.getValue();
                    cr.setFile(commandsMap.getKey())
                            .setName(commandEntry.getKey())
                            .setGroup(command.getString("group"))
                            .setDescription(command.getString("description"))
                            .setCommand(command.getString("command"));
                    cronJobsModel.addCommandResult(cr);
                } catch (RuntimeException e) {
                    cronJobsModel.addException(new RuntimeException("Command parse Error. Name, Group, Description and Command itself are mandatory fields. Object: " + cr.toString()));
                    logger.error("Command parse Error. Group, Operation Description and command itself are mandatory fields. Object: " + cr.toString());
                }
            }
        }
    }


    //  COMMANDS HISTORY ================

    /**
     * Prepares commands history view. Finds all history files and set active newest.
     *
     * @param cronJobsModel - commands model.
     */
    public void prepareCronJobsReports(CronJobsModel cronJobsModel) {
        try {

            List<File> reportsGroups = Arrays.stream(new File(cronJobsReportsPath).listFiles()).filter(File::isDirectory).sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());
            cronJobsModel.setCronJobsReports(new HashMap<>());
            for (File group : reportsGroups) {
                Set<String> filesPathsByDirAndExtension = commonService.getFilesPathsByDirAndExtension(group.getAbsolutePath(), 2, ".txt");
                filesPathsByDirAndExtension.forEach(file -> {
                    cronJobsModel.addReportSource(Files.getNameWithoutExtension(file), file, group.getName());
                });
            }
            cronJobsModel.sortCronJobsReportsAlphabeticallyAsc();
            Optional<Map.Entry<String, Map<String, FileSourceResult>>> first = cronJobsModel.getCronJobsReports().entrySet().stream().findFirst();
            if (first.isPresent()) {
                cronJobsModel.setSelectedReportRaw(commonService.getResourceAsStringByPath(first.get().getValue().values().stream().findFirst().get().getFilePath()));
                cronJobsModel.setSelectedReportLabel(first.get().getValue().keySet().stream().findFirst().get());
                cronJobsModel.setSelectedReportsFolder(first.get().getKey());
            }
        } catch (IOException e) {
            cronJobsModel.addNotificationException("Cannot Prepare Commands for History: Error." + e.getMessage());
            logger.debug(e.getMessage(), e);
        }
    }

//    changeReportsFolder

    /**
     * Shows history for ranges. From Week/Month/Year to today. Default case for single day.
     *
     * @param cm - commands model.
     */
    public void changeHistoryRaw(CronJobsModel cm) {
        LocalDate today = LocalDate.now();
//        switch (cm.getSelectedCommandsHistoryRange()) {
//            case "This Week" -> showHistoryFor(cm, today.with(TemporalAdjusters.previous(DayOfWeek.MONDAY)), today);
//            case "This Month" -> showHistoryFor(cm, today.with(TemporalAdjusters.firstDayOfMonth()), today);
//            case "This Year" -> showHistoryFor(cm, today.with(TemporalAdjusters.firstDayOfYear()), today);
//            case "All" -> showHistoryFor(cm, LocalDate.now().minusMonths(200), today);
//            default -> cm.setSelectedCommandsHistoryRaw(commonService.getResourceAsStringByPath(cm.getCommandsHistories().get(cm.getSelectedCommandsHistoryLabel()).getFilePath()));
//        }
    }

    /**
     * Calculates, which history to show depending on the selected period.
     *
     * @param cm   - commands model.
     * @param from - from date.
     * @param to   - to date.
     */
    private void showHistoryFor(CronJobsModel cm, LocalDate from, LocalDate to) {
        StringBuilder history = new StringBuilder();
        String historyDayHeader = "++++++++++++++++++++++++++++++++++++++++=== %s ===++++++++++++++++++++++++++++++++++++++++\n\n\n";
        Set<String> filesPathsByDirAndExtension = new HashSet<>();

        //get all history files
        try {
            filesPathsByDirAndExtension = commonService.getFilesPathsByDirAndExtension(cronJobsReportsPath, 2, ".txt");
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }

        //get dates range and filter files paths depends of range
        List<LocalDate> datesRange = from.datesUntil(to.plusDays(1)).collect(Collectors.toList());
        Set<String> datesInRange = filesPathsByDirAndExtension.stream()
                .filter(filePath -> datesRange.contains(LocalDate.parse(Files.getNameWithoutExtension(filePath))))
                .collect(Collectors.toSet());

        //sort history DESC
        ImmutableSortedSet<String> sortedDatesInRangeFilePaths = ImmutableSortedSet.copyOf(
                Comparator.comparing(filePath -> LocalDate.parse(Files.getNameWithoutExtension(filePath), DateTimeFormatter.ISO_LOCAL_DATE), Comparator.reverseOrder()), datesInRange);

        //add filtered history to history string builder
        for (String filePath : sortedDatesInRangeFilePaths) {
            history.append(String.format(historyDayHeader, Files.getNameWithoutExtension(filePath))).append(commonService.getResourceAsStringByPath(filePath)).append("\n");
        }
        cm.setSelectedReportRaw(history.toString());
    }
}
