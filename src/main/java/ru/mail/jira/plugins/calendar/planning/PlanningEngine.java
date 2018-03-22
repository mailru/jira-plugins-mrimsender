package ru.mail.jira.plugins.calendar.planning;

import com.atlassian.jira.util.lang.Pair;
import org.joda.time.LocalDate;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.calendar.rest.dto.UserDto;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlanningEngine {
    private final Logger logger = LoggerFactory.getLogger(PlanningEngine.class);
    private static final BigDecimal PROGRESS_START_MARGIN = new BigDecimal(0 + 0.001);
    private static final BigDecimal PROGRESS_END_MARGIN = new BigDecimal(1 - 0.001);

    /**
     * @param issues
     * @param issueDuration  issue duration in hours
     * @param issueDeadline  issue deadline in days from current day
     * @param dependenceList issue depends list. Issues in list must done faster.
     * @param issuePriority  issue priorities in double
     * @param numDays        planning period
     * @param maxDayCapacity max working hours in day
     * @return
     */
    public Map<EventDto, Pair<Date, Date>> generatePlan(List<EventDto> issues,
                                                        Map<EventDto, Integer> issueDuration,
                                                        Map<EventDto, Integer> issueDeadline,
                                                        Map<EventDto, List<EventDto>> dependenceList,
                                                        Map<EventDto, Double> issuePriority,
                                                        int numDays,
                                                        int maxDayCapacity) {

        int numTasks = issues.size();
        Map<EventDto, Integer> issueIndex = new HashMap<>();
        Map<Integer, EventDto> indexIssue = new HashMap<>();
        int idx = 0;
        for (EventDto issue : issues) {
            issueIndex.put(issue, idx);
            indexIssue.put(idx, issue);
            idx++;
        }

        final Map<UserDto, List<EventDto>> userIssueGroup = new HashMap<>();
        issues.forEach(issue -> userIssueGroup.computeIfAbsent(issue.getAssignee(), k -> new ArrayList<>())
                                              .add(issue));

        final Variable[] progressVars = new Variable[numTasks * numDays];// matrix[i][j] = array[i * m + j], 0 <= i < n, 0 <= j <m

        // main function
        if (issuePriority.size() == 0) {
            for (int i = 0; i < numTasks; i++) {
                for (int j = 0; j < numDays; j++) {

                    progressVars[i * numDays + j] = Variable.make(String.format("x_%d_%d", i, j))
                                                            .weight(1)
                                                            .lower(0)
                                                            .upper(1);
                    if (j == 0)
                        progressVars[i * numDays + j].level(0);
                }
            }
        } else {
            for (int i = 0; i < numTasks; i++) {
                EventDto issue = indexIssue.get(i * numDays);
                Double priority = issuePriority.get(issue);
                for (int j = 0; j < numDays; j++) {
                    if (priority != null) {
                        progressVars[i * numDays + j] = Variable.make(String.format("x_%d_%d", i, j))
                                                                .weight(priority)
                                                                .lower(0)
                                                                .upper(1);
                    } else {
                        progressVars[i * numDays + j] = Variable.make(String.format("x_%d_%d", i, j))
                                                                .weight(1)
                                                                .lower(0)
                                                                .upper(1);
                    }
                    if (j == 0)
                        progressVars[i * numDays + j].level(0);
                }
            }
        }

        final ExpressionsBasedModel model = new ExpressionsBasedModel(progressVars);

        // progress constraints
        for (int i = 0; i < numTasks; i++) {
            for (int j = 0; j < numDays - 1; j++) {
                // progress_constraints.append(0 <= progress[task, day + 1] - progress[task, day])
                model.addExpression(String.format("progress_continuous_%d_%d", i, j))
                     .set(progressVars[i * numDays + j + 1], 1)
                     .set(progressVars[i * numDays + j], -1)
                     .lower(0);
            }
        }

        // dependence constraints
        for (Map.Entry<EventDto, List<EventDto>> dep : dependenceList.entrySet()) {
            EventDto issue = dep.getKey();
            List<EventDto> issueDepList = dep.getValue();

            int taskIdx = issueIndex.get(issue);
            // dependence_constraints.append(progress[task, 0] == 0)
            model.addExpression("dependence_start" + issue.getIssueKey())
                 .set(progressVars[taskIdx * numDays], 1)
                 .level(0);

            for (EventDto dependantIssue : issueDepList) {
                if (!issueIndex.containsKey(dependantIssue))
                    continue;
                int dependantIssueIdx = issueIndex.get(dependantIssue);

                for (int j = 1; j < numDays; j++) {
                    //  dependence_constraints.append(progress[task_dep, j - 1] - progress[task, j] >= 0)
                    model.addExpression(String.format("dependence_continuous_%s_%s_%d", issue.getId(), dependantIssue.getId(), j))
                         .set(progressVars[dependantIssueIdx * numDays + j - 1], 1)
                         .set(progressVars[taskIdx * numDays + j], -1)
                         .lower(0);
                    model.addExpression(String.format("dependence_day_%s_%s_%d", issue.getId(), dependantIssue.getId(), j))
                        .set(progressVars[dependantIssueIdx * numDays + j], 1)
                        .set(progressVars[taskIdx * numDays + j], -1)
                        .lower(0);
                }
            }
        }

        // deadlines
        for (Map.Entry<EventDto, Integer> task : issueDeadline.entrySet()) {
            // deadlines_constraint.append(progress[task_index[task], task_deadline[task]] == 1)
            model.addExpression("deadline_" + task.getKey())
                 .set(progressVars[issueIndex.get(task.getKey()) * numDays + task.getValue()], 1)
                 .level(1);
        }

        // hours
        for (int day = 1; day < numDays; day++) {
            for (Map.Entry<UserDto, List<EventDto>> e : userIssueGroup.entrySet()) {
                // constr = sum((progress[task_index[task], day] - progress[task_index[task], day - 1]
                //              ) * task_duration[task]
                //                for task in task_group) <= max_day_capacity

                UserDto user = e.getKey();
                List<EventDto> issueGroup = e.getValue();
                Expression expression = model.addExpression(String.format("work_capacity_%d_%s", day, user));

                for (EventDto issue : issueGroup) {
                    int taskIdx = issueIndex.get(issue);
                    int duration = issueDuration.get(issue);
                    expression.set(progressVars[taskIdx * numDays + day], duration)
                              .set(progressVars[taskIdx * numDays + day - 1], -duration)
                              .upper(maxDayCapacity);


                    /*
                    Expression expression2 = model.addExpression(String.format("work_capacity_%d_%s_%s_min", day, user, issue.getId()));

                    expression2
                        .set(progressVars[taskIdx * numDays + day], duration)
                        .set(progressVars[taskIdx * numDays + day - 1], -duration)
                        .lower(1)
                        .upper(2);*/
                }
            }
        }

        Map<EventDto, Pair<Date, Date>> plan = new HashMap<>();
        logger.debug("maximizing");
        Optimisation.Result result = model.maximise();
        logger.debug("maximized");

        logger.debug("result state {}", result.getState());

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(2);
        decimalFormat.setMinimumFractionDigits(0);

        LocalDate today = LocalDate.now();
        for (int i = 0; i < numTasks; i++) {
            EventDto issue = indexIssue.get(i);
            Integer duration = issueDuration.get(issue);

            StringBuilder sb = new StringBuilder();
            sb.append(issue.getId());
            sb.append('\t');
            sb.append(issue.getAssignee() != null ? issue.getAssignee().getKey() : "");
            sb.append('\t');

            Date planStart = null;
            Date planEnd = null;
            for (int j = 0; j < numDays; j++) {
                BigDecimal progress = result.get(i * numDays + j);

                BigDecimal progressDiff = progress;
                if (j > 0) {
                    progressDiff = progress.subtract(result.get(i * numDays + j - 1));
                }

                sb.append("| ");
                sb.append(decimalFormat.format(progressDiff.multiply(new BigDecimal(duration))));
                sb.append(" \t");

                if (planStart == null && progress.compareTo(PROGRESS_START_MARGIN) > 0) { // start date
                    planStart = today.plusDays(j).toDate();
                }
                if (progress.compareTo(PROGRESS_END_MARGIN) >= 0) { // end date
                    planEnd = today.plusDays(j+1).toDate();
                    break;
                }
            }

            logger.debug(sb.toString());

            plan.put(issue, Pair.nicePairOf(planStart, planEnd));
        }

        return plan;
    }
}
