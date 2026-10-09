package dev.stow.client.memory;

import java.util.List;
import static dev.stow.client.memory.ChestMemoryStore.MaterialGoal;

/** Saved targets in project order; registry IDs make the list independent of language. */
public final class MaterialListExporter {
    private MaterialListExporter(){}
    public static String export(String projectName,List<MaterialGoal> goals){
        var text=new StringBuilder("# ").append(projectName.replaceAll("\\R"," ")).append('\n');
        for(var goal:goals)text.append(goal.target()).append(' ').append(goal.itemId()).append('\n');
        return text.toString();
    }
}
