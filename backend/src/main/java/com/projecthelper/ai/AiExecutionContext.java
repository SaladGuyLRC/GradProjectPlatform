package com.projecthelper.ai;

import com.projecthelper.ai.taskdraft.TaskDraftResult;

import java.util.ArrayList;
import java.util.List;

public final class AiExecutionContext {
    private static final ThreadLocal<List<Citation>> CITATIONS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<List<Action>> ACTIONS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<TaskDraftResult> TASK_DRAFT_RESULT = new ThreadLocal<>();

    private AiExecutionContext() {}

    public static void reset() {
        CITATIONS.set(new ArrayList<>());
        ACTIONS.set(new ArrayList<>());
        TASK_DRAFT_RESULT.remove();
    }

    public static void addCitation(Citation citation) {
        if (CITATIONS.get().stream().noneMatch(existing -> existing.documentId().equals(citation.documentId())
                && existing.chunkIndex() == citation.chunkIndex())) CITATIONS.get().add(citation);
    }

    public static void addAction(Action action) { ACTIONS.get().add(action); }
    public static void setTaskDraftResult(TaskDraftResult result) { TASK_DRAFT_RESULT.set(result); }
    public static List<Citation> citations() { return List.copyOf(CITATIONS.get()); }
    public static List<Action> actions() { return List.copyOf(ACTIONS.get()); }
    public static TaskDraftResult taskDraftResult() { return TASK_DRAFT_RESULT.get(); }
    public static void clear() { CITATIONS.remove(); ACTIONS.remove(); TASK_DRAFT_RESULT.remove(); }

    public record Citation(String documentId, String title, String originalFilename,
                           int chunkIndex, int pageStart, int pageEnd) {}
    public record Action(String type, String entityId, String summary) {}
}
