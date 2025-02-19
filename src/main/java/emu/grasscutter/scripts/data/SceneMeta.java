package emu.grasscutter.scripts.data;

import com.github.davidmoten.rtreemulti.RTree;
import com.github.davidmoten.rtreemulti.geometry.Geometry;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.scripts.SceneIndexManager;
import emu.grasscutter.scripts.ScriptLoader;
import lombok.Setter;
import lombok.ToString;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static emu.grasscutter.Configuration.SCRIPT;

@ToString
@Setter
public class SceneMeta {

    public SceneConfig config;
    public Map<Integer, SceneBlock> blocks;

    public Bindings context;

    public RTree<SceneBlock, Geometry> sceneBlockIndex;

    public static SceneMeta of(int sceneId) {
        return new SceneMeta().load(sceneId);
    }

    public SceneMeta load(int sceneId){
        // Get compiled script if cached
        CompiledScript cs = ScriptLoader.getScriptByPath(
                SCRIPT("Scene/" + sceneId + "/scene" + sceneId + "." + ScriptLoader.getScriptType()));

        if (cs == null) {
            Grasscutter.getLogger().warn("No script found for scene " + sceneId);
            return null;
        }

        // Create bindings
        context = ScriptLoader.getEngine().createBindings();

        // Eval script
        try {
            cs.eval(context);

            this.config = ScriptLoader.getSerializer().toObject(SceneConfig.class, context.get("scene_config"));

            // TODO optimize later
            // Create blocks
            List<Integer> blockIds = ScriptLoader.getSerializer().toList(Integer.class, context.get("blocks"));
            List<SceneBlock> blocks = ScriptLoader.getSerializer().toList(SceneBlock.class, context.get("block_rects"));

            for (int i = 0; i < blocks.size(); i++) {
                SceneBlock block = blocks.get(i);
                block.id = blockIds.get(i);

            }

            this.blocks = blocks.stream().collect(Collectors.toMap(b -> b.id, b -> b));
            this.sceneBlockIndex = SceneIndexManager.buildIndex(2, blocks, SceneBlock::toRectangle);

        } catch (ScriptException e) {
            Grasscutter.getLogger().error("Error running script", e);
            return null;
        }
        Grasscutter.getLogger().info("scene {} metadata is loaded successfully.", sceneId);
        return this;
    }
}
