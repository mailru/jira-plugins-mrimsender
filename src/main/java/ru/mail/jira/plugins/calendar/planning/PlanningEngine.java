package ru.mail.jira.plugins.calendar.planning;

import com.atlassian.jira.util.lang.Pair;
import org.jacop.constraints.Max;
import org.jacop.constraints.XlteqC;
import org.jacop.constraints.XlteqY;
import org.jacop.constraints.XplusCeqZ;
import org.jacop.constraints.XplusClteqZ;
import org.jacop.constraints.diffn.Nooverlap;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainMin;
import org.jacop.search.Search;
import org.jacop.search.SelectChoicePoint;
import org.jacop.search.SmallestMin;
import org.jacop.search.SplitSelect;
import org.joda.time.DateTime;
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
import java.util.Arrays;
import java.util.Comparator;
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

        int lastDay = numDays - 1;
        // main function
        if (issuePriority.size() == 0) {
            for (int i = 0; i < numTasks; i++) {
                for (int j = 0; j < numDays; j++) {

                    progressVars[i * numDays + j] = Variable.make(String.format("x_%d_%d", i, j))
                                                            .weight(1)
                                                            .lower(0)
                                                            .upper(1);
                    if (j == 0) {
                        progressVars[i * numDays + j].level(0);
                    } else if (j == lastDay) {
                        progressVars[i * numDays + j].lower(1);
                    }
                }
            }
        } else {
            for (int i = 0; i < numTasks; i++) {
                EventDto issue = indexIssue.get(i * numDays);
                Double priority = issuePriority.get(issue);
                for (int j = 0; j < numDays; j++) {
                    if (priority != null) {
                        // приоритет должен учитывать время выполнения, если мы хотим, чтобы таск закончился раньше
                        // то его приоритет должен быть таким чтобы priority * task_length < был меньше чем у другого таска
                        int duration = issueDuration.get(issue);
                        double adjustedPriority = 10.0 / (Math.pow(duration / 8, 3) + 2);
                        progressVars[i * numDays + j] = Variable.make(String.format("x_%d_%d", i, j))
                                                                .weight(adjustedPriority)
                                                                .lower(0)
                                                                .upper(1);
                    } else {
                        progressVars[i * numDays + j] = Variable.make(String.format("x_%d_%d", i, j))
                                                                .weight(10)
                                                                .lower(0)
                                                                .upper(1);
                    }
                    if (j == 0) {
                        progressVars[i * numDays + j].level(0);
                    } else if (j == lastDay) {
                        progressVars[i * numDays + j].lower(1);
                    }
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
        //        DirectedAcyclicGraph<EventDto, DefaultEdge> dependenceGraph = new DirectedAcyclicGraph<>(DefaultEdge.class);
        for (Map.Entry<EventDto, List<EventDto>> dep : dependenceList.entrySet()) {
            EventDto issue = dep.getKey();
            List<EventDto> issueDepList = dep.getValue();

            //            dependenceGraph.addVertex(issue);

            int taskIdx = issueIndex.get(issue);
            // dependence_constraints.append(progress[task, 0] == 0)
            model.addExpression("dependence_start" + issue.getIssueKey())
                 .set(progressVars[taskIdx * numDays], 1)
                 .level(0);

            for (EventDto dependantIssue : issueDepList) {
                if (!issueIndex.containsKey(dependantIssue))
                    continue;
                int dependantIssueIdx = issueIndex.get(dependantIssue);

                //                dependenceGraph.addVertex(dependantIssue);
                //                dependenceGraph.addEdge(dependantIssue, issue);

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
                    planEnd = today.plusDays(j + 1).toDate();
                    break;
                }
            }

            logger.debug(sb.toString());

            plan.put(issue, Pair.nicePairOf(planStart, planEnd));
        }

        //        Map<EventDto, Integer> issueDepth = nodeDepths(dependenceGraph);
        //        Integer maxDepth = Collections.max(issueDepth.values());
        //        List<EventRestriction> eventRestrictions = plan.entrySet()
        //                                                       .stream()
        //                                                       .map(entry -> new EventRestriction(entry.getValue().first(), entry.getValue().second(), issueDuration.get(entry.getKey()), entry.getKey()))
        //                                                       .sorted(Comparator.comparing(EventRestriction::getPlanEnd)
        //                                                                         .thenComparingInt(e -> issueDepth.getOrDefault(e, maxDepth))
        //                                                                         .thenComparingInt(e -> Period.fieldDifference(new LocalDate(e.getPlanStart().getTime()), new LocalDate(e.getPlanEnd().getTime())).getDays() * maxDayCapacity / e.getDuration())
        //                                                       )
        //                                                       .collect(Collectors.toList());
        //
        //        for (EventRestriction eventRestriction : eventRestrictions) {
        //
        //        }

        return plan;
    }

    //    private Map<EventDto, Integer> nodeDepths(DirectedAcyclicGraph<EventDto, DefaultEdge> dependenceGraph) {
    //        final Map<EventDto, Integer> nodeDepths = new HashMap<>();
    //
    //        int vertexSize = dependenceGraph.vertexSet().size();
    //        while (vertexSize != nodeDepths.size()) {
    //            new TopologicalOrderIterator<>(dependenceGraph)
    //                    .forEachRemaining(eventDto -> {
    //                        int depth;
    //                        Set<EventDto> ancestors = dependenceGraph.getAncestors(eventDto);
    //                        if (ancestors.isEmpty()) {
    //                            depth = 0;
    //                        } else if (nodeDepths.keySet().containsAll(ancestors)) {
    //                            depth = ancestors.stream()
    //                                             .mapToInt(nodeDepths::get)
    //                                             .max()
    //                                             .orElse(0) + 1;
    //                        } else {
    //                            return;
    //                        }
    //                        nodeDepths.put(eventDto, depth);
    //                    });
    //        }
    //
    //        return nodeDepths;
    //    }

    private class EventRestriction {
        Date planStart;
        Date planEnd;
        int duration;
        EventDto event;

        public EventRestriction(Date planStart, Date planEnd, int duration, EventDto event) {
            this.planStart = planStart;
            this.planEnd = planEnd;
            this.duration = duration;
            this.event = event;
        }

        public Date getPlanStart() {
            return planStart;
        }

        public void setPlanStart(Date planStart) {
            this.planStart = planStart;
        }

        public Date getPlanEnd() {
            return planEnd;
        }

        public void setPlanEnd(Date planEnd) {
            this.planEnd = planEnd;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public EventDto getEvent() {
            return event;
        }

        public void setEvent(EventDto event) {
            this.event = event;
        }
    }

    /**
     * todo
     *
     * @param issues
     * @param issueDuration  issue duration in hours
     * @param issueDeadline  issue deadline in days from current day
     * @param dependenceList issue depends list. Issues in list must done faster.
     * @param issuePriority  issue priorities in double
     * @param numDays        planning period
     * @param maxDayCapacity max working hours in day
     * @return
     */
    public Map<EventDto, Pair<Date, Date>> generatePlan2(List<EventDto> issues,
                                                         Map<EventDto, Integer> issueDuration,
                                                         Map<EventDto, Integer> issueDeadline,
                                                         Map<EventDto, List<EventDto>> dependenceList,
                                                         Map<EventDto, Double> issuePriority,
                                                         int numDays,
                                                         int maxDayCapacity) {
        Store store = new Store();

        int numTasks = issues.size();
        int maxTime = maxDayCapacity * numDays;
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

        List<IntVar> vars = new ArrayList<>();

        final IntVar[] startTime = new IntVar[numTasks];
        final IntVar[] endTime = new IntVar[numTasks];

        final int[] duration = new int[numTasks];
        for (int i = 0; i < numTasks; i++) {
            duration[i] = issueDuration.get(indexIssue.get(i));
            startTime[i] = new IntVar(store, "startTime[" + i + "]", 0, maxTime - duration[i]);
            endTime[i] = new IntVar(store, "endTime[" + i + "]", 0, maxTime);

            store.impose(new XplusCeqZ(startTime[i], duration[i], endTime[i]));

            vars.add(startTime[i]);
            //vars.add(endTime[i]);
        }

        IntVar earliestEndTime = new IntVar(store, "earliestEndTime", 0, maxTime);
        vars.add(earliestEndTime);
        store.impose(new Max(endTime, earliestEndTime));

        // dependence constraints
        for (Map.Entry<EventDto, List<EventDto>> dep : dependenceList.entrySet()) {
            List<EventDto> issueDepList = dep.getValue();
            int taskIdx = issueIndex.get(dep.getKey());

            for (EventDto dependantIssue : issueDepList) {
                if (!issueIndex.containsKey(dependantIssue))
                    continue;
                int dependantIssueIdx = issueIndex.get(dependantIssue);

                store.impose(new XplusClteqZ(startTime[dependantIssueIdx], duration[dependantIssueIdx], startTime[taskIdx]));
            }
        }

        // deadlines
        for (Map.Entry<EventDto, Integer> task : issueDeadline.entrySet()) {
            int issueIdx = issueIndex.get(task.getKey());
            store.impose(new XlteqC(endTime[issueIdx], task.getValue() * maxDayCapacity));
        }

        // no overlap
        for (Map.Entry<UserDto, List<EventDto>> e : userIssueGroup.entrySet()) {
            List<EventDto> issueGroup = e.getValue();

            IntVar[] start = new IntVar[issueGroup.size()];
            IntVar[] length = new IntVar[issueGroup.size()];
            for (int i = 0; i < issueGroup.size(); i++) {
                int taskIdx = issueIndex.get(issueGroup.get(i));
                start[i] = startTime[taskIdx];
                length[i] = new IntVar(store, duration[taskIdx], duration[taskIdx]);
            }
            IntVar[] height = new IntVar[start.length];
            Arrays.fill(height, new IntVar(store, "height", 0, 0));
            IntVar[] heightLength = new IntVar[start.length];
            Arrays.fill(heightLength, new IntVar(store, "heightLength", 1, 1));

            store.impose(new Nooverlap(start, height, length, heightLength));
        }

        //priority
        List<EventDto> hasPriorityAndNoDependance = new ArrayList<>(issues);
        hasPriorityAndNoDependance.removeAll(dependenceList.keySet());
        hasPriorityAndNoDependance.retainAll(issuePriority.keySet());
        hasPriorityAndNoDependance.sort(Comparator.comparingDouble(issuePriority::get).reversed());

        for (int i = 0; i < hasPriorityAndNoDependance.size() - 1; i++) {
            int idx1 = issueIndex.get(hasPriorityAndNoDependance.get(i));
            int idx2 = issueIndex.get(hasPriorityAndNoDependance.get(i + 1));
            store.impose(new XlteqY(startTime[idx1], startTime[idx2]));
        }

        Map<EventDto, Pair<Date, Date>> plan = new HashMap<>();
        logger.debug("maximizing");
        Search<IntVar> search = new DepthFirstSearch<>();

        SelectChoicePoint<IntVar> select = new SplitSelect<>(vars.toArray(new IntVar[1]), new SmallestMin<>(), new IndomainMin<>());
        boolean result = search.labeling(store, select, earliestEndTime);
        logger.debug("maximized");
        logger.debug("result state {}", result);

        DateTime today = DateTime.now();
        for (int i = 0; i < numTasks; i++) {
            EventDto issue = indexIssue.get(i);
            Integer durationHours = issueDuration.get(issue);
            Date planStart = today.plusDays(startTime[i].value() / maxDayCapacity)
                                  .plusHours(startTime[i].value() % maxDayCapacity)
                                  .toDate();
            Date planEnd = today.plusDays((startTime[i].value() + durationHours) / maxDayCapacity)
                                .plusHours((startTime[i].value() + durationHours) % maxDayCapacity)
                                .toDate();

            plan.put(issue, Pair.nicePairOf(planStart, planEnd));
        }

        return plan;
    }
}
