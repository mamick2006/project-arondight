package arondight.weave.nodes;

import java.util.List;

public final class PassiveNode extends BaseNode {

    public PassiveNode(String id, String name, String description, Cluster cluster,
                       List<String> prereqAnyOf) {
        super(id, name, description, cluster, prereqAnyOf);
    }
}
