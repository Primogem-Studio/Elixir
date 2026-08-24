package net.per.elixir.compat.tlm;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;

@LittleMaidExtension
public class ElixirLittleMaidExtension implements ILittleMaid {

    public ElixirLittleMaidExtension() {
    }

    @Override
    public void addMaidTask(TaskManager manager) {
        manager.add(new AlchemyMaidTask());
    }
}
